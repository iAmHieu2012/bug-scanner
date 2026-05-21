package hcmus.bugscanner.ui.wiki

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import hcmus.bugscanner.domain.model.BugInfo
import hcmus.bugscanner.ui.components.BugItemCard

/**
 * Màn hình Bách khoa toàn thư - Tích hợp Responsive Layout bằng GridCells.Adaptive.
 *
 * @param viewModel ViewModel quản lý trạng thái tải, tìm kiếm và dữ liệu Wikipedia.
 * @param onBugSelected Callback chuyển sang màn hình Chi tiết khi nhấn vào một thẻ côn trùng.
 */
@Composable
fun EncyclopediaScreen(
    viewModel: EncyclopediaViewModel = viewModel { EncyclopediaViewModel() },
    onBugSelected: (BugInfo) -> Unit = {}
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Thanh chọn Tab điều hướng (Khám phá nội bộ hoặc Tra cứu Wikipedia)
        PrimaryTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = {
                TabRowDefaults.PrimaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(selectedTabIndex),
                    width = androidx.compose.ui.unit.Dp.Unspecified,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = { Text("Khám phá", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Rounded.GridView, contentDescription = null) }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = { Text("Tra cứu", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Rounded.Search, contentDescription = null) }
            )
        }

        // Nội dung của từng Tab
        if (selectedTabIndex == 0) {
            ExploreTab(viewModel = viewModel, onBugSelected = onBugSelected)
        } else {
            SearchTab(viewModel, onBugSelected)
        }
    }
}

/**
 * Tab hiển thị danh sách các loài côn trùng nổi bật dạng lưới động (Adaptive Grid).
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
            placeholder = { Text("Tìm côn trùng trong dữ liệu ứng dụng...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            singleLine = true
        )

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (exploreList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Không tìm thấy kết quả nào", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyVerticalGrid(
                // Adaptive Layout: Mỗi cột rộng ít nhất 150dp. Compose sẽ tự động chèn thêm cột
                // nếu màn hình đủ rộng, ngăn chặn tình trạng phần tử bị kéo giãn thô kệch.
                columns = GridCells.Adaptive(minSize = 150.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(exploreList) { bug ->
                    Card(
                        onClick = { onBugSelected(bug) },
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column {
                            AsyncImage(
                                model = bug.imageUrl.takeIf { it.isNotBlank() } ?: "https://via.placeholder.com/300?text=No+Image",
                                contentDescription = bug.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    // Khóa tỷ lệ khung hình 1:1 cho ảnh để các card luôn cao bằng nhau
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
 * Tab tra cứu thông tin côn trùng qua API Wikipedia.
 * Sử dụng Adaptive Grid để tối ưu không gian trên các màn hình lớn.
 */
@Composable
fun SearchTab(
    viewModel: EncyclopediaViewModel,
    onBugSelected: (BugInfo) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val searchResults by viewModel.searchResults.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                viewModel.searchInsects(it) // Gửi truy vấn API tự động khi gõ
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            placeholder = { Text("Nhập tên côn trùng để tra cứu Wikipedia...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            singleLine = true
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
                // Thay vì dùng LazyColumn (1 cột duy nhất), ta nâng cấp lên LazyVerticalGrid
                // Mỗi BugItemCard cần không gian lớn hơn dạng thẻ mini bên ExploreTab, nên minSize = 350dp
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 350.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(searchResults) { bug ->
                        // Đảm bảo BugItemCard đã được loại bỏ thuộc tính Modifier.fillMaxWidth()
                        // bên trong nội bộ của nó (như ta đã sửa ở file trước) để nó ngoan ngoãn chui vào Grid
                        BugItemCard(bug = bug, onClick = onBugSelected)
                    }
                }
            }
        }
    }
}