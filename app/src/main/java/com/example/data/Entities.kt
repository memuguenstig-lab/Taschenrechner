package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calculations")
data class Calculation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val expression: String,
    val result: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: String, // "user", "model", "error"
    val text: String,
    val imageUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "generated_images")
data class GeneratedImage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val prompt: String,
    val imageUrl: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "browser_history")
data class BrowserHistoryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String // "typed" or "searched"
)

@Entity(tableName = "intruder_photos")
data class IntruderPhoto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val filePath: String,
    val isMocked: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val coins: Int = 0,
    val photoPath: String,
    val lastLogin: Long = System.currentTimeMillis()
)

@Entity(tableName = "fake_notes")
data class FakeNote(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)
