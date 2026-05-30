package hcmus.bugscanner.data.repository

import dev.gitlive.firebase.firestore.FirebaseFirestore
import hcmus.bugscanner.domain.model.BugInfo
import hcmus.bugscanner.domain.repository.EncyclopediaRepository

/**
 * Lớp thực thi (Implementation) quản lý giao tiếp với cơ sở dữ liệu Bách khoa toàn thư.
 * Sử dụng Firebase Firestore kết hợp thư viện KMP GitLive để đồng bộ đa nền tảng.
 */
class EncyclopediaRepositoryImpl(
    db: FirebaseFirestore
) : EncyclopediaRepository {
    private val encyclopediaCollection = db.collection("encyclopedia")

    /**
     * Lấy danh sách các loài côn trùng từ Firestore.
     * Hỗ trợ tìm kiếm theo tiền tố (Prefix Search) thông qua thủ thuật ký tự `\uf8ff`.
     *
     * @param searchQuery Từ khóa tìm kiếm do người dùng nhập.
     * @param limit Giới hạn số lượng kết quả trả về để tối ưu hiệu suất.
     * @return Danh sách các [BugInfo]. Trả về mảng rỗng nếu lỗi mạng hoặc không có dữ liệu.
     */
    override suspend fun getExploreInsects(searchQuery: String, limit: Int): List<BugInfo> {
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

    /**
     * Truy vấn chính xác một bản ghi côn trùng dựa trên trường "name" (Tên phổ thông).
     *
     * @param name Tên phổ thông cần tìm kiếm.
     * @return Dữ liệu [BugInfo] nếu khớp, ngược lại trả về `null`.
     */
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

    /**
     * Truy vấn chính xác một bản ghi côn trùng dựa trên trường "scientificName" (Tên khoa học).
     *
     * @param scientificName Tên khoa học cần tìm kiếm.
     * @return Dữ liệu [BugInfo] nếu khớp, ngược lại trả về `null`.
     */
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