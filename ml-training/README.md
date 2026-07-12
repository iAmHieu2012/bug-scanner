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
- reviewed Roboflow Universe exports for rice gall midge, rice leaf caterpillar, fruit fly, spider mite, thrips, longhorn beetle, flea beetle, cabbage pests, and leafminer where the class mapping is safe enough
- a narrow Roboflow planthopper candidate source for `whitebacked_planthopper`; this source uses numeric class ids, so keep the mapping under review before using it for a final model

Do not use `eljazouly/ip102-coco-annotations` for object detection training. Inspection showed its boxes are full-image pseudo boxes, not real insect locations.

Do not train on classification-only folders directly. Classification images go into `datasets/labeling-queue/` and must be manually annotated with real boxes first.

Do not merge generic thrips into `rice_thrips`, or unlabeled planthopper class ids into specific planthopper classes. Keep those sources in review until their taxonomy/class-id mapping is verified.

The previous weak-class review queue from `agricultural-pest-detection-11-species` is not recommended for v4 replacement data. Manual inspection found noisy classification-style images, text overlays, and collage images. Prefer clean detection exports with real boxes before spending review time on that queue.

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

Current rebuilt dataset check after adding reviewed Roboflow sources: 33/33 classes, 27,321 train images / 52,240 boxes, 4,669 val images / 9,403 boxes, and 4,385 test images / 7,177 boxes. Full-image-like boxes remain low at about 0.2-0.3%, so this is suitable for YOLO detection experiments. See:

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

## V4 Weak-Class Data

V4 should focus on clean object-detection data for the classes that stayed weak after v3:

- `rice_thrips`
- `thrips`
- `whitebacked_planthopper`
- `small_brown_planthopper`
- `hairy_caterpillar`
- `cabbage_caterpillar`

Use these clean boxed sources first:

- `roboflow-thrips`: YOLO boxes from `https://universe.roboflow.com/disease-74h5a/thrips-ecqs7/dataset/2`, mapped only to generic `thrips`
- `roboflow-planthopper-wbph-candidate`: YOLO boxes from `https://universe.roboflow.com/wang-pingan-hf2it/planthopper-8oogf/dataset/1`, mapped only from numeric source class `'1'` to `whitebacked_planthopper`
- existing clean weak-class support already included in the practical config: `roboflow-spider-mite`, `roboflow-cabbage-pest`, and aphid mask boxes
- manually reviewed Roboflow exports under `datasets/manual/extracted/`:
  - `rice_pest_detection_6_v1i_yolov11`, mapped only from `rice thrips` to `rice_thrips`
  - `small_brown_plant_hopper_yolov11`, mapped only from `small_brown_plant_hopper` to `small_brown_planthopper`
  - `Pest_detection_thesis_yolov11`, mapped only from `Red_Hairy_Catterpillar_C` to `hairy_caterpillar`
  - `Pest_Detection_v4i_yolov11`, mapped only from `Black hairy caterpillar` to `hairy_caterpillar`

Do not map `roboflow-thrips` into `rice_thrips`. Do not map the planthopper export into `brown_planthopper` or `small_brown_planthopper` until its numeric class-id order has been independently verified.

Do not use `rice-pest-bb.v3-bb_augmented_320_3x` for v4 unless it is manually cleaned. Inspection showed mosaic/cutout augmentation, black squares, and mixed cropped patches. Do not use the downloaded YOLO-JD zip directly; the inspected archive contains images but no annotation files.

If clean boxed data is still missing for `rice_thrips`, `small_brown_planthopper`, or `hairy_caterpillar`, then use manual review/labeling. The old weak-class review queue is deprecated because inspection found noisy images. Rebuild it only if better candidate sources are added:

```bash
python scripts/prepare_roboflow_weakclass_package.py \
  --queue datasets/labeling-queue \
  --output datasets/roboflow-v4-weakclass-upload
```

Default package size is about 500 images:

- `Delphacidae`: 160 images for planthopper review
- `Thripidae`: 120 images for thrips review
- `Phlaeothripidae`: 120 images for thrips review
- `Noctuidae`: 100 images for hairy caterpillar review

Upload only reviewed, clean images from `datasets/roboflow-v4-weakclass-upload/images/` to Roboflow. Use exact class names when labeling:

```text
rice_thrips
thrips
whitebacked_planthopper
small_brown_planthopper
hairy_caterpillar
cabbage_caterpillar
```

Reject ambiguous images instead of forcing them into a class. Draw tight pest boxes only; do not draw full-image boxes.

Optionally create draft labels with the current v3 model before importing into Roboflow:

```bash
python scripts/prelabel_weakclass_package.py \
  --weights output-vietnamese-3/runs/train/yolo11m-vn-practical-896-v3-dataqa/weights/best.pt \
  --data configs/vietnam-practical-yolo.yaml \
  --images-root datasets/roboflow-v4-weakclass-upload/images \
  --output datasets/roboflow-v4-weakclass-prelabel-yolo
```

Import `datasets/roboflow-v4-weakclass-prelabel-yolo/` if you want to review/edit draft boxes instead of drawing every box from scratch. Treat all draft labels as untrusted and correct or delete them during review.

After Roboflow labeling, export as YOLOv8/YOLO format and place the export at:

```text
datasets/v4-weak-reviewed-yolo/
  data.yaml
  train/images/
  train/labels/
  valid/images/
  valid/labels/
  test/images/
  test/labels/
```

The practical dataset config already includes this reviewed source as `roboflow-v4-weak-reviewed`. Rebuild with external sources:

```bash
python scripts/build_practical_yolo_dataset.py \
  --config configs/vietnam-practical-groups.yaml \
  --ip102-root datasets/ip102-yolo \
  --output datasets/vietnam-practical-yolo \
  --data-yaml configs/vietnam-practical-yolo.yaml \
  --include-external

python scripts/inspect_yolo_dataset.py --data configs/vietnam-practical-yolo.yaml
```

Then upload a new Kaggle dataset version and train with run name:

```text
yolo11m-vn-practical-896-v4-weakclass
```

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
