package hcmus.bugscanner.core.utils

import androidx.compose.runtime.Composable

/**
 * Interface quản lý logic Share chung cho mọi nền tảng.
 */
interface ShareManager {
    /**
     * Hàm xử lý chia sẻ thông tin côn trùng.
     *
     * @param bugName Tên phổ thông của côn trùng.
     * @param scientificName Tên khoa học của côn trùng.
     */
    fun shareBugInfo(bugName: String, scientificName: String)
}

/**
 * Hàm khởi tạo ShareManager tương ứng với từng nền tảng.
 */
@Composable
expect fun rememberShareManager(): ShareManager