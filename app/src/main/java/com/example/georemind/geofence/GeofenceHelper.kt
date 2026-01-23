package com.example.georemind.geofence

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.georemind.data.TaskEntity
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

/**
 * Helper class for managing geofences.
 * Handles adding, removing, and updating geofences using the GeofencingClient API.
 */
class GeofenceHelper(private val context: Context) {
    
    companion object {
        private const val TAG = "GeofenceHelper"
        
        // Geofence expiration: never expire (use Long.MAX_VALUE equivalent)
        private const val GEOFENCE_EXPIRATION_MS = Geofence.NEVER_EXPIRE
        
        // Loitering delay for dwell trigger (not used in MVP, but available)
        private const val LOITERING_DELAY_MS = 30000 // 30 seconds
    }
    
    private val geofencingClient: GeofencingClient = 
        LocationServices.getGeofencingClient(context)
    
    /**
     * PendingIntent for the geofence broadcast receiver.
     * Uses FLAG_MUTABLE as required for Android 12+.
     */
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }
    
    /**
     * Check if we have the required location permissions.
     */
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Check if we have background location permission (required for geofencing).
     */
    fun hasBackgroundLocationPermission(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Not required before Android Q
        }
    }
    
    /**
     * Build a Geofence object from a TaskEntity.
     */
    private fun buildGeofence(task: TaskEntity): Geofence {
        return Geofence.Builder()
            .setRequestId(task.id.toString())
            .setCircularRegion(
                task.latitude,
                task.longitude,
                task.radiusMeters
            )
            .setExpirationDuration(GEOFENCE_EXPIRATION_MS)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()
    }
    
    /**
     * Build the GeofencingRequest with initial trigger.
     */
    private fun buildGeofencingRequest(geofence: Geofence): GeofencingRequest {
        return GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()
    }
    
    /**
     * Add a geofence for a task.
     */
    fun addGeofence(
        task: TaskEntity,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        if (!hasLocationPermission() || !hasBackgroundLocationPermission()) {
            onFailure(SecurityException("Location permission not granted"))
            return
        }
        
        val geofence = buildGeofence(task)
        val request = buildGeofencingRequest(geofence)
        
        try {
            geofencingClient.addGeofences(request, geofencePendingIntent)
                .addOnSuccessListener {
                    Log.d(TAG, "Geofence added successfully for task: ${task.id}")
                    onSuccess()
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Failed to add geofence for task: ${task.id}", exception)
                    onFailure(exception)
                }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception adding geofence", e)
            onFailure(e)
        }
    }
    
    /**
     * Add multiple geofences at once.
     */
    fun addGeofences(
        tasks: List<TaskEntity>,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        if (tasks.isEmpty()) {
            onSuccess()
            return
        }
        
        if (!hasLocationPermission() || !hasBackgroundLocationPermission()) {
            onFailure(SecurityException("Location permission not granted"))
            return
        }
        
        val geofences = tasks.map { buildGeofence(it) }
        
        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geofences)
            .build()
        
        try {
            geofencingClient.addGeofences(request, geofencePendingIntent)
                .addOnSuccessListener {
                    Log.d(TAG, "Added ${geofences.size} geofences successfully")
                    onSuccess()
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Failed to add geofences", exception)
                    onFailure(exception)
                }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception adding geofences", e)
            onFailure(e)
        }
    }
    
    /**
     * Remove a geofence by task ID.
     */
    fun removeGeofence(
        taskId: Long,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        geofencingClient.removeGeofences(listOf(taskId.toString()))
            .addOnSuccessListener {
                Log.d(TAG, "Geofence removed successfully for task: $taskId")
                onSuccess()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to remove geofence for task: $taskId", exception)
                onFailure(exception)
            }
    }
    
    /**
     * Remove all geofences registered by this app.
     */
    fun removeAllGeofences(
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        geofencingClient.removeGeofences(geofencePendingIntent)
            .addOnSuccessListener {
                Log.d(TAG, "All geofences removed successfully")
                onSuccess()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to remove all geofences", exception)
                onFailure(exception)
            }
    }
}
