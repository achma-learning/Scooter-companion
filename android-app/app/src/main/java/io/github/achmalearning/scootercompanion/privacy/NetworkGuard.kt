package io.github.achmalearning.scootercompanion.privacy

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * The single choke point for network access. The app never goes online by itself: the only
 * calls are downloads of public files the owner asks for with a tap (today's fuel price,
 * "Check for updates" / "Download & install"). Nothing about the owner is ever sent.
 */
object NetworkGuard {

    private val _userRequests = MutableStateFlow(0)
    /** Downloads the owner started with a tap (see [userRequested]). */
    val userRequests: StateFlow<Int> = _userRequests

    /**
     * Wraps a download of a public file that the owner explicitly asked for with a tap. Call
     * it ONLY from a click handler — never from a worker, app start or a LaunchedEffect — so
     * the app still never goes online by itself.
     */
    suspend fun <T> userRequested(block: suspend () -> T): T {
        _userRequests.value += 1
        return block()
    }
}
