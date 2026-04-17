package com.example.xq.flashcard.ui.scan

import android.graphics.RectF

data class OcrTextBlock(
    val text: String,
    val bounds: RectF
)

data class RecognizedTextPayload(
    val fullText: String,
    val blocks: List<OcrTextBlock>,
    val imageWidth: Int,
    val imageHeight: Int
)
