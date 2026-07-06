package hcmus.bugscanner.ui.text

/**
 * Enum đại diện cho các kiểu nhấn mạnh văn bản trong Markdown.
 */
enum class MarkdownEmphasis {
    NORMAL,
    BOLD,
    ITALIC,
    BOLD_ITALIC
}

/**
 * Lớp đại diện cho một phân đoạn văn bản nhỏ (Span) cùng với kiểu nhấn mạnh của nó.
 *
 * @property text Chuỗi văn bản thô.
 * @property emphasis Kiểu in đậm hoặc in nghiêng.
 */
data class MarkdownSpan(
    val text: String,
    val emphasis: MarkdownEmphasis = MarkdownEmphasis.NORMAL
)

/**
 * Lớp đại diện cho một khối văn bản Markdown (Block), chứa nhiều Spans.
 */
sealed interface MarkdownBlock {
    val spans: List<MarkdownSpan>

    data class Paragraph(override val spans: List<MarkdownSpan>) : MarkdownBlock
    data class Bullet(override val spans: List<MarkdownSpan>) : MarkdownBlock
}

/**
 * Bộ phân giải Markdown tối giản, được thiết kế đặc biệt để loại bỏ các thư viện cồng kềnh.
 * Chỉ hỗ trợ phân giải danh sách dạng Bullet và kiểu chữ (Đậm/Nghiêng).
 */
object SimpleMarkdown {
    /**
     * Phân giải một văn bản hoàn chỉnh thành các khối đoạn văn và danh sách.
     *
     * @param input Chuỗi Markdown thô cần phân giải.
     * @return Danh sách các khối [MarkdownBlock].
     */
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
