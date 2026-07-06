package hcmus.bugscanner.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import hcmus.bugscanner.domain.model.BugInfo
import hcmus.bugscanner.domain.model.ScanSource
import hcmus.bugscanner.ui.chat.ChatPromptSuggestions
import hcmus.bugscanner.ui.components.BugImage
import org.koin.compose.viewmodel.koinViewModel

/**
 * Màn hình hiển thị thông tin chi tiết đầy đủ của một loài côn trùng.
 * Đã được tái cấu trúc (Refactored) tuân thủ Clean Architecture, chuyển logic mạng sang ViewModel.
 * Tự động chuyển đổi bố cục (Adaptive Layout) dựa trên kích thước khung hình hiện tại.
 *
 * @param bug Dữ liệu cơ bản của côn trùng được truyền từ màn hình trước.
 * @param confidence Độ tin cậy (%) của kết quả nhận diện (nếu có).
 * @param source Nguồn nhận diện (YOLO hoặc iNaturalist).
 * @param viewModel ViewModel quản lý trạng thái tải dữ liệu chi tiết (Được tiêm tự động qua Koin).
 * @param onBackClick Callback xử lý sự kiện người dùng nhấn nút quay lại.
 * @param onAskChatbotClick Callback chuyển hướng sang màn hình Chatbot, kèm theo câu lệnh (prompt) thiết lập sẵn.
 * @param onShareClick Callback gọi hệ thống chia sẻ (Share Intent) native của thiết bị.
 */
@Composable
fun BugDetailScreen(
    bug: BugInfo,
    confidence: Float = 0f,
    source: ScanSource = ScanSource.UNKNOWN,
    viewModel: BugDetailViewModel = koinViewModel(),
    onBackClick: () -> Unit,
    onAskChatbotClick: (String, BugInfo) -> Unit,
    onShareClick: (BugInfo) -> Unit
) {
    val scrollState = rememberScrollState()
    val detailedBug by viewModel.detailedBug.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(bug.scientificName) {
        viewModel.loadBugDetails(bug)
    }

    val currentBug = detailedBug ?: bug

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (maxWidth > 800.dp) {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(0.4f)
                        .fillMaxHeight()
                        .background(Color.Black)
                ) {
                    BugImage(
                        imageUrl = currentBug.imageUrl,
                        contentDescription = "Ảnh côn trùng",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .padding(top = 24.dp, start = 16.dp)
                            .size(48.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(0.6f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 8.dp)) {
                            BugDetailHeader(currentBug, source, confidence)
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(scrollState)
                                .padding(horizontal = 24.dp)
                        ) {
                            BugDetailSections(currentBug, isLoading)
                        }
                    }
                    BugDetailBottomBar(currentBug, confidence, source, onAskChatbotClick, onShareClick)
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                BugImage(
                    imageUrl = currentBug.imageUrl,
                    contentDescription = "Ảnh côn trùng",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(320.dp)
                )

                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .padding(top = 48.dp, start = 16.dp)
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 272.dp)
                        .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 8.dp)) {
                        BugDetailHeader(currentBug, source, confidence)
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(scrollState)
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 16.dp)
                    ) {
                        BugDetailSections(currentBug, isLoading)
                    }
                    
                    BugDetailBottomBar(currentBug, confidence, source, onAskChatbotClick, onShareClick)
                }
            }
        }
    }
}

