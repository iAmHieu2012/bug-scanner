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
                applicationId = "1:744753522860:web:a9559e74b6af5005801709",
                gcmSenderId = "744753522860",
                apiKey = "AIzaSyCqVJZdUMpnGBsk6gP6gXd0vYxwrjAh6u8",
                projectId = "bugscanner-2026",
                storageBucket = "bugscanner-2026.firebasestorage.app",
                authDomain = "bugscanner-2026.firebaseapp.com"
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
