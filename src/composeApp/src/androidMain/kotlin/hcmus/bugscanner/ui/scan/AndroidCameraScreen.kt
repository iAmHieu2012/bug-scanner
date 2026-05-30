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
import hcmus.bugscanner.ui.scan.utils.drawYoloBoundingBox
import java.io.ByteArrayOutputStream

/**
 * Component giao diện hiển thị luồng video trực tiếp từ Camera dành riêng cho Android.
 * Sử dụng thư viện CameraX để lấy từng khung hình (frame), đẩy qua YOLO AI để phân tích,
 * và kết hợp với Canvas để vẽ khung nhận diện (Bounding Box) đè lên luồng video.
 *
 * @param viewModel ViewModel quản lý tiến trình nhận diện AI độc lập trên Android.
 * @param modifier Modifier tùy chỉnh kích thước, vị trí.
 * @param onLiveFrameCaptured Callback xuất dữ liệu ảnh nén dạng mảng byte (ByteArray) mỗi khi AI nhận diện thành công một côn trùng.
 */
@Composable
fun AndroidCameraScreen(
    viewModel: ScanViewModel,
    modifier: Modifier = Modifier,
    onLiveFrameCaptured: (ByteArray?) -> Unit
) {
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val context = LocalContext.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    val frameResult by viewModel.frameResult.collectAsState()
    val textMeasurer = rememberTextMeasurer()

    val cameraExecutor = viewModel.cameraExecutor

    // Hủy liên kết (Unbind) camera khi Component này bị gỡ bỏ khỏi màn hình để giải phóng tài nguyên
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
        // Tích hợp Android View truyền thống (PreviewView của CameraX) vào trong Compose
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
                    // Yêu cầu Camera cung cấp ảnh có độ phân giải gần nhất với đầu vào của YOLO
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
                        // Chỉ lấy khung hình mới nhất, bỏ qua các khung hình cũ nếu AI xử lý không kịp (tránh giật lag)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also {
                            it.setAnalyzer(cameraExecutor) { imageProxy ->
                                val bitmap = imageProxy.toBitmap()
                                val rotation = imageProxy.imageInfo.rotationDegrees

                                // 1. Chạy AI nhận diện
                                viewModel.analyzeImage(bitmap, rotation)

                                // 2. Kiểm tra nếu có bọ -> Nén ảnh và đẩy ra ngoài
                                if (viewModel.frameResult.value.boxes.isNotEmpty()) {
                                    val stream = ByteArrayOutputStream()
                                    // Nén chất lượng 70% để tiết kiệm bộ nhớ và băng thông Firebase
                                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, stream)
                                    onLiveFrameCaptured(stream.toByteArray())
                                } else {
                                    // Báo hiệu không có dữ liệu hình ảnh nào đáng lưu
                                    onLiveFrameCaptured(null)
                                }

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

        // Canvas vẽ đè các Bounding Box lên trên AndroidView
        Canvas(modifier = Modifier.fillMaxSize()) {
            val sourceW = frameResult.sourceWidth.toFloat()
            val sourceH = frameResult.sourceHeight.toFloat()

            if (sourceW <= 0f || sourceH <= 0f) return@Canvas

            // Tính toán tỷ lệ Scale và Offset để Map chuẩn xác tọa độ Bounding Box từ AI vào màn hình thực tế
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