package hcmus.bugscanner.domain.model

/**
 * Chứa thông tin chi tiết của côn trùng để hiển thị trên UI.
 */
data class BugInfo(
    val id: String = "",
    val name: String = "",
    val scientificName: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val identification: String = "",
    val danger: String = "",
    val treatment: String = ""
)