package io.github.achmalearning.scootercompanion.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.achmalearning.scootercompanion.R
import io.github.achmalearning.scootercompanion.ui.common.appViewModel
import io.github.achmalearning.scootercompanion.ui.components.BackTopBar
import io.github.achmalearning.scootercompanion.ui.components.ScreenPadding
import io.github.achmalearning.scootercompanion.ui.components.SectionTitle
import io.github.achmalearning.scootercompanion.ui.screens.garage.GarageViewModel

/** "My scooter": every profile field, the same sections as the first-launch wizard. */
@Composable
fun BikeProfileScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val vm = appViewModel { GarageViewModel(context, it) }
    val s = vm.state.collectAsStateWithLifecycle().value ?: return
    val draft = remember(s.profile.id) { ProfileDraft(s.profile) }
    val region = remember(s.profile.region) { vm.region(s.profile.region) }

    Scaffold(topBar = { BackTopBar(stringResource(R.string.my_scooter), onBack) }) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = ScreenPadding, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { SectionTitle(stringResource(R.string.onb_step_type)); ScooterTypeSection(draft, region) }
            item { SectionTitle(stringResource(R.string.onb_step_purchase)); PurchaseSection(draft, region) }
            item { SectionTitle(stringResource(R.string.onb_step_details)); DetailsSection(draft, region) }
            item {
                Button(
                    onClick = { vm.saveProfile(draft.toEntity()); onBack() },
                    enabled = draft.typeValid,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) { Text(stringResource(R.string.action_save)) }
            }
        }
    }
}
