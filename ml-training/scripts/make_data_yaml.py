#!/usr/bin/env python3
import argparse
import re
from pathlib import Path



def repo_root() -> Path:
    return Path(__file__).resolve().parents[2]


def load_app_labels() -> list[str]:
    constants = repo_root() / "src/composeApp/src/commonMain/kotlin/hcmus/bugscanner/ml/YoloConstants.kt"
    text = constants.read_text(encoding="utf-8")
    match = re.search(r"val LABELS = arrayOf\((.*?)\n\s*\)", text, re.S)
    if not match:
        raise RuntimeError(f"Could not find LABELS array in {constants}")
    labels = re.findall(r'"([^"]+)"', match.group(1))
    if len(labels) != 102:
        raise RuntimeError(f"Expected 102 labels, found {len(labels)}")
    return labels


def main() -> None:
    parser = argparse.ArgumentParser(description="Generate YOLO data.yaml using BugScanner app class order.")
    parser.add_argument("--dataset-root", required=True, help="Dataset root, e.g. datasets/ip102-yolo")
    parser.add_argument("--output", required=True, help="Output YAML path")
    args = parser.parse_args()

    root = Path(args.dataset_root)
    labels = load_app_labels()
    lines = [
        f"path: {root.resolve()}",
        "train: images/train",
        "val: images/val",
        "test: images/test",
        "names:",
    ]
    for i, name in enumerate(labels):
        escaped = name.replace("'", "''")
        lines.append(f"  {i}: '{escaped}'")

    output = Path(args.output)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"Wrote {output} with {len(labels)} classes")


if __name__ == "__main__":
    main()
