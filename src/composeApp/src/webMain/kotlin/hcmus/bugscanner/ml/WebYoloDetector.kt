package hcmus.bugscanner.ml

import hcmus.bugscanner.domain.model.DetectionResult
import hcmus.bugscanner.domain.model.FrameResult
import kotlinx.browser.window
import kotlinx.coroutines.await
import org.w3c.dom.HTMLElement
import kotlin.js.Promise

/**
 * Object trung gian xử lý gọi hàm Machine Learning (TensorFlow.js) trên Web.
 */
object WebYoloDetector {

    /**
     * @param sourceElement Có thể là thẻ HTMLVideoElement (Camera) hoặc HTMLImageElement (Ảnh tĩnh)
     */
    suspend fun analyze(sourceElement: HTMLElement, sourceWidth: Int, sourceHeight: Int): FrameResult {
        return try {
            // Truyền trực tiếp Element vào JS để tận dụng GPU WebGL
            val jsonResultPromise = window.asDynamic()
                .detectBugsJS(sourceElement)
                .unsafeCast<Promise<String>>()

            val jsonResult = jsonResultPromise.await()
            val jsArray = kotlin.js.JSON.parse<Array<dynamic>>(jsonResult)

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