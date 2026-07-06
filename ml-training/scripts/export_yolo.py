#!/usr/bin/env python3
import argparse
import subprocess


def main() -> None:
    parser = argparse.ArgumentParser(description="Export trained YOLO weights for Android or web.")
    parser.add_argument("--weights", required=True)
    parser.add_argument("--format", choices=["tflite", "tfjs", "onnx"], required=True)
    parser.add_argument("--imgsz", type=int, default=640)
    parser.add_argument("--int8", action="store_true", help="Enable INT8 quantization where supported.")
    args = parser.parse_args()

    cmd = [
        "yolo",
        "export",
        f"model={args.weights}",
        f"format={args.format}",
        f"imgsz={args.imgsz}",
    ]
    if args.int8:
        cmd.append("int8=True")
    print(" ".join(cmd))
    subprocess.run(cmd, check=True)


if __name__ == "__main__":
    main()
