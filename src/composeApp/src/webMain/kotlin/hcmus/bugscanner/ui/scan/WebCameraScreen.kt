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

/**
 * Màn hình xử lý luồng video trực tiếp từ WebRTC Camera trên trình duyệt.
 * Kết hợp sử dụng thẻ `<video>` ẩn và vẽ lại lên giao diện Compose bằng `Canvas`.
 *
 * @param modifier Tùy chỉnh kích thước và vị trí của Camera View.
 * @param captureTrigger Biến kích hoạt trạng thái chụp ngầm.
 * @param onResult Bắn kết quả AI (Bounding Boxes) lên UI Component cha.
 * @param onFrameCaptured Xuất mảng byte (ByteArray) chất lượng cao của khung hình khi nhận được lệnh chụp.
 */
@Composable
fun WebCameraScreen(
    modifier: Modifier = Modifier,
    captureTrigger: Long,
    onResult: (FrameResult) -> Unit,
    onFrameCaptured: (ByteArray) -> Unit
) {
    val textMeasurer = rememberTextMeasurer()
    var currentFrameResult by remember { mutableStateOf<FrameResult?>(null) }
    var isAiReady by remember { mutableStateOf(false) }
    var pendingCapture by remember { mutableStateOf(false) }
    var videoWidth by remember { mutableStateOf(0f) }
    var videoHeight by remember { mutableStateOf(0f) }
    val coroutineScope = rememberCoroutineScope()

    val videoElement = remember {
        (document.createElement("video") as HTMLVideoElement).apply {
            autoplay = true
            playsInline = true
            style.position = "absolute"
            style.objectFit = "cover"
            style.zIndex = "-1"
        }
    }

    LaunchedEffect(captureTrigger) {
        if (captureTrigger > 0L) {
            pendingCapture = true
        }
    }

    DisposableEffect(Unit) {
        document.body?.appendChild(videoElement)

        val htmlCanvas = document.createElement("canvas") as HTMLCanvasElement
        val ctx = htmlCanvas.getContext("2d", js("{ willReadFrequently: true }")) as CanvasRenderingContext2D

        var streamData: dynamic = null
        var isDetecting = false

        val navigatorDyn = window.navigator.asDynamic()
        navigatorDyn.mediaDevices.getUserMedia(js("{ video: { facingMode: 'environment', width: { ideal: 896 }, height: { ideal: 896 } } }"))
            .then { stream ->
                streamData = stream
                videoElement.srcObject = stream
            }.catch { e: dynamic ->
                println("Lỗi mở camera Web: $e")
            }

        val job = coroutineScope.launch {
            launch {
                delay(1000.milliseconds)
                isAiReady = WebYoloDetector.initialize()
            }

            while (isActive) {
                delay(100.milliseconds)

                if (videoElement.videoWidth > 0 && videoElement.videoHeight > 0) {
                    videoWidth = videoElement.videoWidth.toFloat()
                    videoHeight = videoElement.videoHeight.toFloat()

                    if (pendingCapture) {
                        pendingCapture = false
                        try {
                            htmlCanvas.width = videoElement.videoWidth
                            htmlCanvas.height = videoElement.videoHeight
                            ctx.drawImage(videoElement, 0.0, 0.0, htmlCanvas.width.toDouble(), htmlCanvas.height.toDouble())

                            val hqDataUrl = htmlCanvas.toDataURL("image/jpeg", 0.95)
                            val hqBase64 = hqDataUrl.substringAfter(",")
                            val hqBinary = window.atob(hqBase64)
                            val hqByteArray = ByteArray(hqBinary.length) { i -> hqBinary[i].code.toByte() }
                            onFrameCaptured(hqByteArray)
                        } catch (e: Exception) {
                            println("Lỗi chụp ảnh: ${e.message}")
                        }
                    }

                    if (isAiReady && !isDetecting) {
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
        }

        onDispose {
            job.cancel()
            if (streamData != null) {
                val tracks = streamData.getTracks()
                for (i in 0 until tracks.length as Int) {
                    tracks[i].stop()
                }
            }
            videoElement.srcObject = null
            videoElement.remove()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
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
                Text("Đang khởi động Camera...", color = Color.White)
            }
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(
                    color = Color.Transparent,
                    blendMode = androidx.compose.ui.graphics.BlendMode.Clear
                )

                val canvasWidth = size.width
                val canvasHeight = size.height

                val scale = maxOf(canvasWidth / videoWidth, canvasHeight / videoHeight)
                val drawWidth = videoWidth * scale
                val drawHeight = videoHeight * scale
                val offsetX = (canvasWidth - drawWidth) / 2f
                val offsetY = (canvasHeight - drawHeight) / 2f

                currentFrameResult?.boxes?.forEach { box ->
                    val left = (box.x1 * drawWidth + offsetX).coerceIn(0f, canvasWidth)
                    val top = (box.y1 * drawHeight + offsetY).coerceIn(0f, canvasHeight)
                    val right = (box.x2 * drawWidth + offsetX).coerceIn(0f, canvasWidth)
                    val bottom = (box.y2 * drawHeight + offsetY).coerceIn(0f, canvasHeight)
                    val width = right - left
                    val height = bottom - top

                    if (width > 0f && height > 0f) {
                        val boxColor = getBugColor(box.className)
                        drawRoundRect(
                            color = boxColor,
                            topLeft = Offset(left, top),
                            size = Size(width, height),
                            cornerRadius = CornerRadius(16f, 16f),
                            style = Stroke(width = 5f)
                        )

                        val labelText = "${box.className} (${(box.score * 100).toInt()}%)"
                        val textStyle = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        val textLayoutResult = textMeasurer.measure(text = labelText, style = textStyle)
                        val textWidth = textLayoutResult.size.width.toFloat()
                        val textHeight = textLayoutResult.size.height.toFloat()

                        val labelTop = maxOf(0f, top - textHeight - 12f)
                        drawRoundRect(
                            color = boxColor.copy(alpha = 0.85f),
                            topLeft = Offset(left, labelTop),
                            size = Size(textWidth + 24f, textHeight + 12f),
                            cornerRadius = CornerRadius(12f, 12f)
                        )
                        drawText(
                            textLayoutResult = textLayoutResult,
                            color = Color.White,
                            topLeft = Offset(left + 12f, labelTop + 6f)
                        )
                    }
                }
            }
        }
    }
}