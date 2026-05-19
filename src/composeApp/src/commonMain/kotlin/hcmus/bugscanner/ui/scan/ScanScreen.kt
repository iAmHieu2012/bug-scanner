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
import hcmus.bugscanner.ui.theme.DeepForest
import hcmus.bugscanner.ui.theme.SeedGreen
import hcmus.bugscanner.ui.theme.SoftGreen
import org.jetbrains.compose.resources.stringResource

/**
 * Màn hình xử lý chức năng quét và nhận diện côn trùng.
 */
@Composable
fun ScanScreen(
    isLoggedIn: Boolean,
    onAuthAction: () -> Unit,
    onDetectedBugClick: (String) -> Unit
) {
    var currentMode by remember { mutableStateOf(ScanMode.LIVE) }
    var frameResult by remember { mutableStateOf<FrameResult?>(null) }
    var currentImageId by remember { mutableStateOf<String?>(null) }

    val pickerHelper = rememberImagePickerHelper(
        onModeChange = { currentMode = it },
        onResult = { frameResult = it },
        onImageIdCaptured = { currentImageId = it }
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = stringResource(Res.string.scan_greeting_msg), style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
                Text(text = stringResource(Res.string.scan_what_to_find), style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = DeepForest)
            }
            IconButton(
                onClick = onAuthAction,
                modifier = Modifier.background(SoftGreen, CircleShape)
            ) {
                Icon(
                    imageVector = if (isLoggedIn) Icons.AutoMirrored.Rounded.Logout else Icons.AutoMirrored.Rounded.Login,
                    contentDescription = if (isLoggedIn) stringResource(Res.string.action_logout) else stringResource(Res.string.action_login),
                    tint = DeepForest
                )
            }
        }

        Box(
            modifier = Modifier.weight(1f).padding(horizontal = 20.dp).clip(RoundedCornerShape(32.dp)).border(2.dp, Color.White, RoundedCornerShape(32.dp)).background(Color.Black)
        ) {
            if (currentMode == ScanMode.LIVE) {
                NativeCameraView(
                    modifier = Modifier.fillMaxSize(),
                    onResult = { frameResult = it }
                )
                ScannerOverlay()
            } else {
                NativeStaticDetectionView(
                    modifier = Modifier.fillMaxSize(),
                    imageId = currentImageId,
                    frameResult = frameResult
                )
            }

            val quickModes: List<Pair<ScanMode, ImageVector>> = listOf(
                ScanMode.LIVE to Icons.Rounded.Videocam,
                ScanMode.IMAGE_UPLOAD to Icons.Rounded.PhotoLibrary,
                ScanMode.CAMERA_CAPTURE to Icons.Rounded.Camera
            )

            Row(modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp).background(Color.Black.copy(alpha = 0.4f), CircleShape).padding(4.dp)) {
                quickModes.forEach { (mode, icon) ->
                    IconButton(
                        onClick = {
                            when (mode) {
                                ScanMode.LIVE -> {
                                    currentMode = mode
                                    frameResult = null
                                }
                                ScanMode.IMAGE_UPLOAD -> pickerHelper.launchGallery()
                                ScanMode.CAMERA_CAPTURE -> pickerHelper.launchCamera()
                            }
                        },
                        modifier = Modifier.clip(CircleShape).background(if (currentMode == mode) SeedGreen else Color.Transparent)
                    ) { Icon(icon, null, tint = Color.White) }
                }
            }
        }
        DetectionPanel(frameResult, onDetectedBugClick)
    }
}

/**
 * Lớp phủ UI khung ngắm chữ thập trên Camera.
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