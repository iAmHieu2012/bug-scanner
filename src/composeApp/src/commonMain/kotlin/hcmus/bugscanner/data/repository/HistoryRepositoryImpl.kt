package hcmus.bugscanner.data.repository

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.firestore.where
import hcmus.bugscanner.domain.model.ScanHistory
import hcmus.bugscanner.domain.repository.HistoryRepository

/**
 * Implementation quản lý các thao tác truy xuất và lưu trữ lịch sử trên Firebase.
 */
class HistoryRepositoryImpl : HistoryRepository {
    private val db = Firebase.firestore
    private val historyCollection = db.collection("scan_history")

    override suspend fun saveHistory(history: ScanHistory): Boolean {
        return try {
            val docRef = historyCollection.add(history)
            val historyWithId = history.copy(id = docRef.id)
            docRef.set(historyWithId)
            true
        } catch (e: Exception) {
            println("Lỗi saveHistory: ${e.message}")
            false
        }
    }

    override suspend fun getUserHistory(userId: String): List<ScanHistory> {
        return try {
            val snapshot = historyCollection
                .where { "userId" equalTo userId }
                .orderBy("timestamp", Direction.DESCENDING)
                .get()

            // Dùng .data() của KMP, yêu cầu ScanHistory phải có @Serializable
            snapshot.documents.map { it.data<ScanHistory>() }
        } catch (e: Exception) {
            println("Lỗi getUserHistory: ${e.message}")
            emptyList()
        }
    }
}