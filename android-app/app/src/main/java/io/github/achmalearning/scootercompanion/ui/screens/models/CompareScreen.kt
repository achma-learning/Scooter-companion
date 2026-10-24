package io.github.achmalearning.scootercompanion.ui.screens.models

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.achmalearning.scootercompanion.AppContainer
import io.github.achmalearning.scootercompanion.R
import io.github.achmalearning.scootercompanion.data.models.ScooterModel
import io.github.achmalearning.scootercompanion.ui.charts.chartColors
import io.github.achmalearning.scootercompanion.ui.common.appViewModel
import io.github.achmalearning.scootercompanion.ui.components.BackTopBar
import io.github.achmalearning.scootercompanion.ui.components.Callout
import io.github.achmalearning.scootercompanion.ui.components.Panel
import io.github.achmalearning.scootercompanion.ui.components.ScreenPadding
import io.github.achmalearning.scootercompanion.ui.components.SectionTitle
import io.github.achmalearning.scootercompanion.ui.screens.garage.GarageViewModel
import java.util.Locale

/** Which way a figure is better, for the "best of this comparison" mark. */
private enum class Better { HIGHER, LOWER, NONE }

/** One numeric row: its label, how to read each model's value, and how to print it. */
private class Metric(val label: String, val better: Better, val value: (ScooterModel) -> Double?, val text: (ScooterModel) -> String?)

/**
 * Two or three models from scooter-data side by side. Each number gets a bar scaled to the
 * largest of the row (the matplotlib barh idea, one bar per cell) and the best one is marked.
 */
@Composable
fun CompareScreen(onBack: () -> Unit, initial: List<String>? = null) {
    val context = LocalContext.current
    val catalog = remember { AppContainer.getInstance(context).modelCatalog }
    val vm = appViewModel { GarageViewModel(context, it) }
    val own = vm.state.collectAsStateWithLifecycle().value?.model
    var ids by rememberSaveable { mutableStateOf(initial ?: listOfNotNull(own?.id)) }
    var picking by remember { mutableStateOf(false) }
    val chosen = ids.mapNotNull(catalog::byId)
    val colors = chartColors().series

    Scaffold(topBar = { BackTopBar(stringResource(R.string.compare_title), onBack) }) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = ScreenPadding) {
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    chosen.forEachIndexed { i, m ->
                        InputChip(
                            selected = true, onClick = { ids = ids - m.id },
                            label = { Text(m.name + if (m.id == own?.id) " · " + stringResource(R.string.compare_yours) else "") },
                            leadingIcon = { Box(Modifier.size(10.dp).clip(CircleShape).background(colors[i])) },
                            trailingIcon = { Icon(Icons.Filled.Close, null, Modifier.size(16.dp)) }
                        )
                    }
                    if (chosen.size < 3) AssistChip(
                        onClick = { picking = true }, label = { Text(stringResource(R.string.compare_add)) },
                        leadingIcon = { Icon(Icons.Filled.Add, null) }
                    )
                }
                if (chosen.size < 2) Text(
                    stringResource(R.string.compare_empty), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp)
                )
            }
            if (chosen.isNotEmpty()) {
                item { CompareTable(chosen, colors) }
                if (chosen.any { !it.verified }) item {
                    Text(
                        "⚠ " + chosen.filter { !it.verified }.joinToString { it.name } + ": " + stringResource(R.string.compare_unverified),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp)
                    )
                }
                if (chosen.map { it.figures.weightKind }.filter { it.isNotBlank() }.distinct().size > 1) item {
                    Callout(stringResource(R.string.compare_weight), stringResource(R.string.compare_weight_note))
                }
                item { Callout(stringResource(R.string.compare_learn_title), stringResource(R.string.compare_learn_text)) }
            }
        }
    }

    if (picking) ModelPicker(catalog.models, ids, onDone = { ids = it; picking = false }, onDismiss = { picking = false })
}

