package io.github.achmalearning.scootercompanion.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import io.github.achmalearning.scootercompanion.data.db.MIGRATION_2_3
import io.github.achmalearning.scootercompanion.data.db.ManualDatabase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Runs the real migrations against the exported schemas and lets Room validate the result
 * against the current entities. A logbook lost to a bad migration is the worst bug this app
 * can have. (Version 1 was never exported, so its step can't be replayed here.)
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigrationTest {
    private val dbName = "migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), ManualDatabase::class.java)

    @Test
    fun v2ToV3KeepsTheGarageAndFillsInTheAgility() {
        helper.createDatabase(dbName, 2).use { db ->
            db.execSQL(
                "INSERT INTO bike_profile (id, nickname, plate, vin, engineNo, color, purchaseDate, purchaseKm, dealer, warrantyMonths) " +
                    "VALUES (1, 'Agi', '12345-A-6', 'VIN1', 'E1', 'Red', 1700000000000, 3, 'Mob Shop', 24)"
            )
            db.execSQL("INSERT INTO fuel_entries (date, km, liters, cost, full) VALUES (1700000000000, 120, 1.98, 30.0, 0)")
            db.execSQL(
                "INSERT INTO maintenance_entries (km, date, note, tasks, shop, cost, stamped, updatedAt, dirty, deleted) " +
                    "VALUES (300, 1700000000000, 'first', 'engine-oil', 'Mob Shop', 100.0, 1, 0, 0, 0)"
            )
        }

        helper.runMigrationsAndValidate(dbName, 3, true, MIGRATION_2_3).use { db ->
            db.query("SELECT brand, model, engineCc, powertrain, fuelSystem, cooling, region, nickname, purchaseKm FROM bike_profile").use {
                it.moveToFirst()
                assertEquals("Kymco", it.getString(0))
                assertEquals("Agility 50 4T", it.getString(1))
                assertEquals(50, it.getInt(2))
                assertEquals("PETROL", it.getString(3))
                assertEquals("CARBURETOR", it.getString(4))
                assertEquals("AIR", it.getString(5))
                assertEquals("MA", it.getString(6))
                assertEquals("Agi", it.getString(7))
                assertEquals(3, it.getInt(8))
            }
            db.query("SELECT cost, product, title FROM maintenance_entries").use {
                it.moveToFirst()
                assertEquals(100.0, it.getDouble(0), 0.0)
                assertEquals("", it.getString(1))
                assertEquals("", it.getString(2))
            }
            db.query("SELECT COUNT(*) FROM fuel_entries").use { it.moveToFirst(); assertEquals(1, it.getInt(0)) }
            db.query("SELECT COUNT(*) FROM insurance_policies").use { it.moveToFirst(); assertEquals(0, it.getInt(0)) }
            db.query("SELECT COUNT(*) FROM expenses").use { it.moveToFirst(); assertEquals(0, it.getInt(0)) }
        }
    }

}
