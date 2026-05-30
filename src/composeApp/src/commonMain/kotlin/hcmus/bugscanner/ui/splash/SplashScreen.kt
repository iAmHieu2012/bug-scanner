package hcmus.bugscanner.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/**
 * Màn hình Splash hiển thị đầu tiên khi ứng dụng khởi chạy.
 * Tích hợp hiệu ứng phóng to (scale animation) cho logo và thông điệp.
 * Tự động chuyển hướng sau khi hoàn tất chuỗi hiệu ứng và thời gian chờ.
 *
 * @param onSplashFinished Callback được gọi khi animation và thời gian chờ kết thúc,
 * dùng để báo cho AppNavigation biết để chuyển sang màn hình tiếp theo.
 */
@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    // Khởi tạo giá trị scale ban đầu là 0.5f (thu nhỏ 50% so với kích thước gốc).
    // Sử dụng remember để Compose ghi nhớ trạng thái này qua các chu kỳ Recomposition.
    val scale = remember { Animatable(0.5f) }

    // LaunchedEffect với key1 = true đảm bảo block code bên trong chỉ được kích hoạt
    // MỘT LẦN duy nhất khi SplashScreen được đưa vào Composition (lần render đầu tiên).
    LaunchedEffect(key1 = true) {
        // 1. Chạy hiệu ứng phóng to từ 0.5f lên 1.2f
        scale.animateTo(
            targetValue = 1.2f,
            // Sử dụng tween để tạo chuyển động tuyến tính mượt mà trong 800ms
            animationSpec = tween(durationMillis = 800)
        )

        // 2. Tạm dừng coroutine thêm 1000ms sau khi animation kết thúc
        // để người dùng có đủ thời gian tiếp nhận thông điệp trên màn hình.
        delay(1000.milliseconds)

        // 3. Kích hoạt callback để rẽ nhánh điều hướng (thường là sang AuthScreen hoặc HomeScreen)
        onSplashFinished()
    }

    // Box là container gốc, chiếm toàn bộ kích thước khả dụng và phủ màu nền chính (primary).
    // Căn giữa (Center) toàn bộ các thành phần con bên trong.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Vùng chứa Logo: Sử dụng Surface để dễ dàng bo tròn (CircleShape) và đổ màu nền.
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .size(120.dp)
                    // Áp dụng giá trị scale (đang biến thiên từ LaunchedEffect) vào Modifier
                    .scale(scale.value)
            ) {
                Icon(
                    imageVector = Icons.Rounded.BugReport,
                    contentDescription = "App Logo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Tiêu đề ứng dụng
            Text(
                text = "BugScanner",
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.scale(scale.value) // Đồng bộ hiệu ứng scale với Logo
            )

            // Câu khẩu hiệu (Slogan)
            Text(
                text = "Khám phá thế giới côn trùng",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f), // Giảm alpha xuống 80% để tạo phân cấp thị giác
                modifier = Modifier.scale(scale.value) // Đồng bộ hiệu ứng scale với Logo
            )
        }
    }
}