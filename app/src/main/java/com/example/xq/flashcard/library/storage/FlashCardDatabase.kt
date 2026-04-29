package com.example.xq.flashcard.library.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [FlashCardCollectionEntity::class, FlashCardEntity::class],
    version = 1,
    exportSchema = false
)
abstract class FlashCardDatabase : RoomDatabase() {

    abstract fun flashCardDao(): FlashCardDao

    companion object {
        @Volatile
        private var instance: FlashCardDatabase? = null

        fun getInstance(context: Context): FlashCardDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    FlashCardDatabase::class.java,
                    "flashcard_room.db"
                )
                    .allowMainThreadQueries()
                    .build()
                    .also { instance = it }
            }
        }
    }
}
