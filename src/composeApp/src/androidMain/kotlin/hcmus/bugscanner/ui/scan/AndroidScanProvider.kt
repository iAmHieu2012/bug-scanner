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
     * @param onDenied Callback được gọi khi chưa có quyền. Truyền vào lambda chứa lệnh kích hoạt hộp thoại xin quyền để UI sử dụng.
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
            onDenied{
                launcher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    /**
     * Component hiển thị luồng Camera trực tiếp (CameraX) trên Android.
     * Mọi logic khởi tạo AI nặng nề đều đã được đẩy vào ScanViewModel để chống rò rỉ RAM.
     *
     * @param modifier Modifier để tùy chỉnh kích thước, vị trí của màn hình Camera.
     * @param onResult Callback trả về [FrameResult] chứa tọa độ Bounding Box từ AI để vẽ lên giao diện.
     * @param onLiveFrameCaptured Callback trả về mảng byte (ByteArray) của khung hình camera nếu AI phát hiện côn trùng.
     */
    @Composable
    override fun NativeCameraView(
        modifier: Modifier,
        onResult: (FrameResult) -> Unit,
        onLiveFrameCaptured: (ByteArray?) -> Unit
    ) {
        val context = LocalContext.current.applicationContext

        // Sử dụng ViewModelFactory chuẩn của Android để đảm bảo ScanViewModel chỉ sinh ra 1 lần duy nhất
        val viewModel: ScanViewModel = viewModel(
            factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    return ScanViewModel(context) as T
                }
            }
        )

        // Quan sát State trực tiếp từ ViewModel
        val isReady by viewModel.isReady.collectAsState()
        val frameResult by viewModel.frameResult.collectAsState()

        LaunchedEffect(frameResult) { onResult(frameResult) }

        if (!isReady) {
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            AndroidCameraScreen(
                viewModel = viewModel,
                modifier = modifier,
                onLiveFrameCaptured = onLiveFrameCaptured
            )
        }
    }

    /**
     * Component hiển thị và phân tích ảnh tĩnh trên Android.
     * Chuyển đổi định danh ảnh (URI) thành Bitmap để mô hình AI xử lý và vẽ lên Canvas.
     *
     * @param modifier Modifier tùy chỉnh giao diện.
     * @param imageId Chuỗi URI của bức ảnh tĩnh (nằm trong thư viện hoặc vừa chụp).
     * @param frameResult Kết quả tọa độ phân tích từ AI để vẽ khung giới hạn.
     */
    @Composable
    override fun NativeStaticDetectionView(modifier: Modifier, imageId: String?, frameResult: FrameResult?) {
        val context = LocalContext.current
        // Chuyển đổi định danh (URI dạng String) trở lại thành Bitmap để vẽ
        val bitmap = remember(imageId) {
            if (imageId != null) uriToBitmap(context, imageId.toUri()) else null
        }
        if (frameResult != null) {
            AndroidStaticDetectionScreen(bitmap = bitmap, frameResult = frameResult, modifier = modifier)
        }
    }

    /**
     * Khởi tạo Helper hỗ trợ việc chọn ảnh từ Thư viện (Gallery) hoặc chụp ảnh bằng App Camera gốc của Android.
     *
     * @param onModeChange Callback chuyển đổi chế độ UI khi người dùng đã chọn xong ảnh.
     * @param onResult Callback trả về kết quả phân tích AI của ảnh tĩnh.
     * @param onImageIdCaptured Callback trả về đường dẫn URI (dạng chuỗi) của ảnh.
     * @param onImageBytesCaptured Callback trả về mảng byte (ByteArray) của ảnh tĩnh để phục vụ việc upload.
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