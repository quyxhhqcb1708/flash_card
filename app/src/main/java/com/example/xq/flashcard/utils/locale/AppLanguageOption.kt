package com.example.xq.flashcard.utils.locale

import androidx.annotation.StringRes
import com.example.xq.flashcard.R

enum class AppLanguageOption(
    val code: String,
    @StringRes val labelRes: Int
) {
    ENGLISH("en", R.string.setting_language_option_english),
    VIETNAMESE("vi", R.string.setting_language_option_vietnamese);

    companion object {
        fun fromCode(value: String): AppLanguageOption {
            val normalized = value.trim().lowercase()
            return entries.firstOrNull { it.code == normalized } ?: ENGLISH
        }
    }
}
