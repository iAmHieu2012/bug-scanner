package hcmus.bugscanner.ui.scan.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
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

    drawRect(
        color = Color.Green,
        topLeft = Offset(left, top),
        size = ComposeSize(width, height),
        style = Stroke(width = 8f)
    )
    drawText(
        textMeasurer = textMeasurer,
        text = "$className ${(score * 100).toInt()}%",
        topLeft = Offset(left, maxOf(0f, top - 50f)),
        style = TextStyle(color = Color.White, fontSize = 20.sp, background = Color.Red)
    )
}