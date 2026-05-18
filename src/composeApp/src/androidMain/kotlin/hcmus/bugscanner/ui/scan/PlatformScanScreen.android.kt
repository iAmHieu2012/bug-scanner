package hcmus.bugscanner.ui.scan

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import hcmus.bugscanner.ml.YoloDetector
import hcmus.bugscanner.ui.splash.CameraPermissionScreen
import java.util.concurrent.Executors

@Composable
actual fun PlatformScanScreen(
    isLoggedIn: Boolean,
    onAuthAction: () -> Unit,
    onDetectedBugClick: (String) -> Unit
) {
    val context = LocalContext.current

    // Logic kiểm tra và xin quyền Camera của Android
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    // Nếu có quyền thì mở Camera & Yolo, chưa có thì hiện màn hình xin quyền
    if (hasCameraPermission) {
        val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
        val yoloDetector = remember { YoloDetector(context) }

        DisposableEffect(Unit) {
            onDispose {
                cameraExecutor.shutdown()
            }
        }

        ScanScreen(
            yoloDetector = yoloDetector,
            cameraExecutor = cameraExecutor,
            isLoggedIn = isLoggedIn,
            onAuthAction = onAuthAction,
            onDetectedBugClick = onDetectedBugClick
        )
    } else {
        CameraPermissionScreen {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
}