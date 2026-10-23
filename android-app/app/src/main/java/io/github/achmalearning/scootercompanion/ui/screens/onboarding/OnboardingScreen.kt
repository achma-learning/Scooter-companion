package io.github.achmalearning.scootercompanion.ui.screens.onboarding

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.achmalearning.scootercompanion.AppContainer
import io.github.achmalearning.scootercompanion.R
import io.github.achmalearning.scootercompanion.data.db.BikeProfileEntity
import io.github.achmalearning.scootercompanion.domain.Dates
import io.github.achmalearning.scootercompanion.ui.common.AppLanguage
import io.github.achmalearning.scootercompanion.ui.components.ScooterArt
import io.github.achmalearning.scootercompanion.ui.screens.profile.DetailsSection
import io.github.achmalearning.scootercompanion.ui.screens.profile.ProfileDraft
import io.github.achmalearning.scootercompanion.ui.screens.profile.PurchaseSection
import io.github.achmalearning.scootercompanion.ui.screens.profile.ScooterTypeSection
import io.github.achmalearning.scootercompanion.ui.theme.PrivateGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val STEPS = 3

/**
 * First launch: welcome (language, privacy, import a save file), then three steps to add the
 * scooter: its type, the purchase (new or used), and optional details. [onDone] fires once the
 * profile is saved or a save file imported.
 */
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val container = remember { AppContainer.getInstance(context) }
    val region = remember { container.regionRepository.pack("MA") }
    // Survives the activity being recreated when the language changes.
    var step by rememberSaveable { mutableIntStateOf(0) }
    val draft = remember { ProfileDraft(BikeProfileEntity(purchaseDate = Dates.today(), region = region.id)) }
    var importError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                val text = withContext(Dispatchers.IO) { context.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() } }
                container.garageRepository.importJson(text, container.maintenanceRepository)
            }.onSuccess { onDone() }.onFailure { importError = it.message ?: it.javaClass.simpleName }
        }
    }

    BackHandler(enabled = step > 0) { step-- }

    Column(Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
        if (step > 0) {
            LinearProgressIndicator(progress = { step / STEPS.toFloat() }, modifier = Modifier.fillMaxWidth())
            Text(
                stringResource(R.string.onb_step_of, step, STEPS),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(start = 24.dp, top = 12.dp)
            )
            Text(
                stringResource(listOf(R.string.onb_step_type, R.string.onb_step_purchase, R.string.onb_step_details)[step - 1]),
                style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = if (step == 0) Arrangement.Center else Arrangement.Top
        ) {
            when (step) {
                0 -> Welcome(onImport = { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }, importError = importError)
                1 -> ScooterTypeSection(draft, region)
                2 -> PurchaseSection(draft, region)
                else -> {
                    Text(stringResource(R.string.onb_details_later), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    DetailsSection(draft, region)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            if (step > 0) TextButton(onClick = { step-- }) { Text(stringResource(R.string.action_back)) }
            Spacer(Modifier.weight(1f))
            when {
                step == 0 -> Button(onClick = { step = 1 }) { Text(stringResource(R.string.onb_add_scooter)) }
                step < STEPS -> Column(horizontalAlignment = Alignment.End) {
                    if (step == 1 && !draft.typeValid) {
                        Text(stringResource(R.string.onb_type_required), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(onClick = { step++ }, enabled = step != 1 || draft.typeValid) { Text(stringResource(R.string.action_next)) }
                }
                else -> Button(onClick = {
                    scope.launch {
                        container.garageRepository.saveProfile(draft.toEntity())
                        onDone()
                    }
                }) { Text(stringResource(R.string.action_start)) }
            }
        }
    }
}

@Composable
private fun Welcome(onImport: () -> Unit, importError: String?) {
    ScooterArt(Modifier.fillMaxWidth().height(140.dp))
    Spacer(Modifier.height(16.dp))
    Text(stringResource(R.string.onb_welcome_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
    Text(stringResource(R.string.onb_welcome_text), style = MaterialTheme.typography.bodyLarge)
    Spacer(Modifier.height(16.dp))
    Row(verticalAlignment = Alignment.Top) {
        Icon(Icons.Filled.Shield, contentDescription = null, tint = PrivateGreen)
        Text(
            stringResource(R.string.onb_privacy), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 8.dp)
        )
    }
    Spacer(Modifier.height(16.dp))
    Text(stringResource(R.string.language), style = MaterialTheme.typography.labelLarge)
    LanguageChips()
    Spacer(Modifier.height(16.dp))
    OutlinedButton(onClick = onImport) { Text(stringResource(R.string.onb_import)) }
    importError?.let {
        Text(stringResource(R.string.onb_import_failed, it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
}

/** French / English / Arabic; tapping one switches the whole app right away. */
@Composable
fun LanguageChips(withSystem: Boolean = false) {
    val current = remember {
        AppLanguage.current().let { lang ->
            // Without the "phone's language" chip, show which of the three the phone is using.
            if (lang == AppLanguage.SYSTEM && !withSystem)
                AppLanguage.choices.firstOrNull { it.tag.startsWith(java.util.Locale.getDefault().language) } ?: lang
            else lang
        }
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val options = if (withSystem) listOf(AppLanguage.SYSTEM) + AppLanguage.choices else AppLanguage.choices
        options.forEach { lang ->
            FilterChip(
                selected = lang == current,
                onClick = { if (lang != current) AppLanguage.apply(lang) },
                label = { Text(if (lang == AppLanguage.SYSTEM) stringResource(R.string.language_system) else lang.label) }
            )
        }
    }
}
