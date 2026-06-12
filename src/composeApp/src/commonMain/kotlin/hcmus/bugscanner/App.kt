package hcmus.bugscanner

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import hcmus.bugscanner.core.di.appModule
import hcmus.bugscanner.ui.home.AppTab
import hcmus.bugscanner.ui.layout.classifyAdaptiveWidth
import hcmus.bugscanner.ui.navigation.AppNavigation
import org.koin.compose.KoinApplication
import org.koin.dsl.koinConfiguration

/**
 * Điểm bắt đầu (Root Composable) của toàn bộ ứng dụng BugScanner.
 * Chịu trách nhiệm khởi tạo cây Dependency Injection (Koin) và tính toán WindowSizeClass
 * để hỗ trợ giao diện Adaptive (Responsive) trên mọi nền tảng (Mobile, Tablet, Web, Desktop).
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun App(
    initialTab: AppTab = AppTab.SCAN,
    onTabChanged: (AppTab) -> Unit = {}
) {
    KoinApplication(
        configuration = koinConfiguration {
            modules(appModule)
        }
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(maxWidth, maxHeight))
            AppNavigation(
                windowSizeClass = windowSizeClass,
                layoutSize = classifyAdaptiveWidth(maxWidth.value),
                initialTab = initialTab,
                onTabChanged = onTabChanged
            )
        }
    }
}