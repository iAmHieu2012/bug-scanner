package hcmus.bugscanner.core.utils

import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime

/**
 * Object lấy định dạng thời gian cho từng platform.
 */
object TimeUtils {

    /**
     * Lấy thời gian hiện hành.
     *
     * @return Thời gian hiện tại tính bằng milliseconds.
     */
    fun getCurrentTimeMillis(): Double {
        return Clock.System.now().toEpochMilliseconds().toDouble()
    }

    /**
     * Định dạng chuỗi thời gian.
     *
     * @param timestamp Giá trị thời gian cần định dạng.
     * @return Chuỗi thời gian đã được định dạng.
     */
    fun formatTimestamp(timestamp: Double): String {
        val instant = Instant.fromEpochMilliseconds(timestamp.toLong())
        val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())

        val day = localDateTime.date.day.toString().padStart(2, '0')
        val month = localDateTime.date.month.number.toString().padStart(2, '0')
        val year = localDateTime.date.year
        val hours = localDateTime.time.hour.toString().padStart(2, '0')
        val minutes = localDateTime.time.minute.toString().padStart(2, '0')

        return "$day/$month/$year $hours:$minutes"
    }
}