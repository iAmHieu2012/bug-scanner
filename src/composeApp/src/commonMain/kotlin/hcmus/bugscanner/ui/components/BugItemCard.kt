package hcmus.bugscanner.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import hcmus.bugscanner.domain.model.BugInfo
import coil3.compose.AsyncImage

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
    Card(
        onClick = { onClick(bug) },
        // Sử dụng Modifier truyền từ ngoài vào, kết hợp với các cài đặt mặc định
        modifier = modifier
            .fillMaxWidth() // Chiếm toàn bộ không gian được Parent cấp cho
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Vùng hiển thị hình ảnh
            AsyncImage(
                // Xử lý fallback URL nếu ảnh rỗng để UI không bị gãy
                model = bug.imageUrl.takeIf { it.isNotBlank() } ?: "https://via.placeholder.com/400?text=Hình+ảnh+côn+trùng",
                contentDescription = "Hình ảnh của ${bug.name}",
                contentScale = ContentScale.Crop, // Cắt cúp ảnh để lấp đầy khung mà không méo tỷ lệ
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    // Bo 2 góc trên của ảnh để ăn khớp với độ bo của Card
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            )

            // Vùng nội dung chữ
            Column(modifier = Modifier.padding(16.dp)) {
                // Tên phổ thông
                Text(
                    text = bug.name,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1, // Giới hạn 1 dòng, tránh tên quá dài làm xô lệch layout
                    overflow = TextOverflow.Ellipsis
                )

                // Tên khoa học
                Text(
                    text = bug.scientificName.ifBlank { "Chưa cập nhật tên khoa học" },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                // Mô tả tóm tắt
                Text(
                    text = bug.description.ifBlank { "Đang cập nhật thông tin mô tả chi tiết cho loài côn trùng này..." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3, // Giới hạn mô tả tối đa 3 dòng để Card không bị quá dài
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.4f
                )
            }
        }
    }
}