package com.example.xq.flashcard.ui.practice

import com.example.xq.flashcard.ui.library.FlashCardDifficulty
import com.example.xq.flashcard.ui.library.FlashCardItem

object PracticeReviewScorer {

    fun qualityForAnswer(card: FlashCardItem, isCorrect: Boolean): Int {
        if (!isCorrect) return 2

        val wrongCount = (card.practiceCount - card.correctCount).coerceAtLeast(0)
        val errorRate = if (card.practiceCount <= 0) {
            0.0
        } else {
            wrongCount.toDouble() / card.practiceCount.toDouble()
        }

        var quality = when (card.manualDifficulty) {
            FlashCardDifficulty.EASY -> 5
            FlashCardDifficulty.MEDIUM -> 4
            FlashCardDifficulty.HARD -> 3
        }

        if (errorRate > 0.50 || card.easinessFactor < 1.7) {
            quality = minOf(quality, 3)
        } else if (errorRate > 0.25 || card.easinessFactor < 2.0) {
            quality = minOf(quality, 4)
        }

        if (Sm2Scheduler.isMastered(card) && card.manualDifficulty == FlashCardDifficulty.EASY && errorRate < 0.15) {
            quality = 5
        }

        return quality.coerceIn(3, 5)
    }
}
