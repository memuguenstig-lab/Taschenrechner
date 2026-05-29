package com.example.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiClient
import com.example.data.*
import com.example.utils.MathEvaluator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SecretSection {
    GAMES, CHAT, GALLERY, BROWSER, WATCH
}

enum class GameType {
    SNAKE, TETRIS, FLAPPYBIRD, TICTACTOE, MEMORY, SLOTS, BLACKJACK, MINES, DINO, PONG, TWO_THOUSAND_FORTY_EIGHT, HOME
}

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val calculationDao = database.calculationDao()
    private val chatMessageDao = database.chatMessageDao()
    private val generatedImageDao = database.generatedImageDao()
    private val prefs = application.getSharedPreferences("game_stats", android.content.Context.MODE_PRIVATE)

    // --- Supabase Updater & Config ---
    val updateManager = com.example.utils.UpdateManager(application)
    
    var supabaseProjectRef by mutableStateOf(
        prefs.getString("supabaseProjectRef", "")?.let {
            if (it == "bosdx7nmrnx5k4h75ynslr" || it.isEmpty()) "wjbgmmmqqbtvjinbwwwi" else it
        } ?: "wjbgmmmqqbtvjinbwwwi"
    )
    var supabaseBucketName by mutableStateOf(
        prefs.getString("supabaseBucketName", "")?.let {
            if (it == "updates" || it.isEmpty()) "app-releases" else it
        } ?: "app-releases"
    )
    var supabaseKey by mutableStateOf(prefs.getString("supabaseKey", "sb_secret_5NOMN9xNXEBrdbj5r5URDQ_kHDV6QOz") ?: "sb_secret_5NOMN9xNXEBrdbj5r5URDQ_kHDV6QOz")

    init {
        // Automatically save the corrected default if it was modified or empty
        if (prefs.getString("supabaseProjectRef", "") != supabaseProjectRef) {
            prefs.edit().putString("supabaseProjectRef", supabaseProjectRef).apply()
        }
        if (prefs.getString("supabaseBucketName", "") != supabaseBucketName) {
            prefs.edit().putString("supabaseBucketName", supabaseBucketName).apply()
        }
        updateManager.initialize(supabaseProjectRef, supabaseBucketName, supabaseKey)
    }

    fun saveSupabaseConfig(projectRef: String, bucketName: String, key: String) {
        supabaseProjectRef = projectRef
        supabaseBucketName = bucketName
        supabaseKey = key
        prefs.edit()
            .putString("supabaseProjectRef", projectRef)
            .putString("supabaseBucketName", bucketName)
            .putString("supabaseKey", key)
            .apply()
        updateManager.initialize(projectRef, bucketName, key)
    }

    // --- State Exposures reactive to DB ---
    val calculations: StateFlow<List<Calculation>> = calculationDao.getAllCalculations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chatMessages: StateFlow<List<ChatMessage>> = chatMessageDao.getAllMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val generatedImages: StateFlow<List<GeneratedImage>> = generatedImageDao.getAllImages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Navigation & Flow States ---
    var isSecretUnlocked by mutableStateOf(false)
        private set

    var currentSecretSection by mutableStateOf(SecretSection.GAMES)
    var activeGame by mutableStateOf(GameType.HOME)

    // --- Calculator Inputs ---
    var calculatorInput by mutableStateOf("")
        private set
    var calculatorOutput by mutableStateOf("")
        private set

    // --- Chat Room States ---
    var chatInputText by mutableStateOf("")
    var isImageGenerationMode by mutableStateOf(false)
    var isBotResponding by mutableStateOf(false)

    // --- high score records (In-Memory persist) ---
    private var _snakeHighScore = mutableStateOf(prefs.getInt("snakeHighScore", 0))
    var snakeHighScore: Int
        get() = _snakeHighScore.value
        set(value) {
            _snakeHighScore.value = value
            prefs.edit().putInt("snakeHighScore", value).apply()
        }

    private var _tetrisHighScore = mutableStateOf(prefs.getInt("tetrisHighScore", 0))
    var tetrisHighScore: Int
        get() = _tetrisHighScore.value
        set(value) {
            _tetrisHighScore.value = value
            prefs.edit().putInt("tetrisHighScore", value).apply()
        }

    private var _flappyBirdHighScore = mutableStateOf(prefs.getInt("flappyBirdHighScore", 0))
    var flappyBirdHighScore: Int
        get() = _flappyBirdHighScore.value
        set(value) {
            _flappyBirdHighScore.value = value
            prefs.edit().putInt("flappyBirdHighScore", value).apply()
        }

    private var _dinoHighScore = mutableStateOf(prefs.getInt("dinoHighScore", 0))
    var dinoHighScore: Int
        get() = _dinoHighScore.value
        set(value) {
            _dinoHighScore.value = value
            prefs.edit().putInt("dinoHighScore", value).apply()
        }

    private var _ticTacToeWins = mutableStateOf(prefs.getInt("ticTacToeWins", 0))
    var ticTacToeWins: Int
        get() = _ticTacToeWins.value
        set(value) {
            _ticTacToeWins.value = value
            prefs.edit().putInt("ticTacToeWins", value).apply()
        }

    private var _memoryHighScore = mutableStateOf(prefs.getInt("memoryHighScore", 0))
    var memoryHighScore: Int
        get() = _memoryHighScore.value
        set(value) {
            _memoryHighScore.value = value
            prefs.edit().putInt("memoryHighScore", value).apply()
        }

    private var _coins = mutableStateOf(prefs.getInt("coins", 500))
    var coins: Int
        get() = _coins.value
        set(value) {
            _coins.value = value
            prefs.edit().putInt("coins", value).apply()
        }
        
    var isFullScreen by mutableStateOf(false)

    // --- Calculator Actions ---
    fun onCalculatorChar(char: String) {
        if (char == "C") {
            calculatorInput = ""
            calculatorOutput = ""
        } else if (char == "⌫") {
            if (calculatorInput.isNotEmpty()) {
                calculatorInput = calculatorInput.dropLast(1)
            }
        } else {
            calculatorInput += char
        }
    }

    fun onCalculatorEvaluate() {
        val trimmed = calculatorInput.trim()
        if (trimmed == "0000") {
            // Secret Unlocked trigger!
            isSecretUnlocked = true
            calculatorInput = ""
            calculatorOutput = "Geheimmodus Freigeschaltet!"
            return
        }

        if (trimmed.isEmpty()) return

        val result = MathEvaluator.evaluate(trimmed)
        calculatorOutput = result

        // Save normal query to Room if successful
        val isJustZeros = trimmed.isNotEmpty() && trimmed.all { it == '0' }
        if (result != "Syntax Fehler" && !result.startsWith("Fehler") && !isJustZeros && result != "0000") {
            viewModelScope.launch {
                calculationDao.insertCalculation(
                    Calculation(expression = trimmed, result = result)
                )
            }
        }
    }

    fun useHistoryItem(calc: Calculation) {
        calculatorInput = calc.expression
        calculatorOutput = calc.result
    }

    fun clearHistory() {
        viewModelScope.launch {
            calculationDao.clearHistory()
        }
    }

    // --- Lock Control ---
    fun lockSecretMode() {
        isSecretUnlocked = false
        currentSecretSection = SecretSection.GAMES
        activeGame = GameType.HOME
        calculatorInput = ""
        calculatorOutput = ""
    }

    // --- Chatbot & Image Generation Actions ---
    fun sendChatMessage() {
        val prompt = chatInputText.trim()
        if (prompt.isEmpty() || isBotResponding) return

        chatInputText = ""
        
        viewModelScope.launch {
            // Save User message
            val userMsg = ChatMessage(sender = "user", text = prompt)
            chatMessageDao.insertMessage(userMsg)

            isBotResponding = true

            if (isImageGenerationMode) {
                // Image creation branch !
                try {
                    val imageUrl = GeminiClient.getGeneratedImageUrl(prompt)
                    
                    // Create confirmation message from robot
                    val botMessageText = "Hier ist das generierte Bild für dein Thema: \"$prompt\" 🎨 Hauptsächlich erstellt als kreatives Kunstwerk."
                    
                    val botMsg = ChatMessage(
                        sender = "model",
                        text = botMessageText,
                        imageUrl = imageUrl
                    )
                    chatMessageDao.insertMessage(botMsg)

                    // Add image to Gallery Room DB
                    val savedImage = GeneratedImage(
                        prompt = prompt,
                        imageUrl = imageUrl
                    )
                    generatedImageDao.insertImage(savedImage)
                } catch (e: Exception) {
                    chatMessageDao.insertMessage(
                        ChatMessage(sender = "error", text = "Bilderstellung fehlgeschlagen: ${e.localizedMessage}")
                    )
                } finally {
                    isBotResponding = false
                }
            } else {
                // Conversational chat branch
                try {
                    // Fetch whole conversation from DB to provide complete history (up to last 20 messages for prompt hygiene)
                    val history = chatMessages.value.takeLast(20)
                    val reply = GeminiClient.chat(prompt, history)
                    
                    chatMessageDao.insertMessage(
                        ChatMessage(sender = "model", text = reply)
                    )
                } catch (e: Exception) {
                    chatMessageDao.insertMessage(
                        ChatMessage(sender = "error", text = "Fehler bei der Kontaktaufnahme mit der KI: ${e.localizedMessage}")
                    )
                } finally {
                    isBotResponding = false
                }
            }
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            chatMessageDao.clearChat()
            // Add a friendly welcome message again
            chatMessageDao.insertMessage(
                ChatMessage(
                    sender = "model",
                    text = "Hallo! Chatverlauf zurückgesetzt. Wie kann ich dir im Geheimmodus behilflich sein?"
                )
            )
        }
    }

    // --- Gallery Actions ---
    fun deleteImageFromGallery(id: Long) {
        viewModelScope.launch {
            generatedImageDao.deleteImage(id)
        }
    }
}
