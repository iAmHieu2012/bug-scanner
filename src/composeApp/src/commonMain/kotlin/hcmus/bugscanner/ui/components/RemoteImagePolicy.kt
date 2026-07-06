package hcmus.bugscanner.ui.components

/**
 * Kiểm tra xem URL hình ảnh từ xa có thuộc danh sách máy chủ chặn CORS (Unsafe Canvas)
 * trên nền tảng Web khi gọi hàm render hay không.
 *
 * @param imageUrl Đường dẫn URL tĩnh của ảnh.
 * @return true nếu URL có khả năng gây lỗi CORS trên HTML5 Canvas, ngược lại là false.
 */
fun isKnownCanvasUnsafeImageUrl(imageUrl: String): Boolean {
    val host = imageUrl.substringAfter("://", missingDelimiterValue = "")
        .substringBefore('/')
        .substringBefore(':')
        .lowercase()
    return host == "via.placeholder.com"
}

/**
 * Hàm kiểm tra nền tảng chéo xem có thể tải trực tiếp hình ảnh từ URL này hay không.
 *
 * @param imageUrl Đường dẫn URL tĩnh của ảnh.
 * @return true nếu nền tảng hiện tại được phép tải ảnh từ URL này, ngược lại là false.
 */
expect fun canLoadRemoteImage(imageUrl: String): Boolean
