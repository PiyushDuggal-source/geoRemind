package com.example.georemind.data

import kotlinx.coroutines.flow.Flow

/**
 * Repository for task data operations.
 * Acts as a single source of truth for task data, abstracting the data source.
 */
class TaskRepository(private val taskDao: TaskDao) {
    
    /**
     * Observable stream of all tasks.
     */
    val allTasks: Flow<List<TaskEntity>> = taskDao.getAllTasks()
    
    /**
     * Get all active tasks (for geofence registration).
     */
    suspend fun getActiveTasks(): List<TaskEntity> {
        return taskDao.getActiveTasks()
    }
    
    /**
     * Get a single task by ID.
     */
    suspend fun getTaskById(id: Long): TaskEntity? {
        return taskDao.getTaskById(id)
    }
    
    /**
     * Insert a new task and return its ID.
     */
    suspend fun insertTask(task: TaskEntity): Long {
        return taskDao.insertTask(task)
    }
    
    /**
     * Update an existing task.
     */
    suspend fun updateTask(task: TaskEntity) {
        taskDao.updateTask(task)
    }
    
    /**
     * Delete a task.
     */
    suspend fun deleteTask(task: TaskEntity) {
        taskDao.deleteTask(task)
    }
    
    /**
     * Delete a task by ID.
     */
    suspend fun deleteTaskById(id: Long) {
        taskDao.deleteTaskById(id)
    }
    
    /**
     * Toggle task active state.
     */
    suspend fun setTaskActive(id: Long, isActive: Boolean) {
        taskDao.setTaskActive(id, isActive)
    }
}
