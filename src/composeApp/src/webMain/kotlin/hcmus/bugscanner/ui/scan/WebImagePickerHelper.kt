package hcmus.bugscanner.ui.scan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import hcmus.bugscanner.domain.model.FrameResult
import hcmus.bugscanner.ml.WebYoloDetector
import kotlinx.browser.document
import kotlinx.coroutines.launch
import org.khronos.webgl.Int8Array
import org.w3c.dom.HTMLImageElement
import org.w3c.dom.HTMLInputElement
import org.w3c.files.FileReader
import org.w3c.dom.url.URL

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

    val fileInput = remember {
        val input = document.createElement("input") as HTMLInputElement
        input.apply {
            type = "file"
            accept = "image/*"
        }
    }

    val coroutineScope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        fileInput.onchange = {
            val files = fileInput.files
            if (files != null && files.length > 0) {
                val file = files.item(0)
                if (file != null) {
                    val imageUrl = URL.createObjectURL(file as org.w3c.files.Blob)
                    onModeChange(ScanMode.IMAGE_UPLOAD)
                    onImageIdCaptured(imageUrl)

                    val reader = FileReader()
                    reader.onload = {
                        val buffer = reader.result as org.khronos.webgl.ArrayBuffer
                        val byteArray = Int8Array(buffer).unsafeCast<ByteArray>()
                        onImageBytesCaptured(byteArray)
                        null
                    }
                    reader.readAsArrayBuffer(file as org.w3c.files.Blob)

                    val imgElement = document.createElement("img") as HTMLImageElement
                    imgElement.onload = {
                        coroutineScope.launch {
                            try {
                                val result = WebYoloDetector.analyze(imgElement, imgElement.width, imgElement.height)
                                onResult(result)
                            } catch (e: Exception) {
                                println("Lỗi xử lý ảnh tĩnh AI: ${e.message}")
                            }
                        }
                        null
                    }
                    imgElement.src = imageUrl
                }
            }
            null
        }

        onDispose {
            fileInput.onchange = null
        }
    }

    return remember {
        object : ImagePickerHelper {
            override fun launchGallery() {
                fileInput.removeAttribute("capture")
                fileInput.click()
            }

            override fun launchCamera() {
                fileInput.setAttribute("capture", "environment")
                fileInput.click()
            }
        }
    }
}