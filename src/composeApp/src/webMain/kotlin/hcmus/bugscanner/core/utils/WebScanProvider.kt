package hcmus.bugscanner.ui.scan

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import hcmus.bugscanner.domain.model.FrameResult
import kotlinx.browser.document
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.url.URL

/**
 * Object chứa logic Camera và Image Picker cho Web (JS)
 */
object WebScanProvider : PlatformScanProvider {

    /**
     * Màn hình hiển thị luồng trực tiếp từ Camera bằng HTML <video>
     */
    @Composable
    override fun NativeCameraView(modifier: Modifier, onResult: (FrameResult) -> Unit) {
        // Tạm thời hiển thị một Text thay thế cho đến khi tích hợp UI HTML vào Compose
        // Compose HTML Core cung cấp cách render DOM elements, nhưng tích hợp
        // trực tiếp thẻ <video> vào Compose Multiplatform UI (Canvas) khá phức tạp.
        // Hiện tại, chúng ta hiển thị thông báo. Việc tích hợp YOLO.js sẽ làm sau.
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("Camera Live View chưa được hỗ trợ trực tiếp trên Canvas Web.")
        }
    }

    /**
     * Màn hình hiển thị ảnh tĩnh
     */
    @Composable
    override fun NativeStaticDetectionView(modifier: Modifier, imageId: String?, frameResult: FrameResult?) {
        // Tương tự, việc render ảnh tĩnh và bounding box trên Web sẽ cần Canvas API của HTML
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("Tính năng nhận diện ảnh tĩnh đang được xây dựng cho Web.")
        }
    }

    /**
     * Quản lý việc chọn ảnh (Gallery) và mở Camera Native trên Web
     */
    @Composable
    override fun rememberImagePickerHelper(
        onModeChange: (ScanMode) -> Unit,
        onResult: (FrameResult) -> Unit,
        onImageIdCaptured: (String) -> Unit
    ): ImagePickerHelper {

        // Tạo thẻ <input type="file" accept="image/*"> ngầm để mở file dialog
        val fileInput = remember {
            val input = document.createElement("input") as HTMLInputElement
            input.type = "file"
            input.accept = "image/*"
            input
        }

        DisposableEffect(Unit) {
            // Khi người dùng chọn file
            fileInput.onchange = { event ->
                val files = fileInput.files
                if (files != null && files.length > 0) {
                    // SỬA LỖI 1 & 2: Dùng .item(0) thay vì [0] và cast về org.w3c.files.Blob
                    val file = files.item(0)
                    if (file != null) {
                        val imageUrl = URL.createObjectURL(file as org.w3c.files.Blob)
                        onModeChange(ScanMode.IMAGE_UPLOAD)
                        onImageIdCaptured(imageUrl)

                        // SỬA LỖI 3: Truyền đúng số lượng tham số cho FrameResult
                        // (Khớp với constructor hiện tại của bạn)
                        onResult(FrameResult(emptyList(), 0, 0))
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
                    // Mở thư mục chọn ảnh
                    fileInput.removeAttribute("capture")
                    fileInput.click()
                }

                override fun launchCamera() {
                    // Mở app Camera native trên điện thoại/trình duyệt
                    fileInput.setAttribute("capture", "environment")
                    fileInput.click()
                }
            }
        }
    }
}