package hcmus.bugscanner.data.repository

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import hcmus.bugscanner.domain.model.BugInfo
import hcmus.bugscanner.domain.repository.EncyclopediaRepository

/**
 * Kéo dữ liệu Bách khoa toàn thư từ Firebase Firestore bằng thư viện KMP GitLive.
 */
class EncyclopediaRepositoryImpl : EncyclopediaRepository {
    private val db = Firebase.firestore
    private val encyclopediaCollection = db.collection("encyclopedia")

    override suspend fun getExploreInsects(searchQuery: String, limit: Long): List<BugInfo> {
        return try {
            val query = if (searchQuery.isNotBlank()) {
                val searchStr = searchQuery.trim()
                encyclopediaCollection
                    .orderBy("name")
                    .startAtFieldValues { add(searchStr) }
                    .endAtFieldValues { add(searchStr + "\uf8ff") }
                    .limit(limit)
            } else {
                encyclopediaCollection.orderBy("name").limit(limit)
            }

            val snapshot = query.get()
            // Firebase KMP gọi .data() để parse trực tiếp ra Object
            snapshot.documents.map { it.data<BugInfo>() }
        } catch (e: Exception) {
            println("Lỗi tải danh sách Khám phá: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getBugByName(name: String): BugInfo? {
        return try {
            val snapshot = encyclopediaCollection.where { "name" equalTo name }.get()
            if (snapshot.documents.isNotEmpty()) {
                snapshot.documents.first().data<BugInfo>()
            } else {
                null
            }
        } catch (e: Exception) {
            println("Lỗi truy vấn con bọ theo tên: ${e.message}")
            null
        }
    }

    override suspend fun getBugByScientificName(scientificName: String): BugInfo? {
        return try {
            val snapshot = encyclopediaCollection.where { "scientificName" equalTo scientificName }.get()
            if (snapshot.documents.isNotEmpty()) {
                snapshot.documents.first().data<BugInfo>()
            } else {
                null
            }
        } catch (e: Exception) {
            println("Lỗi truy vấn theo tên khoa học: ${e.message}")
            null
        }
    }
}