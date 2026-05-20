package hcmus.bugscanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import hcmus.bugscanner.ui.scan.AndroidScanProvider
import hcmus.bugscanner.ui.scan.LocalPlatformScanProvider
/**
 * Activity chính khởi tạo các tác vụ AI và điều hướng UI.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            CompositionLocalProvider(
                LocalPlatformScanProvider provides AndroidScanProvider
            ) {
                App()
            }
        }
    }
}