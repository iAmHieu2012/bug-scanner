package hcmus.bugscanner.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import hcmus.bugscanner.ui.theme.AppIcon
import hcmus.bugscanner.ui.theme.IconBugscanner
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/**
 * Component hiển thị màn hình chờ (Splash Screen) khi ứng dụng vừa khởi chạy.
 * Xử lý hiệu ứng thị giác (Scale Animation) và tự động điều hướng sau một khoảng thời gian chờ định trước.
 *
 * @param onSplashFinished Callback kích hoạt khi hoàn tất hiệu ứng, dùng để trigger luồng điều hướng tiếp theo.
 */
@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    val scale = remember { Animatable(0.5f) }

    LaunchedEffect(key1 = true) {
        scale.animateTo(
            targetValue = 1.2f,
            animationSpec = tween(durationMillis = 800)
        )
        delay(1000.milliseconds)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale.value)
            ) {
                Image(
                    imageVector = AppIcon.IconBugscanner,
                    contentDescription = "App Logo",
                    modifier = Modifier.padding(24.dp).fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "BugScanner",
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.scale(scale.value)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Khám phá thế giới côn trùng",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                modifier = Modifier.scale(scale.value)
            )
        }
    }
}
