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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
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
import org.jetbrains.skia.Image
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLVideoElement
import kotlin.time.Duration.Companion.milliseconds

/**
 * Màn hình xử lý luồng video trực tiếp từ WebRTC Camera trên trình duyệt.
 * Kết hợp sử dụng thẻ `<video>` ẩn và vẽ lại lên giao diện Compose bằng `Canvas`.
 *
 * @param modifier Tùy chỉnh kích thước và vị trí của Camera View.
 * @param onResult Bắn kết quả AI (Bounding Boxes) lên UI Component cha để thống kê.
 * @param onLiveFrameCaptured Xuất mảng byte (ByteArray) của khung hình hiện tại nếu AI tìm thấy côn trùng.
 */
@Composable
fun WebCameraScreen(
    modifier: Modifier = Modifier,
    onResult: (FrameResult) -> Unit,
    onLiveFrameCaptured: (ByteArray?) -> Unit
) {
    val textMeasurer = rememberTextMeasurer()
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var currentFrameResult by remember { mutableStateOf<FrameResult?>(null) }
    val coroutineScope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        // Khởi tạo thẻ video ẩn trên DOM để hứng stream từ WebRTC
        val video = document.createElement("video") as HTMLVideoElement
        video.autoplay = true
        video.playsInline = true // Yêu cầu bắt buộc trên iOS Safari để không bật Fullscreen Player

        // Khởi tạo thẻ canvas ẩn để chụp ảnh từ luồng video
        val htmlCanvas = document.createElement("canvas") as HTMLCanvasElement
        val ctx = htmlCanvas.getContext("2d") as CanvasRenderingContext2D

        var streamData: dynamic = null
        var isDetecting = false

        // Yêu cầu quyền Camera từ trình duyệt (Ưu tiên camera sau - environment)
        val navigatorDyn = window.navigator.asDynamic()
        navigatorDyn.mediaDevices.getUserMedia(js("{ video: { facingMode: 'environment' } }"))
            .then { stream ->
                streamData = stream
                video.srcObject = stream
            }.catch { e: dynamic ->
                println("Lỗi mở camera Web: $e")
            }

        // Vòng lặp lấy khung hình liên tục để vẽ lên giao diện Compose
        val job = coroutineScope.launch {
            while (isActive) {
                delay(33.milliseconds) // ~30 FPS

                if (video.videoWidth > 0 && video.videoHeight > 0) {
                    htmlCanvas.width = video.videoWidth
                    htmlCanvas.height = video.videoHeight
                    ctx.drawImage(video, 0.0, 0.0, htmlCanvas.width.toDouble(), htmlCanvas.height.toDouble())

                    // 1. Trích xuất hình ảnh (Base64) chuyển sang Compose ImageBitmap
                    try {
                        val dataUrl = htmlCanvas.toDataURL("image/jpeg", 0.6) // Nén mức 60% để tối ưu RAM
                        val base64Data = dataUrl.substringAfter(",")
                        val binaryString = window.atob(base64Data)
                        val byteArray = ByteArray(binaryString.length) { i -> binaryString[i].code.toByte() }
                        imageBitmap = Image.makeFromEncoded(byteArray).toComposeImageBitmap()

                        // Kích hoạt callback xuất mảng byte lưu ảnh NẾU AI tìm thấy đối tượng
                        if (currentFrameResult?.boxes?.isNotEmpty() == true) {
                            onLiveFrameCaptured(byteArray)
                        } else {
                            onLiveFrameCaptured(null)
                        }

                    } catch (e: Exception) {
                        println("Lỗi render hình ảnh: ${e.message}")
                    }

                    // 2. Chạy luồng phân tích AI ngầm bằng WebGL
                    if (!isDetecting) {
                        isDetecting = true
                        launch {
                            try {
                                val result = WebYoloDetector.analyze(video, video.videoWidth, video.videoHeight)
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

        // Dọn dẹp luồng WebRTC và giải phóng DOM khi Component bị hủy
        onDispose {
            job.cancel()
            if (streamData != null) {
                val tracks = streamData.getTracks()
                for (i in 0 until tracks.length as Int) {
                    tracks[i].stop()
                }
            }
            video.srcObject = null
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (imageBitmap == null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Đang khởi động Camera...", color = Color.White)
            }
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val imgWidth = imageBitmap!!.width.toFloat()
                val imgHeight = imageBitmap!!.height.toFloat()

                // Vẽ hình ảnh khớp màn hình
                val scale = maxOf(canvasWidth / imgWidth, canvasHeight / imgHeight)
                val drawWidth = imgWidth * scale
                val drawHeight = imgHeight * scale
                val offsetX = (canvasWidth - drawWidth) / 2f
                val offsetY = (canvasHeight - drawHeight) / 2f

                drawImage(
                    image = imageBitmap!!,
                    dstOffset = IntOffset(offsetX.toInt(), offsetY.toInt()),
                    dstSize = IntSize(drawWidth.toInt(), drawHeight.toInt())
                )

                // Vẽ Bounding Boxes
                currentFrameResult?.boxes?.forEach { box ->
                    val left = (box.x1 * drawWidth + offsetX).coerceIn(0f, canvasWidth)
                    val top = (box.y1 * drawHeight + offsetY).coerceIn(0f, canvasHeight)
                    val right = (box.x2 * drawWidth + offsetX).coerceIn(0f, canvasWidth)
                    val bottom = (box.y2 * drawHeight + offsetY).coerceIn(0f, canvasHeight)
                    val width = right - left
                    val height = bottom - top

                    if (width > 0f && height > 0f) {
                        val boxColor = getBugColor(box.className)
                        drawRoundRect(color = boxColor, topLeft = Offset(left, top), size = Size(width, height), cornerRadius = CornerRadius(16f, 16f), style = Stroke(width = 5f))

                        val labelText = "${box.className} (${(box.score * 100).toInt()}%)"
                        val textStyle = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        val textLayoutResult = textMeasurer.measure(text = labelText, style = textStyle)
                        val textWidth = textLayoutResult.size.width.toFloat()
                        val textHeight = textLayoutResult.size.height.toFloat()

                        val labelTop = maxOf(0f, top - textHeight - 12f)
                        drawRoundRect(color = boxColor.copy(alpha = 0.85f), topLeft = Offset(left, labelTop), size = Size(textWidth + 24f, textHeight + 12f), cornerRadius = CornerRadius(12f, 12f))
                        drawText(textLayoutResult = textLayoutResult, color = Color.White, topLeft = Offset(left + 12f, labelTop + 6f))
                    }
                }
            }
        }
    }
}