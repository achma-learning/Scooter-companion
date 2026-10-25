package io.github.achmalearning.scootercompanion.ui.screens.models

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.achmalearning.scootercompanion.AppContainer
import io.github.achmalearning.scootercompanion.R
import io.github.achmalearning.scootercompanion.data.model.PdfSource
import io.github.achmalearning.scootercompanion.data.models.ModelDoc
import io.github.achmalearning.scootercompanion.data.models.ScooterModel
import io.github.achmalearning.scootercompanion.data.region.pick
import io.github.achmalearning.scootercompanion.ui.common.appViewModel
import io.github.achmalearning.scootercompanion.ui.components.BackTopBar
import io.github.achmalearning.scootercompanion.ui.components.Callout
import io.github.achmalearning.scootercompanion.ui.components.DocDownloadCard
import io.github.achmalearning.scootercompanion.ui.components.InfoRow
import io.github.achmalearning.scootercompanion.ui.components.NavRow
import io.github.achmalearning.scootercompanion.ui.components.Panel
import io.github.achmalearning.scootercompanion.ui.components.ScreenPadding
import io.github.achmalearning.scootercompanion.ui.components.SectionTitle
import io.github.achmalearning.scootercompanion.ui.components.formatSize
import io.github.achmalearning.scootercompanion.ui.components.intervalText
import io.github.achmalearning.scootercompanion.ui.components.openUrl
import io.github.achmalearning.scootercompanion.ui.screens.garage.GarageViewModel
import io.github.achmalearning.scootercompanion.ui.screens.profile.engineClassLabel
import kotlinx.coroutines.launch

const val SCOOTER_DATA_URL = "https://github.com/achma-learning/Scooter-companion/tree/main/scooter-data"

/**
 * The owner's model from scooter-data: specifications, the manufacturer's schedule, the PDFs
 * to download, and where every figure comes from. For a model the catalogue doesn't know, how
 * to add it.
 */
@Composable
fun ModelScreen(onBack: () -> Unit, openAgilityDoc: (PdfSource) -> Unit, openCompare: () -> Unit) {
    val context = LocalContext.current
    val vm = appViewModel { GarageViewModel(context, it) }
    val s = vm.state.collectAsStateWithLifecycle().value ?: return
    val model = s.model
    Scaffold(topBar = { BackTopBar(stringResource(R.string.model_title), onBack) }) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = ScreenPadding) {
            if (model == null) {
                item {
                    val name = listOf(s.profile.brand, s.profile.model).filter { it.isNotBlank() }.joinToString(" ").ifBlank { engineClassLabel(s.spec.engineClass) }
                    Callout(stringResource(R.string.model_unknown_title, name), stringResource(R.string.model_unknown_text))
                    NavRow(Icons.Filled.Add, stringResource(R.string.model_add_link), SCOOTER_DATA_URL) { context.openUrl(SCOOTER_DATA_URL) }
                    NavRow(Icons.Filled.CompareArrows, stringResource(R.string.compare_title), stringResource(R.string.compare_sub), onClick = openCompare)
                }
                return@LazyColumn
            }
            item {
                Text(model.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                Text(
                    listOf(model.years, model.code, engineClassLabel(s.spec.engineClass)).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                model.note.pick().takeIf { it.isNotBlank() }?.let { Callout(model.name, it) }
                if (!model.verified) Text("⚠ " + stringResource(R.string.compare_unverified), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                NavRow(Icons.Filled.CompareArrows, stringResource(R.string.compare_with), stringResource(R.string.compare_sub), onClick = openCompare)
            }
            if (model.specs.isNotEmpty()) item {
                SectionTitle(stringResource(R.string.model_specs))
                Panel {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                        model.specs.forEach { InfoRow(specLabel(it.key), it.value) }
                    }
                }
                Text(
                    stringResource(R.string.model_check_manual), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp)
                )
            }
            if (model.schedule.isNotEmpty() && !s.isAgility) item {
                SectionTitle(stringResource(R.string.model_schedule))
                Panel {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                        model.schedule.forEach { (taskId, iv) ->
                            val name = vm.content.task(taskId)?.localName() ?: taskId
                            InfoRow(name, intervalText(iv.km, iv.months))
                        }
                    }
                }
                Text(
                    stringResource(R.string.model_schedule_help), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp)
                )
            }
            if (model.docs.isNotEmpty()) {
                item { SectionTitle(stringResource(R.string.model_docs)) }
                items(model.docs, key = { it.id }) { doc -> DocRow(model, doc, s.isAgility, openAgilityDoc) }
            }
            if (model.sources.isNotEmpty()) {
                item { SectionTitle(stringResource(R.string.model_sources)) }
                items(model.sources) { src ->
                    val url = if (src.url.startsWith("https://")) src.url else ""
                    NavRow(Icons.AutoMirrored.Filled.OpenInNew, src.title, url.ifBlank { null }) { if (url.isNotBlank()) context.openUrl(url) }
                }
            }
        }
    }
}

