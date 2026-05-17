package hcmus.bugscanner.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import hcmus.bugscanner.domain.model.ScanHistory
import hcmus.bugscanner.domain.repository.HistoryRepository
import kotlinx.coroutines.tasks.await

/**
 * Implementation quản lý các thao tác truy xuất và lưu trữ lịch sử trên Firebase.
 */
class HistoryRepositoryImpl : HistoryRepository {
    private val db = FirebaseFirestore.getInstance()
    private val historyCollection = db.collection("scan_history")

    override suspend fun saveHistory(history: ScanHistory): Boolean {
        return try {
            val docRef = historyCollection.document()
            val historyWithId = history.copy(id = docRef.id)

            docRef.set(historyWithId).await()
            true
        } catch (e: Exception) {
            Log.e("HistoryRepository", "Lỗi saveHistory: ${e.message}")
            false
        }
    }

    override suspend fun getUserHistory(userId: String): List<ScanHistory> {
        return try {
            val snapshot = historyCollection
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            snapshot.toObjects(ScanHistory::class.java)
        } catch (e: Exception) {
            Log.e("HistoryRepository", "Lỗi getUserHistory: ${e.message}")
            emptyList()
        }
    }
}