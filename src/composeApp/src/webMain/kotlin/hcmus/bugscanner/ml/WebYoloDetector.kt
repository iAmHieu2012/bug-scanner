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

@Serializable
private data class JsDetection(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val label: String,
    val confidence: Float
)

@Serializable
data class WebYoloInitResult(
    val ready: Boolean = false,
    val backend: String = "none",
    val liveDetectionSupported: Boolean = false,
    val error: String? = null
) {
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

internal external fun initYolo(): Promise<String>
internal external fun detectBugsJS(source: HTMLElement): Promise<String>

object WebYoloDetector {
    private val jsonParser = Json { ignoreUnknownKeys = true }

    suspend fun initialize(): WebYoloInitResult = try {
        jsonParser.decodeFromString(initYolo().await())
    } catch (e: Exception) {
        WebYoloInitResult(error = e.message ?: "Lỗi khởi tạo TensorFlow.js")
    }

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
