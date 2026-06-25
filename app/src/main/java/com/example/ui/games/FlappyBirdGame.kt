package com.example.ui.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

enum class FlappyDifficulty(val title: String, val speed: Float, val gapHeight: Float, val label: String) {
    EASY("Leicht", 0.004f, 0.45f, "Entspanntes Fliegen"),
    MEDIUM("Mittel", 0.005f, 0.35f, "Standard Modus"),
    HARD("Schwer", 0.007f, 0.28f, "Schnell & eng")
}

// Pipe class representing obstacles
data class ObstaclePipe(
    var posX: Float, // normalized 0f to 1f (or pixel)
    val gapCenterY: Float, // normalized height center
    val gapHeight: Float, // size of standard gap
    var passed: Boolean = false
)

data class FlappySkin(
    val id: String,
    val name: String,
    val color: Color,
    val eyeColor: Color,
    val beakColor: Color,
    val price: Int,
    val requiredScore: Int = 0
)

val FlappySkinsList = listOf(
    FlappySkin("yellow", "Klassisch Gelb", Color(0xFFFACC15), Color.White, Color(0xFFF97316), 0),
    FlappySkin("mint", "Neon Minze", Color(0xFF00FFCC), Color.White, Color(0xFFFF007F), 50),
    FlappySkin("ruby", "Rubinrot", Color(0xFFEF4444), Color.White, Color(0xFF1E293B), 100),
    FlappySkin("shadow", "Schattenlila", Color(0xFF8B5CF6), Color.Yellow, Color(0xFF00FFCC), 180),
    FlappySkin("gold", "Gefiedertes Gold", Color(0xFFD97706), Color.Cyan, Color(0xFFFFE100), 0, requiredScore = 15)
)

