package com.example.parallax

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object PreferenceManager {
    private val L1_URI_KEY = stringPreferencesKey("layer1_uri_key")
    private val L2_URI_KEY = stringPreferencesKey("layer2_uri_key")
    private val L3_URI_KEY = stringPreferencesKey("layer3_uri_key")
    private val PAGES = intPreferencesKey("pages_key")
    private val L1_VELOCITY_KEY = doublePreferencesKey("layer1_velocity_key")
    private val L2_VELOCITY_KEY = doublePreferencesKey("layer3_velocity_key")
    private val L3_VELOCITY_KEY = doublePreferencesKey("layer3_velocity_key")

    // URI =================================================
    suspend fun saveL1(context: Context, value: String) {
        context.dataStore.edit { preferences ->
            preferences[L1_URI_KEY] = value
        }
    }
    suspend fun saveL2(context: Context, value: String) {
        context.dataStore.edit { preferences ->
            preferences[L2_URI_KEY] = value
        }
    }
    suspend fun saveL3(context: Context, value: String) {
        context.dataStore.edit { preferences ->
            preferences[L3_URI_KEY] = value
        }
    }

    fun getL1(context: Context): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[L1_URI_KEY].toString()
        }
    }
    fun getL2(context: Context): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[L2_URI_KEY].toString()
        }
    }
    fun getL3(context: Context): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[L3_URI_KEY].toString()
        }
    }

    // PAGES =================================================
    suspend fun savePages(context: Context, value: Int) {
        context.dataStore.edit { preferences ->
            preferences[PAGES] = value
        }
    }
    fun getPages(context: Context): Flow<Int> {
        return context.dataStore.data.map { preferences ->
            preferences[PAGES]?.toInt() ?: 1
        }
    }
    
    // VELOCITY =================================================
    suspend fun saveL1Velocity(context: Context, value: Double) {
        context.dataStore.edit { preferences ->
            preferences[L1_VELOCITY_KEY] = value
        }
    }
    suspend fun saveL2Velocity(context: Context, value: Double) {
        context.dataStore.edit { preferences ->
            preferences[L2_VELOCITY_KEY] = value
        }
    }
    suspend fun saveL3Velocity(context: Context, value: Double) {
        context.dataStore.edit { preferences ->
            preferences[L3_VELOCITY_KEY] = value
        }
    }
    fun getL1Velocity(context: Context): Flow<Double> {
        return context.dataStore.data.map { preferences ->
            preferences[L1_VELOCITY_KEY]?.toDouble() ?: 0.0
        }
    }
    fun getL2Velocity(context: Context): Flow<Double> {
        return context.dataStore.data.map { preferences ->
            preferences[L2_VELOCITY_KEY]?.toDouble() ?: 0.0
        }
    }
    fun getL3Velocity(context: Context): Flow<Double> {
        return context.dataStore.data.map { preferences ->
            preferences[L3_VELOCITY_KEY]?.toDouble() ?: 0.0
        }
    }



}