package hcmus.bugscanner.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hcmus.bugscanner.core.config.AppConfigProvider
import hcmus.bugscanner.domain.model.AppConfig
import hcmus.bugscanner.domain.model.BugInfo
import hcmus.bugscanner.domain.model.ScanHistory
import hcmus.bugscanner.domain.model.UserProfile
import hcmus.bugscanner.domain.repository.AdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.toLocalDateTime

/**
 * ViewModel quản lý logic của Bảng điều khiển Quản trị (Admin Dashboard).
 * Tích hợp đa chức năng: quản lý người dùng, nội dung bách khoa, cấu hình AI.
 *
 * @param adminRepository Repository thao tác trực tiếp với dữ liệu Admin trên Firestore.
 * @param appConfigProvider Provider cung cấp bộ nhớ đệm (cache) cho cấu hình ứng dụng.
 */
class AdminViewModel(
    private val adminRepository: AdminRepository,
    private val appConfigProvider: AppConfigProvider
) : ViewModel() {

    private val _currentSection = MutableStateFlow(0)
    val currentSection: StateFlow<Int> = _currentSection.asStateFlow()

    private val _users = MutableStateFlow<List<UserProfile>>(emptyList())
    val users: StateFlow<List<UserProfile>> = _users.asStateFlow()

    private val _allHistory = MutableStateFlow<List<ScanHistory>>(emptyList())
    val allHistory: StateFlow<List<ScanHistory>> = _allHistory.asStateFlow()

    private val _appConfig = MutableStateFlow(AppConfig())
    val appConfig: StateFlow<AppConfig> = _appConfig.asStateFlow()

    private val _topScannedBugs = MutableStateFlow<List<Pair<String, Int>>>(emptyList())
    val topScannedBugs: StateFlow<List<Pair<String, Int>>> = _topScannedBugs.asStateFlow()

    private val _scansPerDay = MutableStateFlow<List<Pair<String, Int>>>(emptyList())
    val scansPerDay: StateFlow<List<Pair<String, Int>>> = _scansPerDay.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        loadDashboardData()
    }

    /**
     * Thay đổi tab (section) đang được hiển thị trên giao diện.
     *
     * @param index Chỉ mục của tab cần chọn.
     */
    fun selectSection(index: Int) {
        _currentSection.value = index
    }

    /**
     * Tải song song toàn bộ dữ liệu cần thiết cho Admin Dashboard.
     */
    fun loadDashboardData() {
        _isLoading.value = true
        viewModelScope.launch {
            launch { loadConfig() }
            launch { loadUsers() }
            launch { loadHistory() }
        }.invokeOnCompletion {
            _isLoading.value = false
        }
    }

    /**
     * Tải danh sách người dùng.
     */
    fun loadUsers() {
        viewModelScope.launch {
            _users.value = adminRepository.getAllUsers()
        }
    }

    /**
     * Tải toàn bộ lịch sử quét của tất cả người dùng.
     */
    @Suppress("DEPRECATION")
    fun loadHistory() {
        viewModelScope.launch {
            val list = adminRepository.getAllHistory()
            _allHistory.value = list

            val top = list
                .groupBy { it.bugName.ifBlank { it.scientificName } }
                .map { it.key to it.value.size }
                .sortedByDescending { it.second }
                .take(5)
            _topScannedBugs.value = top

            val daily = list
                .sortedBy { it.timestamp }
                .groupBy {
                    val instant = kotlinx.datetime.Instant.fromEpochMilliseconds(it.timestamp.toLong())
                    val local = instant.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
                    "${local.date.dayOfMonth.toString().padStart(2, '0')}/${local.monthNumber.toString().padStart(2, '0')}"
                }
                .map { it.key to it.value.size }
                .takeLast(7)
            _scansPerDay.value = daily
        }
    }

    /**
     * Làm mới cache cấu hình và tải lại cấu hình từ Firestore.
     */
    fun loadConfig() {
        viewModelScope.launch {
            appConfigProvider.invalidate()
            _appConfig.value = adminRepository.getAppConfig()
        }
    }

    /**
     * Cập nhật cấu hình ứng dụng (AI models, prompts).
     *
     * @param config Dữ liệu cấu hình mới cần lưu.
     */
    fun updateConfig(config: AppConfig) {
        _isLoading.value = true
        viewModelScope.launch {
            val success = adminRepository.updateAppConfig(config)
            if (success) {
                appConfigProvider.invalidate()
                _appConfig.value = config
                _statusMessage.value = "Đã cập nhật cấu hình thành công!"
            } else {
                _statusMessage.value = "Lỗi khi cập nhật cấu hình."
            }
            _isLoading.value = false
        }
    }

    /**
     * Xóa một bản ghi lịch sử nhận diện khỏi hệ thống.
     *
     * @param id Mã định danh của bản ghi lịch sử cần xóa.
     */
    fun deleteHistoryEntry(id: String) {
        viewModelScope.launch {
            val success = adminRepository.deleteHistory(id)
            if (success) {
                _allHistory.value = _allHistory.value.filter { it.id != id }
                _statusMessage.value = "Đã xóa lịch sử quét."
            }
        }
    }

    /**
     * Khóa hoặc mở khóa tài khoản người dùng.
     *
     * @param user Đối tượng hồ sơ người dùng cần cập nhật.
     */
    fun toggleBanUser(user: UserProfile) {
        _isLoading.value = true
        viewModelScope.launch {
            val newBanStatus = !user.isBanned
            val success = adminRepository.banUser(user.uid, newBanStatus)
            if (success) {
                _statusMessage.value = if (newBanStatus) "Đã khóa tài khoản thành công." else "Đã mở khóa tài khoản thành công."
                loadUsers() // Tải lại danh sách
            } else {
                _statusMessage.value = "Lỗi khi cập nhật trạng thái tài khoản."
            }
            _isLoading.value = false
        }
    }

    /**
     * Xóa thông báo trạng thái hiện tại.
     */
    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}
