package hcmus.bugscanner

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseOptions
import dev.gitlive.firebase.initialize
import hcmus.bugscanner.ui.scan.LocalPlatformScanProvider
import hcmus.bugscanner.ui.scan.WebScanProvider
import kotlinx.browser.document
import org.jetbrains.skiko.wasm.onWasmReady

/**
 * Điểm bắt đầu (Entry Point) của ứng dụng trên nền tảng Web (Wasm).
 */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    try {
        // Truyền trực tiếp cấu hình vào FirebaseOptions (Bỏ qua fetch file tĩnh)
        val options = FirebaseOptions(
            applicationId = "1:744753522860:web:a9559e74b6af5005801709",
            gcmSenderId = "744753522860",
            apiKey = "AIzaSyCqVJZdUMpnGBsk6gP6gXd0vYxwrjAh6u8",
            projectId = "bugscanner-2026",
            storageBucket = "bugscanner-2026.firebasestorage.app",
            authDomain = "bugscanner-2026.firebaseapp.com"
        )

        Firebase.initialize(options = options)

        // Chờ engine Wasm sẵn sàng trước khi vẽ giao diện lên DOM
        onWasmReady {
            ComposeViewport(document.body!!) {
                // Tiêm WebScanProvider (chứa logic Camera trình duyệt) vào toàn bộ App
                CompositionLocalProvider(
                    LocalPlatformScanProvider provides WebScanProvider
                ) {
                    App()
                }
            }
        }
    } catch (error: Throwable) {
        console.error("Lỗi khởi tạo Firebase: ", error)
    }
}