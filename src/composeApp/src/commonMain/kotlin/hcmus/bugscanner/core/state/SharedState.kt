package hcmus.bugscanner.core.state

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

val SeedGreen = Color(0xFF2E7D32)
val DeepForest = Color(0xFF1B5E20)

/**
 * Hiển thị trạng thái trống hoặc thông báo lỗi cho các màn hình.
 */
@Composable
fun EmptyState(text: String, isError: Boolean = false) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isError) MaterialTheme.colorScheme.error else Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

/**
 * Màn hình yêu cầu đăng nhập để chặn người dùng Khách truy cập tính năng.
 */
@Composable
fun RequireAuthScreen(onAuthAction: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF1F8E9)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Rounded.Lock,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = Color.Gray
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Yêu cầu đăng nhập",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = DeepForest
            )
            Text(
                "Tính năng Lịch sử chỉ dành cho thành viên đã đăng nhập để có thể lưu trữ và đồng bộ dữ liệu bảo mật.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            Button(
                onClick = onAuthAction,
                colors = ButtonDefaults.buttonColors(containerColor = SeedGreen),
                modifier = Modifier.height(48.dp)
            ) {
                Icon(Icons.AutoMirrored.Rounded.Login, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Đăng nhập / Đăng ký ngay", fontWeight = FontWeight.Bold)
            }
        }
    }
}