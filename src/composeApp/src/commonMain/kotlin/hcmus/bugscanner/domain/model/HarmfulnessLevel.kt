package hcmus.bugscanner.domain.model

/**
 * Enum đại diện cho mức độ gây hại của côn trùng.
 *
 * @property value Giá trị chuỗi định danh lưu trong cơ sở dữ liệu.
 * @property label Nhãn hiển thị chi tiết trên giao diện.
 * @property shortLabel Nhãn hiển thị rút gọn trên giao diện.
 */
enum class HarmfulnessLevel(
    val value: String,
    val label: String,
    val shortLabel: String
) {
    CROP_PEST("crop_pest", "Có thể gây hại cây trồng", "Gây hại"),
    LOW_RISK("low_risk", "Ít gây hại", "Ít hại"),
    BENEFICIAL("beneficial", "Có lợi / thiên địch", "Có lợi"),
    UNKNOWN("unknown", "Chưa rõ mức độ gây hại", "Chưa rõ");

    companion object {
        /**
         * Chuyển đổi từ chuỗi định danh sang [HarmfulnessLevel] tương ứng.
         *
         * @param value Giá trị chuỗi cần chuyển đổi.
         * @return [HarmfulnessLevel] tương ứng, trả về [UNKNOWN] nếu không tìm thấy.
         */
        fun fromValue(value: String): HarmfulnessLevel {
            val cleanValue = value.trim().lowercase()
            return entries.firstOrNull { it.value == cleanValue } ?: UNKNOWN
        }
    }
}

