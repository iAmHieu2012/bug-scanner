package hcmus.bugscanner.ui.scan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import bugscanner.composeapp.generated.resources.*
import hcmus.bugscanner.domain.model.FrameResult
import hcmus.bugscanner.ui.scan.components.DetectionPanel
import hcmus.bugscanner.ui.scan.components.ScannerOverlay
import org.jetbrains.compose.resources.stringResource

/**
 * Màn hình trung tâm xử lý chức năng quét và nhận diện AI.
 * Hỗ trợ tự động điều chỉnh bố cục (Adaptive Layout) thông qua [BoxWithConstraints]
 * mà không cần phụ thuộc vào WindowSizeClass từ bên ngoài.
 *
 * @param isLoggedIn Trạng thái xác thực hiện tại để hiển thị nút đăng nhập/đăng xuất.
 * @param onAuthAction Callback xử lý khi người dùng nhấn nút xác thực (điều hướng sang màn hình Login/Logout).
 * @param onDetectedBugClick Callback chuyển hướng sang màn hình Chi tiết khi nhấn vào một kết quả, truyền kèm tên sinh vật và mảng byte của ảnh để lưu trữ lịch sử.
 */
@Composable
fun ScanScreen(
    isLoggedIn: Boolean,
    onAuthAction: () -> Unit,
    onDetectedBugClick: (String, ByteArray?) -> Unit
) {
    val platformProvider = LocalPlatformScanProvider.current

    var currentMode by remember { mutableStateOf(ScanMode.LIVE) }
    var frameResult by remember { mutableStateOf<FrameResult?>(null) }
    var currentImageId by remember { mutableStateOf<String?>(null) }
    var capturedImageBytes by remember { mutableStateOf<ByteArray?>(null) }

    val pickerHelper = platformProvider.rememberImagePickerHelper(
        onModeChange = { currentMode = it },
        onResult = { frameResult = it },
        onImageIdCaptured = { currentImageId = it },
        onImageBytesCaptured = { capturedImageBytes = it }
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (maxWidth > 800.dp) {
            // GIAO DIỆN MÀN HÌNH RỘNG
            Row(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(0.7f).fillMaxHeight()) {
                    ScanScreenHeader(isLoggedIn, onAuthAction)
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(32.dp))
                            .border(2.dp, Color.White, RoundedCornerShape(32.dp))
                            .background(Color.Black)
                    ) {
                        ScanContent(
                            currentMode = currentMode,
                            currentImageId = currentImageId,
                            frameResult = frameResult,
                            platformProvider = platformProvider,
                            onResultUpdate = { frameResult = it },
                            onLiveFrameCaptured = { capturedImageBytes = it }
                        )

                        ScanControlButtons(
                            currentMode = currentMode,
                            pickerHelper = pickerHelper,
                            onModeChange = { currentMode = it },
                            onClearResult = { frameResult = null },
                            alignmentModifier = Modifier.align(Alignment.BottomStart)
                        )
                    }
                }

                Box(modifier = Modifier.weight(0.3f).fillMaxHeight()) {
                    DetectionPanel(
                        frameResult = frameResult,
                        imageBytesToSave = capturedImageBytes,
                        onBugClick = onDetectedBugClick,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        } else {
            // GIAO DIỆN MÀN HÌNH HẸP
            Column(modifier = Modifier.fillMaxSize()) {
                ScanScreenHeader(isLoggedIn, onAuthAction)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .border(2.dp, Color.White, RoundedCornerShape(32.dp))
                        .background(Color.Black)
                ) {
                    ScanContent(
                        currentMode = currentMode,
                        currentImageId = currentImageId,
                        frameResult = frameResult,
                        platformProvider = platformProvider,
                        onResultUpdate = { frameResult = it },
                        onLiveFrameCaptured = { capturedImageBytes = it }
                    )

                    ScanControlButtons(
                        currentMode = currentMode,
                        pickerHelper = pickerHelper,
                        onModeChange = { currentMode = it },
                        onClearResult = { frameResult = null },
                        alignmentModifier = Modifier.align(Alignment.BottomCenter)
                    )
                }

                DetectionPanel(
                    frameResult = frameResult,
                    imageBytesToSave = capturedImageBytes,
                    onBugClick = onDetectedBugClick,
                    modifier = Modifier.fillMaxWidth().height(300.dp)
                )
            }
        }
    }
}

/**
 * Component hiển thị lời chào và nút điều hướng tài khoản trên đầu màn hình.
 *
 * @param isLoggedIn Trạng thái đăng nhập của người dùng (dùng để đổi icon Login/Logout).
 * @param onAuthAction Callback xử lý sự kiện khi người dùng nhấn vào nút Account.
 */
