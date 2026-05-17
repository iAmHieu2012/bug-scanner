package hcmus.bugscanner.ui.scan

import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import hcmus.bugscanner.ml.YoloDetector
import hcmus.bugscanner.ui.scan.components.DetectionPanel
import hcmus.bugscanner.core.utils.uriToBitmap
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService

enum class ScanMode { LIVE, IMAGE_UPLOAD, CAMERA_CAPTURE }

val SeedGreen = Color(0xFF2E7D32)
val DeepForest = Color(0xFF1B5E20)

/**
 * Màn hình xử lý chức năng quét và nhận diện côn trùng.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    yoloDetector: YoloDetector,
    cameraExecutor: ExecutorService,
    isLoggedIn: Boolean,
    onAuthAction: () -> Unit,
    onDetectedBugClick: (String) -> Unit
) {
    val context = LocalContext.current
    val viewModel: ScanViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ScanViewModel(yoloDetector, cameraExecutor) as T
            }
        }
    )
    val frameResult by viewModel.frameResult.collectAsState()
    var currentMode by remember { mutableStateOf(ScanMode.LIVE) }
    var staticBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }

    fun createImageUri(): Uri {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val file = File.createTempFile("BUGSCANNER_${timeStamp}_", ".jpg", storageDir)
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            currentMode = ScanMode.IMAGE_UPLOAD
            viewModel.clearResult()
            val bmp = uriToBitmap(context, it)
            staticBitmap = bmp
            bmp?.let { validBmp -> viewModel.analyzeImage(validBmp, 0) }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            capturedImageUri?.let { uri ->
                currentMode = ScanMode.CAMERA_CAPTURE
                viewModel.clearResult()
                val bmp = uriToBitmap(context, uri)
                staticBitmap = bmp
                bmp?.let { validBmp -> viewModel.analyzeImage(validBmp, 0) }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Xin chào! \uD83C\uDF3F", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
                Text("Hôm nay bạn tìm gì?", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = DeepForest)
            }

            IconButton(
                onClick = onAuthAction,
                modifier = Modifier.background(Color(0xFFC8E6C9), CircleShape)
            ) {
                Icon(
                    imageVector = if (isLoggedIn) Icons.AutoMirrored.Rounded.Logout else Icons.AutoMirrored.Rounded.Login,
                    contentDescription = if (isLoggedIn) "Đăng xuất" else "Đăng nhập",
                    tint = DeepForest
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(32.dp))
                .border(2.dp, Color.White, RoundedCornerShape(32.dp))
                .background(Color.Black)
        ) {
            if (currentMode == ScanMode.LIVE) {
                CameraScreen(viewModel)
                ScannerOverlay()
            } else {
                StaticDetectionScreen(staticBitmap, frameResult)
            }

            val quickModes: List<Pair<ScanMode, ImageVector>> = listOf(
                ScanMode.LIVE to Icons.Rounded.Videocam,
                ScanMode.IMAGE_UPLOAD to Icons.Rounded.PhotoLibrary,
                ScanMode.CAMERA_CAPTURE to Icons.Rounded.Camera
            )

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                    .padding(4.dp)
            ) {
                quickModes.forEach { (mode, icon) ->
                    IconButton(
                        onClick = {
                            when (mode) {
                                ScanMode.LIVE -> {
                                    currentMode = mode
                                    viewModel.clearResult()
                                    staticBitmap = null
                                }
                                ScanMode.IMAGE_UPLOAD -> galleryLauncher.launch("image/*")
                                else -> {
                                    val uri = createImageUri()
                                    capturedImageUri = uri
                                    cameraLauncher.launch(uri)
                                }
                            }
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (currentMode == mode) SeedGreen else Color.Transparent)
                    ) {
                        Icon(icon, null, tint = Color.White)
                    }
                }
            }
        }
        DetectionPanel(frameResult, onDetectedBugClick)
    }
}

/**
 * Lớp phủ UI khung ngắm chữ thập trên Camera.
 */
@Composable
fun ScannerOverlay() {
    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize().padding(60.dp)) {
            val strokeWidth = 10f
            val cornerLength = 60f
            val color = Color.White.copy(alpha = 0.8f)

            drawLine(color, Offset(0f, 0f), Offset(cornerLength, 0f), strokeWidth)
            drawLine(color, Offset(0f, 0f), Offset(0f, cornerLength), strokeWidth)
            drawLine(color, Offset(size.width, 0f), Offset(size.width - cornerLength, 0f), strokeWidth)
            drawLine(color, Offset(size.width, 0f), Offset(size.width, cornerLength), strokeWidth)
            drawLine(color, Offset(0f, size.height), Offset(cornerLength, size.height), strokeWidth)
            drawLine(color, Offset(0f, size.height), Offset(0f, size.height - cornerLength), strokeWidth)
            drawLine(color, Offset(size.width, size.height), Offset(size.width - cornerLength, size.height), strokeWidth)
            drawLine(color, Offset(size.width, size.height), Offset(size.width, size.height - cornerLength), strokeWidth)
        }
    }
}