package hcmus.bugscanner.ui.chat

import hcmus.bugscanner.domain.model.BugInfo
import hcmus.bugscanner.domain.repository.EncyclopediaRepository

/**
 * Resolves the most authoritative chat context available for a detected bug.
 */
object ChatContextResolver {
    suspend fun resolve(
        repository: EncyclopediaRepository,
        bugContext: BugInfo?
    ): BugInfo? {
        if (bugContext == null) return null

        val scientificName = bugContext.scientificName.trim()
        if (scientificName.isBlank()) return bugContext

        return repository.getBugByScientificName(scientificName)?.let { databaseBug ->
            databaseBug.copy(
                imageUrl = bugContext.imageUrl.ifBlank { databaseBug.imageUrl },
                imageUrls = (bugContext.displayImageUrls() + databaseBug.displayImageUrls()).distinct()
            )
        } ?: bugContext
    }
}
