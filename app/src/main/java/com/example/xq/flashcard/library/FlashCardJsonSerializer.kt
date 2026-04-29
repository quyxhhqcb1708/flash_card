package com.example.xq.flashcard.library

import com.example.xq.flashcard.ui.library.FlashCardCollection
import com.example.xq.flashcard.ui.library.FlashCardDifficulty
import com.example.xq.flashcard.ui.library.FlashCardItem
import com.example.xq.flashcard.ui.practice.Sm2Scheduler
import org.json.JSONArray
import org.json.JSONObject

internal object FlashCardJsonSerializer {

    fun toJson(collections: List<FlashCardCollection>): String {
        val jsonArray = JSONArray()
        collections.forEach { collection ->
            jsonArray.put(
                JSONObject()
                    .put("id", collection.id)
                    .put("name", collection.name)
                    .put("createdAt", collection.createdAt)
                    .put("updatedAt", collection.updatedAt)
                    .put("cards", JSONArray().apply {
                        collection.cards.forEach { card ->
                            put(
                                JSONObject()
                                    .put("id", card.id)
                                    .put("term", card.term)
                                    .put("definition", card.definition)
                                    .put("sourceLanguageCode", card.sourceLanguageCode)
                                    .put("targetLanguageCode", card.targetLanguageCode)
                                    .put("manualDifficulty", card.manualDifficulty.persistedValue)
                                    .put("createdAt", card.createdAt)
                                    .put("updatedAt", card.updatedAt)
                                    .put("practiceCount", card.practiceCount)
                                    .put("correctCount", card.correctCount)
                                    .put("lastPracticedAt", card.lastPracticedAt)
                                    .put("easinessFactor", card.easinessFactor)
                                    .put("repetitions", card.repetitions)
                                    .put("intervalDays", card.intervalDays)
                                    .put("nextReviewAt", card.nextReviewAt)
                                    .put("lastReviewQuality", card.lastReviewQuality)
                            )
                        }
                    })
            )
        }
        return jsonArray.toString()
    }

    fun fromJson(rawJson: String): List<FlashCardCollection> {
        if (rawJson.isBlank()) return emptyList()
        return runCatching {
            val jsonArray = JSONArray(rawJson)
            buildList {
                for (index in 0 until jsonArray.length()) {
                    val item = jsonArray.optJSONObject(index) ?: continue
                    add(item.toCollection())
                }
            }.sortedByDescending { it.updatedAt }
        }.getOrDefault(emptyList())
    }

    private fun JSONObject.toCollection(): FlashCardCollection {
        val cardsJson = optJSONArray("cards") ?: JSONArray()
        val cards = buildList {
            for (index in 0 until cardsJson.length()) {
                val item = cardsJson.optJSONObject(index) ?: continue
                add(
                    FlashCardItem(
                        id = item.optLong("id"),
                        term = item.optString("term"),
                        definition = item.optString("definition"),
                        sourceLanguageCode = item.optString("sourceLanguageCode"),
                        targetLanguageCode = item.optString("targetLanguageCode"),
                        manualDifficulty = FlashCardDifficulty.Companion.fromValue(
                            item.optString("manualDifficulty")
                        ),
                        createdAt = item.optLong("createdAt"),
                        updatedAt = item.optLong("updatedAt"),
                        practiceCount = item.optInt("practiceCount"),
                        correctCount = item.optInt("correctCount"),
                        lastPracticedAt = item.optLong("lastPracticedAt"),
                        easinessFactor = item.optDouble(
                            "easinessFactor",
                            Sm2Scheduler.DEFAULT_EASINESS_FACTOR
                        ),
                        repetitions = item.optInt("repetitions"),
                        intervalDays = item.optInt("intervalDays"),
                        nextReviewAt = item.optLong("nextReviewAt"),
                        lastReviewQuality = item.optInt("lastReviewQuality", -1)
                    )
                )
            }
        }.sortedByDescending { it.updatedAt }

        return FlashCardCollection(
            id = optLong("id"),
            name = optString("name"),
            createdAt = optLong("createdAt"),
            updatedAt = optLong("updatedAt"),
            cards = cards
        )
    }
}
