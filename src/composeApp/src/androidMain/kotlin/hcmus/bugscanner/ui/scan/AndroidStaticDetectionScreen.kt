package hcmus.bugscanner.ui.scan

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import hcmus.bugscanner.domain.model.FrameResult
import hcmus.bugscanner.ui.scan.utils.drawYoloBoundingBox

/**
 * Component giao diện xử lý và hiển thị hình ảnh tĩnh (được tải lên từ Thư viện hoặc chụp từ Camera).
 * Tự động tính toán tỷ lệ khung hình (Scale) để hiển thị ảnh vừa vặn trên màn hình
 * và vẽ đè các khung nhận diện (Bounding Box) từ kết quả của AI.
 *
 * @param bitmap Đối tượng hình ảnh tĩnh cần hiển thị.
 * @param frameResult Dữ liệu chứa tọa độ các vật thể được nhận diện từ mô hình YOLO.
 * @param modifier Modifier tùy chỉnh vị trí, kích thước từ Component cha.
 */
@Composable
fun AndroidStaticDetectionScreen(bitmap: Bitmap?, frameResult: FrameResult, modifier: Modifier = Modifier) {
    if (bitmap == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Chưa có hình ảnh để phân tích",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val textMeasurer = rememberTextMeasurer()
    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val imageWidth = bitmap.width.toFloat()
        val imageHeight = bitmap.height.toFloat()

        // Tính toán tỷ lệ co giãn (Scale) để ảnh vừa khít Canvas nhưng vẫn giữ đúng Aspect Ratio
        val scale = minOf(canvasWidth / imageWidth, canvasHeight / imageHeight)
        val drawWidth = imageWidth * scale
        val drawHeight = imageHeight * scale

        // Tính toán độ lệch (Offset) để căn giữa bức ảnh trên Canvas
        val offsetX = (canvasWidth - drawWidth) / 2f
        val offsetY = (canvasHeight - drawHeight) / 2f

        // Vẽ bức ảnh gốc lên màn hình
        drawImage(
            image = bitmap.asImageBitmap(),
            dstOffset = IntOffset(offsetX.toInt(), offsetY.toInt()),
            dstSize = IntSize(drawWidth.toInt(), drawHeight.toInt())
        )

        // Duyệt qua danh sách kết quả AI và vẽ đè Bounding Box
        frameResult.boxes.forEach { box ->
            val nx1 = box.x1
            val ny1 = box.y1
            val nx2 = box.x2
            val ny2 = box.y2

            // Chuyển đổi tọa độ tương đối từ AI sang tọa độ thực tế trên màn hình (có tính đến Offset)
            val left = (nx1 * drawWidth + offsetX).coerceIn(offsetX, offsetX + drawWidth)
            val top = (ny1 * drawHeight + offsetY).coerceIn(offsetY, offsetY + drawHeight)
            val right = (nx2 * drawWidth + offsetX).coerceIn(offsetX, offsetX + drawWidth)
            val bottom = (ny2 * drawHeight + offsetY).coerceIn(offsetY, offsetY + drawHeight)

            drawYoloBoundingBox(
                textMeasurer = textMeasurer,
                className = box.className,
                score = box.score,
                left = left,
                top = top,
                right = right,
                bottom = bottom
            )
        }
    }
}