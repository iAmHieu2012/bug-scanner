#!/usr/bin/env python3
import argparse
import subprocess
from pathlib import Path


def main() -> None:
    parser = argparse.ArgumentParser(description="Train YOLO on the IP102 detection subset.")
    parser.add_argument("--model", default="yolo11n.pt")
    parser.add_argument("--data", default="configs/ip102-app-order.yaml")
    parser.add_argument("--epochs", type=int, default=80)
    parser.add_argument("--batch", type=int, default=8)
    parser.add_argument("--imgsz", type=int, default=640)
    parser.add_argument("--device", default="0")
    parser.add_argument("--name", default=None)
    args = parser.parse_args()

    model_stem = Path(args.model).stem
    name = args.name or f"{model_stem}-ip102"
    cmd = [
        "yolo",
        "detect",
        "train",
        f"model={args.model}",
        f"data={args.data}",
        f"epochs={args.epochs}",
        f"batch={args.batch}",
        f"imgsz={args.imgsz}",
        f"device={args.device}",
        "project=runs/train",
        f"name={name}",
        "exist_ok=True",
    ]
    print(" ".join(cmd))
    subprocess.run(cmd, check=True)


if __name__ == "__main__":
    main()
