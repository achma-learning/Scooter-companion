package com.example.kymcoagility50.privacy

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * The single choke point every network-capable call in this app must pass through:
 * Firebase Auth, Firestore reads/writes, and the WorkManager sync worker all call
 * [isNetworkAllowed] (or [guard]) FIRST and bail out immediately if it returns false.
 *
 * This is deliberately dumb and synchronous — it does not itself make any network or
 * connectivity call. It only answers "is Private Mode off", which is the one condition
 * under which this app is allowed to talk to the network at all.
 */
object NetworkGuard {

    private val _attemptsWhilePrivate = MutableStateFlow(0)
    /** Should always read 0. Surfaced on the Privacy Dashboard as a visible guarantee. */
    val attemptsWhilePrivate: StateFlow<Int> = _attemptsWhilePrivate

    private val _userRequests = MutableStateFlow(0)
    /** Downloads the owner started with a tap (see [userRequested]), in either mode. */
    val userRequests: StateFlow<Int> = _userRequests

    fun isNetworkAllowed(): Boolean = !PrivacyManager.isPrivate()

    /**
     * The one exception to "no network in Private Mode": a download of a public file that the
     * owner explicitly asks for with a tap (today's fuel price). Nothing about the owner is
     * sent, and it runs the same in both modes. Call it ONLY from a click handler — never
     * from a worker, app start or a LaunchedEffect — so the app still never goes online by
     * itself. Counted in [userRequests], not [attemptsWhilePrivate].
     */
    suspend fun <T> userRequested(block: suspend () -> T): T {
        _userRequests.value += 1
        return block()
    }

    /**
     * Wrap any network-capable suspend call with this. Returns null and does nothing if
     * Private Mode is on. If Private Mode is somehow on and a caller reaches this point
     * anyway (a bug), it is counted here rather than silently allowed through.
     */
    suspend fun <T> guard(block: suspend () -> T): T? {
        if (!isNetworkAllowed()) {
            _attemptsWhilePrivate.value += 1
            return null
        }
        return block()
    }
}
