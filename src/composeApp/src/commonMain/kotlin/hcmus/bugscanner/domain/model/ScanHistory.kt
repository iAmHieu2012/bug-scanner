package hcmus.bugscanner.domain.model

import kotlinx.serialization.Serializable

/**
 * Nguồn nhận diện tạo ra record scan/history.
 */
enum class ScanSource(val value: String, val displayName: String) {
    YOLO("yolo", "YOLO"),
    INATURALIST("inaturalist", "iNaturalist"),
    UNKNOWN("unknown", "Không rõ");

    companion object {
        fun fromValue(value: String): ScanSource = entries.firstOrNull { it.value == value } ?: UNKNOWN
    }
}

/**
 * Snapshot kết quả nhận diện dùng để truyền xuyên suốt scan -> detail -> chat -> history.
 */
data class DetectedBugSnapshot(
    val bug: BugInfo,
    val imageBytes: ByteArray? = null,
    val confidence: Float = 0f,
    val source: ScanSource = ScanSource.UNKNOWN
)

/**
 * Lớp thực thể (Entity) đại diện cho một bản ghi lịch sử nhận diện của người dùng.
 * Các field mới đều có default để record Firestore cũ vẫn deserialize được.
 */
@Serializable
data class ScanHistory(
    val id: String = "",
    val userId: String = "",
    val bugName: String = "",
    val timestamp: Double = 0.0,
    val imageUrl: String = "",
    val bugId: String = "",
    val scientificName: String = "",
    val englishName: String = "",
    val confidence: Float = 0f,
    val source: String = ScanSource.UNKNOWN.value,
    val description: String = "",
    val identification: String = "",
    val danger: String = "",
    val treatment: String = "",
    val wikiUrl: String = ""
)

fun ScanHistory.toBugInfo(): BugInfo {
    val fallbackName = bugName.ifBlank { scientificName.ifBlank { bugId } }
    val resolvedScientificName = scientificName.ifBlank { fallbackName }

    return BugInfo(
        id = bugId.ifBlank { fallbackName },
        name = bugName.ifBlank { resolvedScientificName },
        englishName = englishName,
        scientificName = resolvedScientificName,
        description = description,
        imageUrl = imageUrl,
        identification = identification,
        danger = danger,
        treatment = treatment,
        wikiUrl = wikiUrl
    )
}

fun DetectedBugSnapshot.toHistory(
    userId: String,
    timestamp: Double,
    uploadedImageUrl: String = ""
): ScanHistory = ScanHistory(
    userId = userId,
    bugId = bug.id,
    bugName = bug.name,
    englishName = bug.englishName,
    scientificName = bug.scientificName,
    timestamp = timestamp,
    imageUrl = uploadedImageUrl.ifBlank { bug.imageUrl },
    confidence = confidence,
    source = source.value,
    description = bug.description,
    identification = bug.identification,
    danger = bug.danger,
    treatment = bug.treatment,
    wikiUrl = bug.wikiUrl
)
