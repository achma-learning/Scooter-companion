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
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import io.github.achmalearning.scootercompanion.R
import io.github.achmalearning.scootercompanion.data.export.TextReport
import io.github.achmalearning.scootercompanion.domain.Dates
import java.io.File
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

    /** Shows [text] as the one-line result under the save-file buttons. */
    private fun done(text: String) { _message.value = text }

    fun exportTo(uri: Uri) = write(uri) { container.garageRepository.exportJson() }

    fun exportReportTo(uri: Uri, hideIds: Boolean) = write(uri) { report(hideIds) }

    private suspend fun report(hideIds: Boolean) =
        TextReport.build(container.garageRepository.current(), container.contentRepository.content, hideIds)

    private fun write(uri: Uri, text: suspend () -> String) {
        viewModelScope.launch {
            done(runCatching {
                val body = text()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(body.toByteArray()) }
                }
                context.getString(R.string.set_saved)
            }.getOrElse { context.getString(R.string.set_failed, it.message ?: it.javaClass.simpleName) })
        }
    }

    /**
     * Opens Android's share sheet (WhatsApp, Quick Share, Bluetooth, e-mail…) with the save
     * file or the AI report, written to the app's cache first.
     */
    fun share(saveFile: Boolean, hideIds: Boolean = true) {
        viewModelScope.launch {
            runCatching {
                val name = if (saveFile) "scooter-companion-${Dates.toIso(Dates.today())}.json" else "scooter-report-${Dates.toIso(Dates.today())}.txt"
                val body = if (saveFile) container.garageRepository.exportJson() else report(hideIds)
                val file = withContext(Dispatchers.IO) {
                    File(context.cacheDir, "exports").apply { mkdirs() }.resolve(name).apply { writeText(body) }
                }
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", file)
                val send = Intent(Intent.ACTION_SEND)
                    .setType(if (saveFile) "application/json" else "text/plain")
                    .putExtra(Intent.EXTRA_STREAM, uri)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (!saveFile) send.putExtra(Intent.EXTRA_TEXT, body)
                context.startActivity(Intent.createChooser(send, name).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }.onFailure { done(context.getString(R.string.set_failed, it.message ?: it.javaClass.simpleName)) }
        }
    }

    fun importFrom(uri: Uri) {
        viewModelScope.launch {
            done(runCatching {
                val text = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() }
                }
                context.getString(R.string.set_imported, container.garageRepository.importJson(text, container.maintenanceRepository))
            }.getOrElse { context.getString(R.string.set_failed, it.message ?: it.javaClass.simpleName) })
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
