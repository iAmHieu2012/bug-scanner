package hcmus.bugscanner.ui.navigation

import androidx.compose.material3.windowsizeclass.WindowSizeClass
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
 * Component quản lý luồng điều hướng chính, trạng thái đăng nhập và cấp quyền của ứng dụng.
 * Hoạt động như một Router trung tâm của toàn bộ ứng dụng, quyết định màn hình nào sẽ được hiển thị.
 *
 * @param windowSizeClass Thông số kích thước màn hình hiện tại (được cung cấp từ cấp cao hơn) để phân phối
 * xuống các màn hình con, giúp xử lý Responsive Layout tự động.
 * @param authViewModel ViewModel quản lý trạng thái xác thực (Login/Guest) của hệ thống.
 */
@Composable
fun AppNavigation(
    windowSizeClass: WindowSizeClass, // <-- THÊM THAM SỐ NHẬN DIỆN KÍCH THƯỚC MÀN HÌNH
    authViewModel: AuthViewModel = viewModel { AuthViewModel() }
) {
    AppTheme {
        // State quản lý việc hiển thị màn hình Splash ban đầu
        var showSplash by remember { mutableStateOf(true) }

        // Theo dõi trạng thái đăng nhập từ Firebase thông qua ViewModel
        val authState by authViewModel.authState.collectAsState()

        // Trình quản lý chia sẻ dữ liệu native tùy thuộc vào nền tảng (Android/Web)
        val shareManager = rememberShareManager()

        // LUỒNG ĐIỀU HƯỚNG CẤP 1: Xử lý Splash Screen
        if (showSplash) {
            SplashScreen(onSplashFinished = {
                showSplash = false
            })
        } else {
            // LUỒNG ĐIỀU HƯỚNG CẤP 2: Phân nhánh dựa trên trạng thái xác thực (Authentication State)
            when (val state = authState) {
                is AuthState.Success -> {
                    // Trạng thái Success: Người dùng đã vào app (có thể là Khách hoặc User đã xác thực)
                    HomeScreen(
                        windowSizeClass = windowSizeClass, // <-- TRUYỀN XUỐNG HOMESCREEN ĐỂ CHIA 2 CỘT
                        isLoggedIn = !state.isGuest,       // Xác định cờ LoggedIn để chặn/mở tính năng
                        onAuthAction = {
                            authViewModel.signOut() // Gọi thẳng hàm signOut của ViewModel
                        },
                        onShareClick = { bug ->
                            shareManager.shareBugInfo(bug.name, bug.scientificName)
                        },
                        scanTabContent = { isLog, onAuth, onDetected ->
                            // Inject ScanScreen vào HomeScreen để giảm thiểu sự phụ thuộc vòng (Circular Dependency)
                            ScanScreen(
                                isLoggedIn = isLog,
                                onAuthAction = onAuth,
                                onDetectedBugClick = onDetected
                            )
                        }
                    )
                }
                else -> {
                    // Các trạng thái còn lại (Idle, Loading, Error): Điều hướng về màn hình Đăng nhập
                    AuthScreen(windowSizeClass = windowSizeClass)
                }
            }
        }
    }
}