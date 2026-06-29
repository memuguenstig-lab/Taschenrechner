package com.example.ui.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import com.example.ui.AppViewModel
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.min

data class TowerBlock(val x: Float, val y: Float, val width: Float, val height: Float)

@Composable
fun TowerStackGame(
    onBack: () -> Unit
) {
    var blocks by remember { mutableStateOf(listOf(TowerBlock(200f, 1500f, 400f, 100f))) }
    var movingBlock by remember { mutableStateOf(TowerBlock(0f, 1400f, 400f, 100f)) }
    var direction by remember { mutableStateOf(1f) }
    var gameState by remember { mutableStateOf("PLAYING") }

    LaunchedEffect(gameState) {
        if (gameState == "PLAYING") {
            while (true) {
                val nextX = movingBlock.x + (15f * direction)
                if (nextX > 800f || nextX < 0f) {
                    direction *= -1f
                }
                movingBlock = movingBlock.copy(x = nextX)
                delay(16)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
        detectTapGestures(onTap = {
            if (gameState != "PLAYING") return@detectTapGestures
            
            val topBlock = blocks.last()
            val overlapLeft = maxOf(movingBlock.x, topBlock.x)
            val overlapRight = minOf(movingBlock.x + movingBlock.width, topBlock.x + topBlock.width)
            val overlapWidth = overlapRight - overlapLeft

            if (overlapWidth <= 0) {
                gameState = "GAME_OVER"
            } else {
                val newBlock = TowerBlock(overlapLeft, topBlock.y - 100f, overlapWidth, 100f)
                blocks = blocks + newBlock
                movingBlock = TowerBlock(0f, newBlock.y - 100f, overlapWidth, 100f)
            }
        })
    }) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            blocks.forEach { block ->
                drawRect(color = Color.Cyan, topLeft = androidx.compose.ui.geometry.Offset(block.x, block.y), size = androidx.compose.ui.geometry.Size(block.width, block.height))
            }
            if (gameState == "PLAYING") {
                drawRect(color = Color.Magenta, topLeft = androidx.compose.ui.geometry.Offset(movingBlock.x, movingBlock.y), size = androidx.compose.ui.geometry.Size(movingBlock.width, movingBlock.height))
            }
        }
        if (gameState == "GAME_OVER") {
            Text("Game Over!", color = Color.White, modifier = Modifier.align(Alignment.Center))
        }
    }
}
