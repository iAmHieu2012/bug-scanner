package hcmus.bugscanner.core.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

/**
 * Triển khai cụ thể (Implementation) của [ShareManager] dành riêng cho nền tảng JVM (Desktop).
 * Hệ thống sẽ tự động sao chép nội dung văn bản vào khay nhớ tạm (Clipboard).
 */
private class JvmShareManager : ShareManager {
    /**
     * Tự động sao chép thông tin côn trùng vào khay nhớ tạm (Clipboard) của Desktop.
     *
     * @param bugName Tên phổ thông của côn trùng.
     * @param scientificName Tên khoa học của côn trùng.
     * @param imageBytes Mảng byte của hình ảnh (bỏ qua trên Desktop).
     * @param confidenceLabel Độ tin cậy của kết quả.
     * @param harmfulnessLabel Mức độ gây hại của côn trùng.
     * @param appLink Đường dẫn tải app hoặc trang web.
     */
    override fun shareBugInfo(
        bugName: String,
        scientificName: String,
        imageBytes: ByteArray?,
        confidenceLabel: String,
        harmfulnessLabel: String,
        appLink: String
    ) {
        val shareText = buildString {
            appendLine("Tôi vừa phát hiện ra loài: $bugName trên BugScanner.")
            appendLine("Tên khoa học: $scientificName.")
            if (confidenceLabel.isNotBlank()) appendLine("Độ tin cậy: $confidenceLabel")
            if (harmfulnessLabel.isNotBlank()) appendLine("Mức gây hại: $harmfulnessLabel")
            appendLine()
            append("Khám phá ngay tại: $appLink")
        }

        runCatching {
            Toolkit.getDefaultToolkit()
                .systemClipboard
                .setContents(StringSelection(shareText), null)
        }
    }
}

/**
 * Hàm actual khởi tạo và ghi nhớ [JvmShareManager] trên nền tảng Desktop.
 *
 * @return Phiên bản [ShareManager] hoạt động trên Desktop.
 */
@Composable
actual fun rememberShareManager(): ShareManager {
    return remember { JvmShareManager() }
}
