package hcmus.bugscanner.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Hiển thị trạng thái trống (Empty State) hoặc thông báo lỗi cho các màn hình danh sách.
 * Sử dụng Box căn giữa tự động giúp tương thích với mọi kích thước giao diện.
 *
 * @param text Nội dung thông báo hiển thị cho người dùng.
 * @param isError Cờ xác định có phải là thông báo lỗi hay không (hiển thị màu đỏ nếu true).
 */
@Composable
fun EmptyState(text: String, isError: Boolean = false) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}