package com.example.georemind.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.georemind.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver that handles device boot completion.
 * Re-registers all active geofences after the device reboots,
 * since geofences are lost when the device restarts.
 */
class BootReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "BootReceiver"
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Device boot completed - re-registering geofences")
            reRegisterGeofences(context)
        }
    }
    
    /**
     * Re-register all active geofences after device boot.
     */
    private fun reRegisterGeofences(context: Context) {
        scope.launch {
            try {
                val database = AppDatabase.getDatabase(context)
                val activeTasks = database.taskDao().getActiveTasks()
                
                if (activeTasks.isEmpty()) {
                    Log.d(TAG, "No active tasks to re-register")
                    return@launch
                }
                
                Log.d(TAG, "Re-registering ${activeTasks.size} geofences")
                
                val geofenceHelper = GeofenceHelper(context)
                geofenceHelper.addGeofences(
                    tasks = activeTasks,
                    onSuccess = {
                        Log.d(TAG, "Successfully re-registered all geofences")
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Failed to re-register geofences", exception)
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error re-registering geofences after boot", e)
            }
        }
    }
}
