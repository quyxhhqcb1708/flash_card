package com.example.xq.flashcard.ui.library

import android.content.Context
import com.example.xq.flashcard.ui.practice.Sm2Scheduler
import com.example.xq.flashcard.utils.sharedpreference.SharePreferUtils
import org.json.JSONArray
import org.json.JSONObject

data class FlashCardItem(
    val id: Long,
    val term: String,
    val definition: String,
    val sourceLanguageCode: String,
    val targetLanguageCode: String,
    val manualDifficulty: FlashCardDifficulty = FlashCardDifficulty.MEDIUM,
    val createdAt: Long,
    val updatedAt: Long,
    val practiceCount: Int = 0,
    val correctCount: Int = 0,
    val lastPracticedAt: Long = 0L,
    val easinessFactor: Double = Sm2Scheduler.DEFAULT_EASINESS_FACTOR,
    val repetitions: Int = 0,
    val intervalDays: Int = 0,
    val nextReviewAt: Long = 0L,
    val lastReviewQuality: Int = -1
)

data class FlashCardCollection(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val cards: List<FlashCardItem>
)

data class RecentFlashCardEntry(
    val collectionId: Long,
    val collectionName: String,
    val card: FlashCardItem
)

data class FlashCardSaveResult(
    val collectionId: Long,
    val collectionName: String,
    val cardId: Long
)

data class FlashCardPracticeSummary(
    val totalCards: Int,
    val practicedCards: Int,
    val masteredCards: Int,
    val readyToReviewCards: Int,
    val upcomingCards: Int,
    val lastPracticedAt: Long
)

object FlashCardLibraryStore {

    private const val KEY_FLASHCARD_COLLECTIONS = "flashcard_collections_v1"
    private const val MAX_COLLECTION_NAME_LENGTH = 100

    fun getCollections(context: Context): List<FlashCardCollection> {
        SharePreferUtils.init(context)
        val rawValue = SharePreferUtils.getString(KEY_FLASHCARD_COLLECTIONS)
        if (rawValue.isBlank()) return emptyList()

        return runCatching {
            val jsonArray = JSONArray(rawValue)
            buildList {
                for (index in 0 until jsonArray.length()) {
                    val item = jsonArray.optJSONObject(index) ?: continue
                    add(item.toCollection())
                }
            }
        }.getOrDefault(emptyList())
            .sortedByDescending { it.updatedAt }
    }

    fun getCollection(context: Context, collectionId: Long): FlashCardCollection? {
        return getCollections(context).firstOrNull { it.id == collectionId }
    }

    fun getCard(context: Context, collectionId: Long, cardId: Long): FlashCardItem? {
        return getCollection(context, collectionId)?.cards?.firstOrNull { it.id == cardId }
    }

    fun getAllCards(context: Context): List<RecentFlashCardEntry> {
        return getCollections(context)
            .flatMap { collection ->
                collection.cards.map { card ->
                    RecentFlashCardEntry(
                        collectionId = collection.id,
                        collectionName = collection.name,
                        card = card
                    )
                }
            }
            .sortedByDescending { it.card.updatedAt }
    }

    fun getPracticeSummary(collection: FlashCardCollection): FlashCardPracticeSummary {
        val now = System.currentTimeMillis()
        val practicedCards = collection.cards.count { it.practiceCount > 0 }
        val masteredCards = collection.cards.count(Sm2Scheduler::isMastered)
        val readyToReviewCards = collection.cards.count { Sm2Scheduler.isDueToday(it, now) }
        val lastPracticedAt = collection.cards.maxOfOrNull { it.lastPracticedAt } ?: 0L
        return FlashCardPracticeSummary(
            totalCards = collection.cards.size,
            practicedCards = practicedCards,
            masteredCards = masteredCards,
            readyToReviewCards = readyToReviewCards,
            upcomingCards = collection.cards.count {
                !Sm2Scheduler.isDueToday(it, now) && !Sm2Scheduler.isMastered(it)
            },
            lastPracticedAt = lastPracticedAt
        )
    }

