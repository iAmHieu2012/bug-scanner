package hcmus.bugscanner.domain.model

import kotlinx.serialization.Serializable

/**
 * DTO mapping dữ liệu trả về từ iNaturalist API (v1).
 */
@Serializable
data class INaturalistResponse(
    val total_results: Int = 0,
    val results: List<INaturalistTaxon> = emptyList()
)

@Serializable
data class INaturalistTaxon(
    val id: Long,
    val name: String = "",
    val preferred_common_name: String? = null,
    val english_common_name: String? = null,
    val default_photo: INaturalistPhoto? = null,
    val rank: String? = null,
    val observations_count: Int = 0,
    val wikipedia_url: String? = null
)

@Serializable
data class INaturalistPhoto(
    val medium_url: String? = null,
    val square_url: String? = null
)