package hcmus.bugscanner.ui.wiki

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hcmus.bugscanner.data.remote.EncyclopediaRepository
import hcmus.bugscanner.data.remote.RetrofitClient
import hcmus.bugscanner.domain.model.BugInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel quản lý trạng thái và logic gọi API/Firebase cho Bách khoa toàn thư.
 */
class EncyclopediaViewModel : ViewModel() {
    private val repository = EncyclopediaRepository()

    // Danh sách dành cho tab Khám phá (kéo từ Firebase)
    private val _exploreList = MutableStateFlow<List<BugInfo>>(emptyList())
    val exploreList: StateFlow<List<BugInfo>> = _exploreList.asStateFlow()

    // Danh sách dành cho tab Tra cứu (kéo từ Wikipedia API)
    private val _searchResults = MutableStateFlow<List<BugInfo>>(emptyList())
    val searchResults: StateFlow<List<BugInfo>> = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var searchJob: Job? = null

    init {
        fetchExploreList()
    }

    private fun fetchExploreList() {
        viewModelScope.launch {
            _isLoading.value = true
            _exploreList.value = repository.getExploreInsects()
            _isLoading.value = false
        }
    }

    fun searchInsects(query: String) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.length < 2) {
            _searchResults.value = emptyList()
            return
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500)
            _isLoading.value = true
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.searchInsects(query = trimmedQuery)
                }
                val pages = response.query?.pages
                if (!pages.isNullOrEmpty()) {
                    val bugs = pages.values
                        .sortedBy { it.index }
                        .map { page ->
                            BugInfo(
                                id = page.title,
                                name = page.title,
                                scientificName = "Côn trùng / Thực vật",
                                description = page.extract ?: "Không có mô tả chi tiết.",
                                imageUrl = page.thumbnail?.source ?: "https://via.placeholder.com/300?text=No+Image"
                            )
                        }
                    _searchResults.value = bugs
                } else {
                    _searchResults.value = emptyList()
                }
            } catch (e: Exception) {
                _searchResults.value = emptyList()
                android.util.Log.e("EncyclopediaVM", "Search error: ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}