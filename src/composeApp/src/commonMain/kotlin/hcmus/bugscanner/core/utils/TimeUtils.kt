package hcmus.bugscanner.core.utils

/**
 * Khai báo hàm yêu cầu cung cấp cách lấy thời gian.
 *
 * @return Thời gian hiện tại tính bằng milliseconds.
 */
expect fun getCurrentTimeMillis(): Double

/**
 * Khai báo hàm yêu cầu cung cấp cách format thời gian.
 *
 * @param timestamp Giá trị thời gian cần định dạng.
 * @return Chuỗi thời gian đã được định dạng.
 */
expect fun formatTimestamp(timestamp: Double): String