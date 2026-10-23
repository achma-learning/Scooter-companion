package io.github.achmalearning.scootercompanion.ui.screens.settings

import androidx.compose.ui.Alignment
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.Checkbox
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
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
import io.github.achmalearning.scootercompanion.R
import io.github.achmalearning.scootercompanion.ui.common.appViewModel
import io.github.achmalearning.scootercompanion.ui.screens.onboarding.LanguageChips
import androidx.compose.ui.res.stringResource

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
    var hideIds by rememberSaveable { mutableStateOf(true) }
    val reportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        uri?.let { vm.exportReportTo(it, hideIds) }
    }

    Scaffold(topBar = { BackTopBar(stringResource(R.string.set_title), onBack) }) { padding ->
    LazyColumn(Modifier.fillMaxWidth().padding(padding).padding(horizontal = 16.dp)) {
        item {
            Text(stringResource(R.string.language), style = MaterialTheme.typography.titleMedium)
            LanguageChips(withSystem = true)
            Divider(Modifier.padding(vertical = 12.dp))
        }

        item {
            AppUpdatesSection(vm)
            Divider(Modifier.padding(vertical = 12.dp))
        }

        item {
            Text(stringResource(R.string.set_reminders), style = MaterialTheme.typography.titleMedium)
            ListItem(
                headlineContent = { Text(stringResource(R.string.set_reminders_item)) },
                supportingContent = { Text(stringResource(R.string.set_reminders_text)) },
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
            Text(stringResource(R.string.set_save_file), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.set_save_text), style = MaterialTheme.typography.bodySmall)
            FlowRow(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { exportLauncher.launch("scooter-companion-${Dates.toIso(Dates.today())}.json") }) { Text(stringResource(R.string.set_export)) }
                OutlinedButton(onClick = { vm.share(saveFile = true) }) { Text(stringResource(R.string.set_share)) }
                OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }) { Text(stringResource(R.string.set_import)) }
            }
            message?.let {
                Callout(stringResource(R.string.set_save_file), it)
                LaunchedEffect(it) { kotlinx.coroutines.delay(6000); vm.consumeMessage() }
            }
            Divider(Modifier.padding(vertical = 12.dp))
        }

        item {
            Text(stringResource(R.string.set_ai_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.set_ai_text), style = MaterialTheme.typography.bodySmall)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(hideIds, { hideIds = it })
                Text(stringResource(R.string.set_ai_hide), style = MaterialTheme.typography.bodyMedium)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { reportLauncher.launch("scooter-report-${Dates.toIso(Dates.today())}.txt") }) { Text(stringResource(R.string.set_export)) }
                OutlinedButton(onClick = { vm.share(saveFile = false, hideIds = hideIds) }) { Text(stringResource(R.string.set_share)) }
            }
            Divider(Modifier.padding(vertical = 12.dp))
        }

        item {
            Text(stringResource(R.string.set_appearance), style = MaterialTheme.typography.titleMedium)
            ListItem(
                headlineContent = { Text(stringResource(R.string.set_keep_on)) },
                trailingContent = {
                    Switch(checked = state.settings.keepScreenOn, onCheckedChange = vm::setKeepScreenOn)
                }
            )
            Text(stringResource(R.string.set_theme), style = MaterialTheme.typography.bodyMedium)
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = state.settings.theme == mode,
                        onClick = { vm.setTheme(mode) },
                        label = {
                            Text(stringResource(when (mode) { ThemeMode.LIGHT -> R.string.set_theme_LIGHT; ThemeMode.DARK -> R.string.set_theme_DARK; ThemeMode.SYSTEM -> R.string.set_theme_SYSTEM }))
                        },
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
            }
            Divider(Modifier.padding(vertical = 12.dp))
        }

        item {
            Text(stringResource(R.string.set_data), style = MaterialTheme.typography.titleMedium)
            OutlinedButton(onClick = { vm.clearRecent() }) { Text(stringResource(R.string.set_clear_recent)) }
            OutlinedButton(onClick = { vm.clearBookmarks() }) { Text(stringResource(R.string.set_clear_bookmarks)) }
            OutlinedButton(onClick = { vm.clearOcrIndexAndReseed() }) { Text(stringResource(R.string.set_rebuild_search)) }
            OutlinedButton(onClick = { confirmWipe = true }) { Text(stringResource(R.string.set_erase)) }
            Text(" ", Modifier.padding(bottom = 96.dp))
        }
    }
    }

    confirmImport?.let { uri ->
        AlertDialog(
            onDismissRequest = { confirmImport = null },
            title = { Text(stringResource(R.string.set_replace_title)) },
            text = { Text(stringResource(R.string.set_replace_text)) },
            confirmButton = { TextButton(onClick = { vm.importFrom(uri); confirmImport = null }) { Text(stringResource(R.string.set_replace)) } },
            dismissButton = { TextButton(onClick = { confirmImport = null }) { Text(stringResource(R.string.action_cancel)) } }
        )
    }

    if (confirmWipe) {
        AlertDialog(
            onDismissRequest = { confirmWipe = false },
            title = { Text(stringResource(R.string.set_erase_title)) },
            text = { Text(stringResource(R.string.set_erase_text)) },
            confirmButton = { TextButton(onClick = { vm.wipeGarage(); confirmWipe = false }) { Text(stringResource(R.string.set_erase_confirm)) } },
            dismissButton = { TextButton(onClick = { confirmWipe = false }) { Text(stringResource(R.string.action_cancel)) } }
        )
    }
}

