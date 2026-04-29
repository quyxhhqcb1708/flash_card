package com.example.xq.flashcard.library.storage

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.xq.flashcard.ui.library.FlashCardCollection
import com.example.xq.flashcard.ui.library.FlashCardDifficulty
import com.example.xq.flashcard.ui.library.FlashCardItem
import com.example.xq.flashcard.ui.practice.Sm2Scheduler

@Entity(
    tableName = "flashcard_collections",
    indices = [
        Index(value = ["owner_user_id"]),
        Index(value = ["owner_user_id", "updated_at"])
    ]
)
data class FlashCardCollectionEntity(
    @PrimaryKey
    val id: Long,
    @ColumnInfo(name = "owner_user_id")
    val ownerUserId: String,
    val name: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)

@Entity(
    tableName = "flashcard_cards",
    foreignKeys = [
        ForeignKey(
            entity = FlashCardCollectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["collection_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["collection_id"]),
        Index(value = ["updated_at"])
    ]
)
data class FlashCardEntity(
    @PrimaryKey
    val id: Long,
    @ColumnInfo(name = "collection_id")
    val collectionId: Long,
    val term: String,
    val definition: String,
    @ColumnInfo(name = "source_language_code")
    val sourceLanguageCode: String,
    @ColumnInfo(name = "target_language_code")
    val targetLanguageCode: String,
    @ColumnInfo(name = "manual_difficulty")
    val manualDifficulty: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    @ColumnInfo(name = "practice_count")
    val practiceCount: Int = 0,
    @ColumnInfo(name = "correct_count")
    val correctCount: Int = 0,
    @ColumnInfo(name = "last_practiced_at")
    val lastPracticedAt: Long = 0L,
    @ColumnInfo(name = "easiness_factor")
    val easinessFactor: Double = Sm2Scheduler.DEFAULT_EASINESS_FACTOR,
    val repetitions: Int = 0,
    @ColumnInfo(name = "interval_days")
    val intervalDays: Int = 0,
    @ColumnInfo(name = "next_review_at")
    val nextReviewAt: Long = 0L,
    @ColumnInfo(name = "last_review_quality")
    val lastReviewQuality: Int = -1
)

data class CollectionWithCards(
    @Embedded
    val collection: FlashCardCollectionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "collection_id"
    )
    val cards: List<FlashCardEntity>
)

internal fun CollectionWithCards.toDomain(): FlashCardCollection {
    return FlashCardCollection(
        id = collection.id,
        name = collection.name,
        createdAt = collection.createdAt,
        updatedAt = collection.updatedAt,
        cards = cards
            .map(FlashCardEntity::toDomain)
            .sortedByDescending { it.updatedAt }
    )
}

internal fun FlashCardEntity.toDomain(): FlashCardItem {
    return FlashCardItem(
        id = id,
        term = term,
        definition = definition,
        sourceLanguageCode = sourceLanguageCode,
        targetLanguageCode = targetLanguageCode,
        manualDifficulty = FlashCardDifficulty.fromValue(manualDifficulty),
        createdAt = createdAt,
        updatedAt = updatedAt,
        practiceCount = practiceCount,
        correctCount = correctCount,
        lastPracticedAt = lastPracticedAt,
        easinessFactor = easinessFactor,
        repetitions = repetitions,
        intervalDays = intervalDays,
        nextReviewAt = nextReviewAt,
        lastReviewQuality = lastReviewQuality
    )
}

internal fun FlashCardCollection.toEntity(ownerUserId: String): FlashCardCollectionEntity {
    return FlashCardCollectionEntity(
        id = id,
        ownerUserId = ownerUserId,
        name = name,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

internal fun FlashCardItem.toEntity(collectionId: Long): FlashCardEntity {
    return FlashCardEntity(
        id = id,
        collectionId = collectionId,
        term = term,
        definition = definition,
        sourceLanguageCode = sourceLanguageCode,
        targetLanguageCode = targetLanguageCode,
        manualDifficulty = manualDifficulty.persistedValue,
        createdAt = createdAt,
        updatedAt = updatedAt,
        practiceCount = practiceCount,
        correctCount = correctCount,
        lastPracticedAt = lastPracticedAt,
        easinessFactor = easinessFactor,
        repetitions = repetitions,
        intervalDays = intervalDays,
        nextReviewAt = nextReviewAt,
        lastReviewQuality = lastReviewQuality
    )
}
