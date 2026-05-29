package com.example.ui.games

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

data class Tile(
    val id: Int,
    var value: Int,
    var row: Int,
    var col: Int
)

@Composable
fun TwoThousandFortyEightGame(
    highScore: Int,
    onHighScoreUpdate: (Int) -> Unit,
    onBack: () -> Unit
) {
    var nextId by remember { mutableStateOf(1) }
    var tiles by remember { mutableStateOf<List<Tile>>(emptyList()) }
    var score by remember { mutableStateOf(0) }
    var gameOver by remember { mutableStateOf(false) }

    fun addRandomTile(currentTiles: MutableList<Tile>) {
        val emptyCells = mutableListOf<Pair<Int, Int>>()
        for (i in 0..3) {
            for (j in 0..3) {
                if (currentTiles.none { it.row == i && it.col == j }) {
                    emptyCells.add(Pair(i, j))
                }
            }
        }
        if (emptyCells.isNotEmpty()) {
            val cell = emptyCells.random()
            currentTiles.add(Tile(nextId++, if (Random.nextFloat() < 0.9f) 2 else 4, cell.first, cell.second))
        }
    }

    fun initGame() {
        val newTiles = mutableListOf<Tile>()
        addRandomTile(newTiles)
        addRandomTile(newTiles)
        tiles = newTiles
        score = 0
        gameOver = false
    }

    LaunchedEffect(Unit) {
        if (tiles.isEmpty()) {
            initGame()
        }
    }

    fun move(dx: Int, dy: Int) {
        if (gameOver) return
        var moved = false
        val newTiles = tiles.map { it.copy() }.toMutableList()
        var pointsGained = 0

        val grouped = newTiles.groupBy { if (dx != 0) it.row else it.col }
        
        for (k in 0..3) {
            val line = grouped[k] ?: emptyList()
            val sortedLine = if (dx > 0 || dy > 0) {
                line.sortedByDescending { if (dx != 0) it.col else it.row }
            } else {
                line.sortedBy { if (dx != 0) it.col else it.row }
            }.toMutableList()

            var writePos = if (dx > 0 || dy > 0) 3 else 0
            val step = if (dx > 0 || dy > 0) -1 else 1
            
            var i = 0
            while (i < sortedLine.size) {
                val current = sortedLine[i]
                var merged = false
                if (i < sortedLine.size - 1) {
                    val next = sortedLine[i + 1]
                    if (current.value == next.value) {
                        // Merge
                        current.value *= 2
                        current.row = if (dx != 0) current.row else writePos
                        current.col = if (dx != 0) writePos else current.col
                        
                        next.row = current.row
                        next.col = current.col
                        // Remove the merged tile from next update rendering
                        newTiles.remove(next)
                        
                        pointsGained += current.value
                        i++
                        merged = true
                        moved = true
                    }
                }
                if (!merged) {
                    val oldRow = current.row
                    val oldCol = current.col
                    current.row = if (dx != 0) current.row else writePos
                    current.col = if (dx != 0) writePos else current.col
                    if (oldRow != current.row || oldCol != current.col) moved = true
                }
                writePos += step
                i++
            }
        }

        if (moved) {
            score += pointsGained
            if (score > highScore) onHighScoreUpdate(score)
            addRandomTile(newTiles)
            tiles = newTiles
            
            // Check game over
            var canMove = false
            for (r in 0..3) {
                for (c in 0..3) {
                    val t = newTiles.find { it.row == r && it.col == c }
                    if (t == null) {
                        canMove = true
                    } else {
                        if (r < 3 && newTiles.find { it.row == r + 1 && it.col == c }?.value == t.value) canMove = true
                        if (c < 3 && newTiles.find { it.row == r && it.col == c + 1 }?.value == t.value) canMove = true
                    }
                }
            }
            if (!canMove) gameOver = true
        }
    }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAF8EF))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück", tint = Color(0xFF776E65))
            }
            Text("2048 Deluxe", color = Color(0xFF776E65), fontSize = 28.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.width(48.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (isLandscape) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Column: Stats & Instructions
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ScoreBox(title = "SCORE", value = score)
                        ScoreBox(title = "BEST", value = highScore)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Wische auf dem Spielfeld,\num die Kacheln zu verschieben.",
                        color = Color(0xFF776E65),
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }

                // Right Column: Board
                Box(
                    modifier = Modifier.weight(1.2f),
                    contentAlignment = Alignment.Center
                ) {
                    BoardView(
                        tiles = tiles,
                        gameOver = gameOver,
                        onReset = { initGame() },
                        move = { dx, dy -> move(dx, dy) }
                    )
                }
            }
        } else {
            // Portrait Layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                ScoreBox(title = "SCORE", value = score)
                ScoreBox(title = "BEST", value = highScore)
            }

            Spacer(modifier = Modifier.height(24.dp))

            BoardView(
                tiles = tiles,
                gameOver = gameOver,
                onReset = { initGame() },
                move = { dx, dy -> move(dx, dy) }
            )

            Spacer(modifier = Modifier.height(24.dp))
            Text("Wische, um die Kacheln zu bewegen.", color = Color(0xFF776E65), fontSize = 16.sp)
        }
    }
}

