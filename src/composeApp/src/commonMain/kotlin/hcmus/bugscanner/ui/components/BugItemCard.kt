package hcmus.bugscanner.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import hcmus.bugscanner.domain.model.BugInfo
import coil3.compose.AsyncImage

/**
 * Thẻ hiển thị thông tin tóm tắt và hình ảnh của một loài côn trùng.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BugItemCard(
    bug: BugInfo,
    onClick: (BugInfo) -> Unit = {}
) {
    Card(
        onClick = { onClick(bug) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            AsyncImage(
                model = bug.imageUrl,
                contentDescription = bug.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = bug.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = bug.scientificName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = bug.description,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}