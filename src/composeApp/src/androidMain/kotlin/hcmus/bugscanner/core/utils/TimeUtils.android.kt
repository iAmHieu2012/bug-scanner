package hcmus.bugscanner.core.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Code thuần Android xử lý thời gian
actual fun getCurrentTimeMillis(): Long = System.currentTimeMillis()

actual fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}