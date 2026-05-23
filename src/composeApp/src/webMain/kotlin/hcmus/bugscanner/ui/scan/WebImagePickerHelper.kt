package hcmus.bugscanner.ui.scan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import hcmus.bugscanner.domain.model.FrameResult
import hcmus.bugscanner.ml.WebYoloDetector
import kotlinx.browser.document
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLImageElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.url.URL

@Composable
fun rememberWebImagePickerHelper(
    onModeChange: (ScanMode) -> Unit,
    onResult: (FrameResult) -> Unit,
    onImageIdCaptured: (String) -> Unit
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