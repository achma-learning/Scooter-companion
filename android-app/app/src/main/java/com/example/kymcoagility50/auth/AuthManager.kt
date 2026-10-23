package com.example.kymcoagility50.auth

import android.content.Context
import android.content.Intent
import com.example.kymcoagility50.privacy.PrivacyManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Everything Firebase-related lives behind this object. Nothing here runs, and
 * [FirebaseApp.initializeApp] is never called, unless [PrivacyManager.isPrivate] is false —
 * i.e. the user is in Sync Mode. This is the one place Firebase is allowed to be touched.
 */
object AuthManager {

    // Web/OAuth client ID from google-services.json, resolved at runtime rather than hardcoded
    // so the placeholder file still compiles; see README for the real value once you swap it in.
    private fun webClientId(context: Context): String {
        val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        return if (resId != 0) context.getString(resId) else ""
    }

    private var googleSignInClient: GoogleSignInClient? = null

    /** Must be called before any other AuthManager function. No-ops entirely in Private Mode. */
    fun ensureInitialized(context: Context) {
        if (PrivacyManager.isPrivate()) return
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }
        if (googleSignInClient == null) {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId(context))
                .requestEmail()
                .build()
            googleSignInClient = GoogleSignIn.getClient(context, gso)
        }
    }

    fun currentUser(context: Context): FirebaseUser? {
        if (PrivacyManager.isPrivate()) return null
        ensureInitialized(context)
        return runCatching { FirebaseAuth.getInstance().currentUser }.getOrNull()
    }

    fun signInIntent(context: Context): Intent? {
        if (PrivacyManager.isPrivate()) return null
        ensureInitialized(context)
        return googleSignInClient?.signInIntent
    }

    suspend fun handleSignInResult(idToken: String): FirebaseUser? {
        if (PrivacyManager.isPrivate()) return null
        val credential: AuthCredential = GoogleAuthProvider.getCredential(idToken, null)
        val result = FirebaseAuth.getInstance().signInWithCredential(credential).await()
        return result.user
    }

    /** Local sign-out. Also called automatically whenever the user switches into Private Mode. */
    fun signOut(context: Context) {
        runCatching { FirebaseAuth.getInstance().signOut() }
        runCatching { googleSignInClient?.signOut() }
    }

    /** Emits the current user on every auth-state change; Private Mode emits null once and stops. */
    fun authStateFlow(context: Context): Flow<FirebaseUser?> = callbackFlow {
        if (PrivacyManager.isPrivate()) {
            trySend(null)
            close()
            return@callbackFlow
        }
        ensureInitialized(context)
        val auth = FirebaseAuth.getInstance()
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }
}
