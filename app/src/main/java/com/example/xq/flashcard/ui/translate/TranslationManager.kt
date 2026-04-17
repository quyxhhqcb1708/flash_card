package com.example.xq.flashcard.ui.translate

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions

class TranslationManager {

    private val translators = mutableMapOf<Pair<String, String>, Translator>()

    fun translateText(
        text: String,
        sourceLanguage: AppTranslationLanguage,
        targetLanguage: AppTranslationLanguage,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        if (text.isBlank()) {
            onSuccess("")
            return
        }
        if (sourceLanguage == targetLanguage) {
            onSuccess(text)
            return
        }
        val translator = getTranslator(sourceLanguage, targetLanguage)
        translator.downloadModelIfNeeded(DownloadConditions.Builder().build())
            .addOnSuccessListener {
                translator.translate(text)
                    .addOnSuccessListener(onSuccess)
                    .addOnFailureListener(onError)
            }
            .addOnFailureListener(onError)
    }

    fun close() {
        translators.values.forEach { it.close() }
        translators.clear()
    }

    private fun getTranslator(
        sourceLanguage: AppTranslationLanguage,
        targetLanguage: AppTranslationLanguage
    ): Translator {
        val key = sourceLanguage.mlKitCode to targetLanguage.mlKitCode
        return translators.getOrPut(key) {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(sourceLanguage.mlKitCode)
                .setTargetLanguage(targetLanguage.mlKitCode)
                .build()
            Translation.getClient(options)
        }
    }
}
