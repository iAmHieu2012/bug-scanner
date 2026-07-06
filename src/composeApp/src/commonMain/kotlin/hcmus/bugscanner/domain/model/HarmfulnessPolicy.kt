package hcmus.bugscanner.domain.model

enum class HarmfulnessLevel(
    val value: String,
    val label: String,
    val shortLabel: String
) {
    CROP_PEST("crop_pest", "Có thể gây hại cây trồng", "Gây hại"),
    LOW_RISK("low_risk", "Ít gây hại", "Ít hại"),
    BENEFICIAL("beneficial", "Có lợi / thiên địch", "Có lợi"),
    UNKNOWN("unknown", "Chưa rõ mức độ gây hại", "Chưa rõ");
}

object HarmfulnessPolicy {
    fun fromValue(value: String): HarmfulnessLevel {
        val cleanValue = value.trim().lowercase()
        return HarmfulnessLevel.entries.firstOrNull { it.value == cleanValue } ?: HarmfulnessLevel.UNKNOWN
    }
}
