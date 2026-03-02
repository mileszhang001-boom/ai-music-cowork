package com.music.perception.sdk

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraSource(private val context: Context, private val lifecycleOwner: LifecycleOwner) {

    private var imageAnalysis: ImageAnalysis? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var isBound = false

    interface ImageCallback {
        fun onBitmapAvailable(bitmap: Bitmap)
    }

    private var callback: ImageCallback? = null

    fun setCallback(cb: ImageCallback) {
        this.callback = cb
    }

    fun start() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.e("CameraSource", "Camera permission missing")
            return
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases()
            } catch (exc: Exception) {
                Log.e("CameraSource", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: return
        
        val cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(android.util.Size(640, 480))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()

        imageAnalysis?.setAnalyzer(cameraExecutor) { imageProxy ->
            processImageProxy(imageProxy)
        }

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                imageAnalysis
            )
            isBound = true
            Log.d("CameraSource", "Camera bound successfully")
        } catch (exc: Exception) {
            Log.e("CameraSource", "Use case binding failed", exc)
        }
    }

    private fun processImageProxy(imageProxy: ImageProxy) {
        try {
            val bitmap = imageProxyToBitmap(imageProxy)
            if (bitmap != null) {
                callback?.onBitmapAvailable(bitmap)
            }
        } catch (e: Exception) {
            Log.e("CameraSource", "Error processing image", e)
        } finally {
            imageProxy.close()
        }
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
        return try {
            val buffer = image.planes[0].buffer
            val width = image.width
            val height = image.height
            
            if (width <= 10 || height <= 10) {
                Log.w("CameraSource", "Image too small: ${width}x${height}")
                return null
            }
            
            if (buffer.remaining() < width * height * 4) {
                Log.w("CameraSource", "Buffer too small: ${buffer.remaining()} bytes for ${width}x${height}")
                return null
            }
            
            val pixels = IntArray(buffer.remaining() / 4)
            buffer.asIntBuffer().get(pixels)
            
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            
            val targetWidth = 640
            val targetHeight = 480
            Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
        } catch (e: Exception) {
            Log.e("CameraSource", "Failed to convert image to bitmap: ${e.message}")
            null
        }
    }

    fun stop() {
        cameraExecutor.shutdown()
    }
}
