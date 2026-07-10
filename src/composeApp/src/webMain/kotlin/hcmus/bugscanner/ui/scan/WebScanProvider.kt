package hcmus.bugscanner.ui.scan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import hcmus.bugscanner.domain.model.FrameResult
import hcmus.bugscanner.ml.WebYoloDetector
import kotlinx.coroutines.launch
import org.khronos.webgl.Int8Array
import org.w3c.dom.HTMLImageElement
import org.w3c.dom.events.Event
import org.w3c.files.FileReader
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Triển khai cụ thể (Implementation) của [PlatformScanProvider] dành cho nền tảng Web (Wasm/JS).
 * Đóng vai trò là cầu nối giữa cây giao diện Compose Multiplatform và các API đặc thù của Trình duyệt Web.
 */
object WebScanProvider : PlatformScanProvider {
    @Composable
    override fun RegisterClipboardImagePasteHandler(onImageBytesPasted: (ByteArray) -> Unit) {
        DisposableEffect(onImageBytesPasted) {
            val listener: (Event) -> Unit = listener@{ event ->
                fun readClipboardFile(file: dynamic) {
                    val reader = FileReader()
                    reader.onload = {
                        val buffer = reader.result as org.khronos.webgl.ArrayBuffer
                        onImageBytesPasted(Int8Array(buffer).unsafeCast<ByteArray>())
                        null
                    }
                    reader.readAsArrayBuffer(file)
                    event.preventDefault()
                }

                val clipboardData = event.asDynamic().clipboardData ?: return@listener
                val items = clipboardData.items
                val length = (items?.length as? Int) ?: 0
                for (index in 0 until length) {
                    val item = items[index] ?: continue
                    val type = item.type as? String ?: ""
                    if (type.startsWith("image/")) {
                        val file = item.getAsFile() ?: continue
                        readClipboardFile(file)
                        return@listener
                    }
                }

                val files = clipboardData.files
                val fileLength = (files?.length as? Int) ?: 0
                for (index in 0 until fileLength) {
                    val file = files[index] ?: continue
                    val type = file.type as? String ?: ""
                    if (type.startsWith("image/")) {
                        readClipboardFile(file)
                        return@listener
                    }
                }
            }

            kotlinx.browser.window.addEventListener("paste", listener)
            onDispose {
                kotlinx.browser.window.removeEventListener("paste", listener)
            }
        }
    }

    /**
     * Đăng ký trình xử lý (Listener) để bắt sự kiện dán ảnh (Paste) từ khay nhớ tạm trên trình duyệt.
     * Tự động giải mã ảnh dán và trả về mảng byte.
     *
     * @param onImageBytesPasted Callback trả về mảng byte của ảnh sau khi được người dùng dán thành công.
     */
    @Composable
    override fun registerClipboardImagePasteHandler(onImageBytesPasted: (ByteArray) -> Unit) {
        DisposableEffect(onImageBytesPasted) {
            val listener: (Event) -> Unit = listener@{ event ->
                fun readClipboardFile(file: dynamic) {
                    val reader = FileReader()
                    reader.onload = {
                        val buffer = reader.result as org.khronos.webgl.ArrayBuffer
                        onImageBytesPasted(Int8Array(buffer).unsafeCast<ByteArray>())
                        null
                    }
                    reader.readAsArrayBuffer(file)
                    event.preventDefault()
                }

                val clipboardData = event.asDynamic().clipboardData ?: return@listener
                val items = clipboardData.items
                val length = (items?.length as? Int) ?: 0
                for (index in 0 until length) {
                    val item = items[index] ?: continue
                    val type = item.type as? String ?: ""
                    if (type.startsWith("image/")) {
                        val file = item.getAsFile() ?: continue
                        readClipboardFile(file)
                        return@listener
                    }
                }

                val files = clipboardData.files
                val fileLength = (files?.length as? Int) ?: 0
                for (index in 0 until fileLength) {
                    val file = files[index] ?: continue
                    val type = file.type as? String ?: ""
                    if (type.startsWith("image/")) {
                        readClipboardFile(file)
                        return@listener
                    }
                }
            }

            kotlinx.browser.window.addEventListener("paste", listener)
            onDispose {
                kotlinx.browser.window.removeEventListener("paste", listener)
            }
        }
    }

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
    ) = onGranted()

    /**
     * Màn hình hiển thị luồng trực tiếp từ Camera trên nền tảng Web thông qua thẻ <video> HTML.
     *
     * @param modifier Modifier định dạng kích thước và vị trí của Camera.
     * @param captureTrigger Trạng thái kích hoạt lệnh chụp ảnh ngầm từ màn hình chính.
     * @param onResult Callback trả về kết quả tọa độ Bounding Box của mô hình AI.
     * @param onFrameCaptured Callback xuất dữ liệu ảnh (ByteArray) chất lượng cao khi có lệnh chụp.
     * @param onRuntimeStatus Callback đồng bộ trạng thái runtime lên component cha.
     */
    @Composable
    override fun NativeCameraView(
        modifier: Modifier,
        captureTrigger: Long,
        onResult: (FrameResult) -> Unit,
        onFrameCaptured: (ByteArray) -> Unit,
        onRuntimeStatus: (ScanRuntimeStatus) -> Unit
    ) {
        WebCameraScreen(modifier, captureTrigger, onResult, onFrameCaptured, onRuntimeStatus)
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
     * @param onRuntimeStatus Callback đồng bộ trạng thái runtime lên component cha.
     */
    @OptIn(ExperimentalEncodingApi::class)
    @Composable
    override fun NativeStaticDetectionView(
        modifier: Modifier,
        imageId: String?,
        imageBytes: ByteArray?,
        frameResult: FrameResult?,
        onResultUpdate: (FrameResult) -> Unit,
        onRuntimeStatus: (ScanRuntimeStatus) -> Unit
    ) {
        val displayId = remember(imageId, imageBytes) {
            imageBytes?.let { "data:image/jpeg;base64,${Base64.encode(it)}" } ?: imageId
        }
        val scope = rememberCoroutineScope()

        LaunchedEffect(displayId) {
            if (displayId == null) return@LaunchedEffect
            onRuntimeStatus(ScanRuntimeStatus.LoadingModel())
            val runtime = WebYoloDetector.initialize()
            onRuntimeStatus(runtime.toRuntimeStatus())
            if (!runtime.ready) return@LaunchedEffect

            val image = kotlinx.browser.document.createElement("img") as HTMLImageElement
            image.onload = {
                scope.launch {
                    onResultUpdate(WebYoloDetector.analyze(image, image.width, image.height))
                }
                null
            }
            image.src = displayId
        }

        WebStaticDetectionScreen(modifier = modifier, imageId = displayId, frameResult = frameResult)
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
    ): ImagePickerHelper = rememberWebImagePickerHelper(
        onModeChange,
        onResult,
        onImageIdCaptured,
        onImageBytesCaptured
    )
}
