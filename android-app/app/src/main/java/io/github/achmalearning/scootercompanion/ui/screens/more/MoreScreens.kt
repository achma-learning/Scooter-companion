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
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sos
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.achmalearning.scootercompanion.R
import io.github.achmalearning.scootercompanion.data.region.pick
import io.github.achmalearning.scootercompanion.domain.Dates
import io.github.achmalearning.scootercompanion.domain.InsurancePolicy
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
    val settings: () -> Unit,
    val insurance: () -> Unit = {},
    val expenses: () -> Unit = {},
    val recommendations: () -> Unit = {},
    val stats: () -> Unit = {},
    val guide: () -> Unit = {},
    val manual: () -> Unit = {},
    val model: () -> Unit = {},
    val compare: () -> Unit = {}
)

@Composable
fun MoreScreen(nav: MoreNav) {
    val context = LocalContext.current
    val vm = appViewModel { GarageViewModel(context, it) }
    val s = vm.state.collectAsStateWithLifecycle().value
    val agility = s?.isAgility == true
    LazyColumn(contentPadding = ScreenPadding) {
        item {
            Text(stringResource(R.string.more_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            SectionTitle(stringResource(R.string.more_money))
        }
        item { NavRow(Icons.Filled.BarChart, stringResource(R.string.nav_stats), stringResource(R.string.stats_sub), onClick = nav.stats) }
        if (s?.spec?.isElectric != true) item { NavRow(Icons.Filled.LocalGasStation, stringResource(R.string.fuel_title), stringResource(R.string.more_fuel_sub), onClick = nav.fuel) }
        item { NavRow(Icons.Filled.Shield, stringResource(R.string.ins_title), stringResource(R.string.more_insurance_sub), onClick = nav.insurance) }
        item { NavRow(Icons.Filled.Receipt, stringResource(R.string.money_title), stringResource(R.string.more_expenses_sub), onClick = nav.expenses) }
        item { NavRow(Icons.Filled.ThumbUp, stringResource(R.string.rec_title), stringResource(R.string.more_rec_sub), onClick = nav.recommendations) }
        item { SectionTitle(stringResource(R.string.more_ownership)) }
        item { NavRow(Icons.Filled.Edit, stringResource(R.string.my_scooter), stringResource(R.string.more_profile_sub), onClick = nav.profile) }
        item { NavRow(Icons.Filled.Description, stringResource(R.string.model_title), stringResource(R.string.model_sub), onClick = nav.model) }
        item { NavRow(Icons.Filled.CompareArrows, stringResource(R.string.compare_title), stringResource(R.string.compare_sub), onClick = nav.compare) }
        if (agility) item { NavRow(Icons.Filled.MenuBook, stringResource(R.string.nav_manual), stringResource(R.string.more_manual_sub), onClick = nav.manual) }
        if (agility) item { NavRow(Icons.Filled.Verified, stringResource(R.string.more_passport), stringResource(R.string.more_passport_sub), onClick = nav.passport) }
        item { NavRow(Icons.Filled.Badge, stringResource(R.string.more_wallet), stringResource(R.string.more_wallet_sub), onClick = nav.wallet) }
        item { SectionTitle(stringResource(R.string.more_help)) }
        item {
            if (agility) NavRow(Icons.Filled.MenuBook, stringResource(R.string.nav_guide), stringResource(R.string.more_guide_sub), onClick = nav.guide)
            else NavRow(Icons.Filled.MenuBook, stringResource(R.string.check_title), stringResource(R.string.check_every_ride) + " · " + stringResource(R.string.check_weekly), onClick = nav.guide)
        }
        item { NavRow(Icons.Filled.Sos, stringResource(R.string.sos_title), stringResource(R.string.more_sos_sub), tint = LocalStatusColors.current.due, onClick = nav.sos) }
        if (agility) item { NavRow(Icons.Filled.Public, stringResource(R.string.more_wiki), vm.content.bike.wiki) { context.openUrl(vm.content.bike.wiki) } }
        item { SectionTitle(stringResource(R.string.more_app)) }
        item { NavRow(Icons.Filled.Settings, stringResource(R.string.more_settings), stringResource(R.string.more_settings_sub), onClick = nav.settings) }
        item {
            Text(
                stringResource(R.string.more_footer),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@Composable
fun SosScreen(onBack: () -> Unit, openInsurance: () -> Unit) {
    val context = LocalContext.current
    val vm = appViewModel { GarageViewModel(context, it) }
    val s = vm.state.collectAsStateWithLifecycle().value ?: return
    val region = s.region
    val red = LocalStatusColors.current.due
    Scaffold(topBar = { BackTopBar(stringResource(R.string.sos_title), onBack) }) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = ScreenPadding) {
            item { SectionTitle(stringResource(R.string.sos_numbers, region.name.pick())) }
            items(region.sos) { e ->
                Panel(onClick = { context.dial(e.number) }, modifier = Modifier.padding(vertical = 4.dp)) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Filled.Call, red)
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(e.label.pick(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            e.note.pick().takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        }
                        Text(e.number, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = red)
                    }
                }
            }
            if (region.accidentSteps.isNotEmpty()) {
                item { SectionTitle(stringResource(R.string.sos_after)) }
                itemsIndexed(region.accidentSteps) { i, step ->
                    Text("${i + 1}.  ${step.pick()}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 4.dp))
                }
            }
            // The contract that covers today, else the latest one.
            val policy = s.insurance.firstOrNull { it.startDate <= s.today && InsurancePolicy(it.startDate, it.months, it.totalPaid).endDate >= s.today }
                ?: s.insurance.firstOrNull()
            item {
                Callout(
                    stringResource(R.string.sos_insurance),
                    policy?.let { p ->
                        listOf(p.insurer, p.policyNumber.takeIf { it.isNotBlank() }?.let { "n° $it" }.orEmpty(),
                            stringResource(R.string.ins_until, Dates.display(InsurancePolicy(p.startDate, p.months, p.totalPaid).endDate)))
                            .filter { it.isNotBlank() }.joinToString(" · ")
                    } ?: stringResource(R.string.sos_insurance_none),
                    red
                )
                FilledTonalButton(onClick = openInsurance, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.sos_open_insurance)) }
            }
            if (s.isAgility) {
                item { SectionTitle(stringResource(R.string.sos_dealers)) }
                items(vm.content.shops) { shop -> NavRow(Icons.Filled.Map, shop.name, stringResource(R.string.sos_maps, shop.role)) { context.openUrl(shop.maps) } }
            }
        }
    }
}