/**
 * Khối Component hiển thị danh sách các trường thông tin chi tiết.
 * Trích xuất để tái sử dụng giữa các bố cục ngang/dọc, giữ cho hàm chính luôn gọn gàng.
 *
 * @param detailedBug Đối tượng chứa thông tin chi tiết của sinh vật để render.
 * @param isLoading Trạng thái tải dữ liệu từ API/Firebase (Hiển thị con xoay nếu true).
 * @param source Nguồn mở màn hình để xác định có hiển thị nhãn Đã nhận diện hay không.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BugDetailHeader(detailedBug: BugInfo, source: ScanSource, confidence: Float) {
    val confidenceInfo = hcmus.bugscanner.domain.model.ConfidencePolicy.explain(confidence)
    val harmfulness = hcmus.bugscanner.domain.model.HarmfulnessLevel.fromValue(detailedBug.harmfulnessLevel)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = detailedBug.name,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = detailedBug.scientificName.ifBlank { "Chưa rõ" },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        ResultMetadataBadges(
            confidenceLabel = if (confidence > 0f) "${confidenceInfo.label} (${confidenceInfo.percentText})" else null,
            sourceLabel = if (source != ScanSource.UNKNOWN) "Nguồn: ${source.userFacingName}" else null,
            harmfulness = harmfulness
        )
        if (confidence > 0f && confidence < 0.5f) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = confidenceInfo.guidance,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResultMetadataBadges(
    confidenceLabel: String?,
    sourceLabel: String?,
    harmfulness: hcmus.bugscanner.domain.model.HarmfulnessLevel
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        confidenceLabel?.let {
            AssistChip(
                onClick = {},
                label = { Text(it) },
                leadingIcon = { Icon(Icons.Rounded.Verified, null, modifier = Modifier.size(16.dp)) }
            )
        }
        AssistChip(
            onClick = {},
            label = { Text(harmfulness.label) },
            leadingIcon = { Icon(Icons.Rounded.Warning, null, modifier = Modifier.size(16.dp)) },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                labelColor = MaterialTheme.colorScheme.onErrorContainer
            )
        )
        sourceLabel?.let {
            AssistChip(
                onClick = {},
                label = { Text(it) },
                leadingIcon = { Icon(Icons.Rounded.Analytics, null, modifier = Modifier.size(16.dp)) }
            )
        }
    }
}

/**
 * Khối Component hiển thị danh sách các trường thông tin chi tiết.
 * @param detailedBug Đối tượng chứa thông tin chi tiết của sinh vật để render.
 * @param isLoading Trạng thái tải dữ liệu từ API/Firebase.
 */
