package hcmus.bugscanner.ui.scan.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Lớp phủ đồ họa (Overlay) vẽ 4 góc viền ngắm (Viewfinder) đặc trưng của các ứng dụng Scanner.
 * Sử dụng [Canvas] để vẽ trực tiếp bằng đồ họa 2D.
 */
@Composable
fun ScannerOverlay() {
    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize().padding(60.dp)) {
            val strokeWidth = 10f
            val cornerLength = 60f
            val color = Color.White.copy(alpha = 0.8f)

            drawLine(color, Offset(0f, 0f), Offset(cornerLength, 0f), strokeWidth)
            drawLine(color, Offset(0f, 0f), Offset(0f, cornerLength), strokeWidth)
            drawLine(color, Offset(size.width, 0f), Offset(size.width - cornerLength, 0f), strokeWidth)
            drawLine(color, Offset(size.width, 0f), Offset(size.width, cornerLength), strokeWidth)
            drawLine(color, Offset(0f, size.height), Offset(cornerLength, size.height), strokeWidth)
            drawLine(color, Offset(0f, size.height), Offset(0f, size.height - cornerLength), strokeWidth)
            drawLine(color, Offset(size.width, size.height), Offset(size.width - cornerLength, size.height), strokeWidth)
            drawLine(color, Offset(size.width, size.height), Offset(size.width, size.height - cornerLength), strokeWidth)
        }
    }
}