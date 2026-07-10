package hcmus.bugscanner.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.*
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hcmus.bugscanner.domain.model.BugInfo
import hcmus.bugscanner.ui.theme.ThemeMode
import hcmus.bugscanner.domain.model.DetectedBugSnapshot
import hcmus.bugscanner.domain.model.ScanSource
import hcmus.bugscanner.domain.model.toBugInfo
import hcmus.bugscanner.ui.admin.AdminDashboardScreen
import hcmus.bugscanner.ui.chat.ChatScreen
import hcmus.bugscanner.ui.components.RequireAuthScreen
import hcmus.bugscanner.ui.detail.BugDetailScreen
import hcmus.bugscanner.ui.encyclopedia.EncyclopediaScreen
import hcmus.bugscanner.ui.history.HistoryScreen
import hcmus.bugscanner.ui.history.HistoryViewModel
import hcmus.bugscanner.ui.layout.AdaptiveLayoutSize
import org.koin.compose.viewmodel.koinViewModel

/**
 * Danh sách liệt kê các Tab chức năng chính trong ứng dụng.
 */
enum class AppTab { SCAN, HISTORY, WIKI, CHATBOT, PROFILE, ADMIN }

/**
 * Màn hình chính (Home Screen) của ứng dụng.
 * Đóng vai trò là Navigation Host cấp Tab và quản lý sự thay đổi bố cục thích ứng (Adaptive Layout).
 * Tích hợp thanh điều hướng bên (NavigationRail) trên màn hình lớn và thanh điều hướng dưới (NavigationBar) trên màn hình nhỏ.
 *
 * @param layoutSize Kích thước thích ứng của màn hình (AdaptiveLayoutSize) dùng để xác định loại thanh điều hướng.
 * @param initialTab Tab ban đầu khi mở màn hình, mặc định là tab quét (AppTab.SCAN).
 * @param onTabChanged Callback kích hoạt khi người dùng chuyển tab.
 * @param isLoggedIn Trạng thái đăng nhập của người dùng.
 * @param isAdmin Cho biết người dùng có quyền Admin hay không để hiển thị chức năng quản trị.
 * @param themeMode Chế độ giao diện (System, Light, Dark).
 * @param onThemeChange Callback kích hoạt khi người dùng chuyển đổi chế độ giao diện.
 * @param onAuthAction Callback xử lý các thao tác liên quan đến tài khoản (Đăng nhập/Đăng xuất).
 * @param onShareClick Callback xử lý chia sẻ thông tin côn trùng.
 * @param scanTabContent Nội dung Composable hiển thị riêng cho tab quét.
 * @param historyViewModel ViewModel quản lý lịch sử nhận diện côn trùng.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    layoutSize: AdaptiveLayoutSize,
    initialTab: AppTab = AppTab.SCAN,
    onTabChanged: (AppTab) -> Unit = {},
    isLoggedIn: Boolean,
    isAdmin: Boolean = false,
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    onAuthAction: () -> Unit,
    onShareClick: (BugInfo, ByteArray?, Float) -> Unit,
    scanTabContent: @Composable (onDetectedBugClick: (DetectedBugSnapshot) -> Unit) -> Unit,
    historyViewModel: HistoryViewModel = koinViewModel()
) {
    var currentTab by remember { mutableStateOf(initialTab) }
    var selectedSnapshot by remember { mutableStateOf<DetectedBugSnapshot?>(null) }
    var initialChatPrompt by remember { mutableStateOf<String?>(null) }
    var initialChatImage by remember { mutableStateOf<ByteArray?>(null) }
    var initialChatImageUrl by remember { mutableStateOf<String?>(null) }
    var initialChatBugContext by remember { mutableStateOf<BugInfo?>(null) }

    val navItems: List<Triple<AppTab, String, ImageVector>> = buildList {
        add(Triple(AppTab.SCAN, "Nhận diện", Icons.Rounded.CenterFocusWeak))
        add(Triple(AppTab.HISTORY, "Lịch sử", Icons.Rounded.History))
        add(Triple(AppTab.WIKI, "Bách khoa", Icons.AutoMirrored.Rounded.MenuBook))
        add(Triple(AppTab.CHATBOT, "Trợ lý", Icons.Rounded.SmartToy))
        add(Triple(AppTab.PROFILE, "Tài khoản", Icons.Rounded.Person))
    }

    LaunchedEffect(initialTab) {
        if (selectedSnapshot == null) currentTab = initialTab
    }

    fun selectTab(tab: AppTab) {
        if (tab == AppTab.CHATBOT) {
            initialChatPrompt = null
            initialChatImage = null
            initialChatImageUrl = null
            initialChatBugContext = null
        }
        selectedSnapshot = null
        currentTab = tab
        onTabChanged(tab)
    }

    val isWideScreen = layoutSize == AdaptiveLayoutSize.EXPANDED
    val snapshotToShow = selectedSnapshot

    if (snapshotToShow != null) {
        BugDetailScreen(
            bug = snapshotToShow.bug,
            confidence = snapshotToShow.confidence,
            source = snapshotToShow.source,
            onBackClick = { selectedSnapshot = null },
            onAskChatbotClick = { prompt, detailedBug ->
                initialChatPrompt = prompt.takeIf { it.isNotBlank() }
                if (snapshotToShow.source != hcmus.bugscanner.domain.model.ScanSource.UNKNOWN) {
                    initialChatImage = snapshotToShow.imageBytes
                    initialChatImageUrl = if (snapshotToShow.imageBytes == null) detailedBug.imageUrl else null
                } else {
                    initialChatImage = null
                    initialChatImageUrl = null
                }
                initialChatBugContext = detailedBug
                selectedSnapshot = null
                currentTab = AppTab.CHATBOT
                onTabChanged(AppTab.CHATBOT)
            },
            onShareClick = { bug ->
                onShareClick(bug, snapshotToShow.imageBytes, snapshotToShow.confidence)
            }
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
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    navItems.forEachIndexed { index, (tab, label, icon) ->
                        NavigationRailItem(
                            icon = { Icon(icon, contentDescription = null) },
                            label = { Text(label, fontSize = 12.sp) },
                            selected = currentTab == tab,
                            onClick = { selectTab(tab) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        if (index < navItems.lastIndex) {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    Crossfade(
                        targetState = currentTab,
                        animationSpec = tween(300)
                    ) { tab ->
                        HomeContent(
                            currentTab = tab,
                            isLoggedIn = isLoggedIn,
                            isAdmin = isAdmin,
                            themeMode = themeMode,
                            onThemeChange = onThemeChange,
                            onAuthAction = onAuthAction,
                            scanTabContent = scanTabContent,
                            historyViewModel = historyViewModel,
                            onSnapshotSelected = { selectedSnapshot = it },
                            initialChatPrompt = initialChatPrompt,
                            initialChatImage = initialChatImage,
                            initialChatImageUrl = initialChatImageUrl,
                            initialChatBugContext = initialChatBugContext,
                            onNavigateToAdmin = { selectTab(AppTab.ADMIN) },
                            onNavigateToChatbot = { query ->
                                initialChatPrompt = query
                                selectTab(AppTab.CHATBOT)
                            },
                            onClearChatPrompt = {
                                initialChatPrompt = null
                                initialChatImage = null
                                initialChatImageUrl = null
                                initialChatBugContext = null
                            }
                        )
                    }
                }
            }
        } else {
            Scaffold(
                bottomBar = {
                    Surface(
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(24.dp)),
                        shadowElevation = 8.dp,
                        tonalElevation = 5.dp,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    ) {
                        NavigationBar(
                            containerColor = Color.Transparent,
                            modifier = Modifier.height(64.dp)
                        ) {
                            navItems.forEach { (tab, label, icon) ->
                                NavigationBarItem(
                                    icon = { Icon(icon, contentDescription = null) },
                                    label = { Text(label, fontSize = 10.sp) },
                                    selected = currentTab == tab,
                                onClick = { selectTab(tab) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        indicatorColor = MaterialTheme.colorScheme.primary
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
                    Crossfade(
                        targetState = currentTab,
                        animationSpec = tween(300)
                    ) { tab ->
                        HomeContent(
                            currentTab = tab,
                            isLoggedIn = isLoggedIn,
                            isAdmin = isAdmin,
                            themeMode = themeMode,
                            onThemeChange = onThemeChange,
                            onAuthAction = onAuthAction,
                            scanTabContent = scanTabContent,
                            historyViewModel = historyViewModel,
                            onSnapshotSelected = { selectedSnapshot = it },
                            initialChatPrompt = initialChatPrompt,
                            initialChatImage = initialChatImage,
                            initialChatImageUrl = initialChatImageUrl,
                            initialChatBugContext = initialChatBugContext,
                            onNavigateToAdmin = { selectTab(AppTab.ADMIN) },
                            onNavigateToChatbot = { query ->
                                initialChatPrompt = query
                                selectTab(AppTab.CHATBOT)
                            },
                            onClearChatPrompt = {
                                initialChatPrompt = null
                                initialChatImage = null
                                initialChatImageUrl = null
                                initialChatBugContext = null
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Component điều phối nội dung chi tiết tương ứng với Tab đang được chọn.
 *
 * @param currentTab Tab hiện tại đang hiển thị.
 * @param isLoggedIn Trạng thái đăng nhập của người dùng.
 * @param isAdmin Cờ báo quyền Admin.
 * @param themeMode Chế độ giao diện hiện tại.
 * @param onThemeChange Callback xử lý chuyển đổi giao diện Sáng/Tối/Hệ thống.
 * @param onAuthAction Callback xử lý đăng nhập/xuất.ác thực.
 * @param scanTabContent Nội dung Composable hiển thị riêng cho tab quét.
 * @param historyViewModel ViewModel quản lý dữ liệu lịch sử.
 * @param onSnapshotSelected Callback khi một snapshot côn trùng được lựa chọn (ví dụ: sau khi quét hoặc từ lịch sử).
 * @param initialChatPrompt Nội dung prompt khởi tạo truyền sang màn hình Trợ lý.
 * @param initialChatImage Dữ liệu ảnh dạng mảng byte khởi tạo truyền sang màn hình Trợ lý.
 * @param initialChatImageUrl URL ảnh khởi tạo truyền sang màn hình Trợ lý.
 * @param initialChatBugContext Dữ liệu bách khoa khởi tạo để Gemini dùng làm ngữ cảnh.
 * @param onClearChatPrompt Callback để xoá sạch các thông tin khởi tạo của chatbot.
 * @param onNavigateToAdmin Callback chuyển hướng sang màn hình Quản trị hệ thống.
 * @param onNavigateToChatbot Callback chuyển hướng sang màn hình Trợ lý (Chatbot) kèm theo một câu hỏi mồi.
 */
