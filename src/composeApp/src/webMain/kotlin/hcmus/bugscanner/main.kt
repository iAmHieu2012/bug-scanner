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
import kotlinx.browser.window
import org.jetbrains.skiko.wasm.onWasmReady

/**
 * Điểm bắt đầu (Entry Point) của ứng dụng trên nền tảng Web (Wasm).
 * Chịu trách nhiệm tải cấu hình Firebase từ file JSON tĩnh và khởi tạo giao diện Compose.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // Tải tệp cấu hình Firebase từ thư mục public/web của server
    window.fetch("firebase-config.json", js("{}"))
        .then { response ->
            if (!response.ok) throw Exception("Không tìm thấy file firebase-config.json")
            response.json()
        }
        .then { jsonConfig ->
            val dynamicConfig = jsonConfig.asDynamic()

            // Khởi tạo thông số Firebase tương thích với thư viện GitLive
            val options = FirebaseOptions(
                applicationId = dynamicConfig.appId as String,
                gcmSenderId = dynamicConfig.messagingSenderId as String,
                authDomain = dynamicConfig.authDomain as String,
                apiKey = dynamicConfig.apiKey as String,
                projectId = dynamicConfig.projectId as String,
                storageBucket = dynamicConfig.storageBucket as String
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
        }.catch { error ->
            console.error("Lỗi khởi tạo Firebase: ", error)
        }
}