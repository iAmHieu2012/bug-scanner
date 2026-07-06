package hcmus.bugscanner.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import hcmus.bugscanner.core.utils.TimeUtils.formatTimestamp
import hcmus.bugscanner.domain.model.ConfidencePolicy
import hcmus.bugscanner.domain.model.HarmfulnessPolicy
import hcmus.bugscanner.domain.model.ScanHistory
import hcmus.bugscanner.domain.model.ScanSource
import hcmus.bugscanner.ui.components.BugImage
import hcmus.bugscanner.ui.components.EmptyState
import hcmus.bugscanner.ui.components.ScreenHeader
import org.koin.compose.viewmodel.koinViewModel

/**
 * Màn hình hiển thị danh sách lịch sử nhận diện của người dùng.
 * Hỗ trợ Adaptive Layout: Tự động chuyển đổi giữa danh sách dọc (Mobile) và dạng lưới (Web/Tablet).
 *
 * @param historyViewModel ViewModel chịu trách nhiệm lấy dữ liệu lịch sử từ Database/API.
 * @param onItemClick Callback kích hoạt khi người dùng nhấn vào một thẻ lịch sử.
 */
@Composable
fun HistoryScreen(
    historyViewModel: HistoryViewModel = koinViewModel(),
    onItemClick: (ScanHistory) -> Unit = {}
) {
    val historyList by historyViewModel.historyList.collectAsState()
    val isSavingHistory by historyViewModel.isSavingHistory.collectAsState()
    val saveMessage by historyViewModel.saveMessage.collectAsState()

    LaunchedEffect(Unit) {
        historyViewModel.fetchHistory()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 24.dp, start = 16.dp, end = 16.dp)
    ) {
        ScreenHeader(
            title = "Lịch sử khám phá",
            subtitle = "Những loài côn trùng bạn đã tìm thấy",
            leadingIcon = Icons.Rounded.History
        )

        if (isSavingHistory || saveMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = if (saveMessage?.startsWith("Chưa") == true) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSavingHistory) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = if (isSavingHistory) "Đang lưu kết quả nhận diện..." else saveMessage.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (saveMessage?.startsWith("Chưa") == true) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (historyList.isEmpty()) {
            EmptyState(
                text = "Bạn chưa lưu côn trùng nào.\nHãy dùng Camera để khám phá nhé! 🌿",
                isError = false
            )
        } else {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                if (maxWidth > 600.dp) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 300.dp),
                        contentPadding = PaddingValues(bottom = 100.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(historyList) { item: ScanHistory ->
                            HistoryItemCard(item, onClick = { onItemClick(item) })
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(historyList) { item: ScanHistory ->
                            HistoryItemCard(item, onClick = { onItemClick(item) })
                        }
                    }
                }
            }
        }
    }
}

/**
 * Thẻ (Card) hiển thị một bản ghi (record) trong lịch sử kèm theo hình ảnh nhận diện thực tế.
 *
 * @param item Khối dữ liệu chứa thông tin của một lần nhận diện.
 * @param onClick Hàm kích hoạt khi nhấn vào thẻ để xem chi tiết.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryItemCard(item: ScanHistory, onClick: () -> Unit) {
    val dateString = formatTimestamp(item.timestamp)
    val confidence = ConfidencePolicy.explain(item.confidence)
    val harmfulness = HarmfulnessPolicy.fromValue(item.harmfulnessLevel)
    val source = ScanSource.fromValue(item.source)

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BugImage(
                imageUrl = item.imageUrl,
                contentDescription = "Ảnh chụp ${item.bugName}",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.bugName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.AccessTime,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = dateString,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HistoryBadge(if (item.confidence > 0f) "${confidence.shortLabel} ${confidence.percentText}" else "Chưa có độ tin cậy")
                    HistoryBadge(harmfulness.shortLabel)
                    HistoryBadge(source.userFacingName)
                }
            }

            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = "Xem chi tiết",
                tint = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}

@Composable
private fun HistoryBadge(text: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
    }
}
