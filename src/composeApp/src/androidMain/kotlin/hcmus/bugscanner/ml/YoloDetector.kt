package hcmus.bugscanner.ml

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import hcmus.bugscanner.domain.model.DetectionResult
import hcmus.bugscanner.domain.model.FrameResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.Rot90Op
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class YoloDetector(context: Context, modelPath: String = YoloConstants.MODEL_PATH) {
    private var interpreter: Interpreter? = null

    private val _frameResult = MutableStateFlow(FrameResult(emptyList(), 0, 0))
    val frameResult: StateFlow<FrameResult> = _frameResult.asStateFlow()

    private var imageProcessor: ImageProcessor? = null
    private var lastRotation = -1
    private val tensorImage = TensorImage(DataType.FLOAT32)
    private val outputBuffer = TensorBuffer.createFixedSize(intArrayOf(1, 106, 16464), DataType.FLOAT32)

    init {
        val options = Interpreter.Options()
        val compatList = CompatibilityList()
        if (compatList.isDelegateSupportedOnThisDevice) {
            options.addDelegate(GpuDelegate(compatList.bestOptionsForThisDevice))
        } else {
            options.setNumThreads(4)
        }
        interpreter = Interpreter(loadModelFile(context, modelPath), options)
    }

    private fun loadModelFile(context: Context, modelPath: String): MappedByteBuffer {
        val fd = context.assets.openFd(modelPath)
        return FileInputStream(fd.fileDescriptor).channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
    }

    fun clearResult() {
        _frameResult.value = FrameResult(emptyList(), 0, 0)
    }
    fun analyze(inputBitmap: Bitmap, rotationDegrees: Int) {
        if (interpreter == null) return

        try {
            if (imageProcessor == null || rotationDegrees != lastRotation) {
                lastRotation = rotationDegrees
                imageProcessor = ImageProcessor.Builder()
                    .add(Rot90Op(rotationDegrees / 90))
                    .add(ResizeOp(YoloConstants.INPUT_SIZE, YoloConstants.INPUT_SIZE, ResizeOp.ResizeMethod.BILINEAR))
                    .add(NormalizeOp(0f, 255f))
                    .build()
            }

            val isRotated = rotationDegrees % 180 != 0
            val sourceW = if (isRotated) inputBitmap.height else inputBitmap.width
            val sourceH = if (isRotated) inputBitmap.width else inputBitmap.height

            tensorImage.load(inputBitmap)
            val processedImage = imageProcessor!!.process(tensorImage)

            interpreter?.run(processedImage.buffer, outputBuffer.buffer.rewind())

            val results = parseYoloOutput(outputBuffer.floatArray)
            Log.d("YOLO_DEBUG", "Số vật thể: ${results.size}")
            _frameResult.value = FrameResult(results, sourceW, sourceH)

        } catch (e: Exception) {
            Log.e("YOLO_ERROR", "Crash logic AI: ${e.message}", e)
        }
    }

    private fun parseYoloOutput(array: FloatArray): List<DetectionResult> {
        val boxes = mutableListOf<DetectionResult>()
        val numColumns = 16464
        val numRows = 106

        val maxScores = FloatArray(numColumns) { 0f }
        val classIds = IntArray(numColumns) { -1 }

        for (r in 4 until numRows) {
            val rowOffset = r * numColumns
            for (c in 0 until numColumns) {
                val score = array[rowOffset + c]
                if (score > maxScores[c]) {
                    maxScores[c] = score
                    classIds[c] = r - 4
                }
            }
        }

        for (c in 0 until numColumns) {
            if (maxScores[c] > YoloConstants.CONFIDENCE_THRESHOLD) {
                val cx = array[0 * numColumns + c]
                val cy = array[1 * numColumns + c]
                val w = array[2 * numColumns + c]
                val h = array[3 * numColumns + c]

                val x1 = cx - w / 2
                val y1 = cy - h / 2
                val x2 = cx + w / 2
                val y2 = cy + h / 2

                boxes.add(DetectionResult(x1, y1, x2, y2, maxScores[c], YoloConstants.LABELS[classIds[c]]))
            }
        }

        return applyNMS(boxes)
    }

    private fun applyNMS(boxes: List<DetectionResult>): List<DetectionResult> {
        val sortedBoxes = boxes.sortedByDescending { it.score }
        val selectedBoxes = mutableListOf<DetectionResult>()

        for (box in sortedBoxes) {
            var shouldSelect = true
            for (selected in selectedBoxes) {
                if (calculateIoU(box, selected) > YoloConstants.IOU_THRESHOLD) {
                    shouldSelect = false
                    break
                }
            }
            if (shouldSelect) selectedBoxes.add(box)
        }
        return selectedBoxes
    }

    private fun calculateIoU(box1: DetectionResult, box2: DetectionResult): Float {
        val xA = maxOf(box1.x1, box2.x1)
        val yA = maxOf(box1.y1, box2.y1)
        val xB = minOf(box1.x2, box2.x2)
        val yB = minOf(box1.y2, box2.y2)

        val interArea = maxOf(0f, xB - xA) * maxOf(0f, yB - yA)
        val box1Area = (box1.x2 - box1.x1) * (box1.y2 - box1.y1)
        val box2Area = (box2.x2 - box2.x1) * (box2.y2 - box2.y1)

        return interArea / (box1Area + box2Area - interArea)
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}