package io.github.achmalearning.scootercompanion.work

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
import io.github.achmalearning.scootercompanion.AppContainer
import io.github.achmalearning.scootercompanion.MainActivity
import io.github.achmalearning.scootercompanion.R
import io.github.achmalearning.scootercompanion.domain.Dates
import io.github.achmalearning.scootercompanion.domain.CoverageState
import io.github.achmalearning.scootercompanion.domain.DueStatus
import io.github.achmalearning.scootercompanion.domain.ExpenseLedger
import io.github.achmalearning.scootercompanion.domain.Insurance
import io.github.achmalearning.scootercompanion.domain.Spend
import io.github.achmalearning.scootercompanion.ui.components.formatMoney
import io.github.achmalearning.scootercompanion.ui.screens.money.spendCategoryRes
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Once a day, on-device only. Posts up to three notifications:
 * - services overdue and documents about to expire;
 * - insurance: every day once fewer than [Insurance.REMIND_DAYS] days of cover remain, or once
 *   cover has run out, until the owner enters the renewal;
 * - on the first run of a new month, last month's spending against the target.
 * Never touches the network.
 */
class ReminderWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val container = AppContainer.getInstance(applicationContext)
        if (!container.settingsRepository.settings.first().reminders) return Result.success()
        val content = container.contentRepository.content
        val snapshot = container.garageRepository.current()
        val c = applicationContext

        val overdue = snapshot.statuses.filter { it.status == DueStatus.OVERDUE && it.task.intervalKm != null }
        val expiring = snapshot.documents.filter { doc ->
            val expiry = doc.expiry ?: return@filter false
            Dates.daysBetween(snapshot.today, expiry) <= content.documentType(doc.type).remindDays
        }
        if (overdue.isNotEmpty() || expiring.isNotEmpty()) {
            val lines = buildList {
                overdue.take(4).forEach { add(c.getString(R.string.notif_task_due, it.task.localName(), "${it.dueKm} km")) }
                expiring.forEach { doc ->
                    val days = Dates.daysBetween(snapshot.today, doc.expiry!!)
                    add(if (days < 0) c.getString(R.string.notif_doc_expired, doc.title, -days) else c.getString(R.string.notif_doc_expires, doc.title, days))
                }
            }
            val title = when {
                overdue.isNotEmpty() && expiring.isNotEmpty() -> c.getString(R.string.notif_title_both)
                overdue.isNotEmpty() -> if (overdue.size == 1) c.getString(R.string.notif_title_one, overdue[0].task.localName()) else c.getString(R.string.notif_title_many, overdue.size)
                else -> c.getString(R.string.notif_title_doc)
            }
            notify(c, ID_SERVICES, CHANNEL, title, lines)
        }

        val coverage = snapshot.coverage
        when (coverage.state) {
            CoverageState.EXPIRING -> notify(
                c, ID_INSURANCE, CHANNEL, c.getString(R.string.notif_insurance_expiring, coverage.daysLeft ?: 0),
                listOf(c.getString(R.string.notif_insurance_expiring_text))
            )
            CoverageState.EXPIRED -> notify(
                c, ID_INSURANCE, CHANNEL, c.getString(R.string.notif_insurance_expired),
                listOf(c.getString(R.string.ins_state_expired_text))
            )
            else -> NotificationManagerCompat.from(c).cancel(ID_INSURANCE)
        }

        monthlyReport(c, snapshot.spends, snapshot.today, snapshot.profile.monthlyBudget)
        return Result.success()
    }

    /** Last month in one notification, sent once per month (the first daily run after the 1st). */
    private fun monthlyReport(c: Context, spends: List<Spend>, today: Long, target: Double?) {
        val prefs = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val thisMonth = Dates.monthKey(today)
        if (prefs.getInt(KEY_LAST_REPORT, -1) == thisMonth) return
        prefs.edit().putInt(KEY_LAST_REPORT, thisMonth).apply()

        val r = ExpenseLedger.previousMonthReport(spends, today, target)
        if (r.total <= 0) return
        val month = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Dates.monthStart(r.key)).replaceFirstChar { it.uppercase() }
        val lines = buildList {
            r.top?.let { add(c.getString(R.string.notif_report_top, c.getString(spendCategoryRes(it)), formatMoney(r.topAmount))) }
            if (r.previousTotal > 0 && r.change != 0.0) add(
                if (r.change > 0) c.getString(R.string.notif_report_more, formatMoney(r.change))
                else c.getString(R.string.notif_report_less, formatMoney(-r.change))
            )
            r.target?.let { add(c.getString(if (r.total <= it) R.string.notif_report_target_ok else R.string.notif_report_target_over, formatMoney(it))) }
        }
        notify(c, ID_REPORT, CHANNEL_REPORTS, c.getString(R.string.notif_report_title, month, formatMoney(r.total)), lines.ifEmpty { listOf(month) })
    }

    companion object {
        private const val CHANNEL = "service_reminders"
        private const val CHANNEL_REPORTS = "monthly_reports"
        private const val WORK_NAME = "daily_reminders"
        private const val PREFS = "reminders"
        private const val KEY_LAST_REPORT = "last_report_month"
        private const val ID_SERVICES = 1
        private const val ID_INSURANCE = 2
        private const val ID_REPORT = 3

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        fun cancel(context: Context) = WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)

        private fun notify(context: Context, id: Int, channel: String, title: String, lines: List<String>) {
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) return
            val manager = context.getSystemService(NotificationManager::class.java)
            if (Build.VERSION.SDK_INT >= 26) {
                manager.createNotificationChannel(
                    NotificationChannel(
                        channel,
                        context.getString(if (channel == CHANNEL) R.string.notif_channel_reminders else R.string.notif_channel_reports),
                        NotificationManager.IMPORTANCE_DEFAULT
                    )
                )
            }
            val open = PendingIntent.getActivity(
                context, id, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val style = NotificationCompat.InboxStyle().also { s -> lines.forEach { s.addLine(it) } }
            val notification = NotificationCompat.Builder(context, channel)
                .setSmallIcon(R.drawable.ic_stat_wrench)
                .setContentTitle(title)
                .setContentText(lines.first())
                .setStyle(style)
                .setContentIntent(open)
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(context).notify(id, notification)
        }
    }
}
