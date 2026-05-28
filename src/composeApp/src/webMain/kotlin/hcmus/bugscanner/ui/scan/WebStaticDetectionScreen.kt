package hcmus.bugscanner.ui.scan

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import hcmus.bugscanner.ui.scan.components.getBugColor
import kotlinx.browser.window
import kotlinx.coroutines.await
import org.jetbrains.skia.Image
import org.khronos.webgl.Int8Array

/**
 * Component giao diện hiển thị hình ảnh tĩnh được chọn từ thiết bị (File Explorer) trên nền tảng Web.
 * Xử lý giải mã Blob URL thành mảng byte để Skia vẽ lên Canvas, đồng thời phủ các Bounding Box từ AI lên trên.
 *
 * @param modifier Modifier tùy chỉnh kích thước, vị trí từ Component cha.
 * @param imageId Đường dẫn Blob URL nội bộ của bức ảnh trên trình duyệt.
 * @param frameResult Dữ liệu chứa tọa độ các vật thể được nhận diện.
 */
@Composable
fun WebStaticDetectionScreen(
    modifier: Modifier = Modifier,
    imageId: String?,
    frameResult: FrameResult?
) {
    val textMeasurer = rememberTextMeasurer()
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    // Theo dõi và tải hình ảnh bất cứ khi nào Blob URL (imageId) thay đổi
    LaunchedEffect(imageId) {
        if (imageId != null) {
            try {
                // Fetch trực tiếp dữ liệu từ Blob URL
                val response = window.fetch(imageId, js("{}")).await()
                val arrayBuffer = response.arrayBuffer().await()
                // Chuyển đổi dữ liệu thô sang ByteArray tương thích với Kotlin/JS
                val byteArray = Int8Array(arrayBuffer).unsafeCast<ByteArray>()
                // Dùng engine Skia giải mã mảng byte thành ImageBitmap để Compose vẽ được
                imageBitmap = Image.makeFromEncoded(byteArray).toComposeImageBitmap()
            } catch (e: Exception) {
                println("Lỗi load ảnh hiển thị: ${e.message}")
            }
        } else {
            imageBitmap = null
        }
    }

    if (imageId == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Chưa có hình ảnh để phân tích", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (imageBitmap == null) {
            CircularProgressIndicator()
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val imgWidth = imageBitmap!!.width.toFloat()
                val imgHeight = imageBitmap!!.height.toFloat()

                // Tính toán tỷ lệ co giãn (Scale) và vị trí căn giữa (Offset) cho hình ảnh
                val scale = minOf(canvasWidth / imgWidth, canvasHeight / imgHeight)
                val drawWidth = imgWidth * scale
                val drawHeight = imgHeight * scale
                val offsetX = (canvasWidth - drawWidth) / 2f
                val offsetY = (canvasHeight - drawHeight) / 2f

                // Vẽ hình ảnh tĩnh
                drawImage(
                    image = imageBitmap!!,
                    dstOffset = IntOffset(offsetX.toInt(), offsetY.toInt()),
                    dstSize = IntSize(drawWidth.toInt(), drawHeight.toInt())
                )

                // Duyệt qua kết quả AI và vẽ các ô vuông giới hạn (Bounding Boxes)
                if (frameResult != null && frameResult.boxes.isNotEmpty()) {
                    frameResult.boxes.forEach { box ->
                        val left = (box.x1 * drawWidth + offsetX).coerceIn(offsetX, offsetX + drawWidth)
                        val top = (box.y1 * drawHeight + offsetY).coerceIn(offsetY, offsetY + drawHeight)
                        val right = (box.x2 * drawWidth + offsetX).coerceIn(offsetX, offsetX + drawWidth)
                        val bottom = (box.y2 * drawHeight + offsetY).coerceIn(offsetY, offsetY + drawHeight)
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
}