/** One PDF: Download until it is on the phone, then Open and Remove. */
@Composable
private fun DocRow(model: ScooterModel, doc: ModelDoc, agility: Boolean, openAgilityDoc: (PdfSource) -> Unit) {
    val context = LocalContext.current
    val docs = remember { AppContainer.getInstance(context).modelDocs }
    val scope = rememberCoroutineScope()
    var version by remember { mutableStateOf(0) }
    val downloaded = remember(version) { docs.isDownloaded(model, doc) }
    val progress = remember { mutableStateMapOf<String, Float>() }
    var error by remember { mutableStateOf<String?>(null) }
    val title = doc.title.pick()

    if (!downloaded) {
        DocDownloadCard(
            title, doc.bytes, progress[doc.id], error,
            onDownload = {
                error = null
                progress[doc.id] = 0f
                scope.launch {
                    docs.download(model, doc) { p -> progress[doc.id] = p }
                        .onFailure { error = it.message ?: "?" }
                    progress.remove(doc.id)
                    version++
                }
            },
            modifier = Modifier.padding(vertical = 4.dp)
        )
        return
    }
    val noViewer = stringResource(R.string.doc_no_viewer)
    Panel(modifier = Modifier.padding(vertical = 4.dp)) {
        Column(Modifier.padding(14.dp)) {
            Row {
                androidx.compose.material3.Icon(Icons.Filled.Description, null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.padding(start = 10.dp)) {
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(listOf(formatSize(doc.bytes), doc.lang).filter { it.isNotBlank() }.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                Button(onClick = {
                    // The Agility's documents open in the app's own reader, with search and bookmarks.
                    val inApp = if (agility) PdfSource.entries.firstOrNull { it.name == doc.id } else null
                    if (inApp != null) openAgilityDoc(inApp)
                    else if (!docs.openExternally(model, doc)) Toast.makeText(context, noViewer, Toast.LENGTH_SHORT).show()
                }) { Text(stringResource(R.string.doc_open)) }
                TextButton(onClick = { docs.delete(model, doc); version++ }) { Text(stringResource(R.string.doc_remove)) }
            }
        }
    }
}

@Composable
fun specLabel(key: String): String = when (key) {
    "engine" -> stringResource(R.string.spec_engine)
    "power" -> stringResource(R.string.spec_power)
    "engineOil" -> stringResource(R.string.spec_engineOil)
    "engineOilQty" -> stringResource(R.string.spec_engineOilQty)
    "gearOil" -> stringResource(R.string.spec_gearOil)
    "coolant" -> stringResource(R.string.spec_coolant)
    "sparkPlug" -> stringResource(R.string.spec_sparkPlug)
    "valves" -> stringResource(R.string.spec_valves)
    "tyreFront" -> stringResource(R.string.spec_tyreFront)
    "tyreRear" -> stringResource(R.string.spec_tyreRear)
    "pressureFront" -> stringResource(R.string.spec_pressureFront)
    "pressureRear" -> stringResource(R.string.spec_pressureRear)
    "brakeFront" -> stringResource(R.string.spec_brakeFront)
    "brakeRear" -> stringResource(R.string.spec_brakeRear)
    "transmission" -> stringResource(R.string.spec_transmission)
    "brakeFluid" -> stringResource(R.string.spec_brakeFluid)
    "battery" -> stringResource(R.string.spec_battery)
    "fuel" -> stringResource(R.string.spec_fuel)
    "idle" -> stringResource(R.string.spec_idle)
    else -> key
}

/** Which specs matter for a maintenance task, shown on its page. */
fun specKeysFor(taskId: String): List<String> = when (taskId) {
    "engine-oil", "oil-filter", "oil-screen" -> listOf("engineOil", "engineOilQty")
    "gear-oil" -> listOf("gearOil")
    "coolant-level", "coolant-replace", "radiator-clean" -> listOf("coolant")
    "plug-replace", "plug-clean" -> listOf("sparkPlug")
    "valves" -> listOf("valves")
    "tyre-pressure" -> listOf("pressureFront", "pressureRear")
    "tyre-front" -> listOf("tyreFront", "pressureFront")
    "tyre-rear" -> listOf("tyreRear", "pressureRear")
    "tyre-condition" -> listOf("tyreFront", "tyreRear")
    "brake-fluid" -> listOf("brakeFluid")
    "battery" -> listOf("battery")
    else -> emptyList()
}
