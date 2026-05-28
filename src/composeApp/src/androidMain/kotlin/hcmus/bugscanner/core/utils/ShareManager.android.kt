package hcmus.bugscanner.core.utils

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Triển khai cụ thể (Implementation) của [ShareManager] dành riêng cho nền tảng Android.
 * Sử dụng hệ thống Intent gốc của Android để gọi bảng chia sẻ (Share Sheet).
 *
 * @property context Context của ứng dụng Android dùng để khởi chạy Intent.
 */
class AndroidShareManager(private val context: Context) : ShareManager {
    /**
     * Mở hộp thoại chia sẻ dữ liệu côn trùng qua các ứng dụng khác (Mạng xã hội, Tin nhắn...).
     *
     * @param bugName Tên phổ thông của côn trùng.
     * @param scientificName Tên khoa học của côn trùng.
     */
    override fun shareBugInfo(bugName: String, scientificName: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Nhận diện côn trùng qua BugScanner")
            putExtra(
                Intent.EXTRA_TEXT,
                "Tôi vừa phát hiện ra loài: $bugName trên ứng dụng BugScanner. Tên khoa học: $scientificName.\nTìm hiểu ngay ứng dụng BugScanner!"
            )
        }

        // Hiển thị bảng chọn ứng dụng (Chooser) để người dùng quyết định nơi chia sẻ
        context.startActivity(Intent.createChooser(shareIntent, "Chia sẻ thông tin qua"))
    }
}

/**
 * Hàm actual khởi tạo và ghi nhớ [AndroidShareManager].
 * Tự động trích xuất và liên kết với Android Context hiện tại thông qua [LocalContext].
 *
 * @return Phiên bản [ShareManager] hoạt động trên nền tảng Android.
 */
@Composable
actual fun rememberShareManager(): ShareManager {
    val context = LocalContext.current
    return remember(context) { AndroidShareManager(context) }
}