package hcmus.bugscanner.ui.encyclopedia

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import hcmus.bugscanner.domain.model.BugInfo
import hcmus.bugscanner.ui.components.BugEditDialog
import hcmus.bugscanner.ui.components.BugImage
import hcmus.bugscanner.ui.components.BugItemCard
import hcmus.bugscanner.ui.components.EmptyState
import hcmus.bugscanner.ui.components.ScreenHeader
import org.koin.compose.viewmodel.koinViewModel

/**
 * Màn hình Bách khoa toàn thư - Tích hợp Responsive Layout bằng GridCells.Adaptive.
 *
 * @param viewModel ViewModel quản lý trạng thái tải, tìm kiếm và dữ liệu Wikipedia.
 * @param isAdmin Cho biết người dùng hiện tại có quyền Admin hay không.
 * @param onBugSelected Callback chuyển sang màn hình Chi tiết khi nhấn vào một thẻ côn trùng.
 */
@Composable
fun EncyclopediaScreen(
    viewModel: EncyclopediaViewModel = koinViewModel(),
    isAdmin: Boolean = false,
    onBugSelected: (BugInfo) -> Unit = {}
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var bugToEdit by remember { mutableStateOf<BugInfo?>(null) }
    var isAddingNew by remember { mutableStateOf(false) }

    val statusMessage by viewModel.statusMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (isAdmin && selectedTabIndex == 0) {
                FloatingActionButton(
                    onClick = { isAddingNew = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Thêm mới bài viết")
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            ScreenHeader(
                title = "Bách khoa côn trùng",
                subtitle = "Dữ liệu nhận diện và thông tin sinh học tham khảo.",
                leadingIcon = Icons.Rounded.GridView,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 16.dp)
            )

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

            if (selectedTabIndex == 0) {
                ExploreTab(
                    viewModel = viewModel,
                    isAdmin = isAdmin,
                    onBugSelected = onBugSelected,
                    onEditRequest = { bugToEdit = it }
                )
            } else {
                SearchTab(viewModel, onBugSelected)
            }
        }
    }

    if (isAddingNew) {
        BugEditDialog(
            bugInfo = null,
            onDismiss = { isAddingNew = false },
            onSave = {
                viewModel.saveBugEntry(it)
                isAddingNew = false
            }
        )
    }

    bugToEdit?.let { bug ->
        BugEditDialog(
            bugInfo = bug,
            onDismiss = { bugToEdit = null },
            onSave = {
                viewModel.saveBugEntry(it)
                bugToEdit = null
            }
        )
    }
}

/**
 * Tab hiển thị danh sách các loài côn trùng nổi bật dạng lưới động (Adaptive Grid).
 * Card được cấu hình tỷ lệ 1:1 cho hình ảnh để duy trì tính đồng nhất trên giao diện đa cột.
 *
 * @param viewModel ViewModel chứa luồng dữ liệu Khám phá.
 * @param isAdmin Cho biết quyền Admin để hiển thị nút Sửa/Xóa.
 * @param onBugSelected Callback xử lý nhấn vào thẻ côn trùng.
 * @param onEditRequest Callback khi người dùng nhấn nút Sửa.
 */
@Composable
fun ExploreTab(
    viewModel: EncyclopediaViewModel,
    isAdmin: Boolean,
    onBugSelected: (BugInfo) -> Unit,
    onEditRequest: (BugInfo) -> Unit
) {
    val exploreList by viewModel.exploreList.collectAsState()
    val searchQuery by viewModel.exploreSearchQuery.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    var bugToDelete by remember { mutableStateOf<BugInfo?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = viewModel::onExploreSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            placeholder = { Text("Tìm côn trùng trong dữ liệu ứng dụng...") },
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
                columns = GridCells.Adaptive(minSize = 150.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(exploreList.size) { index ->
                    val bug = exploreList[index]
                    
                    if (index == exploreList.lastIndex) {
                        LaunchedEffect(index) {
                            viewModel.loadMoreExploreInsects()
                        }
                    }

                    Card(
                        onClick = { onBugSelected(bug) },
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column {
                            BugImage(
                                imageUrl = bug.imageUrl,
                                contentDescription = bug.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = bug.name,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                if (isAdmin) {
                                    Row(horizontalArrangement = Arrangement.End) {
                                        IconButton(onClick = { onEditRequest(bug) }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.Edit, contentDescription = "Sửa", tint = MaterialTheme.colorScheme.primary)
                                        }
                                        IconButton(onClick = { bugToDelete = bug }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.Delete, contentDescription = "Xóa", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    bugToDelete?.let { bug ->
        AlertDialog(
            onDismissRequest = { bugToDelete = null },
            title = { Text("Xóa bài viết") },
            text = { Text("Bạn có chắc chắn muốn xóa bài viết '${bug.name}' không? Hành động này không thể hoàn tác.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteBugEntry(bug)
                        bugToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Xóa")
                }
            },
            dismissButton = {
                TextButton(onClick = { bugToDelete = null }) {
                    Text("Hủy")
                }
            }
        )
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
            placeholder = { Text("Nhập tên côn trùng để tra cứu iNaturalist...") },
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
