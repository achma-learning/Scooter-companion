package io.github.achmalearning.scootercompanion.privacy

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val Context.privacyStore by preferencesDataStore(name = "privacy_prefs")

/**
 * The app has no account and no cloud: everything stays on this device. The only network
 * calls are public downloads the owner starts with a tap (see [NetworkGuard.userRequested]).
 * What remains here is the first-launch flag, read synchronously by the onboarding gate.
 */
object PrivacyManager {

    private val ONBOARDED_KEY = booleanPreferencesKey("onboarded")

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _onboarded = MutableStateFlow(false)
    val onboarded: StateFlow<Boolean> = _onboarded

    private var appContext: Context? = null
    @Volatile private var initialized = false

    /** Call once from Application.onCreate() before any UI reads [onboarded]. */
    fun initialize(context: Context) {
        if (initialized) return
        initialized = true
        appContext = context.applicationContext
        scope.launch {
            _onboarded.value = appContext!!.privacyStore.data.first()[ONBOARDED_KEY] ?: false
        }
    }

    suspend fun completeOnboarding() {
        _onboarded.value = true
        appContext?.privacyStore?.edit { it[ONBOARDED_KEY] = true }
    }
}
