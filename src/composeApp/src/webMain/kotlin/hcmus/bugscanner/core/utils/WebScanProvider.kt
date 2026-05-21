package hcmus.bugscanner.ui.scan

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hcmus.bugscanner.domain.model.DetectionResult
import hcmus.bugscanner.domain.model.FrameResult
import hcmus.bugscanner.ml.YoloConstants
import hcmus.bugscanner.ui.scan.components.getBugColor
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jetbrains.skia.Image
import org.khronos.webgl.Int8Array
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLVideoElement
import org.w3c.dom.url.URL
import kotlin.js.Promise

/**
 * Object chứa logic Camera và Image Picker cho Web (JS)
 */
object WebScanProvider : PlatformScanProvider {

    /**
     * Màn hình hiển thị luồng trực tiếp từ Camera bằng HTML <video> và quét AI liên tục
     */
    @Composable
    override fun NativeCameraView(modifier: Modifier, onResult: (FrameResult) -> Unit) {
        val textMeasurer = rememberTextMeasurer()
        var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
        var currentFrameResult by remember { mutableStateOf<FrameResult?>(null) }
        val coroutineScope = rememberCoroutineScope()

        DisposableEffect(Unit) {
            val video = document.createElement("video") as HTMLVideoElement
            video.autoplay = true
            video.playsInline = true

            val htmlCanvas = document.createElement("canvas") as HTMLCanvasElement
            val ctx = htmlCanvas.getContext("2d") as CanvasRenderingContext2D

            var streamData: dynamic = null

            val navigatorDyn = window.navigator.asDynamic()
            navigatorDyn.mediaDevices.getUserMedia(js("{ video: { facingMode: 'environment' } }"))
                .then { stream ->
                    streamData = stream
                    video.srcObject = stream
                }.catch { e: dynamic ->
                    println("Lỗi mở camera Web: $e")
                }

            val job = coroutineScope.launch {
                while (isActive) {
                    delay(500)

                    if (video.videoWidth > 0 && video.videoHeight > 0) {
                        htmlCanvas.width = video.videoWidth
                        htmlCanvas.height = video.videoHeight
                        ctx.drawImage(video, 0.0, 0.0, htmlCanvas.width.toDouble(), htmlCanvas.height.toDouble())

                        val dataUrl = htmlCanvas.toDataURL("image/jpeg", 0.7)

                        try {
                            // --- Cập nhật giao diện ---
                            val response = window.fetch(dataUrl, js("{}")).await()
                            val arrayBuffer = response.arrayBuffer().await()
                            val byteArray = Int8Array(arrayBuffer).unsafeCast<ByteArray>()
                            imageBitmap = Image.makeFromEncoded(byteArray).toComposeImageBitmap()

                            // --- Chạy AI ---
                            val jsonResultPromise = window.asDynamic().detectBugsJS(dataUrl).unsafeCast<Promise<String>>()
                            val jsonResult = jsonResultPromise.await()
                            val jsArray = kotlin.js.JSON.parse<Array<dynamic>>(jsonResult)

                            val detectionBoxes = jsArray.map { obj ->
                                val xMin = (obj.x as Number).toFloat()
                                val yMin = (obj.y as Number).toFloat()
                                val width = (obj.width as Number).toFloat()
                                val height = (obj.height as Number).toFloat()
                                val conf = (obj.confidence as Number).toFloat()

                                val classId = obj.label.toString().toIntOrNull() ?: 0
                                val labelStr = if (classId in YoloConstants.LABELS.indices) {
                                    YoloConstants.LABELS[classId]
                                } else {
                                    "Unknown Pest"
                                }

                                DetectionResult(
                                    x1 = xMin, y1 = yMin, x2 = xMin + width, y2 = yMin + height,
                                    score = conf, className = labelStr
                                )
                            }.toList()

                            val newResult = FrameResult(
                                boxes = detectionBoxes,
                                sourceWidth = video.videoWidth,
                                sourceHeight = video.videoHeight
                            )
                            currentFrameResult = newResult
                            onResult(newResult)

                        } catch (e: Exception) {
                            println("Lỗi xử lý luồng Live Camera: ${e.message}")
                        }
                    }
                }
            }

            onDispose {
                job.cancel()
                if (streamData != null) {
                    val tracks = streamData.getTracks()
                    for (i in 0 until tracks.length as Int) {
                        tracks[i].stop()
                    }
                }
                video.srcObject = null
            }
        }

        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (imageBitmap == null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Đang khởi động Camera...", color = Color.White)
                }
            } else {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val imgWidth = imageBitmap!!.width.toFloat()
                    val imgHeight = imageBitmap!!.height.toFloat()

                    val scale = maxOf(canvasWidth / imgWidth, canvasHeight / imgHeight)
                    val drawWidth = imgWidth * scale
                    val drawHeight = imgHeight * scale
                    val offsetX = (canvasWidth - drawWidth) / 2f
                    val offsetY = (canvasHeight - drawHeight) / 2f

                    drawImage(
                        image = imageBitmap!!,
                        dstOffset = IntOffset(offsetX.toInt(), offsetY.toInt()),
                        dstSize = IntSize(drawWidth.toInt(), drawHeight.toInt())
                    )

                    currentFrameResult?.boxes?.forEach { box ->
                        val left = (box.x1 * drawWidth + offsetX).coerceIn(0f, canvasWidth)
                        val top = (box.y1 * drawHeight + offsetY).coerceIn(0f, canvasHeight)
                        val right = (box.x2 * drawWidth + offsetX).coerceIn(0f, canvasWidth)
                        val bottom = (box.y2 * drawHeight + offsetY).coerceIn(0f, canvasHeight)
                        val width = right - left
                        val height = bottom - top

                        if (width > 0f && height > 0f) {
                            val boxColor = getBugColor(box.className)
                            drawRoundRect(color = boxColor, topLeft = Offset(left, top), size = Size(width, height), cornerRadius = CornerRadius(16f, 16f), style = Stroke(width = 5f))

                            val labelText = "${box.className} (${(box.score * 100).toInt()}%)"
                            val textStyle = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            val textLayoutResult = textMeasurer.measure(text = labelText, style = textStyle)
                            val textWidth = textLayoutResult.size.width.toFloat()
                            val textHeight = textLayoutResult.size.height.toFloat()

                            val labelTop = maxOf(0f, top - textHeight - 12f)
                            drawRoundRect(color = boxColor.copy(alpha = 0.85f), topLeft = Offset(left, labelTop), size = Size(textWidth + 24f, textHeight + 12f), cornerRadius = CornerRadius(12f, 12f))
                            drawText(textLayoutResult = textLayoutResult, color = Color.White, topLeft = Offset(left + 12f, labelTop + 6f))
                        }
                    }
                }
            }
        }
    }

    /**
     * Màn hình hiển thị ảnh tĩnh và vẽ Bounding Box trực tiếp bằng Compose Canvas
     */
    @Composable
    override fun NativeStaticDetectionView(
        modifier: Modifier,
        imageId: String?,
        frameResult: FrameResult?
    ) {
        val textMeasurer = rememberTextMeasurer()
        var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

        LaunchedEffect(imageId) {
            if (imageId != null) {
                try {
                    val response = window.fetch(imageId, js("{}")).await()
                    val arrayBuffer = response.arrayBuffer().await()
                    val byteArray = Int8Array(arrayBuffer).unsafeCast<ByteArray>()
                    imageBitmap = Image.makeFromEncoded(byteArray).toComposeImageBitmap()
                } catch (e: Exception) {
                    println("Lỗi load ảnh hiển thị: ${e.message}")
                }
            } else {
                imageBitmap = null
            }
        }

        if (imageId == null) {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Chưa có hình ảnh để phân tích",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return
        }

        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (imageBitmap == null) {
                CircularProgressIndicator()
            } else {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val imgWidth = imageBitmap!!.width.toFloat()
                    val imgHeight = imageBitmap!!.height.toFloat()

                    val scale = minOf(canvasWidth / imgWidth, canvasHeight / imgHeight)
                    val drawWidth = imgWidth * scale
                    val drawHeight = imgHeight * scale
                    val offsetX = (canvasWidth - drawWidth) / 2f
                    val offsetY = (canvasHeight - drawHeight) / 2f

                    drawImage(
                        image = imageBitmap!!,
                        dstOffset = IntOffset(offsetX.toInt(), offsetY.toInt()),
                        dstSize = IntSize(drawWidth.toInt(), drawHeight.toInt())
                    )

                    if (frameResult != null && frameResult.boxes.isNotEmpty()) {
                        frameResult.boxes.forEach { box ->
                            val left = (box.x1 * drawWidth + offsetX).coerceIn(offsetX, offsetX + drawWidth)
                            val top = (box.y1 * drawHeight + offsetY).coerceIn(offsetY, offsetY + drawHeight)
                            val right = (box.x2 * drawWidth + offsetX).coerceIn(offsetX, offsetX + drawWidth)
                            val bottom = (box.y2 * drawHeight + offsetY).coerceIn(offsetY, offsetY + drawHeight)
                            val width = right - left
                            val height = bottom - top

                            if (width > 0f && height > 0f) {
                                val boxColor = getBugColor(box.className)
                                drawRoundRect(color = boxColor, topLeft = Offset(left, top), size = Size(width, height), cornerRadius = CornerRadius(16f, 16f), style = Stroke(width = 5f))

                                val labelText = "${box.className} (${(box.score * 100).toInt()}%)"
                                val textStyle = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                val textLayoutResult = textMeasurer.measure(text = labelText, style = textStyle)
                                val textWidth = textLayoutResult.size.width.toFloat()
                                val textHeight = textLayoutResult.size.height.toFloat()

                                val labelTop = maxOf(0f, top - textHeight - 12f)
                                drawRoundRect(color = boxColor.copy(alpha = 0.85f), topLeft = Offset(left, labelTop), size = Size(textWidth + 24f, textHeight + 12f), cornerRadius = CornerRadius(12f, 12f))
                                drawText(textLayoutResult = textLayoutResult, color = Color.White, topLeft = Offset(left + 12f, labelTop + 6f))
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Quản lý việc chọn ảnh (Gallery) trên Web
     */
    @Composable
    override fun rememberImagePickerHelper(
        onModeChange: (ScanMode) -> Unit,
        onResult: (FrameResult) -> Unit,
        onImageIdCaptured: (String) -> Unit
    ): ImagePickerHelper {

        val fileInput = remember {
            val input = document.createElement("input") as HTMLInputElement
            input.apply {
                type = "file"
                accept = "image/*"
            }
        }

        val coroutineScope = rememberCoroutineScope()

        DisposableEffect(Unit) {
            fileInput.onchange = {
                val files = fileInput.files
                if (files != null && files.length > 0) {
                    val file = files.item(0)
                    if (file != null) {
                        val imageUrl = URL.createObjectURL(file as org.w3c.files.Blob)
                        onModeChange(ScanMode.IMAGE_UPLOAD)
                        onImageIdCaptured(imageUrl)

                        coroutineScope.launch {
                            try {
                                val jsonResultPromise = window.asDynamic()
                                    .detectBugsJS(imageUrl)
                                    .unsafeCast<Promise<String>>()

                                val jsonResult = jsonResultPromise.await()
                                val jsArray = kotlin.js.JSON.parse<Array<dynamic>>(jsonResult)

                                val detectionBoxes = jsArray.map { obj ->
                                    val xMin = (obj.x as Number).toFloat()
                                    val yMin = (obj.y as Number).toFloat()
                                    val width = (obj.width as Number).toFloat()
                                    val height = (obj.height as Number).toFloat()
                                    val conf = (obj.confidence as Number).toFloat()

                                    val classId = obj.label.toString().toIntOrNull() ?: 0
                                    val labelStr = if (classId in YoloConstants.LABELS.indices) {
                                        YoloConstants.LABELS[classId]
                                    } else {
                                        "Unknown Pest"
                                    }

                                    DetectionResult(
                                        x1 = xMin, y1 = yMin, x2 = xMin + width, y2 = yMin + height,
                                        score = conf, className = labelStr
                                    )
                                }.toList()

                                onResult(
                                    FrameResult(
                                        boxes = detectionBoxes,
                                        sourceWidth = 896,
                                        sourceHeight = 896
                                    )
                                )
                            } catch (e: Exception) {
                                println("Lỗi xử lý ảnh JS: ${e.message}")
                            }
                        }
                    }
                }
                null
            }
            onDispose {
                fileInput.onchange = null
            }
        }

        return remember {
            object : ImagePickerHelper {
                override fun launchGallery() {
                    fileInput.removeAttribute("capture")
                    fileInput.click()
                }

                override fun launchCamera() {
                    fileInput.setAttribute("capture", "environment")
                    fileInput.click()
                }
            }
        }
    }
}