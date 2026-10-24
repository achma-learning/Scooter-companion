package io.github.achmalearning.scootercompanion.domain

import io.github.achmalearning.scootercompanion.data.content.Content
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Which tasks each kind of scooter gets, against the real knowledge base. */
class MaintenancePlanTest {
    private val content: Content = listOf("../../shared/content/agility50.json", "../shared/content/agility50.json")
        .map(::File).first { it.exists() }.readText()
        .let { Json { ignoreUnknownKeys = true }.decodeFromString(Content.serializer(), it) }

    private fun ids(spec: VehicleSpec) = MaintenancePlan.tasksFor(content, spec, agility = false).map { it.id }.toSet()
    private fun task(spec: VehicleSpec, id: String) = MaintenancePlan.tasksFor(content, spec, agility = false).first { it.id == id }

    @Test fun theAgilityKeepsItsOwnScheduleAndPassport() {
        assertTrue(MaintenancePlan.isAgility50("Kymco", "Agility 50 4T", 50))
        assertTrue(MaintenancePlan.isAgility50(" kymco ", "agility", 50))
        assertFalse(MaintenancePlan.isAgility50("Kymco", "Agility 125", 125))
        assertFalse(MaintenancePlan.isAgility50("SYM", "Orbit", 50))
        val tasks = MaintenancePlan.tasksFor(content, VehicleSpec(cc = 50), agility = true)
        assertEquals(content.tasks, tasks)
        assertEquals(content.milestones, MaintenancePlan.milestonesFor(content, agility = true))
        assertTrue(MaintenancePlan.milestonesFor(content, agility = false).isEmpty())
    }

    @Test fun aCarburettedAirCooled50() {
        val spec = VehicleSpec(Powertrain.PETROL, 50, FuelSystem.CARBURETOR, Cooling.AIR)
        val ids = ids(spec)
        assertTrue("carb-clean" in ids && "fuel-filter" in ids)
        assertFalse("throttle-body" in ids)
        assertFalse("coolant-level" in ids || "coolant-replace" in ids)
        assertEquals(2000, task(spec, "engine-oil").intervalKm)
        assertEquals(8000, task(spec, "belt").intervalKm)
    }

    @Test fun anInjectedLiquidCooled125LikeAPcx() {
        val spec = VehicleSpec(Powertrain.PETROL, 125, FuelSystem.INJECTION, Cooling.LIQUID)
        val ids = ids(spec)
        assertTrue("throttle-body" in ids)
        assertFalse("carb-clean" in ids || "fuel-filter" in ids)
        assertTrue("coolant-level" in ids && "coolant-replace" in ids && "radiator-clean" in ids)
        assertEquals(3000, task(spec, "engine-oil").intervalKm)
    }

    @Test fun aMaxiScooterGetsLongerIntervals() {
        val spec = VehicleSpec(Powertrain.PETROL, 530, FuelSystem.INJECTION, Cooling.LIQUID)
        assertEquals(EngineClass.MAXI, spec.engineClass)
        assertEquals(5000, task(spec, "engine-oil").intervalKm)
        assertEquals(24000, task(spec, "belt").intervalKm)
    }

    @Test fun anElectricScooterHasNoEngineTasks() {
        val ids = ids(VehicleSpec(Powertrain.ELECTRIC, 0))
        assertFalse("engine-oil" in ids || "plug-replace" in ids || "air-filter-clean" in ids || "valves" in ids)
        assertTrue("traction-battery" in ids && "brake-pads" in ids && "tyre-rear" in ids)
    }

    @Test fun genericTasksHaveNamesInEveryLanguage() {
        content.genericTasks.forEach {
            assertTrue(it.id, it.localName("fr").isNotBlank() && it.localName("ar").isNotBlank())
            assertTrue(it.id, it.localName("ar") != it.name)
        }
    }

    @Test fun plannerWorksOnGenericTasks() {
        val spec = VehicleSpec(Powertrain.PETROL, 125, FuelSystem.INJECTION, Cooling.LIQUID)
        val tasks = MaintenancePlan.tasksFor(content, spec, agility = false)
        val day = Dates.fromIso("2026-01-01")!!
        val records = listOf(ServiceRecord(day, 1000, setOf("engine-oil")))
        val oil = ServicePlanner.statuses(tasks, records, currentKm = 3700, today = day, baselineKm = 0, baselineDate = day)
            .first { it.task.id == "engine-oil" }
        // Break-in change at 1,000 km, then every 3,000 km for a 125: 300 km left is "soon".
        assertEquals(4000, oil.dueKm)
        assertEquals(DueStatus.SOON, oil.status)
    }

    @Test fun theOwnersIntervalBeatsTheMakersWhichBeatsTheApps() {
        val spec = VehicleSpec(cc = 125, fuelSystem = FuelSystem.INJECTION, cooling = Cooling.LIQUID)
        val app = MaintenancePlan.tasksFor(content, spec, agility = false).first { it.id == "engine-oil" }
        assertEquals(3000, app.intervalKm)
        val maker = mapOf("engine-oil" to Interval(6000, 12, listOf(1000)))
        val withMaker = MaintenancePlan.tasksFor(content, spec, false, maker).first { it.id == "engine-oil" }
        assertEquals(6000, withMaker.intervalKm)
        assertEquals(12, withMaker.intervalMonths)
        assertEquals(listOf(1000), withMaker.breakInKm)
        val owner = mapOf("engine-oil" to Interval(1500, null))
        val withOwner = MaintenancePlan.tasksFor(content, spec, false, maker, owner).first { it.id == "engine-oil" }
        assertEquals(1500, withOwner.intervalKm)
        assertEquals(null, withOwner.intervalMonths)
        // Tasks neither names keep the app's interval for the engine size.
        assertEquals(task(spec, "brake-pads").intervalKm, MaintenancePlan.tasksFor(content, spec, false, maker, owner).first { it.id == "brake-pads" }.intervalKm)
    }

    @Test fun theOwnerCanChangeAnAgilityInterval() {
        val tasks = MaintenancePlan.tasksFor(content, VehicleSpec(cc = 50), agility = true, owner = mapOf("engine-oil" to Interval(1500, null)))
        val oil = tasks.first { it.id == "engine-oil" }
        assertEquals(1500, oil.intervalKm)
        assertEquals("", oil.label)
        assertTrue(oil.breakInKm.all { it < 1500 })
    }
}
