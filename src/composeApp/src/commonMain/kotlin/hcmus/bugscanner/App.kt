package hcmus.bugscanner

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import hcmus.bugscanner.core.di.appModule
import hcmus.bugscanner.ui.navigation.AppNavigation
import org.koin.compose.KoinApplication
import org.koin.dsl.koinConfiguration

/**
 * Điểm bắt đầu (Root Composable) của toàn bộ ứng dụng BugScanner.
 * Chịu trách nhiệm đo lường kích thước hiển thị thực tế của thiết bị và khởi tạo luồng điều hướng.
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun App() {
    // Sử dụng đúng cấu hình koinConfiguration mới mà anh tìm ra
    KoinApplication(
        configuration = koinConfiguration {
            modules(appModule)
        }
    ) {
        // Bao bọc toàn bộ App bằng BoxWithConstraints để lấy được chính xác
        // maxWidth và maxHeight của màn hình/cửa sổ hiện tại.
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {

            // Tự tính toán WindowSizeClass đa nền tảng dựa trên kích thước thực tế.
            // Giúp giao diện linh hoạt chuyển đổi (Adaptive Layout) giữa Mobile, Tablet và Web.
            val windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(maxWidth, maxHeight))

            // Truyền cấu hình kích thước xuống Router điều hướng trung tâm
            AppNavigation(windowSizeClass = windowSizeClass)
        }
    }
}