package hcmus.bugscanner.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import hcmus.bugscanner.domain.model.BugInfo
import hcmus.bugscanner.domain.repository.EncyclopediaRepository
import kotlinx.coroutines.tasks.await

/**
 * Implementation thực tế xử lý việc kéo dữ liệu Bách khoa toàn thư từ Firebase Firestore.
 */
class EncyclopediaRepositoryImpl : EncyclopediaRepository {
    private val db = FirebaseFirestore.getInstance()
    private val encyclopediaCollection = db.collection("encyclopedia")

    override suspend fun getExploreInsects(searchQuery: String, limit: Long): List<BugInfo> {
        return try {
            var query: Query = encyclopediaCollection

            if (searchQuery.isNotBlank()) {
                val searchStr = searchQuery.trim()
                query = query.orderBy("name")
                    .startAt(searchStr)
                    .endAt(searchStr + "\uf8ff")
            } else {
                query = query.orderBy("name")
            }

            val snapshot = query.limit(limit).get().await()
            snapshot.toObjects(BugInfo::class.java)
        } catch (e: Exception) {
            Log.e("EncyclopediaRepo", "Lỗi tải danh sách Khám phá: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getBugByName(name: String): BugInfo? {
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

    override suspend fun getBugByScientificName(scientificName: String): BugInfo? {
        return try {
            val snapshot = encyclopediaCollection.whereEqualTo("scientificName", scientificName).get().await()
            if (!snapshot.isEmpty) {
                snapshot.documents.first().toObject(BugInfo::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("EncyclopediaRepo", "Lỗi truy vấn theo tên khoa học: ${e.message}")
            null
        }
    }
}