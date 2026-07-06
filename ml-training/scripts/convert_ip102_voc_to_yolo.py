#!/usr/bin/env python3
import argparse
import hashlib
import shutil
import xml.etree.ElementTree as ET
from pathlib import Path

IMAGE_EXTS = [".jpg", ".jpeg", ".png", ".bmp", ".webp"]


def read_ids(path: Path) -> list[str]:
    return [line.strip().split()[0] for line in path.read_text(encoding="utf-8").splitlines() if line.strip()]


def stable_val_member(image_id: str, val_ratio: float) -> bool:
    digest = hashlib.sha1(image_id.encode("utf-8")).hexdigest()
    bucket = int(digest[:8], 16) / 0xFFFFFFFF
    return bucket < val_ratio


def find_image(image_dir: Path, image_id: str) -> Path | None:
    for ext in IMAGE_EXTS:
        candidate = image_dir / f"{image_id}{ext}"
        if candidate.exists():
            return candidate
    # Official XML filenames omit extension, but keep this fallback for mirrors.
    matches = list(image_dir.glob(f"{image_id}.*"))
    return matches[0] if matches else None


def voc_to_yolo(xml_path: Path) -> tuple[str, list[str]]:
    tree = ET.parse(xml_path)
    root = tree.getroot()
    width = float(root.findtext("size/width", "0"))
    height = float(root.findtext("size/height", "0"))
    if width <= 0 or height <= 0:
        raise ValueError(f"Invalid image size in {xml_path}")

    lines = []
    for obj in root.findall("object"):
        raw_name = (obj.findtext("name") or "").strip()
        class_id = int(raw_name)
        if class_id < 0 or class_id > 101:
            raise ValueError(f"Class id {raw_name} outside 0..101 in {xml_path}")
        box = obj.find("bndbox")
        xmin = float(box.findtext("xmin"))
        ymin = float(box.findtext("ymin"))
        xmax = float(box.findtext("xmax"))
        ymax = float(box.findtext("ymax"))
        xmin = max(0.0, min(xmin, width))
        xmax = max(0.0, min(xmax, width))
        ymin = max(0.0, min(ymin, height))
        ymax = max(0.0, min(ymax, height))
        bw = xmax - xmin
        bh = ymax - ymin
        if bw <= 0 or bh <= 0:
            continue
        x = (xmin + xmax) / 2.0 / width
        y = (ymin + ymax) / 2.0 / height
        w = bw / width
        h = bh / height
        lines.append(f"{class_id} {x:.6f} {y:.6f} {w:.6f} {h:.6f}")
    return root.findtext("filename", xml_path.stem).strip(), lines


def link_or_copy(src: Path, dst: Path, copy: bool) -> None:
    dst.parent.mkdir(parents=True, exist_ok=True)
    if dst.exists() or dst.is_symlink():
        dst.unlink()
    if copy:
        shutil.copy2(src, dst)
    else:
        dst.symlink_to(src.resolve())


def convert_split(ids: list[str], split: str, voc_root: Path, out_root: Path, copy_images: bool) -> dict:
    image_dir = voc_root / "JPEGImages"
    annotation_dir = voc_root / "Annotations"
    stats = {"ids": len(ids), "converted": 0, "missing_xml": 0, "missing_image": 0, "malformed_xml": 0, "empty_labels": 0}
    for image_id in ids:
        xml_path = annotation_dir / f"{image_id}.xml"
        image_path = find_image(image_dir, image_id)
        if not xml_path.exists():
            stats["missing_xml"] += 1
            continue
        if image_path is None:
            stats["missing_image"] += 1
            continue
        try:
            _, lines = voc_to_yolo(xml_path)
        except Exception as exc:
            print(f"Skipping malformed annotation {xml_path}: {exc}")
            stats["malformed_xml"] += 1
            continue
        if not lines:
            stats["empty_labels"] += 1
            continue
        out_image = out_root / "images" / split / image_path.name
        out_label = out_root / "labels" / split / f"{image_path.stem}.txt"
        link_or_copy(image_path, out_image, copy_images)
        out_label.parent.mkdir(parents=True, exist_ok=True)
        out_label.write_text("\n".join(lines) + "\n", encoding="utf-8")
        stats["converted"] += 1
    return stats


def main() -> None:
    parser = argparse.ArgumentParser(description="Convert official IP102 VOC2007 detection subset to YOLO format.")
    parser.add_argument("--voc-root", default="datasets/ip102-voc/VOC2007")
    parser.add_argument("--output", default="datasets/ip102-yolo")
    parser.add_argument("--val-ratio", type=float, default=0.10)
    parser.add_argument("--copy-images", action="store_true", help="Copy images instead of symlinking them.")
    args = parser.parse_args()

    voc_root = Path(args.voc_root)
    out_root = Path(args.output)
    split_dir = voc_root / "ImageSets" / "Main"
    trainval = read_ids(split_dir / "trainval.txt")
    test = read_ids(split_dir / "test.txt")
    train = [image_id for image_id in trainval if not stable_val_member(image_id, args.val_ratio)]
    val = [image_id for image_id in trainval if stable_val_member(image_id, args.val_ratio)]

    for split in ("train", "val", "test"):
        for sub in ("images", "labels"):
            path = out_root / sub / split
            if path.exists():
                shutil.rmtree(path)

    print(f"train ids: {len(train)}")
    print(f"val ids:   {len(val)}")
    print(f"test ids:  {len(test)}")
    for split, ids in (("train", train), ("val", val), ("test", test)):
        stats = convert_split(ids, split, voc_root, out_root, args.copy_images)
        print(split, stats)


if __name__ == "__main__":
    main()