@Composable
fun BoardView(
    tiles: List<Tile>,
    gameOver: Boolean,
    onReset: () -> Unit,
    move: (Int, Int) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFBBADA0))
            .pointerInput(Unit) {
                var dragOffset = androidx.compose.ui.geometry.Offset.Zero
                var swipeHandled = false
                detectDragGestures(
                    onDragStart = { 
                        dragOffset = androidx.compose.ui.geometry.Offset.Zero 
                        swipeHandled = false
                    },
                    onDragEnd = {},
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (!swipeHandled) {
                            dragOffset += dragAmount
                            if (dragOffset.getDistance() > 50) {
                                if (kotlin.math.abs(dragOffset.x) > kotlin.math.abs(dragOffset.y)) {
                                    if (dragOffset.x > 0) move(1, 0) else move(-1, 0)
                                } else {
                                    if (dragOffset.y > 0) move(0, 1) else move(0, -1)
                                }
                                swipeHandled = true
                            }
                        }
                    }
                )
            }
            .padding(8.dp)
    ) {
        val tileSize = if (maxWidth > 24.dp) (maxWidth - 24.dp) / 4 else 0.dp
        
        if (tileSize > 0.dp) {
            // Draw empty grid
            for (i in 0..3) {
                for (j in 0..3) {
                    Box(
                        modifier = Modifier
                            .offset(x = (j * (tileSize.value + 8)).dp, y = (i * (tileSize.value + 8)).dp)
                            .size(tileSize)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0x55EEE4DA))
                    )
                }
            }
            
            // Draw tiles
            for (tile in tiles) {
                val offsetX by animateDpAsState(targetValue = (tile.col * (tileSize.value + 8)).dp, animationSpec = tween(150), label = "x")
                val offsetY by animateDpAsState(targetValue = (tile.row * (tileSize.value + 8)).dp, animationSpec = tween(150), label = "y")
                
                Box(
                    modifier = Modifier
                        .offset(x = offsetX, y = offsetY)
                        .size(tileSize)
                        .clip(RoundedCornerShape(4.dp))
                        .background(getTileColor(tile.value)),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.animation.AnimatedContent(
                        targetState = tile.value,
                        transitionSpec = {
                            (androidx.compose.animation.fadeIn(animationSpec = tween(200)) + 
                            androidx.compose.animation.scaleIn(initialScale = 0.5f, animationSpec = tween(200))) togetherWith androidx.compose.animation.fadeOut(animationSpec = tween(200))
                        },
                        label = "tile_anim_${tile.id}"
                    ) { targetValue ->
                        Text(
                            text = targetValue.toString(),
                            color = getTextColor(targetValue),
                            fontSize = if (targetValue > 1000) 20.sp else if (targetValue > 100) 24.sp else 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        
        if (gameOver) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x88EEE4DA)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Game Over!", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFF776E65))
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onReset,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8F7A66))
                    ) {
                        Text("Neustart", fontSize = 18.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ScoreBox(title: String, value: Int) {
    Column(
        modifier = Modifier
            .background(Color(0xFFBBADA0), RoundedCornerShape(4.dp))
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, color = Color(0xFFEEE4DA), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(value.toString(), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

fun getTileColor(value: Int): Color {
    return when (value) {
        0 -> Color(0x55EEE4DA)
        2 -> Color(0xFFEEE4DA)
        4 -> Color(0xFFEDE0C8)
        8 -> Color(0xFFF2B179)
        16 -> Color(0xFFF59563)
        32 -> Color(0xFFF67C5F)
        64 -> Color(0xFFF65E3B)
        128 -> Color(0xFFEDCF72)
        256 -> Color(0xFFEDCC61)
        512 -> Color(0xFFEDC850)
        1024 -> Color(0xFFEDC53F)
        2048 -> Color(0xFFEDC22E)
        else -> Color(0xFF3C3A32)
    }
}

fun getTextColor(value: Int): Color {
    return if (value <= 4) Color(0xFF776E65) else Color.White
}
