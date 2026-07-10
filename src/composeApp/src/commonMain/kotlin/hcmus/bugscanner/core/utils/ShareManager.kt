package hcmus.bugscanner.core.utils

import androidx.compose.runtime.Composable

/**
 * Interface quản lý logic Share chung cho mọi nền tảng.
 */
interface ShareManager {
    /**
     * Hàm xử lý chia sẻ thông tin côn trùng kèm hình ảnh và link.
     *
     * @param bugName Tên phổ thông của côn trùng.
     * @param scientificName Tên khoa học của côn trùng.
     * @param imageBytes Mảng byte của hình ảnh (nếu có).
     * @param confidenceLabel Độ tin cậy của kết quả nhận diện.
     * @param harmfulnessLabel Mức độ gây hại của côn trùng.
     * @param appLink Đường dẫn tải app hoặc trang web.
     */
    fun shareBugInfo(
        bugName: String,
        scientificName: String,
        imageBytes: ByteArray? = null,
        confidenceLabel: String = "",
        harmfulnessLabel: String = "",
        appLink: String = "https://bugscanner-2026.web.app"
    )
}

/**
 * Hàm khởi tạo ShareManager tương ứng với từng nền tảng.
 */
@Composable
expect fun rememberShareManager(): ShareManager
