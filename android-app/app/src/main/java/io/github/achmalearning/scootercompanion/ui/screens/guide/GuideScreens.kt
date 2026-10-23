package io.github.achmalearning.scootercompanion.ui.screens.guide

import io.github.achmalearning.scootercompanion.domain.MaintenancePlan
import io.github.achmalearning.scootercompanion.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChecklistRtl
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.OilBarrel
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SportsMotorsports
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.achmalearning.scootercompanion.data.content.Content
import io.github.achmalearning.scootercompanion.domain.Dates
import io.github.achmalearning.scootercompanion.ui.common.appViewModel
import io.github.achmalearning.scootercompanion.ui.components.BackTopBar
import io.github.achmalearning.scootercompanion.ui.components.Callout
import io.github.achmalearning.scootercompanion.ui.components.HSpace
import io.github.achmalearning.scootercompanion.ui.components.InfoRow
import io.github.achmalearning.scootercompanion.ui.components.NavRow
import io.github.achmalearning.scootercompanion.ui.components.Panel
import io.github.achmalearning.scootercompanion.ui.components.ScreenPadding
import io.github.achmalearning.scootercompanion.ui.components.SectionTitle
import io.github.achmalearning.scootercompanion.ui.components.StatusBar
import io.github.achmalearning.scootercompanion.ui.components.StatusPill
import io.github.achmalearning.scootercompanion.ui.components.VSpace
import io.github.achmalearning.scootercompanion.ui.components.openUrl
import io.github.achmalearning.scootercompanion.ui.screens.garage.GarageViewModel
import io.github.achmalearning.scootercompanion.ui.theme.LocalStatusColors

@Composable
private fun content(): Content {
    val context = LocalContext.current
    return remember { io.github.achmalearning.scootercompanion.AppContainer.getInstance(context).contentRepository.content }
}

data class GuideNav(
    val checklist: () -> Unit,
    val troubleshoot: () -> Unit,
    val issues: () -> Unit,
    val howTo: () -> Unit,
    val specs: () -> Unit,
    val oils: () -> Unit,
    val seasons: () -> Unit,
    val parts: () -> Unit,
    val gear: () -> Unit
)

