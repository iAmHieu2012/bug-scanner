#!/usr/bin/env python3
import argparse
import csv
import hashlib
import json
import shutil
from collections import Counter, defaultdict
from pathlib import Path

import yaml
from PIL import Image

try:
    import cv2
except ImportError:
    cv2 = None


IMAGE_EXTS = {".jpg", ".jpeg", ".png", ".bmp", ".webp"}
SPLITS = ("train", "val", "test")
DEFAULT_QUALITY_THRESHOLDS = {
    "ready_train_images": 300,
    "low_train_images": 100,
    "critical_train_images": 50,
    "min_val_images": 10,
    "min_test_images": 10,
}


def load_yaml(path: Path) -> dict:
    with path.open("r", encoding="utf-8") as f:
        return yaml.safe_load(f)


def image_roots(root: Path, split: str) -> list[Path]:
    return [
        root / "images" / split,
        root / split / "images",
    ]


def iter_images(root: Path, split: str) -> list[Path]:
    for image_root in image_roots(root, split):
        if image_root.exists():
            return sorted(p for p in image_root.rglob("*") if p.suffix.lower() in IMAGE_EXTS)
    return []


def label_for_image(root: Path, split: str, image_path: Path) -> Path:
    for image_root in image_roots(root, split):
        try:
            rel = image_path.relative_to(image_root)
        except ValueError:
            continue
        if image_root == root / "images" / split:
            return root / "labels" / split / rel.with_suffix(".txt")
        return root / split / "labels" / rel.with_suffix(".txt")
    raise ValueError(f"Image {image_path} is not under a known YOLO image directory for split {split}")


def link_or_copy(src: Path, dst: Path, copy: bool) -> None:
    dst.parent.mkdir(parents=True, exist_ok=True)
    if dst.exists() or dst.is_symlink():
        dst.unlink()
    if copy:
        shutil.copy2(src, dst)
    else:
        dst.symlink_to(src.resolve())


def unique_destination(out_root: Path, split: str, source_name: str, image_path: Path) -> Path:
    stem = image_path.stem
    suffix = image_path.suffix.lower()
    candidate = out_root / "images" / split / f"{source_name}_{stem}{suffix}"
    if not candidate.exists() and not candidate.is_symlink():
        return candidate
    digest = abs(hash(str(image_path.resolve()))) % 1_000_000_000
    return out_root / "images" / split / f"{source_name}_{stem}_{digest}{suffix}"


def parse_label_lines(label_path: Path) -> list[tuple[int, list[str]]]:
    parsed = []
    for line_no, raw in enumerate(label_path.read_text(encoding="utf-8").splitlines(), start=1):
        line = raw.strip()
        if not line:
            continue
        parts = line.split()
        if len(parts) < 5:
            continue
        class_id = int(parts[0])
        values = [float(value) for value in parts[1:]]
        if len(values) == 4:
            x, y, w, h = values
        elif len(values) >= 6 and len(values) % 2 == 0:
            xs = values[0::2]
            ys = values[1::2]
            xmin, xmax = min(xs), max(xs)
            ymin, ymax = min(ys), max(ys)
            x = (xmin + xmax) / 2.0
            y = (ymin + ymax) / 2.0
            w = xmax - xmin
            h = ymax - ymin
        else:
            raise ValueError(f"{label_path}:{line_no}: expected YOLO bbox or polygon values, got {len(parts)} values")
        if not all(0.0 <= value <= 1.0 for value in (x, y, w, h)):
            raise ValueError(f"{label_path}:{line_no}: bbox values must be normalized 0..1")
        if w <= 0.0 or h <= 0.0:
            raise ValueError(f"{label_path}:{line_no}: width and height must be positive")
        coords = [f"{value:.6f}" for value in (x, y, w, h)]
        parsed.append((class_id, coords))
    return parsed


def clear_output(out_root: Path) -> None:
    for split in SPLITS:
        for subdir in ("images", "labels"):
            path = out_root / subdir / split
            if path.exists():
                shutil.rmtree(path)


def write_data_yaml(out_root: Path, names: list[str], output: Path) -> None:
    lines = [
        f"path: {out_root.resolve()}",
        "train: images/train",
        "val: images/val",
        "test: images/test",
        "names:",
    ]
    for idx, name in enumerate(names):
        escaped = name.replace("'", "''")
        lines.append(f"  {idx}: '{escaped}'")
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text("\n".join(lines) + "\n", encoding="utf-8")


