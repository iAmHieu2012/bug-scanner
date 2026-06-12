package hcmus.bugscanner.ui.scan

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import hcmus.bugscanner.core.utils.uriToBitmap
import hcmus.bugscanner.domain.model.FrameResult

/**
 * Object triển khai (Implementation) cụ thể của [PlatformScanProvider] dành cho nền tảng Android.
 * Cung cấp các thành phần giao diện và logic native liên quan đến luồng Camera, xử lý ảnh tĩnh và Thư viện ảnh.
 */
object AndroidScanProvider : PlatformScanProvider {

    /**
     * Hàm kiểm tra và xin quyền Camera trên Android.
     * Sử dụng [rememberLauncherForActivityResult] để gọi hộp thoại xin quyền mặc định của hệ điều hành.
     *
     * @param onGranted Callback được gọi khi ứng dụng đã có sẵn quyền hoặc người dùng vừa bấm "Cho phép".
     * @param onDenied Callback được gọi khi chưa có quyền. Truyền vào lambda chứa lệnh kích hoạt hộp thoại xin quyền.
     */
    @Composable
    override fun RequireCameraPermission(
        onGranted: @Composable () -> Unit,
        onDenied: @Composable (onRequestPermission: () -> Unit) -> Unit
    ) {
        val context = LocalContext.current
        var isGranted by remember {
            mutableStateOf(
                ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            )
        }

        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { result ->
            isGranted = result
        }

        if (isGranted) {
            onGranted()
        } else {
            onDenied {
                launcher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    /**
     * Component hiển thị luồng Camera trực tiếp (CameraX) trên Android.
     * Quản lý tiến trình nhận diện AI và thực thi lệnh chụp ảnh ngầm từ người dùng.
     *
     * @param modifier Modifier để tùy chỉnh kích thước, vị trí của màn hình Camera.
     * @param captureTrigger Cờ tín hiệu kích hoạt lệnh trích xuất khung hình.
     * @param onResult Callback trả về [FrameResult] chứa tọa độ Bounding Box từ AI.
     * @param onFrameCaptured Callback trả về mảng byte (ByteArray) của ảnh vừa được chụp.
     */
    @Composable
    override fun NativeCameraView(
        modifier: Modifier,
        captureTrigger: Long,
        onResult: (FrameResult) -> Unit,
        onFrameCaptured: (ByteArray) -> Unit,
        onRuntimeStatus: (ScanRuntimeStatus) -> Unit
    ) {
        val context = LocalContext.current.applicationContext

        val viewModel: ScanViewModel = viewModel(
            factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    return ScanViewModel(context) as T
                }
            }
        )

        val isReady by viewModel.isReady.collectAsState()
        val frameResult by viewModel.frameResult.collectAsState()

        LaunchedEffect(frameResult) { onResult(frameResult) }
        LaunchedEffect(isReady) {
            onRuntimeStatus(
                if (isReady) ScanRuntimeStatus.Ready(ScanRuntimeBackend.ANDROID)
                else ScanRuntimeStatus.LoadingModel()
            )
        }

        if (!isReady) {
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            AndroidCameraScreen(
                viewModel = viewModel,
                modifier = modifier,
                captureTrigger = captureTrigger,
                onFrameCaptured = onFrameCaptured
            )
        }
    }

    /**
     * Component hiển thị và phân tích ảnh tĩnh trên Android.
     * Chuyển đổi định danh ảnh (URI) hoặc mảng byte thành Bitmap, thực hiện phân tích qua ViewModel
     * và hiển thị kết quả lên màn hình.
     *
     * @param modifier Modifier tùy chỉnh giao diện.
     * @param imageId Chuỗi URI của bức ảnh tĩnh.
     * @param imageBytes Mảng byte của ảnh (được ưu tiên sử dụng để dựng hình ảnh tức thời).
     * @param frameResult Kết quả tọa độ phân tích từ AI (có thể null nếu đang chờ xử lý).
     * @param onResultUpdate Callback cập nhật kết quả sau khi thực hiện phân tích lại từ Bitmap.
     */
    @Composable
    override fun NativeStaticDetectionView(
        modifier: Modifier,
        imageId: String?,
        imageBytes: ByteArray?,
        frameResult: FrameResult?,
        onResultUpdate: (FrameResult) -> Unit,
        onRuntimeStatus: (ScanRuntimeStatus) -> Unit
    ) {
        val context = LocalContext.current

        val viewModel: ScanViewModel = viewModel(
            factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    return ScanViewModel(context) as T
                }
            }
        )

        LaunchedEffect(Unit) {
            onRuntimeStatus(ScanRuntimeStatus.Ready(ScanRuntimeBackend.ANDROID))
        }

        val bitmap = remember(imageId, imageBytes) {
            if (imageBytes != null) {
                android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            } else if (imageId != null) {
                uriToBitmap(context, imageId.toUri())
            } else {
                null
            }
        }

        LaunchedEffect(bitmap) {
            if (bitmap != null && imageBytes != null) {
                viewModel.analyzeImage(bitmap, 0)
                onResultUpdate(viewModel.frameResult.value)
            }
        }

        if (frameResult != null && bitmap != null) {
            AndroidStaticDetectionScreen(bitmap = bitmap, frameResult = frameResult, modifier = modifier)
        } else if (bitmap != null) {
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
    }

    /**
     * Khởi tạo Helper hỗ trợ việc chọn ảnh từ Thư viện (Gallery) hoặc chụp ảnh bằng App Camera gốc của Android.
     *
     * @param onModeChange Callback chuyển đổi chế độ UI.
     * @param onResult Callback trả về kết quả phân tích AI của ảnh tĩnh.
     * @param onImageIdCaptured Callback trả về đường dẫn URI (dạng chuỗi) của ảnh.
     * @param onImageBytesCaptured Callback trả về mảng byte (ByteArray) của ảnh tĩnh.
     * @return [ImagePickerHelper] được cấu hình sẵn cho Android.
     */
    @Composable
    override fun rememberImagePickerHelper(
        onModeChange: (ScanMode) -> Unit,
        onResult: (FrameResult) -> Unit,
        onImageIdCaptured: (String) -> Unit,
        onImageBytesCaptured: (ByteArray?) -> Unit
    ): ImagePickerHelper {
        return rememberAndroidImagePickerHelper(
            onModeChange = onModeChange,
            onResult = onResult,
            onImageIdCaptured = onImageIdCaptured,
            onImageBytesCaptured = onImageBytesCaptured
        )
    }
}