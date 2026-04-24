package com.example.xq.flashcard.ui.login

import android.content.Context
import com.example.xq.flashcard.utils.sharedpreference.SharePreferUtils

object GuestSessionStore {

    private const val KEY_GUEST_MODE_ENABLED = "guest_mode_enabled"

    fun isGuestMode(context: Context): Boolean {
        SharePreferUtils.init(context)
        return SharePreferUtils.getBoolean(KEY_GUEST_MODE_ENABLED, false)
    }

    fun setGuestMode(context: Context, enabled: Boolean) {
        SharePreferUtils.init(context)
        SharePreferUtils.saveKey(KEY_GUEST_MODE_ENABLED, enabled)
    }

    fun clear(context: Context) {
        setGuestMode(context, false)
    }
}
