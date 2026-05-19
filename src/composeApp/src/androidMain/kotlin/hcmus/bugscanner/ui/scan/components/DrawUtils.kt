package hcmus.bugscanner.ui.scan.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Size as ComposeSize

/**
 * Các hàm hỗ trợ vẽ giao diện (Canvas) cho tính năng Scan.
 */
fun DrawScope.drawYoloBoundingBox(
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    className: String,
    score: Float,
    left: Float,
    top: Float,
    right: Float,
    bottom: Float
) {
    val width = right - left
    val height = bottom - top

    if (width <= 0f || height <= 0f) return

    // Lấy màu riêng cho từng con bọ từ hàm dùng chung
    val boxColor = getBugColor(className)

    drawRoundRect(
        color = boxColor,
        topLeft = Offset(left, top),
        size = ComposeSize(width, height),
        cornerRadius = CornerRadius(16f, 16f),
        style = Stroke(width = 5f)
    )

    val textStyle = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    val textLayoutResult = textMeasurer.measure(text = className, style = textStyle)
    val textWidth = textLayoutResult.size.width.toFloat()
    val textHeight = textLayoutResult.size.height.toFloat()

    val labelTop = maxOf(0f, top - textHeight - 12f)

    drawRoundRect(
        color = boxColor.copy(alpha = 0.85f),
        topLeft = Offset(left, labelTop),
        size = ComposeSize(textWidth + 24f, textHeight + 12f),
        cornerRadius = CornerRadius(12f, 12f)
    )

    drawText(
        textLayoutResult = textLayoutResult,
        color = Color.White,
        topLeft = Offset(left + 12f, labelTop + 6f)
    )
}