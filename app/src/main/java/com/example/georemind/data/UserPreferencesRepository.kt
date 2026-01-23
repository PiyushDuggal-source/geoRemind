package com.example.georemind.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class UserPreferencesRepository(private val context: Context) {

    companion object {
        val DEFAULT_RADIUS_KEY = floatPreferencesKey("default_radius")
        const val DEFAULT_RADIUS_VALUE = 200f
    }

    val defaultRadius: Flow<Float> =
            context.dataStore.data.map { preferences ->
                preferences[DEFAULT_RADIUS_KEY] ?: DEFAULT_RADIUS_VALUE
            }

    suspend fun saveDefaultRadius(radius: Float) {
        context.dataStore.edit { preferences -> preferences[DEFAULT_RADIUS_KEY] = radius }
    }
}
