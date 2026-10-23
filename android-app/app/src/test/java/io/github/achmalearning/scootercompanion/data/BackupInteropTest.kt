package io.github.achmalearning.scootercompanion.data

import androidx.test.core.app.ApplicationProvider
import io.github.achmalearning.scootercompanion.AppContainer
import io.github.achmalearning.scootercompanion.data.backup.BackupFile
import io.github.achmalearning.scootercompanion.domain.Dates
import io.github.achmalearning.scootercompanion.domain.StampState
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * The app must import what the website exports and vice-versa — same fixture as
 * web/tests/store.test.mjs. Goes through the real Room database.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupInteropTest {

    private val fixture = listOf("../../shared/fixtures/backup-sample.json", "../shared/fixtures/backup-sample.json")
        .map(::File).first { it.exists() }.readText()

    @Test
    fun importThenExportRoundTrips() = runBlocking {
        val container = AppContainer.getInstance(ApplicationProvider.getApplicationContext())
        val summary = container.garageRepository.importJson(fixture, container.maintenanceRepository)
        assertTrue(summary, summary.startsWith("2 services"))

        val snap = container.garageRepository.current()
        assertEquals("Zitoun", snap.profile.nickname)
        assertEquals(2890, snap.currentKm)
        assertEquals(Dates.fromIso("2025-11-09"), snap.profile.purchaseDate)
        assertEquals(StampState.STAMPED, snap.milestones.first { it.milestone.km == 300 }.state)
        assertEquals(StampState.DONE, snap.milestones.first { it.milestone.km == 2000 }.state)
        assertEquals(Dates.fromIso("2026-10-08"), snap.lastPreRide)

        val json = Json { ignoreUnknownKeys = true }
        val original = json.decodeFromString(BackupFile.serializer(), fixture)
        val exported = json.decodeFromString(BackupFile.serializer(), container.garageRepository.exportJson())
        assertEquals(original.bike, exported.bike)
        assertEquals(original.services.sortedBy { it.km }, exported.services.sortedBy { it.km })
        assertEquals(original.fuel.sortedBy { it.km }, exported.fuel.sortedBy { it.km })
        assertEquals(original.documents, exported.documents)
        assertEquals(original.odometer, exported.odometer)
        assertEquals(original.preRide, exported.preRide)
    }
}
