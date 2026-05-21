package hcmus.bugscanner.core.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.browser.window

class WebShareManager : ShareManager {
    override fun shareBugInfo(bugName: String, scientificName: String) {
        val shareText = "Tôi vừa phát hiện ra loài: $bugName trên ứng dụng BugScanner. Tên khoa học: $scientificName."

        try {
            val navigator = window.navigator.asDynamic()

            // Kiểm tra trình duyệt có Web Share API không
            if (navigator.share != undefined) {
                val shareData = js("{}")
                shareData.title = "Nhận diện côn trùng qua BugScanner"
                shareData.text = shareText

                // BẮT BUỘC PHẢI CÓ .catch() ĐỂ CHẶN LỖI DOMException CỦA TRÌNH DUYỆT
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

    // Hàm dự phòng: Tự động copy text nếu không Share được
    private fun fallbackToClipboard(text: String) {
        try {
            window.navigator.clipboard.writeText(text)
            window.alert("Trình duyệt đã chặn chia sẻ trực tiếp (Do bảo mật của Canvas). Đã tự động copy thông tin vào khay nhớ tạm!")
        } catch (e: Exception) {
            window.alert("Thông tin côn trùng:\n$text")
        }
    }
}

@Composable
actual fun rememberShareManager(): ShareManager {
    return remember { WebShareManager() }
}