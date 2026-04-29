package com.example.xq.flashcard.ui.library

import android.content.Context
import com.example.xq.flashcard.library.FlashCardJsonSerializer
import com.example.xq.flashcard.sync.StudyCloudSyncManager
import com.example.xq.flashcard.library.storage.FlashCardDatabase
import com.example.xq.flashcard.library.storage.FlashCardLegacyMigration
import com.example.xq.flashcard.library.storage.GUEST_OWNER_SCOPE
import com.example.xq.flashcard.library.storage.toDomain
import com.example.xq.flashcard.library.storage.toEntity
import com.example.xq.flashcard.ui.practice.Sm2Scheduler
import com.google.firebase.auth.FirebaseAuth

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

data class FlashCardLibrarySnapshot(
    val rawJson: String,
    val collectionCount: Int,
    val cardCount: Int,
    val updatedAt: Long
) {
    val hasData: Boolean
        get() = collectionCount > 0 || cardCount > 0
}

object FlashCardLibraryStore {

    private const val MAX_COLLECTION_NAME_LENGTH = 100

    fun initialize(context: Context) {
        database(context)
    }

    fun getCollections(context: Context): List<FlashCardCollection> {
        return dao(context)
            .getCollectionsForOwner(resolveOwnerScope())
            .map { it.toDomain() }
    }

    fun hasLocalData(context: Context): Boolean {
        return getCollections(context).isNotEmpty()
    }

    fun getCollection(context: Context, collectionId: Long): FlashCardCollection? {
        return dao(context)
            .getCollectionForOwner(resolveOwnerScope(), collectionId)
            ?.toDomain()
    }

    fun getCard(context: Context, collectionId: Long, cardId: Long): FlashCardItem? {
        return dao(context)
            .getCardForOwner(resolveOwnerScope(), collectionId, cardId)
            ?.toDomain()
    }

    fun hasDuplicateCard(
        context: Context,
        collectionId: Long,
        term: String,
        definition: String
    ): Boolean {
        val normalizedTerm = term.trim()
        val normalizedDefinition = definition.trim().ifBlank { normalizedTerm }
        if (normalizedTerm.isBlank()) return false

        return getCollection(context, collectionId)?.cards?.any { card ->
            isDuplicateCard(card, normalizedTerm, normalizedDefinition)
        } == true
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
        val ownerScope = resolveOwnerScope()
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
        dao(context).insertCollection(collection.toEntity(ownerScope))
        notifyLibraryChanged(context)
        return collection
    }

    fun renameCollection(context: Context, collectionId: Long, name: String): FlashCardCollection? {
        val ownerScope = resolveOwnerScope()
        val currentCollection = getCollection(context, collectionId) ?: return null
        val normalizedName = normalizeCollectionName(name)
        val hasDuplicate = getCollections(context).any {
            it.id != collectionId && it.name.equals(normalizedName, ignoreCase = true)
        }
        if (hasDuplicate) {
            return null
        }

        val updatedCollection = currentCollection.copy(
            name = normalizedName,
            updatedAt = System.currentTimeMillis()
        )
        dao(context).updateCollection(updatedCollection.toEntity(ownerScope))
        notifyLibraryChanged(context)
        return updatedCollection
    }

