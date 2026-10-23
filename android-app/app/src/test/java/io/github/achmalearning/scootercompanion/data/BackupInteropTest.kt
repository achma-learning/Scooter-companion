package io.github.achmalearning.scootercompanion.data

import androidx.test.core.app.ApplicationProvider
import io.github.achmalearning.scootercompanion.AppContainer
import io.github.achmalearning.scootercompanion.data.backup.BackupFile
import io.github.achmalearning.scootercompanion.data.export.TextReport
import io.github.achmalearning.scootercompanion.domain.CoverageState
import io.github.achmalearning.scootercompanion.domain.Cooling
import io.github.achmalearning.scootercompanion.domain.Dates
import io.github.achmalearning.scootercompanion.domain.SpendCategory
import io.github.achmalearning.scootercompanion.domain.StampState
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * The save file is a public contract: Scooter Companion's own files must round-trip, and
 * Agility Companion's (app 2.0.x and the website, same fixture as web/tests/store.test.mjs)
 * must still import. Goes through the real Room database.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupInteropTest {

    private fun fixture(name: String) = listOf("../../shared/fixtures/$name", "../shared/fixtures/$name")
        .map(::File).first { it.exists() }.readText()

    private val json = Json { ignoreUnknownKeys = true }
    private val container get() = AppContainer.getInstance(ApplicationProvider.getApplicationContext())

    @Test
    fun agilityCompanionBackupStillImports() = runBlocking {
        val legacy = fixture("backup-sample.json")
        val summary = container.garageRepository.importJson(legacy, container.maintenanceRepository)
        assertTrue(summary, summary.startsWith("2 services"))

        val snap = container.garageRepository.current()
        assertEquals("Zitoun", snap.profile.nickname)
        assertEquals("Kymco", snap.profile.brand)
        assertEquals("Agility 50 4T", snap.profile.model)
        assertEquals(50, snap.profile.engineCc)
        assertEquals(2890, snap.currentKm)
        assertEquals(Dates.fromIso("2025-11-09"), snap.profile.purchaseDate)
        assertEquals(StampState.STAMPED, snap.milestones.first { it.milestone.km == 300 }.state)
        assertEquals(StampState.DONE, snap.milestones.first { it.milestone.km == 2000 }.state)
        assertEquals(Dates.fromIso("2026-10-08"), snap.lastPreRide)

        // Re-exported as a version 2 file that keeps every version 1 field.
        val original = json.decodeFromString(BackupFile.serializer(), legacy)
        val exported = json.decodeFromString(BackupFile.serializer(), container.garageRepository.exportJson())
        assertEquals(BackupFile.FORMAT, exported.format)
        assertEquals(2, exported.version)
        assertEquals(original.bike.copy(brand = "Kymco", model = "Agility 50 4T", engineCc = 50), exported.bike)
        assertEquals(original.services.sortedBy { it.km }, exported.services.sortedBy { it.km })
        assertEquals(original.fuel.sortedBy { it.km }, exported.fuel.sortedBy { it.km })
        assertEquals(original.documents, exported.documents)
        assertEquals(original.odometer, exported.odometer)
        assertEquals(original.preRide, exported.preRide)
    }

    @Test
    fun scooterCompanionBackupRoundTrips() = runBlocking {
        val v2 = fixture("backup-sample-v2.json")
        container.garageRepository.importJson(v2, container.maintenanceRepository)

        val snap = container.garageRepository.current()
        assertEquals("PCX 125", snap.profile.model)
        assertEquals(Cooling.LIQUID, snap.spec.cooling)
        assertTrue(snap.profile.boughtUsed)
        assertEquals(1, snap.insurance.size)
        assertEquals(3, snap.expenses.size)
        assertEquals("Michelin City Grip 2", snap.entries.first { it.km == 9800 }.product)
        // 2,000 insurance + 1,100 gear and top case + 150 fine + 1,364 services + 110 fuel
        assertEquals(4724.0, snap.ledger.running, 1e-9)
        assertEquals(21000.0, snap.ledger.purchase, 1e-9)
        assertEquals(1364.0, snap.spends.filter { it.category == SpendCategory.MAINTENANCE }.sumOf { it.amount }, 1e-9)
        assertTrue(snap.coverage.state != CoverageState.NONE)

        val original = json.decodeFromString(BackupFile.serializer(), v2)
        val exported = json.decodeFromString(BackupFile.serializer(), container.garageRepository.exportJson())
        assertEquals(original.bike, exported.bike)
        assertEquals(original.services.sortedBy { it.km }, exported.services.sortedBy { it.km })
        assertEquals(original.fuel.sortedBy { it.km }, exported.fuel.sortedBy { it.km })
        assertEquals(original.documents, exported.documents)
        assertEquals(original.insurance, exported.insurance)
        assertEquals(original.expenses.sortedBy { it.date }, exported.expenses.sortedBy { it.date })
        assertEquals(original.preRide, exported.preRide)
    }

    @Test
    fun aiReportHasEverythingAndCanHideIdentifiers() = runBlocking {
        container.garageRepository.importJson(fixture("backup-sample-v2.json"), container.maintenanceRepository)
        val snap = container.garageRepository.current()
        val hidden = TextReport.build(snap, container.contentRepository.content, hideIds = true, exportedOn = "2026-10-23")
        listOf("Honda PCX 125", "125 cc, injection, liquid-cooled", "bought: used".replaceFirstChar { it.uppercase() },
            "== FUEL (2 fill-ups) ==", "Michelin City Grip 2", "Wafa Assurance", "Passenger without helmet", "== MONTH BY MONTH")
            .forEach { assertTrue("missing: $it", it in hidden) }
        listOf("LXYJCBL01P0000001", "54321-B-40", "JF81E-0001", "RC-2026-0042").forEach { assertFalse("leaked: $it", it in hidden) }

        // -PscreenshotsDir=… also keeps a sample report for a human to read.
        System.getProperty("screenshots.dir").orEmpty().takeIf { it.isNotBlank() }?.let { File(it).mkdirs(); File(it, "sample-report.txt").writeText(hidden) }
        val shown = TextReport.build(snap, container.contentRepository.content, hideIds = false)
        assertTrue("LXYJCBL01P0000001" in shown && "RC-2026-0042" in shown)
    }
}
