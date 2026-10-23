package io.github.achmalearning.scootercompanion.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ListItem
import io.github.achmalearning.scootercompanion.ui.components.openUrl
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import android.Manifest
import android.os.Build
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import io.github.achmalearning.scootercompanion.domain.Dates
import io.github.achmalearning.scootercompanion.ui.components.BackTopBar
import io.github.achmalearning.scootercompanion.ui.components.Callout
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.achmalearning.scootercompanion.data.repository.ThemeMode
import io.github.achmalearning.scootercompanion.ui.common.appViewModel

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val vm = appViewModel { SettingsViewModel(context, it) }
    val state by vm.uiState.collectAsState()
    val message by vm.message.collectAsState()
    var confirmImport by remember { mutableStateOf<android.net.Uri?>(null) }
    var confirmWipe by remember { mutableStateOf(false) }

    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        vm.setReminders(granted)
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(vm::exportTo)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        confirmImport = uri
    }

    Scaffold(topBar = { BackTopBar("Settings & backup", onBack) }) { padding ->
    LazyColumn(Modifier.fillMaxWidth().padding(padding).padding(horizontal = 16.dp)) {
        item {
            AppUpdatesSection(vm)
            Divider(Modifier.padding(vertical = 12.dp))
        }

        item {
            Text("Reminders", style = MaterialTheme.typography.titleMedium)
            ListItem(
                headlineContent = { Text("Service & document reminders") },
                supportingContent = { Text("One daily check on this device: overdue services, insurance/licence expiring. No network.") },
                trailingContent = {
                    Switch(
                        checked = state.settings.reminders,
                        onCheckedChange = { on ->
                            if (on && Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            else vm.setReminders(on)
                        }
                    )
                }
            )
            Divider(Modifier.padding(vertical = 12.dp))
        }

        item {
            Text("Save file", style = MaterialTheme.typography.titleMedium)
            Text(
                "Export everything (scooter, services, fuel, insurance, expenses, documents) to one file. Import it on another phone to carry on there. Agility Companion backups import too.",
                style = MaterialTheme.typography.bodySmall
            )
            Row(Modifier.padding(vertical = 8.dp)) {
                Button(onClick = { exportLauncher.launch("scooter-companion-${Dates.toIso(Dates.today())}.json") }) { Text("Export") }
                Text(" ")
                OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }) { Text("Import…") }
            }
            message?.let {
                Callout("Backup", it)
                LaunchedEffect(it) { kotlinx.coroutines.delay(6000); vm.consumeMessage() }
            }
            Divider(Modifier.padding(vertical = 12.dp))
        }

        item {
            Text("Reading", style = MaterialTheme.typography.titleMedium)
            ListItem(
                headlineContent = { Text("Keep screen on while reading") },
                trailingContent = {
                    Switch(checked = state.settings.keepScreenOn, onCheckedChange = vm::setKeepScreenOn)
                }
            )
            Text("Theme", style = MaterialTheme.typography.bodyMedium)
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = state.settings.theme == mode,
                        onClick = { vm.setTheme(mode) },
                        label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
            }
            Divider(Modifier.padding(vertical = 12.dp))
        }

        item {
            Text("Data", style = MaterialTheme.typography.titleMedium)
            OutlinedButton(onClick = { vm.clearRecent() }) { Text("Clear recent history") }
            OutlinedButton(onClick = { vm.clearBookmarks() }) { Text("Clear bookmarks") }
            OutlinedButton(onClick = { vm.clearOcrIndexAndReseed() }) { Text("Rebuild search index") }
            OutlinedButton(onClick = { confirmWipe = true }) { Text("Erase garage data…") }
            Text(" ", Modifier.padding(bottom = 96.dp))
        }
    }
    }

    confirmImport?.let { uri ->
        AlertDialog(
            onDismissRequest = { confirmImport = null },
            title = { Text("Replace everything with this file?") },
            text = { Text("Your scooter, services, fuel log, insurance, expenses and documents on this phone will be replaced by the file's content.") },
            confirmButton = { TextButton(onClick = { vm.importFrom(uri); confirmImport = null }) { Text("Replace") } },
            dismissButton = { TextButton(onClick = { confirmImport = null }) { Text("Cancel") } }
        )
    }

    if (confirmWipe) {
        AlertDialog(
            onDismissRequest = { confirmWipe = false },
            title = { Text("Erase all garage data?") },
            text = { Text("Deletes the scooter profile and photo, every service, fill-up, insurance contract, expense and document from this phone. Export a save file first if unsure.") },
            confirmButton = { TextButton(onClick = { vm.wipeGarage(); confirmWipe = false }) { Text("Erase") } },
            dismissButton = { TextButton(onClick = { confirmWipe = false }) { Text("Cancel") } }
        )
    }
}

/** Check GitHub Releases, download the APK and hand it to the system installer — all on tap. */
@Composable
private fun AppUpdatesSection(vm: SettingsViewModel) {
    val context = LocalContext.current
    val update by vm.update.collectAsState()
    val mb = { bytes: Long -> if (bytes > 0) " (%.0f MB)".format(bytes / 1_000_000.0) else "" }

    Text("App updates", style = MaterialTheme.typography.titleMedium)
    ListItem(
        headlineContent = { Text("Scooter Companion ${vm.installedVersion}") },
        supportingContent = {
            Text(
                when (val u = update) {
                    UpdateState.Idle -> "Updates come from this app's GitHub Releases. Checked only when you tap."
                    UpdateState.Checking -> "Checking GitHub…"
                    is UpdateState.UpToDate -> "You're up to date (latest is ${u.latest.versionName})."
                    is UpdateState.Available -> "Version ${u.update.versionName} is available" +
                        (u.update.publishedOn?.let { " · ${Dates.fromIso(it)?.let(Dates::display) ?: it}" } ?: "") + "."
                    is UpdateState.Downloading -> "Downloading ${u.update.versionName}…" + (u.fraction?.let { " ${(it * 100).toInt()}%" } ?: "")
                    is UpdateState.ReadyToInstall ->
                        if (u.needsPermission) "Allow \"Install unknown apps\" for Scooter Companion, come back, then tap Install."
                        else "Version ${u.update.versionName} is downloaded. It installs over this app and keeps your data."
                    is UpdateState.Failed -> u.message
                }
            )
        }
    )
    when (val u = update) {
        is UpdateState.Downloading ->
            if (u.fraction != null) LinearProgressIndicator(progress = { u.fraction ?: 0f }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
            else LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 8.dp))
        is UpdateState.Available ->
            Button(onClick = { vm.downloadUpdate(u.update) }) { Text("Download & install" + mb(u.update.apkBytes)) }
        is UpdateState.ReadyToInstall ->
            Button(onClick = { vm.installUpdate(u.update) }) { Text("Install ${u.update.versionName}") }
        is UpdateState.Failed -> Row {
            OutlinedButton(onClick = { vm.checkForUpdate() }) { Text("Try again") }
            // Fallback: the browser can always fetch the APK from the release page.
            u.update?.let { failed ->
                Text(" ")
                TextButton(onClick = { context.openUrl(failed.pageUrl) }) { Text("Open release page") }
            }
        }
        else -> OutlinedButton(enabled = update != UpdateState.Checking, onClick = { vm.checkForUpdate() }) { Text("Check for updates") }
    }
    if (vm.isDebugBuild) {
        Text(
            "This is a debug build: a release APK installs as a separate app beside it.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
