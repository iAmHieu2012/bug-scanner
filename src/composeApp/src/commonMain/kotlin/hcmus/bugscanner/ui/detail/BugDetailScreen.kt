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
import coil3.compose.AsyncImage
import hcmus.bugscanner.domain.model.BugInfo
import hcmus.bugscanner.domain.repository.EncyclopediaRepository
import org.koin.compose.koinInject

/**
 * Màn hình hiển thị thông tin chi tiết đầy đủ của một loài côn trùng.
 * Tự động chuyển đổi bố cục (Adaptive Layout) dựa trên kích thước khung hình hiện tại.
 *
 * @param bug Dữ liệu cơ bản của côn trùng được truyền từ màn hình trước.
 * @param onBackClick Callback xử lý sự kiện quay lại.
 * @param onAskChatbotClick Callback chuyển hướng sang màn hình Chatbot với câu hỏi (prompt) được thiết lập sẵn.
 * @param onShareClick Callback gọi hệ thống chia sẻ (Share Intent).
 */
@Composable
fun BugDetailScreen(
    bug: BugInfo,
    onBackClick: () -> Unit,
    onAskChatbotClick: (String) -> Unit,
    onShareClick: (BugInfo) -> Unit
) {
    // Trạng thái cuộn cho phần nội dung chi tiết
    val scrollState = rememberScrollState()

    // Quản lý trạng thái dữ liệu: detailedBug có thể được cập nhật thêm thông tin từ Firebase
    var detailedBug by remember { mutableStateOf(bug) }
    var isLoading by remember { mutableStateOf(false) }

    // Khởi tạo Repository lấy dữ liệu thông qua Koin
    val repository: EncyclopediaRepository = koinInject()

    // Khởi tạo Wiki API để lấy thông tin mô tả chi tiết nếu cần thông qua Koin
    val wikiApi: hcmus.bugscanner.data.remote.WikiApiService = koinInject()

    // Fetch dữ liệu chi tiết nếu các trường quan trọng đang bị trống
    LaunchedEffect(bug.scientificName) {
        // Điều kiện chạy: Nếu đang thiếu cách xử lý (quét từ camera) HOẶC có link wiki cần tải (từ iNaturalist)
        if (bug.treatment.isBlank() || bug.wikiUrl.isNotBlank()) {
            isLoading = true

            // 1. Ưu tiên tìm trong Firebase trước (đảm bảo tính năng Quét Camera không bị ảnh hưởng)
            val realBug = repository.getBugByScientificName(bug.scientificName)

            if (realBug != null) {
                // Nếu đối tượng 'bug' ban đầu được truyền vào có chứa link ảnh (từ Lịch sử),
                // thì giữ lại ảnh đó, chỉ đè các thông tin text (description, treatment...)
                // Nếu không có ảnh lịch sử thì mới lấy ảnh mặc định của database (realBug.imageUrl)
                detailedBug = realBug.copy(
                    imageUrl = bug.imageUrl.takeIf { it.isNotBlank() } ?: realBug.imageUrl
                )
            } else if (bug.wikiUrl.isNotBlank()) {
                // 2. Nếu Firebase KHÔNG CÓ, tiến hành bóc tách Link Wiki để kéo Text
                // Ví dụ link: http://en.wikipedia.org/wiki/Western_honey_bee
                val uriParts = bug.wikiUrl.split("/wiki/")
                if (uriParts.size == 2) {
                    val lang = uriParts[0].substringAfter("://").substringBefore(".") // Lấy "en" hoặc "vi"
                    val title = uriParts[1] // Lấy "Western_honey_bee"

                    val summary = wikiApi.getSummaryByTitle(title, lang)

                    if (!summary.isNullOrBlank()) {
                        // Cập nhật lại description bằng text mới kéo về
                        detailedBug = detailedBug.copy(description = summary)
                    }
                }
            }
            isLoading = false
        }
    }

    // Sử dụng BoxWithConstraints để đo kích thước màn hình và quyết định luồng giao diện
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (maxWidth > 800.dp) {
            // =================================================================
            // LAYOUT MÀN HÌNH RỘNG (Web / Desktop / Tablet ngang)
            // =================================================================
            Row(modifier = Modifier.fillMaxSize()) {
                // Cột trái (40%): Hiển thị hình ảnh kích thước lớn và nút Back
                Box(
                    modifier = Modifier
                        .weight(0.4f)
                        .fillMaxHeight()
                        .background(Color.Black) // Nền đen cho ảnh nếu ảnh không lấp đầy
                ) {
                    AsyncImage(
                        model = detailedBug.imageUrl.takeIf { it.isNotBlank() } ?: "https://via.placeholder.com/1000?text=Hình+ảnh+côn+trùng",
                        contentDescription = "Bug Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Nút Back đặt góc trên cùng bên trái
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

                // Cột phải (60%): Cuộn nội dung thông tin và BottomBar cố định đáy
                Column(
                    modifier = Modifier
                        .weight(0.6f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    // Nội dung thông tin cuộn (Chiếm toàn bộ không gian còn lại bằng weight = 1f)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(scrollState)
                            .padding(24.dp)
                    ) {
                        BugDetailContent(detailedBug, isLoading)
                    }

                    // Thanh thao tác (BottomBar) cố định ở dưới cùng
                    BugDetailBottomBar(detailedBug, onAskChatbotClick, onShareClick)
                }
            }
        } else {
            // =================================================================
            // LAYOUT MÀN HÌNH HẸP (Mobile / Tablet dọc)
            // =================================================================
            Box(modifier = Modifier.fillMaxSize()) {
                // Hình ảnh nền ở trên cùng
                AsyncImage(
                    model = detailedBug.imageUrl.takeIf { it.isNotBlank() } ?: "https://via.placeholder.com/500?text=Hình+ảnh+côn+trùng",
                    contentDescription = "Bug Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                )

                // Nút Back
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .padding(top = 48.dp, start = 16.dp)
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
                }

                // Vùng nội dung cuộn đè lên hình ảnh một khoảng (padding top 300.dp)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 300.dp)
                        .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                        .background(MaterialTheme.colorScheme.background)
                        .verticalScroll(scrollState)
                        .padding(bottom = 100.dp) // Chừa khoảng trống cho BottomBar
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        BugDetailContent(detailedBug, isLoading)
                    }
                }

                // Thanh thao tác (BottomBar) neo tại đáy Box
                Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                    BugDetailBottomBar(detailedBug, onAskChatbotClick, onShareClick)
                }
            }
        }
    }
}

