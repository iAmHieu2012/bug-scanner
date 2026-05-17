package hcmus.bugscanner.ui.scan

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import hcmus.bugscanner.domain.model.FrameResult
import hcmus.bugscanner.ui.scan.components.drawYoloBoundingBox

/**
 * Màn hình xử lý và vẽ bounding box cho ảnh tĩnh (Gallery/Chụp).
 */
@Composable
fun StaticDetectionScreen(bitmap: Bitmap?, frameResult: FrameResult, modifier: Modifier = Modifier) {
    if (bitmap == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Chưa có hình ảnh để phân tích")
        }
        return
    }

    val textMeasurer = rememberTextMeasurer()
    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val imageWidth = bitmap.width.toFloat()
        val imageHeight = bitmap.height.toFloat()

        val scale = minOf(canvasWidth / imageWidth, canvasHeight / imageHeight)
        val drawWidth = imageWidth * scale
        val drawHeight = imageHeight * scale
        val offsetX = (canvasWidth - drawWidth) / 2f
        val offsetY = (canvasHeight - drawHeight) / 2f

        drawImage(
            image = bitmap.asImageBitmap(),
            dstOffset = IntOffset(offsetX.toInt(), offsetY.toInt()),
            dstSize = IntSize(drawWidth.toInt(), drawHeight.toInt())
        )

        frameResult.boxes.forEach { box ->
            val nx1 = box.x1
            val ny1 = box.y1
            val nx2 = box.x2
            val ny2 = box.y2

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