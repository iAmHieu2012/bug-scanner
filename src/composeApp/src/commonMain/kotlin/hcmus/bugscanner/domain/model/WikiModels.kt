package hcmus.bugscanner.domain.model

/**
 * DTO mapping dữ liệu trả về từ Wikipedia API.
 */
data class WikiResponse(
    val query: WikiQuery? = null
)

data class WikiQuery(
    val pages: Map<String, WikiPage>? = null
)

data class WikiPage(
    val title: String,
    val extract: String? = null,
    val thumbnail: WikiThumbnail? = null,
    val index: Int = 0
)

data class WikiThumbnail(
    val source: String
)