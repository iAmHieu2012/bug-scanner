package hcmus.bugscanner.ui.chat

import hcmus.bugscanner.domain.model.BugInfo
import hcmus.bugscanner.domain.repository.EncyclopediaRepository

/**
 * Bộ xử lý và phân giải ngữ cảnh bách khoa toàn thư cho đoạn chat.
 * Tự động tìm kiếm thông tin chi tiết nhất về sinh vật để mồi cho AI.
 */
object ChatContextResolver {
    /**
     * Truy vấn thông tin đầy đủ của sinh vật từ cơ sở dữ liệu dựa trên tên khoa học.
     * Tự động gộp ảnh từ kết quả scan và ảnh từ database.
     *
     * @param repository Kho dữ liệu bách khoa toàn thư.
     * @param bugContext Dữ liệu sinh vật thô truyền từ màn hình scan.
     * @return Dữ liệu sinh vật hoàn chỉnh nhất, hoặc trả về dữ liệu thô nếu không tìm thấy.
     */
    suspend fun resolve(
        repository: EncyclopediaRepository,
        bugContext: BugInfo?
    ): BugInfo? {
        if (bugContext == null) return null

        val scientificName = bugContext.scientificName.trim()
        if (scientificName.isBlank()) return bugContext

        return repository.getBugByScientificName(scientificName)?.let { databaseBug ->
            databaseBug.copy(
                imageUrl = bugContext.imageUrl.ifBlank { databaseBug.imageUrl },
                imageUrls = (bugContext.displayImageUrls() + databaseBug.displayImageUrls()).distinct()
            )
        } ?: bugContext
    }
}
