package hcmus.bugscanner.core.utils

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Triển khai logic Share thông qua hệ thống Intent của Android.
 */
class AndroidShareManager(private val context: Context) : ShareManager {
    override fun shareBugInfo(bugName: String, scientificName: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Nhận diện côn trùng qua BugScanner")
            putExtra(Intent.EXTRA_TEXT, "Tôi vừa phát hiện ra loài: $bugName trên ứng dụng BugScanner. Tên khoa học: $scientificName.\nTìm hiểu ngay ứng dụng BugScanner!")
        }

        // Hiển thị bảng chọn ứng dụng (Chooser) để người dùng quyết định nơi chia sẻ
        context.startActivity(Intent.createChooser(shareIntent, "Chia sẻ thông tin qua"))
    }
}

/**
 * Khởi tạo và ghi nhớ AndroidShareManager, tự động trích xuất và liên kết với Android Context hiện tại.
 */
@Composable
actual fun rememberShareManager(): ShareManager {
    val context = LocalContext.current
    return remember(context) { AndroidShareManager(context) }
}