@Composable
private fun BugDetailSections(detailedBug: BugInfo, isLoading: Boolean) {
    if (isLoading) {
        Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    } else {
        val images = detailedBug.displayImageUrls()
        if (images.size > 1) {
            Text(
                text = "Thư viện ảnh",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(images) { url ->
                    BugImage(
                        imageUrl = url,
                        contentDescription = "Ảnh ${detailedBug.name}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(100.dp).clip(RoundedCornerShape(8.dp))
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        val overview = detailedBug.description.cleanFarmerText()
        val identification = listOf(detailedBug.identification, detailedBug.identificationTips.joinDisplay()).joinDisplay().cleanFarmerText()
        val danger = listOf(detailedBug.danger, detailedBug.damageSymptoms.joinDisplay()).joinDisplay().cleanFarmerText()
        val treatment = listOf(detailedBug.treatment, detailedBug.safeActions.joinDisplay(), detailedBug.ipmNotes.joinDisplay()).joinDisplay().cleanFarmerText()

        if (overview.isNotBlank()) {
            SectionCard(title = "Tổng quan", icon = Icons.AutoMirrored.Rounded.MenuBook, iconTint = MaterialTheme.colorScheme.secondary, content = overview)
            Spacer(modifier = Modifier.height(16.dp))
        }
        if (detailedBug.affectedCrops.isNotEmpty() || detailedBug.hostPlants.isNotEmpty()) {
            val crops = listOf(
                detailedBug.affectedCrops.joinDisplay(prefix = "Cây trồng thường gặp: "),
                detailedBug.hostPlants.joinDisplay(prefix = "Cây ký chủ tham khảo: ")
            ).joinDisplay().cleanFarmerText()
            SectionCard(title = "Cây thường gặp", icon = Icons.Rounded.Grass, iconTint = MaterialTheme.colorScheme.primary, content = crops)
            Spacer(modifier = Modifier.height(16.dp))
        }
        if (identification.isNotBlank()) {
            SectionCard(title = "Dấu hiệu nhận biết", icon = Icons.Rounded.Info, iconTint = MaterialTheme.colorScheme.secondary, content = identification)
            Spacer(modifier = Modifier.height(16.dp))
        }
        if (danger.isNotBlank()) {
            SectionCard(title = "Gây hại", icon = Icons.Rounded.Warning, iconTint = MaterialTheme.colorScheme.error, content = danger)
            Spacer(modifier = Modifier.height(16.dp))
        }
        if (detailedBug.whereToFind.isNotEmpty() || detailedBug.season.isNotBlank()) {
            val where = listOf(
                detailedBug.whereToFind.joinDisplay(prefix = "Thường thấy ở: "),
                detailedBug.season.takeIf { it.isNotBlank() }?.let { "Thời điểm thường gặp: $it" }.orEmpty()
            ).joinDisplay().cleanFarmerText()
            SectionCard(title = "Khi kiểm tra ruộng vườn", icon = Icons.Rounded.Place, iconTint = MaterialTheme.colorScheme.secondary, content = where)
            Spacer(modifier = Modifier.height(16.dp))
        }
        if (treatment.isNotBlank()) {
            SectionCard(title = "Nên làm gì?", icon = Icons.Rounded.Eco, iconTint = MaterialTheme.colorScheme.primary, content = treatment)
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Thanh nút bấm hành động được neo cố định dưới cùng màn hình (Share, AI Chat).
 *
 * @param detailedBug Đối tượng chứa thông tin sinh vật để truyền dữ liệu cho tính năng chia sẻ hoặc hỏi AI.
 * @param confidence Độ tin cậy (%) của kết quả nhận diện.
 * @param source Nguồn nhận diện (YOLO hoặc iNaturalist).
 * @param onAskChatbotClick Callback điều hướng sang màn hình Chatbot.
 * @param onShareClick Callback kích hoạt tính năng chia sẻ native.
 */
@Composable
private fun BugDetailBottomBar(
    detailedBug: BugInfo,
    confidence: Float,
    source: ScanSource,
    onAskChatbotClick: (String, BugInfo) -> Unit,
    onShareClick: (BugInfo) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 16.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = { onShareClick(detailedBug) },
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Rounded.Share, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Chia sẻ", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { onAskChatbotClick("", detailedBug) },
                modifier = Modifier.weight(1.5f).height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Rounded.SmartToy, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                Spacer(Modifier.width(8.dp))
                Text("Hỏi BugScanner AI", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

/**
 * Component thẻ thông tin chuẩn hóa để hiển thị từng mục tài liệu (Đặc điểm, Xử lý...).
 *
 * @param title Tiêu đề lớn hiển thị đầu thẻ.
 * @param icon Biểu tượng minh họa nằm cạnh tiêu đề.
 * @param iconTint Màu sắc chủ đạo của biểu tượng.
 * @param content Nội dung văn bản chi tiết.
 */
@Composable
fun SectionCard(title: String, icon: ImageVector, iconTint: Color, content: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = iconTint)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            SectionBody(content = content)
        }
    }
}

@Composable
private fun SectionBody(content: String) {
    val items = DetailSectionTextPolicy.sectionItems(content)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEach { item ->
            Row(verticalAlignment = Alignment.Top) {
                if (items.size > 1) {
                    Text(
                        text = "•",
                        modifier = Modifier.padding(end = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                SectionItemText(
                    item = item,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SectionItemText(item: DetailSectionItem, modifier: Modifier = Modifier) {
    Text(
        text = buildAnnotatedString {
            if (item.label.isNotBlank()) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(item.label)
                    append(": ")
                }
            }
            append(item.body)
        },
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.35f
    )
}

private fun List<String>.joinDisplay(prefix: String = ""): String {
    val value = map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .joinToString("; ")
    return if (value.isBlank()) "" else prefix + value
}

private fun String.cleanFarmerText(): String {
    val forbidden = listOf("YOLO", "IP102", "GBIF", "iNaturalist", "dataset", "mã lớp", "nhãn mô hình")
    return lines()
        .map { it.trim() }
        .filter { line -> line.isNotBlank() && forbidden.none { line.contains(it, ignoreCase = true) } }
        .joinToString("\n")
        .trim()
}