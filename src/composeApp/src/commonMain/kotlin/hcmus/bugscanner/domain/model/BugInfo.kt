package hcmus.bugscanner.domain.model

/**
 * Lớp dữ liệu chứa thông tin chi tiết của một loài côn trùng để hiển thị trên giao diện người dùng (UI).
 * Domain Model này yêu cầu truyền đầy đủ dữ liệu (không có giá trị mặc định ngoại trừ wikiUrl)
 * để đảm bảo an toàn, tránh lỗi hiển thị khi thiếu dữ liệu.
 *
 * @property id Mã định danh duy nhất của bản ghi.
 * @property name Tên phổ thông (tên thường gọi bằng tiếng Việt) của côn trùng.
 * @property englishName Tên phổ thông bằng tiếng Anh.
 * @property scientificName Tên khoa học (Danh pháp hai phần).
 * @property description Đoạn văn bản mô tả tổng quan về loài côn trùng.
 * @property imageUrl Đường dẫn URL tĩnh chứa hình ảnh minh họa.
 * @property imageUrls Danh sách ảnh minh họa bổ sung cho thư viện bách khoa.
 * @property identification Đặc điểm nhận dạng ngoại hình.
 * @property danger Mức độ nguy hiểm hoặc tác hại đối với con người/nông nghiệp.
 * @property harmfulnessLevel Mức gây hại chuẩn hóa để UI hiển thị badge nhất quán.
 * @property treatment Biện pháp xử lý, phòng ngừa hoặc sơ cứu y tế khi tiếp xúc.
 * @property affectedCrops Cây trồng thường gặp hoặc dễ bị ảnh hưởng.
 * @property hostPlants Cây ký chủ được nguồn tham khảo ghi nhận.
 * @property damageSymptoms Dấu hiệu gây hại người dùng có thể quan sát.
 * @property identificationTips Đặc điểm nhận biết bằng ngôn ngữ đơn giản.
 * @property whereToFind Vị trí thường thấy trên cây hoặc ngoài ruộng/vườn.
 * @property season Thời điểm/điều kiện thường xuất hiện nếu có dữ liệu.
 * @property safeActions Việc nên làm an toàn, không thay thế tư vấn chuyên gia.
 * @property ipmNotes Ghi chú quản lý dịch hại tổng hợp từ nguồn đáng tin.
 * @property sourceRefs Danh sách nguồn tham khảo dùng để xây dựng hồ sơ.
 * @property searchTokens Từ khóa tìm kiếm bổ sung đã chuẩn hóa/curate.
 * @property wikiUrl Đường dẫn đến bài viết Wikipedia (nếu có, mặc định là chuỗi rỗng).
 */
data class BugInfo(
    val id: String,
    val name: String,
    val englishName: String,
    val scientificName: String,
    val description: String,
    val imageUrl: String,
    val imageUrls: List<String> = emptyList(),
    val identification: String,
    val danger: String,
    val harmfulnessLevel: String = HarmfulnessLevel.UNKNOWN.value,
    val treatment: String,
    val affectedCrops: List<String> = emptyList(),
    val hostPlants: List<String> = emptyList(),
    val damageSymptoms: List<String> = emptyList(),
    val identificationTips: List<String> = emptyList(),
    val whereToFind: List<String> = emptyList(),
    val season: String = "",
    val safeActions: List<String> = emptyList(),
    val ipmNotes: List<String> = emptyList(),
    val sourceRefs: List<String> = emptyList(),
    val searchTokens: List<String> = emptyList(),
    val wikiUrl: String = ""
) {
    fun displayImageUrls(excludedUrls: Set<String> = emptySet()): List<String> {
        return (listOf(imageUrl) + imageUrls)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .filterNot { it in excludedUrls }
            .filterNot { it.isKnownPlaceholderImageUrl() }
            .distinct()
    }

    /**
     * Hàm tiện ích giúp UI khởi tạo một đối tượng rỗng (Empty State)
     * trong lúc chờ dữ liệu tải về từ API/Firebase.
     */
    companion object {
        fun empty() = BugInfo(
            id = "",
            name = "",
            englishName = "",
            scientificName = "",
            description = "",
            imageUrl = "",
            imageUrls = emptyList(),
            identification = "",
            danger = "",
            harmfulnessLevel = HarmfulnessLevel.UNKNOWN.value,
            treatment = "",
            affectedCrops = emptyList(),
            hostPlants = emptyList(),
            damageSymptoms = emptyList(),
            identificationTips = emptyList(),
            whereToFind = emptyList(),
            season = "",
            safeActions = emptyList(),
            ipmNotes = emptyList(),
            sourceRefs = emptyList(),
            searchTokens = emptyList(),
            wikiUrl = ""
        )
    }
}

private fun String.isKnownPlaceholderImageUrl(): Boolean {
    val host = substringAfter("://", missingDelimiterValue = "")
        .substringBefore('/')
        .substringBefore(':')
        .lowercase()
    return host == "via.placeholder.com"
}
