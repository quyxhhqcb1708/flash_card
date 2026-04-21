package com.example.xq.flashcard.ui.scan

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.example.xq.flashcard.R
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min

class OcrOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.scan_overlay_fill)
        style = Paint.Style.FILL
    }

    private val selectedStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.auth_gradient_start)
        style = Paint.Style.STROKE
        strokeWidth = 3f * resources.displayMetrics.density
    }

    private val selectionStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.auth_surface)
        style = Paint.Style.STROKE
        strokeWidth = 2f * resources.displayMetrics.density
    }

    private val translatedFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.auth_surface)
        style = Paint.Style.FILL
    }

    private val translatedTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.auth_text_primary)
        textSize = 14f * resources.displayMetrics.scaledDensity
    }

    private val blocks = mutableListOf<OcrTextBlock>()
    private var sourceWidth = 0f
    private var sourceHeight = 0f
    private var renderedBlocks: List<Pair<OcrTextBlock, RectF>> = emptyList()
    private var selectedBlocks: List<OcrTextBlock> = emptyList()
    private var translatedBlocks: List<TranslatedOcrBlock> = emptyList()
    private var interactionEnabled = false
    private var selectionRect: RectF? = null
    private var anchorBlock: OcrTextBlock? = null
    private var downX = 0f
    private var downY = 0f

    var onSelectionChanged: ((List<OcrTextBlock>) -> Unit)? = null

    fun setBlocks(items: List<OcrTextBlock>, imageWidth: Int, imageHeight: Int) {
        blocks.clear()
        blocks.addAll(items)
        sourceWidth = imageWidth.toFloat()
        sourceHeight = imageHeight.toFloat()
        if (selectedBlocks.any { selected -> items.none { it == selected } }) {
            selectedBlocks = emptyList()
        }
        invalidate()
    }

    fun clearBlocks() {
        blocks.clear()
        renderedBlocks = emptyList()
        selectedBlocks = emptyList()
        translatedBlocks = emptyList()
        selectionRect = null
        invalidate()
    }

    fun setInteractionEnabled(enabled: Boolean) {
        interactionEnabled = enabled
    }

    fun selectBlocks(items: List<OcrTextBlock>) {
        selectedBlocks = items
        invalidate()
    }

    fun showTranslatedBlocks(items: List<TranslatedOcrBlock>) {
        translatedBlocks = items
        selectedBlocks = emptyList()
        selectionRect = null
        invalidate()
    }

    fun clearTranslatedBlocks() {
        translatedBlocks = emptyList()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        renderedBlocks = mapBlocks()

        if (translatedBlocks.isNotEmpty()) {
            translatedBlocks.forEach { block ->
                val mappedRect = mapRect(block.bounds) ?: return@forEach
                drawTranslatedBlock(
                    canvas = canvas,
                    rect = mappedRect,
                    block = block
                )
            }
            return
        }

        renderedBlocks.forEach { (block, rect) ->
            if (selectedBlocks.contains(block)) {
                canvas.drawRoundRect(rect, 16f, 16f, fillPaint)
                canvas.drawRoundRect(rect, 16f, 16f, selectedStrokePaint)
            }
        }
        selectionRect?.let { rect ->
            canvas.drawRoundRect(rect, 16f, 16f, fillPaint)
            canvas.drawRoundRect(rect, 16f, 16f, selectionStrokePaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!interactionEnabled || translatedBlocks.isNotEmpty()) return false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                anchorBlock = findTouchedBlock(event.x, event.y)
                selectionRect = RectF(event.x, event.y, event.x, event.y)
                if (anchorBlock != null) {
                    selectedBlocks = listOf(anchorBlock!!)
                }
                parent?.requestDisallowInterceptTouchEvent(true)
                invalidate()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                selectionRect = RectF(downX, downY, event.x, event.y).normalized()
                if (anchorBlock != null) {
                    selectedBlocks = resolveSelection(anchorBlock!!, event.x, event.y)
                }
                invalidate()
                return true
            }

            MotionEvent.ACTION_UP -> {
                val movedDistance = hypot(event.x - downX, event.y - downY)
                val items = when {
                    anchorBlock != null && movedDistance < TAP_SLOP_DP * resources.displayMetrics.density -> {
                        listOf(anchorBlock!!)
                    }

                    anchorBlock != null -> {
                        resolveSelection(anchorBlock!!, event.x, event.y)
                    }

                    else -> {
                        emptyList()
                    }
                }
                selectionRect = null
                anchorBlock = null
                selectedBlocks = items
                invalidate()
                performClick()
                onSelectionChanged?.invoke(items)
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                selectionRect = null
                anchorBlock = null
                invalidate()
                return true
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

    private fun mapRect(sourceRect: RectF): RectF? {
        if (sourceWidth <= 0f || sourceHeight <= 0f || width == 0 || height == 0) {
            return null
        }
        val scale = min(width / sourceWidth, height / sourceHeight)
        val dx = (width - sourceWidth * scale) / 2f
        val dy = (height - sourceHeight * scale) / 2f
        return RectF(
            sourceRect.left * scale + dx,
            sourceRect.top * scale + dy,
            sourceRect.right * scale + dx,
            sourceRect.bottom * scale + dy
        )
    }

    private fun resolveSelection(anchor: OcrTextBlock, x: Float, y: Float): List<OcrTextBlock> {
        val anchorRect = renderedBlocks.firstOrNull { it.first == anchor }?.second
        val brushRect = if (anchorRect != null) {
            RectF(anchorRect.centerX(), anchorRect.centerY(), x, y).normalized()
        } else {
            selectionRect?.normalized()
        } ?: return listOf(anchor)

        val brushed = renderedBlocks
            .filter { (_, rect) -> isBlockInsideSelection(rect, brushRect) }
            .map { it.first }

        return if (brushed.isEmpty()) listOf(anchor) else sortByReadingOrder(brushed)
    }

    private fun isBlockInsideSelection(blockRect: RectF, selectionRect: RectF): Boolean {
        if (selectionRect.contains(blockRect.centerX(), blockRect.centerY())) {
            return true
        }

        val intersection = RectF(blockRect)
        val intersects = intersection.intersect(selectionRect)
        if (!intersects) {
            return false
        }

        val intersectionArea = intersection.width() * intersection.height()
        val blockArea = blockRect.width() * blockRect.height()
        if (blockArea <= 0f) {
            return false
        }
        return intersectionArea / blockArea >= MIN_INTERSECTION_RATIO
    }

    private fun findTouchedBlock(x: Float, y: Float): OcrTextBlock? {
        renderedBlocks.lastOrNull { (_, rect) -> rect.contains(x, y) }?.let { return it.first }

        val touchRadius = TOUCH_RADIUS_DP * resources.displayMetrics.density
        return renderedBlocks
            .map { (block, rect) ->
                val dx = when {
                    x < rect.left -> rect.left - x
                    x > rect.right -> x - rect.right
                    else -> 0f
                }
                val dy = when {
                    y < rect.top -> rect.top - y
                    y > rect.bottom -> y - rect.bottom
                    else -> 0f
                }
                block to hypot(dx, dy)
            }
            .filter { it.second <= touchRadius }
            .minByOrNull { it.second }
            ?.first
    }

    private fun sortByReadingOrder(items: List<OcrTextBlock>): List<OcrTextBlock> {
        val sortedByTop = items.sortedBy { it.bounds.top }
        val lines = mutableListOf<MutableList<OcrTextBlock>>()

        sortedByTop.forEach { block ->
            val match = lines.firstOrNull { line ->
                val reference = line.first()
                abs(reference.bounds.centerY() - block.bounds.centerY()) <=
                    maxOf(reference.bounds.height(), block.bounds.height()) * 0.7f
            }
            if (match == null) {
                lines.add(mutableListOf(block))
            } else {
                match.add(block)
            }
        }

        return lines
            .sortedBy { line -> line.minOf { it.bounds.top } }
            .flatMap { line -> line.sortedBy { it.bounds.left } }
    }

    private fun drawTranslatedBlock(canvas: Canvas, rect: RectF, block: TranslatedOcrBlock) {
        val density = resources.displayMetrics.density
        val cornerRadius = 4f * density
        val drawRect = RectF(rect).apply {
            inset(0.5f * density, 0.5f * density)
        }
        translatedFillPaint.color = block.backgroundColor
        translatedFillPaint.alpha = android.graphics.Color.alpha(block.backgroundColor)
        canvas.drawRoundRect(drawRect, cornerRadius, cornerRadius, translatedFillPaint)
        drawTranslatedText(
            canvas = canvas,
            rect = drawRect,
            text = block.translatedText.ifBlank { block.sourceText },
            textColor = block.textColor
        )
    }

    private fun drawTranslatedText(canvas: Canvas, rect: RectF, text: String, textColor: Int) {
        if (text.isBlank()) return

        val density = resources.displayMetrics.density
        val scaledDensity = resources.displayMetrics.scaledDensity
        val horizontalPadding = 4f * density
        val verticalPadding = 2.5f * density
        val contentWidth = (rect.width() - horizontalPadding * 2).toInt().coerceAtLeast(1)
        val contentHeight = rect.height() - verticalPadding * 2
        if (contentHeight <= 0f) return

        translatedTextPaint.color = textColor
        val maxTextSize = (rect.height() * 0.34f)
            .coerceIn(10.5f * scaledDensity, 17f * scaledDensity)
        val minTextSize = 7.5f * scaledDensity
        val staticLayout = buildBestTranslatedLayout(
            text = text,
            availableWidth = contentWidth,
            availableHeight = contentHeight,
            maxTextSize = maxTextSize,
            minTextSize = minTextSize
        )

        canvas.save()
        val textTop = rect.top + verticalPadding + maxOf(0f, (contentHeight - staticLayout.height) / 2f)
        canvas.translate(rect.left + horizontalPadding, textTop)
        staticLayout.draw(canvas)
        canvas.restore()
    }

    private fun buildBestTranslatedLayout(
        text: String,
        availableWidth: Int,
        availableHeight: Float,
        maxTextSize: Float,
        minTextSize: Float
    ): StaticLayout {
        val stepSize = 0.75f * resources.displayMetrics.scaledDensity
        var textSize = maxTextSize
        var fallbackLayout: StaticLayout? = null

        while (textSize >= minTextSize) {
            translatedTextPaint.textSize = textSize
            val lineHeightEstimate = translatedTextPaint.textSize * 1.18f
            val maxLines = (availableHeight / lineHeightEstimate).toInt().coerceIn(1, 2)
            val layout = StaticLayout.Builder
                .obtain(text, 0, text.length, translatedTextPaint, availableWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setIncludePad(false)
                .setEllipsize(TextUtils.TruncateAt.END)
                .setMaxLines(maxLines)
                .build()

            val hasEllipsis = (0 until layout.lineCount).any { layout.getEllipsisCount(it) > 0 }
            fallbackLayout = layout
            if (!hasEllipsis && layout.height <= availableHeight) {
                return layout
            }
            textSize -= stepSize
        }

        translatedTextPaint.textSize = minTextSize
        return fallbackLayout ?: StaticLayout.Builder
            .obtain(text, 0, text.length, translatedTextPaint, availableWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setIncludePad(false)
            .setEllipsize(TextUtils.TruncateAt.END)
            .setMaxLines(1)
            .build()
    }

    private fun RectF.normalized(): RectF {
        return RectF(
            min(left, right),
            min(top, bottom),
            maxOf(left, right),
            maxOf(top, bottom)
        )
    }

    private companion object {
        const val TAP_SLOP_DP = 12
        const val TOUCH_RADIUS_DP = 22
        const val MIN_INTERSECTION_RATIO = 0.65f
    }
}
