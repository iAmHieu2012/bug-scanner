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
import kotlinx.coroutines.launch
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.readBytes

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
        var showAuthScreen by remember { mutableStateOf(false) }
        val shareManager = rememberShareManager()
        val encyclopediaRepository: EncyclopediaRepository = koinInject()
        val httpClient: HttpClient = koinInject()
        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(authState) {
            if (authState is AuthState.Success) {
                showAuthScreen = false
                launch { encyclopediaRepository.prefetchDatabase() }
            }
            if (authState is AuthState.Unauthenticated) {
                if (isStartup) {
                    showAuthScreen = true
                }
            }
            if (authState is AuthState.Success || authState is AuthState.Unauthenticated || authState is AuthState.Error) {
                isStartup = false
            }
        }

        if (showSplash || (isStartup && authState !is AuthState.Error)) {
            SplashScreen(onSplashFinished = {
                showSplash = false
            })
        } else if (showAuthScreen) {
            AuthScreen(
                windowSizeClass = windowSizeClass,
                onSkipAuth = { showAuthScreen = false }
            )
        } else {
            val state = authState as? AuthState.Success
            HomeScreen(
                layoutSize = layoutSize,
                initialTab = initialTab,
                onTabChanged = onTabChanged,
                isLoggedIn = state != null,
                isAdmin = state?.isAdmin == true,
                themeMode = themeMode,
                onThemeChange = { themeMode = it },
                onAuthAction = {
                    if (state != null) {
                        authViewModel.signOut()
                    } else {
                        showAuthScreen = true
                    }
                },
                onShareClick = { bug, imageBytes, confidence ->
                    val shareAction = { bytes: ByteArray? ->
                        shareManager.shareBugInfo(
                            bugName = bug.name,
                            scientificName = bug.scientificName,
                            imageBytes = bytes,
                            confidenceLabel = if (confidence > 0f) {
                                val confidenceInfo = ConfidencePolicy.explain(confidence)
                                "${confidenceInfo.shortLabel} ${confidenceInfo.percentText}"
                            } else {
                                ""
                            },
                            harmfulnessLabel = HarmfulnessLevel.fromValue(bug.harmfulnessLevel).label
                        )
                    }

                    if (imageBytes != null) {
                        shareAction(imageBytes)
                    } else if (bug.imageUrl.isNotBlank()) {
                        coroutineScope.launch {
                            try {
                                val bytes = httpClient.get(bug.imageUrl).readBytes()
                                shareAction(bytes)
                            } catch (e: Exception) {
                                shareAction(null)
                            }
                        }
                    } else {
                        shareAction(null)
                    }
                },
                scanTabContent = { onDetected ->
                    ScanScreen(
                        onDetectedBugClick = onDetected,
                        onNavigateToHistory = { onTabChanged(hcmus.bugscanner.ui.home.AppTab.HISTORY) }
                    )
                }
            )
        }
    }
}
