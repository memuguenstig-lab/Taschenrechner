package com.example.ui.games

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.sin
import kotlin.random.Random

// Represents a falling note
data class RhythmNote(
    val id: Int,
    val lane: Int,
    var yPercent: Float, // 0f at top, 100f at hit line
    val spawnTimeMs: Long,
    var isHit: Boolean = false,
    var isMissed: Boolean = false
)

// Preset Beatmaps
data class Beatmap(
    val name: String,
    val description: String,
    val bpm: Int,
    val notes: List<Pair<Long, Int>> // timestamp in ms to Lane Index (0..3)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RhythmTapperGame(
    highScore: Int,
    onHighScoreUpdate: (Int) -> Unit,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    
    // Selectable Beatmaps (Legendäre Hits!)
    val beatmaps = remember {
        listOf(
            Beatmap(
                name = "DARUDE - SANDSTORM",
                description = "Der legendäre Techno-Trance-Smasher von 1999! Schnell, energiegeladen und unaufhaltsam. (BPM: 140)",
                bpm = 140,
                notes = List(200) { index ->
                    val time = 1000L + index * 428L
                    val lane = when {
                        index % 12 == 0 -> 2
                        index % 8 == 0 -> 3
                        index % 4 == 0 -> 1
                        index % 3 == 0 -> 0
                        else -> index % 4
                    }
                    time to lane
                }
            ),
            Beatmap(
                name = "BEETHOVEN 5TH REMIX",
                description = "Die berühmteste Sinfonie aller Zeiten im epischen Cyberpunk Future Bass-Gewand! (BPM: 125)",
                bpm = 125,
                notes = mutableListOf<Pair<Long, Int>>().apply {
                    val beat = 480L
                    var time = 1200L
                    repeat(25) { cycle ->
                        // "da-da-da-DAAA"
                        add(time to 0)
                        add(time + beat to 1)
                        add(time + beat * 2 to 2)
                        add(time + beat * 3 to 3) // gehaltene Note
                        
                        time += beat * 6
                        
                        // "da-da-da-DAAA"
                        add(time to 2)
                        add(time + beat to 1)
                        add(time + beat * 2 to 0)
                        add(time + beat * 3 to 3) // gehaltene Note

                        time += beat * 8
                    }
                }
            ),
            Beatmap(
                name = "BEE GEES - STAYIN' ALIVE",
                description = "Kultiger 70s Disco-Klassiker! Fühle die funky Bassline und bleib im Groove. (BPM: 104)",
                bpm = 104,
                notes = List(110) { index ->
                    val beat = 576L
                    val time = 1200L + index * beat
                    val lane = when (index % 8) {
                        0, 2 -> 0
                        4, 6 -> 3
                        1, 5 -> 1
                        3, 7 -> 2
                        else -> 1
                    }
                    time to lane
                }
            ),
            Beatmap(
                name = "DEEP PURPLE - SMOKE ON THE WATER",
                description = "Das berühmteste Hard-Rock-Riff der Musikgeschichte! Klassischer, unvergesslicher Rhythmus. (BPM: 114)",
                bpm = 114,
                notes = mutableListOf<Pair<Long, Int>>().apply {
                    val beat = 526L
                    var time = 1500L
                    repeat(20) { cycle ->
                        add(time to 0)             // 0
                        add(time + beat to 1)       // 3
                        add(time + beat * 2 to 2)   // 5
                        
                        add(time + beat * 4 to 0)   // 0
                        add(time + beat * 5 to 1)   // 3
                        add(time + beat * 6 to 3)   // 6
                        add(time + beat * 7 to 2)   // 5
                        
                        add(time + beat * 9 to 0)   // 0
                        add(time + beat * 10 to 1)  // 3
                        add(time + beat * 11 to 2)  // 5
                        add(time + beat * 12 to 1)  // 3
                        add(time + beat * 13 to 0)  // 0
                        
                        time += beat * 16
                    }
                }
            )
        )
    }

    var selectedMapIndex by remember { mutableStateOf(0) }
    val activeMap = beatmaps[selectedMapIndex]

    var score by remember { mutableStateOf(0) }
    var highscore by remember(highScore) { mutableStateOf(highScore) }

    LaunchedEffect(score) {
        if (score > highscore) {
            highscore = score
            onHighScoreUpdate(score)
        }
    }
    var combo by remember { mutableStateOf(0) }
    var maxCombo by remember { mutableStateOf(0) }
    var gameStarted by remember { mutableStateOf(false) }
    var gameOver by remember { mutableStateOf(false) }
    
    // Lane flash states
    val laneFlashed = remember { mutableStateListOf(false, false, false, false) }
    
    // Active notes list
    val activeNotes = remember { mutableStateListOf<RhythmNote>() }
    
    // Feedback phrase ("Perfect", "Good", "Miss")
    var feedbackPhrase by remember { mutableStateOf("") }
    var feedbackColor by remember { mutableStateOf(Color.White) }
    
    // Live Game Time tracker
    var gameStartTime by remember { mutableStateOf(0L) }
    
    // Synthesizer Thread State
    var isSynthRunning by remember { mutableStateOf(false) }

    // Helper to synthesize and play tones on hits and backing track using AudioTrack
    fun playSynthSound(freq: Double, durationMs: Int) {
        coroutineScope.launch(Dispatchers.Default) {
            try {
                val sampleRate = 44100
                val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
                val sample = DoubleArray(numSamples)
                val generatedSnd = ByteArray(2 * numSamples)
                
                for (i in 0 until numSamples) {
                    sample[i] = sin(2.0 * Math.PI * i / (sampleRate / freq))
                }
                
                var idx = 0
                for (dVal in sample) {
                    val valShort = (dVal * 32767).toInt().toShort()
                    // in 16 bit wav, first byte is the low order, second is the high order
                    generatedSnd[idx++] = (valShort.toInt() and 0x00ff).toByte()
                    generatedSnd[idx++] = ((valShort.toInt() and 0xff00) ushr 8).toByte()
                }

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(generatedSnd.size)
                    .build()

                audioTrack.write(generatedSnd, 0, generatedSnd.size)
                audioTrack.play()
                delay(durationMs.toLong() + 50L)
                audioTrack.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Play backing rhythm Synth loop (Maßgeschneiderte bekannte Melodien/Riffs!)
    LaunchedEffect(gameStarted, selectedMapIndex, gameOver) {
        if (gameStarted && !gameOver) {
            isSynthRunning = true
            val beatInterval = (60000 / activeMap.bpm).toLong()
            var step = 0
            while (isSynthRunning && !gameOver) {
                when (activeMap.name) {
                    "DARUDE - SANDSTORM" -> {
                        // High energetic lead synth motif
                        val sandstormMelody = listOf(493.88, 493.88, 493.88, 493.88, 493.88, 440.0, 440.0, 440.0)
                        val freq = sandstormMelody[step % sandstormMelody.size]
                        playSynthSound(freq, 90)
                        delay(beatInterval / 2)
                        if (gameOver) break
                        
                        // Alternate kick beat sound
                        val beatSound = if (step % 2 == 0) 90.0 else 900.0
                        playSynthSound(beatSound, 30)
                        delay(beatInterval / 2)
                    }
                    "BEETHOVEN 5TH REMIX" -> {
                        val loopIndex = step % 8
                        when (loopIndex) {
                            0, 1, 2 -> {
                                playSynthSound(392.0, 150) // G4
                                delay(beatInterval)
                            }
                            3 -> {
                                playSynthSound(311.13, 350) // Eb4 (held)
                                delay(beatInterval * 2)
                            }
                            4, 5, 6 -> {
                                playSynthSound(349.23, 150) // F4
                                delay(beatInterval)
                            }
                            7 -> {
                                playSynthSound(293.66, 350) // D4 (held)
                                delay(beatInterval * 2)
                            }
                        }
                    }
                    "BEE GEES - STAYIN' ALIVE" -> {
                        val stayinMelody = listOf(174.61, 130.81, 174.61, 174.61, 155.56, 174.61, 207.65, 174.61) // F-C-F-F-Eb-F-Ab-F funky bass
                        val freq = stayinMelody[step % stayinMelody.size]
                        playSynthSound(freq, 160)
                        delay(beatInterval / 2)
                        if (gameOver) break
                        playSynthSound(700.0, 25) // disco hihat check
                        delay(beatInterval / 2)
                    }
                    "DEEP PURPLE - SMOKE ON THE WATER" -> {
                        val smokeRiff = listOf(196.0, 233.08, 261.63, 196.0, 233.08, 277.18, 261.63, 196.0, 233.08, 261.63, 233.08, 196.0)
                        val freq = smokeRiff[step % smokeRiff.size]
                        playSynthSound(freq, 220)
                        delay(beatInterval)
                    }
                    else -> {
                        playSynthSound(65.4, 120)
                        delay(beatInterval / 2)
                        if (gameOver) break
                        playSynthSound(800.0, 30)
                        delay(beatInterval / 2)
                    }
                }
                step++
                if (gameOver) break
            }
        } else {
            isSynthRunning = false
        }
    }

    // Lane Frequencies
    val laneFrequencies = listOf(261.63, 293.66, 329.63, 392.00) // C4, D4, E4, G4

    fun triggerLaneHit(laneIdx: Int) {
        if (!gameStarted || gameOver) return
        
        // Audio effect
        playSynthSound(laneFrequencies[laneIdx], 100)
        
        // Visual flashing
        coroutineScope.launch {
            laneFlashed[laneIdx] = true
            delay(100)
            laneFlashed[laneIdx] = false
        }
        
        // Find nearest note in this lane
        val currentTimeMs = System.currentTimeMillis() - gameStartTime
        val noteToHit = activeNotes
            .filter { it.lane == laneIdx && !it.isHit && !it.isMissed }
            .minByOrNull { kotlin.math.abs(it.yPercent - 85f) } // 85% is target line

        if (noteToHit != null) {
            val dist = kotlin.math.abs(noteToHit.yPercent - 85f)
            if (dist < 8f) { // Perfect Hit
                noteToHit.isHit = true
                score += 100 + (combo * 5)
                combo++
                maxCombo = maxOf(maxCombo, combo)
                feedbackPhrase = "🎸 PERFECT!"
                feedbackColor = Color(0xFF00FFCC)
                activeNotes.remove(noteToHit)
            } else if (dist < 18f) { // Good Hit
                noteToHit.isHit = true
                score += 50 + (combo * 2)
                combo++
                maxCombo = maxOf(maxCombo, combo)
                feedbackPhrase = "✨ GOOD"
                feedbackColor = Color(0xFFFACC15)
                activeNotes.remove(noteToHit)
            } else if (dist < 28f) { // Bad hit or early
                noteToHit.isHit = true
                score += 15
                combo = 0
                feedbackPhrase = "OKAY"
                feedbackColor = Color(0xFF3B82F6)
                activeNotes.remove(noteToHit)
            } else {
                // Too far to count
            }
        }
    }

    // Central Game Ticker Loop
    LaunchedEffect(gameStarted, selectedMapIndex) {
        if (gameStarted) {
            gameOver = false
            score = 0
            combo = 0
            maxCombo = 0
            activeNotes.clear()
            feedbackPhrase = "READY?"
            feedbackColor = Color.White
            
            gameStartTime = System.currentTimeMillis()
            var lastSpawnedIndex = 0
            val spawnAheadMs = 1200L // notes spawn 1.2s before hitting target line
            
            while (gameStarted && !gameOver) {
                delay(16) // ~60fps
                val elapsed = System.currentTimeMillis() - gameStartTime
                
                // 1. Spawning Notes
                while (lastSpawnedIndex < activeMap.notes.size) {
                    val noteInfo = activeMap.notes[lastSpawnedIndex]
                    val scheduledTime = noteInfo.first
                    if (elapsed >= (scheduledTime - spawnAheadMs)) {
                        activeNotes.add(
                            RhythmNote(
                                id = Random.nextInt(),
                                lane = noteInfo.second,
                                yPercent = 0f,
                                spawnTimeMs = scheduledTime
                            )
                        )
                        lastSpawnedIndex++
                    } else {
                        break
                    }
                }
                
                // 2. Physics & Scoring update step
                val speedFactor = 0.08f // how fast notes descend
                val iterator = activeNotes.iterator()
                while (iterator.hasNext()) {
                    val note = iterator.next()
                    val noteElapsed = elapsed - (note.spawnTimeMs - spawnAheadMs)
                    note.yPercent = (noteElapsed.toFloat() / spawnAheadMs.toFloat()) * 85f
                    
                    // If note has moved beyond the hit boundary without hit
                    if (note.yPercent > 95f && !note.isHit && !note.isMissed) {
                        note.isMissed = true
                        combo = 0
                        feedbackPhrase = "❌ MISS"
                        feedbackColor = Color(0xFFEF4444)
                        iterator.remove()
                    }
                }
                
                // 3. Victory or game end check
                if (lastSpawnedIndex >= activeMap.notes.size && activeNotes.isEmpty()) {
                    delay(1500)
                    gameOver = true
                }
            }
        }
    }

    // Layout
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070A13)) // Neon cyber space dark
    ) {
        TopAppBar(
            title = {
                Text(
                    "💎 BEAT SLAM: TAPPER",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF43F5E)
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Zurück", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF030712))
        )

        if (!gameStarted) {
            // Setup Screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = "Musik",
                    tint = Color(0xFFF43F5E),
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "BEAT SLAM",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp
                )
                Text(
                    "Triff die fallenden Beams im richtigen Takt der Synthesizer-Beats!",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))
                
                Text(
                    "WÄHLE DEINEN TRACK:",
                    color = Color(0xFF00FFCC),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.align(Alignment.Start)
                )

                Text(
                    "🏆 DEIN HIGHSCORE: $highscore",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.align(Alignment.Start).padding(top = 4.dp, bottom = 4.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Maps List
                beatmaps.forEachIndexed { idx, map ->
                    Card(
                        onClick = { selectedMapIndex = idx },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedMapIndex == idx) Color(0xFF1E1E38) else Color(0xFF0F111E)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .border(
                                1.dp,
                                if (selectedMapIndex == idx) Color(0xFFF43F5E) else Color(0xFF1F2937),
                                RoundedCornerShape(12.dp)
                            ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    map.name,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedMapIndex == idx) Color(0xFFF43F5E) else Color.White,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    map.description,
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                            }
                            if (selectedMapIndex == idx) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Ausgewählt", tint = Color(0xFF00FFCC))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                Button(
                    onClick = { gameStarted = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF43F5E)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("BEAT STARTEN 🚀", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        } else {
            // Gameplay rendering
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // Game Canvas
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            // Detect taps across columns to trigger hit mechanism
                            detectTapGestures { offset ->
                                val columnW = size.width / 4f
                                val colIdx = (offset.x / columnW)
                                    .toInt()
                                    .coerceIn(0, 3)
                                triggerLaneHit(colIdx)
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height
                    val colW = w / 4f
                    
                    // Draw lane lines and glowing areas
                    for (i in 0..3) {
                        val sideX = i * colW
                        
                        // Vertical lane separator
                        if (i > 0) {
                            drawLine(
                                color = Color(0xFF1E293B),
                                start = Offset(sideX, 0f),
                                end = Offset(sideX, h),
                                strokeWidth = 1f
                            )
                        }

                        // Hot flashing visual highlight
                        if (laneFlashed[i]) {
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        when (i) {
                                            0 -> Color(0xFF00FFCC).copy(alpha = 0.25f)
                                            1 -> Color(0xFFEC4899).copy(alpha = 0.25f)
                                            2 -> Color(0xFF10B981).copy(alpha = 0.25f)
                                            else -> Color(0xFFFBBF24).copy(alpha = 0.25f)
                                        }
                                    )
                                ),
                                topLeft = Offset(sideX, 0f),
                                size = Size(colW, h)
                            )
                        }
                    }

                    // Target line base (around 85% height)
                    val targetY = h * 0.85f
                    drawLine(
                        color = Color(0xFF475569),
                        start = Offset(0f, targetY),
                        end = Offset(w, targetY),
                        strokeWidth = 3f
                    )
                    
                    // Glow effect for target receptors
                    for (i in 0..3) {
                        val rx = i * colW + colW/2f
                        drawCircle(
                            color = when (i) {
                                0 -> Color(0xFF00FFCC)
                                1 -> Color(0xFFEC4899)
                                2 -> Color(0xFF10B981)
                                else -> Color(0xFFFBBF24)
                            }.copy(alpha = 0.2f),
                            radius = 35f,
                            center = Offset(rx, targetY)
                        )
                        drawCircle(
                            color = when (i) {
                                0 -> Color(0xFF00FFCC)
                                1 -> Color(0xFFEC4899)
                                2 -> Color(0xFF10B981)
                                else -> Color(0xFFFBBF24)
                            },
                            radius = 24f,
                            center = Offset(rx, targetY),
                            style = Stroke(width = 4f)
                        )
                    }

                    // Draw falling notes
                    activeNotes.forEach { note ->
                        val colLeft = note.lane * colW
                        val ny = (note.yPercent / 85f) * targetY
                        
                        // Note parameters
                        val noteH = 26f
                        val noteW = colW * 0.75f
                        val indent = (colW - noteW) / 2f
                        
                        val noteColor = when (note.lane) {
                            0 -> Color(0xFF00FFCC)  // Neon Cyan
                            1 -> Color(0xFFEC4899)  // Hot Pink
                            2 -> Color(0xFF10B981)  // Radiant Green
                            else -> Color(0xFFFBBF24) // Gold
                        }

                        // Glowing box
                        drawRoundRect(
                            color = noteColor,
                            topLeft = Offset(colLeft + indent, ny - noteH/2f),
                            size = Size(noteW, noteH),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                        )
                        // Inner core white glow
                        drawRoundRect(
                            color = Color.White,
                            topLeft = Offset(colLeft + indent + 8f, ny - noteH/2f + 4f),
                            size = Size(noteW - 16f, noteH - 8f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
                        )
                    }
                }

                // HUD Overlay (Scores, multipliers, combo, feedback text)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("SCORE", color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            Text(
                                String.format("%06d", score),
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("MAX COMBO", color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            Text(
                                "$maxCombo",
                                color = Color(0xFF00FFCC),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(100.dp))

                    // Combo Counter
                    if (combo >= 2) {
                        Text(
                            "$combo",
                            fontSize = 62.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            "COMBO MULTIPLIER",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF43F5E),
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Floating dynamic tap rating phrase
                    Text(
                        feedbackPhrase,
                        color = feedbackColor,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 140.dp)
                    )
                }

                // Manual Lane touch receptors at bottom (just as a helper guide for tapping)
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(72.dp)
                        .background(Color(0xFF0C101F).copy(alpha = 0.8f))
                ) {
                    for (i in 0..3) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { triggerLaneHit(i) }
                                .border(1.dp, Color(0xFF1E293B)),
                            contentAlignment = Alignment.Center
                        ) {
                            val btnColor = when (i) {
                                0 -> Color(0xFF00FFCC)
                                1 -> Color(0xFFEC4899)
                                2 -> Color(0xFF10B981)
                                else -> Color(0xFFFBBF24)
                            }
                            Text(
                                when (i) {
                                    0 -> "[A]"
                                    1 -> "[S]"
                                    2 -> "[K]"
                                    else -> "[L]"
                                },
                                color = btnColor,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Game Over or win summary
        if (gameOver) {
            androidx.compose.ui.window.Dialog(onDismissRequest = {}) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF101424)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .border(2.dp, Color(0xFFF43F5E), RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "BEAT KOMPLETT! 🎉",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        HorizontalDivider(color = Color(0xFF1F2937))
                        Spacer(modifier = Modifier.height(16.dp))

                        Text("DEIN SCORE", color = Color.Gray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        Text(
                            "$score",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF00FFCC),
                            fontFamily = FontFamily.Monospace
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("MAX COMBO", color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                Text("$maxCombo", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("BEWERTUNG", color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                val rating = when {
                                    score > 10000 -> "SSS"
                                    score > 7000 -> "S"
                                    score > 5000 -> "A"
                                    score > 3000 -> "B"
                                    else -> "C"
                                }
                                Text(rating, color = Color(0xFFF43F5E), fontWeight = FontWeight.Black, fontSize = 18.sp, fontFamily = FontFamily.Monospace)
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    gameOver = false
                                    gameStarted = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2937)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Zurück", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Menü")
                            }

                            Button(
                                onClick = {
                                    gameOver = false
                                    gameStarted = true
                                    score = 0
                                    combo = 0
                                    maxCombo = 0
                                    activeNotes.clear()
                                    gameStartTime = System.currentTimeMillis()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF43F5E)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }
            }
        }
    }
}
