package hcmus.bugscanner.ui.scan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import hcmus.bugscanner.domain.model.FrameResult

/**
 * Liệt kê các chế độ quét hiện tại của ứng dụng.
 */
enum class ScanMode { LIVE, IMAGE_UPLOAD, CAMERA_CAPTURE }

/**
 * Interface hỗ trợ gọi các API mở thư viện ảnh hoặc camera chụp tĩnh của hệ thống.
 */
interface ImagePickerHelper {
    fun launchGallery()
    fun launchCamera()
}

/**
 * Interface định nghĩa các thành phần UI và Logic yêu cầu nền tảng cụ thể (Android/Web/iOS) xử lý.
 */
interface PlatformScanProvider {

    /**
     * Component hiển thị luồng Camera trực tiếp.
     * * @param modifier Modifier tùy chỉnh kích thước, vị trí.
     * @param onResult Callback trả về kết quả tọa độ Bounding Box của AI.
     * @param onLiveFrameCaptured Callback trả về mảng byte (ByteArray) của khung hình camera HIỆN TẠI nếu AI phát hiện có côn trùng. Trả về null nếu không có.
     */
    @Composable
    fun NativeCameraView(
        modifier: Modifier,
        onResult: (FrameResult) -> Unit,
        onLiveFrameCaptured: (ByteArray?) -> Unit
    )

    /**
     * Component hiển thị hình ảnh tĩnh để nhận diện.
     * * @param modifier Modifier tùy chỉnh kích thước, vị trí.
     * @param imageId Đường dẫn/Định danh của hình ảnh tĩnh.
     * @param frameResult Kết quả phân tích Bounding Box.
     */
    @Composable
    fun NativeStaticDetectionView(modifier: Modifier, imageId: String?, frameResult: FrameResult?)

    /**
     * Khởi tạo Helper xử lý thư viện ảnh và chụp tĩnh.
     * * @param onModeChange Callback chuyển đổi chế độ UI.
     * @param onResult Callback trả về kết quả AI của ảnh.
     * @param onImageIdCaptured Callback trả về đường dẫn URI của ảnh.
     * @param onImageBytesCaptured Callback trả về mảng byte (ByteArray) của ảnh tĩnh để upload.
     */
    @Composable
    fun rememberImagePickerHelper(
        onModeChange: (ScanMode) -> Unit,
        onResult: (FrameResult) -> Unit,
        onImageIdCaptured: (String) -> Unit,
        onImageBytesCaptured: (ByteArray?) -> Unit
    ): ImagePickerHelper
}

/** * Biến cục bộ (CompositionLocal) truyền PlatformScanProvider xuyên suốt cây UI mà không cần pass qua từng hàm.
 */
val LocalPlatformScanProvider = staticCompositionLocalOf<PlatformScanProvider> {
    error("Chưa cung cấp PlatformScanProvider cho nền tảng này!")
}