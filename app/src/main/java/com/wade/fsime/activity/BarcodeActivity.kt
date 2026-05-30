package com.wade.fsime.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.wade.fsime.R
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class BarcodeActivity : AppCompatActivity() {

    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    private lateinit var previewView: PreviewView
    private lateinit var viewfinder: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ocr) // Reusing OCR layout

        previewView = findViewById(R.id.previewView)
        viewfinder = findViewById(R.id.viewfinder)
        val captureOverlay = findViewById<View>(R.id.capture_overlay)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        captureOverlay.setOnClickListener {
            takePhotoAndScan()
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhotoAndScan() {
        val imageCapture = imageCapture ?: return

        Toast.makeText(this, "正在讀取條碼...", Toast.LENGTH_SHORT).show()

        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    processImageProxy(imageProxy)
                }

                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                    Toast.makeText(this@BarcodeActivity, "拍攝失敗", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(imageProxy: ImageProxy) {
        val bitmap = imageProxyToBitmap(imageProxy)
        imageProxy.close()

        if (bitmap == null) {
            Log.e(TAG, "Failed to convert imageProxy to Bitmap")
            runOnUiThread { Toast.makeText(this, "處理影像失敗", Toast.LENGTH_SHORT).show() }
            return
        }

        val croppedBitmap = getCroppedBitmap(bitmap)
        val image = InputImage.fromBitmap(croppedBitmap, 0)
        
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .build()
        val scanner = BarcodeScanning.getClient(options)

        Log.d(TAG, "Starting ML Kit barcode scanning...")
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                if (barcodes.isNotEmpty()) {
                    val resultText = barcodes[0].rawValue ?: ""
                    Log.d(TAG, "Barcode Success Result: '$resultText'")
                    if (resultText.isNotEmpty()) {
                        BarcodeResultHolder.pendingResult = resultText
                        finish()
                    } else {
                        Log.w(TAG, "Barcode Success but text was empty")
                        Toast.makeText(this, "方框內找不到條碼，請再對準一點", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Log.w(TAG, "Barcode list is empty")
                    Toast.makeText(this, "方框內找不到條碼，請再對準一點", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Barcode scanning failed", e)
                Toast.makeText(this, "辨識發生錯誤: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        
        val rotationDegrees = image.imageInfo.rotationDegrees.toFloat()
        return if (rotationDegrees != 0f) {
            val matrix = Matrix()
            matrix.postRotate(rotationDegrees)
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }
    }

    private fun getCroppedBitmap(fullBitmap: Bitmap): Bitmap {
        val previewWidth = previewView.width.toFloat()
        val previewHeight = previewView.height.toFloat()
        val bitmapWidth = fullBitmap.width.toFloat()
        val bitmapHeight = fullBitmap.height.toFloat()

        val scale: Float
        val dx: Float
        val dy: Float

        if (bitmapWidth * previewHeight > previewWidth * bitmapHeight) {
            scale = previewHeight / bitmapHeight
            dx = (previewWidth - bitmapWidth * scale) * 0.5f
            dy = 0f
        } else {
            scale = previewWidth / bitmapWidth
            dx = 0f
            dy = (previewHeight - bitmapHeight * scale) * 0.5f
        }

        val matrix = Matrix()
        matrix.postScale(scale, scale)
        matrix.postTranslate(dx, dy)
        
        val inverseMatrix = Matrix()
        matrix.invert(inverseMatrix)

        val viewfinderRect = RectF(
            viewfinder.left.toFloat(),
            viewfinder.top.toFloat(),
            viewfinder.right.toFloat(),
            viewfinder.bottom.toFloat()
        )
        
        val bitmapRect = RectF()
        inverseMatrix.mapRect(bitmapRect, viewfinderRect)

        val padding = 15f
        val left = (bitmapRect.left - padding).coerceAtLeast(0f).toInt()
        val top = (bitmapRect.top - padding).coerceAtLeast(0f).toInt()
        val width = (bitmapRect.width() + padding * 2).toInt().coerceAtMost(fullBitmap.width - left)
        val height = (bitmapRect.height() + padding * 2).toInt().coerceAtMost(fullBitmap.height - top)

        return Bitmap.createBitmap(fullBitmap, left, top, width, height)
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "需開啟相機權限才能辨識條碼，請在設定中開啟", Toast.LENGTH_LONG).show()
                openAppSettings()
                finish()
            }
        }
    }

    companion object {
        private const val TAG = "BarcodeActivity"
        private const val REQUEST_CODE_PERMISSIONS = 11
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}
