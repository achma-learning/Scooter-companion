package io.github.achmalearning.scootercompanion.ui.screens.profile

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.achmalearning.scootercompanion.AppContainer
import io.github.achmalearning.scootercompanion.R
import io.github.achmalearning.scootercompanion.data.db.BikeProfileEntity
import io.github.achmalearning.scootercompanion.data.region.RegionPack
import io.github.achmalearning.scootercompanion.data.region.UsedMarket
import io.github.achmalearning.scootercompanion.domain.Cooling
import io.github.achmalearning.scootercompanion.domain.EngineClass
import io.github.achmalearning.scootercompanion.domain.ExpenseLedger
import io.github.achmalearning.scootercompanion.domain.FuelSystem
import io.github.achmalearning.scootercompanion.domain.Powertrain
import io.github.achmalearning.scootercompanion.domain.enumOr
import io.github.achmalearning.scootercompanion.ui.components.DateField
import io.github.achmalearning.scootercompanion.ui.components.NumberField
import io.github.achmalearning.scootercompanion.ui.components.ScooterArt
import io.github.achmalearning.scootercompanion.ui.components.openUrl
import java.math.BigDecimal

/**
 * The editable copy of the scooter profile, shared by the first-launch wizard and the
 * "My scooter" screen. [toEntity] copies onto the stored row, so a field this form doesn't
 * show is never wiped.
 */
class ProfileDraft(private val base: BikeProfileEntity) {
    var powertrain by mutableStateOf(enumOr(base.powertrain, Powertrain.PETROL))
    var brand by mutableStateOf(base.brand)
    var model by mutableStateOf(base.model)
    var cc by mutableStateOf(if (base.engineCc > 0) base.engineCc.toString() else "")
    var fuelSystem by mutableStateOf(enumOr(base.fuelSystem, FuelSystem.CARBURETOR))
    var cooling by mutableStateOf(enumOr(base.cooling, Cooling.AIR))
    var modelYear by mutableStateOf(base.modelYear?.toString().orEmpty())

    var boughtUsed by mutableStateOf(base.boughtUsed)
    var purchaseDate by mutableStateOf(base.purchaseDate)
    var purchasePrice by mutableStateOf(base.purchasePrice.text())
    var purchaseKm by mutableStateOf(base.purchaseKm.toString())
    var newPrice by mutableStateOf(base.newPrice.text())
    var firstRegistration by mutableStateOf(base.firstRegistration)
    var previousOwners by mutableStateOf(base.previousOwners?.toString().orEmpty())
    var warranty by mutableStateOf(base.warrantyMonths.toString())

    var photo by mutableStateOf(base.photo)
    var nickname by mutableStateOf(base.nickname)
    var plate by mutableStateOf(base.plate)
    var vin by mutableStateOf(base.vin)
    var engineNo by mutableStateOf(base.engineNo)
    var color by mutableStateOf(base.color)
    var dealer by mutableStateOf(base.dealer)
    var monthlyBudget by mutableStateOf(base.monthlyBudget.text())

    val electric: Boolean get() = powertrain == Powertrain.ELECTRIC
    val ccValue: Int? get() = cc.toIntOrNull()?.takeIf { it in 1..3000 }
    val engineClass: EngineClass get() = EngineClass.of(powertrain, ccValue ?: 0)

    /** The wizard's first step needs brand, model and (for petrol) the engine size. */
    val typeValid: Boolean get() = brand.isNotBlank() && model.isNotBlank() && (electric || ccValue != null)

    /** New and used scooters have different usual warranties; follow the switch unless edited. */
    fun setUsed(used: Boolean) {
        if (warranty == (if (boughtUsed) "0" else "24")) warranty = if (used) "0" else "24"
        boughtUsed = used
    }

