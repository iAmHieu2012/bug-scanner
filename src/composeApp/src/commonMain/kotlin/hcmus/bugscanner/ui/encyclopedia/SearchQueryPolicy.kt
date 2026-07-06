package hcmus.bugscanner.ui.encyclopedia

import hcmus.bugscanner.domain.model.BugInfo

object SearchQueryPolicy {
    private val asciiQuery = Regex("""^[A-Za-z][A-Za-z\s-]*$""")
    private val scientificName = Regex("""^[A-Z][a-z]+\s[a-z][a-z-]+$""")

    fun shouldTranslateWithGroq(query: String): Boolean {
        val cleanQuery = query.trim()
        return cleanQuery.isNotBlank() &&
            !scientificName.matches(cleanQuery) &&
            !asciiQuery.matches(cleanQuery)
    }

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
