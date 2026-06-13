package hcmus.bugscanner.ml

import hcmus.bugscanner.domain.model.DetectionResult
import hcmus.bugscanner.domain.model.FrameResult
import hcmus.bugscanner.ui.scan.ScanRuntimeBackend
import hcmus.bugscanner.ui.scan.ScanRuntimeStatus
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
 * Lớp lưu trữ kết quả khởi tạo mô hình YOLO trên trình duyệt Web.
 *
 * @property ready Cờ xác định mô hình đã được tải thành công và sẵn sàng chạy nhận diện hay chưa.
 * @property backend Backend TensorFlow.js đang hoạt động (ví dụ: "webgl", "wasm").
 * @property liveDetectionSupported Chế độ quét camera thời gian thực có được hỗ trợ hay không.
 * @property error Thông điệp lỗi nếu quá trình nạp mô hình gặp sự cố.
 */
@Serializable
data class WebYoloInitResult(
    val ready: Boolean = false,
    val backend: String = "none",
    val liveDetectionSupported: Boolean = false,
    val error: String? = null
) {
    /**
     * Chuyển đổi thông tin kết quả khởi tạo thành trạng thái runtime [ScanRuntimeStatus] tương thích với KMP.
     *
     * @return Trạng thái runtime tương ứng.
     */
    fun toRuntimeStatus(): ScanRuntimeStatus {
        if (!ready) return ScanRuntimeStatus.Error(error ?: "Không thể khởi tạo mô hình nhận diện.")
        val runtimeBackend = when (backend.lowercase()) {
            "webgl" -> ScanRuntimeBackend.WEBGL
            "wasm" -> ScanRuntimeBackend.WASM
            else -> ScanRuntimeBackend.NONE
        }
        return ScanRuntimeStatus.Ready(runtimeBackend, liveDetectionSupported)
    }
}

/**
 * Hàm ngoại vi khởi tạo mô hình YOLO thông qua TensorFlow.js.
 *
 * @return [Promise] chứa kết quả khởi tạo dưới dạng chuỗi JSON của [WebYoloInitResult].
 */
internal external fun initYolo(): Promise<String>

/**
 * Hàm ngoại vi gửi phần tử HTML chứa ảnh/video sang JavaScript để nhận diện.
 *
 * @param source Phần tử HTML (<img> hoặc <video>) chứa dữ liệu ảnh.
 * @return [Promise] chứa chuỗi JSON biểu diễn danh sách kết quả nhận diện [JsDetection].
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
     * @return Trả về [WebYoloInitResult] chứa thông tin kết quả.
     */
    suspend fun initialize(): WebYoloInitResult = try {
        jsonParser.decodeFromString(initYolo().await())
    } catch (e: Exception) {
        WebYoloInitResult(error = e.message ?: "Lỗi khởi tạo TensorFlow.js")
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
            val jsList = jsonParser.decodeFromString<List<JsDetection>>(detectBugsJS(sourceElement).await())
            val detectionBoxes = jsList.map { obj ->
                val classId = obj.label.toIntOrNull() ?: 0
                DetectionResult(
                    x1 = obj.x,
                    y1 = obj.y,
                    x2 = obj.x + obj.width,
                    y2 = obj.y + obj.height,
                    score = obj.confidence,
                    className = YoloConstants.LABELS.getOrElse(classId) { "Unknown Pest" }
                )
            }
            FrameResult(boxes = detectionBoxes, sourceWidth = sourceWidth, sourceHeight = sourceHeight)
        } catch (e: Exception) {
            println("Lỗi WebYoloDetector: ${e.message}")
            FrameResult(emptyList(), sourceWidth, sourceHeight)
        }
    }
}
