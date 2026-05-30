package hcmus.bugscanner.ui.components

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
 * Màn hình chặn truy cập (Paywall/Authwall) dùng chung cho bất kỳ tính năng nào yêu cầu tài khoản.
 * Hỗ trợ tự động căn giữa và giới hạn chiều rộng hiển thị đẹp mắt trên màn hình ngang (Web/Tablet).
 *
 * @param title Tiêu đề hiển thị (Mặc định: "Yêu cầu đăng nhập").
 * @param description Đoạn văn bản giải thích lý do cần đăng nhập. Có giá trị mặc định dùng chung.
 * @param onAuthAction Callback kích hoạt quy trình chuyển hướng người dùng sang màn hình Đăng nhập/Đăng ký.
 */
@Composable
fun RequireAuthScreen(
    title: String = "Yêu cầu đăng nhập",
    description: String = "Vui lòng đăng nhập hoặc tạo tài khoản để sử dụng tính năng này và đồng bộ dữ liệu của bạn.",
    onAuthAction: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            // Giới hạn widthIn giúp đoạn văn bản không bị kéo dài trên màn hình ngang,
            // đảm bảo tính dễ đọc (readability).
            modifier = Modifier
                .padding(32.dp)
                .widthIn(max = 400.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = "Authentication Required",
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            Button(
                onClick = onAuthAction,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
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