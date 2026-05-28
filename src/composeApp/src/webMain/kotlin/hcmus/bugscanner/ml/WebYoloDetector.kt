package hcmus.bugscanner.ml

import hcmus.bugscanner.domain.model.DetectionResult
import hcmus.bugscanner.domain.model.FrameResult
import kotlinx.browser.window
import kotlinx.coroutines.await
import org.w3c.dom.HTMLElement
import kotlin.js.Promise

/**
 * Đối tượng trung gian (Bridge) xử lý gọi hàm Machine Learning trên nền tảng Web.
 * Tận dụng thư viện TensorFlow.js (được nhúng qua thẻ `<script>` trong `index.html`)
 * để phân tích hình ảnh bằng sức mạnh của GPU thông qua WebGL.
 */
object WebYoloDetector {

    /**
     * Thực hiện gửi yêu cầu phân tích hình ảnh/video sang môi trường JavaScript thuần.
     *
     * @param sourceElement Phần tử HTML chứa dữ liệu ảnh. Có thể là thẻ `HTMLVideoElement` (Camera) hoặc `HTMLImageElement` (Ảnh tĩnh).
     * @param sourceWidth Chiều rộng gốc của phần tử nguồn.
     * @param sourceHeight Chiều cao gốc của phần tử nguồn.
     * @return Dữ liệu [FrameResult] chứa danh sách các vật thể (Bounding Boxes) đã nhận diện.
     */
    suspend fun analyze(sourceElement: HTMLElement, sourceWidth: Int, sourceHeight: Int): FrameResult {
        return try {
            // Truyền trực tiếp Element vào JS để tận dụng GPU WebGL thay vì gửi mảng Byte (giúp tăng tốc độ xử lý)
            val jsonResultPromise = window.asDynamic()
                .detectBugsJS(sourceElement)
                .unsafeCast<Promise<String>>()

            // Chờ JS xử lý xong và parse chuỗi JSON trả về
            val jsonResult = jsonResultPromise.await()
            val jsArray = JSON.parse<Array<dynamic>>(jsonResult)

            val detectionBoxes = jsArray.map { obj ->
                val xMin = (obj.x as Number).toFloat()
                val yMin = (obj.y as Number).toFloat()
                val width = (obj.width as Number).toFloat()
                val height = (obj.height as Number).toFloat()
                val conf = (obj.confidence as Number).toFloat()

                val classId = obj.label.toString().toIntOrNull() ?: 0
                val labelStr = if (classId in YoloConstants.LABELS.indices) {
                    YoloConstants.LABELS[classId]
                } else {
                    "Unknown Pest"
                }

                DetectionResult(
                    x1 = xMin, y1 = yMin, x2 = xMin + width, y2 = yMin + height,
                    score = conf, className = labelStr
                )
            }.toList()

            FrameResult(boxes = detectionBoxes, sourceWidth = sourceWidth, sourceHeight = sourceHeight)
        } catch (e: Exception) {
            println("Lỗi WebYoloDetector: ${e.message}")
            FrameResult(emptyList(), sourceWidth, sourceHeight)
        }
    }
}