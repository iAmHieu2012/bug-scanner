package hcmus.bugscanner.ui.detail

data class DetailSectionItem(
    val label: String,
    val body: String
) {
    val fullText: String
        get() = if (label.isBlank()) body else "$label: $body"
}

object DetailSectionTextPolicy {
    fun sectionItems(content: String): List<DetailSectionItem> {
        return content
            .lines()
            .flatMap { line -> line.split(";") }
            .map { it.trim().trimStart('-', '•').trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .map { item ->
                val label = item.substringBefore(": ", missingDelimiterValue = "")
                val body = item.substringAfter(": ", missingDelimiterValue = "")
                if (label.isNotBlank() && body.isNotBlank()) {
                    DetailSectionItem(label = label, body = body)
                } else {
                    DetailSectionItem(label = "", body = item)
                }
            }
    }
}
