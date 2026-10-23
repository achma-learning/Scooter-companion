package com.example.kymcoagility50.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.kymcoagility50.AppContainer
import com.example.kymcoagility50.MainActivity
import com.example.kymcoagility50.R
import com.example.kymcoagility50.domain.Dates
import com.example.kymcoagility50.domain.DueStatus
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Once a day, on-device only: posts one summary notification when services are overdue or
 * a document (insurance, licence…) is about to expire. Never touches the network, so it
 * runs the same in Private Mode and Sync Mode.
 */
class ReminderWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val container = AppContainer.getInstance(applicationContext)
        if (!container.settingsRepository.settings.first().reminders) return Result.success()
        val content = container.contentRepository.content
        val snapshot = container.garageRepository.current()

        val overdue = snapshot.statuses.filter { it.status == DueStatus.OVERDUE && it.task.intervalKm != null }
        val expiring = snapshot.documents.filter { doc ->
            val expiry = doc.expiry ?: return@filter false
            Dates.daysBetween(snapshot.today, expiry) <= content.documentType(doc.type).remindDays
        }
        if (overdue.isEmpty() && expiring.isEmpty()) return Result.success()

        val lines = buildList {
            overdue.take(4).forEach { add("${it.task.name} — due at ${it.dueKm} km") }
            expiring.forEach { doc ->
                val days = Dates.daysBetween(snapshot.today, doc.expiry!!)
                add(if (days < 0) "${doc.title} expired ${-days} days ago" else "${doc.title} expires in $days days")
            }
        }
        val title = when {
            overdue.isNotEmpty() && expiring.isNotEmpty() -> "Service due and documents expiring"
            overdue.isNotEmpty() -> if (overdue.size == 1) "${overdue[0].task.name} is due" else "${overdue.size} services are due"
            else -> "A document needs renewing"
        }
        notify(applicationContext, title, lines)
        return Result.success()
    }

    companion object {
        private const val CHANNEL = "service_reminders"
        private const val WORK_NAME = "daily_reminders"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        fun cancel(context: Context) = WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)

        private fun notify(context: Context, title: String, lines: List<String>) {
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) return
            val manager = context.getSystemService(NotificationManager::class.java)
            if (Build.VERSION.SDK_INT >= 26) {
                manager.createNotificationChannel(NotificationChannel(CHANNEL, "Service reminders", NotificationManager.IMPORTANCE_DEFAULT))
            }
            val open = PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val style = NotificationCompat.InboxStyle().also { s -> lines.forEach { s.addLine(it) } }
            val notification = NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_wrench)
                .setContentTitle(title)
                .setContentText(lines.first())
                .setStyle(style)
                .setContentIntent(open)
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(context).notify(1, notification)
        }
    }
}
