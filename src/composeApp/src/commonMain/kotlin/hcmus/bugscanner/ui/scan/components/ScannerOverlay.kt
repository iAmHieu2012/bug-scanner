package hcmus.bugscanner.ui.scan.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp

/**
 * Lớp phủ đồ họa (Overlay) vẽ 4 góc viền ngắm (Viewfinder) đặc trưng của các ứng dụng Scanner.
 * Đã được nâng cấp để có hiệu ứng Laser quét sinh động sử dụng màu sắc Primary của Theme.
 */
@Composable
fun ScannerOverlay() {
    val infiniteTransition = rememberInfiniteTransition()
    val laserY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val primaryColor = MaterialTheme.colorScheme.primary

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize().padding(60.dp)) {
            val strokeWidth = 10f
            val cornerLength = 60f
            val halfStroke = strokeWidth / 2f
            val color = primaryColor.copy(alpha = 0.9f)
            val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)

            fun drawCorner(first: Offset, second: Offset, third: Offset) {
                val path = Path().apply {
                    moveTo(first.x, first.y)
                    lineTo(second.x, second.y)
                    lineTo(third.x, third.y)
                }
                drawPath(path = path, color = color, style = stroke)
            }

            drawCorner(Offset(halfStroke, cornerLength), Offset(halfStroke, halfStroke), Offset(cornerLength, halfStroke))
            drawCorner(Offset(size.width - cornerLength, halfStroke), Offset(size.width - halfStroke, halfStroke), Offset(size.width - halfStroke, cornerLength))
            drawCorner(Offset(halfStroke, size.height - cornerLength), Offset(halfStroke, size.height - halfStroke), Offset(cornerLength, size.height - halfStroke))
            drawCorner(Offset(size.width - cornerLength, size.height - halfStroke), Offset(size.width - halfStroke, size.height - halfStroke), Offset(size.width - halfStroke, size.height - cornerLength))

            // Hiệu ứng tia Laser quét dọc theo màn hình
            val currentY = size.height * laserY

            // Lớp sương sáng (Glow effect) bao quanh tia laser
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, primaryColor.copy(alpha = 0.5f), Color.Transparent),
                    startY = currentY - 40f,
                    endY = currentY + 40f
                ),
                topLeft = Offset(0f, currentY - 40f),
                size = androidx.compose.ui.geometry.Size(size.width, 80f)
            )

            // Lõi tia Laser siêu sáng
            drawLine(
                color = primaryColor,
                start = Offset(0f, currentY),
                end = Offset(size.width, currentY),
                strokeWidth = 6f
            )
        }
    }
}
