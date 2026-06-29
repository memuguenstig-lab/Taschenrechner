package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Calculation::class, ChatMessage::class, GeneratedImage::class, BrowserHistoryEntry::class, IntruderPhoto::class, FakeNote::class, UserProfile::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun calculationDao(): CalculationDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun generatedImageDao(): GeneratedImageDao
    abstract fun browserHistoryDao(): BrowserHistoryDao
    abstract fun intruderPhotoDao(): IntruderPhotoDao
    abstract fun fakeNoteDao(): FakeNoteDao
    abstract fun userProfileDao(): UserProfileDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "calculator_secret_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
