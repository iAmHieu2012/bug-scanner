package hcmus.bugscanner.domain.model

import kotlinx.serialization.Serializable

/**
 * DTO mapping dữ liệu trả về từ Wikipedia API.
 */

@Serializable
data class WikiResponse(
    val query: WikiQuery? = null
)

@Serializable
data class WikiQuery(
    val pages: Map<String, WikiPage>? = null
)

@Serializable
data class WikiPage(
    val title: String,
    val extract: String? = null,
    val thumbnail: WikiThumbnail? = null,
    val index: Int = 0
)

@Serializable
data class WikiThumbnail(
    val source: String
)