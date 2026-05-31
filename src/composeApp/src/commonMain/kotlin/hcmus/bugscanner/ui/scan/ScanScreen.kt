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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import bugscanner.composeapp.generated.resources.*
import hcmus.bugscanner.domain.model.FrameResult
import hcmus.bugscanner.ui.scan.components.DetectionPanel
import hcmus.bugscanner.ui.scan.components.ScannerOverlay
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Màn hình trung tâm xử lý toàn bộ chức năng quét và nhận diện AI của ứng dụng.
 * Quản lý vòng đời của Camera, điều phối dữ liệu giữa UI, mô hình YOLO Offline và API iNaturalist.
 * Hỗ trợ tự động điều chỉnh bố cục (Adaptive Layout) thông qua [BoxWithConstraints].
 *
 * @param isLoggedIn Trạng thái xác thực hiện tại để hiển thị nút đăng nhập/đăng xuất.
 * @param onAuthAction Callback xử lý khi người dùng nhấn nút xác thực.
 * @param onDetectedBugClick Callback chuyển hướng sang màn hình Chi tiết khi nhấn vào một kết quả.
 * @param fallbackViewModel ViewModel quản lý luồng gọi mạng dự phòng để phân tích AI chuyên sâu.
 */
@Composable
fun ScanScreen(
    isLoggedIn: Boolean,
    onAuthAction: () -> Unit,
    onDetectedBugClick: (String, ByteArray?) -> Unit,
    fallbackViewModel: ScanFallbackViewModel = koinViewModel()
) {
    val platformProvider = LocalPlatformScanProvider.current

    // Quản lý trạng thái dữ liệu hình ảnh và kết quả nhận diện
    var currentMode by remember { mutableStateOf(ScanMode.LIVE) }
    var frameResult by remember { mutableStateOf<FrameResult?>(null) }
    var currentImageId by remember { mutableStateOf<String?>(null) }
    var capturedImageBytes by remember { mutableStateOf<ByteArray?>(null) }

    // Quản lý trạng thái luồng Camera và cờ kích hoạt lệnh chụp ảnh ngầm
    var isScanningLive by remember { mutableStateOf(true) }
    var captureTrigger by remember { mutableLongStateOf(0L) }

    val isAnalyzingFallback by fallbackViewModel.isAnalyzing.collectAsState()

    // Trình hỗ trợ mở thư viện ảnh hệ thống và cập nhật trạng thái UI
    val pickerHelper = platformProvider.rememberImagePickerHelper(
        onModeChange = {
            currentMode = it
            isScanningLive = false
        },
        onResult = { frameResult = it },
        onImageIdCaptured = { currentImageId = it },
        onImageBytesCaptured = { capturedImageBytes = it }
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (maxWidth > 800.dp) {
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
                            capturedImageBytes = capturedImageBytes,
                            frameResult = frameResult,
                            isScanningLive = isScanningLive,
                            captureTrigger = captureTrigger,
                            platformProvider = platformProvider,
                            onResultUpdate = { frameResult = it },
                            onFrameCaptured = { bytes ->
                                capturedImageBytes = bytes
                                isScanningLive = false // Đóng băng luồng UI ngay khi nhận được ảnh chụp từ Native
                            }
                        )

                        ScanControlButtons(
                            currentMode = currentMode,
                            isScanningLive = isScanningLive,
                            onToggleLive = {
                                // Nếu đang Live: Kích hoạt cò súng (trigger) để Native Camera bắt đầu trích xuất frame.
                                // Nếu đang Freeze: Mở lại luồng Live Camera bình thường.
                                if (isScanningLive) captureTrigger++ else isScanningLive = true
                            },
                            pickerHelper = pickerHelper,
                            onModeChange = { currentMode = it },
                            onClearResult = {
                                frameResult = null
                                capturedImageBytes = null
                                captureTrigger = 0L // Đặt lại cò súng về 0 để ngăn lỗi tự động chụp khi Camera khởi động lại
                            },
                            alignmentModifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }

                Box(modifier = Modifier.weight(0.3f).fillMaxHeight()) {
                    DetectionPanel(
                        currentMode = currentMode,
                        isScanningLive = isScanningLive,
                        frameResult = frameResult,
                        imageBytesToSave = capturedImageBytes,
                        isAnalyzingFallback = isAnalyzingFallback,
                        onFallbackClick = {
                            capturedImageBytes?.let { bytes ->
                                fallbackViewModel.analyzeFallbackImage(bytes) { aiName ->
                                    if (aiName != null) {
                                        onDetectedBugClick(aiName, bytes)
                                    }
                                }
                            }
                        },
                        onBugClick = onDetectedBugClick,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        } else {
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
                        capturedImageBytes = capturedImageBytes,
                        frameResult = frameResult,
                        isScanningLive = isScanningLive,
                        captureTrigger = captureTrigger,
                        platformProvider = platformProvider,
                        onResultUpdate = { frameResult = it },
                        onFrameCaptured = { bytes ->
                            capturedImageBytes = bytes
                            isScanningLive = false // Đóng băng luồng UI ngay khi nhận được ảnh chụp từ Native
                        }
                    )

                    ScanControlButtons(
                        currentMode = currentMode,
                        isScanningLive = isScanningLive,
                        onToggleLive = {
                            // Nếu đang Live: Kích hoạt cò súng (trigger) để Native Camera bắt đầu trích xuất frame.
                            // Nếu đang Freeze: Mở lại luồng Live Camera bình thường.
                            if (isScanningLive) captureTrigger++ else isScanningLive = true
                        },
                        pickerHelper = pickerHelper,
                        onModeChange = { currentMode = it },
                        onClearResult = {
                            frameResult = null
                            capturedImageBytes = null
                            captureTrigger = 0L // Đặt lại cò súng về 0 để ngăn lỗi tự động chụp khi Camera khởi động lại
                        },
                        alignmentModifier = Modifier.align(Alignment.BottomCenter)
                    )
                }

                DetectionPanel(
                    currentMode = currentMode,
                    isScanningLive = isScanningLive,
                    frameResult = frameResult,
                    imageBytesToSave = capturedImageBytes,
                    isAnalyzingFallback = isAnalyzingFallback,
                    onFallbackClick = {
                        capturedImageBytes?.let { bytes ->
                            fallbackViewModel.analyzeFallbackImage(bytes) { aiName ->
                                if (aiName != null) {
                                    onDetectedBugClick(aiName, bytes)
                                }
                            }
                        }
                    },
                    onBugClick = onDetectedBugClick,
                    modifier = Modifier.fillMaxWidth().height(300.dp)
                )
            }
        }
    }
}

