package hcmus.bugscanner.domain.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Entity đại diện cho một bản ghi lịch sử quét côn trùng trên Firestore.
 */
data class ScanHistory(
    val id: String = "",
    val userId: String = "",
    val bugName: String = "",
    @ServerTimestamp
    val timestamp: Date? = null
)