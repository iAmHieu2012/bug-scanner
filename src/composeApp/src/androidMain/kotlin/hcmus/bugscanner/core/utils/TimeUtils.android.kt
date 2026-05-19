package hcmus.bugscanner.core.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Khai báo hàm yêu cầu Android cung cấp cách lấy thời gian
 */
actual fun getCurrentTimeMillis(): Long = System.currentTimeMillis()

/**
 * Khai báo hàm yêu cầu Android cung cấp cách format thời gian
 */
actual fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}