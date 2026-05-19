package hcmus.bugscanner.ui.scan

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import hcmus.bugscanner.domain.model.FrameResult

enum class ScanMode { LIVE, IMAGE_UPLOAD, CAMERA_CAPTURE }

/**
 * Màn hình hiển thị luồng trực tiếp từ Camera native và vẽ bounding box YOLO.
 */
@Composable
expect fun NativeCameraView(modifier: Modifier, onResult: (FrameResult) -> Unit)

/**
 * Màn hình xử lý và vẽ bounding box cho ảnh tĩnh native (Gallery/Chụp).
 */
@Composable
expect fun NativeStaticDetectionView(modifier: Modifier, imageId: String?, frameResult: FrameResult?)

/**
 * Helper hỗ trợ mở thư viện ảnh và camera của Native platform (Android/iOS).
 */
expect class ImagePickerHelper {
    fun launchGallery()
    fun launchCamera()
}

@Composable
expect fun rememberImagePickerHelper(
    onModeChange: (ScanMode) -> Unit,
    onResult: (FrameResult) -> Unit,
    onImageIdCaptured: (String) -> Unit
): ImagePickerHelper