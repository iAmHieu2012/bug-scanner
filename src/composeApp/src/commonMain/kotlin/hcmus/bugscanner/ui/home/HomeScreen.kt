package hcmus.bugscanner.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import hcmus.bugscanner.domain.model.BugInfo
import hcmus.bugscanner.ui.detail.BugDetailScreen
import hcmus.bugscanner.ui.history.HistoryScreen
import hcmus.bugscanner.ui.history.HistoryViewModel
import hcmus.bugscanner.core.state.RequireAuthScreen
import hcmus.bugscanner.ui.chat.ChatScreen
import hcmus.bugscanner.ui.wiki.EncyclopediaScreen

enum class AppTab { SCAN, HISTORY, WIKI, CHATBOT }

/**
 * Màn hình chính đóng vai trò là bộ định tuyến nội bộ (Internal Router) cho các tính năng cốt lõi.
 * * Tích hợp Adaptive Layout (WindowSizeClass):
 * - Thiết bị nhỏ (Mobile/Compact): Hiển thị NavigationBar (Bottom Bar) sát mép dưới.
 * - Thiết bị lớn (Web/Tablet/Expanded): Hiển thị NavigationRail (Side Bar) sát mép trái.
 *
 * @param windowSizeClass Dữ liệu đo đạc màn hình được truyền xuống từ AppNavigation.
 * @param isLoggedIn Trạng thái xác thực hiện tại của người dùng.
 * @param onAuthAction Callback yêu cầu đăng nhập/đăng xuất.
 * @param onShareClick Callback xử lý chia sẻ thông tin côn trùng.
 * @param scanTabContent Nội dung của tab Scan được tiêm (inject) từ bên ngoài để giảm phụ thuộc (Decoupling).
 * @param historyViewModel ViewModel quản lý lịch sử quét.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    windowSizeClass: WindowSizeClass, // Yêu cầu biến này để phân luồng Responsive
    isLoggedIn: Boolean,
    onAuthAction: () -> Unit,
    onShareClick: (BugInfo) -> Unit,
    scanTabContent: @Composable (isLoggedIn: Boolean, onAuthAction: () -> Unit, onDetectedBugClick: (String) -> Unit) -> Unit,
    historyViewModel: HistoryViewModel = viewModel { HistoryViewModel() }
) {
    // === VÙNG QUẢN LÝ TRẠNG THÁI (STATE MANAGEMENT) ===
    var currentTab by remember { mutableStateOf(AppTab.SCAN) }
    var selectedBug by remember { mutableStateOf<BugInfo?>(null) }
    var initialChatPrompt by remember { mutableStateOf<String?>(null) }
    val bugToShow = selectedBug

    // Định nghĩa cấu trúc dữ liệu tĩnh cho Menu điều hướng (Tab, Nhãn, Icon)
    val navItems: List<Triple<AppTab, String, ImageVector>> = listOf(
        Triple(AppTab.SCAN, "Nhận diện", Icons.Rounded.CenterFocusWeak),
        Triple(AppTab.HISTORY, "Lịch sử", Icons.Rounded.History),
        Triple(AppTab.WIKI, "Bách khoa", Icons.AutoMirrored.Rounded.MenuBook),
        Triple(AppTab.CHATBOT, "Trợ lý", Icons.Rounded.SmartToy)
    )

    // Xác định ngưỡng màn hình rộng (>= 840dp theo chuẩn Material 3)
    val isWideScreen = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded

    // === VÙNG ĐIỀU HƯỚNG GIAO DIỆN (UI ROUTING) ===
    if (bugToShow != null) {
        // Ưu tiên hiển thị màn hình Chi tiết (Detail Screen) phủ lên trên khi có một đối tượng được chọn
        BugDetailScreen(
            bug = bugToShow,
            onBackClick = { selectedBug = null },
            onAskChatbotClick = { prompt ->
                initialChatPrompt = prompt
                selectedBug = null // Đóng màn hình chi tiết
                currentTab = AppTab.CHATBOT // Tự động chuyển hướng sang tab Chatbot
            },
            onShareClick = onShareClick
        )
    } else {
        if (isWideScreen) {
            // ---------------------------------------------------------
            // LAYOUT DÀNH CHO MÀN HÌNH RỘNG (Web, Desktop, Tablet ngang)
            // ---------------------------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Thanh điều hướng dọc nằm bên trái
                NavigationRail(
                    modifier = Modifier
                        .padding(vertical = 16.dp, horizontal = 8.dp)
                        .clip(RoundedCornerShape(24.dp)),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Spacer(modifier = Modifier.weight(1f)) // Đẩy các item vào giữa chiều dọc
                    navItems.forEach { (tab, label, icon) ->
                        NavigationRailItem(
                            icon = { Icon(icon, contentDescription = null) },
                            label = { Text(label, fontSize = 12.sp) },
                            selected = currentTab == tab,
                            onClick = {
                                if (tab == AppTab.CHATBOT) initialChatPrompt = null
                                currentTab = tab
                            },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }

                // Không gian bên phải dành cho nội dung chính
                Box(
                    modifier = Modifier
                        .weight(1f) // Chiếm toàn bộ phần không gian còn lại
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    HomeContent(
                        currentTab = currentTab,
                        isLoggedIn = isLoggedIn,
                        onAuthAction = onAuthAction,
                        scanTabContent = scanTabContent,
                        historyViewModel = historyViewModel,
                        onBugSelected = { selectedBug = it },
                        initialChatPrompt = initialChatPrompt,
                        onClearChatPrompt = { initialChatPrompt = null }
                    )
                }
            }
        } else {
            // ---------------------------------------------------------
            // LAYOUT DÀNH CHO MÀN HÌNH HẸP (Mobile, Tablet dọc)
            // ---------------------------------------------------------
            Scaffold(
                bottomBar = {
                    Surface(
                        modifier = Modifier
                            .navigationBarsPadding() // Xử lý viền an toàn (Safe Area) với phím điều hướng ảo của hệ điều hành
                            .padding(16.dp)
                            .clip(RoundedCornerShape(24.dp)),
                        tonalElevation = 8.dp,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        // Thanh điều hướng ngang nằm dưới đáy
                        NavigationBar(
                            containerColor = Color.Transparent,
                            modifier = Modifier.height(70.dp)
                        ) {
                            navItems.forEach { (tab, label, icon) ->
                                NavigationBarItem(
                                    icon = { Icon(icon, contentDescription = null) },
                                    label = { Text(label, fontSize = 10.sp) },
                                    selected = currentTab == tab,
                                    onClick = {
                                        // Reset prompt nếu người dùng chủ động bấm vào tab Chatbot lần nữa
                                        if (tab == AppTab.CHATBOT) initialChatPrompt = null
                                        currentTab = tab
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                                    )
                                )
                            }
                        }
                    }
                }
            ) { paddingValues ->
                // Không gian bên trên dành cho nội dung chính
                Box(
                    modifier = Modifier
                        .padding(paddingValues) // Chừa khoảng trống tránh đè lên BottomBar
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    HomeContent(
                        currentTab = currentTab,
                        isLoggedIn = isLoggedIn,
                        onAuthAction = onAuthAction,
                        scanTabContent = scanTabContent,
                        historyViewModel = historyViewModel,
                        onBugSelected = { selectedBug = it },
                        initialChatPrompt = initialChatPrompt,
                        onClearChatPrompt = { initialChatPrompt = null }
                    )
                }
            }
        }
    }
}

/**
 * Component hiển thị nội dung của từng Tab.
 * Được tách ra (Extract Composable) nhằm mục đích tái sử dụng chung cho cả giao diện màn hình Rộng (Row) và Hẹp (Scaffold).
 */
