package hcmus.bugscanner.ui.scan

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import hcmus.bugscanner.ml.YoloDetector
import java.util.concurrent.ExecutorService

/**
 * ViewModel xử lý chức năng quét và nhận diện côn trùng.
 */
class ScanViewModel(
    private val yoloDetector: YoloDetector,
    val cameraExecutor: ExecutorService
) : ViewModel() {

    val frameResult = yoloDetector.frameResult

    fun analyzeImage(bitmap: Bitmap, rotation: Int = 0) {
        yoloDetector.analyze(bitmap, rotation)
    }

    fun clearResult() {
        yoloDetector.clearResult()
    }

    override fun onCleared() {
        super.onCleared()
        yoloDetector.close()
        cameraExecutor.shutdown()
    }
}