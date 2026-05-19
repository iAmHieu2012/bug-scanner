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
import hcmus.bugscanner.ui.theme.DeepForest
import hcmus.bugscanner.ui.theme.SeedGreen
import kotlin.math.abs

/**
 * Hàm sinh màu tự động và CỐ ĐỊNH cho từng loại côn trùng dựa vào tên của nó.
 */
fun getBugColor(className: String): Color {
    val colors = listOf(
        Color(0xFFE53935), // Đỏ
        Color(0xFF43A047), // Xanh lá
        Color(0xFF1E88E5), // Xanh dương
        Color(0xFFFFB300), // Vàng
        Color(0xFF8E24AA), // Tím
        Color(0xFF00ACC1), // Xanh lơ
        Color(0xFFF4511E), // Cam
        Color(0xFF7CB342), // Xanh lục nhạt
        Color(0xFF3949AB)  // Chàm
    )
    return colors[abs(className.hashCode()) % colors.size]
}

/**
 * Bảng hiển thị danh sách kết quả nhận diện côn trùng bên dưới màn hình quét.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetectionPanel(
    frameResult: FrameResult?,
    onBugClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Nhóm các box theo tên côn trùng. Trả về Pair(số lượng con, độ chính xác MAX của loài đó)
    val detectionSummary = frameResult?.boxes?.groupBy { it.className }?.mapValues { entry ->
        val count = entry.value.size
        val maxScore = entry.value.maxOf { it.score }
        Pair(count, maxScore)
    } ?: emptyMap()

    val totalBugs = detectionSummary.values.sumOf { it.first }
    val isInitial = frameResult == null || frameResult.sourceWidth == 0

    Surface(
        modifier = modifier.fillMaxWidth().height(250.dp).padding(top = 16.dp),
        color = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        shadowElevation = 16.dp
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Kết quả phát hiện",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = DeepForest
                )
                if (detectionSummary.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    Badge(containerColor = SeedGreen) {
                        Text("$totalBugs", color = Color.White)
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
                        val bugColor = getBugColor(name) // Lấy màu của loài này

                        Card(
                            onClick = { onBugClick(name) },
                            modifier = Modifier.padding(vertical = 4.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9))
                        ) {
                            ListItem(
                                leadingContent = {
                                    // Áp dụng đúng màu khung BoundingBox cho icon để người dùng dễ dàng so sánh
                                    Icon(Icons.Rounded.Eco, contentDescription = null, tint = bugColor)
                                },
                                headlineContent = { Text(name, fontWeight = FontWeight.SemiBold) },
                                supportingContent = {
                                    // Hiển thị Score (Độ chính xác) ở đây
                                    Text("Độ chính xác: ${(maxScore * 100).toInt()}%", color = Color.Gray, fontSize = 13.sp)
                                },
                                trailingContent = { Text("x$count", fontWeight = FontWeight.Bold, color = DeepForest) },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )
                        }
                    }
                }
            }
        }
    }
}