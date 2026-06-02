package hcmus.bugscanner.core.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log

/**
 * Chuyển đổi một đường dẫn ảnh dạng [Uri] thành một đối tượng hình ảnh [Bitmap]
 * để có thể đưa vào mô hình AI xử lý hoặc hiển thị lên giao diện.
 * Tự động tương thích ngược với các phân bản Android cũ.
 *
 * @param context Context hiện tại của ứng dụng dùng để truy cập ContentResolver.
 * @param uri Đường dẫn đến tệp hình ảnh (thường lấy từ Gallery hoặc Camera).
 * @return Đối tượng [Bitmap] nếu giải mã thành công, ngược lại trả về `null` nếu có lỗi.
 */
fun uriToBitmap(context: Context, uri: Uri): Bitmap? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.isMutableRequired = true
            }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    } catch (e: Exception) {
        Log.e("UriToBitmap", "Lỗi khi chuyển đổi Uri sang Bitmap: ${e.localizedMessage}", e)
        null
    }
}