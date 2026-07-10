package hcmus.bugscanner.ui.encyclopedia

import hcmus.bugscanner.domain.model.BugInfo

/**
 * Policy xử lý logic tìm kiếm và lọc danh sách sinh vật bách khoa.
 */
object SearchQueryPolicy {
    private val asciiQuery = Regex("""^[A-Za-z][A-Za-z\s-]*$""")
    private val scientificName = Regex("""^[A-Z][a-z]+\s[a-z][a-z-]+$""")

    /**
     * Kiểm tra xem chuỗi tìm kiếm có cần dịch sang tiếng Anh bằng AI Groq hay không.
     * Chỉ dịch nếu chuỗi là tiếng Việt (chứa dấu hoặc nằm ngoài định dạng tên khoa học).
     *
     * @param query Từ khóa tìm kiếm của người dùng.
     * @return `true` nếu cần gọi Groq AI để dịch, ngược lại `false`.
     */
    fun shouldTranslateWithGroq(query: String): Boolean {
        val cleanQuery = query.trim()
        return cleanQuery.isNotBlank() &&
            !scientificName.matches(cleanQuery) &&
            !asciiQuery.matches(cleanQuery)
    }

    /**
     * Lọc danh sách sinh vật dựa trên từ khóa tìm kiếm tiếng Việt không dấu.
     * Quét qua nhiều trường thông tin như tên, đặc điểm, cây trồng, v.v.
     *
     * @param bugs Danh sách bách khoa gốc.
     * @param query Từ khóa tìm kiếm thô.
     * @return Danh sách các sinh vật khớp với từ khóa.
     */
    fun filterBugs(bugs: List<BugInfo>, query: String): List<BugInfo> {
        val normalizedQuery = normalize(query)
        if (normalizedQuery.isBlank()) return bugs
        val includeLongText = normalizedQuery.length >= 4

        return bugs.filter { bug ->
            val nameFields = listOf(
                bug.name,
                bug.englishName,
                bug.scientificName
            )
            val detailFields = listOf(
                bug.description,
                bug.identification,
                bug.danger,
                bug.treatment,
                bug.season
            ) + bug.affectedCrops +
                bug.hostPlants +
                bug.damageSymptoms +
                bug.identificationTips +
                bug.whereToFind +
                bug.safeActions +
                bug.ipmNotes +
                bug.searchTokens

            nameFields.any { normalize(it).contains(normalizedQuery) } ||
                (includeLongText && detailFields.any { normalize(it).contains(normalizedQuery) })
        }
    }

    private fun normalize(value: String): String {
        return value.trim()
            .lowercase()
            .map { char -> vietnameseAsciiMap[char] ?: char }
            .joinToString("")
            .replace(Regex("""\s+"""), " ")
    }

    private val vietnameseAsciiMap = mapOf(
        'à' to 'a', 'á' to 'a', 'ạ' to 'a', 'ả' to 'a', 'ã' to 'a',
        'â' to 'a', 'ầ' to 'a', 'ấ' to 'a', 'ậ' to 'a', 'ẩ' to 'a', 'ẫ' to 'a',
        'ă' to 'a', 'ằ' to 'a', 'ắ' to 'a', 'ặ' to 'a', 'ẳ' to 'a', 'ẵ' to 'a',
        'è' to 'e', 'é' to 'e', 'ẹ' to 'e', 'ẻ' to 'e', 'ẽ' to 'e',
        'ê' to 'e', 'ề' to 'e', 'ế' to 'e', 'ệ' to 'e', 'ể' to 'e', 'ễ' to 'e',
        'ì' to 'i', 'í' to 'i', 'ị' to 'i', 'ỉ' to 'i', 'ĩ' to 'i',
        'ò' to 'o', 'ó' to 'o', 'ọ' to 'o', 'ỏ' to 'o', 'õ' to 'o',
        'ô' to 'o', 'ồ' to 'o', 'ố' to 'o', 'ộ' to 'o', 'ổ' to 'o', 'ỗ' to 'o',
        'ơ' to 'o', 'ờ' to 'o', 'ớ' to 'o', 'ợ' to 'o', 'ở' to 'o', 'ỡ' to 'o',
        'ù' to 'u', 'ú' to 'u', 'ụ' to 'u', 'ủ' to 'u', 'ũ' to 'u',
        'ư' to 'u', 'ừ' to 'u', 'ứ' to 'u', 'ự' to 'u', 'ử' to 'u', 'ữ' to 'u',
        'ỳ' to 'y', 'ý' to 'y', 'ỵ' to 'y', 'ỷ' to 'y', 'ỹ' to 'y',
        'đ' to 'd'
    )
}
