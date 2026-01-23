package com.example.georemind.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.georemind.viewmodel.TaskViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onNavigateBack: () -> Unit, viewModel: TaskViewModel = viewModel()) {
    val defaultRadius by viewModel.defaultRadius.collectAsState(initial = 200f)
    var radiusInput by remember(defaultRadius) { mutableStateOf(defaultRadius.toInt().toString()) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text("Settings") },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                    text = "Default Task Settings",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                        text = "Default Geofence Radius (meters)",
                        style = MaterialTheme.typography.bodyMedium
                )

                OutlinedTextField(
                        value = radiusInput,
                        onValueChange = { newValue ->
                            if (newValue.all { it.isDigit() }) {
                                radiusInput = newValue
                                // Auto-save logic or validate on focus lost could go here
                                // For now, let's just save valid values immediately or provide a
                                // save button?
                                // Let's autosave for simplicity if valid
                                newValue.toFloatOrNull()?.let { radius ->
                                    if (radius >= 30f && radius <= 1000f) {
                                        viewModel.saveDefaultRadius(radius)
                                    }
                                }
                            }
                        },
                        label = { Text("Radius (30m - 1000m)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                )

                Text(
                        text = "This radius will be used when you first tap to create a new task.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
