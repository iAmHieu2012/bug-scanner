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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hcmus.bugscanner.domain.model.FrameResult
import hcmus.bugscanner.ml.YoloConstants
import hcmus.bugscanner.ui.scan.ScanMode
import hcmus.bugscanner.ui.scan.utils.getBugColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetectionPanel(
    currentMode: ScanMode,
    isScanningLive: Boolean,
    frameResult: FrameResult?,
    imageBytesToSave: ByteArray?,
    onBugClick: (className: String, displayName: String, confidence: Float, imageBytes: ByteArray?) -> Unit,
    isAnalyzingFallback: Boolean,
    fallbackErrorMessage: String?,
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
        modifier = modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 5.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Kết quả phát hiện",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isLiveActive) "Đang quét trực tiếp" else "Khung hình hiện tại",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (detectionSummary.isNotEmpty()) {
                    Badge(containerColor = MaterialTheme.colorScheme.primary) {
                        Text("$totalBugs", color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (isInitial) {
                Text(
                    text = "Đưa côn trùng vào khung camera hoặc chọn ảnh từ thư viện.",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, bottom = 28.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            } else if (isYoloFailed && imageBytesToSave != null) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Không nhận diện rõ côn trùng",
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
                    if (!fallbackErrorMessage.isNullOrBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = fallbackErrorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
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
                            onClick = { onBugClick(name, displayVietnameseName, maxScore, imageBytesToSave) },
                            modifier = Modifier.padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
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
