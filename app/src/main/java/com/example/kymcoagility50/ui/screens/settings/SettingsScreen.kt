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
import androidx.compose.runtime.Composable
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
fun SettingsScreen() {
    val context = LocalContext.current
    val vm = appViewModel { SettingsViewModel(context, it) }
    val state by vm.uiState.collectAsState()
    var showModeConfirm by remember { mutableStateOf<AppMode?>(null) }
    val scope = rememberCoroutineScope()

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

    LazyColumn(Modifier.fillMaxWidth().padding(16.dp)) {
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
                    OutlinedButton(onClick = { vm.signOut() }) { Text("Sign out") }
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
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    TextButton(onClick = { vm.setTheme(mode) }) {
                        Text(mode.name + if (state.settings.theme == mode) " ✓" else "")
                    }
                }
            }
            Divider(Modifier.padding(vertical = 12.dp))
        }

        item {
            Text("Data", style = MaterialTheme.typography.titleMedium)
            OutlinedButton(onClick = { vm.clearRecent() }) { Text("Clear recent history") }
            OutlinedButton(onClick = { vm.clearBookmarks() }) { Text("Clear bookmarks") }
            OutlinedButton(onClick = { vm.clearOcrIndexAndReseed() }) { Text("Rebuild search index") }
            OutlinedButton(onClick = { vm.wipeMaintenanceLogLocal() }) { Text("Wipe local maintenance log") }
        }
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
