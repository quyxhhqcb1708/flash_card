package com.example.xq.flashcard.speech

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import java.util.Locale

class EnglishTextSpeaker(context: Context) {

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val textToSpeech = TextToSpeech(appContext) { status ->
        mainHandler.post {
            handleInitialization(status)
        }
    }

    private var isInitialized = false
    private var isEnglishSupported = false
    private var isReleased = false
    private var pendingText: String? = null

    private fun handleInitialization(status: Int) {
        if (isReleased) return
        isInitialized = true
        if (status != TextToSpeech.SUCCESS) {
            isEnglishSupported = false
            pendingText = null
            return
        }

        val preferredLocale = when {
            textToSpeech.isLanguageAvailable(Locale.US) >= TextToSpeech.LANG_AVAILABLE -> Locale.US
            textToSpeech.isLanguageAvailable(Locale.UK) >= TextToSpeech.LANG_AVAILABLE -> Locale.UK
            else -> null
        }

        if (preferredLocale == null) {
            isEnglishSupported = false
            pendingText = null
            return
        }

        val result = textToSpeech.setLanguage(preferredLocale)
        isEnglishSupported = result != TextToSpeech.LANG_MISSING_DATA &&
            result != TextToSpeech.LANG_NOT_SUPPORTED

        pendingText?.takeIf { isEnglishSupported }?.let(::speakNow)
        pendingText = null
    }

    fun speak(text: String): Boolean {
        if (isReleased) return false
        val normalizedText = text.trim()
        if (normalizedText.isBlank()) return false

        if (!isInitialized) {
            pendingText = normalizedText
            return true
        }

        if (!isEnglishSupported) {
            return false
        }

        speakNow(normalizedText)
        return true
    }

    fun release() {
        if (isReleased) return
        isReleased = true
        pendingText = null
        textToSpeech.stop()
        textToSpeech.shutdown()
    }

    private fun speakNow(text: String) {
        textToSpeech.stop()
        textToSpeech.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "english_tts_${text.hashCode()}"
        )
    }
}
