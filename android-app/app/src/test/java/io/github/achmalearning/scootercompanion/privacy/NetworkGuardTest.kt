package io.github.achmalearning.scootercompanion.privacy

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/** Every network call is a tap-started download routed through [NetworkGuard.userRequested]. */
class NetworkGuardTest {

    @Test
    fun `a tap-started download runs and is counted`() = runBlocking {
        val tapsBefore = NetworkGuard.userRequests.value
        val result = NetworkGuard.userRequested { "price" }
        assertEquals("price", result)
        assertEquals(tapsBefore + 1, NetworkGuard.userRequests.value)
    }
}
