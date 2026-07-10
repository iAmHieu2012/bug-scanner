package hcmus.bugscanner.ui.scan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import hcmus.bugscanner.domain.model.FrameResult

/**
 * Liệt kê các chế độ quét hiện tại của ứng dụng.
 */
enum class ScanMode { LIVE, IMAGE_UPLOAD, CAMERA_CAPTURE }

enum class ScanRuntimeBackend(val label: String) {
    ANDROID("Thiết bị"),
    WEBGL("WebGL"),
    WASM("WASM"),
    NONE("Không khả dụng")
}

sealed interface ScanRuntimeStatus {
    val supportsLiveDetection: Boolean
        get() = (this as? Ready)?.liveDetectionSupported == true

    val backendLabel: String
        get() = (this as? Ready)?.backend?.label ?: ScanRuntimeBackend.NONE.label

    data object Idle : ScanRuntimeStatus
    data object RequestingCamera : ScanRuntimeStatus
    data class LoadingModel(val progressPercent: Int? = null) : ScanRuntimeStatus
    data class Ready(
        val backend: ScanRuntimeBackend,
        val liveDetectionSupported: Boolean = true
    ) : ScanRuntimeStatus
    data class PermissionDenied(
        val message: String = "Trình duyệt chưa được cấp quyền camera."
    ) : ScanRuntimeStatus
    data class Unsupported(
        val message: String = "Thiết bị này không hỗ trợ nhận diện trực tiếp."
    ) : ScanRuntimeStatus
    data class Error(val message: String) : ScanRuntimeStatus
}


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

    @Composable
    fun RegisterClipboardImagePasteHandler(onImageBytesPasted: (ByteArray) -> Unit) = Unit

    /**
     * Component hiển thị luồng Camera trực tiếp.
     *
     * @param modifier Modifier tùy chỉnh kích thước, vị trí.
     * @param captureTrigger Biến trạng thái kích hoạt lệnh chụp ngầm khung hình.
     * @param onResult Callback trả về kết quả tọa độ Bounding Box của AI.
     * @param onFrameCaptured Callback trả về mảng byte (ByteArray) của khung hình vừa chụp ngầm.
     * @param onRuntimeStatus Callback đồng bộ trạng thái runtime của camera lên component cha.
     */
    @Composable
    fun NativeCameraView(
        modifier: Modifier,
        captureTrigger: Long,
        onResult: (FrameResult) -> Unit,
        onFrameCaptured: (ByteArray) -> Unit,
        onRuntimeStatus: (ScanRuntimeStatus) -> Unit
    )

    /**
     * Component hiển thị hình ảnh tĩnh để nhận diện.
     *
     * @param modifier Modifier tùy chỉnh kích thước, vị trí.
     * @param imageId Đường dẫn/Định danh của hình ảnh tĩnh.
     * @param imageBytes Mảng byte của ảnh (được ưu tiên sử dụng để render ảnh đóng băng).
     * @param frameResult Kết quả phân tích Bounding Box ban đầu.
     * @param onResultUpdate Callback cập nhật lại kết quả sau khi phân tích chuyên sâu ảnh tĩnh.
     */
    @Composable
    fun NativeStaticDetectionView(
        modifier: Modifier,
        imageId: String?,
        imageBytes: ByteArray?,
        frameResult: FrameResult?,
        onResultUpdate: (FrameResult) -> Unit,
        onRuntimeStatus: (ScanRuntimeStatus) -> Unit
    )

    /**
     * Khởi tạo Helper xử lý thư viện ảnh và chụp tĩnh.
     *
     * @param onModeChange Callback chuyển đổi chế độ UI.
     * @param onResult Callback trả về kết quả AI của ảnh tĩnh.
     * @param onImageIdCaptured Callback trả về đường dẫn URI của ảnh.
     * @param onImageBytesCaptured Callback trả về mảng byte của ảnh để upload.
     */
    @Composable
    fun rememberImagePickerHelper(
        onModeChange: (ScanMode) -> Unit,
        onResult: (FrameResult) -> Unit,
        onImageIdCaptured: (String) -> Unit,
        onImageBytesCaptured: (ByteArray?) -> Unit
    ): ImagePickerHelper

    /**
     * Hàm kiểm tra và xin quyền Camera.
     *
     * @param onGranted Callback được gọi khi quyền đã được cấp.
     * @param onDenied Callback được gọi khi chưa có quyền.
     */
    @Composable
    fun RequireCameraPermission(
        onGranted: @Composable () -> Unit,
        onDenied: @Composable (onRequestPermission: () -> Unit) -> Unit
    )

    /**
     * Đăng ký trình xử lý bắt sự kiện dán ảnh từ clipboard.
     * Mặc định không làm gì cả, chỉ hiện thực trên các nền tảng có hỗ trợ.
     */
    @Composable
    fun registerClipboardImagePasteHandler(onImageBytesPasted: (ByteArray) -> Unit) {}
}

/**
 * Biến cục bộ (CompositionLocal) truyền PlatformScanProvider xuyên suốt cây UI.
 */
val LocalPlatformScanProvider = staticCompositionLocalOf<PlatformScanProvider> {
    error("Chưa cung cấp PlatformScanProvider cho nền tảng này!")
}
