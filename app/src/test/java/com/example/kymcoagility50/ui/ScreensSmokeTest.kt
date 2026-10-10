package com.example.kymcoagility50.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.kymcoagility50.AppContainer
import com.example.kymcoagility50.data.db.BikeProfileEntity
import com.example.kymcoagility50.data.db.DocumentEntity
import com.example.kymcoagility50.data.db.FuelEntity
import com.example.kymcoagility50.data.db.MaintenanceEntryEntity
import com.example.kymcoagility50.data.model.PdfSource
import com.example.kymcoagility50.domain.Dates
import com.example.kymcoagility50.ui.screens.garage.BikeProfileScreen
import com.example.kymcoagility50.ui.screens.garage.FuelScreen
import com.example.kymcoagility50.ui.screens.garage.GarageNav
import com.example.kymcoagility50.ui.screens.garage.GarageScreen
import com.example.kymcoagility50.ui.screens.garage.PassportScreen
import com.example.kymcoagility50.ui.screens.garage.WalletScreen
import com.example.kymcoagility50.ui.screens.guide.ChecklistScreen
import com.example.kymcoagility50.ui.screens.guide.FailureModesScreen
import com.example.kymcoagility50.ui.screens.guide.GearScreen
import com.example.kymcoagility50.ui.screens.guide.GuideDetailScreen
import com.example.kymcoagility50.ui.screens.guide.GuideHubScreen
import com.example.kymcoagility50.ui.screens.guide.GuideNav
import com.example.kymcoagility50.ui.screens.guide.GuidesListScreen
import com.example.kymcoagility50.ui.screens.guide.OilsScreen
import com.example.kymcoagility50.ui.screens.guide.PartsScreen
import com.example.kymcoagility50.ui.screens.guide.SeasonsScreen
import com.example.kymcoagility50.ui.screens.guide.SpecsScreen
import com.example.kymcoagility50.ui.screens.guide.TroubleTreeScreen
import com.example.kymcoagility50.ui.screens.guide.TroubleshootScreen
import com.example.kymcoagility50.ui.screens.home.ManualHubScreen
import com.example.kymcoagility50.ui.screens.more.MoreNav
import com.example.kymcoagility50.ui.screens.more.MoreScreen
import com.example.kymcoagility50.ui.screens.more.SosScreen
import com.example.kymcoagility50.ui.screens.service.LogServiceScreen
import com.example.kymcoagility50.ui.screens.service.ServiceScreen
import com.example.kymcoagility50.ui.screens.service.TaskDetailScreen
import com.example.kymcoagility50.ui.screens.settings.SettingsScreen
import com.example.kymcoagility50.ui.theme.KymcoTheme
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Renders every screen with a realistic garage on the JVM — catches runtime crashes (bad
 * lazy-list keys, missing content ids, asset loading) that compilation can't. Pass
 * -PscreenshotsDir=/some/dir to also save a PNG of each screen.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class ScreensSmokeTest {

    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val outDir = System.getProperty("screenshots.dir").orEmpty().takeIf { it.isNotBlank() }?.let { File(it).apply { mkdirs() } }

    private fun seed() = runBlocking {
        val c = AppContainer.getInstance(compose.activity)
        val today = Dates.today()
        val bought = Dates.addMonths(today, -11)
        c.garageRepository.saveProfile(
            BikeProfileEntity(nickname = "Zitoun", plate = "12345-A-40", vin = "RFBKN10EM0000001", purchaseDate = bought, purchaseKm = 0, dealer = "YOUKA Moto")
        )
        fun entry(km: Int, monthsAgo: Int, tasks: String, milestone: Int?, stamped: Boolean, shop: String, cost: Double?) =
            MaintenanceEntryEntity(km = km, date = Dates.addMonths(today, -monthsAgo), note = "", tasks = tasks, milestoneKm = milestone,
                shop = shop, cost = cost, stamped = stamped, updatedAt = 0)
        c.maintenanceRepository.save(entry(310, 10, "engine-oil,gear-oil,oil-screen,brake-pads,bolts", 300, true, "YOUKA Moto", 150.0))
        c.maintenanceRepository.save(entry(1020, 8, "engine-oil,gear-oil,air-filter-clean", 1000, true, "YOUKA Moto", 160.0))
        c.maintenanceRepository.save(entry(2040, 5, "engine-oil,plug-clean,air-filter-replace,battery", 2000, false, "DIY", 180.0))
        c.garageRepository.addOdometer(2890, today)
        c.garageRepository.saveFuel(FuelEntity(date = Dates.addDays(today, -20), km = 2600, liters = 4.1, cost = 55.0, full = true))
        c.garageRepository.saveFuel(FuelEntity(date = Dates.addDays(today, -9), km = 2790, liters = 3.6, cost = 48.5, full = true))
        c.garageRepository.saveDocument(DocumentEntity(type = "insurance", title = "Assur'Auto — Sanlam", number = "AU-2025-8841", expiry = Dates.addDays(today, 9)))
        c.garageRepository.saveDocument(DocumentEntity(type = "registration", title = "Carte grise", number = "12345-A-40"))
    }

    private val noop: () -> Unit = {}

    private val screens: List<Pair<String, @Composable () -> Unit>> = listOf(
        "01-garage" to {
            GarageScreen(GarageNav({}, noop, noop, noop, noop, noop, noop, noop, noop, noop, noop))
        },
        "02-service" to { ServiceScreen(openTask = {}, logService = {}) },
        "03-task-engine-oil" to { TaskDetailScreen("engine-oil", noop, {}, {}) },
        "04-log-service" to { LogServiceScreen(null, null, 3000, noop) },
        "05-passport" to { PassportScreen(noop, noop, {}, {}) },
        "06-fuel" to { FuelScreen(noop) },
        "07-wallet" to { WalletScreen(noop) },
        "08-profile" to { BikeProfileScreen(noop) },
        "09-manual" to { ManualHubScreen({ _: PdfSource, _: Int -> }, noop, noop, noop) },
        "10-guide" to { GuideHubScreen(GuideNav(noop, noop, noop, noop, noop, noop, noop, noop, noop)) },
        "11-checklist" to { ChecklistScreen(noop) },
        "12-troubleshoot" to { TroubleshootScreen(noop) {} },
        "13-trouble-tree" to { TroubleTreeScreen("wont-start", noop) {} },
        "14-issues" to { FailureModesScreen(noop) },
        "15-howto" to { GuidesListScreen(noop) {} },
        "16-guide-oil" to { GuideDetailScreen("engine-oil", noop) },
        "17-specs" to { SpecsScreen(noop) },
        "18-oils" to { OilsScreen(noop) },
        "19-seasons" to { SeasonsScreen(noop) },
        "20-parts" to { PartsScreen(noop) },
        "21-gear" to { GearScreen(noop) },
        "22-more" to { MoreScreen(MoreNav(noop, noop, noop, noop, noop, noop)) },
        "23-sos" to { SosScreen(noop, noop) },
        "24-settings" to { SettingsScreen(noop) }
    )

    @Test
    fun everyScreenRenders() {
        seed()
        var index by mutableStateOf(0)
        var dark by mutableStateOf(false)
        compose.setContent {
            KymcoTheme(darkTheme = dark) {
                Surface(color = MaterialTheme.colorScheme.background) { Box { screens[index].second() } }
            }
        }
        for (theme in listOf(false, true)) {
            dark = theme
            for (i in screens.indices) {
                if (theme && i !in setOf(0, 2, 4, 9)) continue // dark mode: a representative subset
                index = i
                compose.waitForIdle()
                compose.mainClock.advanceTimeBy(500)
                compose.waitForIdle()
                outDir?.let { dir ->
                    val name = screens[i].first + if (theme) "-dark" else ""
                    // captureToImage() waits for a draw callback Robolectric never fires; draw directly.
                    val view = compose.activity.window.decorView
                    val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                    view.draw(Canvas(bitmap))
                    File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                }
            }
        }
    }
}
