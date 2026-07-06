#!/usr/bin/env python3
import argparse
from collections import Counter
from pathlib import Path

import yaml


IMAGE_EXTS = {".jpg", ".jpeg", ".png", ".bmp", ".webp"}


def load_yaml(path: Path) -> dict:
    with path.open("r", encoding="utf-8") as f:
        return yaml.safe_load(f)


def iter_images(path: Path) -> list[Path]:
    if not path.exists():
        return []
    return sorted(p for p in path.rglob("*") if p.suffix.lower() in IMAGE_EXTS)


def label_path_for(root: Path, split: str, image: Path) -> Path:
    image_root = root / "images" / split
    rel = image.relative_to(image_root)
    return root / "labels" / split / rel.with_suffix(".txt")


def inspect_split(root: Path, split: str, num_classes: int) -> dict:
    images = iter_images(root / "images" / split)
    missing_labels = []
    invalid_lines = []
    full_image_boxes = 0
    total_boxes = 0
    class_counts = Counter()

    for image in images:
        label_file = label_path_for(root, split, image)
        if not label_file.exists():
            missing_labels.append(str(image))
            continue
        for line_no, raw in enumerate(label_file.read_text(encoding="utf-8").splitlines(), start=1):
            line = raw.strip()
            if not line:
                continue
            parts = line.split()
            if len(parts) != 5:
                invalid_lines.append(f"{label_file}:{line_no}: expected 5 values, got {len(parts)}")
                continue
            try:
                class_id = int(parts[0])
                x, y, w, h = [float(v) for v in parts[1:]]
            except ValueError:
                invalid_lines.append(f"{label_file}:{line_no}: non-numeric value")
                continue
            if class_id < 0 or class_id >= num_classes:
                invalid_lines.append(f"{label_file}:{line_no}: class id {class_id} outside 0..{num_classes - 1}")
                continue
            if not all(0.0 <= value <= 1.0 for value in (x, y, w, h)):
                invalid_lines.append(f"{label_file}:{line_no}: bbox values must be normalized 0..1")
                continue
            if w <= 0.0 or h <= 0.0:
                invalid_lines.append(f"{label_file}:{line_no}: width/height must be positive")
                continue
            total_boxes += 1
            class_counts[class_id] += 1
            if abs(x - 0.5) < 0.01 and abs(y - 0.5) < 0.01 and w > 0.98 and h > 0.98:
                full_image_boxes += 1

    return {
        "images": len(images),
        "missing_labels": missing_labels,
        "invalid_lines": invalid_lines,
        "total_boxes": total_boxes,
        "full_image_boxes": full_image_boxes,
        "class_counts": class_counts,
    }


def main() -> None:
    parser = argparse.ArgumentParser(description="Validate a YOLO-format IP102 dataset before training.")
    parser.add_argument("--data", required=True, help="YOLO data.yaml")
    args = parser.parse_args()

    data_path = Path(args.data)
    data = load_yaml(data_path)
    root = Path(data["path"])
    names = data["names"]
    num_classes = len(names)

    failed = False
    aggregate = Counter()
    for split in ("train", "val", "test"):
        result = inspect_split(root, split, num_classes)
        aggregate.update(result["class_counts"])
        print(f"{split}: {result['images']} images, {result['total_boxes']} boxes")
        if result["missing_labels"]:
            failed = True
            print(f"  missing labels: {len(result['missing_labels'])}")
            for item in result["missing_labels"][:10]:
                print(f"    {item}")
        if result["invalid_lines"]:
            failed = True
            print(f"  invalid label lines: {len(result['invalid_lines'])}")
            for item in result["invalid_lines"][:10]:
                print(f"    {item}")
        if result["total_boxes"]:
            ratio = result["full_image_boxes"] / result["total_boxes"]
            print(f"  full-image-like boxes: {result['full_image_boxes']} ({ratio:.1%})")
            if ratio > 0.20:
                print("  warning: many boxes look like full-image pseudo labels")

    missing_classes = [idx for idx in range(num_classes) if aggregate[idx] == 0]
    print(f"classes with boxes: {num_classes - len(missing_classes)}/{num_classes}")
    if missing_classes:
        print(f"classes with no boxes: {missing_classes[:30]}")

    if failed:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
