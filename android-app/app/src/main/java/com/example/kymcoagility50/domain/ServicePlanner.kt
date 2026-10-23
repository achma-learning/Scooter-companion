package com.example.kymcoagility50.domain

import com.example.kymcoagility50.data.content.Milestone
import com.example.kymcoagility50.data.content.Task
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** A logbook entry as the planner sees it. */
data class ServiceRecord(
    val date: Long,
    val km: Int,
    val tasks: Set<String>,
    val milestoneKm: Int? = null,
    val stamped: Boolean = false,
    val shop: String = ""
)

enum class DueStatus { OVERDUE, SOON, OK }

data class TaskStatus(
    val task: Task,
    val lastKm: Int?,
    val lastDate: Long?,
    val doneBefore: Boolean,
    val dueKm: Int?,
    val dueDate: Long?,
    val kmLeft: Int?,
    val daysLeft: Int?,
    /** 0 = just done, 1 = due now, > 1 = overdue. */
    val progress: Float,
    val status: DueStatus
)

enum class StampState { STAMPED, DONE, NEXT, MISSED, UPCOMING }

data class MilestoneStatus(val milestone: Milestone, val record: ServiceRecord?, val state: StampState)

/**
 * Turns the knowledge base's intervals + the owner's logbook into "what is due, and when".
 * Mirrors web/js/planner.js line for line — keep the two in sync (both are unit-tested
 * against the same scenarios).
 */
object ServicePlanner {
    const val MISSED_TOLERANCE_KM = 1000

    /** Highest km we know of: logbook, odometer updates, fuel fill-ups, purchase km. */
    fun currentKm(purchaseKm: Int, vararg kmSources: Iterable<Int>): Int =
        kmSources.fold(purchaseKm) { acc, src -> max(acc, src.maxOrNull() ?: acc) }

    fun statuses(
        tasks: List<Task>,
        records: List<ServiceRecord>,
        currentKm: Int,
        today: Long,
        baselineKm: Int,
        baselineDate: Long?
    ): List<TaskStatus> = tasks.map { status(it, records, currentKm, today, baselineKm, baselineDate) }
        .sortedWith(compareBy<TaskStatus> { it.status.ordinal }.thenByDescending { it.progress })

    fun status(
        task: Task,
        records: List<ServiceRecord>,
        currentKm: Int,
        today: Long,
        baselineKm: Int,
        baselineDate: Long?
    ): TaskStatus {
        val last = records.filter { task.id in it.tasks }
            .maxWithOrNull(compareBy<ServiceRecord> { it.km }.thenBy { it.date })
        val lastKm = last?.km ?: baselineKm
        val lastDate = last?.date ?: baselineDate

        var dueKm: Int? = null
        var spanKm = 0
        task.intervalKm?.let { interval ->
            val nextBreakIn = task.breakInKm.filter { it > lastKm }.minOrNull()
            dueKm = if (nextBreakIn != null) min(nextBreakIn, lastKm + interval) else lastKm + interval
            spanKm = max(1, dueKm!! - lastKm)
        }

        var dueDate: Long? = null
        var spanDays = 0
        if (lastDate != null) {
            task.intervalMonths?.let { dueDate = Dates.addMonths(lastDate, it) }
            task.intervalDays?.let { days ->
                val byDays = Dates.addDays(lastDate, days)
                dueDate = dueDate?.let { min(it, byDays) } ?: byDays
            }
            dueDate?.let { spanDays = max(1, Dates.daysBetween(lastDate, it)) }
        }

        val kmLeft = dueKm?.let { it - currentKm }
        val daysLeft = if (task.intervalKm == null && last == null) null else dueDate?.let { Dates.daysBetween(today, it) }

        val kmProgress = if (dueKm != null) (currentKm - lastKm).toFloat() / spanKm else 0f
        val dayProgress = if (dueDate != null && lastDate != null) Dates.daysBetween(lastDate, today).toFloat() / spanDays else 0f
        // A purely time-based check (weekly tyre pressure…) that was never logged is simply
        // "due once": counting weeks from the purchase date would only produce noise.
        val neverTimed = task.intervalKm == null && last == null
        val progress = if (neverTimed) 1f else max(kmProgress, dayProgress).coerceAtLeast(0f)

        val overdue = (kmLeft != null && kmLeft <= 0) || (daysLeft != null && daysLeft < 0)
        val soonKm = kmLeft != null && kmLeft <= max(100, (spanKm * 0.15f).roundToInt())
        val soonDays = daysLeft != null && daysLeft <= max(1, spanDays / 5)
        val status = when {
            overdue && !neverTimed -> DueStatus.OVERDUE
            overdue || soonKm || soonDays || neverTimed -> DueStatus.SOON
            else -> DueStatus.OK
        }
        return TaskStatus(task, last?.km, lastDate, last != null, dueKm, dueDate, kmLeft, daysLeft, progress, status)
    }

    /** 0-100: severity-weighted share of tasks that are on schedule. */
    fun healthScore(statuses: List<TaskStatus>): Int {
        if (statuses.isEmpty()) return 100
        var total = 0f
        var earned = 0f
        for (s in statuses) {
            val weight = when (s.task.severity) { "critical" -> 3f; "high" -> 2f; else -> 1f }
            total += weight
            earned += weight * when (s.status) { DueStatus.OK -> 1f; DueStatus.SOON -> 0.6f; DueStatus.OVERDUE -> 0f }
        }
        return (100 * earned / total).roundToInt()
    }

    /** Passport stamp grid: which official/wiki milestones are stamped, done, next or missed. */
    fun milestones(milestones: List<Milestone>, records: List<ServiceRecord>, currentKm: Int): List<MilestoneStatus> {
        var nextAssigned = false
        return milestones.map { m ->
            val record = records.filter { it.milestoneKm == m.km }.maxByOrNull { it.date }
            val state = when {
                record != null && record.stamped -> StampState.STAMPED
                record != null -> StampState.DONE
                // An unlogged milestone stays "next" until the bike is half a service interval
                // past it; beyond that it's missed and the following one becomes next.
                !nextAssigned && m.km >= currentKm - MISSED_TOLERANCE_KM -> { nextAssigned = true; StampState.NEXT }
                m.km < currentKm - MISSED_TOLERANCE_KM -> StampState.MISSED
                else -> StampState.UPCOMING
            }
            MilestoneStatus(m, record, state)
        }
    }
}
