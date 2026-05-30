package hcmus.bugscanner.core.utils

import android.content.Context
import android.content.Intent
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
    override fun shareBugInfo(bugName: String, scientificName: String, imageBytes: ByteArray?, appLink: String) {
        val shareText = "Tôi vừa phát hiện ra loài: $bugName trên BugScanner.\nTên khoa học: $scientificName.\n\nKhám phá ngay tại: $appLink"

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Nhận diện côn trùng qua BugScanner")
            putExtra(Intent.EXTRA_TEXT, shareText)

            // Kiểm tra nếu có dữ liệu hình ảnh thì tiến hành ghi ra file vật lý
            if (imageBytes != null && imageBytes.isNotEmpty()) {
                try {
                    // 1. Tạo thư mục tạm trong vùng nhớ Cache của ứng dụng
                    val cachePath = File(context.cacheDir, "shared_images")
                    cachePath.mkdirs()

                    // 2. Ghi mảng byte thành file JPEG
                    val file = File(cachePath, "bug_scanned_image.jpg")
                    FileOutputStream(file).use { stream ->
                        stream.write(imageBytes)
                    }

                    // 3. Tạo URI an toàn thông qua FileProvider để cấp quyền cho app khác (Zalo, FB...) đọc
                    val authority = "${context.packageName}.fileprovider"
                    val uri = FileProvider.getUriForFile(context, authority, file)

                    // 4. Chuyển kiểu Intent sang hình ảnh và đính kèm URI
                    type = "image/jpeg"
                    putExtra(Intent.EXTRA_STREAM, uri)

                    // Yêu cầu bắt buộc: Cấp quyền đọc URI tạm thời cho ứng dụng đích
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (e: Exception) {
                    // Nếu lỗi lưu file (hết dung lượng, v.v.), bỏ qua lỗi và vẫn share văn bản bình thường
                    e.printStackTrace()
                }
            }
        }

        // Tạo hộp thoại chọn ứng dụng (Chooser) thân thiện với người dùng
        val chooser = Intent.createChooser(shareIntent, "Chia sẻ kết quả qua")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // Yêu cầu khi gọi startActivity từ bên ngoài Activity
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