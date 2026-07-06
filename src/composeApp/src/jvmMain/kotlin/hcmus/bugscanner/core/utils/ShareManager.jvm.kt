package hcmus.bugscanner.core.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

private class JvmShareManager : ShareManager {
    override fun shareBugInfo(
        bugName: String,
        scientificName: String,
        imageBytes: ByteArray?,
        confidenceLabel: String,
        harmfulnessLabel: String,
        appLink: String
    ) {
        val shareText = listOf(
            "Tôi vừa phát hiện ra loài: $bugName trên BugScanner.",
            "Tên khoa học: $scientificName.",
            confidenceLabel.takeIf { it.isNotBlank() }?.let { "Độ tin cậy: $it" }.orEmpty(),
            harmfulnessLabel.takeIf { it.isNotBlank() }?.let { "Mức gây hại: $it" }.orEmpty(),
            "",
            "Khám phá ngay tại: $appLink"
        ).filter { it.isNotBlank() }.joinToString("\n")

        runCatching {
            Toolkit.getDefaultToolkit()
                .systemClipboard
                .setContents(StringSelection(shareText), null)
        }
    }
}

@Composable
actual fun rememberShareManager(): ShareManager {
    return remember { JvmShareManager() }
}
