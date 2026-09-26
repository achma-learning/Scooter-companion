package com.example.kymcoagility50.privacy

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Verifies the core Private-Mode guarantee at the unit level, without touching Android or
 * Firebase: as long as PrivacyManager has not been switched to SYNC (it defaults to PRIVATE,
 * fail-closed, even before initialize() has run), NetworkGuard refuses to execute any block
 * passed to it, and reports the attempt instead of the caller being able to silently proceed.
 *
 * This deliberately does not mock Firebase — the point is that AuthManager, the sync
 * worker, etc. can never reach a real network call because they all route through
 * NetworkGuard.isNetworkAllowed() / NetworkGuard.guard {} first, and this test pins down
 * that those two functions behave correctly in isolation.
 */
class NetworkGuardTest {

    @Test
    fun `defaults to Private Mode (fail closed) before any explicit mode is set`() {
        assertEquals(AppMode.PRIVATE, PrivacyManager.mode.value)
        assertTrue(PrivacyManager.isPrivate())
    }

    @Test
    fun `network is not allowed while Private Mode is on`() {
        assertFalse(NetworkGuard.isNetworkAllowed())
    }

    @Test
    fun `guard never executes its block while Private Mode is on`() = runBlocking {
        var blockRan = false
        val result = NetworkGuard.guard {
            blockRan = true
            "network result"
        }
        assertNull(result)
        assertFalse("network-capable block must never run in Private Mode", blockRan)
    }

    @Test
    fun `every attempted network call while private is counted, not silently dropped`() = runBlocking {
        val before = NetworkGuard.attemptsWhilePrivate.value
        NetworkGuard.guard { "should not run" }
        val after = NetworkGuard.attemptsWhilePrivate.value
        assertEquals(before + 1, after)
    }

    private fun assertTrue(condition: Boolean) = org.junit.Assert.assertTrue(condition)
}
