package com.example.georemind

import android.app.Application
import com.example.georemind.notification.NotificationHelper

/**
 * Application class for GeoRemind.
 * Initializes app-wide components like the notification channel.
 */
class GeoRemindApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Create notification channel on app start
        NotificationHelper(this).createNotificationChannel()
    }
}
