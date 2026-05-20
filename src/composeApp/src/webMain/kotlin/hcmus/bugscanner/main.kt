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

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    window.fetch("firebase-config.json")
        .then { response ->
            if (!response.ok) throw Exception("Không tìm thấy file firebase-config.json")
            response.json()
        }
        .then { jsonConfig ->
            val dynamicConfig = jsonConfig.asDynamic()

            val options = FirebaseOptions(
                applicationId = dynamicConfig.appId as String,
                gcmSenderId = dynamicConfig.messagingSenderId as String,
                authDomain = dynamicConfig.authDomain as String,
                apiKey = dynamicConfig.apiKey as String,
                projectId = dynamicConfig.projectId as String,
                storageBucket = dynamicConfig.storageBucket as String
            )

            Firebase.initialize(options = options)

            onWasmReady {
                ComposeViewport(document.body!!) {
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