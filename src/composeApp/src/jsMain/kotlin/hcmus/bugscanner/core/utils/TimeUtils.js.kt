package hcmus.bugscanner.core.utils

import kotlin.js.Date

/**
 * Triển khai hàm lấy thời gian hiện hành (actual function) cho nền tảng Web (JS/Wasm).
 * Sử dụng đối tượng `Date` gốc của JavaScript.
 *
 * @return Mốc thời gian hiện tại tính bằng milliseconds.
 */
actual fun getCurrentTimeMillis(): Double = Date.now()

/**
 * Triển khai hàm định dạng chuỗi thời gian (actual function) cho nền tảng Web.
 * JavaScript không có SimpleDateFormat gốc như Java, do đó cần thực hiện padding thủ công.
 * Hàm `padStart(2, '0')` được sử dụng để đảm bảo các số nhỏ hơn 10 sẽ có số 0 ở đầu (VD: 9 -> 09).
 *
 * @param timestamp Thời gian dạng milliseconds cần được định dạng.
 * @return Chuỗi thời gian đồng nhất định dạng "dd/MM/yyyy HH:mm" với nền tảng Android.
 */
actual fun formatTimestamp(timestamp: Double): String {
    val date = Date(timestamp)

    val day = date.getDate().toString().padStart(2, '0')
    val month = (date.getMonth() + 1).toString().padStart(2, '0')
    val year = date.getFullYear()
    val hours = date.getHours().toString().padStart(2, '0')
    val minutes = date.getMinutes().toString().padStart(2, '0')

    return "$day/$month/$year $hours:$minutes"
}