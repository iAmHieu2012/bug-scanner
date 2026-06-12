package hcmus.bugscanner.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hcmus.bugscanner.domain.model.BugInfo
import hcmus.bugscanner.domain.model.DetectedBugSnapshot
import hcmus.bugscanner.domain.model.ScanSource
import hcmus.bugscanner.domain.model.toBugInfo
import hcmus.bugscanner.ui.chat.ChatScreen
import hcmus.bugscanner.ui.components.RequireAuthScreen
import hcmus.bugscanner.ui.detail.BugDetailScreen
import hcmus.bugscanner.ui.encyclopedia.EncyclopediaScreen
import hcmus.bugscanner.ui.history.HistoryScreen
import hcmus.bugscanner.ui.history.HistoryViewModel
import hcmus.bugscanner.ui.layout.AdaptiveLayoutSize
import org.koin.compose.viewmodel.koinViewModel

enum class AppTab { SCAN, HISTORY, WIKI, CHATBOT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    layoutSize: AdaptiveLayoutSize,
    initialTab: AppTab = AppTab.SCAN,
    onTabChanged: (AppTab) -> Unit = {},
    isLoggedIn: Boolean,
    onAuthAction: () -> Unit,
    onShareClick: (BugInfo, ByteArray?) -> Unit,
    scanTabContent: @Composable (isLoggedIn: Boolean, onAuthAction: () -> Unit, onDetectedBugClick: (DetectedBugSnapshot) -> Unit) -> Unit,
    historyViewModel: HistoryViewModel = koinViewModel()
) {
    var currentTab by remember { mutableStateOf(initialTab) }
    var selectedSnapshot by remember { mutableStateOf<DetectedBugSnapshot?>(null) }
    var initialChatPrompt by remember { mutableStateOf<String?>(null) }

    val navItems: List<Triple<AppTab, String, ImageVector>> = listOf(
        Triple(AppTab.SCAN, "Nhận diện", Icons.Rounded.CenterFocusWeak),
        Triple(AppTab.HISTORY, "Lịch sử", Icons.Rounded.History),
        Triple(AppTab.WIKI, "Bách khoa", Icons.AutoMirrored.Rounded.MenuBook),
        Triple(AppTab.CHATBOT, "Trợ lý", Icons.Rounded.SmartToy)
    )

    LaunchedEffect(initialTab) {
        if (selectedSnapshot == null) currentTab = initialTab
    }

    fun selectTab(tab: AppTab) {
        if (tab == AppTab.CHATBOT) initialChatPrompt = null
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
            onAskChatbotClick = { prompt ->
                initialChatPrompt = prompt
                selectedSnapshot = null
                currentTab = AppTab.CHATBOT
                onTabChanged(AppTab.CHATBOT)
            },
            onShareClick = { bug ->
                onShareClick(bug, snapshotToShow.imageBytes)
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
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    navItems.forEach { (tab, label, icon) ->
                        NavigationRailItem(
                            icon = { Icon(icon, contentDescription = null) },
                            label = { Text(label, fontSize = 12.sp) },
                            selected = currentTab == tab,
                            onClick = { selectTab(tab) },
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
                        onSnapshotSelected = { selectedSnapshot = it },
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
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(20.dp)),
                        tonalElevation = 5.dp,
                        color = MaterialTheme.colorScheme.surface
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
                        onSnapshotSelected = { selectedSnapshot = it },
                        initialChatPrompt = initialChatPrompt,
                        onClearChatPrompt = { initialChatPrompt = null }
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeContent(
    currentTab: AppTab,
    isLoggedIn: Boolean,
    onAuthAction: () -> Unit,
    scanTabContent: @Composable (isLoggedIn: Boolean, onAuthAction: () -> Unit, onDetectedBugClick: (DetectedBugSnapshot) -> Unit) -> Unit,
    historyViewModel: HistoryViewModel,
    onSnapshotSelected: (DetectedBugSnapshot) -> Unit,
    initialChatPrompt: String?,
    onClearChatPrompt: () -> Unit
) {
    when (currentTab) {
        AppTab.SCAN -> scanTabContent(isLoggedIn, onAuthAction) { snapshot ->
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
            onBugSelected = {
                onSnapshotSelected(DetectedBugSnapshot(bug = it, source = ScanSource.UNKNOWN))
            }
        )
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