    fun deleteCollection(context: Context, collectionId: Long): Boolean {
        val removed = dao(context).deleteCollection(resolveOwnerScope(), collectionId) > 0
        if (removed) {
            notifyLibraryChanged(context)
        }
        return removed
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
        val ownerScope = resolveOwnerScope()
        val normalizedTerm = term.trim()
        val normalizedDefinition = definition.trim().ifBlank { normalizedTerm }
        if (normalizedTerm.isBlank()) return null
        if (hasDuplicateCard(context, collectionId, normalizedTerm, normalizedDefinition)) {
            return null
        }

        val collection = getCollection(context, collectionId) ?: return null
        val currentTime = System.currentTimeMillis()
        val savedCard = FlashCardItem(
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
        val updatedCollection = collection.copy(updatedAt = currentTime)

        val database = database(context)
        database.runInTransaction {
            val roomDao = database.flashCardDao()
            roomDao.updateCollection(updatedCollection.toEntity(ownerScope))
            roomDao.insertCard(savedCard.toEntity(collectionId))
        }
        notifyLibraryChanged(context)

        return FlashCardSaveResult(
            collectionId = updatedCollection.id,
            collectionName = updatedCollection.name,
            cardId = savedCard.id
        )
    }

    fun updateCard(
        context: Context,
        collectionId: Long,
        cardId: Long,
        term: String,
        definition: String
    ): FlashCardItem? {
        val ownerScope = resolveOwnerScope()
        val existingCard = getCard(context, collectionId, cardId) ?: return null
        val collection = getCollection(context, collectionId) ?: return null
        val normalizedTerm = term.trim()
        val normalizedDefinition = definition.trim().ifBlank { normalizedTerm }
        if (normalizedTerm.isBlank()) return null

        val updatedAt = System.currentTimeMillis()
        val updatedCard = existingCard.copy(
            term = normalizedTerm,
            definition = normalizedDefinition,
            updatedAt = updatedAt
        )
        val updatedCollection = collection.copy(updatedAt = updatedAt)

        val database = database(context)
        database.runInTransaction {
            val roomDao = database.flashCardDao()
            roomDao.updateCard(updatedCard.toEntity(collectionId))
            roomDao.updateCollection(updatedCollection.toEntity(ownerScope))
        }
        notifyLibraryChanged(context)
        return updatedCard
    }

    fun deleteCard(context: Context, collectionId: Long, cardId: Long): Boolean {
        val ownerScope = resolveOwnerScope()
        val collection = getCollection(context, collectionId) ?: return false
        val updatedCollection = collection.copy(updatedAt = System.currentTimeMillis())
        var removed = false

        val database = database(context)
        database.runInTransaction {
            val roomDao = database.flashCardDao()
            removed = roomDao.deleteCard(ownerScope, collectionId, cardId) > 0
            if (removed) {
                roomDao.updateCollection(updatedCollection.toEntity(ownerScope))
            }
        }

        if (removed) {
            notifyLibraryChanged(context)
        }
        return removed
    }

    fun recordReviewQuality(
        context: Context,
        collectionId: Long,
        cardId: Long,
        quality: Int
    ): FlashCardItem? {
        val ownerScope = resolveOwnerScope()
        val card = getCard(context, collectionId, cardId) ?: return null
        val collection = getCollection(context, collectionId) ?: return null
        val reviewedAt = System.currentTimeMillis()
        val reviewResult = Sm2Scheduler.applyReview(card, quality, reviewedAt)
        val updatedCard = card.copy(
            practiceCount = card.practiceCount + 1,
            correctCount = card.correctCount + if (quality >= 3) 1 else 0,
            lastPracticedAt = reviewedAt,
            updatedAt = reviewedAt,
            easinessFactor = reviewResult.easinessFactor,
            repetitions = reviewResult.repetitions,
            intervalDays = reviewResult.intervalDays,
            nextReviewAt = reviewResult.nextReviewAt,
            lastReviewQuality = quality
        )
        val updatedCollection = collection.copy(updatedAt = reviewedAt)

        val database = database(context)
        database.runInTransaction {
            val roomDao = database.flashCardDao()
            roomDao.updateCard(updatedCard.toEntity(collectionId))
            roomDao.updateCollection(updatedCollection.toEntity(ownerScope))
        }
        notifyLibraryChanged(context)
        return updatedCard
    }

    fun getMasteryRate(card: FlashCardItem): Float {
        if (card.practiceCount <= 0) return 0f
        return card.correctCount.toFloat() / card.practiceCount.toFloat()
    }

    fun isMastered(card: FlashCardItem): Boolean {
        return Sm2Scheduler.isMastered(card)
    }

    fun getSnapshot(context: Context): FlashCardLibrarySnapshot {
        val collections = getCollections(context)
        return FlashCardLibrarySnapshot(
            rawJson = FlashCardJsonSerializer.toJson(collections).ifBlank { "[]" },
            collectionCount = collections.size,
            cardCount = collections.sumOf { it.cards.size },
            updatedAt = collections.maxOfOrNull { collection ->
                maxOf(
                    collection.updatedAt,
                    collection.cards.maxOfOrNull { it.updatedAt } ?: 0L
                )
            } ?: 0L
        )
    }

    fun exportCollectionsJson(context: Context): String {
        return FlashCardJsonSerializer.toJson(getCollections(context)).ifBlank { "[]" }
    }

    fun importCollectionsJson(
        context: Context,
        rawJson: String,
        triggerCloudSync: Boolean = false
    ): Boolean {
        return runCatching {
            val ownerScope = resolveOwnerScope()
            val collections = FlashCardJsonSerializer.fromJson(rawJson)
            val database = database(context)
            database.runInTransaction {
                val roomDao = database.flashCardDao()
                roomDao.deleteCollectionsForOwner(ownerScope)
                if (collections.isNotEmpty()) {
                    roomDao.insertCollections(collections.map { it.toEntity(ownerScope) })
                    roomDao.insertCards(
                        collections.flatMap { collection ->
                            collection.cards.map { card -> card.toEntity(collection.id) }
                        }
                    )
                }
            }
            notifyLibraryChanged(context, triggerCloudSync)
            true
        }.getOrDefault(false)
    }

    private fun database(context: Context): FlashCardDatabase {
        val database = FlashCardDatabase.getInstance(context.applicationContext)
        FlashCardLegacyMigration.migrateIfNeeded(context.applicationContext, database)
        return database
    }

    private fun dao(context: Context) = database(context).flashCardDao()

    private fun notifyLibraryChanged(context: Context, triggerCloudSync: Boolean = true) {
        if (triggerCloudSync) {
            StudyCloudSyncManager.syncLocalSnapshotInBackground(context.applicationContext)
        }
    }

    private fun resolveOwnerScope(): String {
        return FirebaseAuth.getInstance().currentUser?.uid
            ?.takeIf { it.isNotBlank() }
            ?: GUEST_OWNER_SCOPE
    }

    private fun normalizeCollectionName(value: String): String {
        return value.trim()
            .replace("\\s+".toRegex(), " ")
            .take(MAX_COLLECTION_NAME_LENGTH)
    }

    private fun isDuplicateCard(
        card: FlashCardItem,
        normalizedTerm: String,
        normalizedDefinition: String
    ): Boolean {
        return card.term.equals(normalizedTerm, ignoreCase = true) &&
            card.definition.equals(normalizedDefinition, ignoreCase = true)
    }
}
