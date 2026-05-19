package hcmus.bugscanner.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 1. Cấu hình bảng màu giao diện Sáng
private val LightColorScheme = lightColorScheme(
    primary = SeedGreen,
    onPrimary = Color.White,
    secondary = DeepForest,
    background = LightBackground,
    surface = LightSurface,
    // onBackground = Color.Black,
    // onSurface = Color.Black
)

// 2. Cấu hình bảng màu giao diện Tối
private val DarkColorScheme = darkColorScheme(
    primary = SeedGreen, // Tùy chỉnh lại màu xanh cho hợp với nền đen nếu muốn
    onPrimary = Color.White,
    secondary = SoftGreen,
    background = DarkBackground,
    surface = DarkSurface,
    // onBackground = Color.White,
    // onSurface = Color.White
)

// 3. Hàm AppTheme bọc lấy toàn bộ ứng dụng
@Composable
fun AppTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Tự động chuyển đổi màu theo hệ thống của điện thoại
    val colorScheme = if (useDarkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}