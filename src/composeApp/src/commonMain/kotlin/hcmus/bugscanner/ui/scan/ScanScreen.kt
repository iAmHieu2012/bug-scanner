package hcmus.bugscanner.ui.scan

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import bugscanner.composeapp.generated.resources.*
import hcmus.bugscanner.domain.model.FrameResult
import hcmus.bugscanner.ui.scan.components.DetectionPanel
import org.jetbrains.compose.resources.stringResource

/**
 * Màn hình trung tâm xử lý chức năng quét và nhận diện AI.
 * Hỗ trợ tự động điều chỉnh bố cục (Adaptive Layout) thông qua [BoxWithConstraints]
 * mà không cần phụ thuộc vào WindowSizeClass từ bên ngoài.
 *
 * @param isLoggedIn Trạng thái xác thực hiện tại để hiển thị nút đăng nhập/đăng xuất.
 * @param onAuthAction Callback xử lý khi người dùng nhấn nút xác thực.
 * @param onDetectedBugClick Callback chuyển hướng sang màn hình Chi tiết khi nhấn vào một kết quả, truyền kèm tên và mảng byte của ảnh để lưu trữ.
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
 * * @param isLoggedIn Trạng thái đăng nhập.
 * @param onAuthAction Callback xử lý nhấn nút đăng nhập/đăng xuất.
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
 * Component lõi chuyên trách việc render giao diện của Nền tảng (Native View).
 * Điều hướng giữa màn hình Camera trực tiếp và màn hình phân tích ảnh tĩnh.
 * * @param currentMode Chế độ quét hiện tại.
 * @param currentImageId Định danh ảnh tĩnh hiện tại.
 * @param frameResult Kết quả nhận diện.
 * @param platformProvider Provider cung cấp giao diện native.
 * @param onResultUpdate Callback cập nhật kết quả.
 * @param onLiveFrameCaptured Callback trả về ảnh từ luồng trực tiếp.
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
}

/**
 * Thanh menu thao tác nhanh cho phép đổi chế độ quét (Camera Live / Tải ảnh lên / Chụp ảnh mới).
 * * @param currentMode Chế độ quét hiện tại.
 * @param pickerHelper Helper xử lý chọn/chụp ảnh.
 * @param onModeChange Callback khi thay đổi chế độ.
 * @param onClearResult Callback xóa kết quả nhận diện cũ.
 * @param alignmentModifier Vị trí đặt menu trên màn hình.
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
                        ScanMode.IMAGE_UPLOAD -> pickerHelper.launchGallery()
                        ScanMode.CAMERA_CAPTURE -> pickerHelper.launchCamera()
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

/**
 * Lớp phủ đồ họa (Overlay) vẽ 4 góc viền ngắm (Viewfinder) đặc trưng của các ứng dụng Scanner.
 */
@Composable
fun ScannerOverlay() {
    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize().padding(60.dp)) {
            val strokeWidth = 10f
            val cornerLength = 60f
            val color = Color.White.copy(alpha = 0.8f)

            drawLine(color, Offset(0f, 0f), Offset(cornerLength, 0f), strokeWidth)
            drawLine(color, Offset(0f, 0f), Offset(0f, cornerLength), strokeWidth)
            drawLine(color, Offset(size.width, 0f), Offset(size.width - cornerLength, 0f), strokeWidth)
            drawLine(color, Offset(size.width, 0f), Offset(size.width, cornerLength), strokeWidth)
            drawLine(color, Offset(0f, size.height), Offset(cornerLength, size.height), strokeWidth)
            drawLine(color, Offset(0f, size.height), Offset(0f, size.height - cornerLength), strokeWidth)
            drawLine(color, Offset(size.width, size.height), Offset(size.width - cornerLength, size.height), strokeWidth)
            drawLine(color, Offset(size.width, size.height), Offset(size.width, size.height - cornerLength), strokeWidth)
        }
    }
}