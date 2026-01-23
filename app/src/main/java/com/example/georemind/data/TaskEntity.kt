package com.example.georemind.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a location-based reminder task.
 * Each task is associated with a geographic location and radius
 * that triggers a notification when the user enters the area.
 */
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /** Title of the reminder task */
    val title: String,
    
    /** Optional description with more details */
    val description: String = "",
    
    /** Latitude of the geofence center */
    val latitude: Double,
    
    /** Longitude of the geofence center */
    val longitude: Double,
    
    /** Radius in meters for the geofence trigger area */
    val radiusMeters: Float,
    
    /** Whether this geofence is currently active and registered */
    val isActive: Boolean = true,
    
    /** Timestamp when the task was created */
    val createdAt: Long = System.currentTimeMillis(),
    
    /** Optional address/place name for display purposes */
    val placeName: String? = null
)
