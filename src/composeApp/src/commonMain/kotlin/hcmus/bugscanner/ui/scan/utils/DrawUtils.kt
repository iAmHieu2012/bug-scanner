package hcmus.bugscanner.ui.scan.utils

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Size as ComposeSize

/**
 * Hàm sinh màu tự động và cố định (deterministic) cho từng loại côn trùng dựa vào mã băm (hash) của tên loài.
 * Đảm bảo mỗi loài luôn có một màu sắc nhận diện nhất quán trên giao diện.
 *
 * @param className Tên phân loại của côn trùng.
 * @return [Color] Màu sắc đại diện.
 */
fun getBugColor(className: String): Color {
    val colors = listOf(
        Color(0xFFE53935), // Đỏ
        Color(0xFFFF9800), // Cam
        Color(0xFFFFC107), // Vàng
        Color(0xFF4CAF50), // Lục
        Color(0xFF2196F3), // Lam
        Color(0xFF9C27B0), // Tím
        Color(0xFFE91E63), // Hồng
        Color(0xFF00C8C8), // Xanh ngọc
        Color(0xFF795548)  // Nâu
    )
    return colors[(className.hashCode() and 0x7FFFFFFF) % colors.size]
}

/**
 * Hàm mở rộng (Extension function) của [DrawScope] hỗ trợ vẽ khung giới hạn (Bounding Box)
 * và nhãn tên (Label) cho các đối tượng được AI nhận diện trên Canvas.
 *
 * @param textMeasurer Đối tượng hỗ trợ đo lường kích thước văn bản trước khi vẽ.
 * @param className Tên của đối tượng (loài côn trùng) được nhận diện.
 * @param score Điểm tin cậy (Confidence Score) của dự đoán.
 * @param left Tọa độ X của góc trên bên trái Bounding Box.
 * @param top Tọa độ Y của góc trên bên trái Bounding Box.
 * @param right Tọa độ X của góc dưới bên phải Bounding Box.
 * @param bottom Tọa độ Y của góc dưới bên phải Bounding Box.
 */
fun DrawScope.drawYoloBoundingBox(
    textMeasurer: TextMeasurer,
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