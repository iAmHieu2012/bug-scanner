package hcmus.bugscanner.core.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.io.File
import java.io.FileOutputStream

/**
 * Triển khai cụ thể (Implementation) của [ShareManager] dành riêng cho nền tảng Android.
 * Sử dụng hệ thống Intent gốc của Android kết hợp với [FileProvider] để chia sẻ an toàn văn bản và hình ảnh.
 *
 * @property context Context của ứng dụng Android dùng để khởi chạy Intent và truy cập bộ nhớ đệm (Cache).
 */
class AndroidShareManager(private val context: Context) : ShareManager {

    /**
     * Mở hộp thoại chia sẻ (Share Sheet) của Android.
     * Tự động lưu mảng byte hình ảnh (nếu có) thành tệp tạm thời và đính kèm vào Intent.
     *
     * @param bugName Tên phổ thông của côn trùng.
     * @param scientificName Tên khoa học của côn trùng.
     * @param imageBytes Mảng byte của hình ảnh côn trùng đã quét (có thể null nếu chỉ share chữ).
     * @param appLink Đường dẫn tải app hoặc trang web để người dùng khác click vào.
     */
    override fun shareBugInfo(
        bugName: String,
        scientificName: String,
        imageBytes: ByteArray?,
        confidenceLabel: String,
        harmfulnessLabel: String,
        appLink: String
    ) {
        val extraLines = listOf(
            confidenceLabel.takeIf { it.isNotBlank() }?.let { "Độ tin cậy: $it" },
            harmfulnessLabel.takeIf { it.isNotBlank() }?.let { "Mức gây hại: $it" }
        ).filterNotNull()
        val shareText = buildString {
            appendLine("Tôi vừa phát hiện ra loài: $bugName trên BugScanner.")
            appendLine("Tên khoa học: $scientificName.")
            extraLines.forEach { appendLine(it) }
            appendLine()
            append("Khám phá ngay tại: $appLink")
        }

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("BugScanner Share", shareText)
        clipboard.setPrimaryClip(clip)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            if (imageBytes != null && imageBytes.isNotEmpty()) {
                try {
                    val cachePath = File(context.externalCacheDir ?: context.cacheDir, "shared_images")
                    cachePath.mkdirs()

                    val file = File(cachePath, "bug_scanned_image.jpg")
                    FileOutputStream(file).use { stream ->
                        stream.write(imageBytes)
                    }

                    val authority = "${context.packageName}.fileprovider"
                    val uri = FileProvider.getUriForFile(context, authority, file)

                    type = "image/jpeg"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    clipData = ClipData.newRawUri("", uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
        }

        Toast.makeText(context, "Đã sao chép nội dung, hãy 'Dán' (Paste) chữ khi gửi nhé!", Toast.LENGTH_LONG).show()

        val chooser = Intent.createChooser(shareIntent, "Chia sẻ kết quả qua")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(chooser)
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
