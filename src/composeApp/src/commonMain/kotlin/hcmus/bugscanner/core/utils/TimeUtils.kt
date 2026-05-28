package hcmus.bugscanner.core.utils

/**
 * Khai báo hàm yêu cầu cung cấp cách lấy thời gian
 */
expect fun getCurrentTimeMillis(): Double

/**
 * Khai báo hàm yêu cầu cung cấp cách format thời gian
 */
expect fun formatTimestamp(timestamp: Double): String