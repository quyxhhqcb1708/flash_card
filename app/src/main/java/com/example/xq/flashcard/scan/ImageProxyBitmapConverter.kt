package com.example.xq.flashcard.ui.scan

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream

object ImageProxyBitmapConverter {

    fun toBitmap(imageProxy: ImageProxy): Bitmap? {
        if (imageProxy.format == ImageFormat.JPEG) {
            val jpegPlane = imageProxy.planes.firstOrNull() ?: return null
            val buffer = jpegPlane.buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
            return rotateBitmap(bitmap, imageProxy.imageInfo.rotationDegrees.toFloat())
        }

        val nv21 = yuv420ToNv21(imageProxy) ?: return null
        val yuvImage = YuvImage(
            nv21,
            ImageFormat.NV21,
            imageProxy.width,
            imageProxy.height,
            null
        )
        val outputStream = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, imageProxy.width, imageProxy.height), 100, outputStream)
        val imageBytes = outputStream.toByteArray()
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size) ?: return null
        return rotateBitmap(bitmap, imageProxy.imageInfo.rotationDegrees.toFloat())
    }

    private fun yuv420ToNv21(imageProxy: ImageProxy): ByteArray? {
        val yPlane = imageProxy.planes.getOrNull(0) ?: return null
        val uPlane = imageProxy.planes.getOrNull(1) ?: return null
        val vPlane = imageProxy.planes.getOrNull(2) ?: return null

        val ySize = yPlane.buffer.remaining()
        val uSize = uPlane.buffer.remaining()
        val vSize = vPlane.buffer.remaining()
        val nv21 = ByteArray(ySize + uSize + vSize)

        yPlane.buffer.get(nv21, 0, ySize)
        val rowStride = uPlane.rowStride
        val pixelStride = uPlane.pixelStride
        val width = imageProxy.width
        val height = imageProxy.height
        var offset = ySize

        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer

        val chromaHeight = height / 2
        val chromaWidth = width / 2

        for (row in 0 until chromaHeight) {
            for (col in 0 until chromaWidth) {
                val vuPos = row * rowStride + col * pixelStride
                nv21[offset++] = vBuffer.get(vuPos)
                nv21[offset++] = uBuffer.get(vuPos)
            }
        }
        return nv21
    }

    private fun rotateBitmap(bitmap: Bitmap, rotation: Float): Bitmap {
        if (rotation == 0f) return bitmap
        val matrix = Matrix().apply {
            postRotate(rotation)
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
