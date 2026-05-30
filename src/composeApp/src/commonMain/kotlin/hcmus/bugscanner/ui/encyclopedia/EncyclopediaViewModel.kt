package hcmus.bugscanner.ui.encyclopedia

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
 */
class EncyclopediaViewModel(
    private val repository: EncyclopediaRepository,
    private val iNaturalistApi: INaturalistApiService
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
     * Tự động format, dịch thuật và bóc tách dữ liệu JSON để trả về danh sách [BugInfo].
     *
     * @param query Từ khóa tìm kiếm trên API.
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
                val response = iNaturalistApi.searchInsects(query = trimmedQuery)
                val results = response.results

                if (results.isNotEmpty()) {
                    val bugs = results.map { taxon ->
                        // 1. Dịch cấp bậc
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

                        // 2. Tên phổ thông (Sẽ hiện làm tiêu đề chính của Card)
                        val commonName = taxon.preferredCommonName
                            ?: taxon.englishCommonName
                            ?: taxon.name

                        // 3. XÂY DỰNG LẠI MÔ TẢ NGẮN CHO THẺ CARD (Vừa khít 3 dòng của BugItemCard)
                        val shortDescription = "• Phân loại sinh học: $rankVN\n" +
                                "• Tên quốc tế: ${taxon.englishCommonName ?: "Chưa cập nhật"}\n"

                        // 4. Đẩy các thông số vào Đặc điểm nhận dạng (Sẽ hiện trong màn hình Chi tiết)
                        val bioStats = "• Tên khoa học chuẩn: ${taxon.name}\n" +
                                "• Tên quốc tế (Tiếng Anh): ${taxon.englishCommonName ?: "Chưa cập nhật"}\n" +
                                "• Cấp bậc sinh học: $rankVN"

                        BugInfo(
                            id = taxon.id.toString(),
                            name = commonName.replaceFirstChar { it.uppercase() },
                            scientificName = taxon.name,
                            description = shortDescription,
                            imageUrl = taxon.defaultPhoto?.mediumUrl
                                ?: taxon.defaultPhoto?.squareUrl
                                ?: "https://via.placeholder.com/300?text=No+Image",
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
}