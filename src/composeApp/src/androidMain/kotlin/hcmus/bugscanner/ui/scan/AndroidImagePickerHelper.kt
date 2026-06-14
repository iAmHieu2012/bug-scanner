package hcmus.bugscanner.ui.scan

import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import hcmus.bugscanner.core.utils.uriToBitmap
import hcmus.bugscanner.domain.model.FrameResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Helper cung cấp các API để truy cập Thư viện ảnh (Gallery) hoặc Camera chụp ảnh tĩnh trên nền tảng Android.
 * Xử lý luồng chọn ảnh và bóc tách dữ liệu gốc của ảnh (ByteArray) để trả về UI mà không gây chặn luồng chính.
 *
 * @param onModeChange Callback chuyển đổi chế độ giao diện sang tĩnh/camera.
 * @param onResult Callback (chủ yếu được giữ lại để tuân thủ interface) trả kết quả nhận diện.
 * @param onImageIdCaptured Callback trả về chuỗi định danh URI nội bộ của tấm ảnh được chọn.
 * @param onImageBytesCaptured Callback trả về mảng byte dữ liệu gốc của ảnh để sử dụng cho tính năng Fallback.
 * @return [ImagePickerHelper] Giao diện chứa hàm khởi chạy Intent Thư viện và Camera.
 */
@Composable
fun rememberAndroidImagePickerHelper(
    onModeChange: (ScanMode) -> Unit,
    onResult: (FrameResult) -> Unit,
    onImageIdCaptured: (String) -> Unit,
    onImageBytesCaptured: (ByteArray?) -> Unit
): ImagePickerHelper {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    /**
     * Đọc và nén dữ liệu từ một [Uri] của Android thành mảng byte thô (ByteArray).
     * Quá trình giải mã và nén Bitmap tốn thời gian nên được đẩy xuống luồng nền ([Dispatchers.IO]).
     *
     * @param uri Đường dẫn URI của hình ảnh được chọn từ thiết bị.
     */
    fun extractBytes(uri: Uri) {
        coroutineScope.launch(Dispatchers.IO) {
            val bmp = uriToBitmap(context, uri)
            bmp?.let {
                val stream = ByteArrayOutputStream()
                it.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, stream)
                val imageBytes = stream.toByteArray()
                withContext(Dispatchers.Main) {
                    onImageBytesCaptured(imageBytes)
                }
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            onModeChange(ScanMode.IMAGE_UPLOAD)
            onImageIdCaptured(it.toString())
            extractBytes(it)
        }
    }

    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            capturedImageUri?.let { uri ->
                onModeChange(ScanMode.CAMERA_CAPTURE)
                onImageIdCaptured(uri.toString())
                extractBytes(uri)
            }
        }
    }

    return remember {
        object : ImagePickerHelper {
            override fun launchGallery() {
                galleryLauncher.launch("image/*")
            }

            override fun launchCamera() {
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val file = File.createTempFile(
                    "BUGSCANNER_${timeStamp}_",
                    ".jpg",
                    context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                )
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                capturedImageUri = uri
                cameraLauncher.launch(uri)
            }
        }
    }
}