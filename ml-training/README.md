# BugScanner YOLO Training

This folder keeps model training separate from the Kotlin app.

Tracked:

- `scripts/`: reproducible training/export/inspection helpers
- `configs/`: generated YOLO dataset config files
- `requirements.txt`: Python dependencies

Ignored:

- `datasets/`: downloaded IP102 images and labels
- `runs/`: Ultralytics training outputs
- `weights/`: exported model artifacts

## Dataset

Use the official IP102 source first:

- Original repo: `https://github.com/xpwu95/IP102`
- Dataset paper: `IP102: A Large-Scale Benchmark Dataset for Insect Pest Recognition`, CVPR 2019
- Official README says IP102 has 75k+ images, 102 classes, and about 19k images annotated with bounding boxes for object detection.

Expected local layout after downloading/converting the bbox subset:

```text
ml-training/datasets/ip102-yolo/
  images/
    train/
    val/
    test/
  labels/
    train/
    val/
    test/
```

Each YOLO label file must contain normalized bounding boxes:

```text
class_id x_center y_center width height
```



## Practical Vietnam Pest Dataset

For the app-facing model, use the practical group-level dataset when exact species-level IP102 classes are too noisy or not useful for Vietnamese agriculture users. It merges only sources with real object-localization labels:

- official IP102 detection subset with real bounding boxes
- `nafayunnoor/rice-pest-datasets-for-detection`, a Roboflow YOLO export for common rice pests
- compatible YOLO exports for mango pests, red chili pests, and aphids where the class mapping is safe
- aphid segmentation masks converted into bounding boxes
- reviewed Roboflow Universe exports for rice gall midge, rice leaf caterpillar, fruit fly, spider mite, longhorn beetle, flea beetle, cabbage pests, and leafminer where the class mapping is safe enough

Do not use `eljazouly/ip102-coco-annotations` for object detection training. Inspection showed its boxes are full-image pseudo boxes, not real insect locations.

Do not train on classification-only folders directly. Classification images go into `datasets/labeling-queue/` and must be manually annotated with real boxes first.

Do not merge generic thrips into `rice_thrips`, or unlabeled planthopper class ids into specific planthopper classes. Keep those sources in review until their taxonomy/class-id mapping is verified.

Build and inspect the practical dataset:

```bash
python scripts/build_practical_yolo_dataset.py \
  --config configs/vietnam-practical-groups.yaml \
  --ip102-root datasets/ip102-yolo \
  --output datasets/vietnam-practical-yolo \
  --data-yaml configs/vietnam-practical-yolo.yaml \
  --include-external

python scripts/inspect_yolo_dataset.py --data configs/vietnam-practical-yolo.yaml
```

Current rebuilt dataset check after adding reviewed Roboflow sources: 33/33 classes, 25,413 train images / 48,485 boxes, 4,364 val images / 8,774 boxes, and 4,210 test images / 6,785 boxes. Full-image-like boxes remain low at about 0.2-0.3%, so this is suitable for YOLO detection experiments. See:

```text
datasets/vietnam-practical-yolo/metadata/class_map.csv
datasets/vietnam-practical-yolo/metadata/image_manifest.csv
datasets/vietnam-practical-yolo/metadata/balance_report.csv
datasets/vietnam-practical-yolo/metadata/balance_report.md
datasets/vietnam-practical-yolo/metadata/build_stats.json
```

Audit weak classes and box quality before creating a new Kaggle dataset version:

```bash
python scripts/audit_yolo_dataset_quality.py \
  --data configs/vietnam-practical-yolo.yaml \
  --metrics-log output-vietnamese-2/yolo11m-vn-practical-896-train.log \
  --output datasets/vietnam-practical-yolo/metadata/quality-audit
```

Build a manual annotation queue from classification-only candidate sources:

```bash
python scripts/build_annotation_queue.py \
  --candidates configs/vietnam-candidate-sources.yaml \
  --output datasets/labeling-queue \
  --max-per-class 500
```

To focus the queue on the current v3 weak classes:

```bash
python scripts/build_annotation_queue.py \
  --candidates configs/vietnam-candidate-sources.yaml \
  --output datasets/labeling-queue \
  --target-classes rice_thrips,small_brown_planthopper,whitebacked_planthopper,hairy_caterpillar,cabbage_caterpillar \
  --max-per-class 500
```

The queue currently includes 370 clean `rice_gall_midge` candidate images plus review folders for broader rice-pest families such as `Delphacidae`, `Thripidae`, `Phlaeothripidae`, `Crambidae`, and `Noctuidae`. These review images are useful, but they need taxonomy review and manual boxes before they can improve training.

## Local Machine Notes

Current checked machine:

```text
GPU: NVIDIA GeForce RTX 4060 Laptop GPU, 8GB VRAM
Free disk before dataset download: about 31GB
```

For this project we only need the IP102 bbox/detection subset, around 19k annotated images. The remaining classification-only images are not used for YOLO detection unless they are later given bounding boxes. Avoid keeping both a full archive and extracted full dataset on disk at the same time.

Estimated storage:

```text
IP102 bbox subset only: likely a few GB, depending on image resolution/compression
Training runs/checkpoints: usually 1-5GB per experiment
Full IP102 75k image package: much larger and not preferred on this disk
```

## Setup

```bash
cd ml-training
conda create -n bug-yolo python=3.11 -y
conda activate bug-yolo
pip install -r requirements.txt
yolo checks
```

## Convert Official VOC Detection Subset

After downloading/extracting the official `Detection/VOC2007` files, convert them to YOLO format. By default this creates symlinks to avoid duplicating image data.

```bash
python scripts/convert_ip102_voc_to_yolo.py \
  --voc-root datasets/ip102-voc/VOC2007 \
  --output datasets/ip102-yolo \
  --val-ratio 0.10
```

The official detection XML class ids are already `0..101`, matching the app label order. The display-only `classes.txt` file is numbered `1..102`, so do not subtract 1 during conversion.

## Generate `data.yaml`

This reads the class order from the app's `YoloConstants.kt`, so training output indices match the mobile/web app.

```bash
python scripts/make_data_yaml.py \
  --dataset-root datasets/ip102-yolo \
  --output configs/ip102-app-order.yaml
```

## Inspect Dataset

Run this before training. It catches missing labels, invalid coordinates, class ids outside `0..101`, and suspicious full-image pseudo boxes.

```bash
python scripts/inspect_yolo_dataset.py --data configs/ip102-app-order.yaml
```

## Train

Start with YOLO11n for mobile:

```bash
python scripts/train_yolo.py \
  --model yolo11n.pt \
  --data configs/ip102-app-order.yaml \
  --epochs 80 \
  --batch 8 \
  --imgsz 640
```

If VRAM is fine, try `--batch 16`. For a higher-accuracy comparison, train `yolo11s.pt` with a smaller batch.

## Export

Android:

```bash
python scripts/export_yolo.py \
  --weights runs/train/yolo11n-ip102/weights/best.pt \
  --format tflite
```

Web:

```bash
python scripts/export_yolo.py \
  --weights runs/train/yolo11n-ip102/weights/best.pt \
  --format tfjs
```

After export, copy the generated artifacts into the app only after validating them.
