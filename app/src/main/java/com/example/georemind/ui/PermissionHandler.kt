package com.example.georemind.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.georemind.R
import com.google.accompanist.permissions.*

/**
 * Handles the multi-step location permission flow required for geofencing.
 * 
 * Permission flow:
 * 1. Request foreground location (FINE + COARSE)
 * 2. Request background location (ACCESS_BACKGROUND_LOCATION) - separate on Android 11+
 * 3. Request notification permission (Android 13+)
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LocationPermissionHandler(
    onAllPermissionsGranted: () -> Unit
) {
    val context = LocalContext.current
    
    // Foreground location permissions
    val foregroundPermissionState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )
    
    // Background location permission (Android Q+)
    val backgroundPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        rememberPermissionState(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    } else null
    
    // Notification permission (Android 13+)
    val notificationPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else null
    
    // Check if all required permissions are granted
    val foregroundGranted = foregroundPermissionState.allPermissionsGranted
    val backgroundGranted = backgroundPermissionState?.status?.isGranted ?: true
    val notificationGranted = notificationPermissionState?.status?.isGranted ?: true
    
    LaunchedEffect(foregroundGranted, backgroundGranted, notificationGranted) {
        if (foregroundGranted && backgroundGranted && notificationGranted) {
            onAllPermissionsGranted()
        }
    }
    
    when {
        // Step 1: Request foreground location
        !foregroundGranted -> {
            PermissionScreen(
                icon = Icons.Default.LocationOn,
                title = "Location Access Required",
                description = stringResource(R.string.foreground_permission_rationale),
                buttonText = stringResource(R.string.grant_permission),
                showSettingsButton = foregroundPermissionState.shouldShowRationale,
                onRequestPermission = { foregroundPermissionState.launchMultiplePermissionRequest() },
                onOpenSettings = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }
            )
        }
        
        // Step 2: Request background location (Android Q+)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !backgroundGranted -> {
            PermissionScreen(
                icon = Icons.Default.LocationOn,
                title = "Background Location Required",
                description = stringResource(R.string.background_permission_rationale),
                buttonText = stringResource(R.string.open_settings),
                showSettingsButton = true,
                onRequestPermission = {
                    // On Android 11+, we must direct user to settings
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    } else {
                        backgroundPermissionState?.launchPermissionRequest()
                    }
                },
                onOpenSettings = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }
            )
        }
        
        // Step 3: Request notification permission (Android 13+)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationGranted -> {
            PermissionScreen(
                icon = Icons.Default.Notifications,
                title = "Notification Permission",
                description = stringResource(R.string.notification_permission_rationale),
                buttonText = stringResource(R.string.grant_permission),
                showSettingsButton = notificationPermissionState?.status?.shouldShowRationale == true,
                onRequestPermission = { notificationPermissionState?.launchPermissionRequest() },
                onOpenSettings = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }
            )
        }
    }
}

/**
 * Generic permission request screen with rationale.
 */
@Composable
private fun PermissionScreen(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    buttonText: String,
    showSettingsButton: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = if (showSettingsButton) onOpenSettings else onRequestPermission,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(buttonText)
            }
            
            if (showSettingsButton) {
                Spacer(modifier = Modifier.height(8.dp))
                
                TextButton(onClick = onRequestPermission) {
                    Text("Try Again")
                }
            }
        }
    }
}
