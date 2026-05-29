package com.example.ui.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class PongDifficulty(val title: String, val aiSpeedFactor: Float, val initialBallSpeed: Float) {
    EASY("Leicht", 0.02f, 0.008f),
    MEDIUM("Mittel", 0.05f, 0.012f),
    HARD("Schwer", 0.1f, 0.016f)
}

@Composable
fun PongGame(
    onBack: () -> Unit
) {
    var playerY by remember { mutableStateOf(0.5f) } // 0 to 1
    var aiY by remember { mutableStateOf(0.5f) }
    
    var ballX by remember { mutableStateOf(0.5f) }
    var ballY by remember { mutableStateOf(0.5f) }
    
    var difficulty by remember { mutableStateOf<PongDifficulty?>(null) }
    
    var ballVelX by remember { mutableStateOf(0.012f) }
    var ballVelY by remember { mutableStateOf(0.012f) }
    
    var playerScore by remember { mutableStateOf(0) }
    var aiScore by remember { mutableStateOf(0) }
    
    var isPlaying by remember { mutableStateOf(false) }
    var countdownText by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    fun startGame(selectedDiff: PongDifficulty) {
        difficulty = selectedDiff
        ballX = 0.5f
        ballY = 0.5f
        ballVelX = if (kotlin.random.Random.nextBoolean()) selectedDiff.initialBallSpeed else -selectedDiff.initialBallSpeed
        ballVelY = if (kotlin.random.Random.nextBoolean()) selectedDiff.initialBallSpeed else -selectedDiff.initialBallSpeed
        
        coroutineScope.launch {
            countdownText = "3"
            kotlinx.coroutines.delay(600)
            countdownText = "2"
            kotlinx.coroutines.delay(600)
            countdownText = "1"
            kotlinx.coroutines.delay(600)
            countdownText = "GO!"
            kotlinx.coroutines.delay(400)
            countdownText = ""
            isPlaying = true
        }
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            val currentDiff = difficulty ?: PongDifficulty.MEDIUM
            while (isPlaying) {
                delay(16)
                
                ballX += ballVelX
                ballY += ballVelY
                
                // AI Logic
                val dif = ballY - aiY
                aiY += dif * currentDiff.aiSpeedFactor
                aiY = aiY.coerceIn(0.1f, 0.9f)
                
                // Wall collisions (Top/Bottom)
                if (ballY <= 0.02f || ballY >= 0.98f) {
                    ballVelY *= -1
                }
                
                // Paddle constants
                val paddleWidth = 0.02f
                val paddleHeight = 0.2f
                
                // Player hit
                if (ballX <= 0.05f + paddleWidth && ballX >= 0.05f && kotlin.math.abs(ballY - playerY) < paddleHeight / 2) {
                    ballVelX = kotlin.math.abs(ballVelX) * 1.05f // Speed up slightly
                    ballVelY = (ballY - playerY) * 0.1f
                }
                
                // AI hit
                if (ballX >= 0.95f - paddleWidth && ballX <= 0.95f && kotlin.math.abs(ballY - aiY) < paddleHeight / 2) {
                    ballVelX = -kotlin.math.abs(ballVelX) * 1.05f
                    ballVelY = (ballY - aiY) * 0.1f
                }
                
                // Scorings
                if (ballX < 0f) {
                    aiScore++
                    isPlaying = false
                } else if (ballX > 1f) {
                    playerScore++
                    isPlaying = false
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück", tint = Color.White)
            }
            Text("PONG", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.width(48.dp))
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("$playerScore", color = Color.White, fontSize = 64.sp, fontWeight = FontWeight.Bold)
            Text("$aiScore", color = Color.White, fontSize = 64.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF111111))
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        // Move player paddle
                        val sizeY = size.height.toFloat()
                        playerY += dragAmount.y / sizeY
                        playerY = playerY.coerceIn(0.1f, 0.9f)
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                
                // Center line
                drawLine(
                    color = Color.DarkGray,
                    start = Offset(w / 2, 0f),
                    end = Offset(w / 2, h),
                    strokeWidth = 4f
                )
                
                // Player paddle
                val pw = w * 0.02f
                val ph = h * 0.2f
                drawRect(
                    color = Color.White,
                    topLeft = Offset(w * 0.05f, h * playerY - ph / 2),
                    size = Size(pw, ph)
                )
                
                // AI paddle
                drawRect(
                    color = Color.White,
                    topLeft = Offset(w * 0.95f - pw, h * aiY - ph / 2),
                    size = Size(pw, ph)
                )
                
                // Ball
                drawRect(
                    color = Color.White,
                    topLeft = Offset(w * ballX - pw/2, h * ballY - pw/2), // square ball
                    size = Size(pw, pw)
                )
            }
            
            if (countdownText.isNotEmpty()) {
                Text(
                    text = countdownText,
                    color = Color.White,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center).background(Color.Black.copy(alpha=0.5f), RoundedCornerShape(16.dp)).padding(32.dp)
                )
            } else if (!isPlaying) {
                if (difficulty == null) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Schwierigkeit", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        PongDifficulty.values().forEach { diff ->
                            Button(
                                onClick = { startGame(diff) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = when (diff) {
                                        PongDifficulty.EASY -> Color(0xFF10B981)
                                        PongDifficulty.MEDIUM -> Color(0xFFF59E0B)
                                        PongDifficulty.HARD -> Color(0xFFEF4444)
                                    }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth(0.5f)
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(diff.title, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(
                            onClick = { startGame(difficulty!!) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                        ) {
                            Text("WEITER")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { difficulty = null }) {
                            Text("Schwierigkeit ändern", color = Color.Gray)
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        Text("Wische hoch und runter, um den Schläger zu bewegen", color = Color.Gray, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(16.dp))
    }
}
