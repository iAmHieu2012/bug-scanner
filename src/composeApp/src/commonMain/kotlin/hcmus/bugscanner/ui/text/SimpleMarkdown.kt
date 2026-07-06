package hcmus.bugscanner.ui.text

enum class MarkdownEmphasis {
    NORMAL,
    BOLD,
    ITALIC,
    BOLD_ITALIC
}

data class MarkdownSpan(
    val text: String,
    val emphasis: MarkdownEmphasis = MarkdownEmphasis.NORMAL
)

sealed interface MarkdownBlock {
    val spans: List<MarkdownSpan>

    data class Paragraph(override val spans: List<MarkdownSpan>) : MarkdownBlock
    data class Bullet(override val spans: List<MarkdownSpan>) : MarkdownBlock
}

object SimpleMarkdown {
    fun parse(input: String): List<MarkdownBlock> {
        return input
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { line ->
                val bulletText = line.removePrefix("-").removePrefix("•").trim()
                if (line.startsWith("- ") || line.startsWith("• ")) {
                    MarkdownBlock.Bullet(parseInline(bulletText))
                } else {
                    MarkdownBlock.Paragraph(parseInline(line))
                }
            }
    }

    fun parseInline(input: String): List<MarkdownSpan> {
        val spans = mutableListOf<MarkdownSpan>()
        val buffer = StringBuilder()
        var bold = false
        var italic = false
        var index = 0

        fun flush() {
            if (buffer.isEmpty()) return
            spans += MarkdownSpan(
                text = buffer.toString(),
                emphasis = when {
                    bold && italic -> MarkdownEmphasis.BOLD_ITALIC
                    bold -> MarkdownEmphasis.BOLD
                    italic -> MarkdownEmphasis.ITALIC
                    else -> MarkdownEmphasis.NORMAL
                }
            )
            buffer.clear()
        }

        while (index < input.length) {
            when {
                input.startsWith("**", index) -> {
                    flush()
                    bold = !bold
                    index += 2
                }
                input[index] == '*' -> {
                    flush()
                    italic = !italic
                    index += 1
                }
                else -> {
                    buffer.append(input[index])
                    index += 1
                }
            }
        }

        flush()
        return spans.filter { it.text.isNotEmpty() }
    }
}
