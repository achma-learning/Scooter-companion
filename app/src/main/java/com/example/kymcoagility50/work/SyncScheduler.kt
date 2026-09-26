package com.example.kymcoagility50.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.kymcoagility50.privacy.PrivacyManager
import java.util.concurrent.TimeUnit

private const val PERIODIC_SYNC_NAME = "maintenance_sync_periodic"

/**
 * The only place MaintenanceSyncWorker is scheduled from. Every entry point checks
 * PrivacyManager.isPrivate() first; switching into Private Mode calls [cancelAll], which
 * removes any pending/periodic work so nothing fires later even if the check at schedule
 * time raced with a mode change.
 */
object SyncScheduler {

    fun scheduleSyncNow(context: Context) {
        if (PrivacyManager.isPrivate()) return
        val request = OneTimeWorkRequestBuilder<MaintenanceSyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }

    fun schedulePeriodicSync(context: Context) {
        if (PrivacyManager.isPrivate()) return
        val request = PeriodicWorkRequestBuilder<MaintenanceSyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_SYNC_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    /** Called immediately when the user enables Private Mode. */
    fun cancelAll(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_SYNC_NAME)
        WorkManager.getInstance(context).cancelAllWorkByTag(MaintenanceSyncWorker::class.java.name)
    }
}
