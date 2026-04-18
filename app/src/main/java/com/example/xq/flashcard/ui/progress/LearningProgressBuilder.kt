package com.example.xq.flashcard.ui.progress

import android.content.Context
import com.example.xq.flashcard.ui.library.FlashCardCollection
import com.example.xq.flashcard.ui.library.FlashCardLibraryStore
import com.example.xq.flashcard.ui.practice.Sm2Scheduler

data class LearningDistribution(
    val dueCount: Int,
    val learningCount: Int,
    val upcomingCount: Int,
    val stableCount: Int
)

data class CollectionProgressSummary(
    val collectionId: Long,
    val name: String,
    val totalCards: Int,
    val practicedCount: Int,
    val dueTodayCount: Int,
    val learningCount: Int,
    val upcomingCount: Int,
    val masteredCount: Int,
    val accuracyRate: Float,
    val masteryRate: Float,
    val updatedAt: Long
)

data class LearningProgressDashboard(
    val totalSavedCount: Int,
    val practicedCount: Int,
    val masteredCount: Int,
    val dueTodayCount: Int,
    val averageAccuracyRate: Float,
    val totalCollections: Int,
    val distribution: LearningDistribution,
    val collections: List<CollectionProgressSummary>
)

object LearningProgressBuilder {

    fun build(context: Context, now: Long = System.currentTimeMillis()): LearningProgressDashboard {
        val collections = FlashCardLibraryStore.getCollections(context)
        val allCards = collections.flatMap { it.cards }
        val totalPractice = allCards.sumOf { it.practiceCount }
        val totalCorrect = allCards.sumOf { it.correctCount }
        val collectionSummaries = collections
            .map { buildCollectionSummary(it, now) }
            .sortedWith(
                compareByDescending<CollectionProgressSummary> { it.dueTodayCount }
                    .thenByDescending { it.learningCount }
                    .thenBy { it.masteryRate }
                    .thenByDescending { it.updatedAt }
            )

        return LearningProgressDashboard(
            totalSavedCount = allCards.size,
            practicedCount = allCards.count { it.practiceCount > 0 },
            masteredCount = allCards.count(Sm2Scheduler::isMastered),
            dueTodayCount = allCards.count { Sm2Scheduler.isDueToday(it, now) },
            averageAccuracyRate = if (totalPractice > 0) {
                totalCorrect.toFloat() / totalPractice.toFloat()
            } else {
                0f
            },
            totalCollections = collections.size,
            distribution = buildDistribution(allCards, now),
            collections = collectionSummaries
        )
    }

    private fun buildDistribution(
        cards: List<com.example.xq.flashcard.ui.library.FlashCardItem>,
        now: Long
    ): LearningDistribution {
        var dueCount = 0
        var learningCount = 0
        var upcomingCount = 0
        var stableCount = 0

        cards.forEach { card ->
            when {
                Sm2Scheduler.isDueToday(card, now) -> dueCount += 1
                Sm2Scheduler.isLearning(card) -> learningCount += 1
                Sm2Scheduler.isMastered(card) -> stableCount += 1
                else -> upcomingCount += 1
            }
        }

        return LearningDistribution(
            dueCount = dueCount,
            learningCount = learningCount,
            upcomingCount = upcomingCount,
            stableCount = stableCount
        )
    }

    private fun buildCollectionSummary(
        collection: FlashCardCollection,
        now: Long
    ): CollectionProgressSummary {
        val cards = collection.cards
        val totalPractice = cards.sumOf { it.practiceCount }
        val totalCorrect = cards.sumOf { it.correctCount }
        val dueTodayCount = cards.count { Sm2Scheduler.isDueToday(it, now) }
        val learningCount = cards.count {
            !Sm2Scheduler.isDueToday(it, now) && Sm2Scheduler.isLearning(it)
        }
        val masteredCount = cards.count(Sm2Scheduler::isMastered)
        val upcomingCount = cards.count {
            !Sm2Scheduler.isDueToday(it, now) &&
                !Sm2Scheduler.isLearning(it) &&
                !Sm2Scheduler.isMastered(it)
        }

        return CollectionProgressSummary(
            collectionId = collection.id,
            name = collection.name,
            totalCards = cards.size,
            practicedCount = cards.count { it.practiceCount > 0 },
            dueTodayCount = dueTodayCount,
            learningCount = learningCount,
            upcomingCount = upcomingCount,
            masteredCount = masteredCount,
            accuracyRate = if (totalPractice > 0) {
                totalCorrect.toFloat() / totalPractice.toFloat()
            } else {
                0f
            },
            masteryRate = if (cards.isNotEmpty()) {
                masteredCount.toFloat() / cards.size.toFloat()
            } else {
                0f
            },
            updatedAt = collection.updatedAt
        )
    }
}
