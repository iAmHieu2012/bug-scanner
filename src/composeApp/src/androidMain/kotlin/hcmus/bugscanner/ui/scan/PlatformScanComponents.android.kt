package hcmus.bugscanner.ui.scan

import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import hcmus.bugscanner.core.utils.uriToBitmap
import hcmus.bugscanner.domain.model.FrameResult
import hcmus.bugscanner.ml.YoloDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Màn hình hiển thị luồng trực tiếp từ Camera native và vẽ bounding box YOLO.
 */
@Composable
actual fun NativeCameraView(modifier: Modifier, onResult: (FrameResult) -> Unit) {
    val context = LocalContext.current
    var isReady by remember { mutableStateOf(false) }

    // Dùng AtomicReference để truyền đối tượng an toàn giữa các luồng
    val yoloRef = remember { java.util.concurrent.atomic.AtomicReference<YoloDetector?>(null) }
    val executorRef = remember { java.util.concurrent.atomic.AtomicReference<ExecutorService?>(null) }

    // Đẩy tác vụ load AI nặng nề xuống luồng nền (IO Thread) để không gây lag UI
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            yoloRef.set(YoloDetector(context))
            executorRef.set(Executors.newSingleThreadExecutor())
            isReady = true // Load xong thì báo cờ
        }
    }

    if (!isReady) {
        // Trong lúc chờ load AI (~0.5s), hiện vòng xoay thay vì làm "đơ" toàn bộ màn hình
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White)
        }
    } else {
        // Load xong rồi thì ráp vào ViewModel để chạy Camera
        val viewModel: ScanViewModel = viewModel(
            factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ScanViewModel(yoloRef.get()!!, executorRef.get()!!) as T
                }
            }
        )

        // Lắng nghe kết quả và đẩy lên commonMain
        val frameResult by viewModel.frameResult.collectAsState()
        LaunchedEffect(frameResult) { onResult(frameResult) }

        CameraScreen(viewModel = viewModel, modifier = modifier)
    }
}

/**
 * Màn hình xử lý và vẽ bounding box cho ảnh tĩnh native (Gallery/Chụp).
 */
@Composable
actual fun NativeStaticDetectionView(modifier: Modifier, imageId: String?, frameResult: FrameResult?) {
    val context = LocalContext.current
    val bitmap = remember(imageId) {
        if (imageId != null) uriToBitmap(context, imageId.toUri()) else null
    }
    if (frameResult != null) {
        StaticDetectionScreen(bitmap = bitmap, frameResult = frameResult, modifier = modifier)
    }
}

/**
 * Helper hỗ trợ mở thư viện ảnh và camera của Native platform (Android/iOS).
 */
actual class ImagePickerHelper(
    private val onLaunchGallery: () -> Unit,
    private val onLaunchCamera: () -> Unit
) {
    actual fun launchGallery() = onLaunchGallery()
    actual fun launchCamera() = onLaunchCamera()
}

@Composable
actual fun rememberImagePickerHelper(
    onModeChange: (ScanMode) -> Unit,
    onResult: (FrameResult) -> Unit,
    onImageIdCaptured: (String) -> Unit
): ImagePickerHelper {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Khởi tạo lười (Lazy). Chỉ load AI khi người dùng thật sự click chọn ảnh tĩnh
    var yoloDetector by remember { mutableStateOf<YoloDetector?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            yoloDetector?.close()
        }
    }

    fun analyze(uri: Uri) {
        coroutineScope.launch(Dispatchers.IO) {
            if (yoloDetector == null) {
                yoloDetector = YoloDetector(context)
            }
            val bmp = uriToBitmap(context, uri)
            bmp?.let {
                yoloDetector!!.clearResult()
                yoloDetector!!.analyze(it, 0)
                withContext(Dispatchers.Main) { // Phân tích xong thì ném kết quả lên Main Thread để vẽ
                    onResult(yoloDetector!!.frameResult.value)
                }
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            onModeChange(ScanMode.IMAGE_UPLOAD)
            onImageIdCaptured(it.toString())
            analyze(it)
        }
    }

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
        ImagePickerHelper(
            onLaunchGallery = { galleryLauncher.launch("image/*") },
            onLaunchCamera = {
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val file = File.createTempFile("BUGSCANNER_${timeStamp}_", ".jpg", context.getExternalFilesDir(Environment.DIRECTORY_PICTURES))
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                capturedImageUri = uri
                cameraLauncher.launch(uri)
            }
        )
    }
}