package com.example.georemind.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for task operations.
 * Provides methods to query, insert, update, and delete tasks from the database.
 */
@Dao
interface TaskDao {
    
    /**
     * Get all tasks ordered by creation date (newest first).
     * Returns a Flow for reactive updates.
     */
    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>
    
    /**
     * Get all currently active tasks (for geofence registration).
     */
    @Query("SELECT * FROM tasks WHERE isActive = 1")
    suspend fun getActiveTasks(): List<TaskEntity>
    
    /**
     * Get a single task by its ID.
     */
    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): TaskEntity?
    
    /**
     * Insert a new task. Returns the auto-generated ID.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long
    
    /**
     * Update an existing task.
     */
    @Update
    suspend fun updateTask(task: TaskEntity)
    
    /**
     * Delete a task.
     */
    @Delete
    suspend fun deleteTask(task: TaskEntity)
    
    /**
     * Delete a task by ID.
     */
    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)
    
    /**
     * Toggle the active state of a task.
     */
    @Query("UPDATE tasks SET isActive = :isActive WHERE id = :id")
    suspend fun setTaskActive(id: Long, isActive: Boolean)
}
