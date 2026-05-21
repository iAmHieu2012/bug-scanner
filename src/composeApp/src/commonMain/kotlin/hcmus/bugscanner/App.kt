package hcmus.bugscanner

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import hcmus.bugscanner.ui.navigation.AppNavigation

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun App() {
    // Bao bọc toàn bộ App bằng BoxWithConstraints để lấy được chính xác
    // maxWidth và maxHeight của màn hình/cửa sổ hiện tại.
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {

        // Tự tính toán WindowSizeClass đa nền tảng dựa trên kích thước thực tế.
        val windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(maxWidth, maxHeight))

        // Truyền xuống Router điều hướng
        AppNavigation(windowSizeClass = windowSizeClass)
    }
}