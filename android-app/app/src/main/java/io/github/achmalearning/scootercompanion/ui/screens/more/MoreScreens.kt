package io.github.achmalearning.scootercompanion.ui.screens.more

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sos
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.achmalearning.scootercompanion.domain.Dates
import io.github.achmalearning.scootercompanion.ui.common.appViewModel
import io.github.achmalearning.scootercompanion.ui.components.BackTopBar
import io.github.achmalearning.scootercompanion.ui.components.Callout
import io.github.achmalearning.scootercompanion.ui.components.IconBadge
import io.github.achmalearning.scootercompanion.ui.components.NavRow
import io.github.achmalearning.scootercompanion.ui.components.Panel
import io.github.achmalearning.scootercompanion.ui.components.ScreenPadding
import io.github.achmalearning.scootercompanion.ui.components.SectionTitle
import io.github.achmalearning.scootercompanion.ui.components.dial
import io.github.achmalearning.scootercompanion.ui.components.openUrl
import io.github.achmalearning.scootercompanion.ui.screens.garage.GarageViewModel
import io.github.achmalearning.scootercompanion.ui.theme.LocalStatusColors

data class MoreNav(
    val passport: () -> Unit,
    val fuel: () -> Unit,
    val wallet: () -> Unit,
    val sos: () -> Unit,
    val profile: () -> Unit,
    val settings: () -> Unit
)

@Composable
fun MoreScreen(nav: MoreNav) {
    val context = LocalContext.current
    val vm = appViewModel { GarageViewModel(context, it) }
    LazyColumn(contentPadding = ScreenPadding) {
        item {
            Text("More", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            SectionTitle("Ownership")
        }
        item { NavRow(Icons.Filled.Verified, "Digital passport", "Service stamps & warranty", onClick = nav.passport) }
        item { NavRow(Icons.Filled.LocalGasStation, "Fuel log", "Consumption & running costs", onClick = nav.fuel) }
        item { NavRow(Icons.Filled.Badge, "Documents wallet", "Insurance, carte grise, licence — expiry reminders", onClick = nav.wallet) }
        item { NavRow(Icons.Filled.Edit, "My scooter", "Plate, VIN, purchase date, dealer", onClick = nav.profile) }
        item { SectionTitle("Help") }
        item { NavRow(Icons.Filled.Sos, "SOS & accident", "Emergency numbers, dealers, what to do", tint = LocalStatusColors.current.due, onClick = nav.sos) }
        item { NavRow(Icons.Filled.Public, "Marrakech maintenance wiki", vm.content.bike.wiki) { context.openUrl(vm.content.bike.wiki) } }
        item { SectionTitle("App") }
        item { NavRow(Icons.Filled.Settings, "Settings & backup", "Reminders, backup, app updates", onClick = nav.settings) }
        item {
            Text(
                "Knowledge base updated ${vm.content.updated}. Not affiliated with KYMCO.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@Composable
fun SosScreen(onBack: () -> Unit, openWallet: () -> Unit) {
    val context = LocalContext.current
    val vm = appViewModel { GarageViewModel(context, it) }
    val s = vm.state.collectAsStateWithLifecycle().value
    val c = vm.content
    val red = LocalStatusColors.current.due
    Scaffold(topBar = { BackTopBar("SOS & accident", onBack) }) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = ScreenPadding) {
            item { SectionTitle("Emergency numbers (Morocco)") }
            items(c.emergency) { e ->
                Panel(onClick = { context.dial(e.number) }, modifier = Modifier.padding(vertical = 4.dp)) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Filled.Call, red)
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(e.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            if (e.note.isNotBlank()) Text(e.note, style = MaterialTheme.typography.bodySmall)
                        }
                        Text(e.number, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = red)
                    }
                }
            }
            item { SectionTitle("After an accident") }
            itemsIndexed(c.accidentSteps) { i, step ->
                Text("${i + 1}.  $step", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 4.dp))
            }
            val insurance = s?.documents?.firstOrNull { it.type == "insurance" }
            item {
                Callout(
                    "Insurance: declare within 5 days",
                    insurance?.let { "${it.title}" + (if (it.number.isNotBlank()) " · n° ${it.number}" else "") + (it.expiry?.let { e -> " · valid until ${Dates.display(e)}" } ?: "") }
                        ?: "Add your insurance certificate to the wallet so its number is here when you need it.",
                    red
                )
                FilledTonalButton(onClick = openWallet, modifier = Modifier.fillMaxWidth()) { Text("Open documents wallet") }
            }
            item { SectionTitle("Approved dealers") }
            items(c.shops) { shop -> NavRow(Icons.Filled.Map, shop.name, "${shop.role} — open in Maps") { context.openUrl(shop.maps) } }
        }
    }
}
