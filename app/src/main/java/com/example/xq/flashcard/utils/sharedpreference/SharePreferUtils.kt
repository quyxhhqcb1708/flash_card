package com.example.xq.flashcard.utils.sharedpreference

import android.content.Context
import android.content.SharedPreferences

object SharePreferUtils {

    const val PER_NAME = "data_app_shared_preference"

    lateinit var sharePref: SharedPreferences

    fun init(context: Context) {
        if (!SharePreferUtils::sharePref.isInitialized) {
            sharePref = context.getSharedPreferences(PER_NAME, Context.MODE_PRIVATE)
        }
    }

    fun <T> saveKey(key: String, value: T) {
        when (value) {
            is String -> sharePref.edit().putString(key, value).apply()
            is Int -> sharePref.edit().putInt(key, value).apply()
            is Boolean -> sharePref.edit().putBoolean(key, value).apply()
            is Long -> sharePref.edit().putLong(key, value).apply()
            is Float -> sharePref.edit().putFloat(key, value).apply()
        }
    }

    fun removeKey(key: String) {
        sharePref.edit().remove(key).apply()
    }

    fun getString(key: String, value: String = ""): String {
        return sharePref.getString(key, value)?.trim() ?: value
    }

    fun getInt(key: String, defaultValue: Int = 0): Int {
        return sharePref.getInt(key, defaultValue)
    }

    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return sharePref.getBoolean(key, defaultValue)
    }

    fun getLong(key: String, defaultValue: Long = 0L): Long {
        return sharePref.getLong(key, defaultValue)
    }

    fun getFloat(key: String, defaultValue: Float = 0f): Float {
        return sharePref.getFloat(key, defaultValue)
    }

    //====================================   LANGUAGE   ============================================
    fun getLanguageCode(): String = getString("getLanguageCode", "")
    fun setLanguageCode(value: String) = saveKey("getLanguageCode", value)

    fun getLanguageCode(context: Context): String {
        val shareP = context.getSharedPreferences(PER_NAME, Context.MODE_PRIVATE)
        return shareP.getString("getLanguageCode", "")?.trim() ?: ""
    }

    //=====================================   APP   ==============================================
    fun isNotificationEnabled(): Boolean = getBoolean("setting_notifications_enabled", true)
    fun setNotificationEnabled(value: Boolean) = saveKey("setting_notifications_enabled", value)

    fun isSoundEnabled(): Boolean = getBoolean("setting_sound_enabled", true)
    fun setSoundEnabled(value: Boolean) = saveKey("setting_sound_enabled", value)

}
