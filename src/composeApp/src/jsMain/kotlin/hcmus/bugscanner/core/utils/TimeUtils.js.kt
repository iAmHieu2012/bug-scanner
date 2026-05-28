package hcmus.bugscanner.core.utils

import kotlin.js.Date

actual fun getCurrentTimeMillis(): Double = Date.now()

actual fun formatTimestamp(timestamp: Double): String {
    val date = Date(timestamp)
    // Tự format chuỗi thời gian cho giống định dạng dd/MM/yyyy HH:mm của Android
    val day = date.getDate().toString().padStart(2, '0')
    val month = (date.getMonth() + 1).toString().padStart(2, '0')
    val year = date.getFullYear()
    val hours = date.getHours().toString().padStart(2, '0')
    val minutes = date.getMinutes().toString().padStart(2, '0')

    return "$day/$month/$year $hours:$minutes"
}