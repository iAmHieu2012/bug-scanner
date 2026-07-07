package hcmus.bugscanner.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.Canvas
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import hcmus.bugscanner.domain.model.AppConfig
import hcmus.bugscanner.domain.model.ScanHistory
import hcmus.bugscanner.domain.model.ScanSource
import hcmus.bugscanner.domain.model.UserProfile
import hcmus.bugscanner.ui.components.ScreenHeader
import hcmus.bugscanner.core.utils.TimeUtils
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import hcmus.bugscanner.ui.components.BugImage
import org.koin.compose.viewmodel.koinViewModel

/**
 * Màn hình Bảng điều khiển dành riêng cho Quản trị viên (Admin).
 *
 * @param viewModel ViewModel chứa luồng dữ liệu cấu hình, người dùng và lịch sử cho dashboard.
 */
@Composable
fun AdminDashboardScreen(
    viewModel: AdminViewModel = koinViewModel()
) {
    val currentSection by viewModel.currentSection.collectAsState()
    val users by viewModel.users.collectAsState()
    val allHistory by viewModel.allHistory.collectAsState()
    val appConfig by viewModel.appConfig.collectAsState()
    val topScannedBugs by viewModel.topScannedBugs.collectAsState()
    val scansPerDay by viewModel.scansPerDay.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding()
        ) {
            ScreenHeader(
                title = "Bảng Quản trị",
                subtitle = "Quản lý hệ thống toàn diện",
                leadingIcon = Icons.Rounded.AdminPanelSettings,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 16.dp)
            ) {
                IconButton(
                    onClick = { viewModel.loadDashboardData() },
                    modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, androidx.compose.foundation.shape.CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Làm mới dữ liệu",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            PrimaryScrollableTabRow(
                selectedTabIndex = currentSection,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                val tabs = listOf(
                    "Tổng quan" to Icons.Rounded.Dashboard,
                    "Cấu hình AI" to Icons.Rounded.Settings,
                    "Người dùng" to Icons.Rounded.People,
                    "Lịch sử" to Icons.Rounded.History
                )
                tabs.forEachIndexed { index, (title, icon) ->
                    Tab(
                        selected = currentSection == index,
                        onClick = { viewModel.selectSection(index) },
                        text = { Text(title) },
                        icon = { Icon(imageVector = icon, contentDescription = null) }
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                when (currentSection) {
                    0 -> OverviewSection(users, allHistory, topScannedBugs, scansPerDay)
                    1 -> ConfigSection(appConfig) { viewModel.updateConfig(it) }
                    2 -> UsersSection(users = users, onToggleBan = { viewModel.toggleBanUser(it) })
                    3 -> GlobalHistorySection(allHistory, onDelete = { viewModel.deleteHistoryEntry(it) })
                }

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

/**
 * Thành phần giao diện hiển thị Tổng quan (Overview) của Admin Dashboard.
 *
 * @param users Danh sách người dùng hiện tại.
 * @param allHistory Danh sách toàn bộ lịch sử quét.
 * @param topBugs Danh sách top 5 côn trùng được quét nhiều nhất.
 * @param scansPerDay Danh sách số lượt quét theo từng ngày.
 */
@Composable
private fun OverviewSection(
    users: List<UserProfile>,
    allHistory: List<ScanHistory>,
    topBugs: List<Pair<String, Int>>,
    scansPerDay: List<Pair<String, Int>>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                StatCard(title = "Người dùng", count = users.size, icon = Icons.Rounded.People)
            }
            Box(modifier = Modifier.weight(1f)) {
                StatCard(title = "Lượt quét", count = allHistory.size, icon = Icons.Rounded.History)
            }
        }

        if (scansPerDay.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                ScansPerDayChart(scansPerDay)
            }
        }

        if (topBugs.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                TopBugsChart(topBugs)
            }
        }
    }
}

/**
 * Biểu đồ đường (Line Chart) hiển thị số lượt quét côn trùng trong 7 ngày gần nhất.
 *
 * @param scans Danh sách cặp dữ liệu (Ngày, Số lượt quét).
 */
@Composable
fun ScansPerDayChart(scans: List<Pair<String, Int>>) {
    val maxCount = scans.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurfaceVariant
    
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text("Lượt quét 7 ngày qua", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))
        
        Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
            val width = size.width
            val height = size.height
            
            val stepX = if (scans.size > 1) width / (scans.size - 1) else width
            
            val points = scans.mapIndexed { index, pair ->
                val x = index * stepX
                val y = height - (pair.second.toFloat() / maxCount * height)
                Offset(x, y)
            }
            
            val path = Path().apply {
                if (points.isNotEmpty()) moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    lineTo(points[i].x, points[i].y)
                }
            }
            drawPath(
                path = path,
                color = primaryColor,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            
            points.forEach { point ->
                drawCircle(color = primaryColor, radius = 6.dp.toPx(), center = point)
                drawCircle(color = Color.White, radius = 3.dp.toPx(), center = point)
            }
        }
        
        Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            scans.forEach {
                Text(it.first, style = MaterialTheme.typography.labelSmall, color = onSurfaceColor)
            }
        }
    }
}

/**
 * Biểu đồ thanh ngang (Horizontal Bar Chart) hiển thị Top 5 côn trùng được quét nhiều nhất.
 *
 * @param topBugs Danh sách cặp dữ liệu (Tên côn trùng, Số lượt quét).
 */
