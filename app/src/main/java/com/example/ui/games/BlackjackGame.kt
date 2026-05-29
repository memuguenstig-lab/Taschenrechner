package com.example.ui.games

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

data class BlackjackCard(
    val rank: String,
    val suit: String,
    val value: Int
)

fun createDeck(): MutableList<BlackjackCard> {
    val suits = listOf("♠", "♥", "♦", "♣")
    val ranks = listOf(
        "2" to 2, "3" to 3, "4" to 4, "5" to 5, "6" to 6, "7" to 7, "8" to 8, "9" to 9, "10" to 10,
        "J" to 10, "Q" to 10, "K" to 10, "A" to 11
    )
    val deck = mutableListOf<BlackjackCard>()
    for (suit in suits) {
        for ((rank, value) in ranks) {
            deck.add(BlackjackCard(rank, suit, value))
        }
    }
    return deck
}

fun calculateHandValue(hand: List<BlackjackCard>): Int {
    var total = hand.sumOf { it.value }
    var acesCount = hand.count { it.rank == "A" }
    while (total > 21 && acesCount > 0) {
        total -= 10
        acesCount--
    }
    return total
}

@Composable
fun BlackjackGame(
    coins: Int,
    onCoinsUpdate: (Int) -> Unit,
    onBack: () -> Unit
) {
    var bet by remember { mutableIntStateOf(10) }
    var resultMessage by remember { mutableStateOf("") }
    
    val playerHand = remember { mutableStateListOf<BlackjackCard>() }
    val dealerHand = remember { mutableStateListOf<BlackjackCard>() }
    var isGameOver by remember { mutableStateOf(true) }
    var isDealerSecondCardHidden by remember { mutableStateOf(true) }
    
    val deck = remember { mutableStateListOf<BlackjackCard>() }

    fun resetDeck() {
        deck.clear()
        deck.addAll(createDeck().shuffled())
    }

    fun drawCard(): BlackjackCard {
        if (deck.isEmpty()) {
            resetDeck()
        }
        return deck.removeAt(0)
    }

    fun startGame() {
        if (coins < bet) {
            resultMessage = "Nicht genug Coins!"
            return
        }
        onCoinsUpdate(coins - bet)
        resetDeck()
        
        playerHand.clear()
        dealerHand.clear()
        
        playerHand.add(drawCard())
        playerHand.add(drawCard())
        
        dealerHand.add(drawCard())
        dealerHand.add(drawCard())
        
        isGameOver = false
        isDealerSecondCardHidden = true
        resultMessage = ""
        
        // Check immediate player blackjack
        val playerVal = calculateHandValue(playerHand)
        if (playerVal == 21) {
            isGameOver = true
            isDealerSecondCardHidden = false
            val dealerVal = calculateHandValue(dealerHand)
            if (dealerVal == 21) {
                resultMessage = "Beide Blackjack! Unentschieden (Push)."
                onCoinsUpdate(coins + bet)
            } else {
                resultMessage = "Blackjack! Du gewinnst!"
                onCoinsUpdate(coins + (bet * 2.5).toInt())
            }
        }
    }

    fun hit() {
        if (isGameOver) return
        playerHand.add(drawCard())
        val playerVal = calculateHandValue(playerHand)
        if (playerVal > 21) {
            isGameOver = true
            isDealerSecondCardHidden = false
            resultMessage = "Bust! Du verliest."
        } else if (playerVal == 21) {
            // Auto stand
            isDealerSecondCardHidden = false
            var dealerVal = calculateHandValue(dealerHand)
            while (dealerVal < 17) {
                dealerHand.add(drawCard())
                dealerVal = calculateHandValue(dealerHand)
            }
            isGameOver = true
            if (dealerVal > 21) {
                resultMessage = "Dealer bust! Du gewinnst!"
                onCoinsUpdate(coins + bet * 2)
            } else if (dealerVal == 21) {
                resultMessage = "Beide 21! Unentschieden (Push)."
                onCoinsUpdate(coins + bet)
            } else {
                resultMessage = "21! Du gewinnst!"
                onCoinsUpdate(coins + bet * 2)
            }
        }
    }

    fun stand() {
        if (isGameOver) return
        isDealerSecondCardHidden = false
        var dealerVal = calculateHandValue(dealerHand)
        while (dealerVal < 17) {
            dealerHand.add(drawCard())
            dealerVal = calculateHandValue(dealerHand)
        }
        
        val playerVal = calculateHandValue(playerHand)
        isGameOver = true
        
        if (dealerVal > 21) {
            resultMessage = "Dealer bust! Du gewinnst!"
            onCoinsUpdate(coins + bet * 2)
        } else if (dealerVal > playerVal) {
            resultMessage = "Dealer gewinnt."
        } else if (playerVal > dealerVal) {
            resultMessage = "Du gewinnst!"
            onCoinsUpdate(coins + bet * 2)
        } else {
            resultMessage = "Unentschieden (Push)."
            onCoinsUpdate(coins + bet)
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    Card(
        modifier = Modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Screen Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück", tint = Color.White)
                }
                Text(
                    text = "BLACKJACK ROYAL",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "💰 $coins",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEAB308)
                )
            }

            if (isLandscape) {
                // Landscape Split Layout
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Left area: Hands Board
                    Column(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.SpaceEvenly,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        GameHandsSection(
                            playerHand = playerHand,
                            dealerHand = dealerHand,
                            isGameOver = isGameOver,
                            isDealerSecondCardHidden = isDealerSecondCardHidden
                        )
                    }

                    // Right area: Message, Stats & Controls Layout
                    Column(
                        modifier = Modifier
                            .weight(0.8f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            if (resultMessage.isNotEmpty()) {
                                Text(
                                    text = resultMessage,
                                    color = Color(0xFFFBBF24),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            } else if (isGameOver) {
                                Text(
                                    text = "Einsatz wählen und loslegen!",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        ControlsBox(
                            isGameOver = isGameOver,
                            bet = bet,
                            coins = coins,
                            onBetChange = { bet = it },
                            onStart = { startGame() },
                            onHit = { hit() },
                            onStand = { stand() }
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
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.SpaceAround,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        GameHandsSection(
                            playerHand = playerHand,
                            dealerHand = dealerHand,
                            isGameOver = isGameOver,
                            isDealerSecondCardHidden = isDealerSecondCardHidden
                        )

                        if (resultMessage.isNotEmpty()) {
                            Text(
                                text = resultMessage,
                                color = Color(0xFFFBBF24),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        } else if (isGameOver) {
                            Text(
                                text = "Wähle deinen Einsatz und klicke auf 'DEAL'!",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        }
                    }
                }

                ControlsBox(
                    isGameOver = isGameOver,
                    bet = bet,
                    coins = coins,
                    onBetChange = { bet = it },
                    onStart = { startGame() },
                    onHit = { hit() },
                    onStand = { stand() }
                )
            }
        }
    }
}

@Composable
fun GameHandsSection(
    playerHand: List<BlackjackCard>,
    dealerHand: List<BlackjackCard>,
    isGameOver: Boolean,
    isDealerSecondCardHidden: Boolean
) {
    if (playerHand.isEmpty() && dealerHand.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Platzieren Sie Ihre Wette,\num Blackjack zu spielen!",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )
        }
    } else {
        // Dealer hand
        val dealerScoreText = if (isDealerSecondCardHidden) {
            if (dealerHand.isNotEmpty()) "${dealerHand[0].value} + ?" else "0"
        } else {
            calculateHandValue(dealerHand).toString()
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "DEALER ($dealerScoreText)",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                dealerHand.forEachIndexed { index, card ->
                    val hidden = isDealerSecondCardHidden && index == 1
                    BlackjackCardView(card = card, isHidden = hidden)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Player hand
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "SPIELER (${calculateHandValue(playerHand)})",
                color = Color(0xFF10B981),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                playerHand.forEach { card ->
                    BlackjackCardView(card = card, isHidden = false)
                }
            }
        }
    }
}

@Composable
fun BlackjackCardView(card: BlackjackCard, isHidden: Boolean) {
    Box(
        modifier = Modifier
            .size(width = 64.dp, height = 96.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isHidden) Color(0xFFDC2626) else Color.White)
            .border(
                BorderStroke(2.dp, if (isHidden) Color.White else Color(0xFF94A3B8)),
                RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isHidden) {
            // Card back design
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)), RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "◆",
                    color = Color.White,
                    fontSize = 24.sp
                )
            }
        } else {
            // Card face details
            val isRed = card.suit == "♥" || card.suit == "♦"
            val suitColor = if (isRed) Color(0xFFDC2626) else Color(0xFF0F172A)
            
            Box(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                // Top-Left Value
                Text(
                    text = card.rank,
                    color = suitColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.TopStart)
                )
                
                // Suit Symbol Centered
                Text(
                    text = card.suit,
                    color = suitColor,
                    fontSize = 32.sp,
                    modifier = Modifier.align(Alignment.Center)
                )

                // Bottom-Right small suit
                Text(
                    text = card.suit,
                    color = suitColor,
                    fontSize = 13.sp,
                    modifier = Modifier.align(Alignment.BottomEnd)
                )
            }
        }
    }
}

@Composable
fun ControlsBox(
    isGameOver: Boolean,
    bet: Int,
    coins: Int,
    onBetChange: (Int) -> Unit,
    onStart: () -> Unit,
    onHit: () -> Unit,
    onStand: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)), RoundedCornerShape(16.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isGameOver) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { if (bet > 10) onBetChange(bet - 10) },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.1f))
                ) {
                    Text("-10", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Text(
                    text = "Einsatz: $bet",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )

                IconButton(
                    onClick = { onBetChange(bet + 10) },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.1f))
                ) {
                    Text("+10", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onStart,
                enabled = coins >= bet,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEF4444),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("DEAL", fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onHit,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Text("HIT", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                
                Button(
                    onClick = onStand,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Text("STAND", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
