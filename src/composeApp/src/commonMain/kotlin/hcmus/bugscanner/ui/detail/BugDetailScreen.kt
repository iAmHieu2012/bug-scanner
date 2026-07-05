package hcmus.bugscanner.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
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
    onAskChatbotClick: (String) -> Unit,
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
                            BugDetailHeader(currentBug, source)
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
                        BugDetailHeader(currentBug, source)
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
@Composable
private fun BugDetailHeader(detailedBug: BugInfo, source: ScanSource) {
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

        if (source != ScanSource.UNKNOWN) {
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
        if (detailedBug.description.isNotBlank()) {
            SectionCard(title = "Tổng quan", icon = Icons.AutoMirrored.Rounded.MenuBook, iconTint = MaterialTheme.colorScheme.secondary, content = detailedBug.description)
            Spacer(modifier = Modifier.height(16.dp))
        }
        if (detailedBug.identification.isNotBlank()) {
            SectionCard(title = "Đặc điểm nhận dạng", icon = Icons.Rounded.Info, iconTint = MaterialTheme.colorScheme.secondary, content = detailedBug.identification)
            Spacer(modifier = Modifier.height(16.dp))
        }
        if (detailedBug.danger.isNotBlank()) {
            SectionCard(title = "Mức độ nguy hại", icon = Icons.Rounded.Warning, iconTint = MaterialTheme.colorScheme.error, content = detailedBug.danger)
            Spacer(modifier = Modifier.height(16.dp))
        }
        if (detailedBug.treatment.isNotBlank()) {
            SectionCard(title = "Biện pháp xử lý (Khuyên dùng)", icon = Icons.Rounded.Eco, iconTint = MaterialTheme.colorScheme.primary, content = detailedBug.treatment)
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
    onAskChatbotClick: (String) -> Unit,
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
                onClick = { onAskChatbotClick(ChatPromptSuggestions.detailPrompt(detailedBug, confidence, source)) },
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

            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.5f
            )
        }
    }
}