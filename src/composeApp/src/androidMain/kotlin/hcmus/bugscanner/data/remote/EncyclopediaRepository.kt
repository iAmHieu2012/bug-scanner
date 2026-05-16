package hcmus.bugscanner.data.remote

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import hcmus.bugscanner.domain.model.BugInfo
import kotlinx.coroutines.tasks.await

/**
 * Repository xử lý việc kéo dữ liệu Bách khoa toàn thư từ Firebase Firestore.
 */
class EncyclopediaRepository {
    private val db = FirebaseFirestore.getInstance()
    private val encyclopediaCollection = db.collection("encyclopedia")

    suspend fun getExploreInsects(): List<BugInfo> {
        return try {
            // Lấy tối đa 20 con để hiển thị trên tab Khám phá
            val snapshot = encyclopediaCollection.limit(20).get().await()
            snapshot.toObjects(BugInfo::class.java)
        } catch (e: Exception) {
            Log.e("EncyclopediaRepo", "Lỗi tải danh sách Khám phá: ${e.message}")
            emptyList()
        }
    }
    suspend fun getBugByName(name: String): BugInfo? {
        return try {
            val snapshot = encyclopediaCollection.whereEqualTo("name", name).get().await()
            if (!snapshot.isEmpty) {
                snapshot.documents.first().toObject(BugInfo::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("EncyclopediaRepo", "Lỗi truy vấn con bọ theo tên: ${e.message}")
            null
        }
    }
}