package com.example.xq.flashcard.ui.scan

import android.graphics.RectF
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object OcrTextGrouping {

    fun buildTranslatedGroups(blocks: List<OcrTextBlock>): List<OcrTextBlock> {
        if (blocks.isEmpty()) return emptyList()

        val visualLines = groupVisualLines(blocks)
        return visualLines.flatMap { splitLineIntoSegments(it, blocks) }
    }

    private fun groupVisualLines(blocks: List<OcrTextBlock>): List<List<OcrTextBlock>> {
        val sortedByTop = blocks.sortedBy { it.bounds.top }
        val lines = mutableListOf<MutableList<OcrTextBlock>>()

        sortedByTop.forEach { block ->
            val match = lines.firstOrNull { line ->
                val reference = line.first()
                abs(reference.bounds.centerY() - block.bounds.centerY()) <=
                    max(reference.bounds.height(), block.bounds.height()) * 0.68f
            }
            if (match == null) {
                lines.add(mutableListOf(block))
            } else {
                match.add(block)
            }
        }

        return lines
            .sortedBy { line -> line.minOf { it.bounds.top } }
            .map { line -> line.sortedBy { it.bounds.left } }
    }

    private fun splitLineIntoSegments(
        line: List<OcrTextBlock>,
        allBlocks: List<OcrTextBlock>
    ): List<OcrTextBlock> {
        if (line.isEmpty()) return emptyList()

        val contentBounds = mergeBounds(allBlocks)
        val maxVisualWidth = contentBounds.width().coerceAtLeast(1f) * 0.72f
        val segments = mutableListOf<MutableList<OcrTextBlock>>()
        var currentSegment = mutableListOf(line.first())

        line.drop(1).forEach { block ->
            val previous = currentSegment.last()
            val currentBounds = mergeBounds(currentSegment)
            val averageWidth = currentSegment.map { it.bounds.width() }.average().toFloat().coerceAtLeast(1f)
            val averageHeight = currentSegment.map { it.bounds.height() }.average().toFloat().coerceAtLeast(1f)
            val horizontalGap = (block.bounds.left - previous.bounds.right).coerceAtLeast(0f)
            val shouldSplit = horizontalGap > max(averageHeight * 0.95f, averageWidth * 0.62f) ||
                (currentBounds.width() > maxVisualWidth && horizontalGap > averageHeight * 0.3f) ||
                (previous.text.endsWithAny('.', '!', '?', ':', ';') && horizontalGap > averageHeight * 0.2f)

            if (shouldSplit) {
                segments.add(currentSegment)
                currentSegment = mutableListOf(block)
            } else {
                currentSegment.add(block)
            }
        }
        segments.add(currentSegment)

        return segments.map { buildMergedBlock(it) }
    }

    private fun buildMergedBlock(items: List<OcrTextBlock>): OcrTextBlock {
        val mergedBounds = mergeBounds(items)
        return OcrTextBlock(
            text = items.joinToString(" ") { it.text.trim() }.trim(),
            bounds = mergedBounds
        )
    }

    private fun mergeBounds(items: List<OcrTextBlock>): RectF {
        val mergedBounds = RectF(items.first().bounds)
        items.drop(1).forEach { mergedBounds.union(it.bounds) }
        return mergedBounds
    }

    private fun String.endsWithAny(vararg chars: Char): Boolean {
        val last = trim().lastOrNull() ?: return false
        return chars.any { it == last }
    }
}
