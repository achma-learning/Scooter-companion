package com.example.kymcoagility50.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsStore by preferencesDataStore(name = "app_settings")

enum class ThemeMode { LIGHT, DARK, SYSTEM }

data class AppSettings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val defaultZoom: Float = 1.0f,
    val keepScreenOn: Boolean = false,
    val reminders: Boolean = true
)

/** Settings are always local-only, in both modes — never synced. */
class SettingsRepository(private val context: Context) {

    private val THEME_KEY = stringPreferencesKey("theme")
    private val ZOOM_KEY = floatPreferencesKey("default_zoom")
    private val KEEP_SCREEN_ON_KEY = booleanPreferencesKey("keep_screen_on")
    private val REMINDERS_KEY = booleanPreferencesKey("reminders")

    val settings: Flow<AppSettings> = context.settingsStore.data.map { prefs ->
        AppSettings(
            theme = prefs[THEME_KEY]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
            defaultZoom = prefs[ZOOM_KEY] ?: 1.0f,
            keepScreenOn = prefs[KEEP_SCREEN_ON_KEY] ?: false,
            reminders = prefs[REMINDERS_KEY] ?: true
        )
    }

    suspend fun setTheme(mode: ThemeMode) = context.settingsStore.edit { it[THEME_KEY] = mode.name }
    suspend fun setDefaultZoom(zoom: Float) = context.settingsStore.edit { it[ZOOM_KEY] = zoom }
    suspend fun setKeepScreenOn(enabled: Boolean) = context.settingsStore.edit { it[KEEP_SCREEN_ON_KEY] = enabled }
    suspend fun setReminders(enabled: Boolean) = context.settingsStore.edit { it[REMINDERS_KEY] = enabled }
}