    fun toEntity(): BikeProfileEntity = base.copy(
        id = 1,
        powertrain = powertrain.name,
        brand = brand.trim(),
        model = model.trim(),
        engineCc = if (electric) 0 else ccValue ?: 0,
        fuelSystem = fuelSystem.name,
        cooling = cooling.name,
        modelYear = modelYear.toIntOrNull(),
        boughtUsed = boughtUsed,
        purchaseDate = purchaseDate,
        purchasePrice = purchasePrice.toDoubleOrNull(),
        purchaseKm = purchaseKm.toIntOrNull() ?: 0,
        newPrice = if (boughtUsed) newPrice.toDoubleOrNull() else null,
        firstRegistration = if (boughtUsed) firstRegistration else null,
        previousOwners = if (boughtUsed) previousOwners.toIntOrNull() else null,
        warrantyMonths = warranty.toIntOrNull() ?: 0,
        photo = photo,
        nickname = nickname.trim(),
        plate = plate.trim(),
        vin = vin.trim(),
        engineNo = engineNo.trim(),
        color = color.trim(),
        dealer = dealer.trim(),
        monthlyBudget = monthlyBudget.toDoubleOrNull()?.takeIf { it > 0 }
    )
}

private fun Double?.text(): String = this?.let { BigDecimal(it).stripTrailingZeros().toPlainString() }.orEmpty()

@Composable
fun engineClassLabel(c: EngineClass): String = stringResource(
    when (c) {
        EngineClass.ELECTRIC -> R.string.class_ELECTRIC
        EngineClass.CC50 -> R.string.class_CC50
        EngineClass.CC125 -> R.string.class_CC125
        EngineClass.MID -> R.string.class_MID
        EngineClass.MAXI -> R.string.class_MAXI
    }
)

/** A row of mutually exclusive chips; the chosen one shows a tick. */
@Composable
private fun <T> ChoiceChips(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) ->
            FilterChip(
                selected = value == selected,
                onClick = { onSelect(value) },
                label = { Text(label) },
                leadingIcon = if (value == selected) ({ Icon(Icons.Filled.Check, null) }) else null
            )
        }
    }
}

@Composable
private fun FieldLabel(text: String) =
    Text(text, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))

@Composable
private fun Help(text: String) =
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

@Composable
fun ScooterTypeSection(d: ProfileDraft, region: RegionPack) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ChoiceChips(
            listOf(Powertrain.PETROL to stringResource(R.string.type_petrol), Powertrain.ELECTRIC to stringResource(R.string.type_electric)),
            d.powertrain
        ) { d.powertrain = it }

        OutlinedTextField(d.brand, { d.brand = it }, label = { Text(stringResource(R.string.brand)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            region.brands.forEach { b -> FilterChip(selected = d.brand.equals(b, ignoreCase = true), onClick = { d.brand = b }, label = { Text(b) }) }
        }
        OutlinedTextField(
            d.model, { d.model = it }, label = { Text(stringResource(R.string.model)) },
            placeholder = { Text(stringResource(R.string.model_placeholder)) }, singleLine = true, modifier = Modifier.fillMaxWidth()
        )

        if (!d.electric) {
            NumberField(d.cc, { d.cc = it.take(4) }, stringResource(R.string.engine_cc), suffix = stringResource(R.string.unit_cc))
            if (d.ccValue != null) Help(engineClassLabel(d.engineClass))

            FieldLabel(stringResource(R.string.fuel_system))
            ChoiceChips(
                listOf(FuelSystem.CARBURETOR to stringResource(R.string.fuel_CARBURETOR), FuelSystem.INJECTION to stringResource(R.string.fuel_INJECTION)),
                d.fuelSystem
            ) { d.fuelSystem = it }

            FieldLabel(stringResource(R.string.cooling))
            ChoiceChips(
                listOf(Cooling.AIR to stringResource(R.string.cooling_AIR), Cooling.LIQUID to stringResource(R.string.cooling_LIQUID)),
                d.cooling
            ) { d.cooling = it }
        }
        NumberField(d.modelYear, { d.modelYear = it.take(4) }, stringResource(R.string.model_year))
    }
}

