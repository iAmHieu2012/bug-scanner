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
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
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
import hcmus.bugscanner.domain.model.ConfidencePolicy
import hcmus.bugscanner.domain.model.HarmfulnessLevel
import hcmus.bugscanner.domain.model.ScanSource
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
    var failedImageUrls by remember(currentBug.id, currentBug.scientificName) { mutableStateOf<Set<String>>(emptySet()) }
    val imageUrls = currentBug.displayImageUrls(excludedUrls = failedImageUrls)
    var selectedImageUrl by remember { mutableStateOf("") }

    LaunchedEffect(imageUrls) {
        if (selectedImageUrl !in imageUrls) {
            selectedImageUrl = imageUrls.firstOrNull().orEmpty()
        }
    }

    val heroImageUrl = selectedImageUrl.ifBlank { imageUrls.firstOrNull().orEmpty() }
    val onImageLoadFailed: (String) -> Unit = { failedUrl ->
        failedImageUrls = failedImageUrls + failedUrl
    }
    fun selectRelativeImage(step: Int) {
        if (imageUrls.size <= 1) return
        val currentIndex = imageUrls.indexOf(heroImageUrl).takeIf { it >= 0 } ?: 0
        val nextIndex = (currentIndex + step + imageUrls.size) % imageUrls.size
        selectedImageUrl = imageUrls[nextIndex]
    }

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
                        imageUrl = heroImageUrl,
                        contentDescription = "Ảnh côn trùng",
                        contentScale = ContentScale.Fit,
                        onLoadFailed = onImageLoadFailed,
                        modifier = Modifier.fillMaxSize()
                    )
                    ImageNavigationButtons(
                        canNavigate = imageUrls.size > 1,
                        onPrevious = { selectRelativeImage(-1) },
                        onNext = { selectRelativeImage(1) },
                        modifier = Modifier.align(Alignment.BottomCenter)
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
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(scrollState)
                            .padding(24.dp)
                    ) {
                        BugDetailContent(
                            detailedBug = currentBug,
                            confidence = confidence,
                            source = source,
                            selectedImageUrl = heroImageUrl,
                            failedImageUrls = failedImageUrls,
                            onImageLoadFailed = onImageLoadFailed,
                            onImageSelected = { selectedImageUrl = it },
                            isLoading = isLoading
                        )
                    }
                    BugDetailBottomBar(currentBug, confidence, source, onAskChatbotClick, onShareClick)
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(bottom = 100.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                            .background(Color.Black)
                    ) {
                        BugImage(
                            imageUrl = heroImageUrl,
                            contentDescription = "Ảnh côn trùng",
                            contentScale = ContentScale.Fit,
                            onLoadFailed = onImageLoadFailed,
                            modifier = Modifier.fillMaxSize()
                        )
                        ImageNavigationButtons(
                            canNavigate = imageUrls.size > 1,
                            onPrevious = { selectRelativeImage(-1) },
                            onNext = { selectRelativeImage(1) },
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(24.dp)
                    ) {
                        BugDetailContent(
                            detailedBug = currentBug,
                            confidence = confidence,
                            source = source,
                            selectedImageUrl = heroImageUrl,
                            failedImageUrls = failedImageUrls,
                            onImageLoadFailed = onImageLoadFailed,
                            onImageSelected = { selectedImageUrl = it },
                            isLoading = isLoading
                        )
                    }
                }

                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .padding(top = 48.dp, start = 16.dp)
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
                }

                Box(modifier = Modifier.align(Alignment.BottomCenter)) {
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
 */
@Composable
private fun BugDetailContent(
    detailedBug: BugInfo,
    confidence: Float,
    source: ScanSource,
    selectedImageUrl: String,
    failedImageUrls: Set<String>,
    onImageLoadFailed: (String) -> Unit,
    onImageSelected: (String) -> Unit,
    isLoading: Boolean
) {
    val confidenceInfo = ConfidencePolicy.explain(confidence)
    val harmfulness = HarmfulnessLevel.fromValue(detailedBug.harmfulnessLevel)

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

        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Đã nhận diện", color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Bold)
            }
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

    Spacer(modifier = Modifier.height(24.dp))

    if (isLoading) {
        Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    } else {
        BugImageGallery(
            bug = detailedBug,
            selectedImageUrl = selectedImageUrl,
            failedImageUrls = failedImageUrls,
            onImageLoadFailed = onImageLoadFailed,
            onImageSelected = onImageSelected
        )
        Spacer(modifier = Modifier.height(16.dp))

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
            SectionCard(title = "Khi kiểm tra ruộng vườn", icon = Icons.Rounded.TravelExplore, iconTint = MaterialTheme.colorScheme.secondary, content = where)
            Spacer(modifier = Modifier.height(16.dp))
        }
        if (treatment.isNotBlank()) {
            SectionCard(title = "Nên làm gì", icon = Icons.Rounded.Eco, iconTint = MaterialTheme.colorScheme.primary, content = treatment)
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

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
                containerColor = MaterialTheme.colorScheme.errorContainer,
                labelColor = MaterialTheme.colorScheme.onErrorContainer,
                leadingIconContentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        )
        sourceLabel?.let {
            AssistChip(
                onClick = {},
                label = { Text(it) },
                leadingIcon = { Icon(Icons.Rounded.Memory, null, modifier = Modifier.size(16.dp)) }
            )
        }
    }
}

@Composable
private fun BugImageGallery(
    bug: BugInfo,
    selectedImageUrl: String,
    failedImageUrls: Set<String>,
    onImageLoadFailed: (String) -> Unit,
    onImageSelected: (String) -> Unit
) {
    val images = bug.displayImageUrls(excludedUrls = failedImageUrls)
    if (images.size <= 1) return

    Column {
        Text(
            text = "Thư viện ảnh",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(10.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(end = 4.dp)
        ) {
            items(images) { imageUrl ->
                Card(
                    onClick = { onImageSelected(imageUrl) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (imageUrl == selectedImageUrl) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) {
                    BugImage(
                        imageUrl = imageUrl,
                        contentDescription = "Ảnh tham khảo ${bug.name}",
                        contentScale = ContentScale.Crop,
                        onLoadFailed = onImageLoadFailed,
                        modifier = Modifier
                            .size(width = 140.dp, height = 110.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                }
            }
        }
    }
}

@Composable
private fun ImageNavigationButtons(
    canNavigate: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!canNavigate) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ImageArrowButton(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = "Ảnh trước",
            onClick = onPrevious
        )
        ImageArrowButton(
            icon = Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = "Ảnh tiếp theo",
            onClick = onNext
        )
    }
}

@Composable
private fun ImageArrowButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(46.dp)
            .background(Color.Black.copy(alpha = 0.48f), CircleShape)
    ) {
        Icon(icon, contentDescription = contentDescription, tint = Color.White)
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
