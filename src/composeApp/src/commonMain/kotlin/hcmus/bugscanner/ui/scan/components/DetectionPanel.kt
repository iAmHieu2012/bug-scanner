package hcmus.bugscanner.ui.scan.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hcmus.bugscanner.domain.model.FrameResult
import hcmus.bugscanner.domain.model.ConfidencePolicy
import hcmus.bugscanner.ml.YoloConstants
import hcmus.bugscanner.ui.scan.ScanMode
import hcmus.bugscanner.ui.scan.utils.getBugColor

/**
 * Bảng hiển thị kết quả nhận diện côn trùng thời gian thực hoặc offline (YOLO).
 * Cung cấp chức năng xem chi tiết và hướng dẫn chụp lại nếu nhận diện offline không đạt yêu cầu.
 *
 * @param currentMode Chế độ quét hiện tại (LIVE hoặc tĩnh).
 * @param isScanningLive Trạng thái camera đang chạy trực tiếp.
 * @param frameResult Kết quả tọa độ phân tích AI thu được từ frame hình ảnh.
 * @param imageBytesToSave Mảng byte hình ảnh để lưu cùng kết quả nhận diện.
 * @param onBugClick Callback kích hoạt khi nhấn chọn một kết quả côn trùng cụ thể.
 * @param modifier Modifier tùy chỉnh bố cục.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetectionPanel(
    currentMode: ScanMode,
    isScanningLive: Boolean,
    frameResult: FrameResult?,
    imageBytesToSave: ByteArray?,
    isAnalyzingFallback: Boolean = false,
    fallbackErrorMessage: String? = null,
    onFallbackClick: () -> Unit = {},
    onBugClick: (className: String, displayName: String, confidence: Float, imageBytes: ByteArray?) -> Unit,
    modifier: Modifier = Modifier
) {
    val detectionSummary = remember(frameResult) {
        frameResult?.boxes?.groupBy { it.className }?.mapValues { entry ->
            val count = entry.value.size
            val maxScore = entry.value.maxOf { it.score }
            Pair(count, maxScore)
        } ?: emptyMap()
    }

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
                    Icon(
                        imageVector = Icons.Rounded.CameraAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Không nhận diện rõ côn trùng",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Hãy chụp lại gần hơn, rõ nét hơn hoặc chọn ảnh khác.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onFallbackClick,
                        enabled = !isAnalyzingFallback
                    ) {
                        if (isAnalyzingFallback) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text("Tra cứu iNaturalist")
                    }
                    fallbackErrorMessage?.takeIf { it.isNotBlank() }?.let { message ->
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else {
                LazyColumn {
                    items(detectionSummary.toList()) { (name, stats) ->
                        val count = stats.first
                        val maxScore = stats.second
                        val confidence = ConfidencePolicy.explain(maxScore)
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
                                supportingContent = {
                                    Column {
                                        Text(
                                            "Độ tin cậy: ${confidence.percentText} - ${confidence.shortLabel}",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                                            fontSize = 13.sp
                                        )
                                        if (maxScore < 0.5f) {
                                            Text(
                                                confidence.guidance,
                                                color = MaterialTheme.colorScheme.error,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                },
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
