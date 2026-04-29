package com.example.xq.flashcard.library.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
interface FlashCardDao {

    @Transaction
    @Query(
        """
        SELECT * FROM flashcard_collections
        WHERE owner_user_id = :ownerUserId
        ORDER BY updated_at DESC
        """
    )
    fun getCollectionsForOwner(ownerUserId: String): List<CollectionWithCards>

    @Transaction
    @Query(
        """
        SELECT * FROM flashcard_collections
        WHERE owner_user_id = :ownerUserId AND id = :collectionId
        LIMIT 1
        """
    )
    fun getCollectionForOwner(ownerUserId: String, collectionId: Long): CollectionWithCards?

    @Query(
        """
        SELECT flashcard_cards.* FROM flashcard_cards
        INNER JOIN flashcard_collections
            ON flashcard_collections.id = flashcard_cards.collection_id
        WHERE flashcard_collections.owner_user_id = :ownerUserId
            AND flashcard_cards.collection_id = :collectionId
            AND flashcard_cards.id = :cardId
        LIMIT 1
        """
    )
    fun getCardForOwner(ownerUserId: String, collectionId: Long, cardId: Long): FlashCardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCollection(entity: FlashCardCollectionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCollections(entities: List<FlashCardCollectionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCard(entity: FlashCardEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCards(entities: List<FlashCardEntity>)

    @Update
    fun updateCollection(entity: FlashCardCollectionEntity)

    @Update
    fun updateCard(entity: FlashCardEntity)

    @Query(
        """
        DELETE FROM flashcard_collections
        WHERE owner_user_id = :ownerUserId AND id = :collectionId
        """
    )
    fun deleteCollection(ownerUserId: String, collectionId: Long): Int

    @Query(
        """
        DELETE FROM flashcard_cards
        WHERE id = :cardId
            AND collection_id = :collectionId
            AND collection_id IN (
                SELECT id FROM flashcard_collections WHERE owner_user_id = :ownerUserId
            )
        """
    )
    fun deleteCard(ownerUserId: String, collectionId: Long, cardId: Long): Int

    @Query(
        """
        DELETE FROM flashcard_collections
        WHERE owner_user_id = :ownerUserId
        """
    )
    fun deleteCollectionsForOwner(ownerUserId: String)
}
