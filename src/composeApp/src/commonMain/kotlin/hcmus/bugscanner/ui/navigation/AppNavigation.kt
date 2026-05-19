package hcmus.bugscanner.ui.navigation

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import hcmus.bugscanner.ui.auth.AuthScreen
import hcmus.bugscanner.ui.auth.AuthViewModel
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
        var isLoggedIn by remember { mutableStateOf(false) }
        var isGuest by remember { mutableStateOf(false) }

        val shareManager = rememberShareManager()

        if (showSplash) {
            SplashScreen(onSplashFinished = {
                showSplash = false
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
            HomeScreen(
                isLoggedIn = isLoggedIn,
                onAuthAction = {
                    authViewModel.signOut()
                    isLoggedIn = false
                    isGuest = false
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
    }
}