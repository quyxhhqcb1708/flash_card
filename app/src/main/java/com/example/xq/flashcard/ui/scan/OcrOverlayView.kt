package com.example.xq.flashcard.ui.scan

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.example.xq.flashcard.R
import kotlin.math.min

class OcrOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.scan_overlay_fill)
        style = Paint.Style.FILL
    }

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.scan_overlay_stroke)
        style = Paint.Style.STROKE
        strokeWidth = 2f * resources.displayMetrics.density
    }

    private val selectedStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.auth_gradient_start)
        style = Paint.Style.STROKE
        strokeWidth = 3f * resources.displayMetrics.density
    }

    private val blocks = mutableListOf<OcrTextBlock>()
    private var sourceWidth = 0f
    private var sourceHeight = 0f
    private var renderedBlocks: List<Pair<OcrTextBlock, RectF>> = emptyList()
    private var selectedBlockText: String? = null
    private var interactionEnabled = false

    var onBlockTapped: ((OcrTextBlock) -> Unit)? = null

    fun setBlocks(items: List<OcrTextBlock>, imageWidth: Int, imageHeight: Int) {
        blocks.clear()
        blocks.addAll(items)
        sourceWidth = imageWidth.toFloat()
        sourceHeight = imageHeight.toFloat()
        if (items.none { it.text == selectedBlockText }) {
            selectedBlockText = null
        }
        invalidate()
    }

    fun clearBlocks() {
        blocks.clear()
        renderedBlocks = emptyList()
        selectedBlockText = null
        invalidate()
    }

    fun setInteractionEnabled(enabled: Boolean) {
        interactionEnabled = enabled
    }

    fun selectBlock(text: String?) {
        selectedBlockText = text
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        renderedBlocks = mapBlocks()
        renderedBlocks.forEach { (block, rect) ->
            canvas.drawRoundRect(rect, 16f, 16f, fillPaint)
            canvas.drawRoundRect(
                rect,
                16f,
                16f,
                if (block.text == selectedBlockText) selectedStrokePaint else strokePaint
            )
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!interactionEnabled) return false
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                return renderedBlocks.any { it.second.contains(event.x, event.y) }
            }

            MotionEvent.ACTION_UP -> {
                renderedBlocks.lastOrNull { it.second.contains(event.x, event.y) }?.let { pair ->
                    selectedBlockText = pair.first.text
                    invalidate()
                    performClick()
                    onBlockTapped?.invoke(pair.first)
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun mapBlocks(): List<Pair<OcrTextBlock, RectF>> {
        if (sourceWidth <= 0f || sourceHeight <= 0f || width == 0 || height == 0) {
            return emptyList()
        }
        val scale = min(width / sourceWidth, height / sourceHeight)
        val dx = (width - sourceWidth * scale) / 2f
        val dy = (height - sourceHeight * scale) / 2f
        return blocks.map { block ->
            val mapped = RectF(
                block.bounds.left * scale + dx,
                block.bounds.top * scale + dy,
                block.bounds.right * scale + dx,
                block.bounds.bottom * scale + dy
            )
            block to mapped
        }
    }
}
