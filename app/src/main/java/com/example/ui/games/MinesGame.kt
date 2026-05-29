package com.example.ui.games

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

enum class MineTileState { HIDDEN, GEM, MINE }

@Composable
fun MinesGame(
    coins: Int,
    onCoinsUpdate: (Int) -> Unit,
    onBack: () -> Unit
) {
    val gridSize = 25
    var bet by remember { mutableIntStateOf(10) }
    var numMines by remember { mutableIntStateOf(3) }
    
    var isPlaying by remember { mutableStateOf(false) }
    var isGameOver by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf("") }
    var currentMultiplier by remember { mutableFloatStateOf(1.0f) }
    
    val tiles = remember { mutableStateListOf<MineTileState>() }
    val revealed = remember { mutableStateListOf<Boolean>() }
    
    fun initBoard() {
        tiles.clear()
        revealed.clear()
        for (i in 0 until gridSize) {
            tiles.add(MineTileState.GEM)
            revealed.add(false)
        }
        
        var minesPlaced = 0
        while (minesPlaced < numMines) {
            val idx = Random.nextInt(gridSize)
            if (tiles[idx] != MineTileState.MINE) {
                tiles[idx] = MineTileState.MINE
                minesPlaced++
            }
        }
    }
    
    fun startGame() {
        if (coins < bet) {
            resultMessage = "Nicht genug Coins!"
            return
        }
        onCoinsUpdate(coins - bet)
        isPlaying = true
        isGameOver = false
        resultMessage = ""
        currentMultiplier = 1.0f
        initBoard()
    }
    
    fun cashOut() {
        if (!isPlaying || isGameOver) return
        val winAmount = (bet * currentMultiplier).toInt()
        onCoinsUpdate(coins + winAmount)
        resultMessage = "Gewonnen: $winAmount \uD83D\uDCB0"
        isPlaying = false
        isGameOver = true
    }
    
    fun revealTile(index: Int) {
        if (!isPlaying || isGameOver || revealed[index]) return
        
        revealed[index] = true
        
        if (tiles[index] == MineTileState.MINE) {
            isGameOver = true
            isPlaying = false
            resultMessage = "BUMM! Du hast verloren \uD83D\uDCA3"
            // Reveal all
            for (i in 0 until gridSize) {
                revealed[i] = true
            }
        } else {
            // Increase multiplier
            currentMultiplier += (0.1f * numMines)
            if (revealed.count { it } == gridSize - numMines) {
                cashOut() // won everything
            }
        }
    }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    Card(
        modifier = Modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
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
                Text("MINES EXPLORER", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                Text("💰 $coins", style = MaterialTheme.typography.titleMedium, color = Color(0xFFEAB308))
            }
            
            if (isLandscape) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Column: Board
                    Box(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (tiles.isEmpty()) {
                            Text(
                                text = "Passe deinen Einsatz an\nund starte das Spiel!",
                                color = Color.White.copy(alpha = 0.6f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(5),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth(0.85f)
                            ) {
                                items(gridSize) { i ->
                                    Box(
                                        modifier = Modifier
                                            .aspectRatio(1f)
                                            .background(
                                                color = if (!revealed[i]) Color(0xFF334155) 
                                                        else if (tiles[i] == MineTileState.MINE) Color(0xFFEF4444) 
                                                        else Color(0xFF10B981),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable(enabled = isPlaying && !revealed[i]) { revealTile(i) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (revealed[i]) {
                                            Text(
                                                text = if (tiles[i] == MineTileState.MINE) "💣" else "💎",
                                                fontSize = 18.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Right Column: Controls, multiplier and results
                    Column(
                        modifier = Modifier
                            .weight(0.8f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.SpaceEvenly,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (resultMessage.isNotEmpty()) {
                            Text(
                                text = resultMessage,
                                color = if (resultMessage.contains("Gewonnen")) Color(0xFF10B981) else Color(0xFFEF4444),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }

                        MinesControlPanel(
                            isPlaying = isPlaying,
                            bet = bet,
                            numMines = numMines,
                            currentMultiplier = currentMultiplier,
                            onBetChange = { bet = it },
                            onNumMinesChange = { numMines = it },
                            onStart = { startGame() },
                            onCashOut = { cashOut() }
                        )
                    }
                }
            } else {
                // Portrait Layout
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (tiles.isEmpty()) {
                        Text(
                            text = "Passe deinen Einsatz an\nund starte das Spiel!",
                            color = Color.White.copy(alpha = 0.6f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(5),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            items(gridSize) { i ->
                                Box(
                                        modifier = Modifier
                                            .aspectRatio(1f)
                                            .background(
                                                color = if (!revealed[i]) Color(0xFF334155) 
                                                        else if (tiles[i] == MineTileState.MINE) Color(0xFFEF4444) 
                                                        else Color(0xFF10B981),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable(enabled = isPlaying && !revealed[i]) { revealTile(i) },
                                        contentAlignment = Alignment.Center
                                ) {
                                    if (revealed[i]) {
                                        Text(
                                            text = if (tiles[i] == MineTileState.MINE) "💣" else "💎",
                                            fontSize = 20.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                if (resultMessage.isNotEmpty()) {
                    Text(resultMessage, color = if (resultMessage.contains("Gewonnen")) Color(0xFF10B981) else Color(0xFFEF4444), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                MinesControlPanel(
                    isPlaying = isPlaying,
                    bet = bet,
                    numMines = numMines,
                    currentMultiplier = currentMultiplier,
                    onBetChange = { bet = it },
                    onNumMinesChange = { numMines = it },
                    onStart = { startGame() },
                    onCashOut = { cashOut() }
                )
            }
        }
    }
}

@Composable
fun MinesControlPanel(
    isPlaying: Boolean,
    bet: Int,
    numMines: Int,
    currentMultiplier: Float,
    onBetChange: (Int) -> Unit,
    onNumMinesChange: (Int) -> Unit,
    onStart: () -> Unit,
    onCashOut: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!isPlaying) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = { if (bet > 10) onBetChange(bet - 10) }) { Text("-") }
                Text("Einsatz: $bet", color = Color.White, fontSize = 15.sp)
                Button(onClick = { onBetChange(bet + 10) }) { Text("+") }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = { if (numMines > 1) onNumMinesChange(numMines - 1) }) { Text("-") }
                Text("Minen: $numMines", color = Color.White, fontSize = 15.sp)
                Button(onClick = { if (numMines < 20) onNumMinesChange(numMines + 1) }) { Text("+") }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onStart,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("SPIELEN", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            Text(
                text = "Multiplikator: ${String.format(java.util.Locale.US, "%.2f", currentMultiplier)}x", 
                color = Color(0xFF10B981), 
                fontSize = 18.sp, 
                fontWeight = FontWeight.Bold
            )
            Text("Möglicher Gewinn: ${(bet * currentMultiplier).toInt()}", color = Color.White, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onCashOut,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("CASH OUT", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