/**
 * Khối Component hiển thị danh sách các trường thông tin chi tiết.
 * Trích xuất để tái sử dụng giữa các bố cục ngang/dọc.
 * * @param detailedBug Đối tượng chứa thông tin chi tiết của sinh vật.
 * @param isLoading Trạng thái tải dữ liệu từ API/Firebase.
 */
@Composable
private fun BugDetailContent(detailedBug: BugInfo, isLoading: Boolean) {
    // Header: Tên phổ thông, Tên khoa học và Nhãn trạng thái
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
            color = MaterialTheme.colorScheme.errorContainer,
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Đã quét", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    // Trạng thái Loading hoặc Danh sách thẻ thông tin
    if (isLoading) {
        Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    } else {
        if (detailedBug.description.isNotBlank()) {
            SectionCard(
                title = "Tổng quan",
                icon = Icons.AutoMirrored.Rounded.MenuBook,
                iconTint = MaterialTheme.colorScheme.secondary,
                content = detailedBug.description
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        if (detailedBug.identification.isNotBlank()) {
            SectionCard(
                title = "Đặc điểm nhận dạng",
                icon = Icons.Rounded.Info,
                iconTint = MaterialTheme.colorScheme.secondary,
                content = detailedBug.identification
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        if (detailedBug.danger.isNotBlank()) {
            SectionCard(
                title = "Mức độ nguy hại",
                icon = Icons.Rounded.Warning,
                iconTint = MaterialTheme.colorScheme.error,
                content = detailedBug.danger
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        if (detailedBug.treatment.isNotBlank()) {
            SectionCard(
                title = "Biện pháp xử lý (Khuyên dùng)",
                icon = Icons.Rounded.Eco,
                iconTint = MaterialTheme.colorScheme.primary,
                content = detailedBug.treatment
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Thanh nút bấm hành động được neo dưới cùng màn hình (Share, AI Chat).
 * * @param detailedBug Đối tượng chứa thông tin sinh vật để chia sẻ hoặc hỏi AI.
 * @param onAskChatbotClick Callback điều hướng sang Chatbot.
 * @param onShareClick Callback kích hoạt chia sẻ native.
 */
@Composable
private fun BugDetailBottomBar(
    detailedBug: BugInfo,
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
                onClick = { onAskChatbotClick("Cung cấp cho tôi thông tin chi tiết và cách xử lý ${detailedBug.name}?") },
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
 * Component thẻ thông tin chuẩn hóa cho từng mục (Đặc điểm, Xử lý...).
 * * @param title Tiêu đề của thẻ.
 * @param icon Biểu tượng minh họa.
 * @param iconTint Màu sắc của biểu tượng.
 * @param content Nội dung văn bản hiển thị.
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