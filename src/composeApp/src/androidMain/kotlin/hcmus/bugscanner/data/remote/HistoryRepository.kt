package hcmus.bugscanner.data.remote

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import hcmus.bugscanner.domain.model.ScanHistory
import kotlinx.coroutines.tasks.await

/**
 * Repository quản lý các thao tác truy xuất và lưu trữ lịch sử trên Firebase.
 */
class HistoryRepository {
    private val db = FirebaseFirestore.getInstance()
    private val historyCollection = db.collection("scan_history")

    /**
     * Lưu một bản ghi lịch sử mới lên Firestore.
     */
    suspend fun saveHistory(history: ScanHistory): Boolean {
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

    /**
     * Truy xuất danh sách lịch sử của người dùng, sắp xếp theo thời gian mới nhất.
     */
    suspend fun getUserHistory(userId: String): List<ScanHistory> {
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