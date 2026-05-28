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
import hcmus.bugscanner.ml.YoloDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Helper cung cấp các API để truy cập Thư viện ảnh (Gallery) hoặc Camera chụp ảnh tĩnh trên nền tảng Android.
 * Xử lý luồng chọn ảnh, gọi mô hình AI phân tích và trả kết quả về UI.
 *
 * @param onModeChange Chuyển chế độ quét trên UI.
 * @param onResult Trả về kết quả bounding box từ mô hình AI.
 * @param onImageIdCaptured Trả về định danh URI nội bộ của tấm ảnh.
 * @param onImageBytesCaptured Trả về dữ liệu gốc của ảnh để phục vụ tính năng tải lên Server.
 * @return [ImagePickerHelper] Đối tượng chứa các hàm kích hoạt Gallery/Camera.
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
    var yoloDetector by remember { mutableStateOf<YoloDetector?>(null) }

    // Dọn dẹp bộ nhớ mô hình AI khi Helper này không còn được sử dụng
    DisposableEffect(Unit) {
        onDispose {
            yoloDetector?.close()
        }
    }

    /**
     * Chạy phân tích AI trên bức ảnh vừa được chọn/chụp.
     */
    fun analyze(uri: Uri) {
        coroutineScope.launch(Dispatchers.IO) {
            if (yoloDetector == null) {
                yoloDetector = YoloDetector(context)
            }
            val bmp = uriToBitmap(context, uri)
            bmp?.let {
                // Nén ảnh sang ByteArray bên luồng I/O
                val stream = ByteArrayOutputStream()
                it.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, stream)
                val imageBytes = stream.toByteArray()

                yoloDetector!!.clearResult()
                yoloDetector!!.analyze(it, 0)

                withContext(Dispatchers.Main) {
                    onResult(yoloDetector!!.frameResult.value)
                    onImageBytesCaptured(imageBytes) // Bắn dữ liệu ảnh ra ngoài
                }
            }
        }
    }

    // Launcher kích hoạt Intent mở thư viện ảnh
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            onModeChange(ScanMode.IMAGE_UPLOAD)
            onImageIdCaptured(it.toString())
            analyze(it)
        }
    }

    // Launcher kích hoạt Intent mở Camera chụp ảnh tĩnh (Lưu file tạm)
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            capturedImageUri?.let { uri ->
                onModeChange(ScanMode.CAMERA_CAPTURE)
                onImageIdCaptured(uri.toString())
                analyze(uri)
            }
        }
    }

    return remember {
        object : ImagePickerHelper {
            override fun launchGallery() {
                galleryLauncher.launch("image/*")
            }

            override fun launchCamera() {
                // Sinh tên file duy nhất dựa trên thời gian để tránh ghi đè
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val file = File.createTempFile(
                    "BUGSCANNER_${timeStamp}_",
                    ".jpg",
                    context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                )
                // Sinh URI an toàn thông qua FileProvider theo chuẩn bảo mật Android
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                capturedImageUri = uri
                cameraLauncher.launch(uri)
            }
        }
    }
}