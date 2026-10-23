package io.github.achmalearning.scootercompanion.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.achmalearning.scootercompanion.AppContainer
import io.github.achmalearning.scootercompanion.data.model.PdfSource
import io.github.achmalearning.scootercompanion.data.repository.AppSettings
import io.github.achmalearning.scootercompanion.data.update.ApkUpdater
import io.github.achmalearning.scootercompanion.data.update.AppUpdate
import io.github.achmalearning.scootercompanion.data.update.UpdateChecker
import io.github.achmalearning.scootercompanion.work.ReminderWorker
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Settings → "App updates": every step starts from a tap, nothing runs in the background. */
sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data class UpToDate(val latest: AppUpdate) : UpdateState
    data class Available(val update: AppUpdate) : UpdateState
    data class Downloading(val update: AppUpdate, val fraction: Float?) : UpdateState
    data class ReadyToInstall(val update: AppUpdate, val needsPermission: Boolean = false) : UpdateState
    data class Failed(val message: String, val update: AppUpdate? = null) : UpdateState
}

data class SettingsUiState(
    val settings: AppSettings = AppSettings()
)

class SettingsViewModel(
    private val context: Context,
    private val container: AppContainer
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = container.settingsRepository.settings
        .map { SettingsUiState(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun setTheme(mode: io.github.achmalearning.scootercompanion.data.repository.ThemeMode) {
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
                "Save file written. Import it in Scooter Companion on any phone."
            }.getOrElse { "Export failed: ${it.message}" }
        }
    }

    fun importFrom(uri: Uri) {
        viewModelScope.launch {
            _message.value = runCatching {
                val text = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() }
                }
                container.garageRepository.importJson(text, container.maintenanceRepository)
            }.getOrElse { "Import failed: ${it.message}" }
        }
    }

    fun wipeGarage() {
        viewModelScope.launch {
            container.garageRepository.wipeGarage()
            container.maintenanceRepository.replaceAll(emptyList())
        }
    }

    // ---------------------------------------------------------------- app updates

    private val updater = ApkUpdater(context)
    val installedVersion: String = updater.installedVersionName()
    val isDebugBuild: Boolean = updater.isDebugBuild

    private val _update = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val update = _update.asStateFlow()

    fun checkForUpdate() {
        _update.value = UpdateState.Checking
        viewModelScope.launch {
            _update.value = UpdateChecker.latest().fold(
                onSuccess = { latest ->
                    when {
                        latest.versionCode <= updater.installedVersionCode() -> UpdateState.UpToDate(latest)
                        updater.isDownloaded(latest) -> UpdateState.ReadyToInstall(latest)
                        else -> UpdateState.Available(latest)
                    }
                },
                onFailure = { UpdateState.Failed(it.message ?: "No connection") }
            )
        }
    }

    fun downloadUpdate(update: AppUpdate) {
        _update.value = UpdateState.Downloading(update, null)
        viewModelScope.launch {
            runCatching {
                updater.download(update) { p ->
                    _update.value = when (p) {
                        is ApkUpdater.Progress.Running -> UpdateState.Downloading(update, p.fraction)
                        ApkUpdater.Progress.Done -> UpdateState.ReadyToInstall(update)
                        is ApkUpdater.Progress.Failed -> UpdateState.Failed(p.reason, update)
                    }
                }
            }.onFailure { _update.value = UpdateState.Failed(it.message ?: "Download failed", update) }
        }
    }

    fun installUpdate(update: AppUpdate) {
        _update.value = when (updater.install(update)) {
            // Stays on "Install" so the owner can tap it again once Android has allowed it.
            ApkUpdater.InstallResult.NEEDS_PERMISSION -> UpdateState.ReadyToInstall(update, needsPermission = true)
            ApkUpdater.InstallResult.STARTED -> UpdateState.ReadyToInstall(update)
            ApkUpdater.InstallResult.FAILED -> UpdateState.Failed("Couldn't open the installer", update)
        }
    }
}