@Composable
private fun HomeContent(
    currentTab: AppTab,
    isLoggedIn: Boolean,
    isAdmin: Boolean,
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    onAuthAction: () -> Unit,
    scanTabContent: @Composable (onDetectedBugClick: (DetectedBugSnapshot) -> Unit) -> Unit,
    historyViewModel: HistoryViewModel,
    onSnapshotSelected: (DetectedBugSnapshot) -> Unit,
    initialChatPrompt: String?,
    initialChatImage: ByteArray?,
    initialChatImageUrl: String?,
    initialChatBugContext: BugInfo?,
    onClearChatPrompt: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onNavigateToChatbot: (String) -> Unit
) {
    when (currentTab) {
        AppTab.SCAN -> scanTabContent { snapshot ->
            historyViewModel.addHistory(snapshot)
            onSnapshotSelected(snapshot)
        }
        AppTab.HISTORY -> {
            if (isLoggedIn) {
                HistoryScreen(
                    onItemClick = { historyItem ->
                        onSnapshotSelected(
                            DetectedBugSnapshot(
                                bug = historyItem.toBugInfo(),
                                imageBytes = null,
                                confidence = historyItem.confidence,
                                source = ScanSource.fromValue(historyItem.source)
                            )
                        )
                    }
                )
            } else {
                RequireAuthScreen(onAuthAction = onAuthAction)
            }
        }
        AppTab.WIKI -> EncyclopediaScreen(
            isAdmin = isAdmin,
            onBugSelected = { bug ->
                onSnapshotSelected(DetectedBugSnapshot(bug = bug, source = ScanSource.UNKNOWN))
            },
            onAskAI = onNavigateToChatbot
        )
        AppTab.CHATBOT -> {
            ChatScreen(
                initialPrompt = initialChatPrompt,
                initialImageBytes = initialChatImage,
                initialImageUrl = initialChatImageUrl,
                initialBugContext = initialChatBugContext
            )
            LaunchedEffect(initialChatPrompt, initialChatImage, initialChatImageUrl, initialChatBugContext) {
                if (initialChatPrompt != null || initialChatImage != null || initialChatImageUrl != null || initialChatBugContext != null) {
                    onClearChatPrompt()
                }
            }
        }
        AppTab.PROFILE -> {
            hcmus.bugscanner.ui.profile.ProfileScreen(
                themeMode = themeMode,
                onThemeChange = onThemeChange,
                onNavigateToAdmin = onNavigateToAdmin,
                onAuthAction = onAuthAction
            )
        }
        AppTab.ADMIN -> AdminDashboardScreen()
    }
}
