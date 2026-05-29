package com.example.ui.games

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun SlotMachineGame(
    coins: Int,
    onCoinsUpdate: (Int) -> Unit,
    onBack: () -> Unit
) {
    val symbols = listOf("🍒", "🍋", "🔔", "⭐", "💎", "7️⃣")
    var slots by remember { mutableStateOf(listOf("🍒", "🍒", "🍒")) }
    var resultText by remember { mutableStateOf("") }
    var isSpinning by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val spinCost = 10

    fun spin() {
        if (coins < spinCost || isSpinning) {
            if (coins < spinCost) resultText = "Nicht genug Münzen!"
            return
        }
        
        isSpinning = true
        onCoinsUpdate(coins - spinCost)
        resultText = "Rollen drehen..."

        coroutineScope.launch {
            // Launch independent spinning loops that slow down and stop one by one
            val job1 = launch {
                for (i in 0..12) {
                    slots = listOf(symbols.random(), slots[1], slots[2])
                    delay((60 + i * 15).toLong())
                }
            }
            val job2 = launch {
                for (i in 0..18) {
                    slots = listOf(slots[0], symbols.random(), slots[2])
                    delay((60 + i * 15).toLong())
                }
            }
            val job3 = launch {
                for (i in 0..24) {
                    slots = listOf(slots[0], slots[1], symbols.random())
                    delay((60 + i * 15).toLong())
                }
            }

            job1.join()
            job2.join()
            job3.join()
            
            val (s1, s2, s3) = slots
            if (s1 == s2 && s2 == s3) {
                val win = when (s1) {
                    "7️⃣" -> 500
                    "💎" -> 200
                    "⭐" -> 100
                    else -> 50
                }
                resultText = "JACKPOT! +$win Münzen"
                onCoinsUpdate(coins - spinCost + win)
            } else if (s1 == s2 || s2 == s3 || s1 == s3) {
                resultText = "Kleiner Gewinn! +20 Münzen"
                onCoinsUpdate(coins - spinCost + 20)
            } else {
                resultText = "Leider nichts. Versuch's nochmal!"
            }
            isSpinning = false
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück", tint = Color.White)
            }
            Text(
                text = "Münzen: $coins 💰",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color(0xFFFBBF24)
            )
        }

        if (isLandscape) {
            // Adaptive horizontal layout
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Column: Reels
                Column(
                    modifier = Modifier.weight(1.2f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "GOLDEN SLOTS",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    ReelsDisplay(slots = slots)
                }

                // Right Column: Controls & Result
                Column(
                    modifier = Modifier
                        .weight(0.8f)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    Text(
                        text = resultText,
                        color = if (resultText.contains("Gewinn") || resultText.contains("JACKPOT")) Color(0xFF34D399) else Color(0xFFF87171),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { spin() },
                        enabled = !isSpinning && coins >= spinCost,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF59E0B),
                            disabledContainerColor = Color.Gray
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("SPIN (10 Münzen)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    }
                }
            }
        } else {
            // Portrait layout
            Spacer(modifier = Modifier.weight(1f))
            
            Text(
                text = "🏆 GOLDEN SLOTS 🏆",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.weight(0.5f))

            ReelsDisplay(slots = slots)

            Spacer(modifier = Modifier.weight(0.5f))

            Text(
                text = resultText,
                color = if (resultText.contains("Gewinn") || resultText.contains("JACKPOT")) Color(0xFF34D399) else Color(0xFFF87171),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.height(28.dp)
            )

            Spacer(modifier = Modifier.weight(0.5f))

            Button(
                onClick = { spin() },
                enabled = !isSpinning && coins >= spinCost,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF59E0B),
                    disabledContainerColor = Color.Gray
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("SPIN (10 Münzen)", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun ReelsDisplay(slots: List<String>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F172A), RoundedCornerShape(16.dp))
            .border(3.dp, Color(0xFFF59E0B), RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        slots.forEachIndexed { index, symbol ->
            Box(
                modifier = Modifier
                    .size(width = 68.dp, height = 80.dp)
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .border(2.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = symbol,
                    transitionSpec = {
                        slideInVertically { height -> -height } togetherWith slideOutVertically { height -> height }
                    },
                    label = "reel_$index"
                ) { targetSymbol ->
                    Text(
                        text = targetSymbol,
                        fontSize = 40.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
