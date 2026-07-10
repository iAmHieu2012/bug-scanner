package hcmus.bugscanner

import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseOptions
import dev.gitlive.firebase.initialize
import hcmus.bugscanner.ui.home.AppTab
import hcmus.bugscanner.ui.home.appTabFromHash
import hcmus.bugscanner.ui.home.toHashRoute
import hcmus.bugscanner.ui.scan.LocalPlatformScanProvider
import hcmus.bugscanner.ui.scan.WebScanProvider
import kotlinx.browser.document
import kotlinx.browser.window
import org.jetbrains.skiko.wasm.onWasmReady
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event

/**
 * Điểm bắt đầu (Entry Point) của ứng dụng trên nền tảng Web (Wasm).
 */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    try {
        Firebase.initialize(
            options = FirebaseOptions(
                applicationId = "1:302576079512:web:328d85f070b1b113c3f6ed",
                gcmSenderId = "302576079512",
                apiKey = "AIzaSyBFu8YwXYP6CH6Gmjji_qTGAcyotg4voe0",
                projectId = "bugscanner-test-trung-2026",
                storageBucket = "bugscanner-test-trung-2026.firebasestorage.app",
                authDomain = "bugscanner-test-trung-2026.firebaseapp.com"
            )
        )

        if (window.location.hash.isBlank()) {
            window.history.asDynamic().replaceState(null, "", "#/scan")
        }

        onWasmReady {
            document.getElementById("loading")?.remove()
            val root = document.getElementById("app") as? HTMLElement ?: document.body!!

            ComposeViewport(root) {
                var routeTab by remember { mutableStateOf(appTabFromHash(window.location.hash)) }

                DisposableEffect(Unit) {
                    val listener: (Event) -> Unit = {
                        routeTab = appTabFromHash(window.location.hash)
                    }
                    window.addEventListener("hashchange", listener)
                    onDispose { window.removeEventListener("hashchange", listener) }
                }

                LaunchedEffect(routeTab) {
                    document.title = when (routeTab) {
                        AppTab.SCAN -> "Nhận diện - BugScanner"
                        AppTab.HISTORY -> "Lịch sử - BugScanner"
                        AppTab.WIKI -> "Bách khoa - BugScanner"
                        AppTab.CHATBOT -> "Trợ lý - BugScanner"
                        AppTab.PROFILE -> "Tài khoản - BugScanner"
                        AppTab.ADMIN -> "Quản trị - BugScanner"
                    }
                }

                CompositionLocalProvider(LocalPlatformScanProvider provides WebScanProvider) {
                    App(
                        initialTab = routeTab,
                        onTabChanged = { tab ->
                            val route = tab.toHashRoute()
                            if (window.location.hash != route) window.location.hash = route
                        }
                    )
                }
            }
        }
    } catch (error: Throwable) {
        console.error("Lỗi khởi tạo ứng dụng: ", error)
        document.getElementById("loading")?.textContent = "Không thể khởi động BugScanner."
    }
}
