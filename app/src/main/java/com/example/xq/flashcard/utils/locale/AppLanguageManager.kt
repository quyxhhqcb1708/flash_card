package com.example.xq.flashcard.utils.locale

import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.xq.flashcard.utils.sharedpreference.SharePreferUtils
import java.util.Locale

object AppLanguageManager {

    fun initialize(context: Context) {
        SharePreferUtils.init(context)
        val resolvedCode = resolveStoredOrSystemLanguage(context)
        SharePreferUtils.setLanguageCode(resolvedCode)
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(resolvedCode))
    }

    fun resolveStoredOrSystemLanguage(context: Context): String {
        SharePreferUtils.init(context)
        val storedCode = SharePreferUtils.getLanguageCode(context)
        if (storedCode.isNotBlank()) {
            return normalizeLanguageCode(storedCode)
        }
        return resolveSystemLanguage(context)
    }

    fun getCurrentOption(context: Context): AppLanguageOption {
        return AppLanguageOption.fromCode(resolveStoredOrSystemLanguage(context))
    }

    fun applyLanguage(context: Context, code: String): Boolean {
        SharePreferUtils.init(context)
        val normalizedCode = normalizeLanguageCode(code)
        val currentCode = resolveStoredOrSystemLanguage(context)
        if (currentCode == normalizedCode) return false
        SharePreferUtils.setLanguageCode(normalizedCode)
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(normalizedCode))
        return true
    }

    private fun resolveSystemLanguage(context: Context): String {
        val language = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0]?.language.orEmpty()
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale.language.orEmpty()
        }
        return normalizeLanguageCode(language)
    }

    private fun normalizeLanguageCode(value: String): String {
        return if (value.trim().lowercase(Locale.US).startsWith("vi")) {
            AppLanguageOption.VIETNAMESE.code
        } else {
            AppLanguageOption.ENGLISH.code
        }
    }
}
