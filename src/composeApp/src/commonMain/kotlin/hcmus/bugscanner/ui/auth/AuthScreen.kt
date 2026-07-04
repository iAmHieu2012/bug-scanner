package hcmus.bugscanner.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import hcmus.bugscanner.ui.theme.AppIcon
import hcmus.bugscanner.ui.theme.IconBugscanner
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
    var isLoginMode by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var validationMessage by remember { mutableStateOf<String?>(null) }
    var isPasswordVisible by remember { mutableStateOf(false) }

    val authState by authViewModel.authState.collectAsState()
    val isWideScreen = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded

    fun submitAuth() {
        val validation = AuthValidation.validate(email, password)
        if (validation != null) {
            validationMessage = validation.message
            return
        }

        validationMessage = null
        if (isLoginMode) authViewModel.signInWithEmail(email.trim(), password)
        else authViewModel.signUpWithEmail(email.trim(), password, displayName.trim())
    }

    fun updateEmail(value: String) {
        email = value
        validationMessage = null
    }

    fun updatePassword(value: String) {
        password = value
        validationMessage = null
    }

    if (isWideScreen) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(24.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        imageVector = AppIcon.IconBugscanner,
                        contentDescription = "App Logo Large",
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

            Box(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                AuthForm(
                    isLoginMode = isLoginMode,
                    email = email,
                    password = password,
                    displayName = displayName,
                    authState = authState,
                    validationMessage = validationMessage,
                    isPasswordVisible = isPasswordVisible,
                    onEmailChange = ::updateEmail,
                    onPasswordChange = ::updatePassword,
                    onDisplayNameChange = { displayName = it },
                    onPasswordVisibilityToggle = { isPasswordVisible = !isPasswordVisible },
                    onToggleMode = {
                        validationMessage = null
                        isLoginMode = !isLoginMode
                    },
                    onActionClick = ::submitAuth,
                    onGuestClick = { authViewModel.signInAnonymously() }
                )
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(top = 24.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AuthForm(
                    isLoginMode = isLoginMode,
                    email = email,
                    password = password,
                    displayName = displayName,
                    authState = authState,
                    validationMessage = validationMessage,
                    isPasswordVisible = isPasswordVisible,
                    onEmailChange = ::updateEmail,
                    onPasswordChange = ::updatePassword,
                    onDisplayNameChange = { displayName = it },
                    onPasswordVisibilityToggle = { isPasswordVisible = !isPasswordVisible },
                    onToggleMode = {
                        validationMessage = null
                        isLoginMode = !isLoginMode
                    },
                    onActionClick = ::submitAuth,
                    onGuestClick = { authViewModel.signInAnonymously() }
                )
            }
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
 * @param displayName Giá trị text hiện tại của trường nhập Tên hiển thị (chỉ dùng khi Đăng ký).
 * @param authState Trạng thái xử lý mạng hiện tại để hiển thị Loading hoặc Lỗi từ Firebase.
 * @param validationMessage Thông báo lỗi kiểm tra định dạng tại local.
 * @param isPasswordVisible Trạng thái ẩn/hiện mật khẩu.
 * @param onEmailChange Callback khi người dùng gõ vào trường Email.
 * @param onPasswordChange Callback khi người dùng gõ vào trường Mật khẩu.
 * @param onDisplayNameChange Callback khi người dùng gõ vào trường Tên hiển thị.
 * @param onPasswordVisibilityToggle Callback khi thay đổi ẩn/hiện mật khẩu.
 * @param onToggleMode Callback chuyển đổi qua lại giữa chế độ Đăng nhập và Đăng ký.
 * @param onActionClick Callback kích hoạt hành động gọi API đăng nhập/đăng ký.
 * @param onGuestClick Callback kích hoạt hành động đăng nhập dưới quyền Khách (Ẩn danh).
 */
@Composable
private fun AuthForm(
    isLoginMode: Boolean,
    email: String,
    password: String,
    displayName: String,
    authState: AuthState,
    validationMessage: String?,
    isPasswordVisible: Boolean,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onPasswordVisibilityToggle: () -> Unit,
    onToggleMode: () -> Unit,
    onActionClick: () -> Unit,
    onGuestClick: () -> Unit
) {
    val errorMessage = validationMessage ?: (authState as? AuthState.Error)?.message

    Column(
        modifier = Modifier
            .padding(28.dp)
            .widthIn(max = 400.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 6.dp
        ) {
            Image(
                imageVector = AppIcon.IconBugscanner,
                contentDescription = "Form Logo",
                modifier = Modifier.padding(16.dp).size(64.dp)
            )
        }

        Spacer(modifier = Modifier.height(22.dp))

        Text(
            text = if (isLoginMode) "Chào mừng trở lại" else "Tạo tài khoản BugScanner",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Khám phá thế giới côn trùng ngay hôm nay",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(28.dp))

        if (errorMessage != null) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                )
            }
        }

        if (!isLoginMode) {
            OutlinedTextField(
                value = displayName,
                onValueChange = onDisplayNameChange,
                label = { Text("Tên hiển thị") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Email") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            isError = validationMessage?.contains("Email") == true || validationMessage?.contains("email") == true
        )

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Mật khẩu") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = onPasswordVisibilityToggle) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (isPasswordVisible) "Ẩn mật khẩu" else "Hiện mật khẩu"
                    )
                }
            },
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            isError = validationMessage?.contains("Mật khẩu") == true
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onActionClick,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
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

        Spacer(modifier = Modifier.height(14.dp))

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
