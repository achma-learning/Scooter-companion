package com.example.kymcoagility50

import android.app.Application
import com.example.kymcoagility50.privacy.PrivacyManager

class KymcoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Must run before any Firebase/auth/WorkManager code path can be reached, since all
        // of them gate on PrivacyManager.isPrivate().
        PrivacyManager.initialize(this)
    }
}