@Composable
fun FlappyBirdGame(
    highScore: Int,
    onHighScoreUpdate: (Int) -> Unit,
    coins: Int,
    onCoinsUpdate: (Int) -> Unit,
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("agent_prefs", android.content.Context.MODE_PRIVATE) }

    var selectedSkinId by remember { mutableStateOf(prefs.getString("selected_flappy_skin", "yellow") ?: "yellow") }
    var unlockedSkinsString by remember { mutableStateOf(prefs.getString("unlocked_flappy_skins", "yellow") ?: "yellow") }

    val unlockedSkins = remember(unlockedSkinsString) { unlockedSkinsString.split(",").toSet() }
    val currentSkin = FlappySkinsList.firstOrNull { it.id == selectedSkinId } ?: FlappySkinsList[0]

    // Game dynamic metrics
    var birdY by remember { mutableStateOf(0.4f) } // normalized height from 0f to 1f
    var birdVelocity by remember { mutableStateOf(0f) }
    val gravity = 0.0004f
    val jumpStrength = -0.012f

    var score by remember { mutableStateOf(0) }
    var isGameOver by remember { mutableStateOf(false) }
    var isStarted by remember { mutableStateOf(false) }
    
    var difficulty by remember { mutableStateOf<FlappyDifficulty?>(null) }
    var countdownValue by remember { mutableIntStateOf(0) }

    // Coins-Stake Flow
    var isPaidMode by remember { mutableStateOf(false) }
    var coinsAwardedForThisRun by remember { mutableStateOf(false) }

    // Pipes list (normalized coords)
    val pipes = remember { mutableStateListOf<ObstaclePipe>() }

    // Spawn / reset pipes helper
    fun resetPipes() {
        val diff = difficulty ?: FlappyDifficulty.MEDIUM
        pipes.clear()
        pipes.add(ObstaclePipe(posX = 1.0f, gapCenterY = Random.nextFloat() * 0.4f + 0.3f, gapHeight = diff.gapHeight))
        pipes.add(ObstaclePipe(posX = 1.5f, gapCenterY = Random.nextFloat() * 0.4f + 0.3f, gapHeight = diff.gapHeight))
    }

    // Reset everything
    fun resetGame() {
        birdY = 0.4f
        birdVelocity = 0f
        score = 0
        isGameOver = false
        isStarted = false
        resetPipes()
        countdownValue = 0
        difficulty = null
        coinsAwardedForThisRun = false
    }

    // Action Flap
    fun flap() {
        if (isGameOver || countdownValue > 0 || difficulty == null) return
        if (!isStarted) {
            isStarted = true
        }
        birdVelocity = jumpStrength
    }

    LaunchedEffect(countdownValue) {
        if (countdownValue > 0) {
            delay(1000)
            countdownValue--
            if (countdownValue == 0) {
                isStarted = true
                birdVelocity = jumpStrength // auto initial flap
            }
        }
    }

    // Main physical tick engine
    LaunchedEffect(isStarted, isGameOver) {
        if (isStarted && !isGameOver && countdownValue == 0) {
            while (true) {
                delay(16) // ~60 FPS update rate

                // Physics gravity pull
                birdVelocity += gravity
                birdY += birdVelocity

                // Ceiling boundary
                if (birdY <= 0f) {
                    birdY = 0f
                    if (birdVelocity < 0f) birdVelocity = 0f
                }

                // Ground boundary collision
                if (birdY >= 1.0f) {
                    isGameOver = true
                    if (score > highScore) onHighScoreUpdate(score)
                    if (isPaidMode && !coinsAwardedForThisRun) {
                        onCoinsUpdate(coins + score)
                        coinsAwardedForThisRun = true
                    }
                    break
                }

                // Move pipes leftward
                val speed = difficulty?.speed ?: 0.005f
                for (i in pipes.indices) {
                    val pipe = pipes[i]
                    pipe.posX -= speed

                    // Pass reward score incrementation
                    if (!pipe.passed && pipe.posX < 0.25f) { // 0.25f is hardcoded bird position
                        pipe.passed = true
                        score++
                    }
                }

                // Recycle/Generate pipes
                if (pipes.isNotEmpty() && pipes.first().posX < -0.2f) {
                    pipes.removeAt(0)
                    // Add new one trailing
                    val diff = difficulty ?: FlappyDifficulty.MEDIUM
                    val lastX = if (pipes.isNotEmpty()) pipes.last().posX else 1.0f
                    pipes.add(
                        ObstaclePipe(
                            posX = lastX + 0.5f,
                            gapCenterY = Random.nextFloat() * 0.4f + 0.3f,
                            gapHeight = diff.gapHeight
                        )
                    )
                }

                // Complex Obstacle boundary collision detection
                val birdRadius = 0.035f
                val birdPosLeft = 0.25f - birdRadius
                val birdPosRight = 0.25f + birdRadius

                for (p in pipes) {
                    val pipeWidth = 0.12f
                    val pipeLeft = p.posX
                    val pipeRight = p.posX + pipeWidth

                    // Vertical gap bounds
                    val gapTop = p.gapCenterY - (p.gapHeight / 2)
                    val gapBottom = p.gapCenterY + (p.gapHeight / 2)

                    // Check horizontal intersection
                    if (birdPosRight > pipeLeft && birdPosLeft < pipeRight) {
                        // Check vertical intersection
                        if (birdY - birdRadius < gapTop || birdY + birdRadius > gapBottom) {
                            isGameOver = true
                            if (score > highScore) onHighScoreUpdate(score)
                            if (isPaidMode && !coinsAwardedForThisRun) {
                                onCoinsUpdate(coins + score)
                                coinsAwardedForThisRun = true
                            }
                            break
                        }
                    }
                }

                if (isGameOver) {
                    break
                }
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxSize(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Stats bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "FLAPPY BIRD",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEAB308)
                    )
                    Text(
                        "Punkte: $score  🏆 Rekord: $highScore  💰 $coins",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
                IconButton(onClick = { resetGame() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset Game", tint = Color.White)
                }
            }

            // Game Canvas (Click anywhere to flap!)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .aspectRatio(0.8f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF38BDF8)) // Sky blue
                    .border(2.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                    .pointerInput(Unit) {
                        detectTapGestures {
                            flap()
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val h = size.height
                    val w = size.width

                    // Draw cloud elements/background decorations
                    drawCircle(Color(0xFFE2E8F0), radius = 60f, center = Offset(w * 0.15f, h * 0.15f))
                    drawCircle(Color(0xFFE2E8F0), radius = 80f, center = Offset(w * 0.22f, h * 0.16f))
                    drawCircle(Color(0xFFE2E8F0), radius = 50f, center = Offset(w * 0.33f, h * 0.18f))

                    drawCircle(Color(0xFFE2E8F0), radius = 70f, center = Offset(w * 0.75f, h * 0.22f))
                    drawCircle(Color(0xFFE2E8F0), radius = 50f, center = Offset(w * 0.82f, h * 0.24f))

                    // Draw pipes/hurdles
                    pipes.forEach { pipe ->
                        val pipeW = w * 0.12f
                        val pX = pipe.posX * w

                        // Top Pipe Height / bounds
                        val gapTopY = (pipe.gapCenterY - pipe.gapHeight / 2) * h
                        // Bottom Pipe Height / bounds
                        val gapBotY = (pipe.gapCenterY + pipe.gapHeight / 2) * h

                        // Draw top green pipe rect
                        drawRoundRect(
                            color = Color(0xFF22C55E),
                            topLeft = Offset(pX, 0f),
                            size = Size(pipeW, gapTopY),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                        )
                        // Bevel line for pipe styling
                        drawRect(
                            color = Color(0xFF15803D),
                            topLeft = Offset(pX, 0f),
                            size = Size(pipeW, gapTopY),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
                        )
                        // Pipe Lip
                        drawRect(
                            color = Color(0xFF4ADE80),
                            topLeft = Offset(pX - 4f, gapTopY - 24f),
                            size = Size(pipeW + 8f, 24f)
                        )
                        drawRect(
                            color = Color(0xFF15803D),
                            topLeft = Offset(pX - 4f, gapTopY - 24f),
                            size = Size(pipeW + 8f, 24f),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
                        )

                        // Draw bottom green pipe rect
                        drawRoundRect(
                            color = Color(0xFF22C55E),
                            topLeft = Offset(pX, gapBotY),
                            size = Size(pipeW, h - gapBotY),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                        )
                        drawRect(
                            color = Color(0xFF15803D),
                            topLeft = Offset(pX, gapBotY),
                            size = Size(pipeW, h - gapBotY),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
                        )
                        // Pipe Lip bottom
                        drawRect(
                            color = Color(0xFF4ADE80),
                            topLeft = Offset(pX - 4f, gapBotY),
                            size = Size(pipeW + 8f, 24f)
                        )
                        drawRect(
                            color = Color(0xFF15803D),
                            topLeft = Offset(pX - 4f, gapBotY),
                            size = Size(pipeW + 8f, 24f),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
                        )
                    }

                    // Draw Bird (skin customizable!)
                    val birdX = w * 0.25f
                    val bY = birdY * h
                    val bRadius = h * 0.035f

                    // Wings/Outer body
                    drawCircle(
                        color = currentSkin.color,
                        radius = bRadius,
                        center = Offset(birdX, bY)
                    )
                    // Beak
                    drawRoundRect(
                        color = currentSkin.beakColor,
                        topLeft = Offset(birdX + bRadius - 6f, bY - 8f),
                        size = Size(16f, 14f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                    )
                    // Eye
                    drawCircle(
                        color = currentSkin.eyeColor,
                        radius = bRadius * 0.35f,
                        center = Offset(birdX + bRadius * 0.3f, bY - bRadius * 0.3f)
                    )
                    drawCircle(
                        color = Color.Black,
                        radius = bRadius * 0.15f,
                        center = Offset(birdX + bRadius * 0.4f, bY - bRadius * 0.3f)
                    )
                    // Wing flap overlay
                    drawOval(
                        color = currentSkin.color.copy(alpha = 0.8f),
                        topLeft = Offset(birdX - bRadius * 0.8f, bY - bRadius * 0.2f),
                        size = Size(bRadius * 0.9f, bRadius * 0.6f)
                    )
                }

                // Friendly overlays
                if (difficulty == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.88f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                "Flappy Setup 🐥",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )

                            // --- SECTION 1: SKIN SELECTION ---
                            Text(
                                "Wähle Vogel-Skin",
                                color = Color(0xFF38BDF8),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            androidx.compose.foundation.lazy.LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(FlappySkinsList.size) { index ->
                                    val skin = FlappySkinsList[index]
                                    val isUnlocked = skin.price == 0 && highScore >= skin.requiredScore || unlockedSkins.contains(skin.id) || skin.id == "yellow"
                                    val isLockedByScore = skin.requiredScore > 0 && highScore < skin.requiredScore
                                    val isSelected = skin.id == selectedSkinId

                                    Card(
                                        modifier = Modifier
                                            .width(96.dp)
                                            .clickable {
                                                if (isUnlocked) {
                                                    selectedSkinId = skin.id
                                                    prefs.edit().putString("selected_flappy_skin", skin.id).apply()
                                                } else if (!isLockedByScore && coins >= skin.price) {
                                                    onCoinsUpdate(coins - skin.price)
                                                    val newUnlocked = (unlockedSkins + skin.id).joinToString(",")
                                                    unlockedSkinsString = newUnlocked
                                                    prefs.edit().putString("unlocked_flappy_skins", newUnlocked).apply()
                                                    selectedSkinId = skin.id
                                                    prefs.edit().putString("selected_flappy_skin", skin.id).apply()
                                                }
                                            },
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) Color(0xFF1E293B) else Color(0xFF0F172A)
                                        ),
                                        border = BorderStroke(
                                            2.dp,
                                            if (isSelected) Color(0xFF00FFCC) else Color.White.copy(alpha = 0.1f)
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.padding(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .background(skin.color, CircleShape)
                                                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Box(modifier = Modifier.fillMaxSize()) {
                                                    Box(
                                                        modifier = Modifier
                                                            .align(Alignment.CenterEnd)
                                                            .size(6.dp)
                                                            .background(skin.beakColor, RoundedCornerShape(1.dp))
                                                    )
                                                    Box(
                                                        modifier = Modifier
                                                            .align(Alignment.TopCenter)
                                                            .padding(top = 6.dp, start = 6.dp)
                                                            .size(5.dp)
                                                            .background(skin.eyeColor, CircleShape)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                skin.name,
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            if (isUnlocked) {
                                                Text("Aktiv", color = Color(0xFF10B981), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                            } else if (isLockedByScore) {
                                                Text("🏆 HS: ${skin.requiredScore}", color = Color(0xFFEF4444), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                            } else {
                                                Text("💰 ${skin.price}", color = Color(0xFFFACC15), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            // --- SECTION 2: COIN ENTRY FEE ---
                            Text(
                                "Münzen-Einsatz",
                                color = Color(0xFF38BDF8),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { isPaidMode = false },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (!isPaidMode) Color(0xFF1E293B) else Color(0xFF0F172A)
                                    ),
                                    border = BorderStroke(
                                        2.dp,
                                        if (!isPaidMode) Color(0xFF00FFCC) else Color.White.copy(alpha = 0.05f)
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(10.dp)
                                    ) {
                                        Text("Gratis", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("Belohnung: Keine", color = Color.Gray, fontSize = 8.sp)
                                    }
                                }

                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { if (coins >= 10) isPaidMode = true },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isPaidMode) Color(0xFF1E293B) else Color(0xFF0F172A).copy(alpha = 0.5f)
                                    ),
                                    border = BorderStroke(
                                        2.dp,
                                        if (isPaidMode) Color(0xFFFACC15) else Color.White.copy(alpha = 0.05f)
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(10.dp)
                                    ) {
                                        Text("Einsatz: 10 💰", color = Color(0xFFFACC15), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("Gewinn: 1 💰 / Pkt", color = Color(0xFF10B981), fontSize = 8.sp)
                                    }
                                }
                            }

                            // --- SECTION 3: DIFFICULTIES ---
                            Text(
                                "Schwierigkeit wählen",
                                color = Color(0xFF38BDF8),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FlappyDifficulty.values().forEach { diff ->
                                    Button(
                                        onClick = { 
                                            if (isPaidMode && coins < 10) {
                                                // fallback to free
                                                isPaidMode = false
                                            }
                                            if (isPaidMode) {
                                                onCoinsUpdate(coins - 10)
                                            }
                                            difficulty = diff
                                            resetPipes()
                                            countdownValue = 3
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = when (diff) {
                                                FlappyDifficulty.EASY -> Color(0xFF10B981)
                                                FlappyDifficulty.MEDIUM -> Color(0xFFF59E0B)
                                                FlappyDifficulty.HARD -> Color(0xFFEF4444)
                                            }
                                        ),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(vertical = 12.dp)
                                    ) {
                                        Text(diff.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                } else if (countdownValue > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = countdownValue.toString(),
                            color = Color.White,
                            fontSize = 120.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (!isStarted && !isGameOver) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Tappe auf den Bildschirm\num zu Fliegen! 🐥",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 28.sp
                        )
                    }
                } else if (isGameOver) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.85f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "CRASH! GAME OVER 💥",
                                color = Color.Red,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            
                            if (isPaidMode) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "💰 EINSATZ-RÜCKZAHLUNG: +$score Münzen!",
                                    color = Color(0xFFFACC15),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { resetGame() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFACC15))
                            ) {
                                Text("Nochmal fliegen", color = Color(0xFF1E293B), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("Steuerung: Tippen auf den Himmel zum Fliegen", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
        }
    }

    // Trigger initial setup
    LaunchedEffect(Unit) {
        if (pipes.isEmpty()) {
            resetPipes()
        }
    }
}
