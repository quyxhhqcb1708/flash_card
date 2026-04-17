package com.example.xq.flashcard.ui.scan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlin.math.max
import kotlin.math.roundToInt

object ImageBitmapLoader {

    fun loadBitmap(context: Context, uri: Uri): Bitmap? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val width = info.size.width
                val height = info.size.height
                val ratio = max(width / 1600f, height / 1600f)
                if (ratio > 1f) {
                    decoder.setTargetSize(
                        (width / ratio).roundToInt(),
                        (height / ratio).roundToInt()
                    )
                }
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    }
}
