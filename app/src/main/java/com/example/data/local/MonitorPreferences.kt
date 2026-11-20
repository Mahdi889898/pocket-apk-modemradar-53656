package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.monitorDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "modem_radar_prefs"
)

data class MonitorSettings(
    val targetUrl: String = MonitorPreferences.DEFAULT_TARGET_URL,
    val isMonitoringEnabled: Boolean = true,
    val intervalMinutes: Long = 15L,
    val foregroundAutoPollEnabled: Boolean = true,
    val lastCheckedAt: Long = 0L
)

class MonitorPreferences(private val context: Context) {

    companion object {
        const val DEFAULT_TARGET_URL =
            "https://esam.ir/search?kw=%D9%85%D9%88%D8%AF%D9%85&so=1"

        private val KEY_TARGET_URL = stringPreferencesKey("target_url")
        private val KEY_MONITORING_ENABLED = booleanPreferencesKey("monitoring_enabled")
        private val KEY_INTERVAL_MINUTES = longPreferencesKey("interval_minutes")
        private val KEY_FOREGROUND_AUTO_POLL = booleanPreferencesKey("foreground_auto_poll")
        private val KEY_LAST_CHECKED_AT = longPreferencesKey("last_checked_at")
    }

    val settingsFlow: Flow<MonitorSettings> = context.monitorDataStore.data.map { prefs ->
        MonitorSettings(
            targetUrl = prefs[KEY_TARGET_URL] ?: DEFAULT_TARGET_URL,
            isMonitoringEnabled = prefs[KEY_MONITORING_ENABLED] ?: true,
            intervalMinutes = (prefs[KEY_INTERVAL_MINUTES] ?: 15L).coerceAtLeast(15L),
            foregroundAutoPollEnabled = prefs[KEY_FOREGROUND_AUTO_POLL] ?: true,
            lastCheckedAt = prefs[KEY_LAST_CHECKED_AT] ?: 0L
        )
    }

    suspend fun getCurrentSettings(): MonitorSettings = settingsFlow.first()

    suspend fun setTargetUrl(url: String) {
        context.monitorDataStore.edit { it[KEY_TARGET_URL] = url.trim() }
    }

    suspend fun resetTargetUrl() {
        context.monitorDataStore.edit { it[KEY_TARGET_URL] = DEFAULT_TARGET_URL }
    }

    suspend fun setMonitoringEnabled(enabled: Boolean) {
        context.monitorDataStore.edit { it[KEY_MONITORING_ENABLED] = enabled }
    }

    suspend fun setIntervalMinutes(minutes: Long) {
        context.monitorDataStore.edit { it[KEY_INTERVAL_MINUTES] = minutes.coerceAtLeast(15L) }
    }

    suspend fun setForegroundAutoPollEnabled(enabled: Boolean) {
        context.monitorDataStore.edit { it[KEY_FOREGROUND_AUTO_POLL] = enabled }
    }

    suspend fun setLastCheckedAt(timestamp: Long) {
        context.monitorDataStore.edit { it[KEY_LAST_CHECKED_AT] = timestamp }
    }
}
