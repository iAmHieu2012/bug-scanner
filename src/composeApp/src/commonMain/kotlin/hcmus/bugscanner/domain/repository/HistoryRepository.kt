package hcmus.bugscanner.domain.repository

import hcmus.bugscanner.domain.model.ScanHistory

/**
 * Interface định nghĩa các thao tác với Lịch sử quét
 */
interface HistoryRepository {
    suspend fun saveHistory(history: ScanHistory): Boolean
    suspend fun getUserHistory(userId: String): List<ScanHistory>
}