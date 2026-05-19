package hcmus.bugscanner.core.utils

import androidx.compose.runtime.Composable

/**
 * Interface quản lý logic Share chung cho mọi nền tảng.
 */
interface ShareManager {
    fun shareBugInfo(bugName: String, scientificName: String)
}

/**
 * Hàm khởi tạo ShareManager tương ứng với từng nền tảng.
 */
@Composable
expect fun rememberShareManager(): ShareManager