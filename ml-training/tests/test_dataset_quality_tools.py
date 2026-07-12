import csv
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

import yaml


ROOT = Path(__file__).resolve().parents[1]
SCRIPTS = ROOT / "scripts"


def write_file(path: Path, text: str = "") -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")


def read_csv(path: Path) -> list[dict[str, str]]:
    with path.open("r", encoding="utf-8", newline="") as f:
        return list(csv.DictReader(f))


class DatasetQualityToolsTest(unittest.TestCase):
    def test_audit_reports_weak_classes_and_box_quality(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            dataset = root / "dataset"
            for split in ("train", "val", "test"):
                write_file(dataset / "images" / split / "rare.jpg")
                write_file(dataset / "labels" / split / "rare.txt", "0 0.5 0.5 0.010 0.010\n")
            write_file(dataset / "images" / "train" / "common.jpg")
            write_file(dataset / "labels" / "train" / "common.txt", "1 0.5 0.5 0.990 0.990\n")

            data_yaml = root / "data.yaml"
            write_file(
                data_yaml,
                yaml.safe_dump({
                    "path": str(dataset),
                    "train": "images/train",
                    "val": "images/val",
                    "test": "images/test",
                    "names": {0: "rare pest", 1: "common pest"},
                }, sort_keys=False),
            )
            metrics_log = root / "train.log"
            write_file(metrics_log, "rare pest          1          1      0.1      0.2      0.3      0.4\n")

            out_dir = root / "audit"
            result = subprocess.run(
                [
                    sys.executable,
                    str(SCRIPTS / "audit_yolo_dataset_quality.py"),
                    "--data",
                    str(data_yaml),
                    "--metrics-log",
                    str(metrics_log),
                    "--output",
                    str(out_dir),
                    "--sample-count",
                    "1",
                    "--min-train-images",
                    "3",
                    "--min-val-images",
                    "2",
                    "--min-test-images",
                    "2",
                ],
                cwd=ROOT,
                text=True,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
            )

            self.assertEqual(result.returncode, 0, result.stderr)
            priority = read_csv(out_dir / "weak_class_priority.csv")
            self.assertEqual(priority[0]["name"], "common pest")
            self.assertIn("full-image-like-boxes", priority[0]["issues"])
            self.assertIn("low-train", priority[1]["issues"])

            audit_rows = read_csv(out_dir / "class_quality_report.csv")
            rare = next(row for row in audit_rows if row["name"] == "rare pest")
            self.assertEqual(rare["metric_mAP50-95"], "0.4")
            self.assertEqual(rare["tiny_box_ratio"], "1.0000")
            self.assertTrue((out_dir / "samples" / "rare_pest" / "rare.jpg").exists())

    def test_annotation_queue_can_filter_target_classes(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            source = root / "source"
            write_file(source / "ClassA" / "a.jpg")
            write_file(source / "ClassB" / "b.jpg")
            manifest = root / "candidates.yaml"
            write_file(
                manifest,
                yaml.safe_dump({
                    "candidate_sources": [{
                        "name": "fixture",
                        "local_root": str(source),
                        "decision": "annotation_queue_only",
                        "class_map": {"ClassA": "rice_thrips", "ClassB": "aphid"},
                        "review_map": {"FamilyA": ["rice_thrips"], "FamilyB": ["aphid"]},
                    }]
                }, sort_keys=False),
            )

            out_dir = root / "queue"
            result = subprocess.run(
                [
                    sys.executable,
                    str(SCRIPTS / "build_annotation_queue.py"),
                    "--candidates",
                    str(manifest),
                    "--output",
                    str(out_dir),
                    "--target-classes",
                    "rice_thrips",
                ],
                cwd=ROOT,
                text=True,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
            )

            self.assertEqual(result.returncode, 0, result.stderr)
            rows = read_csv(out_dir / "metadata" / "queue_manifest.csv")
            self.assertEqual([row["target_key"] for row in rows], ["rice_thrips"])
            self.assertTrue((out_dir / "images" / "rice_thrips").exists())
            self.assertFalse((out_dir / "images" / "aphid").exists())

    def test_prepare_roboflow_package_resolves_review_symlinks(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            queue = root / "queue"
            source = root / "source"
            real_delphacid = source / "delphacid.jpg"
            real_thrips = source / "thrips.jpg"
            write_file(real_delphacid, "delphacid image")
            write_file(real_thrips, "thrips image")
            queued_delphacid = queue / "images" / "_review" / "fixture" / "Delphacidae" / "delphacid.jpg"
            queued_thrips = queue / "images" / "_review" / "fixture" / "Thripidae" / "thrips.jpg"
            queued_delphacid.parent.mkdir(parents=True, exist_ok=True)
            queued_thrips.parent.mkdir(parents=True, exist_ok=True)
            queued_delphacid.symlink_to(real_delphacid)
            queued_thrips.symlink_to(real_thrips)
            write_file(
                queue / "metadata" / "review_manifest.csv",
                "\n".join([
                    "source_name,source_class,suggested_targets,source_path,queued_path,license,source_url",
                    f"fixture,Delphacidae,whitebacked_planthopper;small_brown_planthopper,{real_delphacid},images/_review/fixture/Delphacidae/delphacid.jpg,CC BY 4.0,https://example.com/delphacid",
                    f"fixture,Thripidae,rice_thrips;thrips,{real_thrips},{queued_thrips},CC BY 4.0,https://example.com/thrips",
                    "",
                ]),
            )

            out_dir = root / "roboflow"
            result = subprocess.run(
                [
                    sys.executable,
                    str(SCRIPTS / "prepare_roboflow_weakclass_package.py"),
                    "--queue",
                    str(queue),
                    "--output",
                    str(out_dir),
                    "--quota",
                    "Delphacidae=1,Thripidae=1",
                ],
                cwd=ROOT,
                text=True,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
            )

            self.assertEqual(result.returncode, 0, result.stderr)
            copied = out_dir / "images" / "Delphacidae" / "delphacid.jpg"
            self.assertTrue(copied.exists())
            self.assertFalse(copied.is_symlink())
            self.assertEqual(copied.read_text(encoding="utf-8"), "delphacid image")
            rows = read_csv(out_dir / "metadata" / "upload_manifest.csv")
            self.assertEqual([row["source_class"] for row in rows], ["Delphacidae", "Thripidae"])
            self.assertEqual(rows[0]["suggested_targets"], "whitebacked_planthopper;small_brown_planthopper")
            self.assertTrue((out_dir / "README.md").exists())

    def test_practical_builder_writes_image_manifest(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            source = root / "source"
            for split in ("train", "val", "test"):
                write_file(source / "images" / split / f"{split}.jpg")
                write_file(source / "labels" / split / f"{split}.txt", "0 0.5 0.5 0.2 0.2\n")

            config = root / "groups.yaml"
            write_file(
                config,
                yaml.safe_dump({
                    "targets": [{"key": "rare", "name": "rare pest"}],
                    "quality_thresholds": {"ready_train_images": 2, "low_train_images": 1},
                    "ip102": {"class_map": {0: "rare"}},
                }, sort_keys=False),
            )
            out_dir = root / "out"
            data_yaml = root / "out.yaml"
            result = subprocess.run(
                [
                    sys.executable,
                    str(SCRIPTS / "build_practical_yolo_dataset.py"),
                    "--config",
                    str(config),
                    "--ip102-root",
                    str(source),
                    "--output",
                    str(out_dir),
                    "--data-yaml",
                    str(data_yaml),
                ],
                cwd=ROOT,
                text=True,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
            )

            self.assertEqual(result.returncode, 0, result.stderr)
            rows = read_csv(out_dir / "metadata" / "image_manifest.csv")
            self.assertEqual(len(rows), 3)
            self.assertEqual(rows[0]["source_name"], "ip102")
            self.assertEqual(rows[0]["storage"], "symlink")
            self.assertEqual(rows[0]["class_ids"], "0")


if __name__ == "__main__":
    unittest.main()