@Composable
fun TopBugsChart(topBugs: List<Pair<String, Int>>) {
    val maxCount = topBugs.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text("Top 5 Côn trùng được quét", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        topBugs.forEach { (name, count) ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Text(
                    text = name,
                    modifier = Modifier.weight(0.4f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Row(modifier = Modifier.weight(0.6f), verticalAlignment = Alignment.CenterVertically) {
                    val fraction = count.toFloat() / maxCount
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .height(12.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(count.toString(), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

/**
 * Thẻ thống kê (Stat Card) hiển thị một chỉ số cụ thể với biểu tượng.
 *
 * @param title Tiêu đề của chỉ số.
 * @param count Giá trị số lượng hiển thị.
 * @param icon Biểu tượng (Icon) đi kèm.
 */
@Composable
private fun StatCard(title: String, count: Int, icon: ImageVector) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

/**
 * Thành phần giao diện hiển thị Cấu hình AI (Config Section).
 *
 * @param appConfig Cấu hình ứng dụng hiện tại.
 * @param onSave Callback được gọi khi người dùng bấm Lưu cấu hình.
 */
@Composable
private fun ConfigSection(appConfig: AppConfig, onSave: (AppConfig) -> Unit) {
    var geminiModel by remember(appConfig) { mutableStateOf(appConfig.geminiModel) }
    var geminiPrompt by remember(appConfig) { mutableStateOf(appConfig.geminiSystemPrompt) }
    var geminiRag by remember(appConfig) { mutableStateOf(appConfig.geminiRagPrompt) }
    var groqModel by remember(appConfig) { mutableStateOf(appConfig.groqModel) }
    var groqPrompt by remember(appConfig) { mutableStateOf(appConfig.groqSystemPrompt) }
    var groqCrowdsourcing by remember(appConfig) { mutableStateOf(appConfig.groqCrowdsourcingPrompt) }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Gemini AI", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(value = geminiModel, onValueChange = { geminiModel = it }, label = { Text("Tên Model") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = geminiPrompt, onValueChange = { geminiPrompt = it }, label = { Text("System Prompt (Chatbot)") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                OutlinedTextField(value = geminiRag, onValueChange = { geminiRag = it }, label = { Text("RAG Prompt (Ngữ cảnh Bách khoa)") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
            }
        }

        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Groq AI", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(value = groqModel, onValueChange = { groqModel = it }, label = { Text("Tên Model") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = groqPrompt, onValueChange = { groqPrompt = it }, label = { Text("System Prompt (Dịch thuật)") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                OutlinedTextField(value = groqCrowdsourcing, onValueChange = { groqCrowdsourcing = it }, label = { Text("Crowdsourcing Prompt (Cào dữ liệu JSON)") }, modifier = Modifier.fillMaxWidth(), minLines = 5)
            }
        }



        Button(
            onClick = {
                onSave(
                    AppConfig(
                        geminiModel = geminiModel,
                        geminiSystemPrompt = geminiPrompt,
                        geminiRagPrompt = geminiRag,
                        groqModel = groqModel,
                        groqSystemPrompt = groqPrompt,
                        groqCrowdsourcingPrompt = groqCrowdsourcing
                    )
                )
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Lưu Cấu Hình", modifier = Modifier.padding(8.dp))
        }
    }
}

/**
 * Thành phần giao diện hiển thị danh sách Người dùng (Users Section).
 *
 * @param users Danh sách hồ sơ người dùng.
 * @param onToggleBan Callback được gọi khi Admin bấm khóa/mở khóa một người dùng.
 */
@Composable
private fun UsersSection(users: List<UserProfile>, onToggleBan: (UserProfile) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(users, key = { it.uid }) { user ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (user.isAnonymous) Icons.Rounded.PersonOutline else Icons.Rounded.Person,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (user.isAnonymous) "Tài khoản Ẩn danh" else user.email,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (user.isAnonymous) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "Khách",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                        Text(
                            text = "UID: ${user.uid}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "Hoạt động: ${TimeUtils.formatTimestamp(user.lastLoginAt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    Switch(
                        checked = user.isBanned,
                        onCheckedChange = { onToggleBan(user) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.error,
                            checkedTrackColor = MaterialTheme.colorScheme.errorContainer
                        )
                    )
                }
                if (user.isBanned) {
                    Text(
                        text = "Tài khoản đang bị khóa",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(start = 72.dp, bottom = 12.dp)
                    )
                }
            }
        }
    }
}

/**
 * Thành phần giao diện hiển thị danh sách toàn bộ Lịch sử quét của hệ thống.
 *
 * @param history Danh sách lịch sử quét.
 * @param onDelete Callback khi Admin muốn xóa một bản ghi lịch sử.
 */
@Composable
private fun GlobalHistorySection(history: List<ScanHistory>, onDelete: (String) -> Unit) {
    if (history.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Chưa có dữ liệu lịch sử quét", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(history, key = { it.id }) { item ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        hcmus.bugscanner.ui.components.BugImage(
                            imageUrl = item.imageUrl,
                            contentDescription = item.bugName,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.bugName.ifBlank { item.scientificName.ifBlank { "Không xác định" } },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = "UID: ${item.userId}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                maxLines = 1
                            )
                            Text(
                                text = "Thời gian: ${hcmus.bugscanner.core.utils.TimeUtils.formatTimestamp(item.timestamp)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Độ tin cậy: ${(item.confidence * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = hcmus.bugscanner.domain.model.ScanSource.fromValue(item.source).displayName,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                        IconButton(onClick = { onDelete(item.id) }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Xóa", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}
