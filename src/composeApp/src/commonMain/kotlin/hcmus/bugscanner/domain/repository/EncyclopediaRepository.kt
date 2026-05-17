package hcmus.bugscanner.domain.repository

import hcmus.bugscanner.domain.model.BugInfo

/**
 * Interface định nghĩa các thao tác với Bách khoa toàn thư
 */
interface EncyclopediaRepository {
    suspend fun getExploreInsects(searchQuery: String = "", limit: Long = 20): List<BugInfo>
    suspend fun getBugByName(name: String): BugInfo?
    suspend fun getBugByScientificName(scientificName: String): BugInfo?
}