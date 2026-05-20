package hcmus.bugscanner.core.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.browser.window

class WebShareManager : ShareManager {
    override fun shareBugInfo(bugName: String, scientificName: String) {
        // Tạo object chứa data để share của JS
        val shareData = js("{}")
        shareData.title = "Nhận diện côn trùng qua BugScanner"
        shareData.text = "Tôi vừa phát hiện ra loài: $bugName trên ứng dụng BugScanner. Tên khoa học: $scientificName."

        // Kiểm tra xem trình duyệt có hỗ trợ Web Share API không
        if (window.navigator.asDynamic().share != undefined) {
            window.navigator.asDynamic().share(shareData)
        } else {
            // Nếu dùng trình duyệt cũ/không hỗ trợ, hiển thị hộp thoại thông báo
            window.alert("Trình duyệt không hỗ trợ chia sẻ. Bạn đang xem loài: $bugName")
        }
    }
}

@Composable
actual fun rememberShareManager(): ShareManager {
    return remember { WebShareManager() }
}