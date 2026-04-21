package com.example.xq.flashcard.ui.library

import android.content.Context
import com.example.xq.flashcard.R
import com.example.xq.flashcard.translate.AppTranslationLanguage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FlashCardDisplayFormatter {

    fun formatSavedAt(value: Long): String {
        return SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(value))
    }

    fun getLanguagePair(context: Context, item: FlashCardItem): String {
        val sourceLabel = context.getString(AppTranslationLanguage.fromCode(item.sourceLanguageCode).labelRes)
        val targetLabel = context.getString(AppTranslationLanguage.fromCode(item.targetLanguageCode).labelRes)
        return context.getString(R.string.main_card_language_pair, sourceLabel, targetLabel)
    }
}
