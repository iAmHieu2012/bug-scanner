package hcmus.bugscanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import hcmus.bugscanner.ui.scan.AndroidScanProvider
import hcmus.bugscanner.ui.scan.LocalPlatformScanProvider

/**
 * Activity gốc (Entry Point) của ứng dụng BugScanner trên nền tảng Android.
 * Khởi tạo giao diện Compose và tiêm (inject) các thành phần đặc thù của Android vào cây giao diện chung.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Kích hoạt chế độ tràn viền màn hình (Edge-to-Edge), loại bỏ viền đen ở thanh trạng thái (Status Bar)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            // Cung cấp AndroidScanProvider (chứa logic CameraX, TFLite của Android)
            // cho toàn bộ cấu trúc Multiplatform bên trong App() sử dụng.
            CompositionLocalProvider(
                LocalPlatformScanProvider provides AndroidScanProvider
            ) {
                App()
            }
        }
    }
}