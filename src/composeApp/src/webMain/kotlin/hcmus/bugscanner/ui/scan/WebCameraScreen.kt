package hcmus.bugscanner.ui.scan

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hcmus.bugscanner.domain.model.FrameResult
import hcmus.bugscanner.ml.WebYoloDetector
import hcmus.bugscanner.ui.scan.utils.getBugColor
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLVideoElement
import kotlin.time.Duration.Companion.milliseconds

private const val LIVE_DETECTION_INTERVAL_MS = 500

@Composable
fun WebCameraScreen(
    modifier: Modifier = Modifier,
    captureTrigger: Long,
    onResult: (FrameResult) -> Unit,
    onFrameCaptured: (ByteArray) -> Unit,
    onRuntimeStatus: (ScanRuntimeStatus) -> Unit
) {
    val textMeasurer = rememberTextMeasurer()
    var currentFrameResult by remember { mutableStateOf<FrameResult?>(null) }
    var liveDetectionSupported by remember { mutableStateOf(false) }
    var pendingCapture by remember { mutableStateOf(false) }
    var videoWidth by remember { mutableStateOf(0f) }
    var videoHeight by remember { mutableStateOf(0f) }
    val coroutineScope = rememberCoroutineScope()

    val videoElement = remember {
        (document.createElement("video") as HTMLVideoElement).apply {
            autoplay = true
            playsInline = true
            muted = true
            style.position = "absolute"
            style.objectFit = "cover"
            style.asDynamic().pointerEvents = "none"
            style.zIndex = "-1"
        }
    }

    LaunchedEffect(captureTrigger) {
        if (captureTrigger > 0L) pendingCapture = true
    }

    DisposableEffect(Unit) {
        document.body?.appendChild(videoElement)
        onRuntimeStatus(ScanRuntimeStatus.RequestingCamera)

        val htmlCanvas = document.createElement("canvas") as HTMLCanvasElement
        val ctx = htmlCanvas.getContext("2d", js("{ willReadFrequently: true }")) as CanvasRenderingContext2D
        var streamData: dynamic = null
        var isDetecting = false
        val navigatorDyn = window.navigator.asDynamic()

        if (navigatorDyn.mediaDevices == undefined) {
            onRuntimeStatus(ScanRuntimeStatus.Unsupported("Trình duyệt không hỗ trợ truy cập camera."))
        } else {
            navigatorDyn.mediaDevices.getUserMedia(js("{ video: { facingMode: 'environment', width: { ideal: 896 }, height: { ideal: 896 } } }"))
                .then { stream ->
                    streamData = stream
                    videoElement.srcObject = stream
                    onRuntimeStatus(ScanRuntimeStatus.LoadingModel())
                    coroutineScope.launch {
                        val runtime = WebYoloDetector.initialize()
                        liveDetectionSupported = runtime.liveDetectionSupported
                        onRuntimeStatus(runtime.toRuntimeStatus())
                    }
                }.catch { error: dynamic ->
                    val name = error?.name as? String ?: ""
                    if (name == "NotAllowedError" || name == "PermissionDeniedError") {
                        onRuntimeStatus(ScanRuntimeStatus.PermissionDenied())
                    } else {
                        onRuntimeStatus(ScanRuntimeStatus.Error("Không thể mở camera: ${error?.message ?: name}"))
                    }
                }
        }

        val job = coroutineScope.launch {
            while (isActive) {
                delay(LIVE_DETECTION_INTERVAL_MS.milliseconds)
                if ((document.asDynamic().hidden as? Boolean) == true || videoElement.videoWidth <= 0 || videoElement.videoHeight <= 0) continue

                videoWidth = videoElement.videoWidth.toFloat()
                videoHeight = videoElement.videoHeight.toFloat()

                if (pendingCapture) {
                    pendingCapture = false
                    try {
                        htmlCanvas.width = videoElement.videoWidth
                        htmlCanvas.height = videoElement.videoHeight
                        ctx.drawImage(videoElement, 0.0, 0.0, htmlCanvas.width.toDouble(), htmlCanvas.height.toDouble())
                        val base64 = htmlCanvas.toDataURL("image/jpeg", 0.92).substringAfter(",")
                        val binary = window.atob(base64)
                        onFrameCaptured(ByteArray(binary.length) { index -> binary[index].code.toByte() })
                    } catch (e: Exception) {
                        onRuntimeStatus(ScanRuntimeStatus.Error("Không thể chụp khung hình: ${e.message}"))
                    }
                }

                if (liveDetectionSupported && !isDetecting) {
                    isDetecting = true
                    launch {
                        try {
                            val result = WebYoloDetector.analyze(videoElement, videoElement.videoWidth, videoElement.videoHeight)
                            currentFrameResult = result
                            onResult(result)
                        } finally {
                            isDetecting = false
                        }
                    }
                }
            }
        }

        onDispose {
            job.cancel()
            if (streamData != null) {
                val tracks = streamData.getTracks()
                for (index in 0 until tracks.length as Int) tracks[index].stop()
            }
            videoElement.srcObject = null
            videoElement.remove()
        }
    }

    Box(
        modifier = modifier.fillMaxSize().onGloballyPositioned { coordinates ->
            val position = coordinates.positionInWindow()
            videoElement.style.left = "${position.x}px"
            videoElement.style.top = "${position.y}px"
            videoElement.style.width = "${coordinates.size.width}px"
            videoElement.style.height = "${coordinates.size.height}px"
        },
        contentAlignment = Alignment.Center
    ) {
        if (videoWidth == 0f || videoHeight == 0f) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Đang khởi động camera...", color = Color.White)
            }
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val scale = maxOf(size.width / videoWidth, size.height / videoHeight)
                val drawWidth = videoWidth * scale
                val drawHeight = videoHeight * scale
                val offsetX = (size.width - drawWidth) / 2f
                val offsetY = (size.height - drawHeight) / 2f
                currentFrameResult?.boxes?.forEach { box ->
                    val left = (box.x1 * drawWidth + offsetX).coerceIn(0f, size.width)
                    val top = (box.y1 * drawHeight + offsetY).coerceIn(0f, size.height)
                    val right = (box.x2 * drawWidth + offsetX).coerceIn(0f, size.width)
                    val bottom = (box.y2 * drawHeight + offsetY).coerceIn(0f, size.height)
                    if (right > left && bottom > top) {
                        val color = getBugColor(box.className)
                        drawRoundRect(color, Offset(left, top), Size(right - left, bottom - top), CornerRadius(16f, 16f), style = Stroke(5f))
                        val text = textMeasurer.measure("${box.className} (${(box.score * 100).toInt()}%)", TextStyle(Color.White, 16.sp, fontWeight = FontWeight.Bold))
                        val labelTop = maxOf(0f, top - text.size.height - 12f)
                        drawRoundRect(color.copy(alpha = 0.85f), Offset(left, labelTop), Size(text.size.width + 24f, text.size.height + 12f), CornerRadius(12f, 12f))
                        drawText(text, Color.White, Offset(left + 12f, labelTop + 6f))
                    }
                }
            }
        }
    }
}
