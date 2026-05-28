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

/**
 * Danh sách liệt kê các Tab chức năng chính trong ứng dụng.
 */
enum class AppTab { SCAN, HISTORY, WIKI, CHATBOT }

/**
 * Màn hình chính đóng vai trò là bộ định tuyến nội bộ (Internal Router) cho các tính năng cốt lõi.
 * Sử dụng `WindowSizeClass` để hỗ trợ hiển thị linh hoạt (Adaptive Layout):
 * - Màn hình lớn (Web/Tablet): Hiển thị NavigationRail bên trái.
 * - Màn hình nhỏ (Mobile): Hiển thị NavigationBar ở dưới cùng.
 *
 * @param windowSizeClass Kích thước màn hình hiện tại để thiết lập Layout Responsive.
 * @param isLoggedIn Trạng thái xác thực hiện tại của người dùng.
 * @param onAuthAction Callback yêu cầu đăng nhập/đăng xuất.
 * @param onShareClick Callback xử lý sự kiện chia sẻ thông tin côn trùng.
 * @param scanTabContent Component giao diện Tab Quét nhận diện được tiêm (inject) từ bên ngoài. Hàm trả về tên côn trùng và mảng byte ảnh (nếu có).
 * @param historyViewModel ViewModel quản lý thao tác với Database lịch sử.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    windowSizeClass: WindowSizeClass,
    isLoggedIn: Boolean,
    onAuthAction: () -> Unit,
    onShareClick: (BugInfo) -> Unit,
    scanTabContent: @Composable (isLoggedIn: Boolean, onAuthAction: () -> Unit, onDetectedBugClick: (String, ByteArray?) -> Unit) -> Unit,
    historyViewModel: HistoryViewModel = viewModel { HistoryViewModel() }
) {
    var currentTab by remember { mutableStateOf(AppTab.SCAN) }
    var selectedBug by remember { mutableStateOf<BugInfo?>(null) }
    var initialChatPrompt by remember { mutableStateOf<String?>(null) }
    val bugToShow = selectedBug

    val navItems: List<Triple<AppTab, String, ImageVector>> = listOf(
        Triple(AppTab.SCAN, "Nhận diện", Icons.Rounded.CenterFocusWeak),
        Triple(AppTab.HISTORY, "Lịch sử", Icons.Rounded.History),
        Triple(AppTab.WIKI, "Bách khoa", Icons.AutoMirrored.Rounded.MenuBook),
        Triple(AppTab.CHATBOT, "Trợ lý", Icons.Rounded.SmartToy)
    )

    val isWideScreen = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded

    if (bugToShow != null) {
        BugDetailScreen(
            bug = bugToShow,
            onBackClick = { selectedBug = null },
            onAskChatbotClick = { prompt ->
                initialChatPrompt = prompt
                selectedBug = null
                currentTab = AppTab.CHATBOT
            },
            onShareClick = onShareClick
        )
    } else {
        if (isWideScreen) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                NavigationRail(
                    modifier = Modifier
                        .padding(vertical = 16.dp, horizontal = 8.dp)
                        .clip(RoundedCornerShape(24.dp)),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Spacer(modifier = Modifier.weight(1f))
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

                Box(
                    modifier = Modifier
                        .weight(1f)
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
            Scaffold(
                bottomBar = {
                    Surface(
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(16.dp)
                            .clip(RoundedCornerShape(24.dp)),
                        tonalElevation = 8.dp,
                        color = MaterialTheme.colorScheme.surface
                    ) {
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
                Box(
                    modifier = Modifier
                        .padding(paddingValues)
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
 * Component hiển thị nội dung của từng Tab tương ứng, được tách hàm để tái sử dụng cho cấu trúc Adaptive Layout.
 *
 * @param currentTab Tab đang được chọn hiện tại.
 * @param isLoggedIn Trạng thái xác thực.
 * @param onAuthAction Callback xử lý xác thực.
 * @param scanTabContent Nội dung giao diện Tab Quét.
 * @param historyViewModel ViewModel quản lý dữ liệu lịch sử.
 * @param onBugSelected Callback truyền dữ liệu côn trùng khi một bản ghi được nhấn vào.
 * @param initialChatPrompt Nội dung prompt mặc định cần truyền vào Chatbot.
 * @param onClearChatPrompt Callback làm sạch nội dung prompt mặc định sau khi đã gửi đi.
 */
@Composable
private fun HomeContent(
    currentTab: AppTab,
    isLoggedIn: Boolean,
    onAuthAction: () -> Unit,
    scanTabContent: @Composable (isLoggedIn: Boolean, onAuthAction: () -> Unit, onDetectedBugClick: (String, ByteArray?) -> Unit) -> Unit,
    historyViewModel: HistoryViewModel,
    onBugSelected: (BugInfo) -> Unit,
    initialChatPrompt: String?,
    onClearChatPrompt: () -> Unit
) {
    when (currentTab) {
        AppTab.SCAN -> scanTabContent(isLoggedIn, onAuthAction) { detectedName, imageBytes ->
            historyViewModel.addHistory(detectedName, imageBytes)

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
                HistoryScreen(
                    onItemClick = { historyItem ->
                        onBugSelected(
                            BugInfo(
                                id = historyItem.bugName,
                                name = historyItem.bugName,
                                scientificName = historyItem.bugName,
                                imageUrl = historyItem.imageUrl
                            )
                        )
                    }
                )
            } else {
                RequireAuthScreen(onAuthAction = onAuthAction)
            }
        }
        AppTab.WIKI -> EncyclopediaScreen(onBugSelected = onBugSelected)
        AppTab.CHATBOT -> {
            ChatScreen(initialPrompt = initialChatPrompt)
            LaunchedEffect(initialChatPrompt) {
                if (initialChatPrompt != null) {
                    onClearChatPrompt()
                }
            }
        }
    }
}