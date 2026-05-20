package hcmus.bugscanner.ui.navigation

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import hcmus.bugscanner.ui.auth.AuthScreen
import hcmus.bugscanner.ui.auth.AuthViewModel
import hcmus.bugscanner.ui.auth.AuthState
import hcmus.bugscanner.ui.home.HomeScreen
import hcmus.bugscanner.ui.splash.SplashScreen
import hcmus.bugscanner.ui.scan.ScanScreen
import hcmus.bugscanner.core.utils.rememberShareManager
import hcmus.bugscanner.ui.theme.AppTheme

/**
 * Quản lý luồng điều hướng chính và trạng thái đăng nhập, cấp quyền.
 */
@Composable
fun AppNavigation(
    authViewModel: AuthViewModel = viewModel()
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
                    // Trạng thái Success: Đã đăng nhập (có thể là Guest hoặc User thật)
                    HomeScreen(
                        isLoggedIn = !state.isGuest, // Nếu không phải Guest thì là LoggedIn = true
                        onAuthAction = {
                            authViewModel.signOut() // Gọi thẳng hàm signOut của ViewModel
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
                    // Các trạng thái còn lại (Idle, Loading, Error): Hiển thị màn hình đăng nhập
                    AuthScreen()
                }
            }
        }
    }
}