package ru.nubovpn.app.ui.qr

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import ru.nubovpn.app.ui.NuboTheme
import ru.nubovpn.app.util.AppLog
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Полноэкранный сканер QR-кода в стиле NUBO:
 * камера на весь экран, рамка по центру, фонарик, выбор из галереи.
 */
class QrScanActivity : ComponentActivity() {
    companion object {
        const val EXTRA_RESULT = "qr_result"
    }

    private var previewView: PreviewView? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private var torchOn = false
    private var resultReturned = false
    private val analyzing = AtomicBoolean(false)

    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private val barcodeScanner: BarcodeScanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build(),
    )

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            previewView?.let(::bindCamera)
        } else {
            Toast.makeText(this, "Нет доступа к камере", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) decodeFromGallery(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            NuboTheme {
                ScannerScreen(
                    onPreviewReady = { view ->
                        previewView = view
                        if (hasCameraPermission()) bindCamera(view)
                    },
                    onClose = { finish() },
                    onToggleTorch = { on ->
                        torchOn = on
                        camera?.cameraControl?.enableTorch(on)
                    },
                    onSwitchCamera = {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                            CameraSelector.LENS_FACING_FRONT
                        } else {
                            CameraSelector.LENS_FACING_BACK
                        }
                        previewView?.let(::bindCamera)
                    },
                    onPickFromGallery = { pickImageLauncher.launch("image/*") },
                )
            }
        }

        if (!hasCameraPermission()) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    override fun onDestroy() {
        cameraExecutor.shutdown()
        barcodeScanner.close()
        super.onDestroy()
    }

    private fun hasCameraPermission(): Boolean =
        checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private fun bindCamera(previewView: PreviewView) {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener(
            {
                runCatching {
                    val provider = future.get()
                    cameraProvider = provider
                    provider.unbindAll()

                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                    analysis.setAnalyzer(cameraExecutor, ::analyzeFrame)

                    val selector = CameraSelector.Builder()
                        .requireLensFacing(lensFacing)
                        .build()

                    camera = provider.bindToLifecycle(this, selector, preview, analysis)
                    camera?.cameraControl?.enableTorch(torchOn)
                }.onFailure {
                    AppLog.e("QrScanActivity bindCamera failed", it)
                    Toast.makeText(this, "Не удалось запустить камеру", Toast.LENGTH_LONG).show()
                }
            },
            ContextCompat.getMainExecutor(this),
        )
    }

    private fun analyzeFrame(imageProxy: ImageProxy) {
        if (resultReturned) {
            imageProxy.close()
            return
        }
        if (!analyzing.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            analyzing.set(false)
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        barcodeScanner.process(image)
            .addOnSuccessListener { barcodes ->
                val text = barcodes.firstOrNull()?.rawValue
                if (!text.isNullOrBlank()) {
                    runOnUiThread { finishWithResult(text) }
                }
            }
            .addOnFailureListener {
                AppLog.e("QrScanActivity analyzeFrame failed", it)
            }
            .addOnCompleteListener {
                analyzing.set(false)
                imageProxy.close()
            }
    }

    private fun finishWithResult(text: String) {
        val value = text.trim()
        if (resultReturned || value.isBlank()) return
        resultReturned = true
        AppLog.i("QrScanActivity result len=${value.length}")
        setResult(RESULT_OK, Intent().putExtra(EXTRA_RESULT, value))
        finish()
    }

    private fun decodeFromGallery(uri: Uri) {
        val bitmap = loadBitmap(uri)
        if (bitmap == null) {
            Toast.makeText(this, "Не удалось открыть изображение", Toast.LENGTH_LONG).show()
            return
        }

        val image = InputImage.fromBitmap(bitmap, 0)
        barcodeScanner.process(image)
            .addOnSuccessListener { barcodes ->
                val text = barcodes.firstOrNull()?.rawValue
                if (!text.isNullOrBlank()) {
                    finishWithResult(text)
                } else {
                    Toast.makeText(this, "QR-код на изображении не найден", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener {
                AppLog.e("QrScanActivity decodeFromGallery failed", it)
                Toast.makeText(this, "Не удалось распознать QR-код", Toast.LENGTH_LONG).show()
            }
            .addOnCompleteListener {
                bitmap.recycle()
            }
    }

    /** Загружает картинку из галереи с даунскейлом до ~1600px, чтобы не словить OOM. */
    private fun loadBitmap(uri: Uri): Bitmap? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }

        var sampleSize = 1
        val maxSide = maxOf(bounds.outWidth, bounds.outHeight)
        while (maxSide / sampleSize > 1600) sampleSize *= 2

        val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
    }.onFailure {
        AppLog.e("QrScanActivity loadBitmap failed", it)
    }.getOrNull()
}

@Composable
private fun ScannerScreen(
    onPreviewReady: (PreviewView) -> Unit,
    onClose: () -> Unit,
    onToggleTorch: (Boolean) -> Unit,
    onSwitchCamera: () -> Unit,
    onPickFromGallery: () -> Unit,
) {
    var torchOn by remember { mutableStateOf(false) }
    val buttonColor = Color(0xFF2B2E83)

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                PreviewView(context).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    onPreviewReady(this)
                }
            },
        )

        ScannerFrameOverlay(modifier = Modifier.fillMaxSize())

        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            RoundIconButton(
                icon = Icons.Default.Close,
                contentDescription = "Закрыть",
                background = buttonColor,
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp, top = 12.dp),
            )

            RoundIconButton(
                icon = Icons.Default.FlipCameraAndroid,
                contentDescription = "Сменить камеру",
                background = buttonColor,
                onClick = onSwitchCamera,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 16.dp, top = 12.dp),
            )

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                PillButton(
                    text = "Фонарик",
                    icon = if (torchOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                    background = buttonColor,
                    onClick = {
                        torchOn = !torchOn
                        onToggleTorch(torchOn)
                    },
                    modifier = Modifier.weight(1f),
                )
                PillButton(
                    text = "Галерея",
                    icon = Icons.Default.PhotoLibrary,
                    background = buttonColor,
                    onClick = onPickFromGallery,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** Прямоугольная рамка с закруглениями по центру + затемнение вокруг. */
@Composable
private fun ScannerFrameOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val frameWidth = size.width * 0.86f
        val frameHeight = frameWidth * 1.05f
        val left = (size.width - frameWidth) / 2f
        val top = size.height * 0.27f
        val corner = CornerRadius(24.dp.toPx())
        val frameRect = Rect(Offset(left, top), androidx.compose.ui.geometry.Size(frameWidth, frameHeight))

        val windowPath = Path().apply {
            addRoundRect(RoundRect(frameRect, corner))
        }
        clipPath(windowPath, clipOp = ClipOp.Difference) {
            drawRect(Color.Black.copy(alpha = 0.4f))
        }
        drawRoundRect(
            color = Color.White.copy(alpha = 0.85f),
            topLeft = frameRect.topLeft,
            size = frameRect.size,
            cornerRadius = corner,
            style = Stroke(width = 2.dp.toPx()),
        )
    }
}

@Composable
private fun RoundIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    background: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun PillButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    background: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(background)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
        )
    }
}
