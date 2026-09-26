package com.example.kymcoagility50.ui.screens.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.kymcoagility50.ui.theme.KymcoRed
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(700)
        onFinished()
    }
    Column(
        modifier = Modifier.fillMaxSize().background(KymcoRed),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("KYMCO", color = Color.White, style = MaterialTheme.typography.displayMedium)
        Text("Agility 50 — Owner Manual", color = Color.White, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(24.dp))
        CircularProgressIndicator(color = Color.White)
    }
}
