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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import hcmus.bugscanner.domain.model.ScanHistory
import hcmus.bugscanner.core.state.EmptyState
import hcmus.bugscanner.core.utils.formatTimestamp

/**
 * Màn hình hiển thị danh sách lịch sử nhận diện của người dùng.
 * Tích hợp tự động đo lường kích thước (BoxWithConstraints) để chuyển đổi từ danh sách dọc (List)
 * sang dạng lưới (Grid) trên các màn hình kích thước lớn (Web/Tablet).
 *
 * @param historyViewModel ViewModel chịu trách nhiệm lấy dữ liệu lịch sử từ Database/API.
 * @param onItemClick Callback kích hoạt khi người dùng nhấn vào một thẻ lịch sử.
 */
@Composable
fun HistoryScreen(
    historyViewModel: HistoryViewModel = viewModel { HistoryViewModel() },
    onItemClick: (ScanHistory) -> Unit = {}
) {
    val historyList by historyViewModel.historyList.collectAsState()

    LaunchedEffect(Unit) {
        historyViewModel.fetchHistory()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 24.dp, start = 16.dp, end = 16.dp)
    ) {
        Text(
            text = "Lịch sử khám phá",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Những loài côn trùng bạn đã tìm thấy",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

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
                            // 2. Truyền sự kiện click vào thẻ
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
 * @param onClick Hàm kích hoạt khi nhấn vào thẻ.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryItemCard(item: ScanHistory, onClick: () -> Unit) {
    val dateString = formatTimestamp(item.timestamp)

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tải và hiển thị ảnh thu nhỏ (Thumbnail) từ Firebase Storage
            AsyncImage(
                model = item.imageUrl.takeIf { it.isNotBlank() } ?: "https://via.placeholder.com/150?text=No+Image",
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
            }

            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = "Xem chi tiết",
                tint = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}