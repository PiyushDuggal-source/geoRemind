package com.example.georemind.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.georemind.MainActivity
import com.example.georemind.R

/**
 * Helper class for creating and displaying notifications.
 * Handles notification channel creation and geofence alert notifications.
 */
class NotificationHelper(private val context: Context) {
    
    companion object {
        const val CHANNEL_ID = "georemind_alerts"
        const val CHANNEL_NAME = "Location Reminders"
        private const val NOTIFICATION_ID_BASE = 1000
    }
    
    init {
        createNotificationChannel()
    }
    
    /**
     * Create the notification channel for Android 8.0+.
     * This must be called before showing any notifications.
     */
    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                importance
            ).apply {
                description = context.getString(R.string.notification_channel_description)
                enableVibration(true)
                enableLights(true)
            }
            
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Show a notification for a triggered geofence.
     */
    fun showGeofenceNotification(
        taskId: Long,
        title: String,
        description: String,
        placeName: String? = null
    ) {
        // Intent to open the app when notification is tapped
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("task_id", taskId)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Build the notification content
        val contentText = if (placeName != null) {
            "You've arrived at $placeName"
        } else {
            description.ifEmpty { "You've arrived at the reminder location" }
        }
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()
        
        try {
            NotificationManagerCompat.from(context).notify(
                (NOTIFICATION_ID_BASE + taskId).toInt(),
                notification
            )
        } catch (e: SecurityException) {
            // Notification permission not granted
            e.printStackTrace()
        }
    }
}
