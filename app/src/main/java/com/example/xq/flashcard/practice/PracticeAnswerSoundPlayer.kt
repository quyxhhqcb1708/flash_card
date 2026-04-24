package com.example.xq.flashcard.ui.practice

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.example.xq.flashcard.R
import com.example.xq.flashcard.ui.settings.AppSettingsStore

class PracticeAnswerSoundPlayer(context: Context) {

    private val appContext = context.applicationContext
    private val loadedSoundIds = mutableSetOf<Int>()
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()
    private val correctSoundId = soundPool.load(appContext, R.raw.tra_loi_dung, 1)
    private val wrongSoundId = soundPool.load(appContext, R.raw.tra_loi_sai, 1)

    private var pendingSoundId: Int? = null
    private var isReleased = false

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status != 0 || isReleased) return@setOnLoadCompleteListener
            loadedSoundIds += sampleId
            if (pendingSoundId == sampleId) {
                play(sampleId)
                pendingSoundId = null
            }
        }
    }

    fun playAnswerFeedback(isCorrect: Boolean) {
        if (isReleased || !AppSettingsStore.isSoundEnabled(appContext)) return

        val soundId = if (isCorrect) correctSoundId else wrongSoundId
        if (soundId in loadedSoundIds) {
            play(soundId)
        } else {
            pendingSoundId = soundId
        }
    }

    fun release() {
        if (isReleased) return
        isReleased = true
        loadedSoundIds.clear()
        pendingSoundId = null
        soundPool.release()
    }

    private fun play(soundId: Int) {
        if (isReleased || !AppSettingsStore.isSoundEnabled(appContext)) return
        soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
    }
}
