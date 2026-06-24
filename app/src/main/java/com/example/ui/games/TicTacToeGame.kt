package com.example.ui.games

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.SupervisedUserCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

enum class TicTacToeMode {
    SINGLE_PLAYER, // VS Smart BOT
    PASS_AND_PLAY, // Normal Portrait 2 Players
    SPLIT_SCREEN   // Shared Face-to-Face Split-Screen
}

@Composable
fun TicTacToeGame(
    wins: Int,
    onWinUpdate: (Int) -> Unit,
    onBack: () -> Unit
) {
    var board by remember { mutableStateOf(List(9) { "" }) }
    var currentPlayer by remember { mutableStateOf("X") }
    var winner by remember { mutableStateOf<String?>(null) }
    var winningLine by remember { mutableStateOf<List<Int>?>(null) }
    var resultText by remember { mutableStateOf("") }

    // Mode Selector (Defaulting to Split-Screen to highlight the user's requested overhaul!)
    var gameMode by remember { mutableStateOf(TicTacToeMode.SPLIT_SCREEN) }

    // Session Statistics
    var winsX by remember { mutableStateOf(wins) }
    var winsO by remember { mutableStateOf(0) }
    var ties by remember { mutableStateOf(0) }

    val haptic = LocalHapticFeedback.current

    val resetGame = {
        board = List(9) { "" }
        currentPlayer = "X"
        winner = null
        winningLine = null
        resultText = ""
    }

    val makeMove = { index: Int ->
        if (winner == null && board[index].isEmpty()) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            val newBoard = board.toMutableList()
            newBoard[index] = currentPlayer
            board = newBoard

            // Check winning combinations
            val winCombos = listOf(
                listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8), // rows
                listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8), // cols
                listOf(0, 4, 8), listOf(2, 4, 6)                  // diagonals
            )
            var winDetected = false
            for (combo in winCombos) {
                val (a, b, c) = combo
                if (board[a].isNotEmpty() && board[a] == board[b] && board[a] == board[c]) {
                    winner = board[a]
                    winningLine = combo
                    resultText = "Spieler ${board[a]} gewinnt!"
                    winDetected = true

                    if (board[a] == "X") {
                        winsX++
                        onWinUpdate(winsX)
                    } else {
                        winsO++
                    }
                    break
                }
            }
            if (!winDetected && board.none { it.isEmpty() }) {
                winner = "Draw"
                ties++
                resultText = "Unentschieden!"
            }

            if (winner == null) {
                currentPlayer = if (currentPlayer == "X") "O" else "X"
            }
        }
    }

    // Smart Bot logic for Single Player mode
    LaunchedEffect(currentPlayer, gameMode, winner) {
        if (gameMode == TicTacToeMode.SINGLE_PLAYER && currentPlayer == "O" && winner == null) {
            delay(500) // Delay to feel like natural "thinking"
            val botMove = getSmartBotMove(board)
            if (botMove != -1) {
                makeMove(botMove)
            }
        }
    }

    // Color Theme Gradients
    val blueGradient = Brush.verticalGradient(listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364)))
    val pinkGradient = Brush.verticalGradient(listOf(Color(0xFF2D0B2E), Color(0xFF3B061A), Color(0xFF21000B)))
    val goldGradient = Brush.horizontalGradient(listOf(Color(0xFFFFD700), Color(0xFFFFA500)))

    // Active Pulsing Border for Split Screen Turn Indicators
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseSize by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutCirc),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseSize"
    )

    if (gameMode == TicTacToeMode.SPLIT_SCREEN) {
        // OVERHAULED 2-PLAYER SPLIT-SCREEN LAYOUT
        // Split vertically with Top player facing upside down (rotated 180f)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F1016))
        ) {
            // ================== PLAYER O (TOP VIEW - ROTATED 180 DEGREES) ==================
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .graphicsLayer(rotationZ = 180f)
                    .background(pinkGradient)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    val activeO = currentPlayer == "O" && winner == null
                    val scaleFactor = if (activeO) pulseSize else 1f

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.Close, contentDescription = "Beenden", tint = Color.White)
                        }
                        Text(
                            text = "DUELL (SPLIT)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF007F).copy(alpha = 0.8f),
                            letterSpacing = 1.5.sp
                        )
                        IconButton(onClick = resetGame) {
                            Icon(Icons.Default.Refresh, contentDescription = "Neustart", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Player Area with Pulsing Glow on turn
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = if (activeO) Color(0xFFFF007F).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f),
                        border = BorderStroke(
                            2.dp,
                            if (activeO) Color(0xFFFF007F) else Color.White.copy(alpha = 0.1f)
                        ),
                        modifier = Modifier
                            .scale(scaleFactor)
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                            .widthIn(max = 280.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFFFF007F), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "O",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Spieler O",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = if (activeO) "⌛ DEIN TURN!" else "Warte...",
                                    fontSize = 12.sp,
                                    color = if (activeO) Color(0xFFFF007F) else Color.White.copy(alpha = 0.5f),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Siege Spieler O: $winsO",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.weight(1.5f))

                    // Mode switch rotated 180 degrees
                    Button(
                        onClick = { gameMode = TicTacToeMode.PASS_AND_PLAY },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Modus wechseln 🔄", color = Color.White, fontSize = 12.sp)
                    }
                }
            }

            // ================== THE SHARED CENTRAL PLAY GRID (0 DEGREES) ==================
            Box(
                modifier = Modifier
                    .background(Color(0xFF0F1016))
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .aspectRatio(1f)
                        .background(Color(0xFF1E2030), RoundedCornerShape(24.dp))
                        .padding(12.dp)
                ) {
                    for (i in 0..2) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (j in 0..2) {
                                val index = i * 3 + j
                                val cellVal = board[index]
                                val isWinningCell = winningLine?.contains(index) == true

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            when {
                                                isWinningCell -> Color(0xFFFFD700).copy(alpha = 0.2f)
                                                cellVal == "X" -> Color(0xFF3B82F6).copy(alpha = 0.1f)
                                                cellVal == "O" -> Color(0xFFFF007F).copy(alpha = 0.1f)
                                                else -> Color(0xFF131420)
                                            }
                                        )
                                        .border(
                                            width = if (isWinningCell) 3.dp else 1.dp,
                                            color = when {
                                                isWinningCell -> Color(0xFFFFD700)
                                                cellVal == "X" -> Color(0xFF3B82F6).copy(alpha = 0.3f)
                                                cellVal == "O" -> Color(0xFFFF007F).copy(alpha = 0.3f)
                                                else -> Color(0xFF2C2F48)
                                            },
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .clickable { makeMove(index) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    AnimatedCellContent(text = cellVal, isPlayerTwo = cellVal == "O")
                                }
                            }
                        }
                    }
                }

                // Superimposed Overlay for results
                if (winner != null) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color(0xEE0F1016)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "SPIEL ENDE",
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = resultText,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = when (winner) {
                                    "X" -> Color(0xFF3B82F6)
                                    "O" -> Color(0xFFFF007F)
                                    else -> Color(0xFFFFD700)
                                },
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = resetGame,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2D48)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Nächste Runde", color = Color.White)
                            }
                        }
                    }
                }
            }

            // ================== PLAYER X (BOTTOM VIEW - NORMAL 0 DEGREES) ==================
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(blueGradient)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    val activeX = currentPlayer == "X" && winner == null
                    val scaleFactor = if (activeX) pulseSize else 1f

                    Button(
                        onClick = { gameMode = TicTacToeMode.PASS_AND_PLAY },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Modus wechseln 🔄", color = Color.White, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.weight(1.5f))

                    Text(
                        text = "Siege Spieler X: $winsX",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Player Area with Pulsing Glow on turn
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = if (activeX) Color(0xFF3B82F6).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f),
                        border = BorderStroke(
                            2.dp,
                            if (activeX) Color(0xFF3B82F6) else Color.White.copy(alpha = 0.1f)
                        ),
                        modifier = Modifier
                            .scale(scaleFactor)
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                            .widthIn(max = 280.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF3B82F6), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "X",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Spieler X",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = if (activeX) "⚡ DEIN TURN!" else "Warte...",
                                    fontSize = 12.sp,
                                    color = if (activeX) Color(0xFF3B82F6) else Color.White.copy(alpha = 0.5f),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.Close, contentDescription = "Beenden", tint = Color.White)
                        }
                        Text(
                            text = "DUELL (SPLIT)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF3B82F6).copy(alpha = 0.8f),
                            letterSpacing = 1.5.sp
                        )
                        IconButton(onClick = resetGame) {
                            Icon(Icons.Default.Refresh, contentDescription = "Neustart", tint = Color.White)
                        }
                    }
                }
            }
        }
    } else {
        // STANDARD / PORTRAIT MODE LAYOUT (Single Player Bot or Pass & Play)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F1016))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Zurück", tint = Color.White)
                }
                Text(
                    "TIC TAC TOE",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    color = Color.White,
                    letterSpacing = 2.sp
                )
                IconButton(onClick = resetGame) {
                    Icon(Icons.Default.Refresh, contentDescription = "Neustart", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Game Mode Toggles (Modern Rounded Switches)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2030)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val modes = listOf(
                        Triple(TicTacToeMode.SINGLE_PLAYER, "Solo vs BOT", Icons.Default.SmartToy),
                        Triple(TicTacToeMode.PASS_AND_PLAY, "Pass&Play", Icons.Default.Person),
                        Triple(TicTacToeMode.SPLIT_SCREEN, "Duell", Icons.Default.SupervisedUserCircle)
                    )

                    modes.forEach { (mode, name, icon) ->
                        val selected = gameMode == mode
                        Button(
                            onClick = {
                                gameMode = mode
                                resetGame()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selected) Color(0xFF3B82F6) else Color.Transparent,
                                contentColor = if (selected) Color.White else Color.White.copy(alpha = 0.6f)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(name, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Game Stats / Scores Dashboard
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Score X Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF142440)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Spieler X", color = Color(0xFF3B82F6), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("$winsX", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
                    }
                }

                // Balance Ties Card
                Card(
                    modifier = Modifier.weight(0.8f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E212D)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Remis", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Text("$ties", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
                    }
                }

                // Score O / Bot Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = if (gameMode == TicTacToeMode.SINGLE_PLAYER) Color(0xFF281E1E) else Color(0xFF381423)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (gameMode == TicTacToeMode.SINGLE_PLAYER) "Computer" else "Spieler O",
                            color = if (gameMode == TicTacToeMode.SINGLE_PLAYER) Color(0xFFFF5252) else Color(0xFFFF2E93),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text("$winsO", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Current Player Indicator
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.05f),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                color = if (currentPlayer == "X") Color(0xFF3B82F6) else Color(0xFFFF2E93),
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (gameMode == TicTacToeMode.SINGLE_PLAYER && currentPlayer == "O") {
                            "Bot berechnet Spielzug..."
                        } else {
                            "Am Zug: Spieler $currentPlayer"
                        },
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 3x3 Play Grid
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(Color(0xFF1E2030), RoundedCornerShape(24.dp))
                    .padding(12.dp)
            ) {
                for (i in 0..2) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (j in 0..2) {
                            val index = i * 3 + j
                            val cellVal = board[index]
                            val isWinningCell = winningLine?.contains(index) == true

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            when {
                                                isWinningCell -> Color(0xFFFFD700).copy(alpha = 0.2f)
                                                cellVal == "X" -> Color(0xFF3B82F6).copy(alpha = 0.1f)
                                                cellVal == "O" -> Color(0xFFFF007F).copy(alpha = 0.1f)
                                                else -> Color(0xFF131420)
                                            }
                                        )
                                        .border(
                                            width = if (isWinningCell) 3.dp else 1.dp,
                                            color = when {
                                                isWinningCell -> Color(0xFFFFD700)
                                                cellVal == "X" -> Color(0xFF3B82F6).copy(alpha = 0.3f)
                                                cellVal == "O" -> Color(0xFFFF007F).copy(alpha = 0.3f)
                                                else -> Color(0xFF2C2F48)
                                            },
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .clickable {
                                            if (currentPlayer == "X" || gameMode != TicTacToeMode.SINGLE_PLAYER) {
                                                makeMove(index)
                                            }
                                        },
                                contentAlignment = Alignment.Center
                            ) {
                                AnimatedCellContent(text = cellVal, isPlayerTwo = false)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Game over banner
            AnimatedVisibility(
                visible = winner != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2030)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = resultText,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = when (winner) {
                                "X" -> Color(0xFF3B82F6)
                                "O" -> Color(0xFFFF2E93)
                                else -> Color(0xFFFFD700)
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = resetGame,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                        ) {
                            Text("Nochmal spielen", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedCellContent(text: String, isPlayerTwo: Boolean) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(text) {
        if (text.isNotEmpty()) {
            visible = true
        } else {
            visible = false
        }
    }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    if (text.isNotEmpty()) {
        Box(
            modifier = Modifier
                .scale(scale)
                .graphicsLayer(rotationZ = if (isPlayerTwo) 180f else 0f),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 54.sp,
                fontWeight = FontWeight.Black,
                color = if (text == "X") Color(0xFF3B82F6) else Color(0xFFFF007F)
            )
        }
    }
}

fun getSmartBotMove(board: List<String>): Int {
    val winCombos = listOf(
        listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8),
        listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8),
        listOf(0, 4, 8), listOf(2, 4, 6)
    )

    // 1. Can Bot win in this turn?
    for (combo in winCombos) {
        val countO = combo.count { board[it] == "O" }
        val countEmpty = combo.count { board[it].isEmpty() }
        if (countO == 2 && countEmpty == 1) {
            return combo.first { board[it].isEmpty() }
        }
    }

    // 2. Can Bot block Player X from winning?
    for (combo in winCombos) {
        val countX = combo.count { board[it] == "X" }
        val countEmpty = combo.count { board[it].isEmpty() }
        if (countX == 2 && countEmpty == 1) {
            return combo.first { board[it].isEmpty() }
        }
    }

    // 3. Take Center
    if (board[4].isEmpty()) return 4

    // 4. Take Corner
    val corners = listOf(0, 2, 6, 8).filter { board[it].isEmpty() }
    if (corners.isNotEmpty()) {
        return corners.random()
    }

    // 5. Take Random Empty
    val emptyCells = board.mapIndexed { index, value -> index }.filter { board[it].isEmpty() }
    if (emptyCells.isNotEmpty()) {
        return emptyCells.random()
    }

    return -1
}
