package hcmus.bugscanner.ui.scan

import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
 * Object chứa toàn bộ logic Camera của Android
  */
object AndroidScanProvider : PlatformScanProvider {

    /**
     * Màn hình hiển thị luồng trực tiếp từ Camera native và vẽ bounding box YOLO.
     */
    @Composable
    override fun NativeCameraView(modifier: Modifier, onResult: (FrameResult) -> Unit) {
        val context = LocalContext.current
        var isReady by remember { mutableStateOf(false) }

        val yoloRef = remember { java.util.concurrent.atomic.AtomicReference<YoloDetector?>(null) }
        val executorRef = remember { java.util.concurrent.atomic.AtomicReference<ExecutorService?>(null) }

        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                yoloRef.set(YoloDetector(context))
                executorRef.set(Executors.newSingleThreadExecutor())
                isReady = true
            }
        }

        if (!isReady) {
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            val viewModel: ScanViewModel = viewModel {
                ScanViewModel(yoloRef.get()!!, executorRef.get()!!)
            }

            val frameResult by viewModel.frameResult.collectAsState()
            LaunchedEffect(frameResult) { onResult(frameResult) }

            CameraScreen(viewModel = viewModel, modifier = modifier)
        }
    }

    /**
     * Màn hình xử lý và vẽ bounding box cho ảnh tĩnh native (Gallery/Chụp).
     */
    @Composable
    override fun NativeStaticDetectionView(modifier: Modifier, imageId: String?, frameResult: FrameResult?) {
        val context = LocalContext.current
        val bitmap = remember(imageId) {
            if (imageId != null) uriToBitmap(context, imageId.toUri()) else null
        }
        if (frameResult != null) {
            StaticDetectionScreen(bitmap = bitmap, frameResult = frameResult, modifier = modifier)
        }
    }

    @Composable
    override fun rememberImagePickerHelper(
        onModeChange: (ScanMode) -> Unit,
        onResult: (FrameResult) -> Unit,
        onImageIdCaptured: (String) -> Unit
    ): ImagePickerHelper {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
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
                    withContext(Dispatchers.Main) {
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
            object : ImagePickerHelper {
                override fun launchGallery() {
                    galleryLauncher.launch("image/*")
                }

                override fun launchCamera() {
                    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                    val file = File.createTempFile("BUGSCANNER_${timeStamp}_", ".jpg", context.getExternalFilesDir(Environment.DIRECTORY_PICTURES))
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    capturedImageUri = uri
                    cameraLauncher.launch(uri)
                }
            }
        }
    }
}