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
    STATS, GAMES, CHAT, GALLERY, BROWSER, WATCH, SETTINGS, LEADERBOARD
}

enum class GameType {
    SNAKE, TETRIS, FLAPPYBIRD, TICTACTOE, MEMORY, SLOTS, BLACKJACK, MINES, DINO, PONG, TWO_THOUSAND_FORTY_EIGHT, DOTS_AND_BOXES, MOCK_GPS, INTRUDER_PHOTOS, DISGUISE_SETTINGS, COOP_SPLIT_SCREEN, COOP_SPLIT_TUG_OF_WAR, COOP_SPLIT_REACTION, CROWD_RUNNER, DRIFT_CAR, RHYTHM_TAPPER, SPACE_SHOOTER, WIFI_SERVER, HOME
}

enum class AppTheme {
    CLASSIC_DARK,
    OLED_BLACK,
    RETRO_TERMINAL,
    CYBERPUNK,
    AMBER_GOLD
}

enum class DisguiseMode {
    NONE, // Normal Rechner
    CONVERTER, // Einheitenumrechner
    NOTEPAD, // Spy-Gekapseltes Notizbuch
    TELEPHONE // Telefon / Phone dialer fassade
}

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val calculationDao = database.calculationDao()
    private val chatMessageDao = database.chatMessageDao()
    private val generatedImageDao = database.generatedImageDao()
    private val browserHistoryDao = database.browserHistoryDao()
    private val intruderPhotoDao = database.intruderPhotoDao()
    private val fakeNoteDao = database.fakeNoteDao()
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

    val browserHistory: StateFlow<List<BrowserHistoryEntry>> = browserHistoryDao.getAllHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val intruderPhotos: StateFlow<List<IntruderPhoto>> = intruderPhotoDao.getAllPhotos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val fakeNotes: StateFlow<List<FakeNote>> = fakeNoteDao.getAllNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userProfiles: StateFlow<List<UserProfile>> = database.userProfileDao().getAllProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    companion object {
        val ANONYMOUS_PROFILE = UserProfile(name = "Anonymous", coins = 0, photoPath = "")
    }

    // --- Navigation & Flow States ---
    var isSecretUnlocked by mutableStateOf(false)
    var isSecureSecretUnlocked by mutableStateOf(false)
    var showBrowserHistorySecretView by mutableStateOf(false)
    // --- Theme & Disguise Settings ---
    var appTheme by mutableStateOf(
        try {
            AppTheme.valueOf(prefs.getString("appTheme", AppTheme.CLASSIC_DARK.name) ?: AppTheme.CLASSIC_DARK.name)
        } catch (e: Exception) { AppTheme.CLASSIC_DARK }
    )
    fun updateTheme(newTheme: AppTheme) {
        appTheme = newTheme
        prefs.edit().putString("appTheme", newTheme.name).apply()
    }
    var disguiseMode by mutableStateOf(
        try {
            DisguiseMode.valueOf(prefs.getString("disguiseMode", DisguiseMode.NONE.name) ?: DisguiseMode.NONE.name)
        } catch (e: Exception) { DisguiseMode.NONE }
    )
    fun updateDisguiseMode(mode: DisguiseMode) {
        disguiseMode = mode
        prefs.edit().putString("disguiseMode", mode.name).apply()
    }
    // --- Mock GPS Spoofer ---
    var mockGpsLat by mutableStateOf(prefs.getFloat("mockGpsLat", 48.8584f))
    var mockGpsLng by mutableStateOf(prefs.getFloat("mockGpsLng", 2.2945f))
    var mockGpsLabel by mutableStateOf(prefs.getString("mockGpsLabel", "Eiffelturm (Paris)") ?: "Eiffelturm (Paris)")
    var isMockGpsActive by mutableStateOf(prefs.getBoolean("isMockGpsActive", false))
    var isSecretPhotoEnabled by mutableStateOf(prefs.getBoolean("isSecretPhotoEnabled", false))
    fun updateSecretPhotoEnabled(enabled: Boolean) {
        isSecretPhotoEnabled = enabled
        prefs.edit().putBoolean("isSecretPhotoEnabled", enabled).apply()
    }
    var isPanicLockEnabled by mutableStateOf(prefs.getBoolean("isPanicLockEnabled", true))
    var currentUser by mutableStateOf<UserProfile?>(null)
    fun updatePanicLockEnabled(enabled: Boolean) {
        isPanicLockEnabled = enabled
        prefs.edit().putBoolean("isPanicLockEnabled", enabled).apply()
    }
    fun saveMockGps(lat: Float, lng: Float, label: String, active: Boolean) {
        mockGpsLat = lat
        mockGpsLng = lng
        mockGpsLabel = label
        isMockGpsActive = active
        prefs.edit()
            .putFloat("mockGpsLat", lat)
            .putFloat("mockGpsLng", lng)
            .putString("mockGpsLabel", label)
            .putBoolean("isMockGpsActive", active)
            .apply()
    }
    var currentSecretSection by mutableStateOf(SecretSection.GAMES)
    var activeGame by mutableStateOf(GameType.HOME)
    var gamesGridColumns by mutableStateOf(prefs.getInt("gamesGridColumns", 2))
    var isWatchingAd by mutableStateOf(false)
    var isAdSelectionOpen by mutableStateOf(false)
    var adTimeRemaining by mutableStateOf(0)
    var currentAdReward by mutableStateOf(0)
    var currentAdTotalTime by mutableStateOf(0)
    fun openAdSelection() {
        isAdSelectionOpen = true
    }
    fun startAd(minutes: Int, reward: Int) {
        isAdSelectionOpen = false
        if (!isWatchingAd) {
            isWatchingAd = true
            adTimeRemaining = minutes * 60
            currentAdTotalTime = minutes * 60
            currentAdReward = reward
        }
    }
    fun awardAdCoins() {
        if (currentUser != null && currentUser!!.name != ANONYMOUS_PROFILE.name) {
            coins += currentAdReward
        }
        isWatchingAd = false
    }

    fun updateGamesGridColumns(cols: Int) {
        gamesGridColumns = cols
        prefs.edit().putInt("gamesGridColumns", cols).apply()
    }

    // --- Player / Agent Identity ---
    private var _playerAgentName = mutableStateOf(prefs.getString("playerAgentName", "") ?: "")
    var playerAgentName: String
        get() = _playerAgentName.value
        set(value) {
            _playerAgentName.value = value
            prefs.edit().putString("playerAgentName", value).apply()
        }

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

    private var _driftHighScore = mutableStateOf(prefs.getInt("driftHighScore", 0))
    var driftHighScore: Int
        get() = _driftHighScore.value
        set(value) {
            _driftHighScore.value = value
            prefs.edit().putInt("driftHighScore", value).apply()
        }

    private var _crowdHighScore = mutableStateOf(prefs.getInt("crowdHighScore", 0))
    var crowdHighScore: Int
        get() = _crowdHighScore.value
        set(value) {
            _crowdHighScore.value = value
            prefs.edit().putInt("crowdHighScore", value).apply()
        }

    private var _rhythmHighScore = mutableStateOf(prefs.getInt("rhythmHighScore", 0))
    var rhythmHighScore: Int
        get() = _rhythmHighScore.value
        set(value) {
            _rhythmHighScore.value = value
            prefs.edit().putInt("rhythmHighScore", value).apply()
        }

    private var _spaceHighScore = mutableStateOf(prefs.getInt("spaceHighScore", 0))
    var spaceHighScore: Int
        get() = _spaceHighScore.value
        set(value) {
            _spaceHighScore.value = value
            prefs.edit().putInt("spaceHighScore", value).apply()
        }

    private var _twoThousandFortyEightHighScore = mutableStateOf(prefs.getInt("twoThousandFortyEightHighScore", 0))
    var twoThousandFortyEightHighScore: Int
        get() = _twoThousandFortyEightHighScore.value
        set(value) {
            _twoThousandFortyEightHighScore.value = value
            prefs.edit().putInt("twoThousandFortyEightHighScore", value).apply()
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
            // Normal Secret Unlocked
            isSecretUnlocked = true
            isSecureSecretUnlocked = false
            currentSecretSection = SecretSection.GAMES
            calculatorInput = ""
            calculatorOutput = "Geheimmodus Freigeschaltet!"
            return
        }

        if (trimmed == "1111") {
            // Secure Secret Unlocked
            isSecretUnlocked = true
            isSecureSecretUnlocked = true
            currentSecretSection = SecretSection.SETTINGS
            calculatorInput = ""
            calculatorOutput = "Sicherheitsmodus Freigeschaltet!"
            return
        }

        if (trimmed == "5555") {
            showBrowserHistorySecretView = true
            isSecretUnlocked = true
            calculatorInput = ""
            calculatorOutput = "Browser-Verlauf geöffnet!"
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

    private var typingLogJob: kotlinx.coroutines.Job? = null

    fun logBrowserEntry(text: String, type: String) {
        if (text.isBlank()) return
        
        if (type == "Eingetippt" || type == "Gelöscht") {
            typingLogJob?.cancel()
            typingLogJob = viewModelScope.launch {
                kotlinx.coroutines.delay(1200)
                browserHistoryDao.insertHistoryEntry(
                    BrowserHistoryEntry(text = text, type = type)
                )
            }
        } else {
            viewModelScope.launch {
                browserHistoryDao.insertHistoryEntry(
                    BrowserHistoryEntry(text = text, type = type)
                )
            }
        }
    }

    fun clearBrowserHistory() {
        viewModelScope.launch {
            browserHistoryDao.clearHistory()
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

    // --- Notes Actions ---
    fun addFakeNote(title: String, content: String) {
        viewModelScope.launch {
            fakeNoteDao.insertNote(FakeNote(title = title, content = content))
        }
    }

    fun deleteFakeNote(id: Long) {
        viewModelScope.launch {
            fakeNoteDao.deleteNote(id)
        }
    }

    // --- Intruder Actions ---
    fun captureIntruderPhoto(isMocked: Boolean, base64OrPath: String) {
        viewModelScope.launch {
            intruderPhotoDao.insertPhoto(IntruderPhoto(filePath = base64OrPath, isMocked = isMocked))
        }
    }

    fun deleteIntruderPhoto(id: Long) {
        viewModelScope.launch {
            intruderPhotoDao.deletePhoto(id)
        }
    }

    fun clearAllIntruderPhotos() {
        viewModelScope.launch {
            intruderPhotoDao.clearPhotos()
        }
    }
}
