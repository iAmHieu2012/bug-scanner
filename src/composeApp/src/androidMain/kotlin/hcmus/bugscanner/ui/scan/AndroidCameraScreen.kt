package hcmus.bugscanner.ui.scan

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import hcmus.bugscanner.ml.YoloConstants
import hcmus.bugscanner.ui.scan.components.drawYoloBoundingBox

/**
 * Màn hình hiển thị luồng trực tiếp từ Camera và vẽ bounding box YOLO.
 */
@Composable
fun AndroidCameraScreen(
    viewModel: ScanViewModel,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val context = LocalContext.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    val frameResult by viewModel.frameResult.collectAsState()
    val textMeasurer = rememberTextMeasurer()

    val cameraExecutor = viewModel.cameraExecutor

    DisposableEffect(lifecycleOwner) {
        onDispose {
            try {
                val cameraProvider = cameraProviderFuture.get()
                cameraProvider.unbindAll()
            } catch (exc: Exception) {
                exc.printStackTrace()
            }
        }
    }
    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    val resolutionSelector = ResolutionSelector.Builder()
                        .setResolutionStrategy(
                            ResolutionStrategy(
                                android.util.Size(YoloConstants.INPUT_SIZE, YoloConstants.INPUT_SIZE),
                                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                            )
                        )
                        .build()

                    val imageAnalyzer = ImageAnalysis.Builder()
                        .setResolutionSelector(resolutionSelector)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also {
                            it.setAnalyzer(cameraExecutor) { imageProxy ->
                                val bitmap = imageProxy.toBitmap()
                                val rotation = imageProxy.imageInfo.rotationDegrees

                                viewModel.analyzeImage(bitmap, rotation)

                                bitmap.recycle()
                                imageProxy.close()
                            }
                        }

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalyzer
                        )
                    } catch (exc: Exception) { exc.printStackTrace() }
                }, ContextCompat.getMainExecutor(context))
                previewView
            }
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val sourceW = frameResult.sourceWidth.toFloat()
            val sourceH = frameResult.sourceHeight.toFloat()

            if (sourceW <= 0f || sourceH <= 0f) return@Canvas

            val scale = maxOf(size.width / sourceW, size.height / sourceH)
            val dispW = sourceW * scale
            val dispH = sourceH * scale
            val offsetX = (dispW - size.width) / 2f
            val offsetY = (dispH - size.height) / 2f

            frameResult.boxes.forEach { box ->
                val nx1 = box.x1
                val ny1 = box.y1
                val nx2 = box.x2
                val ny2 = box.y2

                val left   = (nx1 * dispW - offsetX).coerceIn(0f, size.width)
                val top    = (ny1 * dispH - offsetY).coerceIn(0f, size.height)
                val right  = (nx2 * dispW - offsetX).coerceIn(0f, size.width)
                val bottom = (ny2 * dispH - offsetY).coerceIn(0f, size.height)

                drawYoloBoundingBox(
                    textMeasurer = textMeasurer,
                    className = box.className,
                    score = box.score,
                    left = left,
                    top = top,
                    right = right,
                    bottom = bottom
                )
            }
        }
    }
}