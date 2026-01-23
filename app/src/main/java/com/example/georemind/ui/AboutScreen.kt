package com.example.georemind.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onNavigateBack: () -> Unit) {
    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text("About") },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        }
                )
            }
    ) { padding ->
        Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                    text = "GeoRemind",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
            )

            Text(text = "Version 1.0", style = MaterialTheme.typography.bodyLarge)

            Divider()

            Text(
                    text =
                            "A location-based reminder app that helps you remember tasks when you enter specific areas.",
                    style = MaterialTheme.typography.bodyMedium
            )

            Text(text = "Features:", style = MaterialTheme.typography.titleSmall)
            Text("- Geofence triggering")
            Text("- Custom radius")
            Text("- Push notifications")
            Text("- Google Maps integration")
        }
    }
}
