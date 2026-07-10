package hcmus.bugscanner.data.repository

import dev.gitlive.firebase.firestore.FirebaseFirestore
import hcmus.bugscanner.data.model.BugInfoEntity
import hcmus.bugscanner.data.model.toDomain
import hcmus.bugscanner.domain.model.BugInfo
import hcmus.bugscanner.domain.repository.EncyclopediaRepository

/**
 * Lớp thực thi (Implementation) quản lý giao tiếp với cơ sở dữ liệu Bách khoa toàn thư.
 * Sử dụng Firebase Firestore kết hợp thư viện KMP GitLive để đồng bộ đa nền tảng.
 *
 * @param db Đối tượng Firestore dùng để kết nối và truy vấn dữ liệu.
 */
class EncyclopediaRepositoryImpl(
    db: FirebaseFirestore
) : EncyclopediaRepository {
    private val encyclopediaCollection = db.collection("encyclopedia")
    private var localFallbackCache: List<BugInfo>? = null

    @OptIn(org.jetbrains.compose.resources.ExperimentalResourceApi::class)
    private suspend fun getFallbackData(): List<BugInfo> {
        if (localFallbackCache != null) return localFallbackCache!!
        return try {
            val bytes = bugscanner.composeapp.generated.resources.Res.readBytes("files/backup_encyclopedia.json")
            val jsonString = bytes.decodeToString()
            val entities = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }.decodeFromString<List<hcmus.bugscanner.data.model.BugInfoEntity>>(jsonString)
            localFallbackCache = entities.map { it.toDomain() }
            localFallbackCache!!
        } catch (e: Exception) {
            println("Lỗi đọc file backup JSON offline: ${e.message}")
            emptyList()
        }
    }

    /**
     * Lấy danh sách các loài côn trùng từ Firestore.
     * Hỗ trợ tìm kiếm theo tiền tố (Prefix Search) thông qua thủ thuật ký tự `\uf8ff`.
     *
     * @param searchQuery Từ khóa tìm kiếm do người dùng nhập.
     * @param limit Giới hạn số lượng kết quả trả về để tối ưu hiệu suất.
     * @return Danh sách các [BugInfo]. Trả về mảng rỗng nếu lỗi mạng hoặc không có dữ liệu.
     */
    override suspend fun getExploreInsects(searchQuery: String, limit: Int, harmfulnessLevel: String?, yoloOnly: Boolean): List<BugInfo> {
        // Lọc cục bộ cho yoloOnly vì mảng YoloConstants.LABELS có 32 phần tử (quá giới hạn 30 của toán tử 'in' trên Firebase)
        fun applyYoloFilter(list: List<BugInfo>): List<BugInfo> {
            if (!yoloOnly) return list
            return list.filter { bug ->
                val lowerScientificName = bug.scientificName.lowercase()
                hcmus.bugscanner.ml.YoloConstants.LABELS.any { it.lowercase() == lowerScientificName }
            }
        }

        try {
            var baseQuery: dev.gitlive.firebase.firestore.Query = encyclopediaCollection
            if (harmfulnessLevel != null && harmfulnessLevel != "Tất cả") {
                baseQuery = baseQuery.where { "harmfulnessLevel" equalTo harmfulnessLevel }
            }

            if (searchQuery.isNotBlank()) {
                val variations = listOf(
                    searchQuery,
                    searchQuery.lowercase(),
                    searchQuery.replaceFirstChar { it.uppercase() }
                ).distinct()
                
                val results = mutableListOf<BugInfo>()
                for (variation in variations) {
                    val snapshot = baseQuery
                        .orderBy("name")
                        .startAtFieldValues { add(variation) }
                        .endAtFieldValues { add(variation + "\uf8ff") }
                        .limit(limit)
                        .get()
                        
                    results.addAll(snapshot.documents.map { it.data<BugInfoEntity>().toDomain() })
                }
                
                val distinctResults = results.distinctBy { it.id }.take(limit)
                val finalResults = applyYoloFilter(distinctResults)
                
                if (finalResults.isEmpty()) {
                    var localMatches = getFallbackData()
                    if (harmfulnessLevel != null && harmfulnessLevel != "Tất cả") {
                        localMatches = localMatches.filter { it.harmfulnessLevel == harmfulnessLevel }
                    }
                    localMatches = localMatches.filter { 
                        it.name.contains(searchQuery, ignoreCase = true) || 
                        it.scientificName.contains(searchQuery, ignoreCase = true) 
                    }
                    return applyYoloFilter(localMatches).take(limit)
                }
                return finalResults
            } else {
                val snapshot = baseQuery.orderBy("name").limit(limit).get()
                val finalResults = snapshot.documents.map { it.data<BugInfoEntity>().toDomain() }
                val filteredResults = applyYoloFilter(finalResults)
                
                if (filteredResults.isEmpty()) {
                    var fallback = getFallbackData()
                    if (harmfulnessLevel != null && harmfulnessLevel != "Tất cả") {
                        fallback = fallback.filter { it.harmfulnessLevel == harmfulnessLevel }
                    }
                    return applyYoloFilter(fallback).take(limit)
                }
                return filteredResults
            }
        } catch (e: Exception) {
            println("Lỗi tải danh sách Khám phá (Rớt mạng hoặc Firebase lỗi), dùng Fallback: ${e.message}")
            var fallback = getFallbackData()
            if (harmfulnessLevel != null && harmfulnessLevel != "Tất cả") {
                fallback = fallback.filter { it.harmfulnessLevel == harmfulnessLevel }
            }
            if (searchQuery.isNotBlank()) {
                fallback = fallback.filter { 
                    it.name.contains(searchQuery, ignoreCase = true) || 
                    it.scientificName.contains(searchQuery, ignoreCase = true) 
                }
            }
            return applyYoloFilter(fallback).take(limit)
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
                snapshot.documents.first().data<BugInfoEntity>().toDomain()
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
                snapshot.documents.first().data<BugInfoEntity>().toDomain()
            } else {
                null
            }
        } catch (e: Exception) {
            println("Lỗi truy vấn theo tên khoa học: ${e.message}")
            null
        }
    }

    /**
     * Tải toàn bộ dữ liệu từ collection "encyclopedia".
     * Lệnh get() này sẽ ép Firebase SDK trên Android/iOS tải data về và nhét vào Local Cache.
     */
    override suspend fun prefetchDatabase() {
        try {
            encyclopediaCollection.get()
            println("Đã tải xong bản sao Bách khoa toàn thư vào máy!")
        } catch (e: Exception) {
            println("Lỗi tải bản sao Database: ${e.message}")
        }
    }

    /**
     * Lưu thông tin một loài côn trùng mới lên Firestore.
     * Sử dụng Tên khoa học (scientificName) làm Document ID để đảm bảo tính duy nhất.
     * Dữ liệu được ánh xạ sang dạng Map để an toàn với mọi cấu hình Serialization.
     *
     * @param bug Đối tượng sinh vật cần lưu trữ.
     * @return `true` nếu ghi dữ liệu thành công, ngược lại `false`.
     */
    override suspend fun saveBugToFirebase(bug: BugInfo): Boolean {
        return try {
            val docId = bug.scientificName.ifBlank { bug.id }.lowercase().replace(" ", "_")

            val bugEntity = BugInfoEntity(
                id = bug.id,
                name = bug.name,
                englishName = bug.englishName,
                scientificName = bug.scientificName,
                description = bug.description,
                imageUrl = bug.imageUrl,
                imageUrls = bug.displayImageUrls(),
                identification = bug.identification,
                danger = bug.danger,
                harmfulnessLevel = bug.harmfulnessLevel,
                treatment = bug.treatment,
                affectedCrops = bug.affectedCrops,
                hostPlants = bug.hostPlants,
                damageSymptoms = bug.damageSymptoms,
                identificationTips = bug.identificationTips,
                whereToFind = bug.whereToFind,
                season = bug.season,
                safeActions = bug.safeActions,
                ipmNotes = bug.ipmNotes,
                searchTokens = bug.searchTokens,
                wikiUrl = bug.wikiUrl
            )

            encyclopediaCollection.document(docId).set(bugEntity)
            true
        } catch (e: Exception) {
            println("Lỗi khi lưu dữ liệu lên Firebase: ${e.message}")
            false
        }
    }
    /**
     * Xóa một mục trong Bách khoa toàn thư dựa trên Document ID.
     *
     * @param docId Document ID của mục cần xóa.
     * @return `true` nếu xóa thành công, ngược lại `false`.
     */
    override suspend fun deleteBugEntry(docId: String): Boolean {
        return try {
            encyclopediaCollection.document(docId).delete()
            true
        } catch (e: Exception) {
            println("Lỗi xóa mục Bách khoa toàn thư: ${e.message}")
            false
        }
    }
}
