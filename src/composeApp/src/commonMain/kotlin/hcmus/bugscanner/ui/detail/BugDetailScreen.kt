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
import hcmus.bugscanner.data.repository.EncyclopediaRepositoryImpl
import hcmus.bugscanner.domain.model.BugInfo
import hcmus.bugscanner.domain.repository.EncyclopediaRepository

/**
 * Màn hình hiển thị thông tin chi tiết đầy đủ của một loài côn trùng.
 */
@Composable
fun BugDetailScreen(
    bug: BugInfo,
    onBackClick: () -> Unit,
    onAskChatbotClick: (String) -> Unit,
    onShareClick: (BugInfo) -> Unit
) {
    val scrollState = rememberScrollState()
    var detailedBug by remember { mutableStateOf(bug) }
    var isLoading by remember { mutableStateOf(false) }
    val repository: EncyclopediaRepository = remember { EncyclopediaRepositoryImpl() }

    LaunchedEffect(bug.scientificName) {
        if (bug.treatment.isBlank() && bug.identification.isBlank()) {
            isLoading = true
            val realBug = repository.getBugByScientificName(bug.scientificName)
            if (realBug != null) {
                detailedBug = realBug
            }
            isLoading = false
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AsyncImage(
            model = detailedBug.imageUrl.takeIf { it.isNotBlank() } ?: "https://via.placeholder.com/500?text=Hình+ảnh+côn+trùng",
            contentDescription = "Bug Image",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
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
                .padding(top = 300.dp)
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
                .padding(bottom = 100.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
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
                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Đã quét", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

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
        }

        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 16.dp,
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
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
}

@Composable
fun SectionCard(title: String, icon: ImageVector, iconTint: Color, content: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) // Giảm elevation vì surfaceVariant đã phân biệt khá tốt với nền
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = iconTint)
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
            Text(content, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.5f)
        }
    }
}