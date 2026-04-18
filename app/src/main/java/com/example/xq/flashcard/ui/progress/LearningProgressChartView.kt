package com.example.xq.flashcard.ui.progress

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.graphics.withSave
import kotlin.math.max
import kotlin.math.min

data class ProgressChartEntry(
    val label: String,
    val value: Int,
    val color: Int
)

class LearningProgressChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val entries = mutableListOf<ProgressChartEntry>()
    private val barTrackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFF0F2F8")
        style = Paint.Style.FILL
    }
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val valuePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF2A2A33")
        textAlign = Paint.Align.CENTER
        textSize = 13f.sp()
        typeface = Typeface.DEFAULT_BOLD
    }
    private val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF7A7E89")
        textAlign = Paint.Align.CENTER
        textSize = 12f.sp()
    }
    private val minChartHeight = 212f.dp().toInt()
    private val barCornerRadius = 14f.dp()
    private val valueGap = 10f.dp()
    private val labelGap = 18f.dp()
    private val minBarWidth = 28f.dp()
    private val maxBarWidth = 42f.dp()

    fun setEntries(values: List<ProgressChartEntry>) {
        entries.clear()
        entries.addAll(values)
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredHeight = paddingTop + paddingBottom + minChartHeight
        val resolvedHeight = resolveSize(desiredHeight, heightMeasureSpec)
        setMeasuredDimension(resolveSize(suggestedMinimumWidth, widthMeasureSpec), resolvedHeight)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (entries.isEmpty()) return

        val maxValue = max(entries.maxOfOrNull { it.value } ?: 0, 1)
        val contentLeft = paddingLeft.toFloat()
        val contentRight = width - paddingRight.toFloat()
        val contentTop = paddingTop.toFloat()
        val contentBottom = height - paddingBottom.toFloat()
        val valueBaseline = contentTop + valuePaint.textSize
        val labelsAreaHeight = labelPaint.textSize + labelGap
        val chartTop = valueBaseline + valueGap
        val chartBottom = contentBottom - labelsAreaHeight
        val chartHeight = (chartBottom - chartTop).coerceAtLeast(0f)
        val slotWidth = (contentRight - contentLeft) / entries.size.toFloat()
        val barWidth = min(maxBarWidth, max(minBarWidth, slotWidth * 0.48f))

        entries.forEachIndexed { index, entry ->
            val centerX = contentLeft + slotWidth * index + slotWidth / 2f
            val barLeft = centerX - barWidth / 2f
            val barRight = centerX + barWidth / 2f
            val ratio = entry.value / maxValue.toFloat()
            val barHeight = chartHeight * ratio
            val barTop = chartBottom - barHeight
            val trackRect = RectF(barLeft, chartTop, barRight, chartBottom)
            val valueY = chartTop - 4f.dp()
            val labelY = contentBottom - 6f.dp()

            canvas.drawRoundRect(trackRect, barCornerRadius, barCornerRadius, barTrackPaint)
            if (entry.value > 0) {
                barPaint.color = entry.color
                canvas.drawRoundRect(
                    RectF(barLeft, barTop, barRight, chartBottom),
                    barCornerRadius,
                    barCornerRadius,
                    barPaint
                )
            }
            canvas.drawText(entry.value.toString(), centerX, valueY, valuePaint)
            canvas.withSave {
                drawText(entry.label, centerX, labelY, labelPaint)
            }
        }
    }

    private fun Float.dp(): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            this,
            resources.displayMetrics
        )
    }

    private fun Float.sp(): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            this,
            resources.displayMetrics
        )
    }
}