@Composable
fun PurchaseSection(d: ProfileDraft, region: RegionPack) {
    val money = region.currencySymbol.ifBlank { null }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FieldLabel(stringResource(R.string.condition))
        ChoiceChips(listOf(false to stringResource(R.string.bought_new), true to stringResource(R.string.bought_used)), d.boughtUsed) { d.setUsed(it) }

        DateField(d.purchaseDate, { d.purchaseDate = it }, stringResource(R.string.purchase_date), clearable = true)
        NumberField(d.purchasePrice, { d.purchasePrice = it }, stringResource(R.string.price_paid), decimal = true, suffix = money)
        Help(stringResource(R.string.price_paid_help))
        NumberField(
            d.purchaseKm, { d.purchaseKm = it },
            stringResource(if (d.boughtUsed) R.string.km_at_purchase_used else R.string.km_at_purchase_new), suffix = "km"
        )
        Help(stringResource(R.string.km_help))

        if (d.boughtUsed) {
            NumberField(d.newPrice, { d.newPrice = it }, stringResource(R.string.new_price), decimal = true, suffix = money)
            Help(stringResource(R.string.new_price_help))
            ExpenseLedger.usedSaving(d.purchasePrice.toDoubleOrNull(), d.newPrice.toDoubleOrNull())?.let { (saved, fraction) ->
                if (saved > 0) Text(
                    stringResource(R.string.used_saving, "%,.0f %s".format(saved, money.orEmpty()).trim(), (fraction * 100).toInt()),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary
                )
            }
            DateField(d.firstRegistration, { d.firstRegistration = it }, stringResource(R.string.first_registration), clearable = true)
            NumberField(d.previousOwners, { d.previousOwners = it.take(2) }, stringResource(R.string.previous_owners))
            UsedMarkets(region.usedMarkets)
        }
        NumberField(d.warranty, { d.warranty = it.take(3) }, stringResource(R.string.warranty), suffix = stringResource(R.string.unit_months))
    }
}

/** Links to used-scooter marketplaces, opened in the browser. */
@Composable
fun UsedMarkets(markets: List<UsedMarket>) {
    if (markets.isEmpty()) return
    val context = LocalContext.current
    FieldLabel(stringResource(R.string.used_markets))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        markets.forEach { m ->
            AssistChip(
                onClick = { context.openUrl(m.url) },
                label = { Text(m.name) },
                trailingIcon = { Icon(Icons.AutoMirrored.Filled.OpenInNew, null) }
            )
        }
    }
}

/** The scooter's photo, or the drawn logo when there is none. */
@Composable
fun ScooterPicture(photo: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val photos = remember { AppContainer.getInstance(context).garageRepository.photos }
    val bitmap = remember(photo) {
        photos.path()?.takeIf { photo.isNotBlank() }?.let { BitmapFactory.decodeFile(it.absolutePath)?.asImageBitmap() }
    }
    if (bitmap != null) {
        Image(bitmap, contentDescription = null, contentScale = ContentScale.Crop, modifier = modifier.clip(RoundedCornerShape(16.dp)))
    } else {
        ScooterArt(modifier)
    }
}

@Composable
fun DetailsSection(d: ProfileDraft, region: RegionPack) {
    val context = LocalContext.current
    val photos = remember { AppContainer.getInstance(context).garageRepository.photos }
    var photoVersion by remember { mutableStateOf(0) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) runCatching { photos.saveFrom(uri) }.onSuccess { d.photo = it; photoVersion++ }
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // Re-read the picture after each pick (the file name stays the same).
        androidx.compose.runtime.key(photoVersion) {
            ScooterPicture(d.photo, Modifier.fillMaxWidth().height(160.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                Icon(Icons.Filled.PhotoCamera, null)
                Text(" " + stringResource(R.string.photo_pick))
            }
            if (d.photo.isNotBlank()) TextButton(onClick = { photos.delete(); d.photo = ""; photoVersion++ }) { Text(stringResource(R.string.photo_remove)) }
        }
        OutlinedTextField(d.nickname, { d.nickname = it }, label = { Text(stringResource(R.string.nickname)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(d.plate, { d.plate = it }, label = { Text(stringResource(R.string.plate)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(d.vin, { d.vin = it.uppercase() }, label = { Text(stringResource(R.string.vin)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(d.engineNo, { d.engineNo = it }, label = { Text(stringResource(R.string.engine_no)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(d.color, { d.color = it }, label = { Text(stringResource(R.string.colour)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(d.dealer, { d.dealer = it }, label = { Text(stringResource(R.string.seller)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        NumberField(d.monthlyBudget, { d.monthlyBudget = it }, stringResource(R.string.monthly_target), decimal = true, suffix = region.currencySymbol.ifBlank { null })
        Help(stringResource(R.string.monthly_target_help))
    }
}
