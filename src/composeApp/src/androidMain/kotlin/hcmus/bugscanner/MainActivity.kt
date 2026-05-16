package hcmus.bugscanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import hcmus.bugscanner.ml.YoloConstants
import hcmus.bugscanner.ml.YoloDetector
import hcmus.bugscanner.ui.navigation.AppNavigation
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Activity chính khởi tạo các tác vụ AI và điều hướng UI.
 */
class MainActivity : ComponentActivity() {
    private lateinit var yoloDetector: YoloDetector
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        cameraExecutor = Executors.newSingleThreadExecutor()
        yoloDetector = YoloDetector(this, YoloConstants.MODEL_PATH)

        setContent {
            MaterialTheme {
                AppNavigation(yoloDetector, cameraExecutor)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        yoloDetector.close()
        cameraExecutor.shutdown()
    }
}