def load_source_names(source_root: Path) -> dict[int, str]:
    data_yaml = source_root / "data.yaml"
    if not data_yaml.exists():
        raise FileNotFoundError(f"Missing data.yaml for external source: {source_root}")
    data = load_yaml(data_yaml)
    raw_names = data["names"]
    if isinstance(raw_names, dict):
        return {int(k): str(v) for k, v in raw_names.items()}
    return {idx: str(name) for idx, name in enumerate(raw_names)}


def add_source(
    *,
    source_name: str,
    source_root: Path,
    class_map: dict[int, int],
    out_root: Path,
    copy_images: bool,
    stats: dict,
) -> None:
    source_stats = stats["sources"][source_name]
    for split in SPLITS:
        images = iter_images(source_root, split)
        if not images and split == "val":
            images = iter_images(source_root, "valid")
            split_for_label = "valid"
        else:
            split_for_label = split
        for image_path in images:
            source_stats["seen_images"] += 1
            label_path = label_for_image(source_root, split_for_label, image_path)
            if not label_path.exists():
                source_stats["missing_labels"] += 1
                continue
            remapped = []
            for old_class_id, coords in parse_label_lines(label_path):
                if old_class_id not in class_map:
                    source_stats["dropped_boxes"] += 1
                    continue
                new_class_id = class_map[old_class_id]
                remapped.append(f"{new_class_id} {' '.join(coords)}")
                stats["boxes_per_class"][str(new_class_id)] += 1
                stats["boxes_per_split"][split] += 1
            if not remapped:
                source_stats["empty_after_filter"] += 1
                continue
            dst_image = unique_destination(out_root, split, source_name, image_path)
            dst_label = out_root / "labels" / split / dst_image.with_suffix(".txt").name
            link_or_copy(image_path, dst_image, copy_images)
            dst_label.parent.mkdir(parents=True, exist_ok=True)
            dst_label.write_text("\n".join(remapped) + "\n", encoding="utf-8")
            source_stats["kept_images"] += 1
            stats["images_per_split"][split] += 1
            stats["image_manifest"].append({
                "split": split,
                "source_name": source_name,
                "source_type": "yolo",
                "storage": "copy" if copy_images else "symlink",
                "source_image": str(image_path),
                "output_image": str(dst_image),
                "output_label": str(dst_label),
                "class_ids": ";".join(str(class_id) for class_id in sorted({int(line.split()[0]) for line in remapped})),
                "box_count": str(len(remapped)),
            })


def deterministic_split(value: str, ratios: dict | None = None) -> str:
    ratios = ratios or {"train": 0.8, "val": 0.1, "test": 0.1}
    digest = int(hashlib.sha1(value.encode("utf-8")).hexdigest()[:8], 16)
    bucket = digest / 0xFFFFFFFF
    if bucket < ratios.get("train", 0.8):
        return "train"
    if bucket < ratios.get("train", 0.8) + ratios.get("val", 0.1):
        return "val"
    return "test"


def mask_component_boxes(mask_path: Path, min_area: int) -> list[tuple[float, float, float, float]]:
    mask = Image.open(mask_path).convert("L")
    width, height = mask.size
    if width <= 0 or height <= 0:
        return []

    if cv2 is None:
        xs = []
        ys = []
        pixels = mask.load()
        for y in range(height):
            for x in range(width):
                if pixels[x, y] > 0:
                    xs.append(x)
                    ys.append(y)
        if len(xs) < min_area:
            return []
        xmin, xmax = min(xs), max(xs)
        ymin, ymax = min(ys), max(ys)
        return [((xmin + xmax + 1) / 2 / width, (ymin + ymax + 1) / 2 / height, (xmax - xmin + 1) / width, (ymax - ymin + 1) / height)]

    import numpy as np

    binary = (np.array(mask) > 0).astype("uint8")
    component_count, _labels, stats, _centroids = cv2.connectedComponentsWithStats(binary, connectivity=8)
    boxes = []
    for component_id in range(1, component_count):
        x, y, w, h, area = stats[component_id]
        if area < min_area or w <= 1 or h <= 1:
            continue
        boxes.append(((x + w / 2) / width, (y + h / 2) / height, w / width, h / height))
    return boxes


