package com.substream.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.substream.data.preferences.ServerPreferences
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/**
 * Settings tab: shows the connected server, verifies the stored credentials, and
 * allows changing the server or logging out.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onChangeServer: () -> Unit = {},
    serverPreferences: ServerPreferences = koinInject(),
    viewModel: PingViewModel = koinViewModel(),
) {
    val config by serverPreferences.config.collectAsState()
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Server", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = config?.url ?: "Not configured",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "User: ${config?.username ?: "Not configured"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            when (val current = state) {
                is PingState.Loading -> CircularProgressIndicator()

                is PingState.Success -> Text(
                    text = "Connected • API ${current.data.version}",
                    color = MaterialTheme.colorScheme.primary
                )

                is PingState.Error -> Text(
                    text = current.message,
                    color = MaterialTheme.colorScheme.error
                )

                PingState.Idle -> Unit
            }

            OutlinedButton(
                onClick = viewModel::testConnection,
                enabled = state !is PingState.Loading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Test connection")
            }

            OutlinedButton(
                onClick = onChangeServer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Change server")
            }

            Button(
                onClick = serverPreferences::logout,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Log out")
            }
        }
    }
}
