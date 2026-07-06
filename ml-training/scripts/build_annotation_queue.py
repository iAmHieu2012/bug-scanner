#!/usr/bin/env python3
import argparse
import csv
import shutil
from collections import Counter
from pathlib import Path

import yaml


IMAGE_EXTS = {".jpg", ".jpeg", ".png", ".bmp", ".webp"}


def load_yaml(path: Path) -> dict:
    with path.open("r", encoding="utf-8") as f:
        return yaml.safe_load(f)


def link_or_copy(src: Path, dst: Path, copy: bool) -> None:
    dst.parent.mkdir(parents=True, exist_ok=True)
    if dst.exists() or dst.is_symlink():
        dst.unlink()
    if copy:
        shutil.copy2(src, dst)
    else:
        dst.symlink_to(src.resolve())


def candidate_images(class_dir: Path) -> list[Path]:
    if not class_dir.exists():
        return []
    return sorted(p for p in class_dir.rglob("*") if p.suffix.lower() in IMAGE_EXTS)


def destination_for(out_root: Path, target_key: str, source_name: str, source_class: str, image: Path) -> Path:
    safe_source_class = "".join(ch if ch.isalnum() or ch in ("-", "_") else "_" for ch in source_class)
    return out_root / "images" / target_key / f"{source_name}_{safe_source_class}_{image.name}"


def write_readme(out_root: Path, counts: Counter, review_counts: Counter) -> None:
    lines = [
        "# BugScanner Annotation Queue",
        "",
        "These images are candidate data only. They came from classification/image-only sources, so they must be labeled with real bounding boxes before YOLO training.",
        "",
        "Do not convert these to full-image boxes. Import the `images/` folders into CVAT, Roboflow, Label Studio, or another annotation tool and export real YOLO/COCO/VOC boxes after review.",
        "",
        "## Queued Images",
        "",
        "| target class | images |",
        "|---|---:|",
    ]
    for key, count in sorted(counts.items()):
        lines.append(f"| {key} | {count} |")
    if review_counts:
        lines.extend([
            "",
            "## Taxonomy Review Queue",
            "",
            "These folders are useful source material, but their source labels are broader than the model classes. Review and assign each image to the correct target class before annotation.",
            "",
            "| source class | images |",
            "|---|---:|",
        ])
        for key, count in sorted(review_counts.items()):
            lines.append(f"| {key} | {count} |")
    (out_root / "README.md").write_text("\n".join(lines) + "\n", encoding="utf-8")


def parse_target_classes(raw: str | None) -> set[str] | None:
    if not raw:
        return None
    targets = {item.strip() for item in raw.split(",") if item.strip()}
    return targets or None


def main() -> None:
    parser = argparse.ArgumentParser(description="Build a manual annotation queue from classification-only candidate pest images.")
    parser.add_argument("--candidates", default="configs/vietnam-candidate-sources.yaml", help="Candidate-source manifest.")
    parser.add_argument("--output", default="datasets/labeling-queue", help="Output queue root.")
    parser.add_argument("--max-per-class", type=int, default=500, help="Maximum images to queue for each target class.")
    parser.add_argument("--copy-images", action="store_true", help="Copy images instead of symlinking.")
    parser.add_argument(
        "--target-classes",
        help="Comma-separated target keys to queue, for example rice_thrips,whitebacked_planthopper.",
    )
    args = parser.parse_args()

    candidate_config = load_yaml(Path(args.candidates))
    out_root = Path(args.output)
    target_classes = parse_target_classes(args.target_classes)
    if out_root.exists():
        shutil.rmtree(out_root)
    out_root.mkdir(parents=True, exist_ok=True)

    rows = []
    review_rows = []
    counts = Counter()
    review_counts = Counter()
    skipped_sources = []

    for source in candidate_config.get("candidate_sources", []):
        if source.get("decision") != "annotation_queue_only":
            continue
        source_root = Path(source["local_root"])
        if not source_root.exists():
            skipped_sources.append((source["name"], f"missing local root: {source_root}"))
            continue
        for source_class, target_key in source.get("class_map", {}).items():
            if target_classes is not None and target_key not in target_classes:
                continue
            images = candidate_images(source_root / source_class)
            for image in images[: args.max_per_class]:
                dst = destination_for(out_root, target_key, source["name"], source_class, image)
                link_or_copy(image, dst, args.copy_images)
                counts[target_key] += 1
                rows.append({
                    "target_key": target_key,
                    "source_name": source["name"],
                    "source_class": source_class,
                    "source_path": str(image),
                    "queued_path": str(dst),
                    "license": source.get("license", ""),
                    "source_url": source.get("url", ""),
                })
        for source_class, suggested_targets in source.get("review_map", {}).items():
            suggested_set = set(suggested_targets or [])
            if target_classes is not None and suggested_set.isdisjoint(target_classes):
                continue
            images = candidate_images(source_root / source_class)
            review_key = f"_review/{source['name']}/{source_class}"
            for image in images[: args.max_per_class]:
                dst = destination_for(out_root, review_key, source["name"], source_class, image)
                link_or_copy(image, dst, args.copy_images)
                review_counts[source_class] += 1
                review_rows.append({
                    "source_name": source["name"],
                    "source_class": source_class,
                    "suggested_targets": ";".join(suggested_targets or []),
                    "source_path": str(image),
                    "queued_path": str(dst),
                    "license": source.get("license", ""),
                    "source_url": source.get("url", ""),
                })

    metadata_dir = out_root / "metadata"
    metadata_dir.mkdir(parents=True, exist_ok=True)
    with (metadata_dir / "queue_manifest.csv").open("w", encoding="utf-8", newline="") as f:
        fieldnames = ["target_key", "source_name", "source_class", "source_path", "queued_path", "license", "source_url"]
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(rows)

    with (metadata_dir / "review_manifest.csv").open("w", encoding="utf-8", newline="") as f:
        fieldnames = ["source_name", "source_class", "suggested_targets", "source_path", "queued_path", "license", "source_url"]
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(review_rows)

    if skipped_sources:
        with (metadata_dir / "skipped_sources.csv").open("w", encoding="utf-8", newline="") as f:
            writer = csv.writer(f)
            writer.writerow(["source_name", "reason"])
            writer.writerows(skipped_sources)

    write_readme(out_root, counts, review_counts)
    print(f"Wrote {out_root}")
    for key, count in sorted(counts.items()):
        print(f"{key}: {count} candidate images")
    for key, count in sorted(review_counts.items()):
        print(f"review/{key}: {count} candidate images")
    if skipped_sources:
        print(f"skipped sources: {len(skipped_sources)}")


if __name__ == "__main__":
    main()