@Composable
private fun ScanScreenHeader(isLoggedIn: Boolean, onAuthAction: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = stringResource(Res.string.scan_greeting_msg),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(Res.string.scan_what_to_find),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        IconButton(
            onClick = onAuthAction,
            modifier = Modifier.background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
        ) {
            Icon(
                imageVector = if (isLoggedIn) Icons.AutoMirrored.Rounded.Logout else Icons.AutoMirrored.Rounded.Login,
                contentDescription = if (isLoggedIn) stringResource(Res.string.action_logout) else stringResource(Res.string.action_login),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

/**
 * Component lõi chuyên trách việc render giao diện của Nền tảng (Native View) và kiểm tra Quyền Camera.
 * Điều hướng giữa màn hình Camera trực tiếp và màn hình phân tích ảnh tĩnh.
 *
 * @param currentMode Chế độ quét hiện tại (LIVE, IMAGE_UPLOAD, CAMERA_CAPTURE).
 * @param currentImageId Định danh (URI/Blob) của ảnh tĩnh hiện tại đang được chọn để phân tích.
 * @param frameResult Kết quả nhận diện trả về từ AI Model.
 * @param platformProvider Provider cung cấp giao diện native (CameraX cho Android hoặc Video Element cho Web).
 * @param onResultUpdate Callback cập nhật kết quả nhận diện lên State của luồng chính.
 * @param onLiveFrameCaptured Callback trả về mảng byte của khung hình được chụp từ luồng trực tiếp.
 */
@Composable
private fun ScanContent(
    currentMode: ScanMode,
    currentImageId: String?,
    frameResult: FrameResult?,
    platformProvider: PlatformScanProvider,
    onResultUpdate: (FrameResult) -> Unit,
    onLiveFrameCaptured: (ByteArray?) -> Unit
) {
    // Gọi hàm quản lý quyền đa nền tảng thông qua Interface Provider
    platformProvider.RequireCameraPermission(
        onGranted = {
            // NẾU ĐÃ CÓ QUYỀN (Android) HOẶC ĐANG Ở TRÊN WEB
            if (currentMode == ScanMode.LIVE) {
                platformProvider.NativeCameraView(
                    modifier = Modifier.fillMaxSize(),
                    onResult = onResultUpdate,
                    onLiveFrameCaptured = onLiveFrameCaptured
                )
                ScannerOverlay()
            } else {
                platformProvider.NativeStaticDetectionView(
                    modifier = Modifier.fillMaxSize(),
                    imageId = currentImageId,
                    frameResult = frameResult
                )
            }
        },
        onDenied = { launchPermissionRequest ->
            // NẾU CHƯA CÓ QUYỀN (Android) -> HIỂN THỊ UI XIN QUYỀN
            CameraPermissionScreen(
                onRequestPermission = launchPermissionRequest
            )
        }
    )
}

/**
 * Thanh menu thao tác nhanh cho phép đổi chế độ quét (Camera Live / Tải ảnh lên / Chụp ảnh mới).
 * * @param currentMode Chế độ quét hiện tại để làm nổi bật Icon tương ứng.
 * @param pickerHelper Helper chịu trách nhiệm mở thư viện ảnh hoặc ứng dụng máy ảnh gốc của nền tảng.
 * @param onModeChange Callback kích hoạt khi người dùng thay đổi chế độ quét.
 * @param onClearResult Callback xóa kết quả nhận diện cũ trên màn hình khi chuyển chế độ.
 * @param alignmentModifier Modifier dùng để định vị trí đặt menu trên màn hình (vd: BottomStart, BottomCenter).
 */
@Composable
private fun ScanControlButtons(
    currentMode: ScanMode,
    pickerHelper: ImagePickerHelper,
    onModeChange: (ScanMode) -> Unit,
    onClearResult: () -> Unit,
    alignmentModifier: Modifier = Modifier
) {
    val quickModes: List<Pair<ScanMode, ImageVector>> = listOf(
        ScanMode.LIVE to Icons.Rounded.Videocam,
        ScanMode.IMAGE_UPLOAD to Icons.Rounded.PhotoLibrary,
        ScanMode.CAMERA_CAPTURE to Icons.Rounded.Camera
    )

    Row(
        modifier = alignmentModifier
            .padding(16.dp)
            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
            .padding(4.dp)
    ) {
        quickModes.forEach { (mode, icon) ->
            IconButton(
                onClick = {
                    when (mode) {
                        ScanMode.LIVE -> {
                            onModeChange(mode)
                            onClearResult()
                        }
                        ScanMode.IMAGE_UPLOAD -> {
                            onClearResult()
                            pickerHelper.launchGallery()
                        }
                        ScanMode.CAMERA_CAPTURE -> {
                            onClearResult()
                            pickerHelper.launchCamera()
                        }
                    }
                },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (currentMode == mode) MaterialTheme.colorScheme.primary else Color.Transparent)
            ) {
                Icon(icon, null, tint = if (currentMode == mode) MaterialTheme.colorScheme.onPrimary else Color.White)
            }
        }
    }
}