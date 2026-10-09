package com.example.kymcoagility50.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.kymcoagility50.auth.AuthManager
import com.example.kymcoagility50.data.db.ManualDatabase
import com.example.kymcoagility50.data.db.MaintenanceEntryEntity
import com.example.kymcoagility50.privacy.NetworkGuard
import com.example.kymcoagility50.privacy.PrivacyManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Pushes locally-dirty maintenance-log entries to
 * /users/{uid}/maintenanceEntries/{docId} and pulls the server copy, merging by
 * last-write-wins on `updatedAt`. This worker touches NOTHING else — bookmarks, recent
 * pages, and settings never leave the device, in either mode.
 *
 * The very first thing this does is check PrivacyManager.isPrivate(). If true, it returns
 * Result.success() immediately without importing/using Firestore in any way, and without
 * being able to have been scheduled in the first place (see SyncScheduler).
 */
class MaintenanceSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (PrivacyManager.isPrivate()) return Result.success()
        val user = AuthManager.currentUser(applicationContext) ?: return Result.success() // not signed in; nothing to do
        if (!NetworkGuard.isNetworkAllowed()) return Result.success()

        val dao = ManualDatabase.getInstance(applicationContext).maintenanceEntryDao()
        val firestore = FirebaseFirestore.getInstance()
        val collection = firestore.collection("users").document(user.uid).collection("maintenanceEntries")

        return try {
            // 1. Push local dirty rows (including tombstones for deletions).
            for (entry in dao.getDirty()) {
                if (entry.deleted) {
                    if (entry.remoteId != null) {
                        collection.document(entry.remoteId).delete().await()
                    }
                    dao.hardDelete(entry.id)
                } else {
                    val docRef = if (entry.remoteId != null) collection.document(entry.remoteId) else collection.document()
                    docRef.set(entry.toRemoteMap()).await()
                    dao.markSynced(entry.id, docRef.id)
                }
            }

            // 2. Pull remote rows, merging by remoteId. A row already known locally (by
            // remoteId) is updated in place — preserving its local `id` — rather than
            // inserted again, and only if the remote copy is newer (last-write-wins).
            // A remote row not yet known locally is inserted as new.
            val remoteSnapshot = collection.get().await()
            for (doc in remoteSnapshot.documents) {
                val km = (doc.getLong("km") ?: continue).toInt()
                val date = doc.getLong("date") ?: continue
                val note = doc.getString("note") ?: ""
                val updatedAt = doc.getLong("updatedAt") ?: 0L
                // Fields added in v2 of the logbook; older documents simply lack them.
                val tasks = doc.getString("tasks") ?: ""
                val milestoneKm = doc.getLong("milestoneKm")?.toInt()
                val shop = doc.getString("shop") ?: ""
                val cost = doc.getDouble("cost")
                val stamped = doc.getBoolean("stamped") ?: false

                val existing = dao.getByRemoteId(doc.id)
                if (existing != null) {
                    if (updatedAt > existing.updatedAt) {
                        dao.upsert(
                            existing.copy(
                                km = km, date = date, note = note, tasks = tasks, milestoneKm = milestoneKm,
                                shop = shop, cost = cost, stamped = stamped, updatedAt = updatedAt, dirty = false
                            )
                        )
                    }
                } else {
                    dao.upsert(
                        MaintenanceEntryEntity(
                            remoteId = doc.id,
                            km = km,
                            date = date,
                            note = note,
                            tasks = tasks,
                            milestoneKm = milestoneKm,
                            shop = shop,
                            cost = cost,
                            stamped = stamped,
                            updatedAt = updatedAt,
                            dirty = false,
                            deleted = false
                        )
                    )
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun MaintenanceEntryEntity.toRemoteMap() = mapOf(
        "km" to km,
        "date" to date,
        "note" to note,
        "tasks" to tasks,
        "milestoneKm" to milestoneKm,
        "shop" to shop,
        "cost" to cost,
        "stamped" to stamped,
        "updatedAt" to updatedAt
    )
}
