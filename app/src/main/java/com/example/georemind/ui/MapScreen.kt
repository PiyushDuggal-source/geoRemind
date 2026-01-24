package com.example.georemind.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.material3.Divider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.georemind.R
import com.example.georemind.ui.theme.GeofenceActiveFill
import com.example.georemind.ui.theme.GeofenceActiveStroke
import com.example.georemind.ui.theme.GeofenceFill
import com.example.georemind.ui.theme.GeofenceStroke
import com.example.georemind.viewmodel.TaskViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/** Main map screen with Google Maps, task markers, and geofence circles. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
        viewModel: TaskViewModel = viewModel(),
        onNavigateToSettings: () -> Unit,
        onNavigateToAbout: () -> Unit
) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }

        // Collect state from ViewModel
        val tasks by viewModel.tasks.collectAsState()
        val selectedLocation by viewModel.selectedLocation.collectAsState()
        val selectedRadius by viewModel.selectedRadius.collectAsState()
        val showTaskSheet by viewModel.showTaskSheet.collectAsState()
        val editingTask by viewModel.editingTask.collectAsState()
        val isLoading by viewModel.isLoading.collectAsState()
        val errorMessage by viewModel.errorMessage.collectAsState()
        val successMessage by viewModel.successMessage.collectAsState()

        // Location permission for "My Location" button
        val locationPermissionState =
                rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

        // Task list drawer state
        val drawerState = rememberDrawerState(DrawerValue.Closed)

        // Camera position state - default to a central location
        val defaultLocation = LatLng(37.7749, -122.4194) // San Francisco
        val cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(defaultLocation, 14f)
        }

        // State for location services dialog
        var showLocationServicesDialog by remember { mutableStateOf(false) }

        // Helper function to check if location services are enabled
        fun isLocationEnabled(): Boolean {
                val locationManager =
                        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                        locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }

        ModalNavigationDrawer(
                drawerState = drawerState,
                gesturesEnabled = drawerState.isOpen,
                drawerContent = {
                        ModalDrawerSheet(
                                drawerContainerColor = MaterialTheme.colorScheme.surface,
                        ) {
                                Spacer(Modifier.height(12.dp))
                                NavigationDrawerItem(
                                        label = { Text("Map") },
                                        selected = true,
                                        onClick = { scope.launch { drawerState.close() } },
                                        modifier =
                                                Modifier.padding(
                                                        NavigationDrawerItemDefaults.ItemPadding
                                                )
                                )
                                NavigationDrawerItem(
                                        label = { Text("Settings") },
                                        selected = false,
                                        onClick = {
                                                scope.launch { drawerState.close() }
                                                onNavigateToSettings()
                                        },
                                        modifier =
                                                Modifier.padding(
                                                        NavigationDrawerItemDefaults.ItemPadding
                                                )
                                )
                                NavigationDrawerItem(
                                        label = { Text("About") },
                                        selected = false,
                                        onClick = {
                                                scope.launch { drawerState.close() }
                                                onNavigateToAbout()
                                        },
                                        modifier =
                                                Modifier.padding(
                                                        NavigationDrawerItemDefaults.ItemPadding
                                                )
                                )

                                Divider(modifier = Modifier.padding(vertical = 8.dp))

                                Text(
                                        text = stringResource(R.string.my_tasks),
                                        style = MaterialTheme.typography.headlineSmall,
                                        modifier = Modifier.padding(16.dp)
                                )

                                TaskList(
                                        tasks = tasks,
                                        onTaskClick = { task ->
                                                scope.launch {
                                                        drawerState.close()
                                                        // Move camera to task location
                                                        val taskLatLng =
                                                                LatLng(
                                                                        task.latitude,
                                                                        task.longitude
                                                                )
                                                        cameraPositionState.animate(
                                                                CameraUpdateFactory.newLatLngZoom(
                                                                        taskLatLng,
                                                                        18f
                                                                )
                                                        )
                                                        // Open edit sheet
                                                        viewModel.showTaskEditSheet(task)
                                                }
                                        },
                                        onDeleteTask = { task -> viewModel.deleteTask(task) },
                                        onToggleActive = { task ->
                                                viewModel.toggleTaskActive(task)
                                        }
                                )
                        }
                }
        ) {

                // Animation for FAB position
                val fabPadding by
                        androidx.compose.animation.core.animateDpAsState(
                                targetValue = if (selectedLocation != null) 160.dp else 0.dp,
                                label = "fabPadding"
                        )

                // Move camera to user's current location on first load
                LaunchedEffect(locationPermissionState.status.isGranted) {
                        if (locationPermissionState.status.isGranted) {
                                try {
                                        val fusedLocationClient =
                                                LocationServices.getFusedLocationProviderClient(
                                                        context
                                                )
                                        @SuppressLint("MissingPermission")
                                        val location = fusedLocationClient.lastLocation.await()
                                        location?.let {
                                                val userLatLng = LatLng(it.latitude, it.longitude)
                                                cameraPositionState.animate(
                                                        CameraUpdateFactory.newLatLngZoom(
                                                                userLatLng,
                                                                15f
                                                        )
                                                )
                                        }
                                } catch (e: Exception) {
                                        // Ignore - will use default location
                                }
                        }
                }

                // Show error messages
                LaunchedEffect(errorMessage) {
                        errorMessage?.let {
                                snackbarHostState.showSnackbar(it)
                                viewModel.clearError()
                        }
                }

                // Show success messages
                LaunchedEffect(successMessage) {
                        successMessage?.let {
                                snackbarHostState.showSnackbar(it)
                                viewModel.clearSuccess()
                        }
                }

                // Re-register geofences after permissions are granted
                LaunchedEffect(locationPermissionState.status.isGranted) {
                        if (locationPermissionState.status.isGranted) {
                                viewModel.reRegisterAllGeofences()
                        }
                }

                Scaffold(
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        floatingActionButton = {
                                Column(
                                        horizontalAlignment = Alignment.End,
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.padding(bottom = fabPadding)
                                ) {

                                        // My Location FAB
                                        SmallFloatingActionButton(
                                                onClick = {
                                                        if (locationPermissionState.status.isGranted
                                                        ) {
                                                                // Check if location services are
                                                                // enabled
                                                                if (!isLocationEnabled()) {
                                                                        showLocationServicesDialog =
                                                                                true
                                                                        return@SmallFloatingActionButton
                                                                }
                                                                scope.launch {
                                                                        try {
                                                                                val fusedLocationClient =
                                                                                        LocationServices
                                                                                                .getFusedLocationProviderClient(
                                                                                                        context
                                                                                                )
                                                                                @SuppressLint(
                                                                                        "MissingPermission"
                                                                                )
                                                                                val location =
                                                                                        fusedLocationClient
                                                                                                .lastLocation
                                                                                                .await()
                                                                                if (location != null
                                                                                ) {
                                                                                        val userLatLng =
                                                                                                LatLng(
                                                                                                        location.latitude,
                                                                                                        location.longitude
                                                                                                )
                                                                                        cameraPositionState
                                                                                                .animate(
                                                                                                        CameraUpdateFactory
                                                                                                                .newLatLngZoom(
                                                                                                                        userLatLng,
                                                                                                                        18f
                                                                                                                )
                                                                                                )
                                                                                } else {
                                                                                        // Location
                                                                                        // is null,
                                                                                        // might
                                                                                        // need to
                                                                                        // enable
                                                                                        // GPS
                                                                                        showLocationServicesDialog =
                                                                                                true
                                                                                }
                                                                        } catch (e: Exception) {
                                                                                snackbarHostState
                                                                                        .showSnackbar(
                                                                                                "Could not get location"
                                                                                        )
                                                                        }
                                                                }
                                                        } else {
                                                                locationPermissionState
                                                                        .launchPermissionRequest()
                                                        }
                                                },
                                                containerColor =
                                                        MaterialTheme.colorScheme.secondaryContainer
                                        ) {
                                                Icon(
                                                        imageVector = Icons.Default.MyLocation,
                                                        contentDescription = "My Location"
                                                )
                                        }

                                        // Add Task FAB (only enabled when location is selected)
                                        FloatingActionButton(
                                                onClick = {
                                                        if (selectedLocation != null) {
                                                                viewModel.showTaskCreationSheet()
                                                        }
                                                },
                                                containerColor =
                                                        if (selectedLocation != null) {
                                                                MaterialTheme.colorScheme
                                                                        .primaryContainer
                                                        } else {
                                                                MaterialTheme.colorScheme
                                                                        .surfaceVariant
                                                        }
                                        ) {
                                                Icon(
                                                        imageVector = Icons.Default.Add,
                                                        contentDescription =
                                                                stringResource(R.string.add_task)
                                                )
                                        }
                                }
                        },
                        topBar = {
                                TopAppBar(
                                        title = { Text("GeoRemind") },
                                        navigationIcon = {
                                                IconButton(
                                                        onClick = {
                                                                scope.launch { drawerState.open() }
                                                        }
                                                ) {
                                                        Icon(
                                                                imageVector =
                                                                        Icons.AutoMirrored.Filled
                                                                                .List,
                                                                contentDescription =
                                                                        stringResource(
                                                                                R.string.my_tasks
                                                                        )
                                                        )
                                                }
                                        },
                                        colors =
                                                TopAppBarDefaults.topAppBarColors(
                                                        containerColor =
                                                                MaterialTheme.colorScheme.surface
                                                                        .copy(alpha = 0.9f)
                                                )
                                )
                        }
                ) { padding ->
                        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                                // Google Map
                                GoogleMap(
                                        modifier = Modifier.fillMaxSize(),
                                        cameraPositionState = cameraPositionState,
                                        properties =
                                                MapProperties(
                                                        isMyLocationEnabled =
                                                                locationPermissionState
                                                                        .status
                                                                        .isGranted
                                                ),
                                        uiSettings =
                                                MapUiSettings(
                                                        myLocationButtonEnabled =
                                                                false, // We have our own FAB
                                                        zoomControlsEnabled = false
                                                ),
                                        onMapClick = { latLng -> viewModel.selectLocation(latLng) }
                                ) {
                                        // Selected location marker and circle (for new task)
                                        selectedLocation?.let { location ->
                                                Marker(
                                                        state = MarkerState(position = location),
                                                        title = "New Task Location",
                                                        snippet =
                                                                "Radius: ${selectedRadius.toInt()}m"
                                                )

                                                Circle(
                                                        center = location,
                                                        radius = selectedRadius.toDouble(),
                                                        fillColor = GeofenceFill,
                                                        strokeColor = GeofenceStroke,
                                                        strokeWidth = 3f
                                                )
                                        }

                                        // Existing task markers and circles
                                        tasks.forEach { task ->
                                                val taskLocation =
                                                        LatLng(task.latitude, task.longitude)

                                                Marker(
                                                        state =
                                                                MarkerState(
                                                                        position = taskLocation
                                                                ),
                                                        title = task.title,
                                                        snippet =
                                                                task.description.ifEmpty {
                                                                        "${task.radiusMeters.toInt()}m radius"
                                                                },
                                                        alpha = if (task.isActive) 1f else 0.5f,
                                                        onClick = {
                                                                viewModel.showTaskEditSheet(task)
                                                                true
                                                        }
                                                )

                                                Circle(
                                                        center = taskLocation,
                                                        radius = task.radiusMeters.toDouble(),
                                                        fillColor =
                                                                if (task.isActive)
                                                                        GeofenceActiveFill
                                                                else
                                                                        GeofenceFill.copy(
                                                                                alpha = 0.2f
                                                                        ),
                                                        strokeColor =
                                                                if (task.isActive)
                                                                        GeofenceActiveStroke
                                                                else
                                                                        GeofenceStroke.copy(
                                                                                alpha = 0.5f
                                                                        ),
                                                        strokeWidth = 2f
                                                )
                                        }
                                }

                                // Radius Slider Sheet (visible when location is selected)
                                selectedLocation?.let {
                                        Surface(
                                                modifier =
                                                        Modifier.align(Alignment.BottomCenter)
                                                                .fillMaxWidth(),
                                                color = MaterialTheme.colorScheme.surface,
                                                tonalElevation = 8.dp,
                                                shadowElevation = 8.dp,
                                                shape =
                                                        MaterialTheme.shapes.extraLarge.copy(
                                                                bottomStart =
                                                                        androidx.compose.foundation
                                                                                .shape.CornerSize(
                                                                                0.dp
                                                                        ),
                                                                bottomEnd =
                                                                        androidx.compose.foundation
                                                                                .shape.CornerSize(
                                                                                0.dp
                                                                        )
                                                        )
                                        ) {
                                                Column(
                                                        modifier =
                                                                Modifier.padding(16.dp)
                                                                        .padding(bottom = 16.dp)
                                                ) {
                                                        Text(
                                                                text = "Adjust Radius",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .titleMedium,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .primary,
                                                                modifier =
                                                                        Modifier.padding(
                                                                                bottom = 12.dp
                                                                        )
                                                        )

                                                        Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                verticalAlignment =
                                                                        Alignment.CenterVertically,
                                                                horizontalArrangement =
                                                                        Arrangement.spacedBy(12.dp)
                                                        ) {
                                                                // Slider for quick adjustments (30m
                                                                // - 200m)
                                                                Column(
                                                                        modifier =
                                                                                Modifier.weight(1f)
                                                                ) {
                                                                        Slider(
                                                                                value =
                                                                                        selectedRadius
                                                                                                .coerceIn(
                                                                                                        30f,
                                                                                                        200f
                                                                                                ),
                                                                                onValueChange = {
                                                                                        viewModel
                                                                                                .updateRadius(
                                                                                                        it
                                                                                                )
                                                                                },
                                                                                valueRange =
                                                                                        30f..200f,
                                                                                modifier =
                                                                                        Modifier.fillMaxWidth()
                                                                        )
                                                                        Row(
                                                                                modifier =
                                                                                        Modifier.fillMaxWidth(),
                                                                                horizontalArrangement =
                                                                                        Arrangement
                                                                                                .SpaceBetween
                                                                        ) {
                                                                                Text(
                                                                                        "30m",
                                                                                        style =
                                                                                                MaterialTheme
                                                                                                        .typography
                                                                                                        .bodySmall
                                                                                )
                                                                                Text(
                                                                                        "200m",
                                                                                        style =
                                                                                                MaterialTheme
                                                                                                        .typography
                                                                                                        .bodySmall
                                                                                )
                                                                        }
                                                                }

                                                                // Custom Input Field (up to 1km)
                                                                OutlinedTextField(
                                                                        value =
                                                                                selectedRadius
                                                                                        .toInt()
                                                                                        .toString(),
                                                                        onValueChange = { str ->
                                                                                str
                                                                                        .filter {
                                                                                                it.isDigit()
                                                                                        }
                                                                                        .toFloatOrNull()
                                                                                        ?.let { num
                                                                                                ->
                                                                                                viewModel
                                                                                                        .updateRadius(
                                                                                                                num
                                                                                                        )
                                                                                        }
                                                                        },
                                                                        label = { Text("Meters") },
                                                                        singleLine = true,
                                                                        keyboardOptions =
                                                                                KeyboardOptions(
                                                                                        keyboardType =
                                                                                                androidx.compose
                                                                                                        .ui
                                                                                                        .text
                                                                                                        .input
                                                                                                        .KeyboardType
                                                                                                        .Number
                                                                                ),
                                                                        modifier =
                                                                                Modifier.width(
                                                                                        100.dp
                                                                                )
                                                                )
                                                        }
                                                }
                                        }
                                }

                                // Hint text when no location selected
                                if (selectedLocation == null && tasks.isEmpty()) {
                                        Card(
                                                modifier =
                                                        Modifier.align(Alignment.BottomCenter)
                                                                .padding(16.dp)
                                                                .padding(bottom = 80.dp),
                                                colors =
                                                        CardDefaults.cardColors(
                                                                containerColor =
                                                                        MaterialTheme.colorScheme
                                                                                .surfaceVariant
                                                        )
                                        ) {
                                                Text(
                                                        text =
                                                                "Tap on the map to select a location",
                                                        modifier = Modifier.padding(16.dp),
                                                        style = MaterialTheme.typography.bodyMedium
                                                )
                                        }
                                }

                                // Task creation bottom sheet
                                if (showTaskSheet) {
                                        ModalBottomSheet(
                                                onDismissRequest = { viewModel.hideTaskSheet() },
                                                sheetState =
                                                        rememberModalBottomSheetState(
                                                                skipPartiallyExpanded = true
                                                        )
                                        ) {
                                                TaskCreationSheet(
                                                        editingTask = editingTask,
                                                        currentRadius = selectedRadius,
                                                        isLoading = isLoading,
                                                        onRadiusChange = {
                                                                viewModel.updateRadius(it)
                                                        },
                                                        onSave = { title, description ->
                                                                viewModel.saveTask(
                                                                        title,
                                                                        description
                                                                )
                                                        },
                                                        onDismiss = { viewModel.hideTaskSheet() }
                                                )
                                        }
                                }
                        }
                }
        }

        // Location Services Dialog
        if (showLocationServicesDialog) {
                AlertDialog(
                        onDismissRequest = { showLocationServicesDialog = false },
                        title = { Text("Location Services Disabled") },
                        text = {
                                Text(
                                        "Location services are turned off. Please enable GPS or network location to use this feature."
                                )
                        },
                        confirmButton = {
                                TextButton(
                                        onClick = {
                                                showLocationServicesDialog = false
                                                context.startActivity(
                                                        Intent(
                                                                Settings.ACTION_LOCATION_SOURCE_SETTINGS
                                                        )
                                                )
                                        }
                                ) { Text("Open Settings") }
                        },
                        dismissButton = {
                                TextButton(onClick = { showLocationServicesDialog = false }) {
                                        Text("Cancel")
                                }
                        }
                )
        }
}
