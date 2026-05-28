package hcmus.bugscanner.core.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Triển khai hàm lấy thời gian hiện hành (actual function) cho nền tảng Android.
 *
 * @return Mốc thời gian hiện tại tính bằng milliseconds (chuyển sang Double để tương thích KMP).
 */
actual fun getCurrentTimeMillis(): Double = System.currentTimeMillis().toDouble()

/**
 * Triển khai hàm định dạng chuỗi thời gian (actual function) cho nền tảng Android.
 * Sử dụng [SimpleDateFormat] tiêu chuẩn của Java.
 *
 * @param timestamp Thời gian dạng milliseconds cần được định dạng.
 * @return Chuỗi thời gian trực quan theo định dạng "dd/MM/yyyy HH:mm".
 */
actual fun formatTimestamp(timestamp: Double): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp.toLong()))
}