/** "Guide" tab — the wiki, offline. */
@Composable
fun GuideHubScreen(nav: GuideNav) {
    val c = content()
    LazyColumn(contentPadding = ScreenPadding) {
        item {
            Text("Owner's guide", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            Text(c.bike.tagline, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            SectionTitle("Ride")
        }
        item { NavRow(Icons.Filled.ChecklistRtl, "Pre-ride checklist", "${c.checklist.size} checks · every ride & weekly", onClick = nav.checklist) }
        item { NavRow(Icons.Filled.Psychology, "Troubleshooting", "Won't start, power loss, noises, overheating", onClick = nav.troubleshoot) }
        item { NavRow(Icons.Filled.WbSunny, "Seasons & storage", "Summer heat, winter starts, Ramadan storage", onClick = nav.seasons) }
        item { SectionTitle("Maintain") }
        item { NavRow(Icons.Filled.Construction, "How-to guides", "${c.guides.size} step-by-step procedures", onClick = nav.howTo) }
        item { NavRow(Icons.Filled.Warning, "Known failure modes", "What breaks first in Marrakech, ranked", tint = LocalStatusColors.current.due, onClick = nav.issues) }
        item { NavRow(Icons.Filled.OilBarrel, "Oils", "Two oil systems, grades by season", onClick = nav.oils) }
        item { SectionTitle("Reference") }
        item { NavRow(Icons.Filled.Speed, "Specifications & torque", "Factory figures from the manual p.40", onClick = nav.specs) }
        item { NavRow(Icons.Filled.ShoppingCart, "Parts, prices & resale", "MAD prices, where to buy, when to sell", onClick = nav.parts) }
        item { NavRow(Icons.Filled.SportsMotorsports, "Gear & accessories", "Helmet, visibility, lock, top case", onClick = nav.gear) }
    }
}

@Composable
fun ChecklistScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val vm = appViewModel { GarageViewModel(context, it) }
    val s = vm.state.collectAsStateWithLifecycle().value
    val c = vm.content
    val checked = remember { mutableStateListOf<String>() }
    var weekly by rememberSaveable { mutableStateOf(false) }
    val agility = s?.isAgility == true
    val all = s?.let { MaintenancePlan.checklistFor(c, it.spec, agility) } ?: c.checklist
    val items = all.filter { it.group == if (weekly) "weekly" else "ride" }
    val done = items.count { it.id in checked }

    Scaffold(topBar = { BackTopBar(stringResource(R.string.check_title), onBack) }) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = ScreenPadding) {
            item {
                TabRow(selectedTabIndex = if (weekly) 1 else 0) {
                    Tab(selected = !weekly, onClick = { weekly = false }, text = { Text(stringResource(R.string.check_every_ride)) })
                    Tab(selected = weekly, onClick = { weekly = true }, text = { Text(stringResource(R.string.check_weekly)) })
                }
                VSpace(10.dp)
                StatusBar(done.toFloat() / items.size.coerceAtLeast(1), LocalStatusColors.current.ok)
                Text(
                    "$done / ${items.size}" + (s?.lastPreRide?.let { " · " + stringResource(R.string.check_last, Dates.display(it)) } ?: ""),
                    style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp)
                )
            }
            items(items, key = { it.id }) { item ->
                val on = item.id in checked
                Panel(onClick = { if (on) checked.remove(item.id) else checked.add(item.id) }, modifier = Modifier.padding(vertical = 4.dp)) {
                    Row(Modifier.padding(horizontal = 6.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(on, { if (on) checked.remove(item.id) else checked.add(item.id) })
                        Column(Modifier.weight(1f)) {
                            Text(item.localTitle(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(item.localDetail(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            item {
                VSpace(12.dp)
                Button(
                    enabled = done == items.size,
                    onClick = { vm.markPreRideDone(); if (weekly) listOf("tyre-pressure", "tyre-condition").forEach(vm::markTaskDone); onBack() },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(if (done == items.size) R.string.check_all_good else R.string.check_tick_all)) }
                Callout(stringResource(R.string.check_dont_ride), stringResource(R.string.check_dont_ride_text), LocalStatusColors.current.due)
                if (agility) SectionTitle(stringResource(R.string.check_local))
            }
            if (agility) items(c.localTips) { Callout(it.title, it.text, LocalStatusColors.current.soon) }
        }
    }
}

private fun troubleIcon(icon: String): ImageVector = when (icon) {
    "key" -> Icons.Filled.Key
    "speed" -> Icons.Filled.Speed
    "noise" -> Icons.Filled.Hearing
    "heat" -> Icons.Filled.LocalFireDepartment
    else -> Icons.Filled.Psychology
}

@Composable
fun TroubleshootScreen(onBack: () -> Unit, openTree: (String) -> Unit) {
    val c = content()
    Scaffold(topBar = { BackTopBar("Troubleshooting", onBack) }) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = ScreenPadding) {
            item { Text("Answer a few questions to find the likely cause.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 8.dp)) }
            items(c.troubleshooting) { t -> NavRow(troubleIcon(t.icon), t.title, t.nodes[t.start]?.q) { openTree(t.id) } }
        }
    }
}

@Composable
fun TroubleTreeScreen(treeId: String, onBack: () -> Unit, openGuide: (String) -> Unit) {
    val c = content()
    val tree = c.troubleshooting.first { it.id == treeId }
    val path = remember { mutableStateListOf(tree.start) }
    val answers = remember { mutableStateListOf<String>() }
    val node = tree.nodes.getValue(path.last())
    val colors = LocalStatusColors.current

    Scaffold(topBar = { BackTopBar(tree.title, onBack) }) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = ScreenPadding) {
            itemsIndexed(answers) { i, answer ->
                Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CheckCircle, null, tint = colors.ok, modifier = Modifier.size(18.dp))
                    HSpace(8.dp)
                    Column {
                        Text(tree.nodes[path[i]]?.q ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(answer, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }
                }
            }
            item { VSpace(8.dp) }
            if (node.q != null) {
                item {
                    Panel {
                        Column(Modifier.padding(16.dp)) {
                            Text("Step ${path.size}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Text(node.q, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                            VSpace(12.dp)
                            node.options.forEach { (label, next) ->
                                OutlinedButton(onClick = { answers.add(label); path.add(next) }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Text(label) }
                            }
                        }
                    }
                }
            } else {
                item {
                    val color = when (node.level) { "danger" -> colors.due; "warn" -> colors.soon; else -> colors.ok }
                    Surface(color = color.copy(alpha = 0.12f), shape = MaterialTheme.shapes.large) {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            Text("Likely cause", style = MaterialTheme.typography.labelMedium, color = color)
                            Text(node.result ?: "", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            VSpace(8.dp)
                            Text(node.text ?: "", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    node.guide?.let { g -> c.guide(g)?.let { NavRow(Icons.AutoMirrored.Filled.MenuBook, "Guide: ${it.title}", it.interval) { openGuide(g) } } }
                }
            }
            if (path.size > 1) item {
                VSpace(12.dp)
                OutlinedButton(onClick = { path.clear(); path.add(tree.start); answers.clear() }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Refresh, null); Text("  Start over")
                }
            }
        }
    }
}

@Composable
fun FailureModesScreen(onBack: () -> Unit) {
    val c = content()
    val colors = LocalStatusColors.current
    Scaffold(topBar = { BackTopBar("Known failure modes", onBack) }) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = ScreenPadding) {
            item { Text("Ranked by risk under Marrakech heat, dust and stop-and-go traffic.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 8.dp)) }
            items(c.failureModes) { f ->
                val color = when { f.risk >= 60 -> colors.due; f.risk >= 40 -> colors.soon; else -> colors.ok }
                Panel(Modifier.padding(vertical = 5.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${f.rank}. ${f.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            StatusPill("${f.risk}%", color)
                        }
                        VSpace(6.dp)
                        StatusBar(f.risk / 100f, color)
                        VSpace(8.dp)
                        Labeled("How it fails", f.how)
                        Labeled("Detect", f.detect)
                        Labeled("Prevent", f.prevent)
                    }
                }
            }
        }
    }
}

@Composable
private fun Labeled(label: String, text: String) {
    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp))
    Text(text, style = MaterialTheme.typography.bodyMedium)
}

@Composable
fun GuidesListScreen(onBack: () -> Unit, openGuide: (String) -> Unit) {
    val c = content()
    Scaffold(topBar = { BackTopBar("How-to guides", onBack) }) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = ScreenPadding) {
            item { Text("Always work on a cold engine unless a guide says otherwise.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 8.dp)) }
            items(c.guides) { g -> NavRow(Icons.Filled.Build, g.title, "${g.interval} · ${g.time} · ${g.difficulty}") { openGuide(g.id) } }
        }
    }
}

@Composable
fun GuideDetailScreen(guideId: String, onBack: () -> Unit) {
    val c = content()
    val g = c.guide(guideId) ?: return
    val done = remember { mutableStateListOf<Int>() }
    val colors = LocalStatusColors.current
    Scaffold(topBar = { BackTopBar(g.title, onBack) }) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = ScreenPadding) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                    item { StatusPill(g.interval, MaterialTheme.colorScheme.primary) }
                    item { StatusPill(g.time, colors.muted) }
                    item { StatusPill(g.difficulty, colors.ok) }
                }
                if (g.need.isNotEmpty()) {
                    SectionTitle("You need")
                    g.need.forEach { Text("•  $it", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 2.dp)) }
                }
                SectionTitle("Steps")
            }
            itemsIndexed(g.steps) { i, (title, text) ->
                val on = i in done
                Row(
                    Modifier.fillMaxWidth().clickable { if (on) done.remove(i) else done.add(i) }.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Surface(shape = CircleShape, color = if (on) colors.ok else MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Text(if (on) "✓" else "${i + 1}", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    HSpace(12.dp)
                    Column(Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            items(g.tips) { (kind, title, text) -> Callout(title, text, if (kind == "warn") colors.due else colors.soon) }
        }
    }
}

@Composable
fun SpecsScreen(onBack: () -> Unit) {
    val c = content()
    var tab by rememberSaveable { mutableStateOf(0) }
    Scaffold(topBar = { BackTopBar("Specifications", onBack) }) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = ScreenPadding) {
            item {
                TabRow(selectedTabIndex = tab) {
                    Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Specs") })
                    Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Torque") })
                }
            }
            if (tab == 0) {
                items(c.specs) { g ->
                    SectionTitle(g.group)
                    Panel { Column(Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) { g.rows.forEach { InfoRow(it[0], it[1]) } } }
                }
            } else {
                item { Text("N·m. Replace crush washers and U-nuts every time.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 10.dp)) }
                items(c.torque) { (name, nm, note) ->
                    Panel(Modifier.padding(vertical = 3.dp)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                if (note.isNotBlank()) Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("$nm N·m", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OilsScreen(onBack: () -> Unit) {
    val c = content()
    val colors = LocalStatusColors.current
    Scaffold(topBar = { BackTopBar("Oils", onBack) }) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = ScreenPadding) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    OilCard("Engine oil", "10W-40 → 10W-60", "700 ml · JASO MB\nDrain: right, 17 mm\nEvery 2,000 km", Modifier.weight(1f))
                    OilCard("Gear oil", "80W-90 GL-4", "120-150 ml\nDrain: left rear, 12 mm\nEvery 4,000 km", Modifier.weight(1f))
                }
                Callout("Never mix them", "Gear oil in the engine starves the cam and valves; engine oil in the gearbox lacks EP additives and pits the gears. Label your bottles ENGINE and GEARS.", colors.due)
                SectionTitle("What to pour, when")
            }
            items(c.oilStrategy) { o ->
                Panel(Modifier.padding(vertical = 4.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Text(o.period, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text(o.oil, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text("${o.price} · change every ${o.interval}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item { Callout("JASO MB, not MA/MA2", "The Agility 50 4T has a dry centrifugal clutch — use scooter-rated JASO MB oil.", colors.soon) }
        }
    }
}

@Composable
private fun OilCard(title: String, grade: String, body: String, modifier: Modifier) {
    Panel(modifier) {
        Column(Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(grade, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            VSpace(4.dp)
            Text(body, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun SeasonsScreen(onBack: () -> Unit) {
    val c = content()
    val month = Dates.month(Dates.today())
    var selected by rememberSaveable { mutableStateOf(c.seasonForMonth(month)?.id ?: c.seasons.first().id) }
    val season = c.seasons.first { it.id == selected }
    Scaffold(topBar = { BackTopBar("Seasons & storage", onBack) }) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = ScreenPadding) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                    items(c.seasons) { s ->
                        FilterChip(
                            selected = s.id == selected, onClick = { selected = s.id },
                            label = { Text(s.name + if (month in s.months) " · now" else "") },
                            leadingIcon = { Icon(if (s.id == "winter") Icons.Filled.AcUnit else Icons.Filled.WbSunny, null) }
                        )
                    }
                }
                Text(season.range, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(season.headline, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                VSpace(8.dp)
            }
            items(season.rows) { (area, advice) ->
                Panel(Modifier.padding(vertical = 3.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(area, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(advice, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            item { SectionTitle("Parked > 2 weeks (Ramadan, holidays)") }
            itemsIndexed(c.storage) { i, step -> Text("${i + 1}.  $step", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 3.dp)) }
        }
    }
}

@Composable
fun PartsScreen(onBack: () -> Unit) {
    val c = content()
    val context = LocalContext.current
    Scaffold(topBar = { BackTopBar("Parts, prices & resale", onBack) }) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = ScreenPadding) {
            item { Text("Indicative Marrakech prices (MAD), early 2026. Negotiate; bring the old part.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 8.dp)) }
            items(c.parts) { (name, spec, price, where) ->
                Panel(Modifier.padding(vertical = 3.dp)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text("$spec · $where", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(price, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
            item { SectionTitle("Fixed yearly costs") }
            items(c.fixedCosts) { (name, price, note) -> Callout("$name — $price", note, MaterialTheme.colorScheme.secondary) }
            item { SectionTitle("Workshops") }
            items(c.shops) { shop -> NavRow(Icons.Filled.SettingsSuggest, shop.name, "${shop.role}. ${shop.note}") { context.openUrl(shop.maps) } }
            item {
                SectionTitle("Resale")
                Callout(c.resale.headline, c.resale.text)
                SectionTitle("Links")
            }
            items(c.links) { l -> NavRow(Icons.AutoMirrored.Filled.MenuBook, l.title, l.url.removePrefix("https://").take(48)) { context.openUrl(l.url) } }
        }
    }
}

@Composable
fun GearScreen(onBack: () -> Unit) {
    val c = content()
    Scaffold(topBar = { BackTopBar("Gear & accessories", onBack) }) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = ScreenPadding) {
            item { Callout("Priority #1: be seen", "On a 50 cc in faster traffic, most crashes are a driver who didn't see you. Visibility spending pays back more than any comfort accessory.") }
            items(c.gear) { tier ->
                SectionTitle(tier.tier)
                Panel {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                        tier.items.forEach { (name, why) ->
                            Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
                            Text(why, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp))
                        }
                    }
                }
            }
        }
    }
}
