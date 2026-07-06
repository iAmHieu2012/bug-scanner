#!/usr/bin/env python3
import argparse
import csv
import re
import shutil
from collections import Counter, defaultdict
from pathlib import Path

import yaml


IMAGE_EXTS = {".jpg", ".jpeg", ".png", ".bmp", ".webp"}
SPLITS = ("train", "val", "test")


def load_yaml(path: Path) -> dict:
    with path.open("r", encoding="utf-8") as f:
        return yaml.safe_load(f)


def normalize_names(raw_names) -> dict[int, str]:
    if isinstance(raw_names, dict):
        return {int(k): str(v) for k, v in raw_names.items()}
    return {idx: str(name) for idx, name in enumerate(raw_names)}


def iter_images(root: Path, split: str) -> list[Path]:
    image_root = root / "images" / split
    if not image_root.exists():
        return []
    return sorted(p for p in image_root.rglob("*") if p.suffix.lower() in IMAGE_EXTS)


def label_for_image(root: Path, split: str, image: Path) -> Path:
    rel = image.relative_to(root / "images" / split)
    return root / "labels" / split / rel.with_suffix(".txt")


def parse_label_file(label_path: Path) -> list[tuple[int, float, float, float, float]]:
    boxes = []
    if not label_path.exists():
        return boxes
    for raw in label_path.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line:
            continue
        parts = line.split()
        if len(parts) != 5:
            continue
        try:
            class_id = int(parts[0])
            x, y, w, h = [float(value) for value in parts[1:]]
        except ValueError:
            continue
        boxes.append((class_id, x, y, w, h))
    return boxes


def safe_key(name: str) -> str:
    return re.sub(r"[^A-Za-z0-9_.-]+", "_", name).strip("_") or "class"


def parse_metrics_log(path: Path | None, names: dict[int, str]) -> dict[str, dict[str, str]]:
    if path is None or not path.exists():
        return {}
    by_name = {}
    name_set = set(names.values())
    for raw in path.read_text(encoding="utf-8", errors="replace").replace("\r", "\n").splitlines():
        line = raw.strip()
        if not line:
            continue
        parts = line.split()
        if len(parts) < 7:
            continue
        numeric_tail = parts[-6:]
        try:
            int(float(numeric_tail[0]))
            int(float(numeric_tail[1]))
            float_values = [str(float(value)).rstrip("0").rstrip(".") for value in numeric_tail[2:]]
        except ValueError:
            continue
        class_name = " ".join(parts[:-6])
        if class_name not in name_set:
            continue
        by_name[class_name] = {
            "metric_images": str(int(float(numeric_tail[0]))),
            "metric_instances": str(int(float(numeric_tail[1]))),
            "metric_precision": float_values[0],
            "metric_recall": float_values[1],
            "metric_mAP50": float_values[2],
            "metric_mAP50-95": float_values[3],
        }
    return by_name


