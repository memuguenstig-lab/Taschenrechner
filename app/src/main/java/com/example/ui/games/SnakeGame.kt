package com.example.ui.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import com.example.ui.GameType
import kotlinx.coroutines.delay
import kotlin.random.Random
import kotlin.math.abs

// Direction vectors
enum class Direction { UP, DOWN, LEFT, RIGHT }

@Composable
fun SnakeGame(
    highScore: Int,
    onHighScoreUpdate: (Int) -> Unit,
    onBack: () -> Unit
) {
    var snake by remember { mutableStateOf(listOf(Offset(10f, 10f), Offset(10f, 11f), Offset(10f, 12f))) }
    var direction by remember { mutableStateOf(Direction.UP) }
    var food by remember { mutableStateOf(Offset(5f, 5f)) }
    var score by remember { mutableStateOf(0) }
    var isGameOver by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }

    val gridWidth = 25
    val gridHeight = 35

    // Relocate food helper
    fun spawnFood() {
        var newFood: Offset
        do {
            newFood = Offset(
                Random.nextInt(0, gridWidth).toFloat(),
                Random.nextInt(0, gridHeight).toFloat()
            )
        } while (snake.contains(newFood))
        food = newFood
    }

    // Reset Game helper
    fun resetGame() {
        snake = listOf(Offset(10f, 10f), Offset(10f, 11f), Offset(10f, 12f))
        direction = Direction.UP
        score = 0
        isGameOver = false
        isPaused = false
        spawnFood()
    }

    // Game loop
    LaunchedEffect(isGameOver, isPaused) {
        if (!isGameOver && !isPaused) {
            while (true) {
                delay(120) // Speed ticker speed
                val head = snake.first()
                val nextHead = when (direction) {
                    Direction.UP -> Offset(head.x, head.y - 1)
                    Direction.DOWN -> Offset(head.x, head.y + 1)
                    Direction.LEFT -> Offset(head.x - 1, head.y)
                    Direction.RIGHT -> Offset(head.x + 1, head.y)
                }

                // Collision detection (walls or self)
                if (nextHead.x < 0 || nextHead.x >= gridWidth ||
                    nextHead.y < 0 || nextHead.y >= gridHeight ||
                    snake.contains(nextHead)
                ) {
                    isGameOver = true
                    if (score > highScore) {
                        onHighScoreUpdate(score)
                    }
                    break
                }

                // Check eating
                if (nextHead == food) {
                    snake = listOf(nextHead) + snake
                    score += 10
                    spawnFood()
                } else {
                    snake = listOf(nextHead) + snake.dropLast(1)
                }
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Info Bar
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
                        "SNAKE ARCADE",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981)
                    )
                    Text(
                        "Punkte: $score  🏆 Rekord: $highScore",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
                IconButton(onClick = { isPaused = !isPaused }) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = "Pause",
                        tint = Color.White
                    )
                }
            }

            // LCD Game Canvas Grid
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F172A))
                    .border(2.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                    .pointerInput(Unit) {
                        var dragOffset = Offset.Zero
                        detectDragGestures(
                            onDragStart = { dragOffset = Offset.Zero },
                            onDragEnd = {
                                if (dragOffset.getDistance() > 20f) {
                                    val x = dragOffset.x
                                    val y = dragOffset.y
                                    if (abs(x) > abs(y)) {
                                        if (x > 0 && direction != Direction.LEFT) direction = Direction.RIGHT
                                        else if (x < 0 && direction != Direction.RIGHT) direction = Direction.LEFT
                                    } else {
                                        if (y > 0 && direction != Direction.UP) direction = Direction.DOWN
                                        else if (y < 0 && direction != Direction.DOWN) direction = Direction.UP
                                    }
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragOffset += dragAmount
                            }
                        )
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val scaleX = size.width / gridWidth
                    val scaleY = size.height / gridHeight

                    // Draw grid lines subtly
                    for (i in 0..gridWidth) {
                        drawLine(
                            color = Color(0xFF1E293B),
                            start = Offset(i * scaleX, 0f),
                            end = Offset(i * scaleX, size.height),
                            strokeWidth = 1f
                        )
                    }
                    for (i in 0..gridHeight) {
                        drawLine(
                            color = Color(0xFF1E293B),
                            start = Offset(0f, i * scaleY),
                            end = Offset(size.width, i * scaleY),
                            strokeWidth = 1f
                        )
                    }

                    // Draw apple/food
                    drawCircle(
                        color = Color(0xFFEF4444),
                        radius = (scaleX / 2f) * 0.85f,
                        center = Offset((food.x + 0.5f) * scaleX, (food.y + 0.5f) * scaleY)
                    )

                    // Draw Snake body
                    snake.forEachIndexed { index, segment ->
                        val isHead = index == 0
                        val color = if (isHead) Color(0xFF34D399) else Color(0xFF059669)
                        drawRoundRect(
                            color = color,
                            topLeft = Offset(segment.x * scaleX + 1f, segment.y * scaleY + 1f),
                            size = Size(scaleX - 2f, scaleY - 2f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                        )
                    }
                }

                if (isGameOver) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.85f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "GAME OVER \uD83C\uDF4E",
                                color = Color.Red,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { resetGame() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                            ) {
                                Text("Nochmal spielen", color = Color.White)
                            }
                        }
                    }
                } else if (isPaused) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "PAUSE \u23F8\uFE0F\nTippe Play zum Fortsetzen",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
