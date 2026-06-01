package com.example.ui.games

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MemoryGame(
    highScore: Int, // The lowest moves to win
    onHighScoreUpdate: (Int) -> Unit,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val icons = listOf("🍎", "🍌", "🍇", "🍓", "🍍", "🥥", "🥝", "🍉")
    var cards by remember { mutableStateOf(generateCards(icons)) }
    
    var firstSelected by remember { mutableStateOf<Int?>(null) }
    var stopClicks by remember { mutableStateOf(false) }
    
    var moves by remember { mutableStateOf(0) }
    var isWin by remember { mutableStateOf(false) }

    // --- Multiplayer States ---
    var isMultiplayer by remember { mutableStateOf(false) }
    var activePlayer by remember { mutableStateOf(1) }
    var player1Pairs by remember { mutableStateOf(0) }
    var player2Pairs by remember { mutableStateOf(0) }

    val checkWin = {
        if (cards.all { it.isMatched }) {
            isWin = true
            if (!isMultiplayer) {
                if (highScore == 0 || moves < highScore) {
                    onHighScoreUpdate(moves)
                }
            }
        }
    }

    val onCardClick = { index: Int ->
        if (!stopClicks && !cards[index].isFaceUp && !cards[index].isMatched) {
            val newCards = cards.toMutableList()
            newCards[index] = newCards[index].copy(isFaceUp = true)
            cards = newCards

            if (firstSelected == null) {
                firstSelected = index
            } else {
                moves++
                val firstIdx = firstSelected!!
                if (cards[firstIdx].icon == cards[index].icon) {
                    // Match
                    val matchedCards = cards.toMutableList()
                    matchedCards[firstIdx] = matchedCards[firstIdx].copy(isMatched = true)
                    matchedCards[index] = matchedCards[index].copy(isMatched = true)
                    cards = matchedCards
                    firstSelected = null
                    
                    if (isMultiplayer) {
                        if (activePlayer == 1) {
                            player1Pairs++
                        } else {
                            player2Pairs++
                        }
                    }
                    
                    checkWin()
                } else {
                    // No match
                    stopClicks = true
                    coroutineScope.launch {
                        delay(1000)
                        val resetCards = cards.toMutableList()
                        resetCards[firstIdx] = resetCards[firstIdx].copy(isFaceUp = false)
                        resetCards[index] = resetCards[index].copy(isFaceUp = false)
                        cards = resetCards
                        firstSelected = null
                        stopClicks = false
                        
                        if (isMultiplayer) {
                            activePlayer = if (activePlayer == 1) 2 else 1
                        }
                    }
                }
            }
        }
    }

    val resetGame = {
        cards = generateCards(icons)
        firstSelected = null
        stopClicks = false
        moves = 0
        isWin = false
        activePlayer = 1
        player1Pairs = 0
        player2Pairs = 0
    }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFDFBFF))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Zurück")
            }
            Text("MEMORY PAIRS", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF1B1B1F))
            IconButton(onClick = resetGame) {
                Icon(Icons.Default.Refresh, contentDescription = "Neustart")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { isMultiplayer = false; resetGame() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!isMultiplayer) Color(0xFF3B5BA9) else Color(0xFFE2E2EC),
                    contentColor = if (!isMultiplayer) Color.White else Color(0xFF44474E)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text("Einzelspieler", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { isMultiplayer = true; resetGame() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isMultiplayer) Color(0xFFEC4899) else Color(0xFFE2E2EC),
                    contentColor = if (isMultiplayer) Color.White else Color(0xFF44474E)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("2 Spieler (Lokal)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
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
                // Left Area: Scores & messages
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Column(
                        modifier = Modifier
                            .background(Color(0xFFE2E2EC).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (!isMultiplayer) {
                            Text("Züge: $moves", fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = Color(0xFF44474E))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Bester Score: ${if (highScore > 0) highScore else "-"}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFFEC4899))
                        } else {
                            Text("Am Zug: Spieler $activePlayer", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = if (activePlayer == 1) Color(0xFF3B5BA9) else Color(0xFFEC4899))
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Spieler 1", color = Color(0xFF3B5BA9), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("$player1Pairs Paare", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Spieler 2", color = Color(0xFFEC4899), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("$player2Pairs Paare", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                        }
                    }
                    
                    if (isWin) {
                        Spacer(modifier = Modifier.height(16.dp))
                        val winText = if (!isMultiplayer) {
                            "Gewonnen in $moves Zügen!"
                        } else {
                            when {
                                player1Pairs > player2Pairs -> "Spieler 1 gewinnt ($player1Pairs vs $player2Pairs)!"
                                player2Pairs > player1Pairs -> "Spieler 2 gewinnt ($player2Pairs vs $player1Pairs)!"
                                else -> "Unentschieden ($player1Pairs vs $player2Pairs)!"
                            }
                        }
                        Text(
                            text = winText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                    }
                }

                // Right Area: Grid
                Box(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    MemoryBoardGrid(
                        cards = cards,
                        onCardClick = onCardClick,
                        modifier = Modifier.fillMaxHeight().aspectRatio(1f)
                    )
                }
            }
        } else {
            // Portrait Layout
            if (!isMultiplayer) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Züge: $moves", fontWeight = FontWeight.SemiBold, color = Color(0xFF44474E))
                    Text("Bester: ${if (highScore > 0) highScore else "-"}", fontWeight = FontWeight.Bold, color = Color(0xFFEC4899))
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("P1: $player1Pairs Paare", color = Color(0xFF3B5BA9), fontWeight = FontWeight.Bold)
                    Text("Spieler $activePlayer am Zug", color = if (activePlayer == 1) Color(0xFF3B5BA9) else Color(0xFFEC4899), fontWeight = FontWeight.Black)
                    Text("P2: $player2Pairs Paare", color = Color(0xFFEC4899), fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.weight(1.2f))

            MemoryBoardGrid(
                cards = cards,
                onCardClick = onCardClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            )

            Spacer(modifier = Modifier.weight(1f))

            if (isWin) {
                val winText = if (!isMultiplayer) {
                    "Gewonnen in $moves Zügen!"
                } else {
                    when {
                        player1Pairs > player2Pairs -> "Spieler 1 gewinnt das Duell!"
                        player2Pairs > player1Pairs -> "Spieler 2 gewinnt das Duell!"
                        else -> "Unentschieden!"
                    }
                }
                Text(
                    text = winText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun MemoryBoardGrid(
    cards: List<MemoryCard>,
    onCardClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .background(Color(0xFFE2E2EC), RoundedCornerShape(16.dp))
            .padding(8.dp)
    ) {
        for (i in 0..3) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (j in 0..3) {
                    val index = i * 4 + j
                    val card = cards[index]
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(
                                if (card.isFaceUp || card.isMatched) Color.White else Color(0xFF3B5BA9), 
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { onCardClick(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (card.isFaceUp || card.isMatched) {
                            Text(
                                text = card.icon,
                                fontSize = 32.sp,
                                color = if (card.isMatched) Color.Gray else Color.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

data class MemoryCard(
    val id: Int,
    val icon: String,
    val isFaceUp: Boolean = false,
    val isMatched: Boolean = false
)

fun generateCards(icons: List<String>): List<MemoryCard> {
    val doubled = (icons + icons).shuffled()
    return doubled.mapIndexed { index, icon -> MemoryCard(id = index, icon = icon) }
}