/**
 * Component hiển thị tiêu đề, lời chào và nút điều hướng tài khoản ở phần trên cùng của màn hình.
 *
 * @param isLoggedIn Trạng thái đăng nhập để cấu hình biểu tượng và chức năng của nút bấm.
 * @param onAuthAction Callback xử lý sự kiện đăng nhập / đăng xuất.
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
 * Component chịu trách nhiệm hiển thị luồng Camera trực tiếp hoặc ảnh tĩnh thông qua Native Provider.
 * Xử lý cơ chế đóng băng khung hình (Freeze Frame) và đồng bộ hóa kết quả AI.
 *
 * @param currentMode Chế độ quét hiện tại (LIVE hoặc tải ảnh tĩnh).
 * @param currentImageId Định danh URI của ảnh tĩnh đang được chọn từ Thư viện.
 * @param capturedImageBytes Mảng byte của khung hình bị đóng băng, ưu tiên hiển thị cao nhất.
 * @param frameResult Kết quả tọa độ phân tích AI để vẽ khung nhận diện (Bounding Box).
 * @param isScanningLive Trạng thái luồng camera để xác định việc render NativeCameraView hay NativeStaticDetectionView.
 * @param captureTrigger Cờ tín hiệu truyền xuống Native để thực thi lệnh chụp ảnh ngầm.
 * @param platformProvider Giao diện Native của nền tảng (Android/Web).
 * @param onResultUpdate Callback đồng bộ trạng thái kết quả AI lên Component cha.
 * @param onFrameCaptured Callback nhận dữ liệu hình ảnh sau khi Camera chụp ngầm thành công.
 */
