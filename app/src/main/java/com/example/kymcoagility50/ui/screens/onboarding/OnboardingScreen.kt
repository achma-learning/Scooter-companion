package com.example.kymcoagility50.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.kymcoagility50.privacy.AppMode
import com.example.kymcoagility50.ui.theme.PrivateGreen

@Composable
fun OnboardingScreen(onChoose: (AppMode) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Welcome", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "This app reads your KYMCO Agility 50 manual and passeport entirely on your " +
                "device. Choose how you'd like to keep your maintenance logbook.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = PrivateGreen.copy(alpha = 0.08f))
        ) {
            Column(Modifier.padding(16.dp)) {
                Icon(Icons.Filled.Shield, contentDescription = null, tint = PrivateGreen)
                Spacer(Modifier.height(8.dp))
                Text("Private Mode (recommended)", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Nothing ever leaves this device. No account, no network requests, " +
                        "your maintenance log stays local only.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = { onChoose(AppMode.PRIVATE) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Enable Private Mode")
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Icon(Icons.Filled.CloudSync, contentDescription = null)
                Spacer(Modifier.height(8.dp))
                Text("Sync Mode", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Sign in to keep your maintenance logbook (date / km / notes) backed up " +
                        "and available on another device. Reading and search still work exactly " +
                        "the same, fully offline.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = { onChoose(AppMode.SYNC) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Use Sync Mode")
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            "You can switch between the two anytime in Settings.",
            style = MaterialTheme.typography.labelMedium
        )
    }
}
