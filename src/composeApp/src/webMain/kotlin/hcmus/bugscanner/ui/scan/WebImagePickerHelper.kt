package hcmus.bugscanner.ui.scan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import hcmus.bugscanner.domain.model.FrameResult
import kotlinx.browser.document
import kotlinx.browser.window
import org.khronos.webgl.Int8Array
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.url.URL
import org.w3c.files.FileReader

private const val MAX_WEB_IMAGE_BYTES = 10 * 1024 * 1024

/**
 * Trình quản lý tương tác với hệ thống File Explorer và Native Camera của trình duyệt Web.
 * Giao tiếp thông qua một thẻ `<input type="file">` ẩn trong DOM.
 *
 * @param onModeChange Chuyển trạng thái UI sang chế độ tĩnh.
 * @param onResult Đẩy kết quả tọa độ từ AI ra Component cha.
 * @param onImageIdCaptured Đẩy Blob URL ra để vẽ lên giao diện `WebStaticDetectionScreen`.
 * @param onImageBytesCaptured Dùng `FileReader` đọc file gốc thành mảng byte để tải lên Firebase Storage.
 * @return [ImagePickerHelper] Đối tượng Helper được cấu hình cho môi trường Web.
 */
@Composable
fun rememberWebImagePickerHelper(
    onModeChange: (ScanMode) -> Unit,
    onResult: (FrameResult) -> Unit,
    onImageIdCaptured: (String) -> Unit,
    onImageBytesCaptured: (ByteArray?) -> Unit
): ImagePickerHelper {
    var activeObjectUrl by remember { mutableStateOf<String?>(null) }
    val fileInput = remember {
        (document.createElement("input") as HTMLInputElement).apply {
            type = "file"
            accept = "image/jpeg,image/png,image/webp"
        }
    }

    DisposableEffect(Unit) {
        fileInput.onchange = {
            val file = fileInput.files?.item(0)
            if (file != null) {
                val fileType = file.asDynamic().type as? String ?: ""
                val fileSize = file.asDynamic().size as? Double ?: 0.0
                when {
                    !fileType.startsWith("image/") -> window.alert("Vui lòng chọn một tệp hình ảnh.")
                    fileSize > MAX_WEB_IMAGE_BYTES -> window.alert("Ảnh vượt quá giới hạn 10 MB.")
                    else -> {
                        activeObjectUrl?.let(URL::revokeObjectURL)
                        val imageUrl = URL.createObjectURL(file)
                        activeObjectUrl = imageUrl
                        onModeChange(ScanMode.IMAGE_UPLOAD)
                        onImageIdCaptured(imageUrl)

                        val reader = FileReader()
                        reader.onload = {
                            val buffer = reader.result as org.khronos.webgl.ArrayBuffer
                            onImageBytesCaptured(Int8Array(buffer).unsafeCast<ByteArray>())
                            null
                        }
                        reader.readAsArrayBuffer(file)
                    }
                }
            }
            null
        }

        onDispose {
            fileInput.onchange = null
            activeObjectUrl?.let(URL::revokeObjectURL)
        }
    }

    return remember(fileInput) {
        object : ImagePickerHelper {
            override fun launchGallery() {
                fileInput.value = ""
                fileInput.removeAttribute("capture")
                fileInput.click()
            }

            override fun launchCamera() {
                fileInput.value = ""
                fileInput.setAttribute("capture", "environment")
                fileInput.click()
            }
        }
    }
}
