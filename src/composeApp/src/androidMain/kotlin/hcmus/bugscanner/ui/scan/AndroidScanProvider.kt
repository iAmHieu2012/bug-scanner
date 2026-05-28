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
 * Object triển khai (Implementation) cụ thể của [PlatformScanProvider] dành cho nền tảng Android.
 * Cung cấp các thành phần giao diện và logic native liên quan đến luồng Camera, xử lý ảnh tĩnh và Thư viện ảnh.
 */
object AndroidScanProvider : PlatformScanProvider {

    @Composable
    override fun NativeCameraView(
        modifier: Modifier,
        onResult: (FrameResult) -> Unit,
        onLiveFrameCaptured: (ByteArray?) -> Unit
    ) {
        val context = LocalContext.current
        var isReady by remember { mutableStateOf(false) }

        // Khởi tạo luồng Thread chạy ngầm và tải mô hình YOLO ở Background
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

            // Theo dõi kết quả từ ViewModel và bắn ra ngoài UI thông qua onResult
            val frameResult by viewModel.frameResult.collectAsState()
            LaunchedEffect(frameResult) { onResult(frameResult) }

            AndroidCameraScreen(
                viewModel = viewModel,
                modifier = modifier,
                onLiveFrameCaptured = onLiveFrameCaptured
            )
        }
    }

    @Composable
    override fun NativeStaticDetectionView(modifier: Modifier, imageId: String?, frameResult: FrameResult?) {
        val context = LocalContext.current
        // Chuyển đổi định danh (URI dạng String) trở lại thành Bitmap để vẽ
        val bitmap = remember(imageId) {
            if (imageId != null) uriToBitmap(context, imageId.toUri()) else null
        }
        if (frameResult != null) {
            AndroidStaticDetectionScreen(bitmap = bitmap, frameResult = frameResult, modifier = modifier)
        }
    }

    @Composable
    override fun rememberImagePickerHelper(
        onModeChange: (ScanMode) -> Unit,
        onResult: (FrameResult) -> Unit,
        onImageIdCaptured: (String) -> Unit,
        onImageBytesCaptured: (ByteArray?) -> Unit
    ): ImagePickerHelper {
        return rememberAndroidImagePickerHelper(
            onModeChange = onModeChange,
            onResult = onResult,
            onImageIdCaptured = onImageIdCaptured,
            onImageBytesCaptured = onImageBytesCaptured
        )
    }
}