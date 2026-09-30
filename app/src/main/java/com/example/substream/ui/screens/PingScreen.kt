package com.example.substream.ui.screens

import com.example.substream.BuildConfig
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel

@Composable
fun PingScreen(
    viewModel: PingViewModel = koinViewModel()
) {
    // Collects the state from ViewModel and triggers recomposition on changes
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(
            // TODO: Replace with your actual username and password
            onClick = { viewModel.testConnection(BuildConfig.NAVIDROME_USER, BuildConfig.NAVIDROME_PASS) },
            enabled = state !is PingState.Loading
        ) {
            Text("Test Substream Connection")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // UI reacts to the current state
        when (val currentState = state) {
            is PingState.Idle -> Text("Press the button to ping the server.")
            is PingState.Loading -> CircularProgressIndicator()
            is PingState.Success -> {
                Text("Success! 🎉", color = Color(0xFF4CAF50)) // Green color
                Text("API Version: ${currentState.data.version}")
                Text("Status: ${currentState.data.status}")
            }
            is PingState.Error -> {
                Text("Error ❌", color = Color.Red)
                Text(currentState.message)
            }
        }
    }
}