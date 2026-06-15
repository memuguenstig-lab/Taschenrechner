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
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random
import androidx.compose.ui.platform.LocalContext
import android.app.Activity
import android.content.pm.ActivityInfo

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
    
    // Adjusted jump physics for perfect hang-time & matching landscape screens
    val gravity = -0.14f
    val jumpPower = 4.2f
    
    var survivalTicks by remember { mutableStateOf(0L) }
    
    var obstacleX by remember { mutableStateOf(200f) } // 200 to 0 percentage of screen
    var obstacleSpeed by remember { mutableStateOf(1.1f) }
    var obstacleType by remember { mutableStateOf(0) } // 0=small cactus, 1=large cactus, 2=pterodactyl
    var obstacleY by remember { mutableStateOf(0f) }
    
    var clouds by remember { mutableStateOf(List(5) { Offset(Random.nextFloat() * 200f, Random.nextFloat() * 40f + 20f) }) }

    val context = LocalContext.current
    var isLandscape by remember { mutableStateOf(false) }

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
                
                // Increment survival ticks
                survivalTicks++
                
                // Award points over time (approx. every 1 second)
                // The points award scales up the longer the survival time
                if (survivalTicks > 0 && survivalTicks % 60 == 0L) {
                    val seconds = survivalTicks / 60
                    val pointsToAdd = when {
                        seconds < 15 -> 1
                        seconds < 35 -> 2
                        else -> 4
                    }
                    score += pointsToAdd
                }
                
                // Dynamically scale obstacle speed over time up to 3.0f max so it remains jumpable
                val currentSpeedBase = 1.1f + (survivalTicks / 300f) * 0.05f
                obstacleSpeed = currentSpeedBase.coerceAtMost(3.0f)
                
                // Obstacle Movement
                obstacleX -= obstacleSpeed
                
                if (obstacleX < -30f) {
                    obstacleX = 200f + Random.nextFloat() * 20f
                    obstacleType = Random.nextInt(0, 3)
                    
                    if (obstacleType == 2) { // Pterodactyl height
                        obstacleY = if (Random.nextBoolean()) 20f else 35f
                    } else {
                        obstacleY = 0f
                    }
                    
                    // Award bonus score for successfully escaping obstacle
                    score += 5
                }
                
                // Clouds
                clouds = clouds.map {
                    val nx = it.x - obstacleSpeed * 0.15f
                    if (nx < -40f) Offset(200f, Random.nextFloat() * 40f + 20f) else Offset(nx, it.y)
                }
                
                // Collision with tighter/more forgiving hitboxes (ignoring grazing contact)
                val scaleFactor = 0.7f
                val dinoLeft = 10f + (6f * scaleFactor) + 1.5f
                val dinoRight = (if (isDucking) (10f + 36f * scaleFactor) else (10f + 32f * scaleFactor)) - 2.5f
                val dinoBottom = dinoY + 1.0f
                val dinoTop = dinoY + (if (isDucking) 11.2f else 21.0f) - 2.0f
                
                val obsWidth = if (obstacleType == 1) 16f else if (obstacleType == 0) 12f else 24f
                val obsHeight = if (obstacleType == 0) 20f else if (obstacleType == 1) 28f else 12f
                
                // Inset the boundaries slightly inside to forgive narrow misses
                val obsLeft = obstacleX + 3.5f
                val obsRight = obstacleX + obsWidth - 3.5f
                val obsBottom = obstacleY + 1.5f
                val obsTop = obstacleY + obsHeight - 2f
                
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
            survivalTicks = 0L
            isPlaying = true
            dinoVelocity = jumpPower
            isDucking = false
        } else if (isPlaying && dinoY == 0f) {
            dinoVelocity = jumpPower
            isDucking = false
        } else if (isGameOver) {
            isGameOver = false
            score = 0
            survivalTicks = 0L
            obstacleX = 200f
            obstacleSpeed = 1.1f
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
            .padding(if (isLandscape) 8.dp else 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                val activity = context as? Activity
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                onBack()
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück", tint = Color(0xFF535353))
            }
            Text("DINO JUMP", color = Color(0xFF535353), fontSize = if (isLandscape) 18.sp else 24.sp, fontWeight = FontWeight.Black)
            
            Button(
                onClick = {
                    isLandscape = !isLandscape
                    val activity = context as? Activity
                    if (isLandscape) {
                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    } else {
                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE2E2E2), contentColor = Color(0xFF535353)),
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Text(if (isLandscape) "Hochformat" else "Querformat", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = if (isLandscape) 2.dp else 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("HI ${(highScore).toString().padStart(5, '0')}  ${score.toString().padStart(5, '0')}", color = Color(0xFF535353), fontSize = if (isLandscape) 14.sp else 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = (if (isLandscape) {
                    Modifier
                        .fillMaxHeight()
                        .aspectRatio(2.4f)
                } else {
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                })
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
                
                val unit = w / 200f
                
                // Draw ground
                drawLine(
                    color = Color(0xFF535353),
                    start = Offset(0f, groundY),
                    end = Offset(w, groundY),
                    strokeWidth = 2f * unit
                )
                
                // Draw clouds
                clouds.forEach { cloud ->
                    val cx = cloud.x * unit
                    val cy = groundY - cloud.y * (h * 0.01f) - h * 0.3f
                    drawLine(Color(0xFFE2E2E2), Offset(cx, cy), Offset(cx + 12f * unit, cy), strokeWidth = 2f * unit)
                    drawLine(Color(0xFFE2E2E2), Offset(cx + 2f * unit, cy - 2f * unit), Offset(cx + 10f * unit, cy - 2f * unit), strokeWidth = 2f * unit)
                    drawLine(Color(0xFFE2E2E2), Offset(cx + 4f * unit, cy - 4f * unit), Offset(cx + 8f * unit, cy - 4f * unit), strokeWidth = 2f * unit)
                }
                
                // Draw Obstacle
                val obsXDraw = obstacleX * unit
                val obsYDraw = groundY - obstacleY * unit
                val color = Color(0xFF535353)
                
                if (obstacleType == 0 || obstacleType == 1) { // Cactus
                    val isLarge = obstacleType == 1
                    val scale = if (isLarge) 1.5f else 1f
                    
                    // Main trunk
                    drawRect(color, topLeft = Offset(obsXDraw + 3f * unit * scale, obsYDraw - 20f * unit * scale), size = Size(6f * unit * scale, 20f * unit * scale))
                    // Left arm horizontal
                    drawRect(color, topLeft = Offset(obsXDraw, obsYDraw - 12f * unit * scale), size = Size(4f * unit * scale, 4f * unit * scale))
                    // Left arm vertical
                    drawRect(color, topLeft = Offset(obsXDraw, obsYDraw - 16f * unit * scale), size = Size(4f * unit * scale, 8f * unit * scale))
                    
                    // Right arm horizontal
                    drawRect(color, topLeft = Offset(obsXDraw + 8f * unit * scale, obsYDraw - 15f * unit * scale), size = Size(6f * unit * scale, 4f * unit * scale))
                    // Right arm vertical
                    drawRect(color, topLeft = Offset(obsXDraw + 10f * unit * scale, obsYDraw - 19f * unit * scale), size = Size(4f * unit * scale, 8f * unit * scale))

                } else { // Pterodactyl
                    val wingDown = (score / 4) % 2 == 0
                    
                    // Body
                    drawRect(color, topLeft = Offset(obsXDraw + 8f * unit, obsYDraw - 8f * unit), size = Size(10f * unit, 6f * unit))
                    // Head/beak
                    drawRect(color, topLeft = Offset(obsXDraw, obsYDraw - 10f * unit), size = Size(12f * unit, 4f * unit))
                    drawRect(color, topLeft = Offset(obsXDraw + 2f * unit, obsYDraw - 6f * unit), size = Size(6f * unit, 2f * unit))
                    
                    if (wingDown) {
                        // Wings down
                        drawRect(color, topLeft = Offset(obsXDraw + 12f * unit, obsYDraw - 2f * unit), size = Size(8f * unit, 4f * unit))
                        drawRect(color, topLeft = Offset(obsXDraw + 16f * unit, obsYDraw + 2f * unit), size = Size(4f * unit, 4f * unit))
                    } else {
                        // Wings up
                        drawRect(color, topLeft = Offset(obsXDraw + 12f * unit, obsYDraw - 14f * unit), size = Size(8f * unit, 6f * unit))
                        drawRect(color, topLeft = Offset(obsXDraw + 16f * unit, obsYDraw - 18f * unit), size = Size(4f * unit, 4f * unit))
                    }
                }
                
                // Draw Dino
                val dinoXDraw = 10f * unit
                val dinoYDraw = groundY - dinoY * unit
                
                val runCycle = (score / 5) % 2 == 0
                val isJumping = dinoY > 0f
                // color already defined above

                scale(scaleX = 0.7f, scaleY = 0.7f, pivot = Offset(dinoXDraw, dinoYDraw)) {
                    if (isDucking) { 
                        // Ducking
                        // Body
                        drawRect(color, topLeft = Offset(dinoXDraw, dinoYDraw - 16f * unit), size = Size(28f * unit, 12f * unit))
                        // Head
                        drawRect(color, topLeft = Offset(dinoXDraw + 20f * unit, dinoYDraw - 16f * unit), size = Size(10f * unit, 8f * unit))
                        // Snout
                        drawRect(color, topLeft = Offset(dinoXDraw + 30f * unit, dinoYDraw - 16f * unit), size = Size(6f * unit, 4f * unit))
                        // Eye
                        drawRect(Color.White, topLeft = Offset(dinoXDraw + 24f * unit, dinoYDraw - 14f * unit), size = Size(2f * unit, 2f * unit))
                        // Legs
                        if (isJumping || !runCycle) {
                            drawRect(color, topLeft = Offset(dinoXDraw + 4f * unit, dinoYDraw - 4f * unit), size = Size(3f * unit, 4f * unit))
                            drawRect(color, topLeft = Offset(dinoXDraw + 12f * unit, dinoYDraw - 4f * unit), size = Size(3f * unit, 4f * unit))
                        } else {
                            drawRect(color, topLeft = Offset(dinoXDraw + 2f * unit, dinoYDraw - 4f * unit), size = Size(3f * unit, 4f * unit))
                            drawRect(color, topLeft = Offset(dinoXDraw + 14f * unit, dinoYDraw - 4f * unit), size = Size(3f * unit, 4f * unit))
                        }
                    } else { 
                        // Normal
                        // Body
                        drawRect(color, topLeft = Offset(dinoXDraw + 6f * unit, dinoYDraw - 22f * unit), size = Size(12f * unit, 16f * unit))
                        // Tail
                        drawRect(color, topLeft = Offset(dinoXDraw, dinoYDraw - 18f * unit), size = Size(6f * unit, 6f * unit))
                        // Tail tip
                        drawRect(color, topLeft = Offset(dinoXDraw - 2f * unit, dinoYDraw - 20f * unit), size = Size(2f * unit, 4f * unit))
                        // Head
                        drawRect(color, topLeft = Offset(dinoXDraw + 14f * unit, dinoYDraw - 30f * unit), size = Size(12f * unit, 10f * unit))
                        // Snout
                        drawRect(color, topLeft = Offset(dinoXDraw + 26f * unit, dinoYDraw - 30f * unit), size = Size(6f * unit, 6f * unit))
                        // Eye
                        drawRect(Color.White, topLeft = Offset(dinoXDraw + 18f * unit, dinoYDraw - 28f * unit), size = Size(2f * unit, 2f * unit))
                        // Arm
                        drawRect(color, topLeft = Offset(dinoXDraw + 18f * unit, dinoYDraw - 18f * unit), size = Size(6f * unit, 2f * unit))
                        // Arm hand
                        drawRect(color, topLeft = Offset(dinoXDraw + 22f * unit, dinoYDraw - 16f * unit), size = Size(2f * unit, 2f * unit))
                        
                        // Legs
                        if (isJumping) {
                            drawRect(color, topLeft = Offset(dinoXDraw + 6f * unit, dinoYDraw - 6f * unit), size = Size(3f * unit, 6f * unit))
                            drawRect(color, topLeft = Offset(dinoXDraw + 12f * unit, dinoYDraw - 6f * unit), size = Size(3f * unit, 6f * unit))
                        } else {
                            if (runCycle) {
                                // Leg 1 down, Leg 2 up
                                drawRect(color, topLeft = Offset(dinoXDraw + 6f * unit, dinoYDraw - 6f * unit), size = Size(3f * unit, 6f * unit))
                                drawRect(color, topLeft = Offset(dinoXDraw + 14f * unit, dinoYDraw - 6f * unit), size = Size(3f * unit, 3f * unit))
                            } else {
                                // Leg 1 up, Leg 2 down
                                drawRect(color, topLeft = Offset(dinoXDraw + 4f * unit, dinoYDraw - 6f * unit), size = Size(3f * unit, 3f * unit))
                                drawRect(color, topLeft = Offset(dinoXDraw + 12f * unit, dinoYDraw - 6f * unit), size = Size(3f * unit, 6f * unit))
                            }
                        }
                    }
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
    }

        if (!isLandscape) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Steuerung: Tippen = Springen, Wischen unten = Ducken", color = Color(0xFF535353).copy(alpha = 0.6f), fontSize = 12.sp)
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

