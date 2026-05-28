package hcmus.bugscanner.ui.scan.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hcmus.bugscanner.domain.model.FrameResult
import hcmus.bugscanner.core.state.EmptyState

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
 * Bảng điều khiển (Panel) hiển thị danh sách thống kê kết quả nhận diện từ AI.
 * Tự động nhóm các sinh vật cùng loại, đếm số lượng và lấy độ chính xác cao nhất.
 *
 * @param frameResult Kết quả phân tích Bounding Box và Confidence Score từ mô hình AI.
 * @param imageBytesToSave Mảng byte của hình ảnh hiện tại chứa sinh vật, dùng để lưu trữ lên Cloud.
 * @param onBugClick Callback kích hoạt khi người dùng nhấn vào thẻ của một con bọ để xem chi tiết (truyền kèm tên và ảnh).
 * @param modifier Modifier tùy chỉnh kích thước, vị trí từ component cha.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetectionPanel(
    frameResult: FrameResult?,
    imageBytesToSave: ByteArray?,
    onBugClick: (String, ByteArray?) -> Unit,
    modifier: Modifier = Modifier
) {
    val detectionSummary = frameResult?.boxes?.groupBy { it.className }?.mapValues { entry ->
        val count = entry.value.size
        val maxScore = entry.value.maxOf { it.score }
        Pair(count, maxScore)
    } ?: emptyMap()

    val totalBugs = detectionSummary.values.sumOf { it.first }
    val isInitial = frameResult == null || frameResult.sourceWidth == 0

    Surface(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(32.dp),
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(24.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Kết quả phát hiện",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (detectionSummary.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    Badge(containerColor = MaterialTheme.colorScheme.primary) {
                        Text("$totalBugs", color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (isInitial) {
                EmptyState("Đang chờ hình ảnh... \uD83D\uDD0D")
            } else if (detectionSummary.isEmpty()) {
                EmptyState("Không tìm thấy côn trùng nào! \uD83D\uDC1C", isError = true)
            } else {
                LazyColumn {
                    items(detectionSummary.toList()) { (name, stats) ->
                        val count = stats.first
                        val maxScore = stats.second
                        val bugColor = getBugColor(name)

                        Card(
                            onClick = { onBugClick(name, imageBytesToSave) },
                            modifier = Modifier.padding(vertical = 4.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            ListItem(
                                leadingContent = { Icon(Icons.Rounded.Eco, null, tint = bugColor) },
                                headlineContent = { Text(name, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                supportingContent = { Text("Độ chính xác: ${(maxScore * 100).toInt()}%", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 13.sp) },
                                trailingContent = { Text("x$count", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )
                        }
                    }
                }
            }
        }
    }
}