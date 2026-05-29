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

@Composable
fun TicTacToeGame(
    wins: Int,
    onWinUpdate: (Int) -> Unit,
    onBack: () -> Unit
) {
    var board by remember { mutableStateOf(List(9) { "" }) }
    var currentPlayer by remember { mutableStateOf("X") }
    var winner by remember { mutableStateOf<String?>(null) }
    var resultText by remember { mutableStateOf("") }

    val checkWinner = {
        val winCombos = listOf(
            listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8), // rows
            listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8), // cols
            listOf(0, 4, 8), listOf(2, 4, 6) // diagonals
        )
        var winDetected = false
        for (combo in winCombos) {
            val (a, b, c) = combo
            if (board[a].isNotEmpty() && board[a] == board[b] && board[a] == board[c]) {
                winner = board[a]
                resultText = "Spieler ${board[a]} gewinnt!"
                winDetected = true
                if (board[a] == "X") {
                    onWinUpdate(wins + 1)
                }
                break
            }
        }
        if (!winDetected && board.none { it.isEmpty() }) {
            winner = "Draw"
            resultText = "Unentschieden!"
        }
    }

    val onCellClick = { index: Int ->
        if (winner == null && board[index].isEmpty()) {
            val newBoard = board.toMutableList()
            newBoard[index] = currentPlayer
            board = newBoard
            checkWinner()
            if (winner == null) {
                currentPlayer = if (currentPlayer == "X") "O" else "X"
            }
        }
    }

    val resetGame = {
        board = List(9) { "" }
        currentPlayer = "X"
        winner = null
        resultText = ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFDFBFF))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Bar equivalent
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Zurück")
            }
            Text("TIC TAC TOE", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF1B1B1F))
            IconButton(onClick = resetGame) {
                Icon(Icons.Default.Refresh, contentDescription = "Neustart")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Siege (X): $wins", fontWeight = FontWeight.Bold, color = Color(0xFF3B82F6))
            Text("Am Zug: Spieler $currentPlayer", fontWeight = FontWeight.SemiBold, color = Color(0xFF44474E))
        }

        Spacer(modifier = Modifier.weight(1f))

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(Color(0xFFE2E2EC), RoundedCornerShape(16.dp))
                .padding(8.dp)
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
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(Color.White, RoundedCornerShape(8.dp))
                                .clickable { onCellClick(index) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = board[index],
                                fontSize = 64.sp,
                                fontWeight = FontWeight.Black,
                                color = if (board[index] == "X") Color(0xFF3B82F6) else Color(0xFFEC4899)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        if (winner != null) {
            Text(
                text = resultText,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = if (winner == "X") Color(0xFF3B82F6) else if (winner == "O") Color(0xFFEC4899) else Color(0xFF44474E)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
