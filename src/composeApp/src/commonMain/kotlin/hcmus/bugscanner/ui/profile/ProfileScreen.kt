package hcmus.bugscanner.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import hcmus.bugscanner.ui.auth.AuthState
import hcmus.bugscanner.ui.auth.AuthViewModel
import hcmus.bugscanner.ui.components.ScreenHeader
import org.koin.compose.viewmodel.koinViewModel

/**
 * Màn hình Hồ sơ (Profile) hiển thị thông tin tài khoản, thông tin ứng dụng và chức năng quản trị (nếu là Admin).
 *
 * @param useDarkTheme Trạng thái giao diện sáng/tối hiện tại.
 * @param onThemeToggle Callback kích hoạt chuyển đổi giao diện sáng/tối.
 * @param onNavigateToAdmin Callback chuyển hướng sang màn hình Quản trị (chỉ dành cho Admin).
 * @param onAuthAction Callback xử lý hành động xác thực (Đăng nhập).
 * @param viewModel ViewModel quản lý trạng thái xác thực.
 */
@Composable
fun ProfileScreen(
    useDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onAuthAction: () -> Unit,
    viewModel: AuthViewModel = koinViewModel()
) {
    val authState by viewModel.authState.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        ScreenHeader(
            title = "Tài khoản",
            subtitle = "Quản lý hồ sơ cá nhân và thông tin ứng dụng",
            leadingIcon = Icons.Rounded.Person,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 16.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    val isGuest = authState is AuthState.Success && (authState as AuthState.Success).isGuest
                    Icon(
                        imageVector = if (isGuest) Icons.Rounded.PersonOutline else Icons.Rounded.Person,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }


                when (val state = authState) {
                    is AuthState.Success -> {
                        if (state.isGuest) {
                            Text("Tài khoản Ẩn danh", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("Bạn đang trải nghiệm dưới quyền Khách", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = onAuthAction) {
                                Text("Đăng nhập Tài khoản Chính")
                            }
                        } else {
                            val name = state.displayName?.takeIf { it.isNotBlank() } ?: "Thành viên Ứng dụng"
                            Text(name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("UID: ${state.uid}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { viewModel.signOut() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Đăng xuất")
                            }
                        }
                    }
                    else -> {
                        Text("Chưa Đăng Nhập", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Button(onClick = onAuthAction) {
                            Text("Đăng nhập / Đăng ký")
                        }
                    }
                }
            }
        }


        if (authState is AuthState.Success && (authState as AuthState.Success).isAdmin) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.fillMaxWidth(),
                onClick = onNavigateToAdmin
            ) {
                Row(
                    modifier = Modifier.padding(20.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Bảng Điều Khiển (Admin)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Quản lý người dùng & Bách khoa", style = MaterialTheme.typography.bodySmall)
                    }
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null)
                }
            }
        }


        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Thông tin Ứng dụng", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("BugScanner - Ứng dụng Phát hiện và Nhận diện Côn trùng", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Text("Phiên bản: 1.0.0", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                
                HorizontalDivider()
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Phát triển bởi", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Text("BugScanner Developers", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Cá nhân hóa", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (useDarkTheme) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Chế độ tối", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text("Giảm độ sáng, tiết kiệm pin", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Switch(
                        checked = useDarkTheme,
                        onCheckedChange = { onThemeToggle() }
                    )
                }
            }
        }
    }
}
}