    fun createCollection(context: Context, name: String): FlashCardCollection {
        val normalizedName = normalizeCollectionName(name)
        val existingCollection = getCollections(context).firstOrNull {
            it.name.equals(normalizedName, ignoreCase = true)
        }
        if (existingCollection != null) {
            return existingCollection
        }
        val createdAt = System.currentTimeMillis()
        val collection = FlashCardCollection(
            id = createdAt,
            name = normalizedName,
            createdAt = createdAt,
            updatedAt = createdAt,
            cards = emptyList()
        )
        val updatedCollections = getCollections(context)
            .toMutableList()
            .apply { add(0, collection) }
        persist(context, updatedCollections)
        return collection
    }

    fun renameCollection(context: Context, collectionId: Long, name: String): FlashCardCollection? {
        val normalizedName = normalizeCollectionName(name)
        val hasDuplicate = getCollections(context).any {
            it.id != collectionId && it.name.equals(normalizedName, ignoreCase = true)
        }
        if (hasDuplicate) {
            return null
        }
        var updatedCollection: FlashCardCollection? = null
        val updatedCollections = getCollections(context).map { collection ->
            if (collection.id != collectionId) {
                collection
            } else {
                collection.copy(
                    name = normalizedName,
                    updatedAt = System.currentTimeMillis()
                ).also { updatedCollection = it }
            }
        }
        persist(context, updatedCollections)
        return updatedCollection
    }

    fun deleteCollection(context: Context, collectionId: Long): Boolean {
        val currentCollections = getCollections(context)
        val updatedCollections = currentCollections.filterNot { it.id == collectionId }
        val changed = updatedCollections.size != currentCollections.size
        if (changed) {
            persist(context, updatedCollections)
        }
        return changed
    }

    fun saveCard(
        context: Context,
        collectionId: Long,
        term: String,
        definition: String,
        sourceLanguageCode: String,
        targetLanguageCode: String,
        manualDifficulty: FlashCardDifficulty = FlashCardDifficulty.MEDIUM
    ): FlashCardSaveResult? {
        val normalizedTerm = term.trim()
        val normalizedDefinition = definition.trim().ifBlank { normalizedTerm }
        if (normalizedTerm.isBlank()) return null

        var saveResult: FlashCardSaveResult? = null
        val updatedCollections = getCollections(context).map { collection ->
            if (collection.id != collectionId) return@map collection

            val currentTime = System.currentTimeMillis()
            val existingCard = collection.cards.firstOrNull {
                it.term.equals(normalizedTerm, ignoreCase = true) &&
                    it.definition.equals(normalizedDefinition, ignoreCase = true)
            }
            val savedCard = if (existingCard != null) {
                existingCard.copy(
                    term = normalizedTerm,
                    definition = normalizedDefinition,
                    sourceLanguageCode = sourceLanguageCode,
                    targetLanguageCode = targetLanguageCode,
                    manualDifficulty = manualDifficulty,
                    updatedAt = currentTime
                )
            } else {
                FlashCardItem(
                    id = currentTime,
                    term = normalizedTerm,
                    definition = normalizedDefinition,
                    sourceLanguageCode = sourceLanguageCode,
                    targetLanguageCode = targetLanguageCode,
                    manualDifficulty = manualDifficulty,
                    createdAt = currentTime,
                    updatedAt = currentTime,
                    nextReviewAt = 0L
                )
            }

            val updatedCards = collection.cards
                .filterNot { it.id == savedCard.id }
                .toMutableList()
                .apply { add(0, savedCard) }

            val updatedCollection = collection.copy(
                updatedAt = currentTime,
                cards = updatedCards
            )
            saveResult = FlashCardSaveResult(
                collectionId = updatedCollection.id,
                collectionName = updatedCollection.name,
                cardId = savedCard.id
            )
            updatedCollection
        }

        if (saveResult != null) {
            persist(context, updatedCollections)
        }
        return saveResult
    }

