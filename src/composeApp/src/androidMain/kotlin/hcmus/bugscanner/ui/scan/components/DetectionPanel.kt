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
import hcmus.bugscanner.domain.model.FrameResult
import hcmus.bugscanner.core.state.EmptyState

val SeedGreen = Color(0xFF2E7D32)
val DeepForest = Color(0xFF1B5E20)

/**
 * Bảng hiển thị danh sách kết quả nhận diện côn trùng bên dưới màn hình quét.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetectionPanel(
    frameResult: FrameResult,
    onBugClick: (String) -> Unit
) {
    val detectionCounts = frameResult.boxes.groupingBy { it.className }.eachCount()
    val isInitial = frameResult.sourceWidth == 0

    Surface(
        modifier = Modifier.fillMaxWidth().height(250.dp).padding(top = 16.dp),
        color = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        shadowElevation = 16.dp
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Kết quả phát hiện", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = DeepForest)
                if (detectionCounts.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    Badge(containerColor = SeedGreen) { Text("${detectionCounts.values.sum()}", color = Color.White) }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (isInitial) {
                EmptyState("Đang chờ hình ảnh... \uD83D\uDD0D")
            } else if (detectionCounts.isEmpty()) {
                EmptyState("Không tìm thấy côn trùng nào! \uD83D\uDC1C", isError = true)
            } else {
                LazyColumn {
                    items(detectionCounts.toList()) { (name, count) ->
                        Card(
                            onClick = { onBugClick(name) },
                            modifier = Modifier.padding(vertical = 4.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9))
                        ) {
                            ListItem(
                                leadingContent = { Icon(Icons.Rounded.Eco, contentDescription = null, tint = SeedGreen) },
                                headlineContent = { Text(name, fontWeight = FontWeight.SemiBold) },
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