package io.github.achmalearning.scootercompanion.domain

import io.github.achmalearning.scootercompanion.data.content.Content
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Same scenarios as web/tests/planner.test.mjs — the app and the website must agree on what
 * is due. Runs against the real shared/content/agility50.json.
 */
class ServicePlannerTest {

    private val content: Content by lazy {
        val file = listOf("../../shared/content/agility50.json", "../shared/content/agility50.json").map(::File).first { it.exists() }
        Json { ignoreUnknownKeys = true }.decodeFromString(Content.serializer(), file.readText())
    }
    private val day0 = Dates.utcMidnight(2026, 0, 1)
    private fun task(id: String) = content.task(id)!!

    @Test fun newBikeFirstOilChangeIsAt300km() {
        val s = ServicePlanner.status(task("engine-oil"), emptyList(), 120, day0, 0, day0)
        assertEquals(300, s.dueKm)
        assertEquals(180, s.kmLeft)
        assertEquals(DueStatus.OK, s.status)
    }

    @Test fun breakInSequenceThenRegularInterval() {
        val oil = task("engine-oil")
        fun dueAfter(km: Int) = ServicePlanner.status(oil, listOf(ServiceRecord(day0, km, setOf("engine-oil"))), km, day0, 0, day0).dueKm
        assertEquals(1000, dueAfter(300))
        assertEquals(2000, dueAfter(1000))
        assertEquals(3000, dueAfter(2000))
        assertEquals(5000, dueAfter(3000))
        assertEquals(7000, dueAfter(5000))
    }

    @Test fun gearOilBreakInThenEvery4000() {
        val gear = task("gear-oil")
        val after1000 = ServicePlanner.status(gear, listOf(ServiceRecord(day0, 1000, setOf("gear-oil"))), 1200, day0, 0, day0)
        assertEquals(5000, after1000.dueKm)
    }

    @Test fun overdueAndSoonThresholds() {
        val oil = task("engine-oil")
        val record = listOf(ServiceRecord(day0, 3000, setOf("engine-oil")))
        assertEquals(DueStatus.OVERDUE, ServicePlanner.status(oil, record, 5050, day0, 0, day0).status)
        assertEquals(DueStatus.SOON, ServicePlanner.status(oil, record, 4750, day0, 0, day0).status)
        assertEquals(DueStatus.OK, ServicePlanner.status(oil, record, 4000, day0, 0, day0).status)
    }

    @Test fun timeLimitTriggersEvenWithoutKm() {
        val fluid = task("brake-fluid")
        val record = listOf(ServiceRecord(day0, 1000, setOf("brake-fluid")))
        val thirteenMonthsLater = Dates.addMonths(day0, 13)
        val s = ServicePlanner.status(fluid, record, 1500, thirteenMonthsLater, 0, day0)
        assertEquals(DueStatus.OVERDUE, s.status)
        assertTrue(s.daysLeft!! < 0)
    }

    @Test fun weeklyCheckNeverDoneWithoutPurchaseDateIsSoonNotOverdue() {
        val s = ServicePlanner.status(task("tyre-pressure"), emptyList(), 500, day0, 0, null)
        assertNull(s.dueKm)
        assertEquals(DueStatus.SOON, s.status)
    }

    @Test fun weeklyCheckNeverDoneIgnoresPurchaseDate() {
        val s = ServicePlanner.status(task("tyre-pressure"), emptyList(), 500, Dates.addMonths(day0, 6), 0, day0)
        assertEquals(DueStatus.SOON, s.status)
        assertNull(s.daysLeft)
    }

    @Test fun weeklyCheckDoneTenDaysAgoIsOverdue() {
        val rec = listOf(ServiceRecord(day0, 100, setOf("tyre-pressure")))
        val s = ServicePlanner.status(task("tyre-pressure"), rec, 500, Dates.addDays(day0, 10), 0, day0)
        assertEquals(DueStatus.OVERDUE, s.status)
        assertEquals(-3, s.daysLeft)
    }

    @Test fun currentKmTakesTheHighestSource() {
        assertEquals(4200, ServicePlanner.currentKm(0, listOf(3000), listOf(4200, 100), listOf(3900)))
        assertEquals(800, ServicePlanner.currentKm(800, emptyList(), emptyList()))
    }

    @Test fun passportMilestones() {
        val records = listOf(
            ServiceRecord(day0, 310, setOf("engine-oil"), milestoneKm = 300, stamped = true),
            ServiceRecord(day0, 1020, setOf("engine-oil"), milestoneKm = 1000, stamped = false)
        )
        val states = ServicePlanner.milestones(content.milestones, records, 3600).associate { it.milestone.km to it.state }
        assertEquals(StampState.STAMPED, states[300])
        assertEquals(StampState.DONE, states[1000])
        assertEquals(StampState.MISSED, states[2000])
        assertEquals(StampState.NEXT, states[3000])
        assertEquals(StampState.UPCOMING, states[5000])
    }

    @Test fun healthScoreBounds() {
        val all = ServicePlanner.statuses(content.tasks, emptyList(), 0, day0, 0, day0)
        val score = ServicePlanner.healthScore(all)
        assertTrue(score in 1..100)
        val worn = ServicePlanner.statuses(content.tasks, emptyList(), 50_000, Dates.addMonths(day0, 24), 0, day0)
        // Everything km/date-based overdue; only never-logged weekly checks still count as "soon".
        assertTrue(ServicePlanner.healthScore(worn) < 10)
    }
}
