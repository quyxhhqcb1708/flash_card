package com.example.xq.flashcard.ui.practice

import android.content.Context
import com.example.xq.flashcard.ui.library.FlashCardLibraryStore
import com.example.xq.flashcard.ui.library.RecentFlashCardEntry

data class PracticeDayBucket(
    val title: String,
    val entries: List<RecentFlashCardEntry>
)

data class PracticeDashboard(
    val totalCards: Int,
    val dueTodayCount: Int,
    val learningCount: Int,
    val upcomingCount: Int,
    val masteredCount: Int,
    val buckets: List<PracticeDayBucket>
)

object PracticeOverviewBuilder {

    fun build(context: Context, now: Long = System.currentTimeMillis()): PracticeDashboard {
        val entries = FlashCardLibraryStore.getAllCards(context)
        val sortedEntries = entries.sortedWith(reviewComparator(now))
        val dueToday = sortedEntries.filter { Sm2Scheduler.isDueToday(it.card, now) }
        val buckets = sortedEntries
            .groupBy { getBucketKey(it.card.nextReviewAt, now) }
            .toSortedMap()
            .map { (key, bucketEntries) ->
                PracticeDayBucket(
                    title = PracticeUiFormatter.formatBucketTitle(context, key, now),
                    entries = bucketEntries
                )
            }

        return PracticeDashboard(
            totalCards = entries.size,
            dueTodayCount = dueToday.size,
            learningCount = entries.count { Sm2Scheduler.isLearning(it.card) },
            upcomingCount = entries.count {
                !Sm2Scheduler.isDueToday(it.card, now) && !Sm2Scheduler.isMastered(it.card)
            },
            masteredCount = entries.count { Sm2Scheduler.isMastered(it.card) },
            buckets = buckets
        )
    }

    fun buildReviewQueue(
        context: Context,
        collectionId: Long? = null,
        dueOnly: Boolean,
        now: Long = System.currentTimeMillis()
    ): List<RecentFlashCardEntry> {
        val allEntries = FlashCardLibraryStore.getAllCards(context)
            .filter { collectionId == null || it.collectionId == collectionId }
            .sortedWith(reviewComparator(now))

        if (!dueOnly) {
            return allEntries
        }

        val dueEntries = allEntries.filter { Sm2Scheduler.isDueToday(it.card, now) }
        return if (dueEntries.isNotEmpty()) dueEntries else allEntries
    }

    private fun reviewComparator(now: Long): Comparator<RecentFlashCardEntry> {
        return compareBy<RecentFlashCardEntry>(
            { getBucketKey(it.card.nextReviewAt, now) },
            { if (it.card.nextReviewAt == 0L) Long.MIN_VALUE else it.card.nextReviewAt },
            { -it.card.manualDifficulty.priority },
            { it.card.lastReviewQuality.takeIf { quality -> quality >= 0 } ?: Int.MAX_VALUE },
            { it.collectionName.lowercase() },
            { it.card.term.lowercase() }
        )
    }

    private fun getBucketKey(nextReviewAt: Long, now: Long): Int {
        if (nextReviewAt == 0L) return 0
        return Sm2Scheduler.dayOffset(nextReviewAt, now).coerceAtLeast(0)
    }
}
