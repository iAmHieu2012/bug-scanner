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

/**
 * Lớp xử lý nhận diện vật thể (Object Detector) sử dụng mô hình AI YOLO định dạng TFLite.
 * Chịu trách nhiệm tải mô hình, tiền xử lý hình ảnh đầu vào, suy luận (inference) và hậu xử lý kết quả.
 *
 * @param context Context của ứng dụng để truy cập thư mục assets chứa file mô hình.
 * @param modelPath Đường dẫn file mô hình TFLite trong thư mục assets (Mặc định lấy từ YoloConstants).
 */
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

    /**
     * Tải tệp tin mô hình định dạng `.tflite` từ thư mục assets dưới dạng bộ đệm bộ nhớ trực tiếp (MappedByteBuffer).
     *
     * @param context Context của ứng dụng để mở tệp tin từ assets.
     * @param modelPath Đường dẫn tệp tin mô hình trong assets.
     * @return Bộ đệm bộ nhớ [MappedByteBuffer] chứa dữ liệu của tệp mô hình.
     */
    private fun loadModelFile(context: Context, modelPath: String): MappedByteBuffer {
        val fd = context.assets.openFd(modelPath)
        return FileInputStream(fd.fileDescriptor).channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
    }

    /**
     * Xóa kết quả nhận diện hiện tại, đưa trạng thái luồng về danh sách rỗng.
     */
    fun clearResult() {
        _frameResult.value = FrameResult(emptyList(), 0, 0)
    }

    /**
     * Thực hiện phân tích và nhận diện sâu bệnh trên một Frame ảnh (Bitmap).
     * Kết quả phân tích thành công sẽ được cập nhật trực tiếp vào luồng [frameResult].
     *
     * @param inputBitmap Hình ảnh Bitmap cần nhận diện.
     * @param rotationDegrees Góc xoay của ảnh (để ImageProcessor tự động xoay chuẩn hóa trước khi đưa vào mô hình).
     */
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

    /**
     * Giải mã ma trận đầu ra phẳng của mô hình YOLOv8 thành danh sách các Bounding Box thô.
     * Ma trận đầu ra có kích thước hàng nhân cột là (106 x 16464) chuyển thành mảng 1 chiều.
     * Trong đó: 4 hàng đầu là tọa độ (cx, cy, w, h), 102 hàng sau là điểm số (score) của các nhãn sâu bệnh.
     *
     * @param array Mảng phẳng chứa dữ liệu đầu ra từ mô hình TensorFlow Lite.
     * @return Danh sách các kết quả nhận diện [DetectionResult] thô được trích xuất.
     */
    private fun parseYoloOutput(array: FloatArray): List<DetectionResult> {
        val boxes = mutableListOf<DetectionResult>()
        val numColumns = 16464
        val numRows = 106

        val maxScores = FloatArray(numColumns)
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

    /**
     * Áp dụng thuật toán Non-Maximum Suppression (NMS) nhằm giữ lại box tối ưu nhất.
     * Các box có độ phủ chồng chéo nhau lớn hơn [YoloConstants.IOU_THRESHOLD] sẽ bị triệt tiêu.
     *
     * @param boxes Danh sách các Bounding Box thô thu được từ mô hình.
     * @return Danh sách các Bounding Box tối ưu sau khi đã lọc bỏ chồng lấp.
     */
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

    /**
     * Tính toán chỉ số tỉ lệ diện tích chồng lấn Intersection over Union (IoU) giữa hai Bounding Box.
     *
     * @param box1 Khung giới hạn thứ nhất.
     * @param box2 Khung giới hạn thứ hai.
     * @return Tỷ lệ chồng lấn diện tích giao nhau trên diện tích hợp nhất [Float].
     */
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

    /**
     * Giải phóng tài nguyên bộ nhớ của Interpreter khi không còn sử dụng Detector này nữa.
     */
    fun close() {
        interpreter?.close()
        interpreter = null
    }
}