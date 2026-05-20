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
 * Màn hình chính điều hướng các tab của ứng dụng.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    isLoggedIn: Boolean,
    onAuthAction: () -> Unit,
    onShareClick: (BugInfo) -> Unit,
    scanTabContent: @Composable (isLoggedIn: Boolean, onAuthAction: () -> Unit, onDetectedBugClick: (String) -> Unit) -> Unit,
    historyViewModel: HistoryViewModel = viewModel()
) {
    var currentTab by remember { mutableStateOf(AppTab.SCAN) }
    var selectedBug by remember { mutableStateOf<BugInfo?>(null) }
    var initialChatPrompt by remember { mutableStateOf<String?>(null) }
    val bugToShow = selectedBug

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
                        val items: List<Triple<AppTab, String, ImageVector>> = listOf(
                            Triple(AppTab.SCAN, "Nhận diện", Icons.Rounded.CenterFocusWeak),
                            Triple(AppTab.HISTORY, "Lịch sử", Icons.Rounded.History),
                            Triple(AppTab.WIKI, "Bách khoa", Icons.AutoMirrored.Rounded.MenuBook),
                            Triple(AppTab.CHATBOT, "Trợ lý", Icons.Rounded.SmartToy)
                        )
                        items.forEach { (tab, label, icon) ->
                            NavigationBarItem(
                                icon = { Icon(icon, contentDescription = null) },
                                label = { Text(label, fontSize = 10.sp) },
                                selected = currentTab == tab,
                                onClick = {
                                    if (tab == AppTab.CHATBOT) {
                                        initialChatPrompt = null
                                    }
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
                when (currentTab) {
                    AppTab.SCAN -> scanTabContent(isLoggedIn, onAuthAction) { detectedName ->
                        historyViewModel.addHistory(detectedName)
                        selectedBug = BugInfo(
                            id = detectedName,
                            name = "Đang tải...",
                            scientificName = detectedName
                        )
                    }
                    AppTab.HISTORY -> {
                        if (isLoggedIn) {
                            HistoryScreen()
                        } else {
                            RequireAuthScreen(onAuthAction = onAuthAction)
                        }
                    }
                    AppTab.WIKI -> EncyclopediaScreen(onBugSelected = { bug ->
                        selectedBug = bug
                    })
                    AppTab.CHATBOT -> {
                        ChatScreen(initialPrompt = initialChatPrompt)
                        LaunchedEffect(initialChatPrompt) {
                            if (initialChatPrompt != null) {
                                initialChatPrompt = null
                            }
                        }
                    }
                }
            }
        }
    }
}