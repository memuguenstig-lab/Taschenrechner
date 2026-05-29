package com.example.ui.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

// Tetromino Definitions
object Tetrominos {
    val SHAPES = listOf(
        // I
        listOf(listOf(1, 1, 1, 1)),
        // O
        listOf(listOf(1, 1), listOf(1, 1)),
        // T
        listOf(listOf(0, 1, 0), listOf(1, 1, 1)),
        // S
        listOf(listOf(0, 1, 1), listOf(1, 1, 0)),
        // Z
        listOf(listOf(1, 1, 0), listOf(0, 1, 1)),
        // J
        listOf(listOf(1, 0, 0), listOf(1, 1, 1)),
        // L
        listOf(listOf(0, 0, 1), listOf(1, 1, 1))
    )

    val COLORS = listOf(
        Color(0xFF06B6D4), // I: Cyan
        Color(0xFFEAB308), // O: Yellow
        Color(0xFFA855F7), // T: Purple
        Color(0xFF22C55E), // S: Green
        Color(0xFFEF4444), // Z: Red
        Color(0xFF3B82F6), // J: Blue
        Color(0xFFF97316)  // L: Orange
    )
}

@Composable
fun TetrisGame(
    highScore: Int,
    onHighScoreUpdate: (Int) -> Unit,
    onBack: () -> Unit
) {
    val cols = 10
    val rows = 20

    // Board representation. Color.Transparent represents empty space
    var board by remember { mutableStateOf(List(rows) { List(cols) { Color.Transparent } }) }

    // Current piece state
    var pieceIndex by remember { mutableStateOf(0) }
    var currentPiece by remember { mutableStateOf(Tetrominos.SHAPES[0]) }
    var currentPieceColor by remember { mutableStateOf(Tetrominos.COLORS[0]) }
    var pieceX by remember { mutableStateOf(3) }
    var pieceY by remember { mutableStateOf(0) }

    var score by remember { mutableStateOf(0) }
    var isGameOver by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }

    // Rotates the given piece 90deg clockwise
    fun rotatePiece(p: List<List<Int>>): List<List<Int>> {
        val h = p.size
        val w = p[0].size
        val rotated = List(w) { MutableList(h) { 0 } }
        for (r in 0 until h) {
            for (c in 0 until w) {
                rotated[c][h - 1 - r] = p[r][c]
            }
        }
        return rotated
    }

    // Spawn a random new piece
    fun spawnPiece() {
        pieceIndex = Random.nextInt(Tetrominos.SHAPES.size)
        currentPiece = Tetrominos.SHAPES[pieceIndex]
        currentPieceColor = Tetrominos.COLORS[pieceIndex]
        pieceX = cols / 2 - currentPiece[0].size / 2
        pieceY = 0
    }

    // Check validity of piece at coordinate
    fun checkValidMove(p: List<List<Int>>, dx: Int, dy: Int): Boolean {
        for (r in p.indices) {
            for (c in p[r].indices) {
                if (p[r][c] != 0) {
                    val targetX = pieceX + c + dx
                    val targetY = pieceY + r + dy
                    if (targetX < 0 || targetX >= cols || targetY >= rows) return false
                    if (targetY >= 0 && board[targetY][targetX] != Color.Transparent) return false
                }
            }
        }
        return true
    }

    // Reset game handler
    fun resetGame() {
        board = List(rows) { List(cols) { Color.Transparent } }
        score = 0
        isGameOver = false
        isPaused = false
        spawnPiece()
    }

    // Merge piece values into board and clear lines
    fun lockAndClearLines() {
        val newBoard = board.map { it.toMutableList() }.toMutableList()
        for (r in currentPiece.indices) {
            for (c in currentPiece[r].indices) {
                if (currentPiece[r][c] != 0) {
                    val boardY = pieceY + r
                    val boardX = pieceX + c
                    if (boardY in 0 until rows && boardX in 0 until cols) {
                        newBoard[boardY][boardX] = currentPieceColor
                    }
                }
            }
        }

        // Check for full rows
        var clearedLinesCount = 0
        val filteredBoard = newBoard.filter { row -> row.any { it == Color.Transparent } }
        clearedLinesCount = rows - filteredBoard.size
        
        // Pad the top with empty rows
        val finalBoard = List(clearedLinesCount) { List(cols) { Color.Transparent } } + 
                filteredBoard.map { it.toList() }

        board = finalBoard
        score += when (clearedLinesCount) {
            1 -> 100
            2 -> 300
            3 -> 600
            4 -> 1000
            else -> 0
        }

        // Spawn next
        spawnPiece()

        // Check straight away if it spawned in obstacle (Game Over)
        if (!checkValidMove(currentPiece, 0, 0)) {
            isGameOver = true
            if (score > highScore) {
                onHighScoreUpdate(score)
            }
        }
    }

    // Soft drop action
    fun dropOne() {
        if (checkValidMove(currentPiece, 0, 1)) {
            pieceY++
        } else {
            lockAndClearLines()
        }
    }

    // Play/Pause toggler
    fun togglePause() {
        isPaused = !isPaused
    }

    // Initialize first piece
    LaunchedEffect(Unit) {
        if (board.all { row -> row.all { tile -> tile == Color.Transparent } }) {
            spawnPiece()
        }
    }

    // Continuous drop loop
    LaunchedEffect(isGameOver, isPaused) {
        if (!isGameOver && !isPaused) {
            while (true) {
                delay(650) // Fall ticker speed
                dropOne()
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
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header stats
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
                        "TETRIS BLOCK PUZZLE",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA855F7)
                    )
                    Text(
                        "Punkte: $score  🏆 Rekord: $highScore",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
                IconButton(onClick = { togglePause() }) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = "Pause",
                        tint = Color.White
                    )
                }
            }

            // Game grid canvas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .aspectRatio(0.5f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F172A))
                    .border(2.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                    .pointerInput(Unit) {
                        var dragOffset = androidx.compose.ui.geometry.Offset.Zero
                        detectTapGestures(
                            onTap = {
                                if (!isGameOver && !isPaused) {
                                    val rotated = rotatePiece(currentPiece)
                                    if (checkValidMove(rotated, 0, 0)) {
                                        currentPiece = rotated
                                    }
                                }
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        var dragOffset = androidx.compose.ui.geometry.Offset.Zero
                        detectDragGestures(
                            onDragStart = { dragOffset = androidx.compose.ui.geometry.Offset.Zero },
                            onDragEnd = {
                                if (dragOffset.getDistance() > 30) {
                                    if (kotlin.math.abs(dragOffset.x) > kotlin.math.abs(dragOffset.y)) {
                                        if (dragOffset.x > 0) {
                                            if (!isGameOver && !isPaused && checkValidMove(currentPiece, 1, 0)) pieceX++
                                        } else {
                                            if (!isGameOver && !isPaused && checkValidMove(currentPiece, -1, 0)) pieceX--
                                        }
                                    } else {
                                        if (dragOffset.y > 0) {
                                            // Swipe down: Hard drop
                                            if (!isGameOver && !isPaused) {
                                                while (checkValidMove(currentPiece, 0, 1)) {
                                                    pieceY++
                                                }
                                                lockAndClearLines()
                                            }
                                        }
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
                    val sw = size.width / cols
                    val sh = size.height / rows

                    // Draw already locked board blocks
                    for (r in 0 until rows) {
                        for (c in 0 until cols) {
                            val color = board[r][c]
                            if (color != Color.Transparent) {
                                drawRoundRect(
                                    color = color,
                                    topLeft = Offset(c * sw + 1f, r * sh + 1f),
                                    size = Size(sw - 2f, sh - 2f),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                                )
                                // Draw inside inner bevel/outline for 3D appearance block
                                drawRect(
                                    color = Color.White.copy(alpha = 0.2f),
                                    topLeft = Offset(c * sw + 3f, r * sh + 3f),
                                    size = Size(sw - 6f, sh - 6f),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                                )
                            }
                        }
                    }

                    // Draw current falling tetromino piece
                    if (!isGameOver && !isPaused) {
                        for (r in currentPiece.indices) {
                            for (c in currentPiece[r].indices) {
                                if (currentPiece[r][c] != 0) {
                                    val blockY = pieceY + r
                                    val blockX = pieceX + c
                                    if (blockY in 0 until rows && blockX in 0 until cols) {
                                        drawRoundRect(
                                            color = currentPieceColor,
                                            topLeft = Offset(blockX * sw + 1f, blockY * sh + 1f),
                                            size = Size(sw - 2f, sh - 2f),
                                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                                        )
                                        drawRect(
                                            color = Color.White.copy(alpha = 0.35f),
                                            topLeft = Offset(blockX * sw + 3f, blockY * sh + 3f),
                                            size = Size(sw - 6f, sh - 6f),
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                                        )
                                    }
                                }
                            }
                        }
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
                                "PRO-TETRIS OVER 👾",
                                color = Color.Red,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { resetGame() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA855F7))
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
                            "PAUSE ⏸️\nTippe PLAY zum Fortsetzen",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("Steuerung: Wischen = Bewegen / Runterfallen, Tippen = Rotieren", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