@Composable
private fun ScanContent(
    currentMode: ScanMode,
    currentImageId: String?,
    capturedImageBytes: ByteArray?,
    frameResult: FrameResult?,
    isScanningLive: Boolean,
    captureTrigger: Long,
    platformProvider: PlatformScanProvider,
    onResultUpdate: (FrameResult) -> Unit,
    onFrameCaptured: (ByteArray) -> Unit
) {
    platformProvider.RequireCameraPermission(
        onGranted = {
            if (currentMode == ScanMode.LIVE) {
                if (!isScanningLive) {
                    // Xử lý luồng Đóng băng (Freeze): Ưu tiên hiển thị khung hình vừa chụp
                    if (capturedImageBytes != null) {
                        platformProvider.NativeStaticDetectionView(
                            modifier = Modifier.fillMaxSize(),
                            imageId = null,
                            imageBytes = capturedImageBytes,
                            frameResult = frameResult,
                            onResultUpdate = onResultUpdate // Chạy lại AI Re-detect trên ảnh tĩnh để đảm bảo độ chính xác
                        )
                    } else {
                        // Xử lý Race Condition: Giao diện chuyển trạng thái Đóng băng nhưng luồng Native chưa xuất xong ảnh.
                        // Hiển thị cảnh báo tạm thời thay vì văng lỗi NullPointerException.
                        Box(
                            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Rounded.ImageNotSupported, null, modifier = Modifier.size(64.dp), tint = Color.White.copy(alpha = 0.6f))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Chưa bắt kịp khung hình \ud83d\udc1b", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Hãy bấm Quét lại và đợi khung nhận diện hiện rõ", color = Color.White.copy(alpha = 0.7f))
                            }
                        }
                    }
                } else {
                    // Render luồng Camera Live liên tục
                    platformProvider.NativeCameraView(
                        modifier = Modifier.fillMaxSize(),
                        captureTrigger = captureTrigger,
                        onResult = onResultUpdate,
                        onFrameCaptured = onFrameCaptured
                    )
                    ScannerOverlay()
                }
            } else {
                // Xử lý luồng hiển thị ảnh tĩnh được chọn từ Thư viện (Gallery)
                platformProvider.NativeStaticDetectionView(
                    modifier = Modifier.fillMaxSize(),
                    imageId = currentImageId,
                    imageBytes = null,
                    frameResult = frameResult,
                    onResultUpdate = onResultUpdate
                )
            }
        },
        onDenied = { launchPermissionRequest ->
            CameraPermissionScreen(onRequestPermission = launchPermissionRequest)
        }
    )
}

/**
 * Thanh menu công cụ điều khiển chức năng quét.
 * Được thiết kế tối giản với một nút Action trung tâm phục vụ chức năng Live / Freeze.
 *
 * @param currentMode Chế độ quét hiện hành.
 * @param isScanningLive Trạng thái luồng Camera để thay đổi biểu tượng nút Action (Pause / Play).
 * @param onToggleLive Callback điều phối logic thay đổi trạng thái đóng băng hoặc tiếp tục.
 * @param pickerHelper Công cụ hỗ trợ mở thư viện ảnh hệ thống.
 * @param onModeChange Callback chuyển đổi chế độ giao diện UI.
 * @param onClearResult Callback xóa các kết quả nhận diện và dữ liệu ảnh hiện hành để làm mới luồng quét.
 * @param alignmentModifier Modifier định vị trí thanh công cụ.
 */
@Composable
private fun ScanControlButtons(
    currentMode: ScanMode,
    isScanningLive: Boolean,
    onToggleLive: () -> Unit,
    pickerHelper: ImagePickerHelper,
    onModeChange: (ScanMode) -> Unit,
    onClearResult: () -> Unit,
    alignmentModifier: Modifier = Modifier
) {
    Row(
        modifier = alignmentModifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Nút mở thư viện ảnh (Gallery)
        IconButton(
            onClick = {
                onClearResult()
                pickerHelper.launchGallery()
            },
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f))
        ) {
            Icon(Icons.Rounded.PhotoLibrary, contentDescription = "Thư viện ảnh", tint = Color.White)
        }

        // Nút Action Trung tâm (Live / Freeze)
        val isFrozenOrStatic = !isScanningLive || currentMode != ScanMode.LIVE
        Button(
            onClick = {
                if (isFrozenOrStatic) {
                    onModeChange(ScanMode.LIVE)
                    onClearResult()
                    if (!isScanningLive) onToggleLive()
                } else {
                    onToggleLive()
                }
            },
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isFrozenOrStatic) MaterialTheme.colorScheme.primary else Color.White,
                contentColor = if (isFrozenOrStatic) MaterialTheme.colorScheme.onPrimary else Color.Black
            ),
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier
                .size(56.dp)
                .border(3.dp, Color.White.copy(alpha = 0.3f), CircleShape)
        ) {
            if (isFrozenOrStatic) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = "Quét tiếp", modifier = Modifier.size(32.dp))
            } else {
                Icon(Icons.Rounded.Pause, contentDescription = "Đóng băng", modifier = Modifier.size(32.dp))
            }
        }

        // Spacer để cân bằng bố cục với nút Thư viện ảnh ở bên trái
        Spacer(modifier = Modifier.size(56.dp))
    }
}