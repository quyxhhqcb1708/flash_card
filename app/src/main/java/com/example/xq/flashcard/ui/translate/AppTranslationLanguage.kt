package com.example.xq.flashcard.ui.translate

import androidx.annotation.StringRes
import com.example.xq.flashcard.R
import com.google.mlkit.nl.translate.TranslateLanguage

enum class AppTranslationLanguage(
    val mlKitCode: String,
    @StringRes val labelRes: Int
) {
    ENGLISH(TranslateLanguage.ENGLISH, R.string.scan_language_english),
    VIETNAMESE(TranslateLanguage.VIETNAMESE, R.string.scan_language_vietnamese);

    companion object {
        fun fromCode(code: String?): AppTranslationLanguage {
            return entries.firstOrNull { it.mlKitCode == code } ?: ENGLISH
        }
    }
}
