package hcmus.bugscanner.ui.scan

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import hcmus.bugscanner.core.utils.uriToBitmap
import hcmus.bugscanner.domain.model.FrameResult
import hcmus.bugscanner.ml.YoloDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Object chứa toàn bộ cấu trúc điều phối Camera/Picker của Android
 */
object AndroidScanProvider : PlatformScanProvider {

    /**
     * Màn hình hiển thị luồng trực tiếp từ Camera native và vẽ bounding box YOLO.
     */
    @Composable
    override fun NativeCameraView(modifier: Modifier, onResult: (FrameResult) -> Unit) {
        val context = LocalContext.current
        var isReady by remember { mutableStateOf(false) }

        val yoloRef = remember { java.util.concurrent.atomic.AtomicReference<YoloDetector?>(null) }
        val executorRef = remember { java.util.concurrent.atomic.AtomicReference<ExecutorService?>(null) }

        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                yoloRef.set(YoloDetector(context))
                executorRef.set(Executors.newSingleThreadExecutor())
                isReady = true
            }
        }

        if (!isReady) {
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            val viewModel: ScanViewModel = viewModel {
                ScanViewModel(yoloRef.get()!!, executorRef.get()!!)
            }

            val frameResult by viewModel.frameResult.collectAsState()
            LaunchedEffect(frameResult) { onResult(frameResult) }

            AndroidCameraScreen(viewModel = viewModel, modifier = modifier)
        }
    }

    /**
     * Màn hình xử lý và vẽ bounding box cho ảnh tĩnh native (Gallery/Chụp).
     */
    @Composable
    override fun NativeStaticDetectionView(modifier: Modifier, imageId: String?, frameResult: FrameResult?) {
        val context = LocalContext.current
        val bitmap = remember(imageId) {
            if (imageId != null) uriToBitmap(context, imageId.toUri()) else null
        }
        if (frameResult != null) {
            AndroidStaticDetectionScreen(bitmap = bitmap, frameResult = frameResult, modifier = modifier)
        }
    }

    /**
     * Khởi tạo Helper quản lý việc chọn ảnh thông qua file kiến trúc riêng đã tách biệt.
     */
    @Composable
    override fun rememberImagePickerHelper(
        onModeChange: (ScanMode) -> Unit,
        onResult: (FrameResult) -> Unit,
        onImageIdCaptured: (String) -> Unit
    ): ImagePickerHelper {
        return rememberAndroidImagePickerHelper(
            onModeChange = onModeChange,
            onResult = onResult,
            onImageIdCaptured = onImageIdCaptured
        )
    }
}