package hcmus.bugscanner.ui.wiki

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import hcmus.bugscanner.domain.model.BugInfo
import hcmus.bugscanner.ui.components.BugItemCard

/**
 * Màn hình chính của tính năng Bách khoa toàn thư.
 */
@Composable
fun EncyclopediaScreen(
    viewModel: EncyclopediaViewModel = viewModel(),
    onBugSelected: (BugInfo) -> Unit = {}
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val primaryGreen = Color(0xFF2E7D32)

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF1F8E9))) {
        PrimaryTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Color.White,
            contentColor = primaryGreen,
            indicator = {
                TabRowDefaults.PrimaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(selectedTabIndex),
                    width = androidx.compose.ui.unit.Dp.Unspecified,
                    color = primaryGreen
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

        if (selectedTabIndex == 0) {
            ExploreTab(viewModel = viewModel, onBugSelected = onBugSelected)
        } else {
            SearchTab(viewModel, onBugSelected)
        }
    }
}

/**
 * Tab hiển thị danh sách các loài côn trùng nổi bật dạng lưới (Dữ liệu Firebase).
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
                CircularProgressIndicator(color = Color(0xFF2E7D32))
            }
        } else if (exploreList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Không tìm thấy kết quả nào", color = Color.Gray)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
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
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column {
                            AsyncImage(
                                model = bug.imageUrl,
                                contentDescription = bug.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .background(Color.LightGray)
                            )
                            Text(
                                text = bug.name,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = Color(0xFF1B5E20),
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
 * Tab cho phép tra cứu thông tin côn trùng qua API Wikipedia.
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
                viewModel.searchInsects(it)
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
                Text("Không tìm thấy kết quả nào cho '$searchQuery'", modifier = Modifier.padding(top = 32.dp))
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(searchResults) { bug ->
                        BugItemCard(bug, onBugSelected)
                    }
                }
            }
        }
    }
}