def add_mask_source(
    *,
    source_name: str,
    image_root: Path,
    mask_root: Path,
    target_class_id: int,
    out_root: Path,
    copy_images: bool,
    stats: dict,
    min_area: int = 20,
) -> None:
    source_stats = stats["sources"][source_name]
    images = sorted(p for p in image_root.rglob("*") if p.suffix.lower() in IMAGE_EXTS)
    masks = {p.stem: p for p in mask_root.rglob("*") if p.suffix.lower() in IMAGE_EXTS}
    for image_path in images:
        source_stats["seen_images"] += 1
        mask_path = masks.get(image_path.stem)
        if mask_path is None:
            source_stats["missing_masks"] += 1
            continue
        boxes = mask_component_boxes(mask_path, min_area)
        if not boxes:
            source_stats["empty_after_filter"] += 1
            continue
        split = deterministic_split(f"{source_name}:{image_path.stem}")
        remapped = [
            f"{target_class_id} {x:.6f} {y:.6f} {w:.6f} {h:.6f}"
            for x, y, w, h in boxes
        ]
        dst_image = unique_destination(out_root, split, source_name, image_path)
        dst_label = out_root / "labels" / split / dst_image.with_suffix(".txt").name
        link_or_copy(image_path, dst_image, copy_images)
        dst_label.parent.mkdir(parents=True, exist_ok=True)
        dst_label.write_text("\n".join(remapped) + "\n", encoding="utf-8")
        source_stats["kept_images"] += 1
        source_stats["kept_boxes"] += len(remapped)
        stats["images_per_split"][split] += 1
        stats["boxes_per_split"][split] += len(remapped)
        stats["boxes_per_class"][str(target_class_id)] += len(remapped)
        stats["image_manifest"].append({
            "split": split,
            "source_name": source_name,
            "source_type": "mask",
            "storage": "copy" if copy_images else "symlink",
            "source_image": str(image_path),
            "output_image": str(dst_image),
            "output_label": str(dst_label),
            "class_ids": str(target_class_id),
            "box_count": str(len(remapped)),
        })


def image_for_label(out_root: Path, split: str, label_path: Path) -> Path | None:
    image_dir = out_root / "images" / split
    for suffix in IMAGE_EXTS:
        candidate = image_dir / label_path.with_suffix(suffix).name
        if candidate.exists() or candidate.is_symlink():
            return candidate
    return None


def label_classes(label_path: Path) -> set[int]:
    classes = set()
    for raw in label_path.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if line:
            classes.add(int(line.split()[0]))
    return classes


def balance_train_split(out_root: Path, min_images_per_class: int, copy_images: bool, stats: dict) -> None:
    if min_images_per_class <= 0:
        return

    label_dir = out_root / "labels" / "train"
    train_labels = sorted(label_dir.glob("*.txt"))
    class_to_labels: dict[int, list[Path]] = defaultdict(list)
    label_to_classes: dict[Path, set[int]] = {}
    for label_path in train_labels:
        classes = label_classes(label_path)
        label_to_classes[label_path] = classes
        for class_id in classes:
            class_to_labels[class_id].append(label_path)

    image_counts = Counter({str(class_id): len(paths) for class_id, paths in class_to_labels.items()})
    source_stats = stats["sources"]["train-balance"]
    duplicate_index = 0
    for class_id in sorted(class_to_labels):
        candidates = class_to_labels[class_id]
        if not candidates:
            continue
        while image_counts[str(class_id)] < min_images_per_class:
            source_label = candidates[duplicate_index % len(candidates)]
            source_image = image_for_label(out_root, "train", source_label)
            duplicate_index += 1
            if source_image is None:
                source_stats["missing_images"] += 1
                continue

            dst_image = out_root / "images" / "train" / f"balance_c{class_id:02d}_{duplicate_index:05d}_{source_image.name}"
            dst_label = out_root / "labels" / "train" / dst_image.with_suffix(".txt").name
            link_or_copy(source_image, dst_image, copy_images)
            shutil.copy2(source_label, dst_label)

            duplicated_classes = label_to_classes[source_label]
            for duplicated_class_id in duplicated_classes:
                image_counts[str(duplicated_class_id)] += 1
            for old_class_id, _coords in parse_label_lines(source_label):
                stats["boxes_per_class"][str(old_class_id)] += 1
                stats["boxes_per_split"]["train"] += 1
            stats["images_per_split"]["train"] += 1
            source_stats["kept_images"] += 1
            stats["image_manifest"].append({
                "split": "train",
                "source_name": "train-balance",
                "source_type": "balance_duplicate",
                "storage": "copy" if copy_images else "symlink",
                "source_image": str(source_image),
                "output_image": str(dst_image),
                "output_label": str(dst_label),
                "class_ids": ";".join(str(class_id) for class_id in sorted(duplicated_classes)),
                "box_count": str(len(parse_label_lines(source_label))),
            })


