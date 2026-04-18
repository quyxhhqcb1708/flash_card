package com.example.xq.flashcard.ui.library

import com.example.xq.flashcard.R

enum class FlashCardDifficulty(
    val persistedValue: String,
    val labelRes: Int,
    val priority: Int
) {
    EASY("easy", R.string.library_difficulty_easy, 0),
    MEDIUM("medium", R.string.library_difficulty_medium, 1),
    HARD("hard", R.string.library_difficulty_hard, 2);

    companion object {
        fun fromValue(value: String?): FlashCardDifficulty {
            return entries.firstOrNull { it.persistedValue == value } ?: MEDIUM
        }
    }
}
