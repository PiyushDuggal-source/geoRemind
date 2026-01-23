package com.example.georemind.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.georemind.data.AppDatabase
import com.example.georemind.notification.NotificationHelper
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver for geofence transition events.
 * This receiver is triggered by the system when the user enters a geofence,
 * even when the app is killed or backgrounded.
 */
class GeofenceBroadcastReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "GeofenceReceiver"
    }
    
    // Coroutine scope for database operations
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        Log.d(TAG, "Geofence event received")
        
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        
        if (geofencingEvent == null) {
            Log.e(TAG, "GeofencingEvent is null")
            pendingResult.finish()
            return
        }
        
        if (geofencingEvent.hasError()) {
            val errorMessage = getErrorMessage(geofencingEvent.errorCode)
            Log.e(TAG, "Geofencing error: $errorMessage")
            pendingResult.finish()
            return
        }
        
        // Get the transition type
        val transitionType = geofencingEvent.geofenceTransition
        
        // We only care about ENTER transitions
        if (transitionType == Geofence.GEOFENCE_TRANSITION_ENTER) {
            val triggeringGeofences = geofencingEvent.triggeringGeofences
            
            if (triggeringGeofences.isNullOrEmpty()) {
                Log.w(TAG, "No triggering geofences found")
                pendingResult.finish()
                return
            }
            
            Log.d(TAG, "User entered ${triggeringGeofences.size} geofence(s)")
            
            scope.launch {
                try {
                    // Process each triggered geofence
                    triggeringGeofences.forEach { geofence ->
                        val taskId = geofence.requestId.toLongOrNull()
                        if (taskId != null) {
                            val database = AppDatabase.getDatabase(context)
                            val task = database.taskDao().getTaskById(taskId)
                            
                            if (task != null) {
                                val notificationHelper = NotificationHelper(context)
                                notificationHelper.showGeofenceNotification(
                                    taskId = task.id,
                                    title = task.title,
                                    description = task.description,
                                    placeName = task.placeName
                                )
                                Log.d(TAG, "Notification shown for task: ${task.title}")
                            } else {
                                Log.w(TAG, "Task not found in database for ID: $taskId")
                            }
                        } else {
                            Log.e(TAG, "Invalid geofence request ID: ${geofence.requestId}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing geofence event", e)
                } finally {
                    pendingResult.finish()
                }
            }
        } else {
            Log.d(TAG, "Ignoring transition type: $transitionType")
            pendingResult.finish()
        }
    }
    
    /**
     * Get a human-readable error message from geofence error code.
     */
    private fun getErrorMessage(errorCode: Int): String {
        return when (errorCode) {
            GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE -> 
                "Geofence service not available"
            GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES -> 
                "Too many geofences registered"
            GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS -> 
                "Too many pending intents"
            else -> "Unknown geofence error: $errorCode"
        }
    }
}
