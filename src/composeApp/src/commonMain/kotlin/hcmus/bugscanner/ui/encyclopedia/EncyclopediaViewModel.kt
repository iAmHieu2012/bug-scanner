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
import kotlinx.coroutines.launch
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

    init {
        fetchExploreList()
    }

    /**
     * Tải danh sách mặc định các loài côn trùng từ Firebase để hiển thị ở Tab Khám phá.
     */
    fun fetchExploreList() {
        viewModelScope.launch {
            _isLoading.value = true
            val list = repository.getExploreInsects(limit = 20)
            _exploreList.value = list
            _isLoading.value = false
        }
    }

    /**
     * Cập nhật từ khóa tìm kiếm nội bộ và gọi truy vấn Firebase sau một khoảng trễ (Debounce).
     *
     * @param query Từ khóa người dùng nhập vào.
     */
    fun onExploreSearchQueryChange(query: String) {
        _exploreSearchQuery.value = query
        exploreSearchJob?.cancel()
        exploreSearchJob = viewModelScope.launch {
            delay(500.milliseconds)
            _isLoading.value = true
            val list = repository.getExploreInsects(searchQuery = query.trim(), limit = 20)
            _exploreList.value = list
            _isLoading.value = false
        }
    }

    /**
     * Gửi truy vấn tìm kiếm sinh vật học đến API iNaturalist.
     * Tự động tra cứu tên khoa học thông qua dữ liệu nội bộ (Firebase cache)
     * hoặc dịch sang Tên Tiếng Anh thông qua Groq API (nếu tìm bằng tiếng Việt)
     * trước khi gửi yêu cầu lên iNaturalist để mở rộng phạm vi tìm kiếm.
     * Tự động format, dịch thuật cấp bậc phân loại và bóc tách dữ liệu JSON để trả về danh sách [BugInfo] chuẩn hóa.
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
                val cachedBugs = repository.getExploreInsects(searchQuery = trimmedQuery, limit = 1)
                val matchedScientificName = cachedBugs.firstOrNull()?.scientificName
                
                val queryToSearch = if (matchedScientificName != null) {
                    matchedScientificName
                } else {
                    val translated = groqApi.translateToEnglishName(trimmedQuery)
                    if (translated.isNotEmpty()) translated else trimmedQuery
                }

                val response = iNaturalistApi.searchInsects(query = queryToSearch)
                val results = response.results

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

                        BugInfo(
                            id = taxon.id.toString(),
                            name = commonName.replaceFirstChar { it.uppercase() },
                            englishName = taxon.englishCommonName ?: "",
                            scientificName = taxon.name,
                            description = shortDescription,
                            imageUrl = taxon.defaultPhoto?.mediumUrl
                                ?: taxon.defaultPhoto?.squareUrl
                                ?: "",
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