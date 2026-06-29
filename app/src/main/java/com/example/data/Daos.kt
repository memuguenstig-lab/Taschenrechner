package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CalculationDao {
    @Query("SELECT * FROM calculations ORDER BY timestamp DESC")
    fun getAllCalculations(): Flow<List<Calculation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalculation(calculation: Calculation)

    @Query("DELETE FROM calculations")
    suspend fun clearHistory()
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)

    @Query("DELETE FROM chat_messages")
    suspend fun clearChat()
}

@Dao
interface GeneratedImageDao {
    @Query("SELECT * FROM generated_images ORDER BY timestamp DESC")
    fun getAllImages(): Flow<List<GeneratedImage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(image: GeneratedImage)

    @Query("DELETE FROM generated_images WHERE id = :id")
    suspend fun deleteImage(id: Long)
}

@Dao
interface BrowserHistoryDao {
    @Query("SELECT * FROM browser_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<BrowserHistoryEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryEntry(entry: BrowserHistoryEntry)

    @Query("DELETE FROM browser_history")
    suspend fun clearHistory()
}

@Dao
interface IntruderPhotoDao {
    @Query("SELECT * FROM intruder_photos ORDER BY timestamp DESC")
    fun getAllPhotos(): Flow<List<IntruderPhoto>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: IntruderPhoto)

    @Query("DELETE FROM intruder_photos WHERE id = :id")
    suspend fun deletePhoto(id: Long)

    @Query("DELETE FROM intruder_photos")
    suspend fun clearPhotos()
}

@Dao
interface FakeNoteDao {
    @Query("SELECT * FROM fake_notes ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<FakeNote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: FakeNote)

    @Query("DELETE FROM fake_notes WHERE id = :id")
    suspend fun deleteNote(id: Long)
}

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profiles")
    fun getAllProfiles(): Flow<List<UserProfile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserProfile): Long

    @Query("SELECT * FROM user_profiles WHERE name = :name LIMIT 1")
    suspend fun getProfileByName(name: String): UserProfile?
}
