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
import com.example.georemind.ui.LocationPermissionHandler
import com.example.georemind.ui.MapScreen
import com.example.georemind.ui.theme.GeoRemindTheme

/**
 * Main activity for GeoRemind app.
 * Handles permission flow and displays the main map screen.
 */
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
                        MapScreen()
                    } else {
                        // Permission request flow
                        LocationPermissionHandler(
                            onAllPermissionsGranted = {
                                permissionsGranted = true
                            }
                        )
                    }
                }
            }
        }
    }
}
