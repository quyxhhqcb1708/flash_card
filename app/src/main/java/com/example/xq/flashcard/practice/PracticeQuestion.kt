package com.example.xq.flashcard.ui.practice

import java.io.Serializable

data class PracticeQuestion(
    val collectionId: Long,
    val cardId: Long,
    val collectionName: String,
    val prompt: String,
    val promptLanguageCode: String,
    val correctAnswer: String,
    val secondaryText: String,
    val languagePair: String,
    val options: List<String>
) : Serializable
