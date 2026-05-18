package hcmus.bugscanner.core.utils

import androidx.compose.runtime.Composable

expect class ShareManager {
    fun shareBugInfo(bugName: String, scientificName: String)
}

// Hàm này giúp Compose nhớ và khởi tạo ShareManager phù hợp với từng nền tảng
@Composable
expect fun rememberShareManager(): ShareManager