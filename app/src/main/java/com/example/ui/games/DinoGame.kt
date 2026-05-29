package com.example.ui.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun DinoGame(
    highScore: Int,
    onHighScoreUpdate: (Int) -> Unit,
    onBack: () -> Unit
) {
    var isPlaying by remember { mutableStateOf(false) }
    var isGameOver by remember { mutableStateOf(false) }
    var score by remember { mutableStateOf(0) }
    
    var dinoY by remember { mutableStateOf(0f) }
    var dinoVelocity by remember { mutableStateOf(0f) }
    var isDucking by remember { mutableStateOf(false) }
    
    val gravity = -0.5f
    val jumpPower = 8f
    
    var obstacleX by remember { mutableStateOf(100f) } // 100 to 0 percentage of screen
    var obstacleSpeed by remember { mutableStateOf(0.9f) }
    var obstacleType by remember { mutableStateOf(0) } // 0=small cactus, 1=large cactus, 2=pterodactyl
    var obstacleY by remember { mutableStateOf(0f) }
    
    var clouds by remember { mutableStateOf(List(3) { Offset(Random.nextFloat() * 100f, Random.nextFloat() * 30f + 20f) }) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isPlaying) {
                delay(16)
                
                // Physics
                if (dinoY > 0f || dinoVelocity > 0f) {
                    dinoY += dinoVelocity
                    dinoVelocity += gravity
                    if (dinoY <= 0f) {
                        dinoY = 0f
                        dinoVelocity = 0f
                    }
                }
                
                // Obstacle Movement
                obstacleX -= obstacleSpeed
                
                if (obstacleX < -10f) {
                    obstacleX = 100f + Random.nextFloat() * 10f
                    obstacleType = Random.nextInt(0, 3)
                    
                    if (obstacleType == 2) { // Pterodactyl height
                        obstacleY = if (Random.nextBoolean()) 20f else 40f
                    } else {
                        obstacleY = 0f
                    }
                    
                    score++
                    if (score % 5 == 0) {
                        obstacleSpeed += 0.1f
                    }
                }
                
                // Clouds
                clouds = clouds.map {
                    val nx = it.x - obstacleSpeed * 0.2f
                    if (nx < -20f) Offset(100f, Random.nextFloat() * 30f + 20f) else Offset(nx, it.y)
                }
                
                // Collision
                val dinoLeft = 10f
                val dinoRight = if (isDucking) 38f else 26f
                val dinoBottom = dinoY
                val dinoTop = if (isDucking) dinoY + 16f else dinoY + 28f
                
                val obsLeft = obstacleX
                val obsRight = obstacleX + if (obstacleType == 1) 16f else if (obstacleType == 0) 12f else 24f
                val obsBottom = obstacleY
                val obsTop = obstacleY + if (obstacleType == 0) 20f else if (obstacleType == 1) 28f else 12f
                
                val overlapX = dinoRight > obsLeft && dinoLeft < obsRight
                val overlapY = dinoBottom < obsTop && dinoTop > obsBottom
                
                if (overlapX && overlapY) {
                    isPlaying = false
                    isGameOver = true
                    if (score > highScore) {
                        onHighScoreUpdate(score)
                    }
                }
            }
        }
    }
    
    fun jump() {
        if (!isPlaying && !isGameOver) {
            isPlaying = true
            dinoVelocity = jumpPower
            isDucking = false
        } else if (isPlaying && dinoY == 0f) {
            dinoVelocity = jumpPower
            isDucking = false
        } else if (isGameOver) {
            isGameOver = false
            score = 0
            obstacleX = 100f
            obstacleSpeed = 1.2f
            dinoY = 0f
            dinoVelocity = 0f
            isPlaying = true
            isDucking = false
        }
    }
    
    fun duck() {
        if (isPlaying && dinoY == 0f) {
            isDucking = true
        } else if (isPlaying && dinoY > 0f) {
            dinoVelocity = -15f // Fast drop
        }
    }
    
    fun stand() {
        isDucking = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück", tint = Color(0xFF535353))
            }
            Text("DINO JUMP", color = Color(0xFF535353), fontSize = 24.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.width(48.dp))
        }
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("HI ${(highScore).toString().padStart(5, '0')}  ${score.toString().padStart(5, '0')}", color = Color(0xFF535353), fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .border(2.dp, Color(0xFFE2E2E2), RoundedCornerShape(12.dp))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = { stand() },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            if (dragAmount.y > 5) duck()
                            else if (dragAmount.y < -5) jump()
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { jump() }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val groundY = h * 0.8f
                
                val unit = w / 100f
                
                // Draw ground
                drawLine(
                    color = Color(0xFF535353),
                    start = Offset(0f, groundY),
                    end = Offset(w, groundY),
                    strokeWidth = 3f * unit * 0.5f
                )
                
                // Draw clouds
                clouds.forEach { cloud ->
                    val cx = cloud.x * unit
                    val cy = groundY - cloud.y * (h * 0.005f) - h * 0.2f
                    drawLine(Color(0xFFE2E2E2), Offset(cx, cy), Offset(cx + 10f * unit, cy), strokeWidth = 2f * unit)
                    drawLine(Color(0xFFE2E2E2), Offset(cx + 2f * unit, cy - 2f * unit), Offset(cx + 8f * unit, cy - 2f * unit), strokeWidth = 2f * unit)
                }
                
                // Draw Obstacle
                val obsXDraw = obstacleX * unit
                val obsYDraw = groundY - obstacleY * unit
                
                if (obstacleType == 0 || obstacleType == 1) { // Cactus
                    val cWidth = if (obstacleType == 1) 16f else 12f
                    val cHeight = if (obstacleType == 1) 28f else 20f
                    drawRect(
                        Color(0xFF535353),
                        topLeft = Offset(obsXDraw, obsYDraw - cHeight * unit),
                        size = Size(cWidth * unit, cHeight * unit)
                    )
                } else { // Pterodactyl
                    drawRect(
                        Color(0xFF535353),
                        topLeft = Offset(obsXDraw, obsYDraw - 12f * unit),
                        size = Size(24f * unit, 6f * unit)
                    )
                    // Wings
                    drawRect(
                        Color(0xFF535353),
                        topLeft = Offset(obsXDraw + 6f * unit, obsYDraw - 12f * unit - 8f * unit * (score % 2)),
                        size = Size(12f * unit, 8f * unit)
                    )
                }
                
                // Draw Dino
                val dinoXDraw = 10f * unit
                val dinoYDraw = groundY - dinoY * unit
                
                if (isDucking) { // Ducking: 28x16
                    // Ducking body
                    drawRect(Color(0xFF535353), topLeft = Offset(dinoXDraw, dinoYDraw - 16f * unit), size = Size(28f * unit, 16f * unit))
                    // Eye
                    drawRect(Color.White, topLeft = Offset(dinoXDraw + 22f * unit, dinoYDraw - 12f * unit), size = Size(2f * unit, 2f * unit))
                } else { // Normal: 16x28
                    // Body
                    drawRect(Color(0xFF535353), topLeft = Offset(dinoXDraw, dinoYDraw - 28f * unit), size = Size(16f * unit, 28f * unit))
                    // Eye
                    drawRect(Color.White, topLeft = Offset(dinoXDraw + 10f * unit, dinoYDraw - 24f * unit), size = Size(2f * unit, 2f * unit))
                    // Arm
                    drawRect(Color(0xFF535353), topLeft = Offset(dinoXDraw + 16f * unit, dinoYDraw - 16f * unit), size = Size(5f * unit, 3f * unit))
                }
            }

            if (isGameOver) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("G A M E   O V E R", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color(0xFF535353))
                    Spacer(modifier = Modifier.height(16.dp))
                    IconButton(onClick = { jump() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Restart", tint = Color(0xFF535353))
                    }
                }
            } else if (!isPlaying && !isGameOver) {
                Text(
                    "Tippe zum Starten",
                    color = Color(0xFF535353),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Steuerung: Tippen = Springen, Wischen unten = Ducken", color = Color(0xFF535353).copy(alpha = 0.6f), fontSize = 12.sp)
        Spacer(modifier = Modifier.height(16.dp))
    }
}