def audit_dataset(
    data_yaml: Path,
    metrics_log: Path | None,
    output: Path,
    sample_count: int,
    min_train_images: int,
    min_val_images: int,
    min_test_images: int,
) -> None:
    data = load_yaml(data_yaml)
    root = Path(data["path"])
    names = normalize_names(data["names"])
    metrics = parse_metrics_log(metrics_log, names)

    per_class = {
        class_id: {
            "class_id": class_id,
            "name": name,
            "split_images": Counter(),
            "split_boxes": Counter(),
            "tiny_boxes": 0,
            "full_image_boxes": 0,
            "total_boxes": 0,
            "sample_images": [],
        }
        for class_id, name in names.items()
    }
    missing_labels = []
    invalid_classes = Counter()

    for split in SPLITS:
        for image in iter_images(root, split):
            boxes = parse_label_file(label_for_image(root, split, image))
            if not boxes:
                missing_labels.append(str(image))
                continue
            classes_in_image = set()
            for class_id, _x, _y, w, h in boxes:
                if class_id not in per_class:
                    invalid_classes[class_id] += 1
                    continue
                row = per_class[class_id]
                row["total_boxes"] += 1
                row["split_boxes"][split] += 1
                classes_in_image.add(class_id)
                if w * h <= 0.0004 or w <= 0.02 or h <= 0.02:
                    row["tiny_boxes"] += 1
                if w >= 0.98 and h >= 0.98:
                    row["full_image_boxes"] += 1
            for class_id in classes_in_image:
                row = per_class[class_id]
                row["split_images"][split] += 1
                if len(row["sample_images"]) < sample_count:
                    row["sample_images"].append(image)

    output.mkdir(parents=True, exist_ok=True)
    sample_root = output / "samples"
    sample_root.mkdir(parents=True, exist_ok=True)

    fieldnames = [
        "class_id",
        "name",
        "train_images",
        "train_boxes",
        "val_images",
        "val_boxes",
        "test_images",
        "test_boxes",
        "total_boxes",
        "tiny_box_ratio",
        "full_image_box_ratio",
        "metric_precision",
        "metric_recall",
        "metric_mAP50",
        "metric_mAP50-95",
        "issues",
    ]
    rows = []
    priority_rows = []
    for class_id in sorted(per_class):
        row = per_class[class_id]
        total_boxes = row["total_boxes"]
        tiny_ratio = row["tiny_boxes"] / total_boxes if total_boxes else 0.0
        full_ratio = row["full_image_boxes"] / total_boxes if total_boxes else 0.0
        issues = []
        if row["split_images"]["train"] < min_train_images:
            issues.append("low-train")
        if row["split_images"]["val"] < min_val_images:
            issues.append("low-val")
        if row["split_images"]["test"] < min_test_images:
            issues.append("low-test")
        if tiny_ratio >= 0.50:
            issues.append("tiny-boxes")
        if full_ratio > 0.0:
            issues.append("full-image-like-boxes")
        metric_row = metrics.get(row["name"], {})
        out_row = {
            "class_id": class_id,
            "name": row["name"],
            "train_images": row["split_images"]["train"],
            "train_boxes": row["split_boxes"]["train"],
            "val_images": row["split_images"]["val"],
            "val_boxes": row["split_boxes"]["val"],
            "test_images": row["split_images"]["test"],
            "test_boxes": row["split_boxes"]["test"],
            "total_boxes": total_boxes,
            "tiny_box_ratio": f"{tiny_ratio:.4f}",
            "full_image_box_ratio": f"{full_ratio:.4f}",
            "metric_precision": metric_row.get("metric_precision", ""),
            "metric_recall": metric_row.get("metric_recall", ""),
            "metric_mAP50": metric_row.get("metric_mAP50", ""),
            "metric_mAP50-95": metric_row.get("metric_mAP50-95", ""),
            "issues": ";".join(issues),
        }
        rows.append(out_row)
        if issues:
            issue_score = 0
            if "full-image-like-boxes" in issues:
                issue_score += 100
            if "low-train" in issues:
                issue_score += 50
            if "low-val" in issues or "low-test" in issues:
                issue_score += 25
            if "tiny-boxes" in issues:
                issue_score += 10
            priority_rows.append((issue_score, int(out_row["train_images"]), out_row))
        for image in row["sample_images"]:
            dst = sample_root / safe_key(row["name"]) / image.name
            dst.parent.mkdir(parents=True, exist_ok=True)
            if not dst.exists():
                shutil.copy2(image, dst)

    with (output / "class_quality_report.csv").open("w", encoding="utf-8", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(rows)

    priority_rows.sort(key=lambda item: (-item[0], item[1], int(item[2]["class_id"])))
    with (output / "weak_class_priority.csv").open("w", encoding="utf-8", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows([row for _score, _train_images, row in priority_rows])

    summary = [
        "# YOLO Dataset Quality Audit",
        "",
        f"- Classes: {len(names)}",
        f"- Weak classes: {len(priority_rows)}",
        f"- Missing/empty labels: {len(missing_labels)}",
        f"- Invalid class ids: {sum(invalid_classes.values())}",
        "",
        "## Priority Classes",
        "",
        "| class | train | val | test | issues |",
        "|---|---:|---:|---:|---|",
    ]
    for _score, _train_images, row in priority_rows:
        summary.append(
            f"| {row['name']} | {row['train_images']} | {row['val_images']} | "
            f"{row['test_images']} | {row['issues']} |"
        )
    (output / "weak_class_priority.md").write_text("\n".join(summary) + "\n", encoding="utf-8")
    print(f"Wrote {output}")
    print(f"Weak classes: {len(priority_rows)}")


def main() -> None:
    parser = argparse.ArgumentParser(description="Audit YOLO dataset quality and weak classes.")
    parser.add_argument("--data", required=True, help="YOLO data.yaml")
    parser.add_argument("--metrics-log", help="Optional YOLO training log with per-class metrics.")
    parser.add_argument("--output", default="datasets/vietnam-practical-yolo/metadata/quality-audit")
    parser.add_argument("--sample-count", type=int, default=12)
    parser.add_argument("--min-train-images", type=int, default=300)
    parser.add_argument("--min-val-images", type=int, default=30)
    parser.add_argument("--min-test-images", type=int, default=30)
    args = parser.parse_args()

    audit_dataset(
        data_yaml=Path(args.data),
        metrics_log=Path(args.metrics_log) if args.metrics_log else None,
        output=Path(args.output),
        sample_count=args.sample_count,
        min_train_images=args.min_train_images,
        min_val_images=args.min_val_images,
        min_test_images=args.min_test_images,
    )


if __name__ == "__main__":
    main()
