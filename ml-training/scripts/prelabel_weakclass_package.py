#!/usr/bin/env python3
import argparse
import csv
import shutil
from collections import Counter
from pathlib import Path

import yaml
from ultralytics import YOLO


IMAGE_EXTS = {".jpg", ".jpeg", ".png", ".bmp", ".webp"}
EXPORT_NAMES = [
    "rice_thrips",
    "thrips",
    "whitebacked_planthopper",
    "small_brown_planthopper",
    "hairy_caterpillar",
    "cabbage_caterpillar",
]
MODEL_TO_EXPORT = {
    "rice thrips": "rice_thrips",
    "thrips": "thrips",
    "white backed planthopper": "whitebacked_planthopper",
    "small brown planthopper": "small_brown_planthopper",
    "hairy caterpillar": "hairy_caterpillar",
    "cabbage caterpillar": "cabbage_caterpillar",
}


def load_yaml(path: Path) -> dict:
    with path.open("r", encoding="utf-8") as f:
        return yaml.safe_load(f)


def model_names(data_yaml: Path) -> dict[int, str]:
    raw = load_yaml(data_yaml)["names"]
    if isinstance(raw, dict):
        return {int(key): str(value) for key, value in raw.items()}
    return {idx: str(value) for idx, value in enumerate(raw)}


def iter_images(images_root: Path) -> list[Path]:
    return sorted(path for path in images_root.rglob("*") if path.suffix.lower() in IMAGE_EXTS)


def write_data_yaml(out_root: Path) -> None:
    lines = [
        f"path: {out_root.resolve()}",
        "train: train/images",
        "val: valid/images",
        "test: test/images",
        "names:",
    ]
    for idx, name in enumerate(EXPORT_NAMES):
        lines.append(f"  {idx}: {name}")
    (out_root / "data.yaml").write_text("\n".join(lines) + "\n", encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser(description="Pre-label the v4 weak-class Roboflow package with the current YOLO model for human review.")
    parser.add_argument("--weights", required=True, help="YOLO .pt weights to use for draft labels.")
    parser.add_argument("--data", default="configs/vietnam-practical-yolo.yaml", help="Training data.yaml that defines model class names.")
    parser.add_argument("--images-root", default="datasets/roboflow-v4-weakclass-upload/images", help="Prepared Roboflow upload image folders.")
    parser.add_argument("--output", default="datasets/roboflow-v4-weakclass-prelabel-yolo", help="YOLO export folder for Roboflow import.")
    parser.add_argument("--conf", type=float, default=0.12, help="Low confidence threshold for review-oriented prelabels.")
    parser.add_argument("--imgsz", type=int, default=896, help="Inference image size.")
    args = parser.parse_args()

    names = model_names(Path(args.data))
    export_id = {name: idx for idx, name in enumerate(EXPORT_NAMES)}
    out_root = Path(args.output)
    if out_root.exists():
        shutil.rmtree(out_root)
    image_out = out_root / "train" / "images"
    label_out = out_root / "train" / "labels"
    metadata_out = out_root / "metadata"
    image_out.mkdir(parents=True, exist_ok=True)
    label_out.mkdir(parents=True, exist_ok=True)
    metadata_out.mkdir(parents=True, exist_ok=True)

    model = YOLO(args.weights)
    rows = []
    counts: Counter = Counter()
    images = iter_images(Path(args.images_root))

    for image in images:
        dst_image = image_out / image.name
        shutil.copy2(image, dst_image)
        result = model.predict(str(image), conf=args.conf, imgsz=args.imgsz, verbose=False)[0]
        label_lines = []
        for box in result.boxes:
            model_class = names.get(int(box.cls.item()), "")
            export_name = MODEL_TO_EXPORT.get(model_class)
            if export_name is None:
                continue
            x_center, y_center, width, height = box.xywhn[0].tolist()
            confidence = float(box.conf.item())
            label_lines.append(
                f"{export_id[export_name]} {x_center:.6f} {y_center:.6f} {width:.6f} {height:.6f}"
            )
            counts[export_name] += 1
            rows.append({
                "image": str(image),
                "output_image": str(dst_image),
                "output_label": str(label_out / f"{image.stem}.txt"),
                "model_class": model_class,
                "review_class": export_name,
                "confidence": f"{confidence:.6f}",
            })
        (label_out / f"{image.stem}.txt").write_text("\n".join(label_lines) + ("\n" if label_lines else ""), encoding="utf-8")

    write_data_yaml(out_root)
    with (metadata_out / "prelabel_manifest.csv").open("w", encoding="utf-8", newline="") as f:
        fieldnames = ["image", "output_image", "output_label", "model_class", "review_class", "confidence"]
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(rows)

    print(f"Wrote {out_root}")
    print(f"images: {len(images)}")
    print(f"draft boxes: {sum(counts.values())}")
    for name, count in sorted(counts.items()):
        print(f"{name}: {count}")


if __name__ == "__main__":
    main()
