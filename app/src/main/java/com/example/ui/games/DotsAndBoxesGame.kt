package com.example.ui.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.pow

@Composable
fun DotsAndBoxesGame(
    onBack: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    var gridWidth by remember { mutableIntStateOf(5) } // Default is 5x5 boxes for a much larger playing field!
    var gridHeight by remember { mutableIntStateOf(5) }
    val dotsX = gridWidth + 1
    val dotsY = gridHeight + 1

    // Horizontal lines: true/false if drawn. Key format: "c,r" (from c,r to c+1,r)
    val horizontalLines = remember { mutableStateMapOf<String, Int>() } // value is player ID (1 or 2)
    // Vertical lines: true/false if drawn. Key format: "c,r" (from c,r to c,r+1)
    val verticalLines = remember { mutableStateMapOf<String, Int>() } // value is player ID (1 or 2)
    // Box owners: Key format: "c,r" -> value is player ID (1 or 2)
    val boxOwners = remember { mutableStateMapOf<String, Int>() }

    var currentPlayer by remember { mutableStateOf(1) }
    var player1Score by remember { mutableStateOf(0) }
    var player2Score by remember { mutableStateOf(0) }
    var gameMessage by remember { mutableStateOf("Spieler 1 (Blau) - Platziere einen Strich") }
    var isGameOver by remember { mutableStateOf(false) }
    var showRules by remember { mutableStateOf(false) }

    fun resetGame() {
        horizontalLines.clear()
        verticalLines.clear()
        boxOwners.clear()
        currentPlayer = 1
        player1Score = 0
        player2Score = 0
        gameMessage = "Spieler 1 (Blau) am Zug"
        isGameOver = false
    }

    // Helper to check if a specific box is captured
    fun isBoxFull(c: Int, r: Int): Boolean {
        val top = horizontalLines.containsKey("$c,$r")
        val bottom = horizontalLines.containsKey("$c,${r + 1}")
        val left = verticalLines.containsKey("$c,$r")
        val right = verticalLines.containsKey("${c + 1},$r")
        return top && bottom && left && right
    }

    // Handle line placement
    fun placeLine(isHorizontal: Boolean, c: Int, r: Int): Boolean {
        val key = "$c,$r"
        if (isHorizontal) {
            if (horizontalLines.containsKey(key)) return false
            horizontalLines[key] = currentPlayer
        } else {
            if (verticalLines.containsKey(key)) return false
            verticalLines[key] = currentPlayer
        }

        // Check if any box gets completed
        var boxCaptured = false
        val newBoxes = mutableListOf<Pair<Int, Int>>()

        if (isHorizontal) {
            // Horizontal line impacts box below (c, r) and box above (c, r-1)
            if (r < gridHeight && isBoxFull(c, r) && !boxOwners.containsKey("$c,$r")) {
                newBoxes.add(Pair(c, r))
                boxCaptured = true
            }
            if (r > 0 && isBoxFull(c, r - 1) && !boxOwners.containsKey("$c,${r - 1}")) {
                newBoxes.add(Pair(c, r - 1))
                boxCaptured = true
            }
        } else {
            // Vertical line impacts box to the right (c, r) and box to the left (c-1, r)
            if (c < gridWidth && isBoxFull(c, r) && !boxOwners.containsKey("$c,$r")) {
                newBoxes.add(Pair(c, r))
                boxCaptured = true
            }
            if (c > 0 && isBoxFull(c - 1, r) && !boxOwners.containsKey("${c - 1},$r")) {
                newBoxes.add(Pair(c - 1, r))
                boxCaptured = true
            }
        }

        if (boxCaptured) {
            for (box in newBoxes) {
                boxOwners["${box.first},${box.second}"] = currentPlayer
                if (currentPlayer == 1) {
                    player1Score++
                } else {
                    player2Score++
                }
            }

            // Check if game is over
            if (boxOwners.size == gridWidth * gridHeight) {
                isGameOver = true
                gameMessage = when {
                    player1Score > player2Score -> "Spieler 1 (Blau) gewinnt das Match!"
                    player2Score > player1Score -> "Spieler 2 (Pink) gewinnt das Match!"
                    else -> "Unentschieden!"
                }
            } else {
                gameMessage = "Box erobert! Spieler $currentPlayer darf nochmal"
            }
        } else {
            // Pass turn
            currentPlayer = if (currentPlayer == 1) 2 else 1
            gameMessage = "Spieler $currentPlayer (${if (currentPlayer == 1) "Blau" else "Pink"}) ist am Zug"
        }
        return true
    }

    if (showRules) {
        AlertDialog(
            onDismissRequest = { showRules = false },
            title = { Text("Spielregeln — Kästchenspiel") },
            text = {
                Text(
                    "1. Verbinde abwechselnd zwei benachbarte Punkte mit einer Linie.\n" +
                    "2. Wer die vierte Wand eines Kästchens schließt, erhält einen Punkt und MUSS sofort einen weiteren Zug machen.\n" +
                    "3. Das Spiel endet, wenn alle 9 Kästchen geschlossen sind.\n" +
                    "4. Der Spieler mit den meisten geschlossenen Kästchen gewinnt!",
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { showRules = false }) {
                    Text("Verstanden", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Upper Navigation & Action Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück", tint = Color.White)
            }
            Text(
                "KÄSTCHENSPIEL (DOTS & BOXES)",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            Row {
                IconButton(onClick = { showRules = true }) {
                    Icon(Icons.Default.Info, contentDescription = "Regeln", tint = Color.LightGray)
                }
                IconButton(onClick = ::resetGame) {
                    Icon(Icons.Default.Refresh, contentDescription = "Neustart", tint = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Dynamic Size Selector Segmented Control
        Row(
            modifier = Modifier
                .padding(bottom = 12.dp)
                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Spielfeld:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.LightGray,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            listOf(3 to "3x3", 4 to "4x4", 5 to "5x5").forEach { (sz, label) ->
                val isSelected = gridWidth == sz
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color(0xFF00FFCC) else Color.Transparent)
                        .clickable {
                            if (gridWidth != sz) {
                                gridWidth = sz
                                gridHeight = sz
                                resetGame()
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        color = if (isSelected) Color.Black else Color.White,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        // Visual Layout splitting depending on screen orientation
        if (isLandscape) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Panel Scoreboard & Status
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    ScoreboardView(
                        currentPlayer = currentPlayer,
                        p1Score = player1Score,
                        p2Score = player2Score,
                        isGameOver = isGameOver,
                        gameMessage = gameMessage
                    )
                }

                // Right Panel Game Board Canvas
                Box(
                    modifier = Modifier
                        .weight(1.3f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    DotsAndBoxesCanvas(
                        dotsX = dotsX,
                        dotsY = dotsY,
                        gridWidth = gridWidth,
                        gridHeight = gridHeight,
                        horizontalLines = horizontalLines,
                        verticalLines = verticalLines,
                        boxOwners = boxOwners,
                        onDragLine = { isHoriz, c, r ->
                            if (!isGameOver) {
                                placeLine(isHoriz, c, r)
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize(0.9f)
                            .aspectRatio(1f)
                    )
                }
            }
        } else {
            // Portrait Layout
            ScoreboardView(
                currentPlayer = currentPlayer,
                p1Score = player1Score,
                p2Score = player2Score,
                isGameOver = isGameOver,
                gameMessage = gameMessage
            )

            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                DotsAndBoxesCanvas(
                    dotsX = dotsX,
                    dotsY = dotsY,
                    gridWidth = gridWidth,
                    gridHeight = gridHeight,
                    horizontalLines = horizontalLines,
                    verticalLines = verticalLines,
                    boxOwners = boxOwners,
                    onDragLine = { isHoriz, c, r ->
                        if (!isGameOver) {
                            placeLine(isHoriz, c, r)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .aspectRatio(1f)
                )
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun ScoreboardView(
    currentPlayer: Int,
    p1Score: Int,
    p2Score: Int,
    isGameOver: Boolean,
    gameMessage: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                // Player 1 Card
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .background(
                            if (currentPlayer == 1 && !isGameOver) Color(0xFF0A84FF).copy(alpha = 0.2f) else Color.Transparent,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp)
                ) {
                    Text("Spieler 1", color = Color(0xFF0A84FF), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("$p1Score", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black)
                    Text("Kästchen", color = Color.Gray, fontSize = 10.sp)
                }

                // Player 2 Card
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .background(
                            if (currentPlayer == 2 && !isGameOver) Color(0xFFEC4899).copy(alpha = 0.2f) else Color.Transparent,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp)
                ) {
                    Text("Spieler 2", color = Color(0xFFEC4899), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("$p2Score", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black)
                    Text("Kästchen", color = Color.Gray, fontSize = 10.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = gameMessage,
                color = if (isGameOver) Color(0xFF10B981) else Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun DotsAndBoxesCanvas(
    dotsX: Int,
    dotsY: Int,
    gridWidth: Int,
    gridHeight: Int,
    horizontalLines: Map<String, Int>,
    verticalLines: Map<String, Int>,
    boxOwners: Map<String, Int>,
    onDragLine: (Boolean, Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(16.dp))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val w = size.width
                    val h = size.height
                    
                    val paddingX = w * 0.12f
                    val paddingY = h * 0.12f
                    
                    val colWidth = (w - 2 * paddingX) / (dotsX - 1)
                    val rowHeight = (h - 2 * paddingY) / (dotsY - 1)
                    
                    // Convert click positions into local grid float space
                    val gridX = (offset.x - paddingX) / colWidth
                    val gridY = (offset.y - paddingY) / rowHeight

                    // Find nearest horizontal or vertical line
                    var closestIsHorizontal = true
                    var closestCol = -1
                    var closestRow = -1
                    var minDistanceSq = Double.MAX_VALUE

                    // 1. Evaluate closest Horizontal lines
                    for (r in 0 until dotsY) {
                        for (c in 0 until gridWidth) {
                            // Nearest x point on line segment [c, c+1] at y = r
                            val closestSegmentX = gridX.coerceIn(c.toFloat(), (c + 1).toFloat())
                            val distSq = (gridX - closestSegmentX).pow(2) + (gridY - r).pow(2)
                            if (distSq < minDistanceSq) {
                                minDistanceSq = distSq.toDouble()
                                closestIsHorizontal = true
                                closestCol = c
                                closestRow = r
                            }
                        }
                    }

                    // 2. Evaluate closest Vertical lines
                    for (c in 0 until dotsX) {
                        for (r in 0 until gridHeight) {
                            // Nearest y point on line segment [r, r+1] at x = c
                            val closestSegmentY = gridY.coerceIn(r.toFloat(), (r + 1).toFloat())
                            val distSq = (gridY - closestSegmentY).pow(2) + (gridX - c).pow(2)
                            if (distSq < minDistanceSq) {
                                minDistanceSq = distSq.toDouble()
                                closestIsHorizontal = false
                                closestCol = c
                                closestRow = r
                            }
                        }
                    }

                    // Ensure tap matches close range (~0.35 grid size threshold)
                    if (minDistanceSq < 0.15) {
                        onDragLine(closestIsHorizontal, closestCol, closestRow)
                    }
                }
            }
    ) {
        val w = size.width
        val h = size.height

        val paddingX = w * 0.12f
        val paddingY = h * 0.12f

        val colWidth = (w - 2 * paddingX) / (dotsX - 1)
        val rowHeight = (h - 2 * paddingY) / (dotsY - 1)

        // Draw Captured Boxes Backgrounds
        for (r in 0 until gridHeight) {
            for (c in 0 until gridWidth) {
                val owner = boxOwners["$c,$r"] ?: 0
                if (owner > 0) {
                    val bx = paddingX + c * colWidth
                    val by = paddingY + r * rowHeight
                    val centerX = bx + colWidth / 2f
                    val centerY = by + rowHeight / 2f
                    
                    // Draw Rounded Rect Background
                    drawRoundRect(
                        color = if (owner == 1) Color(0xFF0A84FF).copy(alpha = 0.15f) else Color(0xFFEC4899).copy(alpha = 0.15f),
                        topLeft = Offset(bx + 6f, by + 6f),
                        size = Size(colWidth - 12f, rowHeight - 12f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
                    )
                    // Draw Rounded Rect Border
                    drawRoundRect(
                        color = if (owner == 1) Color(0xFF0A84FF).copy(alpha = 0.4f) else Color(0xFFEC4899).copy(alpha = 0.4f),
                        topLeft = Offset(bx + 6f, by + 6f),
                        size = Size(colWidth - 12f, rowHeight - 12f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                    )
                    
                    // Draw glowing center symbol
                    if (owner == 1) {
                        // Player 1 (Blue): Concentric Rings
                        drawCircle(
                            color = Color(0xFF0A84FF).copy(alpha = 0.15f),
                            radius = minOf(colWidth, rowHeight) * 0.22f,
                            center = Offset(centerX, centerY)
                        )
                        drawCircle(
                            color = Color(0xFF0A84FF),
                            radius = minOf(colWidth, rowHeight) * 0.12f,
                            center = Offset(centerX, centerY),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
                        )
                    } else {
                        // Player 2 (Pink): Elegant Cross
                        val sizeFactor = minOf(colWidth, rowHeight) * 0.12f
                        drawLine(
                            color = Color(0xFFEC4899),
                            start = Offset(centerX - sizeFactor, centerY - sizeFactor),
                            end = Offset(centerX + sizeFactor, centerY + sizeFactor),
                            strokeWidth = 5f,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = Color(0xFFEC4899),
                            start = Offset(centerX + sizeFactor, centerY - sizeFactor),
                            end = Offset(centerX - sizeFactor, centerY + sizeFactor),
                            strokeWidth = 5f,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
        }

        // Draw Grid Lines (Unfilled placeholders with neon glow overlay)
        val strokeWidthNormal = 4f
        val strokeWidthFilled = 12f

        for (r in 0 until dotsY) {
            for (c in 0 until gridWidth) {
                val owner = horizontalLines["$c,$r"] ?: 0
                val startX = paddingX + c * colWidth
                val endX = paddingX + (c + 1) * colWidth
                val y = paddingY + r * rowHeight

                val color = when (owner) {
                    1 -> Color(0xFF0A84FF)
                    2 -> Color(0xFFEC4899)
                    else -> Color.White.copy(alpha = 0.08f)
                }
                
                if (owner > 0) {
                    // Underlay Glow line
                    drawLine(
                        color = color.copy(alpha = 0.3f),
                        start = Offset(startX, y),
                        end = Offset(endX, y),
                        strokeWidth = strokeWidthFilled + 8f,
                        cap = StrokeCap.Round
                    )
                }
                
                drawLine(
                    color = color,
                    start = Offset(startX, y),
                    end = Offset(endX, y),
                    strokeWidth = if (owner > 0) strokeWidthFilled else strokeWidthNormal,
                    cap = StrokeCap.Round
                )
            }
        }

        for (c in 0 until dotsX) {
            for (r in 0 until gridHeight) {
                val owner = verticalLines["$c,$r"] ?: 0
                val x = paddingX + c * colWidth
                val startY = paddingY + r * rowHeight
                val endY = paddingY + (r + 1) * rowHeight

                val color = when (owner) {
                    1 -> Color(0xFF0A84FF)
                    2 -> Color(0xFFEC4899)
                    else -> Color.White.copy(alpha = 0.08f)
                }
                
                if (owner > 0) {
                    // Underlay Glow line
                    drawLine(
                        color = color.copy(alpha = 0.3f),
                        start = Offset(x, startY),
                        end = Offset(x, endY),
                        strokeWidth = strokeWidthFilled + 8f,
                        cap = StrokeCap.Round
                    )
                }
                
                drawLine(
                    color = color,
                    start = Offset(x, startY),
                    end = Offset(x, endY),
                    strokeWidth = if (owner > 0) strokeWidthFilled else strokeWidthNormal,
                    cap = StrokeCap.Round
                )
            }
        }

        // Draw Dot Grid Nodes with soft glowing halos
        for (r in 0 until dotsY) {
            for (c in 0 until dotsX) {
                val cx = paddingX + c * colWidth
                val cy = paddingY + r * rowHeight
                
                // Outer glow shadow circle
                drawCircle(
                    color = Color.White.copy(alpha = 0.15f),
                    radius = 16f,
                    center = Offset(cx, cy)
                )
                // Outer clean circle
                drawCircle(
                    color = Color(0xFF334155),
                    radius = 9f,
                    center = Offset(cx, cy)
                )
                // Inner core
                drawCircle(
                    color = Color.White,
                    radius = 4f,
                    center = Offset(cx, cy)
                )
            }
        }
    }
}
