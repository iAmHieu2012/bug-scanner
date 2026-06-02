package hcmus.bugscanner.ui.navigation

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.*
import hcmus.bugscanner.ui.auth.AuthScreen
import hcmus.bugscanner.ui.auth.AuthViewModel
import hcmus.bugscanner.ui.auth.AuthState
import hcmus.bugscanner.ui.home.HomeScreen
import hcmus.bugscanner.ui.splash.SplashScreen
import hcmus.bugscanner.ui.scan.ScanScreen
import hcmus.bugscanner.core.utils.rememberShareManager
import hcmus.bugscanner.ui.theme.AppTheme
import org.koin.compose.viewmodel.koinViewModel

/**
 * Component quản lý luồng điều hướng chính, trạng thái đăng nhập và cấp quyền của ứng dụng.
 * Hoạt động như một Router trung tâm quyết định việc render màn hình dựa trên AuthState.
 *
 * @param windowSizeClass Thông số kích thước màn hình hiện tại để phân phối Responsive Layout.
 * @param authViewModel ViewModel quản lý trạng thái xác thực (Login/Guest) của hệ thống.
 */
@Composable
fun AppNavigation(
    windowSizeClass: WindowSizeClass,
    authViewModel: AuthViewModel = koinViewModel()
) {
    AppTheme {
        var showSplash by remember { mutableStateOf(true) }
        val authState by authViewModel.authState.collectAsState()
        val shareManager = rememberShareManager()

        if (showSplash) {
            SplashScreen(onSplashFinished = {
                showSplash = false
            })
        } else {
            when (val state = authState) {
                is AuthState.Success -> {
                    HomeScreen(
                        windowSizeClass = windowSizeClass,
                        isLoggedIn = !state.isGuest,
                        onAuthAction = {
                            authViewModel.signOut()
                        },
                        onShareClick = { bug ->
                            shareManager.shareBugInfo(bug.name, bug.scientificName)
                        },
                        scanTabContent = { isLog, onAuth, onDetected ->
                            ScanScreen(
                                isLoggedIn = isLog,
                                onAuthAction = onAuth,
                                onDetectedBugClick = onDetected
                            )
                        }
                    )
                }
                else -> {
                    AuthScreen(windowSizeClass = windowSizeClass)
                }
            }
        }
    }
}