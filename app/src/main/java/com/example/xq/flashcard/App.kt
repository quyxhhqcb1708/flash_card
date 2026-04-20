package com.example.xq.flashcard

import android.app.Application
import com.example.xq.flashcard.utils.locale.AppLanguageManager
import com.example.xq.flashcard.utils.sharedpreference.SharePreferUtils

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        SharePreferUtils.init(this)
        AppLanguageManager.initialize(this)
    }
}