def quality_thresholds(config: dict) -> dict:
    thresholds = dict(DEFAULT_QUALITY_THRESHOLDS)
    thresholds.update(config.get("quality_thresholds", {}))
    return thresholds


def class_quality_status(row: dict, thresholds: dict) -> tuple[str, str]:
    train_images = int(row["train_images"])
    val_images = int(row["val_images"])
    test_images = int(row["test_images"])
    if train_images < thresholds["critical_train_images"]:
        return "critical", "too few real train images for reliable detection"
    if val_images < thresholds["min_val_images"] or test_images < thresholds["min_test_images"]:
        return "missing_val_or_test", "validation/test split is too small to trust metrics"
    if train_images < thresholds["low_train_images"]:
        return "critical", "train split is still very small"
    if train_images < thresholds["ready_train_images"]:
        return "low", "usable, but more annotated images would likely improve recall"
    return "ready", "enough real train images for a first practical model"


def write_balance_summary(metadata_dir: Path, class_rows: list[dict], thresholds: dict) -> None:
    summary_rows = []
    for row in class_rows:
        status, recommendation = class_quality_status(row, thresholds)
        summary_rows.append({
            **row,
            "status": status,
            "recommendation": recommendation,
            "target_train_images": thresholds["ready_train_images"],
            "additional_train_images_needed": max(0, thresholds["ready_train_images"] - int(row["train_images"])),
        })

    fieldnames = [
        "id",
        "key",
        "name",
        "vi_name",
        "crop_group",
        "status",
        "recommendation",
        "train_images",
        "train_boxes",
        "val_images",
        "val_boxes",
        "test_images",
        "test_boxes",
        "boxes",
        "target_train_images",
        "additional_train_images_needed",
    ]
    with (metadata_dir / "balance_report.csv").open("w", encoding="utf-8", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(summary_rows)

    counts = Counter(row["status"] for row in summary_rows)
    weak_rows = [row for row in summary_rows if row["status"] != "ready"]
    weak_rows.sort(key=lambda row: (int(row["train_images"]), int(row["boxes"])))

    lines = [
        "# Dataset Balance Report",
        "",
        f"- Ready classes: {counts.get('ready', 0)}",
        f"- Low classes: {counts.get('low', 0)}",
        f"- Critical classes: {counts.get('critical', 0)}",
        f"- Missing validation/test coverage: {counts.get('missing_val_or_test', 0)}",
        "",
        "Thresholds:",
        f"- ready: at least {thresholds['ready_train_images']} train images",
        f"- low: {thresholds['low_train_images']} to {thresholds['ready_train_images'] - 1} train images",
        f"- critical: below {thresholds['low_train_images']} train images, or below {thresholds['critical_train_images']} train images immediately",
        f"- validation/test warning: below {thresholds['min_val_images']} images in either split",
        "",
        "## Classes To Improve First",
        "",
        "| id | key | train images | val | test | status | additional train images needed |",
        "|---:|---|---:|---:|---:|---|---:|",
    ]
    for row in weak_rows:
        lines.append(
            f"| {row['id']} | {row['key']} | {row['train_images']} | {row['val_images']} | "
            f"{row['test_images']} | {row['status']} | {row['additional_train_images_needed']} |"
        )
    (metadata_dir / "balance_report.md").write_text("\n".join(lines) + "\n", encoding="utf-8")


def write_reports(out_root: Path, config: dict, key_to_id: dict[str, int], stats: dict) -> None:
    metadata_dir = out_root / "metadata"
    metadata_dir.mkdir(parents=True, exist_ok=True)
    thresholds = quality_thresholds(config)
    class_rows = []
    for target in config["targets"]:
        class_id = key_to_id[target["key"]]
        split_images = {}
        split_boxes = {}
        for split in SPLITS:
            labels = sorted((out_root / "labels" / split).glob("*.txt"))
            image_count = 0
            box_count = 0
            for label_path in labels:
                has_class = False
                for old_class_id, _coords in parse_label_lines(label_path):
                    if old_class_id == class_id:
                        box_count += 1
                        has_class = True
                if has_class:
                    image_count += 1
            split_images[split] = image_count
            split_boxes[split] = box_count
        class_rows.append({
            "id": class_id,
            "key": target["key"],
            "name": target["name"],
            "vi_name": target.get("vi_name", ""),
            "crop_group": target.get("crop_group", ""),
            "boxes": stats["boxes_per_class"].get(str(class_id), 0),
            "train_images": split_images["train"],
            "train_boxes": split_boxes["train"],
            "val_images": split_images["val"],
            "val_boxes": split_boxes["val"],
            "test_images": split_images["test"],
            "test_boxes": split_boxes["test"],
        })
    with (metadata_dir / "class_map.csv").open("w", encoding="utf-8", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=class_rows[0].keys())
        writer.writeheader()
        writer.writerows(class_rows)
    manifest_fieldnames = [
        "split",
        "source_name",
        "source_type",
        "storage",
        "source_image",
        "output_image",
        "output_label",
        "class_ids",
        "box_count",
    ]
    with (metadata_dir / "image_manifest.csv").open("w", encoding="utf-8", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=manifest_fieldnames)
        writer.writeheader()
        writer.writerows(stats["image_manifest"])
    serializable = json.loads(json.dumps(stats))
    (metadata_dir / "build_stats.json").write_text(json.dumps(serializable, indent=2), encoding="utf-8")
    write_balance_summary(metadata_dir, class_rows, thresholds)


def main() -> None:
    parser = argparse.ArgumentParser(description="Build a practical Vietnam agriculture YOLO dataset.")
    parser.add_argument("--config", default="configs/vietnam-practical-groups.yaml")
    parser.add_argument("--ip102-root", default="datasets/ip102-yolo")
    parser.add_argument("--output", default="datasets/vietnam-practical-yolo")
    parser.add_argument("--data-yaml", default="configs/vietnam-practical-yolo.yaml")
    parser.add_argument("--copy-images", action="store_true", help="Copy images instead of symlinking.")
    parser.add_argument("--include-external", action="store_true", help="Merge compatible external YOLO sources listed in config.")
    parser.add_argument("--min-train-images-per-class", type=int, default=0, help="Oversample rare train classes up to this many images.")
    args = parser.parse_args()

    config = load_yaml(Path(args.config))
    out_root = Path(args.output)
    ip102_root = Path(args.ip102_root)
    key_to_id = {target["key"]: idx for idx, target in enumerate(config["targets"])}
    names = [target["name"] for target in config["targets"]]

    clear_output(out_root)
    stats = {
        "images_per_split": Counter(),
        "boxes_per_split": Counter(),
        "boxes_per_class": Counter(),
        "sources": defaultdict(lambda: Counter()),
        "image_manifest": [],
    }

    ip102_class_map = {
        int(old_id): key_to_id[target_key]
        for old_id, target_key in config["ip102"]["class_map"].items()
        if target_key in key_to_id
    }
    add_source(
        source_name="ip102",
        source_root=ip102_root,
        class_map=ip102_class_map,
        out_root=out_root,
        copy_images=args.copy_images,
        stats=stats,
    )

    if args.include_external:
        for source in config.get("external_yolo_sources", []):
            source_root = Path(source["root"])
            if not source_root.exists():
                print(f"Skipping external source {source['name']}: {source_root} does not exist")
                continue
            source_names = load_source_names(source_root)
            external_map = {}
            for old_id, class_name in source_names.items():
                target_key = source["class_map"].get(class_name)
                if target_key in key_to_id:
                    external_map[old_id] = key_to_id[target_key]
            add_source(
                source_name=source["name"],
                source_root=source_root,
                class_map=external_map,
                out_root=out_root,
                copy_images=args.copy_images,
                stats=stats,
            )
        for source in config.get("external_mask_sources", []):
            target_key = source["target_key"]
            if target_key not in key_to_id:
                print(f"Skipping mask source {source['name']}: unknown target {target_key}")
                continue
            image_root = Path(source["image_root"])
            mask_root = Path(source["mask_root"])
            if not image_root.exists() or not mask_root.exists():
                print(f"Skipping mask source {source['name']}: image or mask root does not exist")
                continue
            add_mask_source(
                source_name=source["name"],
                image_root=image_root,
                mask_root=mask_root,
                target_class_id=key_to_id[target_key],
                out_root=out_root,
                copy_images=args.copy_images,
                stats=stats,
                min_area=int(source.get("min_area", 20)),
            )

    balance_train_split(out_root, args.min_train_images_per_class, args.copy_images, stats)
    write_data_yaml(out_root, names, Path(args.data_yaml))
    write_reports(out_root, config, key_to_id, stats)
    print(f"Wrote {out_root}")
    print(f"Wrote {args.data_yaml}")
    for split in SPLITS:
        print(f"{split}: {stats['images_per_split'][split]} images, {stats['boxes_per_split'][split]} boxes")
    non_empty = sum(1 for idx in range(len(names)) if stats["boxes_per_class"].get(str(idx), 0) > 0)
    print(f"classes with boxes: {non_empty}/{len(names)}")


if __name__ == "__main__":
    main()
