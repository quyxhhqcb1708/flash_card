package com.example.xq.flashcard.library.storage

import android.content.Context
import android.content.SharedPreferences
import com.example.xq.flashcard.library.FlashCardJsonSerializer
import com.example.xq.flashcard.utils.sharedpreference.SharePreferUtils

internal const val GUEST_OWNER_SCOPE = "__guest__"

internal object FlashCardLegacyMigration {

    private const val MIGRATION_FLAG_KEY = "flashcard_room_migration_v1_completed"
    private const val KEY_FLASHCARD_COLLECTIONS_LEGACY = "flashcard_collections_v1"
    private const val KEY_FLASHCARD_COLLECTIONS_OWNER_UID = "flashcard_collections_owner_uid"

    fun migrateIfNeeded(context: Context, database: FlashCardDatabase) {
        SharePreferUtils.init(context)
        val preferences = context.getSharedPreferences(
            SharePreferUtils.PER_NAME,
            Context.MODE_PRIVATE
        )
        if (preferences.getBoolean(MIGRATION_FLAG_KEY, false)) {
            return
        }

        val payloads = collectLegacyPayloads(preferences)
        if (payloads.isEmpty()) {
            preferences.edit()
                .putBoolean(MIGRATION_FLAG_KEY, true)
                .apply()
            return
        }

        val dao = database.flashCardDao()
        database.runInTransaction {
            payloads.forEach { (ownerScope, rawJson) ->
                val collections = FlashCardJsonSerializer.fromJson(rawJson)
                dao.deleteCollectionsForOwner(ownerScope)
                if (collections.isNotEmpty()) {
                    dao.insertCollections(collections.map { it.toEntity(ownerScope) })
                    dao.insertCards(
                        collections.flatMap { collection ->
                            collection.cards.map { card -> card.toEntity(collection.id) }
                        }
                    )
                }
            }
        }

        val editor = preferences.edit()
            .putBoolean(MIGRATION_FLAG_KEY, true)
            .remove(KEY_FLASHCARD_COLLECTIONS_LEGACY)
            .remove(KEY_FLASHCARD_COLLECTIONS_OWNER_UID)
        payloads.keys
            .filter { it != GUEST_OWNER_SCOPE }
            .forEach { ownerScope ->
                editor.remove("${KEY_FLASHCARD_COLLECTIONS_LEGACY}_$ownerScope")
            }
        editor.apply()
    }

    private fun collectLegacyPayloads(
        preferences: SharedPreferences
    ): Map<String, String> {
        val payloads = linkedMapOf<String, String>()
        val scopedPrefix = "${KEY_FLASHCARD_COLLECTIONS_LEGACY}_"

        preferences.all.entries
            .sortedBy { it.key }
            .forEach { (key, value) ->
                if (!key.startsWith(scopedPrefix)) return@forEach
                val ownerScope = key.removePrefix(scopedPrefix).trim()
                val rawJson = (value as? String).orEmpty()
                if (ownerScope.isNotBlank() && rawJson.isNotBlank()) {
                    payloads[ownerScope] = rawJson
                }
            }

        val legacyRawJson = preferences.getString(KEY_FLASHCARD_COLLECTIONS_LEGACY, "").orEmpty()
        val legacyOwnerUserId = preferences.getString(
            KEY_FLASHCARD_COLLECTIONS_OWNER_UID,
            ""
        ).orEmpty().trim()

        if (legacyRawJson.isNotBlank()) {
            if (legacyOwnerUserId.isNotBlank()) {
                payloads.putIfAbsent(legacyOwnerUserId, legacyRawJson)
            } else {
                payloads[GUEST_OWNER_SCOPE] = legacyRawJson
            }
        }

        return payloads
    }
}
