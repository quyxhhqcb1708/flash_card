package com.example.xq.flashcard.ui.practice

import android.content.Context
import com.example.xq.flashcard.ui.library.FlashCardDisplayFormatter
import com.example.xq.flashcard.ui.library.FlashCardDifficulty
import com.example.xq.flashcard.ui.library.RecentFlashCardEntry
import kotlin.math.max
import kotlin.random.Random

object PracticeSessionBuilder {

    private const val QUESTION_COUNT = 15

    fun buildSession(
        context: Context,
        collectionId: Long? = null,
        dueOnly: Boolean,
        now: Long = System.currentTimeMillis()
    ): List<PracticeQuestion> {
        val globalEntries = PracticeOverviewBuilder.buildReviewQueue(
            context = context,
            collectionId = null,
            dueOnly = false,
            now = now
        )
        val promptPool = PracticeOverviewBuilder.buildReviewQueue(
            context = context,
            collectionId = collectionId,
            dueOnly = false,
            now = now
        )
        if (globalEntries.size < 4 || promptPool.isEmpty()) return emptyList()

        val dueEntries = promptPool.filter { Sm2Scheduler.isDueToday(it.card, now) }
        val primaryPool = when {
            dueOnly && dueEntries.isNotEmpty() -> dueEntries
            else -> promptPool
        }

        val selectedCounts = mutableMapOf<Long, Int>()
        val questions = mutableListOf<PracticeQuestion>()
        val maxRepeatsPerCard = when {
            promptPool.size <= 5 -> 4
            promptPool.size <= 10 -> 3
            else -> 2
        }

        var attempts = 0
        while (questions.size < QUESTION_COUNT && attempts < QUESTION_COUNT * 6) {
            attempts += 1
            val candidates = primaryPool.ifEmpty { promptPool }
                .filter { (selectedCounts[it.card.id] ?: 0) < maxRepeatsPerCard }
                .ifEmpty { promptPool }
            val chosen = pickWeightedCandidate(candidates, selectedCounts, now, dueOnly)
            val options = buildOptions(chosen, globalEntries)
            if (options.size < 4) continue

            questions += PracticeQuestion(
                collectionId = chosen.collectionId,
                cardId = chosen.card.id,
                collectionName = chosen.collectionName,
                prompt = chosen.card.term,
                promptLanguageCode = chosen.card.sourceLanguageCode,
                correctAnswer = chosen.card.definition,
                secondaryText = chosen.card.definition,
                languagePair = FlashCardDisplayFormatter.getLanguagePair(context, chosen.card),
                options = options
            )
            selectedCounts[chosen.card.id] = (selectedCounts[chosen.card.id] ?: 0) + 1
        }

        return questions
    }

    private fun pickWeightedCandidate(
        candidates: List<RecentFlashCardEntry>,
        selectedCounts: Map<Long, Int>,
        now: Long,
        dueOnly: Boolean
    ): RecentFlashCardEntry {
        val weighted = candidates.map { entry ->
            val alreadySelected = selectedCounts[entry.card.id] ?: 0
            val effectiveWeight = baseWeight(entry, now, dueOnly) / (1.0 + alreadySelected * 1.7)
            entry to effectiveWeight
        }
        val totalWeight = weighted.sumOf { it.second }
        if (totalWeight <= 0.0) return candidates.random()

        var cursor = Random.nextDouble(totalWeight)
        weighted.forEach { (entry, weight) ->
            cursor -= weight
            if (cursor <= 0.0) return entry
        }
        return weighted.last().first
    }

    private fun baseWeight(entry: RecentFlashCardEntry, now: Long, dueOnly: Boolean): Double {
        val card = entry.card
        val wrongCount = (card.practiceCount - card.correctCount).coerceAtLeast(0)
        val errorRate = if (card.practiceCount <= 0) 0.45 else wrongCount.toDouble() / card.practiceCount.toDouble()
        val dueBoost = when {
            Sm2Scheduler.isDueToday(card, now) -> if (dueOnly) 7.0 else 4.5
            dueOnly -> 0.4
            card.nextReviewAt == 0L -> 3.2
            else -> when (val offset = Sm2Scheduler.dayOffset(card.nextReviewAt, now)) {
                1 -> 2.4
                in 2..3 -> 1.6
                else -> 0.8 / max(1, offset)
            }
        }
        val difficultyBoost = when (card.manualDifficulty) {
            FlashCardDifficulty.HARD -> 3.0
            FlashCardDifficulty.MEDIUM -> 1.8
            FlashCardDifficulty.EASY -> 0.7
        }
        val efBoost = ((2.5 - card.easinessFactor).coerceAtLeast(0.0)) * 2.2
        val qualityBoost = when (card.lastReviewQuality) {
            in 0..2 -> 1.6
            3 -> 0.9
            4 -> 0.3
            else -> 0.0
        }
        val newCardBoost = if (card.practiceCount == 0) 2.8 else 0.0
        val masteredPenalty = if (Sm2Scheduler.isMastered(card)) 0.45 else 1.0
        val stableEasyPenalty = if (
            card.manualDifficulty == FlashCardDifficulty.EASY &&
            wrongCount == 0 &&
            card.correctCount >= 3
        ) 0.65 else 1.0

        return ((1.0 + dueBoost + difficultyBoost + efBoost + qualityBoost + newCardBoost + errorRate * 4.2)
            * masteredPenalty * stableEasyPenalty).coerceAtLeast(0.15)
    }

    private fun buildOptions(
        chosen: RecentFlashCardEntry,
        globalEntries: List<RecentFlashCardEntry>
    ): List<String> {
        val distractors = globalEntries
            .filterNot { it.card.id == chosen.card.id }
            .map { it.card.definition.trim() }
            .filter { it.isNotBlank() && !it.equals(chosen.card.definition, ignoreCase = true) }
            .distinctBy { it.lowercase() }
            .shuffled()
            .take(3)
            .toMutableList()

        if (distractors.size < 3) return emptyList()
        distractors += chosen.card.definition
        return distractors.shuffled()
    }
}
