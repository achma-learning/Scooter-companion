package com.example.kymcoagility50.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kymcoagility50.AppContainer
import com.example.kymcoagility50.auth.AuthManager
import com.example.kymcoagility50.data.model.PdfSource
import com.example.kymcoagility50.data.repository.AppSettings
import com.example.kymcoagility50.privacy.AppMode
import com.example.kymcoagility50.privacy.PrivacyManager
import com.example.kymcoagility50.work.ReminderWorker
import com.example.kymcoagility50.work.SyncScheduler
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val mode: AppMode = AppMode.PRIVATE,
    val settings: AppSettings = AppSettings(),
    val user: FirebaseUser? = null
)

class SettingsViewModel(
    private val context: Context,
    private val container: AppContainer
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        PrivacyManager.mode,
        container.settingsRepository.settings
    ) { mode, settings -> SettingsUiState(mode, settings, AuthManager.currentUser(context)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    /** Switching to Private Mode immediately signs out and cancels any pending sync. */
    fun switchToPrivateMode() {
        viewModelScope.launch {
            AuthManager.signOut(context)
            SyncScheduler.cancelAll(context)
            PrivacyManager.setMode(AppMode.PRIVATE)
        }
    }

    fun switchToSyncMode() {
        viewModelScope.launch {
            PrivacyManager.setMode(AppMode.SYNC)
            AuthManager.ensureInitialized(context)
        }
    }

    fun onSignedIn() {
        // Refresh state after a successful Google Sign-In result is handled by the caller;
        // schedule an initial pull/push of the maintenance log.
        SyncScheduler.scheduleSyncNow(context)
        SyncScheduler.schedulePeriodicSync(context)
    }

    fun signOut() {
        AuthManager.signOut(context)
    }

    fun setTheme(mode: com.example.kymcoagility50.data.repository.ThemeMode) {
        viewModelScope.launch { container.settingsRepository.setTheme(mode) }
    }

    fun setDefaultZoom(zoom: Float) {
        viewModelScope.launch { container.settingsRepository.setDefaultZoom(zoom) }
    }

    fun setKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch { container.settingsRepository.setKeepScreenOn(enabled) }
    }

    fun clearBookmarks() {
        viewModelScope.launch { container.libraryRepository.clearBookmarks() }
    }

    fun clearRecent() {
        viewModelScope.launch { container.libraryRepository.clearRecent() }
    }

    fun clearOcrIndexAndReseed() {
        viewModelScope.launch {
            container.ocrRepository.clear(PdfSource.MANUAL)
            container.ocrRepository.clear(PdfSource.PASSPORT)
            container.ocrRepository.ensureSeeded(PdfSource.MANUAL)
            container.ocrRepository.ensureSeeded(PdfSource.PASSPORT)
        }
    }

    private val _message = MutableStateFlow<String?>(null)
    /** One-shot result of export/import, shown in a snackbar-style line. */
    val message = _message.asStateFlow()
    fun consumeMessage() { _message.value = null }

    fun setReminders(enabled: Boolean) {
        viewModelScope.launch {
            container.settingsRepository.setReminders(enabled)
            if (enabled) ReminderWorker.schedule(context) else ReminderWorker.cancel(context)
        }
    }

    fun exportTo(uri: Uri) {
        viewModelScope.launch {
            _message.value = runCatching {
                val json = container.garageRepository.exportJson()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(json.toByteArray()) }
                }
                "Backup saved — import it on the website to continue there."
            }.getOrElse { "Export failed: ${it.message}" }
        }
    }

    fun importFrom(uri: Uri) {
        viewModelScope.launch {
            _message.value = runCatching {
                val text = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() }
                }
                container.garageRepository.importJson(text, container.maintenanceRepository).also {
                    SyncScheduler.scheduleSyncNow(context)
                }
            }.getOrElse { "Import failed: ${it.message}" }
        }
    }

    fun wipeGarage() {
        viewModelScope.launch {
            container.garageRepository.wipeGarage()
            container.maintenanceRepository.replaceAll(emptyList())
            SyncScheduler.scheduleSyncNow(context)
        }
    }

    fun wipeMaintenanceLogLocal() {
        viewModelScope.launch { container.maintenanceRepository.clearAllLocal() }
    }
}
