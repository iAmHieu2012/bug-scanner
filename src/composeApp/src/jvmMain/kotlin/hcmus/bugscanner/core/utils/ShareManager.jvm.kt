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
        appLink: String
    ) {
        val shareText = listOf(
            "Tôi vừa phát hiện ra loài: $bugName trên BugScanner.",
            "Tên khoa học: $scientificName.",
            "",
            "Khám phá ngay tại: $appLink"
        ).joinToString("\n")

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
