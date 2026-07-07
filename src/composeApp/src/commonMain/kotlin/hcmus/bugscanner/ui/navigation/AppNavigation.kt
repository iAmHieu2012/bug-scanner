package hcmus.bugscanner.ui.navigation

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.*
import hcmus.bugscanner.domain.model.ConfidencePolicy
import hcmus.bugscanner.domain.model.HarmfulnessLevel
import hcmus.bugscanner.ui.auth.AuthScreen
import hcmus.bugscanner.ui.auth.AuthViewModel
import hcmus.bugscanner.ui.auth.AuthState
import hcmus.bugscanner.domain.repository.EncyclopediaRepository
import hcmus.bugscanner.ui.home.AppTab
import hcmus.bugscanner.ui.home.HomeScreen
import hcmus.bugscanner.ui.layout.AdaptiveLayoutSize
import hcmus.bugscanner.ui.splash.SplashScreen
import hcmus.bugscanner.ui.scan.ScanScreen
import hcmus.bugscanner.core.utils.rememberShareManager
import hcmus.bugscanner.ui.theme.AppTheme
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

import androidx.compose.foundation.isSystemInDarkTheme
import hcmus.bugscanner.ui.theme.ThemeMode

/**
 * Component quản lý luồng điều hướng chính, trạng thái đăng nhập và cấp quyền của ứng dụng.
 * Hoạt động như một Router trung tâm quyết định việc render màn hình dựa trên AuthState.
 *
 * @param windowSizeClass Thông số kích thước màn hình hiện tại để phân phối Responsive Layout.
 * @param layoutSize Kích thước thích ứng của màn hình (AdaptiveLayoutSize).
 * @param initialTab Tab ban đầu khi mở màn hình, mặc định là tab quét (AppTab.SCAN).
 * @param onTabChanged Callback kích hoạt khi người dùng chuyển tab.
 * @param authViewModel ViewModel quản lý trạng thái xác thực (Login/Guest) của hệ thống.
 */
@Composable
fun AppNavigation(
    windowSizeClass: WindowSizeClass,
    layoutSize: AdaptiveLayoutSize,
    initialTab: AppTab = AppTab.SCAN,
    onTabChanged: (AppTab) -> Unit = {},
    authViewModel: AuthViewModel = koinViewModel()
) {
    var themeMode by remember { mutableStateOf(ThemeMode.SYSTEM) }
    val useDarkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    AppTheme(useDarkTheme = useDarkTheme) {
        var showSplash by remember { mutableStateOf(true) }
        var isStartup by remember { mutableStateOf(true) }
        val authState by authViewModel.authState.collectAsState()
        val shareManager = rememberShareManager()
        val encyclopediaRepository: EncyclopediaRepository = koinInject()

        LaunchedEffect(authState) {
            if (authState is AuthState.Success) {
                encyclopediaRepository.prefetchDatabase()
            }
            if (authState is AuthState.Success || authState is AuthState.Unauthenticated || authState is AuthState.Error) {
                isStartup = false
            }
        }

        if (showSplash || (isStartup && authState !is AuthState.Error)) {
            SplashScreen(onSplashFinished = {
                showSplash = false
            })
        } else {
            when (val state = authState) {
                is AuthState.Success -> {
                    HomeScreen(
                        layoutSize = layoutSize,
                        initialTab = initialTab,
                        onTabChanged = onTabChanged,
                        isLoggedIn = !state.isGuest,
                        isAdmin = state.isAdmin,
                        themeMode = themeMode,
                        onThemeChange = { themeMode = it },
                        onAuthAction = {
                            authViewModel.signOut()
                        },
                        onShareClick = { bug, imageBytes, confidence ->
                            shareManager.shareBugInfo(
                                bugName = bug.name,
                                scientificName = bug.scientificName,
                                imageBytes = imageBytes,
                                confidenceLabel = if (confidence > 0f) {
                                    val confidenceInfo = ConfidencePolicy.explain(confidence)
                                    "${confidenceInfo.shortLabel} ${confidenceInfo.percentText}"
                                } else {
                                    ""
                                },
                                harmfulnessLabel = HarmfulnessLevel.fromValue(bug.harmfulnessLevel).label
                            )
                        },
                        scanTabContent = { onDetected ->
                            ScanScreen(
                                onDetectedBugClick = onDetected,
                                onNavigateToHistory = { onTabChanged(hcmus.bugscanner.ui.home.AppTab.HISTORY) }
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
