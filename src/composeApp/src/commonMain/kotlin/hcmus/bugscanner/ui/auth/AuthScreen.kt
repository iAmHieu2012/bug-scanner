package hcmus.bugscanner.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel

/**
 * Màn hình xác thực người dùng (Đăng nhập / Đăng ký).
 * Áp dụng mô hình Responsive Layout tự động điều chỉnh dựa trên [WindowSizeClass]:
 * - Màn hình hẹp (Mobile): Hiển thị Form canh giữa toàn màn hình.
 * - Màn hình rộng (Tablet ngang, Web, Desktop): Hiển thị giao diện Split-Screen (Banner minh họa bên trái, Form nhập liệu bên phải).
 *
 * @param windowSizeClass Dữ liệu phân loại kích thước màn hình hiện tại do App Navigation truyền xuống.
 * @param authViewModel ViewModel quản lý logic gọi API xác thực Firebase.
 */
@Composable
fun AuthScreen(
    windowSizeClass: WindowSizeClass,
    authViewModel: AuthViewModel = koinViewModel()
) {
    // Quản lý trạng thái nhập liệu nội bộ (Local State)
    var isLoginMode by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Lắng nghe trạng thái luồng xử lý từ ViewModel
    val authState by authViewModel.authState.collectAsState()

    // Xác định ngưỡng kích thước màn hình. Expanded thường ứng với chiều rộng >= 840dp.
    val isWideScreen = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded

    if (isWideScreen) {
        // GIAO DIỆN MÀN HÌNH RỘNG (SPLIT-SCREEN)
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Nửa bên trái: Banner minh họa ứng dụng
            // Sử dụng weight(1f) để chia đều chính xác 50% chiều rộng màn hình.
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(24.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.BugReport,
                        contentDescription = "App Logo Large",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(120.dp).padding(bottom = 24.dp)
                    )
                    Text(
                        text = "BugScanner",
                        style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Bách khoa toàn thư côn trùng trong tầm tay bạn.",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            // Nửa bên phải: Khu vực chứa Form đăng nhập
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                AuthForm(
                    isLoginMode = isLoginMode,
                    email = email,
                    password = password,
                    authState = authState,
                    onEmailChange = { email = it },
                    onPasswordChange = { password = it },
                    onToggleMode = { isLoginMode = !isLoginMode },
                    onActionClick = {
                        if (isLoginMode) authViewModel.signInWithEmail(email, password)
                        else authViewModel.signUpWithEmail(email, password)
                    },
                    onGuestClick = { authViewModel.signInAnonymously() }
                )
            }
        }
    } else {
        // GIAO DIỆN MÀN HÌNH HẸP (MOBILE)
        // Hiển thị khung Form ở chính giữa, background phủ toàn màn hình.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            AuthForm(
                isLoginMode = isLoginMode,
                email = email,
                password = password,
                authState = authState,
                onEmailChange = { email = it },
                onPasswordChange = { password = it },
                onToggleMode = { isLoginMode = !isLoginMode },
                onActionClick = {
                    if (isLoginMode) authViewModel.signInWithEmail(email, password)
                    else authViewModel.signUpWithEmail(email, password)
                },
                onGuestClick = { authViewModel.signInAnonymously() }
            )
        }
    }
}

/**
 * Component tái sử dụng chứa toàn bộ trường nhập liệu và nút bấm tương tác của chức năng Xác thực.
 * Tách biệt UI Form giúp loại bỏ code lặp giữa 2 layout màn hình (Ngang/Dọc).
 *
 * @param isLoginMode Cờ xác định form đang ở chế độ Đăng nhập (true) hay Đăng ký (false).
 * @param email Giá trị text hiện tại của trường nhập Email.
 * @param password Giá trị text hiện tại của trường nhập Mật khẩu.
 * @param authState Trạng thái xử lý mạng hiện tại để hiển thị Loading hoặc Lỗi.
 * @param onEmailChange Callback khi người dùng gõ vào trường Email.
 * @param onPasswordChange Callback khi người dùng gõ vào trường Mật khẩu.
 * @param onToggleMode Callback chuyển đổi qua lại giữa chế độ Đăng nhập và Đăng ký.
 * @param onActionClick Callback kích hoạt hành động gọi API đăng nhập/đăng ký.
 * @param onGuestClick Callback kích hoạt hành động đăng nhập dưới quyền Khách (Ẩn danh).
 */
@Composable
private fun AuthForm(
    isLoginMode: Boolean,
    email: String,
    password: String,
    authState: AuthState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onToggleMode: () -> Unit,
    onActionClick: () -> Unit,
    onGuestClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(32.dp)
            // Giới hạn chiều rộng tối đa (400dp) tránh form bị dãn quá mức trên Tablet dọc (Medium Size)
            .widthIn(max = 400.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Icon(
                imageVector = Icons.Rounded.BugReport,
                contentDescription = "Form Logo",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(16.dp).size(64.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Tiêu đề động tùy theo chế độ Đăng nhập hay Đăng ký
        Text(
            text = if (isLoginMode) "Chào mừng trở lại!" else "Tạo tài khoản BugScanner",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Khám phá thế giới côn trùng ngay hôm nay",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Khối hiển thị thông báo lỗi từ ViewModel
        if (authState is AuthState.Error) {
            Text(
                text = authState.message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Email") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Mật khẩu") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Nút thực thi tác vụ chính
        Button(
            onClick = onActionClick,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            // Khóa nút khi mạng đang xử lý để chống Spam Request
            enabled = authState !is AuthState.Loading
        ) {
            if (authState is AuthState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(
                    text = if (isLoginMode) "Đăng nhập" else "Đăng ký",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Các nút chuyển đổi chế độ và bỏ qua xác thực
        TextButton(onClick = onToggleMode) {
            Text(
                text = if (isLoginMode) "Chưa có tài khoản? Đăng ký ngay" else "Đã có tài khoản? Đăng nhập",
                color = MaterialTheme.colorScheme.primary
            )
        }

        TextButton(
            onClick = onGuestClick,
            enabled = authState !is AuthState.Loading
        ) {
            Text("Tiếp tục mà không cần đăng nhập", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}