package hcmus.bugscanner.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import hcmus.bugscanner.domain.model.BugInfo

/**
 * Thẻ (Card) hiển thị thông tin tóm tắt và hình ảnh của một loài côn trùng.
 * Được thiết kế theo chuẩn Material Design 3, bao gồm bo góc và xử lý cắt cúp hình ảnh.
 * Tự động điều chỉnh chiều rộng để tương thích với các Adaptive Layout (Grid/List).
 *
 * @param bug Đối tượng chứa dữ liệu chi tiết của côn trùng cần hiển thị.
 * @param modifier Modifier tùy chỉnh vị trí, kích thước hoặc các hiệu ứng từ component cha.
 * @param onClick Callback được kích hoạt khi người dùng nhấn (tap) vào toàn bộ khu vực của thẻ.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BugItemCard(
    bug: BugInfo,
    modifier: Modifier = Modifier,
    onClick: (BugInfo) -> Unit = {}
) {
    var failedImageUrls by remember(bug.id, bug.displayImageUrls()) { mutableStateOf<Set<String>>(emptySet()) }
    val imageUrl = bug.displayImageUrls(excludedUrls = failedImageUrls).firstOrNull().orEmpty()

    Card(
        onClick = { onClick(bug) },
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            BugImage(
                imageUrl = imageUrl,
                contentDescription = "Hình ảnh của ${bug.name}",
                contentScale = ContentScale.Crop,
                onLoadFailed = { failedUrl ->
                    failedImageUrls = failedImageUrls + failedUrl
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            )

            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = bug.name,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = bug.scientificName.ifBlank { "Chưa cập nhật tên khoa học" },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                Text(
                    text = bug.description.ifBlank { "Đang cập nhật thông tin mô tả chi tiết cho loài côn trùng này..." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.4f
                )
            }
        }
    }
}
