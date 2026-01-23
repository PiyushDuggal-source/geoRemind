package com.example.georemind

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.georemind.ui.AboutScreen
import com.example.georemind.ui.LocationPermissionHandler
import com.example.georemind.ui.MapScreen
import com.example.georemind.ui.SettingsScreen
import com.example.georemind.ui.theme.GeoRemindTheme

/** Main activity for GeoRemind app. Handles permission flow and displays the main map screen. */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            GeoRemindTheme {
                Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                ) {
                    var permissionsGranted by remember { mutableStateOf(false) }

                    if (permissionsGranted) {
                        // Main app content
                        // Main app content
                        val navController = rememberNavController()
                        val sharedViewModel: com.example.georemind.viewmodel.TaskViewModel =
                                androidx.lifecycle.viewmodel.compose.viewModel()

                        NavHost(navController = navController, startDestination = "map") {
                            composable("map") {
                                MapScreen(
                                        viewModel = sharedViewModel,
                                        onNavigateToSettings = {
                                            navController.navigate("settings")
                                        },
                                        onNavigateToAbout = { navController.navigate("about") }
                                )
                            }
                            composable("settings") {
                                SettingsScreen(
                                        viewModel = sharedViewModel,
                                        onNavigateBack = { navController.popBackStack() }
                                )
                            }
                            composable("about") {
                                AboutScreen(onNavigateBack = { navController.popBackStack() })
                            }
                        }
                    } else {
                        // Permission request flow
                        LocationPermissionHandler(
                                onAllPermissionsGranted = { permissionsGranted = true }
                        )
                    }
                }
            }
        }
    }
}
