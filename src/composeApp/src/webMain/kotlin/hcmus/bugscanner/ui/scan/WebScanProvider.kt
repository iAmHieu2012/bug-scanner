package hcmus.bugscanner.ui.scan

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import hcmus.bugscanner.domain.model.FrameResult

/**
 * Triển khai cụ thể (Implementation) của `PlatformScanProvider` dành cho nền tảng Web (Wasm/JS).
 * Đóng vai trò là "Cầu nối" (Bridge) giữa cây giao diện Compose Multiplatform dùng chung
 * và các API gọi Camera, Thư viện đặc thù của Trình duyệt Web.
 */
object WebScanProvider : PlatformScanProvider {

    /**
     * Màn hình hiển thị luồng trực tiếp từ Camera trên nền tảng Web.
     *
     * @param modifier Modifier định dạng kích thước và vị trí của Camera.
     * @param onResult Callback trả về kết quả tọa độ Bounding Box của mô hình AI.
     * @param onLiveFrameCaptured Callback xuất dữ liệu ảnh (ByteArray) của khung hình camera HIỆN TẠI nếu AI phát hiện có côn trùng. Trả về null nếu không có.
     */
    @Composable
    override fun NativeCameraView(
        modifier: Modifier,
        onResult: (FrameResult) -> Unit,
        onLiveFrameCaptured: (ByteArray?) -> Unit
    ) {
        WebCameraScreen(
            modifier = modifier,
            onResult = onResult,
            onLiveFrameCaptured = onLiveFrameCaptured
        )
    }

    /**
     * Màn hình xử lý và vẽ bounding box cho ảnh tĩnh trên Web.
     *
     * @param modifier Modifier định dạng giao diện.
     * @param imageId Chuỗi Blob URL nội bộ của bức ảnh trên DOM trình duyệt.
     * @param frameResult Kết quả tọa độ phân tích từ AI để vẽ khung giới hạn.
     */
    @Composable
    override fun NativeStaticDetectionView(
        modifier: Modifier,
        imageId: String?,
        frameResult: FrameResult?
    ) {
        WebStaticDetectionScreen(modifier = modifier, imageId = imageId, frameResult = frameResult)
    }

    /**
     * Khởi tạo Helper hỗ trợ việc chọn ảnh từ máy tính (File Explorer) hoặc chụp ảnh trên thiết bị di động duyệt Web.
     *
     * @param onModeChange Callback chuyển đổi chế độ UI sang dạng hiển thị tĩnh.
     * @param onResult Callback trả về kết quả AI của ảnh tĩnh.
     * @param onImageIdCaptured Callback trả về Blob URL của ảnh để render lên giao diện.
     * @param onImageBytesCaptured Callback trả về mảng byte gốc của ảnh tĩnh để phục vụ việc tải lên Firebase Storage.
     */
    @Composable
    override fun rememberImagePickerHelper(
        onModeChange: (ScanMode) -> Unit,
        onResult: (FrameResult) -> Unit,
        onImageIdCaptured: (String) -> Unit,
        onImageBytesCaptured: (ByteArray?) -> Unit
    ): ImagePickerHelper {
        return rememberWebImagePickerHelper(
            onModeChange = onModeChange,
            onResult = onResult,
            onImageIdCaptured = onImageIdCaptured,
            onImageBytesCaptured = onImageBytesCaptured
        )
    }
}