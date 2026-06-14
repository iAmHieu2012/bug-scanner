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
import hcmus.bugscanner.domain.model.BugInfo
import hcmus.bugscanner.domain.model.DetectedBugSnapshot
import hcmus.bugscanner.domain.model.FrameResult
import hcmus.bugscanner.domain.model.ScanSource
import hcmus.bugscanner.ui.layout.AdaptiveLayoutSize
import hcmus.bugscanner.ui.layout.classifyAdaptiveWidth
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
 * @param onDetectedBugClick Callback chuyển hướng sang màn hình Chi tiết khi nhận diện thành công (kèm dữ liệu sinh học và mảng byte hình ảnh).
 * @param fallbackViewModel ViewModel quản lý luồng gọi mạng dự phòng để phân tích AI chuyên sâu.
 */
@Composable
fun ScanScreen(
    isLoggedIn: Boolean,
    onAuthAction: () -> Unit,
    onDetectedBugClick: (DetectedBugSnapshot) -> Unit,
    fallbackViewModel: ScanFallbackViewModel = koinViewModel()
) {
    val platformProvider = LocalPlatformScanProvider.current

    var currentMode by remember { mutableStateOf(ScanMode.LIVE) }
    var frameResult by remember { mutableStateOf<FrameResult?>(null) }
    var currentImageId by remember { mutableStateOf<String?>(null) }
    var capturedImageBytes by remember { mutableStateOf<ByteArray?>(null) }

    var isScanningLive by remember { mutableStateOf(true) }
    var captureTrigger by remember { mutableLongStateOf(0L) }
    var cameraSessionKey by remember { mutableLongStateOf(0L) }
    var runtimeStatus by remember { mutableStateOf<ScanRuntimeStatus>(ScanRuntimeStatus.Idle) }

    val isAnalyzingFallback by fallbackViewModel.isAnalyzing.collectAsState()
    val fallbackErrorMessage by fallbackViewModel.errorMessage.collectAsState()
    val scanEvent by fallbackViewModel.scanEvent.collectAsState()

    LaunchedEffect(scanEvent) {
        scanEvent?.let {
            onDetectedBugClick(it)
            fallbackViewModel.clearScanEvent()
        }
    }

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
        if (classifyAdaptiveWidth(maxWidth.value) == AdaptiveLayoutSize.EXPANDED) {
            Row(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
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
                            runtimeStatus = runtimeStatus,
                            cameraSessionKey = cameraSessionKey,
                            onRuntimeStatus = { runtimeStatus = it },
                            onRetry = { cameraSessionKey++ },
                            onResultUpdate = { frameResult = it },
                            onFrameCaptured = { bytes ->
                                capturedImageBytes = bytes
                                isScanningLive = false
                            }
                        ) {
                            ScanControlButtons(
                                currentMode = currentMode,
                                isScanningLive = isScanningLive,
                                onToggleLive = {
                                    if (isScanningLive) captureTrigger++ else isScanningLive = true
                                },
                                pickerHelper = pickerHelper,
                                onModeChange = { currentMode = it },
                                onClearResult = {
                                    frameResult = null
                                    capturedImageBytes = null
                                    captureTrigger = 0L
                                },
                                alignmentModifier = Modifier.align(Alignment.BottomCenter)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.widthIn(min = 320.dp, max = 440.dp).fillMaxHeight()) {
                    DetectionPanel(
                        currentMode = currentMode,
                        isScanningLive = isScanningLive,
                        frameResult = frameResult,
                        imageBytesToSave = capturedImageBytes,
                        isAnalyzingFallback = isAnalyzingFallback,
                        fallbackErrorMessage = fallbackErrorMessage,
                        onFallbackClick = {
                            capturedImageBytes?.let { bytes ->
                                fallbackViewModel.analyzeFallbackImage(bytes)
                            }
                        },
                        onBugClick = { className, displayName, confidence, bytes ->
                            fallbackViewModel.handleYoloDetection(className, displayName, confidence, bytes)
                        },
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
                        .padding(horizontal = 16.dp)
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
                        runtimeStatus = runtimeStatus,
                        cameraSessionKey = cameraSessionKey,
                        onRuntimeStatus = { runtimeStatus = it },
                        onRetry = { cameraSessionKey++ },
                        onResultUpdate = { frameResult = it },
                        onFrameCaptured = { bytes ->
                            capturedImageBytes = bytes
                            isScanningLive = false
                        }
                    ) {
                        ScanControlButtons(
                            currentMode = currentMode,
                            isScanningLive = isScanningLive,
                            onToggleLive = {
                                if (isScanningLive) captureTrigger++ else isScanningLive = true
                            },
                            pickerHelper = pickerHelper,
                            onModeChange = { currentMode = it },
                            onClearResult = {
                                frameResult = null
                                capturedImageBytes = null
                                captureTrigger = 0L
                            },
                            alignmentModifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }

                DetectionPanel(
                    currentMode = currentMode,
                    isScanningLive = isScanningLive,
                    frameResult = frameResult,
                    imageBytesToSave = capturedImageBytes,
                    isAnalyzingFallback = isAnalyzingFallback,
                    fallbackErrorMessage = fallbackErrorMessage,
                    onFallbackClick = {
                        capturedImageBytes?.let { bytes ->
                            fallbackViewModel.analyzeFallbackImage(bytes)
                        }
                    },
                    onBugClick = { className, displayName, confidence, bytes ->
                        fallbackViewModel.handleYoloDetection(className, displayName, confidence, bytes)
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 188.dp, max = 260.dp)
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
 * Xử lý cơ chế đóng băng khung hình (Freeze Frame), đồng bộ hóa kết quả AI và điều khiển hiển thị cụm nút chức năng.
 *
 * @param currentMode Chế độ quét hiện hành.
 * @param currentImageId Định danh URI của ảnh tĩnh đang được chọn từ Thư viện.
 * @param capturedImageBytes Mảng byte của khung hình bị đóng băng, ưu tiên hiển thị cao nhất.
 * @param frameResult Kết quả tọa độ phân tích AI để vẽ khung nhận diện (Bounding Box).
 * @param isScanningLive Trạng thái luồng camera để xác định việc render NativeCameraView hay NativeStaticDetectionView.
 * @param captureTrigger Cờ tín hiệu truyền xuống Native để thực thi lệnh chụp ảnh ngầm.
 * @param platformProvider Giao diện Native của nền tảng (Android/Web).
 * @param runtimeStatus Trạng thái khởi chạy thời gian thực (ví dụ: đang tải model, lỗi camera).
 * @param cameraSessionKey Khóa phiên camera dùng để làm mới/khởi chạy lại camera view khi cần thiết.
 * @param onRuntimeStatus Callback đồng bộ trạng thái runtime lên component cha.
 * @param onRetry Callback thử lại khi xảy ra lỗi camera hoặc model.
 * @param onResultUpdate Callback đồng bộ trạng thái kết quả AI lên Component cha.
 * @param onFrameCaptured Callback nhận dữ liệu hình ảnh sau khi Camera chụp ngầm thành công.
 * @param controls Composable hiển thị thanh điều khiển (chụp/quét/refresh) nằm đè lên khung camera.
 */
@Composable
private fun BoxScope.ScanContent(
    currentMode: ScanMode,
    currentImageId: String?,
    capturedImageBytes: ByteArray?,
    frameResult: FrameResult?,
    isScanningLive: Boolean,
    captureTrigger: Long,
    platformProvider: PlatformScanProvider,
    runtimeStatus: ScanRuntimeStatus,
    cameraSessionKey: Long,
    onRuntimeStatus: (ScanRuntimeStatus) -> Unit,
    onRetry: () -> Unit,
    onResultUpdate: (FrameResult) -> Unit,
    onFrameCaptured: (ByteArray) -> Unit,
    controls: @Composable BoxScope.() -> Unit
) {
    platformProvider.RequireCameraPermission(
        onGranted = {
            Box(modifier = Modifier.fillMaxSize()) {
                if (currentMode == ScanMode.LIVE) {
                    if (!isScanningLive) {
                        if (capturedImageBytes != null) {
                            platformProvider.NativeStaticDetectionView(
                                modifier = Modifier.fillMaxSize(),
                                imageId = null,
                                imageBytes = capturedImageBytes,
                                frameResult = frameResult,
                                onResultUpdate = onResultUpdate,
                                onRuntimeStatus = onRuntimeStatus
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Rounded.ImageNotSupported,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = Color.White.copy(alpha = 0.6f)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Chưa bắt kịp khung hình",
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Hãy bấm Quét lại và đợi khung nhận diện hiện rõ",
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    } else {
                        key(cameraSessionKey) {
                            platformProvider.NativeCameraView(
                                modifier = Modifier.fillMaxSize(),
                                captureTrigger = captureTrigger,
                                onResult = onResultUpdate,
                                onFrameCaptured = onFrameCaptured,
                                onRuntimeStatus = onRuntimeStatus
                            )
                        }
                        ScannerOverlay()
                    }
                } else {
                    platformProvider.NativeStaticDetectionView(
                        modifier = Modifier.fillMaxSize(),
                        imageId = currentImageId,
                        imageBytes = null,
                        frameResult = frameResult,
                        onResultUpdate = onResultUpdate,
                        onRuntimeStatus = onRuntimeStatus
                    )
                }

                ScanRuntimeNotice(
                    status = runtimeStatus,
                    onRetry = onRetry,
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                controls()
            }
        },
        onDenied = { launchPermissionRequest ->
            CameraPermissionScreen(onRequestPermission = launchPermissionRequest)
        }
    )
}

/**
 * Component hiển thị thông báo trạng thái hoạt động của camera và mô hình nhận diện AI (đang tải model, lỗi quyền truy cập...).
 *
 * @param status Trạng thái runtime hiện tại của camera/model.
 * @param onRetry Callback khi nhấn nút Thử lại.
 * @param modifier Modifier tùy chỉnh bố cục.
 */
@Composable
private fun ScanRuntimeNotice(
    status: ScanRuntimeStatus,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val message = when (status) {
        ScanRuntimeStatus.Idle -> null
        ScanRuntimeStatus.RequestingCamera -> "Đang yêu cầu quyền camera..."
        is ScanRuntimeStatus.LoadingModel -> status.progressPercent?.let { "Đang tải mô hình AI: $it%" } ?: "Đang tải mô hình AI..."
        is ScanRuntimeStatus.Ready -> if (status.liveDetectionSupported) null else "Backend ${status.backend.label}: hãy đóng băng khung hình hoặc chọn ảnh để nhận diện."
        is ScanRuntimeStatus.PermissionDenied -> status.message
        is ScanRuntimeStatus.Unsupported -> status.message
        is ScanRuntimeStatus.Error -> status.message
    } ?: return

    val canRetry = status is ScanRuntimeStatus.PermissionDenied || status is ScanRuntimeStatus.Unsupported || status is ScanRuntimeStatus.Error
    Surface(
        modifier = modifier.padding(12.dp).widthIn(max = 520.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.Black.copy(alpha = 0.72f),
        contentColor = Color.White
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (status is ScanRuntimeStatus.LoadingModel || status == ScanRuntimeStatus.RequestingCamera) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
            } else {
                Icon(Icons.Rounded.Info, contentDescription = null, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text(message, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
            if (canRetry) {
                TextButton(onClick = onRetry) { Text("Thử lại", color = Color.White) }
            }
        }
    }
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

        IconButton(
            onClick = {
                onModeChange(ScanMode.LIVE)
                onClearResult()
            },
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f))
        ) {
            Icon(Icons.Rounded.Refresh, contentDescription = "Làm mới", tint = Color.White)
        }
    }
}