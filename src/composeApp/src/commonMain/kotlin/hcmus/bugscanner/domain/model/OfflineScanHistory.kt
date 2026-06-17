package hcmus.bugscanner.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class OfflineScanHistory(
    val id: String,
    val userId: String,
    val history: ScanHistory,
    val imageBase64: String
)
