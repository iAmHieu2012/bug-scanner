package hcmus.bugscanner.ui.scan.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudUpload
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
import hcmus.bugscanner.ml.YoloConstants
import hcmus.bugscanner.ui.components.EmptyState
import hcmus.bugscanner.ui.scan.ScanMode
import hcmus.bugscanner.ui.scan.utils.getBugColor

/**
 * Bảng điều khiển (Panel) hiển thị danh sách thống kê kết quả nhận diện từ AI.
 * Tự động nhóm các sinh vật cùng loại, đếm số lượng và lấy độ chính xác cao nhất.
 * Tích hợp cơ chế dự phòng (Fallback) gọi API iNaturalist khi mô hình AI Offline không đạt độ tin cậy.
 *
 * @param currentMode Chế độ quét hiện tại.
 * @param isScanningLive Trạng thái luồng camera (true nếu đang liên tục quét, false nếu đã chốt khung hình).
 * @param frameResult Kết quả phân tích Bounding Box và Confidence Score từ mô hình AI (YOLO).
 * @param imageBytesToSave Mảng byte của hình ảnh hiện tại, dùng để gửi lên API phân tích hoặc lưu trữ.
 * @param onBugClick Callback kích hoạt khi người dùng nhấn vào thẻ của một côn trùng để xem chi tiết.
 * @param isAnalyzingFallback Trạng thái chờ gọi API dự phòng, dùng để hiển thị hiệu ứng tải.
 * @param onFallbackClick Callback kích hoạt khi người dùng yêu cầu phân tích ảnh chuyên sâu qua mạng.
 * @param modifier Modifier tùy chỉnh vị trí và kích thước từ Component cha.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetectionPanel(
    currentMode: ScanMode,
    isScanningLive: Boolean,
    frameResult: FrameResult?,
    imageBytesToSave: ByteArray?,
    onBugClick: (String, ByteArray?) -> Unit,
    isAnalyzingFallback: Boolean,
    onFallbackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val detectionSummary = frameResult?.boxes?.groupBy { it.className }?.mapValues { entry ->
        val count = entry.value.size
        val maxScore = entry.value.maxOf { it.score }
        Pair(count, maxScore)
    } ?: emptyMap()

    val totalBugs = detectionSummary.values.sumOf { it.first }
    val isInitial = frameResult == null || frameResult.sourceWidth == 0

    val isLiveActive = currentMode == ScanMode.LIVE && isScanningLive
    val highestScore = if (detectionSummary.isNotEmpty()) detectionSummary.values.maxOf { it.second } else 0f
    val isYoloFailed = !isInitial && (detectionSummary.isEmpty() || highestScore < 0.4f) && !isLiveActive

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
                EmptyState("Đang chờ hình ảnh... 🔍")
            } else if (isYoloFailed && imageBytesToSave != null) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Không nhận diện rõ côn trùng 🐛",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Button(
                        onClick = onFallbackClick,
                        enabled = !isAnalyzingFallback,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isAnalyzingFallback) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onSecondary, strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Đang phân tích ảnh...")
                        } else {
                            Icon(Icons.Rounded.CloudUpload, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Phân tích bằng AI chuyên sâu")
                        }
                    }
                }
            } else {
                LazyColumn {
                    items(detectionSummary.toList()) { (name, stats) ->
                        val count = stats.first
                        val maxScore = stats.second
                        val bugColor = getBugColor(name)
                        val displayVietnameseName = YoloConstants.BUG_DICTIONARY[name] ?: name

                        Card(
                            onClick = { onBugClick(name, imageBytesToSave) },
                            modifier = Modifier.padding(vertical = 4.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            ListItem(
                                leadingContent = { Icon(Icons.Rounded.Eco, null, tint = bugColor) },
                                headlineContent = { Text(displayVietnameseName, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant) },
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