package hcmus.bugscanner.ui.scan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import hcmus.bugscanner.domain.model.FrameResult
import hcmus.bugscanner.ml.WebYoloDetector
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLImageElement
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

object WebScanProvider : PlatformScanProvider {
    @Composable
    override fun RequireCameraPermission(
        onGranted: @Composable () -> Unit,
        onDenied: @Composable (onRequestPermission: () -> Unit) -> Unit
    ) = onGranted()

    @Composable
    override fun NativeCameraView(
        modifier: Modifier,
        captureTrigger: Long,
        onResult: (FrameResult) -> Unit,
        onFrameCaptured: (ByteArray) -> Unit,
        onRuntimeStatus: (ScanRuntimeStatus) -> Unit
    ) {
        WebCameraScreen(modifier, captureTrigger, onResult, onFrameCaptured, onRuntimeStatus)
    }

    @OptIn(ExperimentalEncodingApi::class)
    @Composable
    override fun NativeStaticDetectionView(
        modifier: Modifier,
        imageId: String?,
        imageBytes: ByteArray?,
        frameResult: FrameResult?,
        onResultUpdate: (FrameResult) -> Unit,
        onRuntimeStatus: (ScanRuntimeStatus) -> Unit
    ) {
        val displayId = remember(imageId, imageBytes) {
            imageBytes?.let { "data:image/jpeg;base64,${Base64.encode(it)}" } ?: imageId
        }
        val scope = rememberCoroutineScope()

        LaunchedEffect(displayId) {
            if (displayId == null) return@LaunchedEffect
            onRuntimeStatus(ScanRuntimeStatus.LoadingModel())
            val runtime = WebYoloDetector.initialize()
            onRuntimeStatus(runtime.toRuntimeStatus())
            if (!runtime.ready) return@LaunchedEffect

            val image = kotlinx.browser.document.createElement("img") as HTMLImageElement
            image.onload = {
                scope.launch {
                    onResultUpdate(WebYoloDetector.analyze(image, image.width, image.height))
                }
                null
            }
            image.src = displayId
        }

        WebStaticDetectionScreen(modifier = modifier, imageId = displayId, frameResult = frameResult)
    }

    @Composable
    override fun rememberImagePickerHelper(
        onModeChange: (ScanMode) -> Unit,
        onResult: (FrameResult) -> Unit,
        onImageIdCaptured: (String) -> Unit,
        onImageBytesCaptured: (ByteArray?) -> Unit
    ): ImagePickerHelper = rememberWebImagePickerHelper(
        onModeChange,
        onResult,
        onImageIdCaptured,
        onImageBytesCaptured
    )
}
