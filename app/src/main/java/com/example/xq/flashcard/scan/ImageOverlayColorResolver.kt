package com.example.xq.flashcard.ui.scan

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.RectF
import kotlin.math.max
import kotlin.math.min

data class OverlayColors(
    val backgroundColor: Int,
    val textColor: Int
)

object ImageOverlayColorResolver {

    private const val DEFAULT_LIGHT_BACKGROUND = 0xFFFDFDFD.toInt()
    private const val DEFAULT_DARK_TEXT = 0xFF20222B.toInt()
    private const val DEFAULT_LIGHT_TEXT = 0xFFFFFFFF.toInt()

    fun resolve(bitmap: Bitmap?, bounds: RectF): OverlayColors {
        if (bitmap == null || bitmap.width <= 0 || bitmap.height <= 0) {
            return OverlayColors(DEFAULT_LIGHT_BACKGROUND, DEFAULT_DARK_TEXT)
        }

        val innerRect = clampRect(bounds, bitmap.width, bitmap.height) ?: return OverlayColors(
            DEFAULT_LIGHT_BACKGROUND,
            DEFAULT_DARK_TEXT
        )

        val padding = max(4, min(innerRect.width(), innerRect.height()) / 5)
        val outerRect = expandRect(innerRect, bitmap.width, bitmap.height, padding)
        val samples = sampleRingPixels(bitmap, innerRect, outerRect)
            .ifEmpty { sampleInnerBorderPixels(bitmap, innerRect) }

        if (samples.isEmpty()) {
            return OverlayColors(DEFAULT_LIGHT_BACKGROUND, DEFAULT_DARK_TEXT)
        }

        val averageColor = averageColor(samples)
        val luminance = Color.luminance(averageColor)
        val textColor = if (luminance >= 0.55f) DEFAULT_DARK_TEXT else DEFAULT_LIGHT_TEXT
        return OverlayColors(averageColor, textColor)
    }

    private fun clampRect(bounds: RectF, maxWidth: Int, maxHeight: Int): android.graphics.Rect? {
        val left = bounds.left.toInt().coerceIn(0, maxWidth - 1)
        val top = bounds.top.toInt().coerceIn(0, maxHeight - 1)
        val right = bounds.right.toInt().coerceIn(left + 1, maxWidth)
        val bottom = bounds.bottom.toInt().coerceIn(top + 1, maxHeight)
        if (right <= left || bottom <= top) return null
        return android.graphics.Rect(left, top, right, bottom)
    }

    private fun expandRect(
        rect: android.graphics.Rect,
        maxWidth: Int,
        maxHeight: Int,
        padding: Int
    ): android.graphics.Rect {
        return android.graphics.Rect(
            (rect.left - padding).coerceAtLeast(0),
            (rect.top - padding).coerceAtLeast(0),
            (rect.right + padding).coerceAtMost(maxWidth),
            (rect.bottom + padding).coerceAtMost(maxHeight)
        )
    }

    private fun sampleRingPixels(
        bitmap: Bitmap,
        innerRect: android.graphics.Rect,
        outerRect: android.graphics.Rect
    ): List<Int> {
        val pixels = mutableListOf<Int>()
        val stepX = ((outerRect.width()) / 10).coerceAtLeast(1)
        val stepY = ((outerRect.height()) / 10).coerceAtLeast(1)

        for (y in outerRect.top until outerRect.bottom step stepY) {
            for (x in outerRect.left until outerRect.right step stepX) {
                if (x in innerRect.left until innerRect.right && y in innerRect.top until innerRect.bottom) {
                    continue
                }
                pixels += bitmap.getPixel(x, y)
            }
        }
        return pixels
    }

    private fun sampleInnerBorderPixels(bitmap: Bitmap, rect: android.graphics.Rect): List<Int> {
        val pixels = mutableListOf<Int>()
        val stepX = (rect.width() / 10).coerceAtLeast(1)
        val stepY = (rect.height() / 10).coerceAtLeast(1)

        for (x in rect.left until rect.right step stepX) {
            pixels += bitmap.getPixel(x, rect.top)
            pixels += bitmap.getPixel(x, rect.bottom - 1)
        }
        for (y in rect.top until rect.bottom step stepY) {
            pixels += bitmap.getPixel(rect.left, y)
            pixels += bitmap.getPixel(rect.right - 1, y)
        }
        return pixels
    }

    private fun averageColor(colors: List<Int>): Int {
        val alpha = colors.map { Color.alpha(it) }.average().toInt().coerceIn(235, 255)
        val red = colors.map { Color.red(it) }.average().toInt().coerceIn(0, 255)
        val green = colors.map { Color.green(it) }.average().toInt().coerceIn(0, 255)
        val blue = colors.map { Color.blue(it) }.average().toInt().coerceIn(0, 255)
        return Color.argb(alpha, red, green, blue)
    }
}
