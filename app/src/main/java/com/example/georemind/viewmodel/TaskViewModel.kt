package com.example.georemind.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.georemind.data.AppDatabase
import com.example.georemind.data.TaskEntity
import com.example.georemind.data.TaskRepository
import com.example.georemind.geofence.GeofenceHelper
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** ViewModel for the main map screen. Manages task data, geofence operations, and UI state. */
class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TaskRepository
    private val geofenceHelper: GeofenceHelper
    private val userPreferences: com.example.georemind.data.UserPreferencesRepository

    // Cache the latest default radius
    private var cachedDefaultRadius = 200f

    init {
        val database = AppDatabase.getDatabase(application)
        repository = TaskRepository(database.taskDao())
        geofenceHelper = GeofenceHelper(application)
        userPreferences = com.example.georemind.data.UserPreferencesRepository(application)

        // Observe default radius
        viewModelScope.launch {
            userPreferences.defaultRadius.collect { radius ->
                cachedDefaultRadius = radius
                // If no location is currently selected (or we are in "idle" mode), update the
                // slider
                if (_selectedLocation.value == null) {
                    _selectedRadius.value = radius
                }
            }
        }
    }

    // Expose for Settings Screen
    val defaultRadius = userPreferences.defaultRadius

    /** Save default radius to preferences */
    fun saveDefaultRadius(radius: Float) {
        viewModelScope.launch { userPreferences.saveDefaultRadius(radius) }
    }

    // All tasks as a StateFlow for Compose
    val tasks: StateFlow<List<TaskEntity>> =
            repository.allTasks.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = emptyList()
            )

    // Currently selected location on the map
    private val _selectedLocation = MutableStateFlow<LatLng?>(null)
    val selectedLocation: StateFlow<LatLng?> = _selectedLocation.asStateFlow()

    // Selected radius for new task (in meters)
    private val _selectedRadius = MutableStateFlow(200f)
    val selectedRadius: StateFlow<Float> = _selectedRadius.asStateFlow()

    // Bottom sheet visibility state
    private val _showTaskSheet = MutableStateFlow(false)
    val showTaskSheet: StateFlow<Boolean> = _showTaskSheet.asStateFlow()

    // Task being edited (null for new task)
    private val _editingTask = MutableStateFlow<TaskEntity?>(null)
    val editingTask: StateFlow<TaskEntity?> = _editingTask.asStateFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Error message
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Success message
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    /** Update the selected location on the map. */
    fun selectLocation(latLng: LatLng) {
        _selectedLocation.value = latLng
    }

    /** Update the radius for a new task. */
    fun updateRadius(radius: Float) {
        _selectedRadius.value = radius.coerceIn(30f, 1000f)
    }

    /** Show the task creation bottom sheet. */
    fun showTaskCreationSheet() {
        _editingTask.value = null
        _showTaskSheet.value = true
    }

    /** Show the task edit bottom sheet. */
    fun showTaskEditSheet(task: TaskEntity) {
        _editingTask.value = task
        _selectedLocation.value = LatLng(task.latitude, task.longitude)
        _selectedRadius.value = task.radiusMeters
        _showTaskSheet.value = true
    }

    /** Hide the task bottom sheet. */
    fun hideTaskSheet() {
        _showTaskSheet.value = false
        _editingTask.value = null
    }

    /** Clear the selected location. */
    fun clearSelection() {
        _selectedLocation.value = null
        _selectedRadius.value = cachedDefaultRadius
    }

    /** Save a new task or update an existing one. */
    fun saveTask(title: String, description: String, placeName: String? = null) {
        val location = _selectedLocation.value ?: return

        viewModelScope.launch {
            _isLoading.value = true

            try {
                val existingTask = _editingTask.value

                val task =
                        TaskEntity(
                                id = existingTask?.id ?: 0,
                                title = title,
                                description = description,
                                latitude = location.latitude,
                                longitude = location.longitude,
                                radiusMeters = _selectedRadius.value,
                                isActive = true,
                                placeName = placeName,
                                createdAt = existingTask?.createdAt ?: System.currentTimeMillis()
                        )

                // Save to database
                val taskId = repository.insertTask(task)
                val savedTask = task.copy(id = if (existingTask != null) task.id else taskId)

                // Register geofence
                geofenceHelper.addGeofence(
                        task = savedTask,
                        onSuccess = { _successMessage.value = "Task saved!" },
                        onFailure = { exception ->
                            _errorMessage.value =
                                    "Task saved but geofence failed: ${exception.message}"
                        }
                )

                // Reset UI state
                hideTaskSheet()
                clearSelection()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to save task: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Delete a task and its geofence. */
    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            try {
                // Remove geofence first
                geofenceHelper.removeGeofence(
                        taskId = task.id,
                        onSuccess = {},
                        onFailure = { /* Ignore, task might not have been registered */}
                )

                // Delete from database
                repository.deleteTask(task)
                _successMessage.value = "Task deleted"
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete task: ${e.message}"
            }
        }
    }

    /** Toggle task active state. */
    fun toggleTaskActive(task: TaskEntity) {
        viewModelScope.launch {
            try {
                val newActiveState = !task.isActive
                repository.setTaskActive(task.id, newActiveState)

                if (newActiveState) {
                    // Re-register geofence
                    val updatedTask = task.copy(isActive = true)
                    geofenceHelper.addGeofence(task = updatedTask, onSuccess = {}, onFailure = {})
                } else {
                    // Remove geofence
                    geofenceHelper.removeGeofence(taskId = task.id, onSuccess = {}, onFailure = {})
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update task: ${e.message}"
            }
        }
    }

    /** Clear the error message. */
    fun clearError() {
        _errorMessage.value = null
    }

    /** Clear the success message. */
    fun clearSuccess() {
        _successMessage.value = null
    }

    /** Re-register all active geofences. Useful after permissions are granted. */
    fun reRegisterAllGeofences() {
        viewModelScope.launch {
            try {
                val activeTasks = repository.getActiveTasks()
                if (activeTasks.isNotEmpty()) {
                    geofenceHelper.addGeofences(tasks = activeTasks, onSuccess = {}, onFailure = {})
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to register geofences: ${e.message}"
            }
        }
    }
}
