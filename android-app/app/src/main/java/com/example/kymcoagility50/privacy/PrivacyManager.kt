package com.example.kymcoagility50.privacy

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.privacyStore by preferencesDataStore(name = "privacy_prefs")

/**
 * Single source of truth for which of the two modes the app is in. Every network-capable
 * code path (auth, Firestore sync, the WorkManager sync worker) MUST consult
 * [NetworkGuard.isNetworkAllowed] — which itself reads [isPrivate] — before doing anything,
 * rather than checking mode ad hoc.
 *
 * [isPrivate] is a plain synchronous read of the last-known value (safe to call from a hot
 * path with no coroutine hop); it is kept in sync with the persisted DataStore value via
 * [initialize], which the app calls once at startup, and via [setMode].
 */
object PrivacyManager {

    private val MODE_KEY = stringPreferencesKey("app_mode")
    private val ONBOARDED_KEY = booleanPreferencesKey("onboarded")

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _mode = MutableStateFlow(AppMode.PRIVATE)
    val mode: StateFlow<AppMode> = _mode

    private val _onboarded = MutableStateFlow(false)
    val onboarded: StateFlow<Boolean> = _onboarded

    private var appContext: Context? = null
    @Volatile private var initialized = false

    /** Call once from Application.onCreate() / MainActivity before any UI reads [mode]. */
    fun initialize(context: Context) {
        if (initialized) return
        initialized = true
        appContext = context.applicationContext
        scope.launch {
            val prefs = appContext!!.privacyStore.data.first()
            _mode.value = prefs[MODE_KEY]?.let { runCatching { AppMode.valueOf(it) }.getOrNull() }
                ?: AppMode.PRIVATE
            _onboarded.value = prefs[ONBOARDED_KEY] ?: false
        }
    }

    /** Synchronous, hot-path-safe check. Defaults to PRIVATE (fail closed) until initialized. */
    fun isPrivate(): Boolean = _mode.value == AppMode.PRIVATE

    suspend fun setMode(newMode: AppMode) {
        _mode.value = newMode
        appContext?.privacyStore?.edit { it[MODE_KEY] = newMode.name }
    }

    suspend fun completeOnboarding(chosenMode: AppMode) {
        _onboarded.value = true
        appContext?.privacyStore?.edit { it[ONBOARDED_KEY] = true }
        setMode(chosenMode)
    }

    /** Live count of network attempts made while Private Mode was ON — must always read 0. */
    val networkAttemptsWhilePrivate = com.example.kymcoagility50.privacy.NetworkGuard.attemptsWhilePrivate
}
