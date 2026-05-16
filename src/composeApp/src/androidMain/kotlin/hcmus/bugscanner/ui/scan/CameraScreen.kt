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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as ComposeSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import hcmus.bugscanner.ml.YoloConstants
import hcmus.bugscanner.ml.YoloDetector
import java.util.concurrent.ExecutorService

/**
 * Màn hình hiển thị luồng trực tiếp từ Camera và vẽ bounding box YOLO.
 */
@Composable
fun CameraScreen(
    yoloDetector: YoloDetector,
    cameraExecutor: ExecutorService,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val context = LocalContext.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val frameResult by yoloDetector.frameResult.collectAsState()
    val textMeasurer = rememberTextMeasurer()

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
                        it.setSurfaceProvider(previewView.surfaceProvider)
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
                                yoloDetector.analyze(bitmap, rotation)

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

                val width = right - left
                val height = bottom - top

                if (width <= 0f || height <= 0f) return@forEach

                drawRect(
                    color = Color.Green,
                    topLeft = Offset(left, top),
                    size = ComposeSize(width, height),
                    style = Stroke(width = 8f)
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = "${box.className} ${(box.score * 100).toInt()}%",
                    topLeft = Offset(left, maxOf(0f, top - 50f)),
                    style = TextStyle(color = Color.White, fontSize = 20.sp, background = Color.Red)
                )
            }
        }
    }
}