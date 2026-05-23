package hcmus.bugscanner.ui.scan

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import hcmus.bugscanner.domain.model.FrameResult

/**
 * Cung cấp các View và logic Camera được tối ưu riêng cho nền tảng Web (JS/Wasm).
 */
object WebScanProvider : PlatformScanProvider {

    @Composable
    override fun NativeCameraView(modifier: Modifier, onResult: (FrameResult) -> Unit) {
        WebCameraScreen(modifier = modifier, onResult = onResult)
    }

    @Composable
    override fun NativeStaticDetectionView(
        modifier: Modifier,
        imageId: String?,
        frameResult: FrameResult?
    ) {
        WebStaticDetectionScreen(modifier = modifier, imageId = imageId, frameResult = frameResult)
    }

    @Composable
    override fun rememberImagePickerHelper(
        onModeChange: (ScanMode) -> Unit,
        onResult: (FrameResult) -> Unit,
        onImageIdCaptured: (String) -> Unit
    ): ImagePickerHelper {
        return rememberWebImagePickerHelper(
            onModeChange = onModeChange,
            onResult = onResult,
            onImageIdCaptured = onImageIdCaptured
        )
    }
}