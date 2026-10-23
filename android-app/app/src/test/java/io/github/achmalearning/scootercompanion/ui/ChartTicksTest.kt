package io.github.achmalearning.scootercompanion.ui

import io.github.achmalearning.scootercompanion.ui.charts.compact
import io.github.achmalearning.scootercompanion.ui.charts.fittedTicks
import io.github.achmalearning.scootercompanion.ui.charts.niceTicks
import org.junit.Assert.assertEquals
import org.junit.Test

class ChartTicksTest {
    @Test fun niceTicksCoverTheMaximumIn125Steps() {
        assertEquals(listOf(0.0, 200.0, 400.0, 600.0, 800.0), niceTicks(650.0))
        assertEquals(listOf(0.0, 0.5, 1.0, 1.5, 2.0), niceTicks(1.7))
        assertEquals(listOf(0.0, 1.0), niceTicks(0.0))
    }

    @Test fun fittedTicksHugTheData() {
        assertEquals(listOf(13.0, 14.0, 15.0, 16.0, 17.0), fittedTicks(13.2, 16.1))
        assertEquals(listOf(14.5, 15.0, 15.5, 16.0, 16.5), fittedTicks(14.6, 16.2))
    }

    @Test fun compactLabels() {
        assertEquals("850", compact(850.0))
        assertEquals("1.3k", compact(1250.0))
        assertEquals("2k", compact(2000.0))
        assertEquals("15k", compact(15000.0))
        assertEquals("1.5", compact(1.5))
    }
}
