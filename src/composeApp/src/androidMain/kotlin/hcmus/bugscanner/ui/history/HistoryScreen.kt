package hcmus.bugscanner.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import hcmus.bugscanner.domain.model.ScanHistory
import hcmus.bugscanner.ui.components.EmptyState
import java.text.SimpleDateFormat
import java.util.*

/**
 * Màn hình hiển thị danh sách lịch sử nhận diện của người dùng.
 */
@Composable
fun HistoryScreen(historyViewModel: HistoryViewModel = viewModel()) {
    val historyList by historyViewModel.historyList.collectAsState()

    LaunchedEffect(Unit) {
        historyViewModel.fetchHistory()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F8E9))
            .padding(top = 24.dp, start = 16.dp, end = 16.dp)
    ) {
        Text(
            text = "Lịch sử khám phá",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFF1B5E20)
        )
        Text(
            text = "Những loài côn trùng bạn đã tìm thấy",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (historyList.isEmpty()) {
            EmptyState(
                text = "Bạn chưa lưu côn trùng nào.\nHãy dùng Camera để khám phá nhé! 🌿",
                isError = false
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                items(historyList) { item ->
                    HistoryItemCard(item)
                }
            }
        }
    }
}

/**
 * Thẻ hiển thị một bản ghi trong lịch sử.
 */
@Composable
fun HistoryItemCard(item: ScanHistory) {
    val dateFormat = SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault())
    val dateString = item.timestamp?.let { dateFormat.format(it) } ?: "Đang cập nhật..."

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFFE8F5E9),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Rounded.BugReport,
                    contentDescription = null,
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.bugName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF1B5E20)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.AccessTime,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = dateString,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = "Xem chi tiết",
                tint = Color.LightGray
            )
        }
    }
}