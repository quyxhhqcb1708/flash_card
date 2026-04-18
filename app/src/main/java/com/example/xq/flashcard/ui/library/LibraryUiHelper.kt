package com.example.xq.flashcard.ui.library

import com.example.xq.flashcard.R

object LibraryUiHelper {

    private val coverBackgrounds = listOf(
        R.drawable.bg_library_cover_pink,
        R.drawable.bg_library_cover_gold,
        R.drawable.bg_library_cover_blue,
        R.drawable.bg_library_cover_green
    )

    fun getCoverBackgroundRes(collectionId: Long, collectionName: String): Int {
        val stableSeed = (collectionId.hashCode() + collectionName.hashCode()).let { if (it < 0) -it else it }
        return coverBackgrounds[stableSeed % coverBackgrounds.size]
    }

    fun getCollectionInitial(name: String): String {
        return name.trim().firstOrNull()?.uppercase() ?: "F"
    }

    fun getSummaryProgressText(summary: FlashCardPracticeSummary): String {
        return "${summary.readyToReviewCards} ready / ${summary.masteredCards} mastered"
    }

    fun getMasteryPercent(card: FlashCardItem): Int {
        if (card.practiceCount <= 0) return 0
        return (FlashCardLibraryStore.getMasteryRate(card) * 100).toInt()
    }
}