@Composable
private fun HomeContent(
    currentTab: AppTab,
    isLoggedIn: Boolean,
    onAuthAction: () -> Unit,
    scanTabContent: @Composable (isLoggedIn: Boolean, onAuthAction: () -> Unit, onDetectedBugClick: (String) -> Unit) -> Unit,
    historyViewModel: HistoryViewModel,
    onBugSelected: (BugInfo) -> Unit,
    initialChatPrompt: String?,
    onClearChatPrompt: () -> Unit
) {
    when (currentTab) {
        AppTab.SCAN -> scanTabContent(isLoggedIn, onAuthAction) { detectedName ->
            // Khi AI nhận diện được côn trùng, tự động lưu lịch sử và kích hoạt mở màn hình Detail
            historyViewModel.addHistory(detectedName)
            onBugSelected(
                BugInfo(
                    id = detectedName,
                    name = "Đang tải...",
                    scientificName = detectedName
                )
            )
        }
        AppTab.HISTORY -> {
            if (isLoggedIn) {
                HistoryScreen()
            } else {
                // Chặn truy cập nếu là Guest (Khách)
                RequireAuthScreen(onAuthAction = onAuthAction)
            }
        }
        AppTab.WIKI -> EncyclopediaScreen(onBugSelected = onBugSelected)
        AppTab.CHATBOT -> {
            ChatScreen(initialPrompt = initialChatPrompt)
            // Xóa prompt sau khi ChatScreen đã tiếp nhận để tránh gửi lặp lại trong các chu kỳ Recomposition
            LaunchedEffect(initialChatPrompt) {
                if (initialChatPrompt != null) {
                    onClearChatPrompt()
                }
            }
        }
    }
}