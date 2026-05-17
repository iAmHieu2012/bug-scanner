package hcmus.bugscanner.domain.model

/**
 * Entity đại diện cho một bản ghi lịch sử quét côn trùng.
 */
data class ScanHistory(
    val id: String = "",
    val userId: String = "",
    val bugName: String = "",
    val timestamp: Long = 0L
)