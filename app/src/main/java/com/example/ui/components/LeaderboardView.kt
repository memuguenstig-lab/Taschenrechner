package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.AppViewModel

@Composable
fun LeaderboardView(viewModel: AppViewModel) {
    val profiles by viewModel.userProfiles.collectAsState()
    val sortedProfiles = profiles.sortedByDescending { it.coins }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Leaderboard", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn {
            items(sortedProfiles) { profile ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(profile.name)
                        Text("${profile.coins} Coins")
                    }
                }
            }
        }
    }
}
