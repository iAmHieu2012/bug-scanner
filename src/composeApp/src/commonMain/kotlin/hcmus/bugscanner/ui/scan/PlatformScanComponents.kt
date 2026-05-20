package hcmus.bugscanner.ui.scan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import hcmus.bugscanner.domain.model.FrameResult

enum class ScanMode { LIVE, IMAGE_UPLOAD, CAMERA_CAPTURE }

interface ImagePickerHelper {
    fun launchGallery()
    fun launchCamera()
}

/**
 * Interface định nghĩa các thành phần UI và Logic yêu cầu nền tảng cụ thể xử lý.
 */
interface PlatformScanProvider {
    @Composable
    fun NativeCameraView(modifier: Modifier, onResult: (FrameResult) -> Unit)

    @Composable
    fun NativeStaticDetectionView(modifier: Modifier, imageId: String?, frameResult: FrameResult?)

    @Composable
    fun rememberImagePickerHelper(
        onModeChange: (ScanMode) -> Unit,
        onResult: (FrameResult) -> Unit,
        onImageIdCaptured: (String) -> Unit
    ): ImagePickerHelper
}

// Biến này sẽ giúp truyền PlatformScanProvider xuyên suốt cây UI mà không cần pass qua từng hàm
val LocalPlatformScanProvider = staticCompositionLocalOf<PlatformScanProvider> {
    error("Chưa cung cấp PlatformScanProvider cho nền tảng này!")
}