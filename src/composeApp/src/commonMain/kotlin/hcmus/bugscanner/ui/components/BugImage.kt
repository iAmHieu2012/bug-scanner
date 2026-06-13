package hcmus.bugscanner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

/**
 * Component hiển thị hình ảnh côn trùng.
 * Tự động kiểm tra quyền tải ảnh từ xa và chuyển sang chế độ hiển thị Fallback nếu link rỗng hoặc tải lỗi.
 *
 * @param imageUrl Đường dẫn URL tĩnh của hình ảnh.
 * @param contentDescription Mô tả hình ảnh hỗ trợ trợ năng.
 * @param modifier Modifier tùy chỉnh bố cục.
 * @param contentScale Tỷ lệ thu phóng/cắt hình ảnh (mặc định là Crop).
 */
@Composable
fun BugImage(
    imageUrl: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    var loadFailed by remember(imageUrl) { mutableStateOf(false) }

    if (imageUrl.isBlank() || loadFailed || !canLoadRemoteImage(imageUrl)) {
        BugImageFallback(
            contentDescription = contentDescription,
            modifier = modifier
        )
    } else {
        AsyncImage(
            model = imageUrl,
            contentDescription = contentDescription,
            contentScale = contentScale,
            onError = { loadFailed = true },
            modifier = modifier
        )
    }
}

/**
 * Component hiển thị ảnh thay thế (Fallback) khi không có hình ảnh thực tế hoặc lỗi tải.
 *
 * @param contentDescription Mô tả hỗ trợ trợ năng.
 * @param modifier Modifier tùy chỉnh bố cục.
 */
@Composable
private fun BugImageFallback(
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Image,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.size(32.dp)
        )
    }
}
