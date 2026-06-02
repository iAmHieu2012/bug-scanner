package hcmus.bugscanner.ui.scan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularProgressIndicator
import hcmus.bugscanner.domain.model.FrameResult
import kotlinx.coroutines.launch
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Triển khai cụ thể (Implementation) của [PlatformScanProvider] dành cho nền tảng Web (Wasm/JS).
 * Đóng vai trò là cầu nối giữa cây giao diện Compose Multiplatform và các API đặc thù của Trình duyệt Web.
 */
object WebScanProvider : PlatformScanProvider {

    /**
     * Hàm xin quyền truy cập Camera trên nền tảng Web.
     * Trình duyệt xử lý quyền tự động khi gọi Video API, nên hàm này gọi thẳng vào block xử lý thành công.
     *
     * @param onGranted Callback được gọi ngay lập tức để luồng UI khởi tạo Camera.
     * @param onDenied Callback không được sử dụng trên Web.
     */
    @Composable
    override fun RequireCameraPermission(
        onGranted: @Composable () -> Unit,
        onDenied: @Composable (onRequestPermission: () -> Unit) -> Unit
    ) {
        onGranted()
    }

    /**
     * Màn hình hiển thị luồng trực tiếp từ Camera trên nền tảng Web thông qua thẻ <video> HTML.
     *
     * @param modifier Modifier định dạng kích thước và vị trí của Camera.
     * @param captureTrigger Trạng thái kích hoạt lệnh chụp ảnh ngầm từ màn hình chính.
     * @param onResult Callback trả về kết quả tọa độ Bounding Box của mô hình AI.
     * @param onFrameCaptured Callback xuất dữ liệu ảnh (ByteArray) chất lượng cao khi có lệnh chụp.
     */
    @Composable
    override fun NativeCameraView(
        modifier: Modifier,
        captureTrigger: Long,
        onResult: (FrameResult) -> Unit,
        onFrameCaptured: (ByteArray) -> Unit
    ) {
        WebCameraScreen(
            modifier = modifier,
            captureTrigger = captureTrigger,
            onResult = onResult,
            onFrameCaptured = onFrameCaptured
        )
    }

    /**
     * Màn hình xử lý và vẽ bounding box cho ảnh tĩnh trên Web thông qua Canvas API.
     * Tự động decode byte array sang Base64 và thực hiện gọi Model phân tích phía trình duyệt.
     *
     * @param modifier Modifier định dạng giao diện.
     * @param imageId Chuỗi Blob URL nội bộ của bức ảnh trên DOM trình duyệt.
     * @param imageBytes Mảng byte của ảnh.
     * @param frameResult Kết quả tọa độ phân tích từ AI.
     * @param onResultUpdate Callback cập nhật kết quả phân tích AI sau khi Model chạy xong.
     */
    @OptIn(ExperimentalEncodingApi::class)
    @Composable
    override fun NativeStaticDetectionView(
        modifier: Modifier,
        imageId: String?,
        imageBytes: ByteArray?,
        frameResult: FrameResult?,
        onResultUpdate: (FrameResult) -> Unit
    ) {
        val displayId = remember(imageId, imageBytes) {
            if (imageBytes != null) {
                "data:image/jpeg;base64,${Base64.encode(imageBytes)}"
            } else {
                imageId
            }
        }

        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(displayId) {
            if (displayId != null && imageBytes != null) {
                val img = kotlinx.browser.document.createElement("img") as org.w3c.dom.HTMLImageElement
                img.src = displayId
                img.onload = { _ ->
                    coroutineScope.launch {
                        try {
                            val result = hcmus.bugscanner.ml.WebYoloDetector.analyze(img, img.width, img.height)
                            onResultUpdate(result)
                        } catch (e: Exception) {
                            println("Lỗi Re-detect Web: $e")
                        }
                    }
                }
            }
        }

        if (frameResult != null) {
            WebStaticDetectionScreen(
                modifier = modifier,
                imageId = displayId,
                frameResult = frameResult
            )
        } else if (displayId != null) {
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }

    /**
     * Khởi tạo Helper hỗ trợ việc chọn ảnh từ máy tính hoặc thiết bị di động duyệt Web.
     *
     * @param onModeChange Callback chuyển đổi chế độ UI sang dạng hiển thị tĩnh.
     * @param onResult Callback trả về kết quả AI của ảnh tĩnh.
     * @param onImageIdCaptured Callback trả về Blob URL của ảnh để render lên giao diện.
     * @param onImageBytesCaptured Callback trả về mảng byte gốc của ảnh tĩnh.
     * @return [ImagePickerHelper] được cấu hình sẵn cho Web.
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