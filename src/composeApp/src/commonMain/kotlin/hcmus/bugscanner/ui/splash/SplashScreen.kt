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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/**
 * Màn hình khởi động của ứng dụng.
 */
@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    val scale = remember { Animatable(0.5f) }
    val primaryGreen = Color(0xFF2E7D32)

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
            .background(primaryGreen),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = CircleShape,
                color = Color.White,
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale.value)
            ) {
                Icon(
                    Icons.Rounded.BugReport,
                    contentDescription = "App Logo",
                    tint = primaryGreen,
                    modifier = Modifier.padding(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "BugScanner",
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                modifier = Modifier.scale(scale.value)
            )
            Text(
                text = "Khám phá thế giới côn trùng",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFC8E6C9),
                modifier = Modifier.scale(scale.value)
            )
        }
    }
}

/**
 * Màn hình yêu cầu cấp quyền Camera.
 */
@Composable
fun CameraPermissionScreen(onRequestPermission: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF1F8E9)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Rounded.BugReport, contentDescription = null, modifier = Modifier.size(80.dp), tint = Color.Gray)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Cần quyền Camera", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "BugScanner cần sử dụng Camera để nhận diện côn trùng. Vui lòng cấp quyền để tiếp tục.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) {
                Text("Cấp quyền ngay")
            }
        }
    }
}