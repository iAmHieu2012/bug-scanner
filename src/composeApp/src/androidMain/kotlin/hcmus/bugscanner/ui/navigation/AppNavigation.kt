package hcmus.bugscanner.ui.navigation

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import hcmus.bugscanner.ml.YoloDetector
import hcmus.bugscanner.ui.auth.AuthScreen
import hcmus.bugscanner.ui.auth.AuthViewModel
import hcmus.bugscanner.ui.home.HomeScreen
import hcmus.bugscanner.ui.splash.CameraPermissionScreen
import hcmus.bugscanner.ui.splash.SplashScreen
import java.util.concurrent.ExecutorService

/**
 * Quản lý luồng điều hướng chính và trạng thái đăng nhập, cấp quyền.
 */
@Composable
fun AppNavigation(
    yoloDetector: YoloDetector,
    cameraExecutor: ExecutorService,
    authViewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    var showSplash by remember { mutableStateOf(true) }
    var isLoggedIn by remember { mutableStateOf(false) }
    var isGuest by remember { mutableStateOf(false) }

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

    if (showSplash) {
        SplashScreen(onSplashFinished = {
            showSplash = false
            if (!hasCameraPermission) {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        })
    } else if (!isLoggedIn && !isGuest) {
        AuthScreen(
            onLoginSuccess = { isGuestLogin ->
                if (isGuestLogin) {
                    isGuest = true
                    isLoggedIn = false
                } else {
                    isLoggedIn = true
                    isGuest = false
                }
            }
        )
    } else {
        if (hasCameraPermission) {
            HomeScreen(
                yoloDetector = yoloDetector,
                cameraExecutor = cameraExecutor,
                isLoggedIn = isLoggedIn,
                onAuthAction = {
                    authViewModel.signOut()
                    isLoggedIn = false
                    isGuest = false
                }
            )
        } else {
            CameraPermissionScreen {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }
}