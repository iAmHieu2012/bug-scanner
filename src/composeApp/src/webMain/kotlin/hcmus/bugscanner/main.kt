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

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val options = FirebaseOptions(
        applicationId = "1:744753522860:web:9d8ac2353252c7f4801709",
        authDomain= "bugscanner-2026.firebaseapp.com",
        apiKey = "AIzaSyBupHX7E2T9_c5zvO68-zeDbGSep2eH3nk",
        projectId = "bugscanner-2026",
        storageBucket = "bugscanner-2026.firebasestorage.app",
        gcmSenderId = "744753522860"
    )

    Firebase.initialize(options = options)

    onWasmReady {
        ComposeViewport(document.body!!) {
            // CUNG CẤP WEBSCANPROVIDER CHO CÂY UI
            CompositionLocalProvider(
                LocalPlatformScanProvider provides WebScanProvider
            ) {
                App()
            }
        }
    }
}