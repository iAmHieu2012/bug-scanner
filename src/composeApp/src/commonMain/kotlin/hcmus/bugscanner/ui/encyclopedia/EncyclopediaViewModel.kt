package hcmus.bugscanner.ui.encyclopedia

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hcmus.bugscanner.data.remote.GroqApiService
import hcmus.bugscanner.data.remote.INaturalistApiService
import hcmus.bugscanner.domain.model.BugInfo
import hcmus.bugscanner.domain.repository.EncyclopediaRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import hcmus.bugscanner.ml.YoloConstants
import kotlin.time.Duration.Companion.milliseconds

/**
 * ViewModel quản lý trạng thái và logic gọi API iNaturalist / Firebase cho màn hình Bách khoa toàn thư.
 *
 * @param repository Đối tượng quản lý giao tiếp với cơ sở dữ liệu Firebase (Bách khoa toàn thư).
 * @param iNaturalistApi Dịch vụ gọi API mạng để tra cứu iNaturalist.
 * @param groqApi Dịch vụ gọi API Groq để dịch thuật ngôn ngữ tự nhiên.
 */
class EncyclopediaViewModel(
    private val repository: EncyclopediaRepository,
    private val iNaturalistApi: INaturalistApiService,
    private val groqApi: GroqApiService
) : ViewModel() {

    private val _exploreList = MutableStateFlow<List<BugInfo>>(emptyList())

    val selectedTabIndex = MutableStateFlow(0)

    val selectedHarmfulnessFilter = MutableStateFlow<String?>("Tất cả")
    val showOnlyYoloDetectable = MutableStateFlow(false)

    val filteredExploreList: StateFlow<List<BugInfo>> = combine(
        _exploreList,
        selectedHarmfulnessFilter,
        showOnlyYoloDetectable
    ) { list, harmfulness, yoloOnly ->
        var result = list
        if (harmfulness != null && harmfulness != "Tất cả") {
            result = result.filter { it.harmfulnessLevel == harmfulness }
        }
        if (yoloOnly) {
            result = result.filter { bug ->
                val lowerScientificName = bug.scientificName.lowercase()
                YoloConstants.LABELS.any { it.lowercase() == lowerScientificName }
            }
        }
        result
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    val exploreList: StateFlow<List<BugInfo>> = _exploreList.asStateFlow()

    private val _exploreSearchQuery = MutableStateFlow("")
    val exploreSearchQuery: StateFlow<String> = _exploreSearchQuery.asStateFlow()

    private var exploreSearchJob: Job? = null

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<BugInfo>>(emptyList())
    val searchResults: StateFlow<List<BugInfo>> = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private var searchJob: Job? = null

    private var currentLimit = 30
    private var hasMoreExplore = true

    init {
        viewModelScope.launch {
            combine(selectedHarmfulnessFilter, showOnlyYoloDetectable) { _, _ -> }.collect {
                fetchExploreList()
            }
        }
    }

    /**
     * Tải danh sách mặc định các loài côn trùng từ Firebase để hiển thị ở Tab Khám phá.
     */
    fun fetchExploreList() {
        currentLimit = 30
        hasMoreExplore = true
        viewModelScope.launch {
            _isLoading.value = true
            val list = repository.getExploreInsects(
                searchQuery = _exploreSearchQuery.value.trim(),
                limit = currentLimit,
                harmfulnessLevel = selectedHarmfulnessFilter.value,
                yoloOnly = showOnlyYoloDetectable.value
            )
            _exploreList.value = list
            _isLoading.value = false
        }
    }

    /**
     * Tải thêm dữ liệu khi cuộn xuống cuối màn hình (Pagination).
     */
    fun loadMoreExploreInsects() {
        if (!hasMoreExplore || _isLoading.value) return
        currentLimit += 30
        viewModelScope.launch {
            // Không set _isLoading = true ở đây để tránh chớp màn hình, chỉ tải ngầm thêm dữ liệu
            val list = repository.getExploreInsects(
                searchQuery = _exploreSearchQuery.value.trim(), 
                limit = currentLimit,
                harmfulnessLevel = selectedHarmfulnessFilter.value,
                yoloOnly = showOnlyYoloDetectable.value
            )
            if (list.size <= _exploreList.value.size) {
                hasMoreExplore = false
            }
            _exploreList.value = list
        }
    }

    /**
     * Cập nhật từ khóa tìm kiếm nội bộ và gọi truy vấn Firebase sau một khoảng trễ (Debounce).
     *
     * @param query Từ khóa người dùng nhập vào.
     */
    fun onExploreSearchQueryChange(query: String) {
        _exploreSearchQuery.value = query
        currentLimit = 30
        hasMoreExplore = true
        exploreSearchJob?.cancel()
        exploreSearchJob = viewModelScope.launch {
            delay(500.milliseconds)
            _isLoading.value = true
            val list = repository.getExploreInsects(
                searchQuery = query.trim(), 
                limit = currentLimit,
                harmfulnessLevel = selectedHarmfulnessFilter.value,
                yoloOnly = showOnlyYoloDetectable.value
            )
            _exploreList.value = list
            _isLoading.value = false
        }
    }

    private val _isScientificSearch = MutableStateFlow(false)
    
    val isScientificSearch: StateFlow<Boolean> = _isScientificSearch.asStateFlow()

    /**
     * Bật hoặc tắt chế độ tra cứu bằng Tên khoa học.
     * Nếu có sẵn từ khóa hợp lệ đang được nhập, hệ thống sẽ tự động tra cứu lại ngay lập tức.
     *
     * @param enabled True nếu muốn bật, False nếu muốn tắt.
     */
    fun toggleScientificSearch(enabled: Boolean) {
        _isScientificSearch.value = enabled
        if (_searchQuery.value.trim().length >= 2) {
            searchInsects(_searchQuery.value)
        }
    }

    /**
     * Gửi truy vấn tìm kiếm sinh vật học đến API iNaturalist.
     * Hỗ trợ 2 chế độ:
     * - Tên khoa học: Tra cứu trực tiếp iNaturalist, không qua dịch thuật.
     * - Tên phổ thông (Tiếng Việt): Dịch qua AI trước khi tra cứu.
     *
     * @param query Từ khóa tìm kiếm do người dùng nhập.
     */
    fun searchInsects(query: String) {
        _searchQuery.value = query
        val trimmedQuery = query.trim()

        if (trimmedQuery.length < 2) {
            searchJob?.cancel()
            _searchResults.value = emptyList()
            return
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500.milliseconds)
            _isLoading.value = true
            try {
                var results = emptyList<hcmus.bugscanner.domain.model.INaturalistTaxon>()

                if (_isScientificSearch.value) {
                    results = iNaturalistApi.searchInsects(query = trimmedQuery).results
                } else {
                    val cachedBugs = repository.getExploreInsects(searchQuery = trimmedQuery, limit = 1)
                    val matchedScientificName = cachedBugs.firstOrNull()?.scientificName
                    
                    val queryToSearch = if (matchedScientificName != null) {
                        matchedScientificName
                    } else {
                        val translated = groqApi.translateToEnglishName(trimmedQuery)
                        if (translated.isNotEmpty()) translated else trimmedQuery
                    }

                    results = iNaturalistApi.searchInsects(query = queryToSearch).results
                }

                if (results.isNotEmpty()) {
                    val bugs = results.map { taxon ->
                        val rankVN = when(taxon.rank) {
                            "species" -> "Loài"
                            "subspecies" -> "Phân loài"
                            "genus" -> "Chi"
                            "family" -> "Họ"
                            "order" -> "Bộ"
                            "class" -> "Lớp"
                            "phylum" -> "Ngành"
                            else -> taxon.rank?.replaceFirstChar { it.uppercase() } ?: "Không rõ"
                        }

                        val commonName = taxon.preferredCommonName
                            ?: taxon.englishCommonName
                            ?: taxon.name

                        val shortDescription = "• Phân loại sinh học: $rankVN\n" +
                                "• Tên quốc tế: ${taxon.englishCommonName ?: "Chưa cập nhật"}\n"

                        val bioStats = "• Tên khoa học chuẩn: ${taxon.name}\n" +
                                "• Tên quốc tế (Tiếng Anh): ${taxon.englishCommonName ?: "Chưa cập nhật"}\n" +
                                "• Cấp bậc sinh học: $rankVN"

                        val photos = taxon.taxonPhotos?.mapNotNull { 
                            it.photo?.mediumUrl ?: it.photo?.squareUrl 
                        } ?: emptyList()

                        BugInfo(
                            id = taxon.name.lowercase().replace(" ", "_"),
                            name = commonName.replaceFirstChar { it.uppercase() },
                            englishName = taxon.englishCommonName ?: "",
                            scientificName = taxon.name,
                            description = shortDescription,
                            imageUrl = taxon.defaultPhoto?.mediumUrl
                                ?: taxon.defaultPhoto?.squareUrl
                                ?: "",
                            imageUrls = photos.take(5), // Lấy tối đa 5 ảnh
                            identification = bioStats,
                            danger = "",
                            treatment = "",
                            wikiUrl = taxon.wikipediaUrl ?: ""
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

    /**
     * Xóa thông báo trạng thái hiện tại.
     */
    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    /**
     * Lưu bài viết côn trùng lên Bách khoa toàn thư.
     * Dành riêng cho quyền Admin.
     *
     * @param bug Đối tượng côn trùng cần lưu.
     */
    fun saveBugEntry(bug: BugInfo) {
        _isLoading.value = true
        viewModelScope.launch {
            val success = repository.saveBugToFirebase(bug)
            if (success) {
                _statusMessage.value = "Đã lưu bài viết thành công!"
                // Tải lại danh sách sau khi lưu
                fetchExploreList()
            } else {
                _statusMessage.value = "Lỗi khi lưu bài viết."
            }
            _isLoading.value = false
        }
    }

    /**
     * Xóa một bài viết côn trùng khỏi Bách khoa toàn thư.
     * Dành riêng cho quyền Admin.
     *
     * @param bug Đối tượng côn trùng cần xóa.
     */
    fun deleteBugEntry(bug: BugInfo) {
        _isLoading.value = true
        viewModelScope.launch {
            val docId = bug.scientificName.ifBlank { bug.id }.replace(" ", "_")
            val success = repository.deleteBugEntry(docId)
            if (success) {
                _statusMessage.value = "Đã xóa bài viết khỏi bách khoa."
                // Cập nhật danh sách trên RAM thay vì gọi API tải lại
                _exploreList.value = _exploreList.value.filter { it.scientificName != bug.scientificName }
            } else {
                _statusMessage.value = "Lỗi khi xóa bài viết."
            }
            _isLoading.value = false
        }
    }
}