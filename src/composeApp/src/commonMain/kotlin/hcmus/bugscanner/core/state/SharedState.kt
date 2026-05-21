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
import androidx.compose.ui.text.font.FontWeight
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

/**
 * Màn hình chặn truy cập (Paywall/Authwall) yêu cầu người dùng phải đăng nhập tài khoản thật
 * để sử dụng các tính năng cao cấp (ví dụ: Lưu lịch sử, đồng bộ cloud).
 * * Tích hợp giới hạn chiều rộng để hiển thị đẹp mắt trên màn hình Web/Desktop.
 *
 * @param onAuthAction Callback kích hoạt quy trình đăng xuất tài khoản Guest và chuyển về màn AuthScreen.
 */
@Composable
fun RequireAuthScreen(onAuthAction: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            // Giới hạn widthIn giúp đoạn văn bản không bị kéo dài lê thê trên màn hình ngang,
            // đảm bảo tính dễ đọc (readability).
            modifier = Modifier
                .padding(32.dp)
                .widthIn(max = 400.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = "Authentication Required", // Đã bổ sung nhãn cho Screen Reader
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Yêu cầu đăng nhập",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Tính năng Lịch sử chỉ dành cho thành viên đã đăng nhập để có thể lưu trữ và đồng bộ dữ liệu bảo mật.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            Button(
                onClick = onAuthAction,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth() // Nút bấm sẽ dãn full chiều rộng khả dụng (max 400dp)
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Login,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Đăng nhập / Đăng ký ngay",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}