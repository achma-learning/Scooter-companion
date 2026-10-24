package io.github.achmalearning.scootercompanion.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.LocaleList
import android.view.View
import android.widget.RemoteViews
import androidx.appcompat.app.AppCompatDelegate
import io.github.achmalearning.scootercompanion.AppContainer
import io.github.achmalearning.scootercompanion.MainActivity
import io.github.achmalearning.scootercompanion.R
import io.github.achmalearning.scootercompanion.data.repository.GarageSnapshot
import io.github.achmalearning.scootercompanion.domain.CoverageState
import io.github.achmalearning.scootercompanion.ui.components.dueText
import io.github.achmalearning.scootercompanion.ui.components.formatMoney
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

/**
 * Home-screen widget: this month's spending against the target, days of insurance left and the
 * next service. Plain RemoteViews (no extra library), refreshed when the app's data changes
 * ([watch]), by the daily ReminderWorker and every 12 hours by the launcher.
 */
class ScooterWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        val pending = goAsync()
        scope.launch {
            try {
                update(context)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        @Volatile private var watching = false

        private fun ids(context: Context): IntArray =
            AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, ScooterWidget::class.java))

        /** Redraws every placed widget from the current data; does nothing when none is placed. */
        suspend fun update(context: Context) {
            val ids = ids(context)
            if (ids.isEmpty()) return
            val snapshot = AppContainer.getInstance(context).garageRepository.current()
            AppWidgetManager.getInstance(context).updateAppWidget(ids, render(context, snapshot))
        }

        /** Keeps placed widgets in step with the app's data while the app process lives. */
        @OptIn(FlowPreview::class)
        fun watch(context: Context) {
            if (watching) return
            watching = true
            val app = context.applicationContext
            scope.launch {
                AppContainer.getInstance(app).garageRepository.snapshot.debounce(1_000).collect { s ->
                    val ids = ids(app)
                    if (ids.isNotEmpty()) AppWidgetManager.getInstance(app).updateAppWidget(ids, render(app, s))
                }
            }
        }

        fun render(context: Context, s: GarageSnapshot): RemoteViews {
            val c = localized(context)
            val v = RemoteViews(context.packageName, R.layout.widget_scooter)
            v.setOnClickPendingIntent(
                R.id.widget_root,
                PendingIntent.getActivity(
                    context, 0, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            val p = s.profile
            v.setTextViewText(
                R.id.widget_title,
                p.nickname.ifBlank { listOf(p.brand, p.model).filter { it.isNotBlank() }.joinToString(" ") }.ifBlank { c.getString(R.string.app_name) }
            )
            if (!s.hasProfile) {
                v.setTextViewText(R.id.widget_spent, "")
                v.setTextViewText(R.id.widget_target, c.getString(R.string.widget_add_scooter))
                showBar(v, null, 0)
                listOf(R.id.widget_budget, R.id.widget_insurance, R.id.widget_service).forEach { v.setViewVisibility(it, View.GONE) }
                return v
            }

            // This month against the target: green, orange once the pace says it will go over, red over.
            val ledger = s.ledger
            val budget = ledger.budget
            v.setTextViewText(R.id.widget_spent, formatMoney(ledger.thisMonth))
            v.setTextViewText(
                R.id.widget_target,
                budget?.let { c.getString(R.string.home_of_target, formatMoney(it.target)) } ?: c.getString(R.string.home_this_month)
            )
            if (budget != null) {
                val state = when { budget.over -> R.id.widget_bar_due; budget.projectedOver -> R.id.widget_bar_soon; else -> R.id.widget_bar_ok }
                showBar(v, state, (budget.fraction.coerceIn(0.0, 1.0) * 1000).toInt())
                val (text, color) = when {
                    budget.over -> c.getString(R.string.home_over, formatMoney(-budget.remaining)) to R.color.widget_due
                    budget.projectedOver -> c.getString(R.string.widget_projected, formatMoney(budget.projected)) to R.color.widget_soon
                    else -> c.getString(R.string.home_left, formatMoney(budget.remaining)) to R.color.widget_ok
                }
                v.setTextViewText(R.id.widget_budget, text)
                v.setTextColor(R.id.widget_budget, c.getColor(color))
                v.setViewVisibility(R.id.widget_budget, View.VISIBLE)
            } else {
                showBar(v, null, 0)
                v.setTextViewText(R.id.widget_budget, c.getString(R.string.widget_projected, formatMoney(ledger.projectedThisMonth)))
                v.setTextColor(R.id.widget_budget, c.getColor(R.color.widget_muted))
                v.setViewVisibility(R.id.widget_budget, if (ledger.thisMonth > 0) View.VISIBLE else View.GONE)
            }

            val cover = s.coverage
            val (insurance, insColor) = when (cover.state) {
                CoverageState.NONE -> c.getString(R.string.ins_state_none) to R.color.widget_soon
                CoverageState.EXPIRED -> c.getString(R.string.ins_state_expired) to R.color.widget_due
                CoverageState.EXPIRING -> c.getString(R.string.ins_state_expiring, cover.daysLeft ?: 0) to R.color.widget_soon
                CoverageState.OK -> c.getString(R.string.ins_state_ok, cover.daysLeft ?: 0) to R.color.widget_ink
            }
            v.setTextViewText(R.id.widget_insurance, "🛡 $insurance")
            v.setTextColor(R.id.widget_insurance, c.getColor(insColor))
            v.setViewVisibility(R.id.widget_insurance, View.VISIBLE)

            // The most urgent service (statuses come sorted, most urgent first).
            val next = s.statuses.firstOrNull { it.kmLeft != null || it.daysLeft != null }
            if (next != null) {
                val language = c.resources.configuration.locales[0].language
                v.setTextViewText(R.id.widget_service, "🔧 " + c.getString(R.string.widget_service, next.task.localName(language), dueText(c.resources, next.kmLeft, next.daysLeft)))
            }
            v.setViewVisibility(R.id.widget_service, if (next != null) View.VISIBLE else View.GONE)
            return v
        }

        private fun showBar(v: RemoteViews, shown: Int?, progress: Int) {
            listOf(R.id.widget_bar_ok, R.id.widget_bar_soon, R.id.widget_bar_due).forEach { id ->
                v.setViewVisibility(id, if (id == shown) View.VISIBLE else View.GONE)
                if (id == shown) v.setProgressBar(id, 1000, progress, false)
            }
        }

        /**
         * The in-app language. Android 13+ applies it to the whole app; on older versions it is
         * only known once AppCompat has loaded it, otherwise the phone's language is used.
         */
        private fun localized(context: Context): Context {
            val locales = AppCompatDelegate.getApplicationLocales()
            if (locales.isEmpty) return context
            val config = Configuration(context.resources.configuration)
            config.setLocales(LocaleList.forLanguageTags(locales.toLanguageTags()))
            return context.createConfigurationContext(config)
        }
    }
}
