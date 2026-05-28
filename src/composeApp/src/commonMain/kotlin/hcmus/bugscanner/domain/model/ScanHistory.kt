package hcmus.bugscanner.domain.model

import kotlinx.serialization.Serializable

/**
 * Entity đại diện cho một bản ghi lịch sử quét côn trùng.
 */
@Serializable
data class ScanHistory(
    val id: String = "",
    val userId: String = "",
    val bugName: String = "",
    val timestamp: Double = 0.0
)