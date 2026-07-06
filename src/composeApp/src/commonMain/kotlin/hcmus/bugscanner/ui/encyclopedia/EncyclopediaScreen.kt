package hcmus.bugscanner.ui.encyclopedia

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import hcmus.bugscanner.domain.model.BugInfo
import hcmus.bugscanner.ui.components.BugItemCard
import hcmus.bugscanner.ui.components.BugImage
import hcmus.bugscanner.ui.components.EmptyState
import hcmus.bugscanner.ui.components.ScreenHeader
import org.koin.compose.viewmodel.koinViewModel

/**
 * Màn hình Bách khoa toàn thư - Tích hợp Responsive Layout bằng GridCells.Adaptive.
 *
 * @param viewModel ViewModel quản lý trạng thái tải, tìm kiếm và dữ liệu Wikipedia.
 * @param onBugSelected Callback chuyển sang màn hình Chi tiết khi nhấn vào một thẻ côn trùng.
 */
@Composable
fun EncyclopediaScreen(
    viewModel: EncyclopediaViewModel = koinViewModel(),
    useDarkTheme: Boolean = true,
    onThemeToggle: (() -> Unit)? = null,
    onBugSelected: (BugInfo) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ScreenHeader(
            title = "Bách khoa côn trùng",
            subtitle = "Dữ liệu nhận diện và thông tin sinh học tham khảo.",
            leadingIcon = Icons.Rounded.GridView,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 6.dp),
            action = {
                if (onThemeToggle != null) {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 3.dp
                    ) {
                        IconButton(onClick = onThemeToggle) {
                            Icon(
                                imageVector = if (useDarkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                                contentDescription = if (useDarkTheme) {
                                    "Chuyển sang giao diện sáng"
                                } else {
                                    "Chuyển sang giao diện tối"
                                },
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        )

        ExploreTab(viewModel = viewModel, onBugSelected = onBugSelected)
    }
}

/**
 * Tab hiển thị danh sách các loài côn trùng nổi bật dạng lưới động (Adaptive Grid).
 * Card được cấu hình tỷ lệ 1:1 cho hình ảnh để duy trì tính đồng nhất trên giao diện đa cột.
 *
 * @param viewModel ViewModel chứa luồng dữ liệu Khám phá.
 * @param onBugSelected Callback xử lý nhấn vào thẻ côn trùng.
 */
@Composable
fun ExploreTab(
    viewModel: EncyclopediaViewModel,
    onBugSelected: (BugInfo) -> Unit
) {
    val exploreList by viewModel.exploreList.collectAsState()
    val searchQuery by viewModel.exploreSearchQuery.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = viewModel::onExploreSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            placeholder = { Text("Tìm côn trùng trong bách khoa ứng dụng...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp)
        )

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (exploreList.isEmpty()) {
            EmptyState("Không tìm thấy kết quả nào")
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 220.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(exploreList) { bug ->
                    var failedImageUrls by remember(bug.id, bug.displayImageUrls()) { mutableStateOf<Set<String>>(emptySet()) }
                    val imageUrl = bug.displayImageUrls(excludedUrls = failedImageUrls).firstOrNull().orEmpty()
                    Card(
                        onClick = { onBugSelected(bug) },
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column {
                            BugImage(
                                imageUrl = imageUrl,
                                contentDescription = bug.name,
                                contentScale = ContentScale.Crop,
                                onLoadFailed = { failedUrl ->
                                    failedImageUrls = failedImageUrls + failedUrl
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            )
                            Text(
                                text = bug.name,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tab tra cứu thông tin côn trùng qua API iNaturalist.
 *
 * @param viewModel ViewModel chứa logic tìm kiếm iNaturalist.
 * @param onBugSelected Callback xử lý nhấn vào thẻ kết quả tìm kiếm.
 */
@Composable
fun SearchTab(
    viewModel: EncyclopediaViewModel,
    onBugSelected: (BugInfo) -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = viewModel::searchInsects,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            placeholder = { Text("Nhập tên côn trùng để tra cứu thêm...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp)
        )

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            if (isLoading && searchResults.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.padding(top = 32.dp))
            } else if (searchResults.isEmpty() && searchQuery.isNotEmpty()) {
                Text(
                    text = "Không tìm thấy kết quả nào cho '$searchQuery'",
                    modifier = Modifier.padding(top = 32.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 350.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(searchResults) { bug ->
                        BugItemCard(bug = bug, onClick = onBugSelected)
                    }
                }
            }
        }
    }
}
