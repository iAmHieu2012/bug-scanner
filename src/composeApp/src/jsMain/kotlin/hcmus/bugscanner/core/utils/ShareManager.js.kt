package hcmus.bugscanner.core.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.browser.window

/**
 * Triển khai cụ thể (Implementation) của [ShareManager] dành riêng cho nền tảng Web (Wasm/JS).
 * Sử dụng Web Share API của trình duyệt.
 */
class WebShareManager : ShareManager {
    /**
     * Mở hộp thoại chia sẻ gốc của hệ điều hành/trình duyệt.
     * Nếu trình duyệt không hỗ trợ hoặc chặn API do lý do bảo mật, hệ thống sẽ tự động chuyển sang chế độ copy văn bản.
     *
     * @param bugName Tên phổ thông của côn trùng.
     * @param scientificName Tên khoa học của côn trùng.
     */
    override fun shareBugInfo(bugName: String, scientificName: String) {
        val shareText = "Tôi vừa phát hiện ra loài: $bugName trên ứng dụng BugScanner. Tên khoa học: $scientificName."

        try {
            val navigator = window.navigator.asDynamic()

            // Kiểm tra xem trình duyệt có hỗ trợ Web Share API hay không
            if (navigator.share != undefined) {
                val shareData = js("{}")
                shareData.title = "Nhận diện côn trùng qua BugScanner"
                shareData.text = shareText

                // BẮT BUỘC PHẢI CÓ .catch() ĐỂ CHẶN LỖI DOMException CỦA TRÌNH DUYỆT
                // Xảy ra khi người dùng hủy chia sẻ hoặc trình duyệt chặn (ví dụ: iframe, thiếu HTTPS).
                val promise = navigator.share(shareData)
                promise.catch {
                    fallbackToClipboard(shareText)
                }
            } else {
                fallbackToClipboard(shareText)
            }
        } catch (e: Exception) {
            fallbackToClipboard(shareText)
        }
    }

    /**
     * Hàm dự phòng: Tự động sao chép văn bản vào khay nhớ tạm (Clipboard) nếu tính năng Share bị lỗi.
     * Hiển thị thông báo (Alert) cho người dùng biết trạng thái.
     *
     * @param text Nội dung văn bản cần sao chép.
     */
    private fun fallbackToClipboard(text: String) {
        try {
            window.navigator.clipboard.writeText(text)
            window.alert("Trình duyệt đã chặn chia sẻ trực tiếp (Do bảo mật của Canvas). Đã tự động copy thông tin vào khay nhớ tạm!")
        } catch (e: Exception) {
            window.alert("Thông tin côn trùng:\n$text")
        }
    }
}

/**
 * Hàm actual khởi tạo và ghi nhớ [WebShareManager] trên nền tảng Web.
 *
 * @return Phiên bản [ShareManager] hoạt động trên trình duyệt.
 */
@Composable
actual fun rememberShareManager(): ShareManager {
    return remember { WebShareManager() }
}