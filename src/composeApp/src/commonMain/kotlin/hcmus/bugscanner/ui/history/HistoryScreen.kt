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
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import hcmus.bugscanner.domain.model.ScanHistory
import hcmus.bugscanner.core.state.EmptyState
import hcmus.bugscanner.core.utils.formatTimestamp

/**
 * Màn hình hiển thị danh sách lịch sử nhận diện của người dùng.
 * Tích hợp tự động đo lường kích thước (BoxWithConstraints) để chuyển đổi từ danh sách dọc (List)
 * sang dạng lưới (Grid) trên các màn hình kích thước lớn (Web/Tablet).
 *
 * @param historyViewModel ViewModel chịu trách nhiệm lấy dữ liệu lịch sử từ Database/API.
 */
@Composable
fun HistoryScreen(historyViewModel: HistoryViewModel = viewModel { HistoryViewModel() }) {
    // Thu thập trạng thái dữ liệu từ ViewModel
    val historyList by historyViewModel.historyList.collectAsState()

    // Trigger lấy dữ liệu ngay khi màn hình vừa được khởi tạo
    LaunchedEffect(Unit) {
        historyViewModel.fetchHistory()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 24.dp, start = 16.dp, end = 16.dp)
    ) {
        // --- Vùng Header ---
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

        // --- Vùng Nội Dung ---
        if (historyList.isEmpty()) {
            EmptyState(
                text = "Bạn chưa lưu côn trùng nào.\nHãy dùng Camera để khám phá nhé! 🌿",
                isError = false
            )
        } else {
            // Sử dụng BoxWithConstraints để quyết định kiểu dàn trang (Layout Manager)
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                if (maxWidth > 600.dp) {
                    // LAYOUT MÀN HÌNH RỘNG: Sử dụng Grid thay vì List thẳng đứng để tránh dãn Card
                    // Adaptive Grid: Mỗi thẻ sẽ rộng ít nhất 300dp, Compose sẽ tự tính số cột phù hợp
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 300.dp),
                        contentPadding = PaddingValues(bottom = 100.dp), // Chừa không gian cho Menu điều hướng
                        horizontalArrangement = Arrangement.spacedBy(16.dp), // Khoảng cách giữa các cột
                        verticalArrangement = Arrangement.spacedBy(12.dp)  // Khoảng cách giữa các dòng
                    ) {
                        items(historyList) { item: ScanHistory ->
                            HistoryItemCard(item)
                        }
                    }
                } else {
                    // LAYOUT MÀN HÌNH HẸP (Mobile): Dùng List thẳng đứng truyền thống
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(historyList) { item: ScanHistory ->
                            HistoryItemCard(item)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Thẻ (Card) hiển thị một bản ghi (record) trong lịch sử.
 * Được thiết kế nhỏ gọn với icon, tên và thời gian quét.
 *
 * @param item Khối dữ liệu chứa thông tin của một lần nhận diện.
 */
@Composable
fun HistoryItemCard(item: ScanHistory) {
    val dateString = formatTimestamp(item.timestamp)

    Card(
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
            // Icon đại diện nằm trong một vùng chứa bo tròn
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.BugReport,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Cụm văn bản thông tin chính
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

            // Biểu tượng chỉ báo (Indicator) cho biết thẻ có thể tương tác/nhấn vào
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = "Xem chi tiết",
                tint = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}