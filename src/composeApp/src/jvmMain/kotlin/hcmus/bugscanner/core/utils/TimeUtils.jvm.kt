package hcmus.bugscanner.core.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

actual fun getCurrentTimeMillis(): Double = System.currentTimeMillis().toDouble()

actual fun formatTimestamp(timestamp: Double): String {
    val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp.toLong()))
}