@Composable
private fun CompareTable(models: List<ScooterModel>, colors: List<Color>) {
    val kinds = mapOf(
        "dry" to stringResource(R.string.weight_dry), "kerb" to stringResource(R.string.weight_kerb), "wet" to stringResource(R.string.weight_wet)
    )
    val metrics = listOf(
        Metric(stringResource(R.string.compare_power), Better.HIGHER, { it.figures.powerKw }) { m ->
            m.figures.powerKw?.let { kw -> num(kw) + " kW" + (m.figures.powerRpm?.let { "\n@ ${grouped(it)} rpm" } ?: "") }
        },
        Metric(stringResource(R.string.compare_torque), Better.HIGHER, { it.figures.torqueNm }) { m ->
            m.figures.torqueNm?.let { nm -> num(nm) + " Nm" + (m.figures.torqueRpm?.let { "\n@ ${grouped(it)} rpm" } ?: "") }
        },
        Metric(stringResource(R.string.compare_weight), Better.LOWER, { it.figures.weightKg }) { m ->
            m.figures.weightKg?.let { num(it) + " kg" + (kinds[m.figures.weightKind]?.let { k -> " ($k)" } ?: "") }
        },
        Metric(stringResource(R.string.compare_ptw), Better.HIGHER, { it.figures.powerToWeight }) { m -> m.figures.powerToWeight?.let { "%.0f W/kg".format(Locale.US, it) } },
        Metric(stringResource(R.string.compare_tank), Better.HIGHER, { it.figures.tankL }) { m -> m.figures.tankL?.let { num(it) + " L" } },
        Metric(stringResource(R.string.compare_consumption), Better.LOWER, { it.figures.consumptionL100 }) { m -> m.figures.consumptionL100?.let { num(it) + " L/100 km" } },
        Metric(stringResource(R.string.compare_range), Better.HIGHER, { it.figures.rangeKm }) { m -> m.figures.rangeKm?.let { "≈ " + grouped(it.toInt()) + " km" } },
        Metric(stringResource(R.string.compare_top), Better.HIGHER, { it.figures.topSpeedKmh?.toDouble() }) { m -> m.figures.topSpeedKmh?.let { "$it km/h" } },
        Metric(stringResource(R.string.compare_seat), Better.NONE, { it.figures.seatMm?.toDouble() }) { m -> m.figures.seatMm?.let { "$it mm" } },
        Metric(stringResource(R.string.compare_wheelbase), Better.NONE, { it.figures.wheelbaseMm?.toDouble() }) { m -> m.figures.wheelbaseMm?.let { grouped(it) + " mm" } }
    )
    val air = stringResource(R.string.compare_air)
    val liquid = stringResource(R.string.compare_liquid)
    val carb = stringResource(R.string.compare_carb)
    val inj = stringResource(R.string.compare_inj)
    val electric = stringResource(R.string.type_electric)
    val best = stringResource(R.string.compare_best)

    Panel(modifier = Modifier.padding(top = 10.dp)) {
        Column(Modifier.padding(12.dp)) {
            // Header: one coloured column per model.
            Row {
                Spacer28()
                models.forEachIndexed { i, m ->
                    Column(Modifier.weight(1f).padding(horizontal = 3.dp)) {
                        Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(colors[i]))
                        Text(m.name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
            TextRow(stringResource(R.string.compare_engine), models) { m ->
                if (m.engine.powertrain == "ELECTRIC") electric
                else "${m.engine.cc} cc\n" + listOf(if (m.engine.cooling == "LIQUID") liquid else air, if (m.engine.fuelSystem == "INJECTION") inj else carb).joinToString(", ")
            }
            metrics.forEach { metric ->
                val values = models.map(metric.value)
                if (values.all { it == null }) return@forEach
                val max = values.filterNotNull().maxOrNull() ?: 0.0
                val bestValue = when (metric.better) {
                    Better.HIGHER -> values.filterNotNull().maxOrNull()
                    Better.LOWER -> values.filterNotNull().minOrNull()
                    Better.NONE -> null
                }?.takeIf { values.count { v -> v != null } > 1 }
                HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant)
                Row(verticalAlignment = Alignment.Top) {
                    Label(metric.label)
                    models.forEachIndexed { i, m ->
                        val v = values[i]
                        val isBest = v != null && v == bestValue
                        Column(Modifier.weight(1f).padding(horizontal = 3.dp)) {
                            Text(
                                (metric.text(m) ?: "—") + if (isBest) " ★" else "",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isBest) FontWeight.Bold else FontWeight.Normal
                            )
                            if (v != null && max > 0) Box(
                                Modifier.padding(top = 3.dp).fillMaxWidth((v / max).toFloat().coerceIn(0.04f, 1f)).height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)).background(colors[i].copy(alpha = if (isBest || bestValue == null) 1f else 0.45f))
                            )
                        }
                    }
                }
            }
            TextRow(stringResource(R.string.compare_tyres), models) { m ->
                listOfNotNull(m.spec("tyreFront"), m.spec("tyreRear")).joinToString("\n").ifBlank { null }
            }
            TextRow(stringResource(R.string.compare_brakes), models) { m ->
                listOfNotNull(m.spec("brakeFront"), m.spec("brakeRear")).joinToString("\n").ifBlank { null }
            }
            Text("★ $best", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun TextRow(label: String, models: List<ScooterModel>, text: (ScooterModel) -> String?) {
    val values = models.map(text)
    if (values.all { it == null }) return
    HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant)
    Row(verticalAlignment = Alignment.Top) {
        Label(label)
        values.forEach { v -> Text(v ?: "—", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f).padding(horizontal = 3.dp)) }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.Label(text: String) =
    Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.9f))

@Composable
private fun androidx.compose.foundation.layout.RowScope.Spacer28() = Box(Modifier.weight(0.9f))

/** "9.6", "138", "2.2": one decimal only when it says something. Western digits everywhere. */
private fun num(v: Double): String = if (v == Math.floor(v)) "%.0f".format(Locale.US, v) else "%.1f".format(Locale.US, v)

private fun grouped(v: Int): String = "%,d".format(Locale.US, v).replace(',', ' ')

/** Every catalogue model by brand; up to three can be ticked. */
@Composable
private fun ModelPicker(models: List<ScooterModel>, selected: List<String>, onDone: (List<String>) -> Unit, onDismiss: () -> Unit) {
    var picked by remember { mutableStateOf(selected) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.compare_pick_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                models.groupBy { it.brand }.forEach { (brand, list) ->
                    SectionTitle(brand)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        list.forEach { m ->
                            val on = m.id in picked
                            FilterChip(
                                selected = on,
                                enabled = on || picked.size < 3,
                                onClick = { picked = if (on) picked - m.id else picked + m.id },
                                label = { Text(m.model) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onDone(picked) }) { Text(stringResource(R.string.action_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}