/** Check GitHub Releases, download the APK and hand it to the system installer — all on tap. */
@Composable
private fun AppUpdatesSection(vm: SettingsViewModel) {
    val context = LocalContext.current
    val update by vm.update.collectAsState()
    val mb = { bytes: Long -> if (bytes > 0) " (%.0f MB)".format(bytes / 1_000_000.0) else "" }

    Text(stringResource(R.string.upd_title), style = MaterialTheme.typography.titleMedium)
    ListItem(
        headlineContent = { Text("Scooter Companion ${vm.installedVersion}") },
        supportingContent = {
            Text(
                when (val u = update) {
                    UpdateState.Idle -> stringResource(R.string.upd_idle)
                    UpdateState.Checking -> stringResource(R.string.upd_checking)
                    is UpdateState.UpToDate -> stringResource(R.string.upd_up_to_date, u.latest.versionName)
                    is UpdateState.Available -> stringResource(R.string.upd_available, u.update.versionName) +
                        (u.update.publishedOn?.let { " · ${Dates.fromIso(it)?.let(Dates::display) ?: it}" } ?: "")
                    is UpdateState.Downloading -> stringResource(R.string.upd_downloading, u.update.versionName) + (u.fraction?.let { " ${(it * 100).toInt()}%" } ?: "")
                    is UpdateState.ReadyToInstall ->
                        if (u.needsPermission) stringResource(R.string.upd_allow)
                        else stringResource(R.string.upd_ready, u.update.versionName)
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
            Button(onClick = { vm.downloadUpdate(u.update) }) { Text(stringResource(R.string.upd_download) + mb(u.update.apkBytes)) }
        is UpdateState.ReadyToInstall ->
            Button(onClick = { vm.installUpdate(u.update) }) { Text(stringResource(R.string.upd_install, u.update.versionName)) }
        is UpdateState.Failed -> Row {
            OutlinedButton(onClick = { vm.checkForUpdate() }) { Text(stringResource(R.string.upd_retry)) }
            // Fallback: the browser can always fetch the APK from the release page.
            u.update?.let { failed ->
                Text(" ")
                TextButton(onClick = { context.openUrl(failed.pageUrl) }) { Text(stringResource(R.string.upd_open_page)) }
            }
        }
        else -> OutlinedButton(enabled = update != UpdateState.Checking, onClick = { vm.checkForUpdate() }) { Text(stringResource(R.string.upd_check)) }
    }
    if (vm.isDebugBuild) {
        Text(
            stringResource(R.string.upd_debug),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