    fun updateCard(
        context: Context,
        collectionId: Long,
        cardId: Long,
        term: String,
        definition: String
    ): FlashCardItem? {
        val normalizedTerm = term.trim()
        val normalizedDefinition = definition.trim().ifBlank { normalizedTerm }
        if (normalizedTerm.isBlank()) return null

        var updatedCard: FlashCardItem? = null
        val updatedCollections = getCollections(context).map { collection ->
            if (collection.id != collectionId) return@map collection

            val cards = collection.cards.map { card ->
                if (card.id != cardId) {
                    card
                } else {
                    card.copy(
                        term = normalizedTerm,
                        definition = normalizedDefinition,
                        updatedAt = System.currentTimeMillis()
                    ).also { updatedCard = it }
                }
            }
            collection.copy(
                updatedAt = System.currentTimeMillis(),
                cards = cards.sortedByDescending { it.updatedAt }
            )
        }
        if (updatedCard != null) {
            persist(context, updatedCollections)
        }
        return updatedCard
    }

    fun deleteCard(context: Context, collectionId: Long, cardId: Long): Boolean {
        var removed = false
        val updatedCollections = getCollections(context).map { collection ->
            if (collection.id != collectionId) return@map collection

            val cards = collection.cards.filterNot {
                val matched = it.id == cardId
                removed = removed || matched
                matched
            }
            collection.copy(
                updatedAt = System.currentTimeMillis(),
                cards = cards
            )
        }
        if (removed) {
            persist(context, updatedCollections)
        }
        return removed
    }

    fun recordReviewQuality(
        context: Context,
        collectionId: Long,
        cardId: Long,
        quality: Int
    ): FlashCardItem? {
        val reviewedAt = System.currentTimeMillis()
        var updatedCard: FlashCardItem? = null
        val updatedCollections = getCollections(context).map { collection ->
            if (collection.id != collectionId) return@map collection

            val updatedCards = collection.cards.map { card ->
                if (card.id != cardId) {
                    card
                } else {
                    val reviewResult = Sm2Scheduler.applyReview(card, quality, reviewedAt)
                    card.copy(
                        practiceCount = card.practiceCount + 1,
                        correctCount = card.correctCount + if (quality >= 3) 1 else 0,
                        lastPracticedAt = reviewedAt,
                        updatedAt = reviewedAt,
                        easinessFactor = reviewResult.easinessFactor,
                        repetitions = reviewResult.repetitions,
                        intervalDays = reviewResult.intervalDays,
                        nextReviewAt = reviewResult.nextReviewAt,
                        lastReviewQuality = quality
                    ).also { updatedCard = it }
                }
            }

            collection.copy(
                updatedAt = reviewedAt,
                cards = updatedCards.sortedByDescending { it.updatedAt }
            )
        }

        if (updatedCard != null) {
            persist(context, updatedCollections)
        }
        return updatedCard
    }

    fun getMasteryRate(card: FlashCardItem): Float {
        if (card.practiceCount <= 0) return 0f
        return card.correctCount.toFloat() / card.practiceCount.toFloat()
    }

    fun isMastered(card: FlashCardItem): Boolean {
        return Sm2Scheduler.isMastered(card)
    }

    private fun persist(context: Context, collections: List<FlashCardCollection>) {
        SharePreferUtils.init(context)
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
        SharePreferUtils.saveKey(KEY_FLASHCARD_COLLECTIONS, jsonArray.toString())
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
                        manualDifficulty = FlashCardDifficulty.fromValue(
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

    private fun normalizeCollectionName(value: String): String {
        return value.trim()
            .replace("\\s+".toRegex(), " ")
            .take(MAX_COLLECTION_NAME_LENGTH)
    }
}
