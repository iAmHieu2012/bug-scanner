package hcmus.bugscanner.core.utils

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

// Thực thi logic Share bằng Intent của Android
actual class ShareManager(private val context: Context) {
    actual fun shareBugInfo(bugName: String, scientificName: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Nhận diện côn trùng qua BugScanner")
            putExtra(Intent.EXTRA_TEXT, "Tôi vừa phát hiện ra loài: $bugName trên ứng dụng BugScanner. Tên khoa học: $scientificName.\nTìm hiểu ngay ứng dụng BugScanner!")
        }
        // Gọi màn hình chọn app để chia sẻ
        context.startActivity(Intent.createChooser(shareIntent, "Chia sẻ thông tin qua"))
    }
}

// Khởi tạo ShareManager và tự động lấy Context của Android truyền vào
@Composable
actual fun rememberShareManager(): ShareManager {
    val context = LocalContext.current
    return remember(context) { ShareManager(context) }
}