package com.example.xq.flashcard.ui.scan

import android.graphics.Bitmap
import android.graphics.RectF
import android.media.Image
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class TextRecognitionCoordinator {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    fun processBitmap(
        bitmap: Bitmap,
        onSuccess: (RecognizedTextPayload) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val image = InputImage.fromBitmap(bitmap, 0)
        processImage(image, bitmap.width, bitmap.height, onSuccess, onError)
    }

    fun processMediaImage(
        mediaImage: Image,
        rotationDegrees: Int,
        onSuccess: (RecognizedTextPayload) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val image = InputImage.fromMediaImage(mediaImage, rotationDegrees)
        val width = if (rotationDegrees == 90 || rotationDegrees == 270) {
            mediaImage.height
        } else {
            mediaImage.width
        }
        val height = if (rotationDegrees == 90 || rotationDegrees == 270) {
            mediaImage.width
        } else {
            mediaImage.height
        }
        processImage(image, width, height, onSuccess, onError)
    }

    fun close() {
        recognizer.close()
    }

    private fun processImage(
        image: InputImage,
        width: Int,
        height: Int,
        onSuccess: (RecognizedTextPayload) -> Unit,
        onError: (Exception) -> Unit
    ) {
        recognizer.process(image)
            .addOnSuccessListener { result ->
                val blocks = result.textBlocks
                    .flatMap { block ->
                        block.lines.flatMap { line ->
                            if (line.elements.isNotEmpty()) {
                                line.elements.mapNotNull { element ->
                                    val box = element.boundingBox ?: return@mapNotNull null
                                    val text = element.text.trim()
                                    if (text.isBlank()) {
                                        null
                                    } else {
                                        OcrTextBlock(text = text, bounds = RectF(box))
                                    }
                                }
                            } else {
                                listOfNotNull(
                                    line.boundingBox?.let { box ->
                                        val text = line.text.trim()
                                        if (text.isBlank()) {
                                            null
                                        } else {
                                            OcrTextBlock(text = text, bounds = RectF(box))
                                        }
                                    }
                                )
                            }
                        }
                    }
                onSuccess(
                    RecognizedTextPayload(
                        fullText = result.text.trim(),
                        blocks = blocks,
                        imageWidth = width,
                        imageHeight = height
                    )
                )
            }
            .addOnFailureListener(onError)
    }
}
