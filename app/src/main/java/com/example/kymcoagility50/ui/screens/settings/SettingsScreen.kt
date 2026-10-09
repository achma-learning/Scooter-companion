package com.example.kymcoagility50.ui.screens.settings

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
import com.example.kymcoagility50.domain.Dates
import com.example.kymcoagility50.ui.components.BackTopBar
import com.example.kymcoagility50.ui.components.Callout
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.kymcoagility50.auth.AuthManager
import com.example.kymcoagility50.data.repository.ThemeMode
import com.example.kymcoagility50.privacy.AppMode
import com.example.kymcoagility50.ui.common.appViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch as coroutinesLaunch

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val vm = appViewModel { SettingsViewModel(context, it) }
    val state by vm.uiState.collectAsState()
    val message by vm.message.collectAsState()
    var showModeConfirm by remember { mutableStateOf<AppMode?>(null) }
    var confirmImport by remember { mutableStateOf<android.net.Uri?>(null) }
    var confirmWipe by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        vm.setReminders(granted)
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(vm::exportTo)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        confirmImport = uri
    }

    val signInLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            scope.coroutinesLaunch {
                AuthManager.handleSignInResult(account.idToken ?: return@coroutinesLaunch)
                vm.onSignedIn()
            }
        } catch (_: ApiException) { /* user cancelled or sign-in failed — nothing to sync */ }
    }

    Scaffold(topBar = { BackTopBar("Settings & backup", onBack) }) { padding ->
    LazyColumn(Modifier.fillMaxWidth().padding(padding).padding(horizontal = 16.dp)) {
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
            Text("Backup & website", style = MaterialTheme.typography.titleMedium)
            Text(
                "Export your garage (bike, services, passport stamps, fuel, documents) to a JSON file. The website imports the same file, and vice-versa.",
                style = MaterialTheme.typography.bodySmall
            )
            Row(Modifier.padding(vertical = 8.dp)) {
                Button(onClick = { exportLauncher.launch("agility-garage-${Dates.toIso(Dates.today())}.json") }) { Text("Export") }
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
            Text("Mode", style = MaterialTheme.typography.titleMedium)
            ListItem(
                headlineContent = { Text("Private Mode") },
                supportingContent = { Text("No account, no network, everything stays on this device.") },
                trailingContent = {
                    Switch(
                        checked = state.mode == AppMode.PRIVATE,
                        onCheckedChange = { checked -> showModeConfirm = if (checked) AppMode.PRIVATE else AppMode.SYNC }
                    )
                }
            )
            if (state.mode == AppMode.SYNC) {
                if (state.user == null) {
                    Button(onClick = { AuthManager.signInIntent(context)?.let { signInLauncher.launch(it) } }) {
                        Text("Sign in with Google")
                    }
                } else {
                    Text("Signed in as ${state.user?.email}", style = MaterialTheme.typography.bodyMedium)
                    Row {
                        Button(onClick = { vm.onSignedIn() }) { Text("Sync logbook now") }
                        Text(" ")
                        OutlinedButton(onClick = { vm.signOut() }) { Text("Sign out") }
                    }
                }
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
            title = { Text("Replace garage with backup?") },
            text = { Text("Your current bike profile, services, fuel log and documents on this device will be replaced by the file's content.") },
            confirmButton = { TextButton(onClick = { vm.importFrom(uri); confirmImport = null }) { Text("Replace") } },
            dismissButton = { TextButton(onClick = { confirmImport = null }) { Text("Cancel") } }
        )
    }

    if (confirmWipe) {
        AlertDialog(
            onDismissRequest = { confirmWipe = false },
            title = { Text("Erase all garage data?") },
            text = { Text("Deletes the bike profile, every logged service, fuel fill-up and document from this device. Export a backup first if unsure.") },
            confirmButton = { TextButton(onClick = { vm.wipeGarage(); confirmWipe = false }) { Text("Erase") } },
            dismissButton = { TextButton(onClick = { confirmWipe = false }) { Text("Cancel") } }
        )
    }

    showModeConfirm?.let { target ->
        AlertDialog(
            onDismissRequest = { showModeConfirm = null },
            title = { Text(if (target == AppMode.PRIVATE) "Enable Private Mode?" else "Switch to Sync Mode?") },
            text = {
                Text(
                    if (target == AppMode.PRIVATE)
                        "You'll be signed out and no data will be sent anywhere. Your maintenance log stays only on this device from now on."
                    else
                        "You'll be able to sign in so your maintenance logbook is backed up and available on other devices. Reading and search stay fully offline either way."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (target == AppMode.PRIVATE) vm.switchToPrivateMode() else vm.switchToSyncMode()
                    showModeConfirm = null
                }) { Text("Confirm") }
            },
            dismissButton = { TextButton(onClick = { showModeConfirm = null }) { Text("Cancel") } }
        )
    }
}
