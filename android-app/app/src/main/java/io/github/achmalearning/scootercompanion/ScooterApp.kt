package io.github.achmalearning.scootercompanion

import android.app.Application
import io.github.achmalearning.scootercompanion.privacy.PrivacyManager

class ScooterApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Loads the first-launch flag before the onboarding gate reads it.
        PrivacyManager.initialize(this)
    }
}
