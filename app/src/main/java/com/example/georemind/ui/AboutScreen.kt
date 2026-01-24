package com.example.georemind.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.georemind.R
import androidx.core.net.toUri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text("About") },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back"
                                )
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

            Spacer(modifier = Modifier.weight(1f))

            // Developer Section
            Divider()

            Text(text = "Developer:", style = MaterialTheme.typography.titleSmall)
            Text(
                    text = "Piyush Duggal + AI",
                    style =
                            MaterialTheme.typography.bodyMedium.copy(
                                    textDecoration = TextDecoration.Underline,
                                    color = MaterialTheme.colorScheme.primary
                            ),
                    modifier =
                            Modifier.clickable {
                                val intent =
                                        Intent(
                                                Intent.ACTION_VIEW,
                                            "https://piyushduggal.me/".toUri()
                                        )
                                context.startActivity(intent)
                            }
            )

            // GitHub Icon Link at Bottom Center
            Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
            ) {
                IconButton(
                        onClick = {
                            val intent =
                                    Intent(
                                            Intent.ACTION_VIEW,
                                        "https://github.com/PiyushDuggal-source/geoRemind".toUri()
                                    )
                            context.startActivity(intent)
                        }
                ) {
                    Icon(
                            painter = painterResource(id = R.drawable.ic_github),
                            contentDescription = "GitHub",
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
