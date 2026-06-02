package hcmus.bugscanner.ml

import hcmus.bugscanner.domain.model.DetectionResult
import hcmus.bugscanner.domain.model.FrameResult
import kotlinx.coroutines.await
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.w3c.dom.HTMLElement
import kotlin.js.Promise

/**
 * Lớp dữ liệu trung gian để map chuỗi JSON trả về từ JavaScript.
 * Thay thế cho việc sử dụng Array<dynamic> vốn không được hỗ trợ trên Wasm.
 *
 * @property x Tọa độ X góc trên bên trái của Bounding Box.
 * @property y Tọa độ Y góc trên bên trái của Bounding Box.
 * @property width Chiều rộng của Bounding Box.
 * @property height Chiều cao của Bounding Box.
 * @property label Mã nhãn (ID) của đối tượng được nhận diện.
 * @property confidence Điểm tin cậy của kết quả nhận diện.
 */
@Serializable
private data class JsDetection(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val label: String,
    val confidence: Float
)

/**
 * Hàm ngoại vi khởi tạo mô hình YOLO thông qua TensorFlow.js.
 *
 * @return [Promise] chứa kết quả khởi tạo (true nếu thành công).
 */
internal external fun initYolo(): Promise<Boolean>

/**
 * Hàm ngoại vi gửi phần tử HTML chứa ảnh/video sang JavaScript để nhận diện.
 *
 * @param source Phần tử HTML (<img> hoặc <video>) chứa dữ liệu ảnh.
 * @return [Promise] chứa chuỗi JSON biểu diễn danh sách kết quả nhận diện.
 */
internal external fun detectBugsJS(source: HTMLElement): Promise<String>

/**
 * Đối tượng trung gian (Bridge) xử lý gọi hàm Machine Learning trên nền tảng Web.
 * Tận dụng thư viện TensorFlow.js để phân tích hình ảnh bằng sức mạnh của GPU thông qua WebGL.
 */
object WebYoloDetector {

    private val jsonParser = Json { ignoreUnknownKeys = true }

    /**
     * Kích hoạt khởi chạy AI Model từ JS.
     *
     * @return Trả về `true` nếu tải AI thành công, `false` nếu thất bại.
     */
    suspend fun initialize(): Boolean {
        return try {
            initYolo().await()
        } catch (e: Exception) {
            println("Lỗi gọi khởi tạo AI từ JS: ${e.message}")
            false
        }
    }

    /**
     * Thực hiện gửi yêu cầu phân tích hình ảnh/video sang môi trường JavaScript thuần.
     *
     * @param sourceElement Phần tử HTML chứa dữ liệu ảnh (HTMLVideoElement hoặc HTMLImageElement).
     * @param sourceWidth Chiều rộng gốc của phần tử nguồn.
     * @param sourceHeight Chiều cao gốc của phần tử nguồn.
     * @return Dữ liệu [FrameResult] chứa danh sách các vật thể đã nhận diện.
     */
    suspend fun analyze(sourceElement: HTMLElement, sourceWidth: Int, sourceHeight: Int): FrameResult {
        return try {
            val jsonResult = detectBugsJS(sourceElement).await()

            val jsList = jsonParser.decodeFromString<List<JsDetection>>(jsonResult)

            val detectionBoxes = jsList.map { obj ->
                val xMin = obj.x
                val yMin = obj.y
                val width = obj.width
                val height = obj.height
                val conf = obj.confidence

                val classId = obj.label.toIntOrNull() ?: 0
                val labelStr = if (classId in YoloConstants.LABELS.indices) {
                    YoloConstants.LABELS[classId]
                } else {
                    "Unknown Pest"
                }

                DetectionResult(
                    x1 = xMin,
                    y1 = yMin,
                    x2 = xMin + width,
                    y2 = yMin + height,
                    score = conf,
                    className = labelStr
                )
            }

            FrameResult(boxes = detectionBoxes, sourceWidth = sourceWidth, sourceHeight = sourceHeight)
        } catch (e: Exception) {
            println("Lỗi WebYoloDetector: ${e.message}")
            FrameResult(emptyList(), sourceWidth, sourceHeight)
        }
    }
}