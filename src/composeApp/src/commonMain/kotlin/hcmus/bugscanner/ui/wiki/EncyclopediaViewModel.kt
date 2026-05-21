package hcmus.bugscanner.ui.wiki

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hcmus.bugscanner.data.remote.WikiApiService
import hcmus.bugscanner.data.repository.EncyclopediaRepositoryImpl
import hcmus.bugscanner.domain.model.BugInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * ViewModel quản lý trạng thái và logic gọi API/Firebase cho Bách khoa toàn thư.
 */
class EncyclopediaViewModel : ViewModel() {
    private val repository = EncyclopediaRepositoryImpl()
    private val wikiApi = WikiApiService()

    private val _exploreList = MutableStateFlow<List<BugInfo>>(emptyList())
    val exploreList: StateFlow<List<BugInfo>> = _exploreList.asStateFlow()

    private val _exploreSearchQuery = MutableStateFlow("")
    val exploreSearchQuery: StateFlow<String> = _exploreSearchQuery.asStateFlow()

    private var exploreSearchJob: Job? = null

    private val _searchResults = MutableStateFlow<List<BugInfo>>(emptyList())
    val searchResults: StateFlow<List<BugInfo>> = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var searchJob: Job? = null

    init {
        fetchExploreList()
    }

    fun onExploreSearchQueryChange(query: String) {
        _exploreSearchQuery.value = query
        exploreSearchJob?.cancel()
        exploreSearchJob = viewModelScope.launch {
            delay(500.milliseconds)
            fetchExploreList()
        }
    }

    private fun fetchExploreList() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _exploreList.value = repository.getExploreInsects(
                    searchQuery = _exploreSearchQuery.value,
                    limit = 20
                )
            } catch (e: Exception) {
                // In lỗi ra Console (F12) để bạn biết chính xác Firebase đang bị gì
                println("Lỗi tải danh sách Khám phá: ${e.message}")
                e.printStackTrace()
                _exploreList.value = emptyList()
            } finally {
                // Đảm bảo luôn tắt vòng xoay loading dù thành công hay thất bại
                _isLoading.value = false
            }
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
            delay(500.milliseconds)
            _isLoading.value = true
            try {
                val response = wikiApi.searchInsects(query = trimmedQuery)
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
                println("EncyclopediaVM Search error: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
}