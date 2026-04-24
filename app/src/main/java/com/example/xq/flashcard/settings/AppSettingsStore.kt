package com.example.xq.flashcard.ui.settings

import android.content.Context
import com.example.xq.flashcard.reminder.StudyReminderPermission
import com.example.xq.flashcard.utils.sharedpreference.SharePreferUtils
import com.google.firebase.auth.FirebaseAuth

object AppSettingsStore {

    private const val KEY_NOTIFICATION_ENABLED = "setting_notifications_enabled"
    private const val KEY_SOUND_ENABLED = "setting_sound_enabled"
    private const val KEY_CLOUD_SYNC_ENABLED = "setting_cloud_sync_enabled"
    private const val KEY_CLOUD_SYNC_LAST_SYNC = "setting_cloud_sync_last_sync"

    fun isNotificationEnabled(context: Context): Boolean {
        SharePreferUtils.init(context)
        return SharePreferUtils.getBoolean(KEY_NOTIFICATION_ENABLED, true) &&
            StudyReminderPermission.hasPermission(context)
    }

    fun setNotificationEnabled(context: Context, enabled: Boolean) {
        SharePreferUtils.init(context)
        SharePreferUtils.saveKey(KEY_NOTIFICATION_ENABLED, enabled)
    }

    fun isSoundEnabled(context: Context): Boolean {
        SharePreferUtils.init(context)
        return SharePreferUtils.getBoolean(KEY_SOUND_ENABLED, true)
    }

    fun setSoundEnabled(context: Context, enabled: Boolean) {
        SharePreferUtils.init(context)
        SharePreferUtils.saveKey(KEY_SOUND_ENABLED, enabled)
    }

    fun isCloudSyncEnabled(context: Context): Boolean {
        SharePreferUtils.init(context)
        return SharePreferUtils.getBoolean(resolveUserScopedKey(KEY_CLOUD_SYNC_ENABLED), false)
    }

    fun setCloudSyncEnabled(context: Context, enabled: Boolean) {
        SharePreferUtils.init(context)
        SharePreferUtils.saveKey(resolveUserScopedKey(KEY_CLOUD_SYNC_ENABLED), enabled)
    }

    fun getCloudSyncLastSyncedAt(context: Context): Long {
        SharePreferUtils.init(context)
        return SharePreferUtils.getLong(resolveUserScopedKey(KEY_CLOUD_SYNC_LAST_SYNC))
    }

    fun setCloudSyncLastSyncedAt(context: Context, timestamp: Long) {
        SharePreferUtils.init(context)
        SharePreferUtils.saveKey(resolveUserScopedKey(KEY_CLOUD_SYNC_LAST_SYNC), timestamp)
    }

    private fun resolveUserScopedKey(baseKey: String): String {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        return if (userId.isNullOrBlank()) {
            baseKey
        } else {
            "${baseKey}_$userId"
        }
    }
}
