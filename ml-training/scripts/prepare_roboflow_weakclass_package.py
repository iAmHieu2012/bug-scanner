#!/usr/bin/env python3
import argparse
import csv
import shutil
from collections import Counter, defaultdict
from pathlib import Path


DEFAULT_QUOTA = {
    "Delphacidae": 160,
    "Thripidae": 120,
    "Phlaeothripidae": 120,
    "Noctuidae": 100,
}

DEFAULT_SOURCE_CLASSES = tuple(DEFAULT_QUOTA.keys())


def read_csv(path: Path) -> list[dict[str, str]]:
    with path.open("r", encoding="utf-8", newline="") as f:
        return list(csv.DictReader(f))


def parse_quota(raw: str | None) -> dict[str, int]:
    if not raw:
        return dict(DEFAULT_QUOTA)
    quotas: dict[str, int] = {}
    for item in raw.split(","):
        if not item.strip():
            continue
        key, _, value = item.partition("=")
        if not key.strip() or not value.strip():
            raise ValueError(f"Invalid quota item: {item!r}. Expected ClassName=Count.")
        count = int(value)
        if count < 0:
            raise ValueError(f"Quota must be non-negative for {key!r}")
        quotas[key.strip()] = count
    return quotas


def parse_source_classes(raw: str | None, quotas: dict[str, int]) -> tuple[str, ...]:
    if raw:
        values = tuple(item.strip() for item in raw.split(",") if item.strip())
        if values:
            return values
    return tuple(quotas.keys()) if quotas else DEFAULT_SOURCE_CLASSES


def safe_filename(value: str) -> str:
    return "".join(ch if ch.isalnum() or ch in ("-", "_", ".") else "_" for ch in value)


def write_readme(out_root: Path, counts: Counter, targets_by_class: dict[str, set[str]]) -> None:
    lines = [
        "# BugScanner V4 Weak-Class Roboflow Upload",
        "",
        "Upload the `images/` folders to Roboflow for manual taxonomy review and bounding-box annotation.",
        "",
        "Use these exact Roboflow class names when assigning labels:",
        "",
        "- `rice_thrips`",
        "- `whitebacked_planthopper`",
        "- `small_brown_planthopper`",
        "- `hairy_caterpillar`",
        "- `cabbage_caterpillar`",
        "",
        "Reject images when the pest is not visible enough, the taxonomy is ambiguous, or the object cannot be boxed tightly.",
        "Do not create full-image boxes.",
        "",
        "## Source Folders",
        "",
        "| source class | images | suggested targets |",
        "|---|---:|---|",
    ]
    for source_class, count in sorted(counts.items()):
        targets = "; ".join(sorted(targets_by_class[source_class]))
        lines.append(f"| {source_class} | {count} | {targets} |")
    (out_root / "README.md").write_text("\n".join(lines) + "\n", encoding="utf-8")


def clear_output(out_root: Path) -> None:
    if out_root.exists():
        shutil.rmtree(out_root)
    (out_root / "images").mkdir(parents=True, exist_ok=True)
    (out_root / "metadata").mkdir(parents=True, exist_ok=True)


def resolve_queued_path(queue_root: Path, raw_path: str) -> Path:
    path = Path(raw_path)
    if path.is_absolute():
        return path
    candidates = [
        path,
        queue_root / path,
        queue_root.parent.parent / path,
    ]
    for candidate in candidates:
        if candidate.exists():
            return candidate
    return candidates[-1]


def main() -> None:
    parser = argparse.ArgumentParser(description="Prepare a copied Roboflow upload package from the weak-class review queue.")
    parser.add_argument("--queue", default="datasets/labeling-queue", help="Annotation queue root.")
    parser.add_argument("--output", default="datasets/roboflow-v4-weakclass-upload", help="Roboflow upload package output root.")
    parser.add_argument("--quota", help="Comma-separated per-source-class quota, e.g. Delphacidae=160,Thripidae=120.")
    parser.add_argument("--source-classes", help="Comma-separated source classes to include. Defaults to quota keys.")
    args = parser.parse_args()

    queue_root = Path(args.queue)
    manifest_path = queue_root / "metadata" / "review_manifest.csv"
    if not manifest_path.exists():
        raise FileNotFoundError(f"Missing review manifest: {manifest_path}")

    quotas = parse_quota(args.quota)
    source_classes = set(parse_source_classes(args.source_classes, quotas))
    out_root = Path(args.output)
    clear_output(out_root)

    kept_rows = []
    counts: Counter = Counter()
    targets_by_class: dict[str, set[str]] = defaultdict(set)
    rows = sorted(read_csv(manifest_path), key=lambda row: (row["source_class"], row["queued_path"]))

    for row in rows:
        source_class = row["source_class"]
        if source_class not in source_classes:
            continue
        if counts[source_class] >= quotas.get(source_class, 0):
            continue
        queued_path = resolve_queued_path(queue_root, row["queued_path"])
        if not queued_path.exists():
            continue
        dst_name = safe_filename(queued_path.name)
        dst_path = out_root / "images" / source_class / dst_name
        dst_path.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(queued_path.resolve(), dst_path)
        counts[source_class] += 1
        targets_by_class[source_class].update(target for target in row["suggested_targets"].split(";") if target)
        kept_rows.append({
            "source_class": source_class,
            "suggested_targets": row["suggested_targets"],
            "source_name": row["source_name"],
            "source_path": row["source_path"],
            "queue_path": row["queued_path"],
            "upload_path": str(dst_path),
            "license": row.get("license", ""),
            "source_url": row.get("source_url", ""),
        })

    with (out_root / "metadata" / "upload_manifest.csv").open("w", encoding="utf-8", newline="") as f:
        fieldnames = ["source_class", "suggested_targets", "source_name", "source_path", "queue_path", "upload_path", "license", "source_url"]
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(kept_rows)

    write_readme(out_root, counts, targets_by_class)
    print(f"Wrote {out_root}")
    for source_class, count in sorted(counts.items()):
        print(f"{source_class}: {count} images")


if __name__ == "__main__":
    main()
