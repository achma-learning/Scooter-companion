package io.github.achmalearning.scootercompanion.domain

import io.github.achmalearning.scootercompanion.data.content.CheckItem
import io.github.achmalearning.scootercompanion.data.content.Content
import io.github.achmalearning.scootercompanion.data.content.Milestone
import io.github.achmalearning.scootercompanion.data.content.Task

/** An interval set by the owner or the manufacturer: null km or months = not tracked that way. */
data class Interval(val km: Int?, val months: Int?, val breakInKm: List<Int>? = null)

/**
 * Which maintenance tasks and passport milestones apply to the owner's scooter.
 *
 * The Kymco Agility 50 4T keeps its own detailed schedule (the Marrakech wiki, the dealer
 * passport). Every other scooter gets the generic list, filtered by the profile (carburettor
 * or injection, air or liquid cooling, petrol or electric) with the km interval of its engine
 * class, then the manufacturer's intervals when scooter-data knows the model. Last, the
 * owner's own intervals win over both.
 */
object MaintenancePlan {

    fun isAgility50(brand: String, model: String, cc: Int): Boolean =
        brand.trim().equals("Kymco", ignoreCase = true) && model.contains("agility", ignoreCase = true) && cc in 1..50

    fun tasksFor(
        content: Content,
        spec: VehicleSpec,
        agility: Boolean,
        manufacturer: Map<String, Interval> = emptyMap(),
        owner: Map<String, Interval> = emptyMap()
    ): List<Task> {
        val base = if (agility || content.genericTasks.isEmpty()) content.tasks
        else content.genericTasks.filter { applies(it, spec) }.map { resolve(it, spec.engineClass) }
            .map { t -> manufacturer[t.id]?.let { withInterval(t, it) } ?: t }
        return base.map { t -> owner[t.id]?.let { withInterval(t, it) } ?: t }
    }

    /** [task] following [interval] instead of its own; its written label no longer applies. */
    fun withInterval(task: Task, interval: Interval): Task = task.copy(
        intervalKm = interval.km,
        intervalMonths = interval.months,
        intervalDays = null,
        breakInKm = interval.breakInKm ?: task.breakInKm.filter { b -> interval.km == null || b < interval.km },
        label = ""
    )

    fun milestonesFor(content: Content, agility: Boolean): List<Milestone> =
        if (agility) content.milestones else emptyList()

    fun checklistFor(content: Content, spec: VehicleSpec, agility: Boolean): List<CheckItem> =
        if (agility || content.genericChecklist.isEmpty()) content.checklist
        else content.genericChecklist.filter { matches(it.requires, spec) }

    fun applies(task: Task, spec: VehicleSpec): Boolean = matches(task.requires, spec)

    private fun matches(requires: Map<String, String>, spec: VehicleSpec): Boolean = requires.all { (field, value) ->
        when (field) {
            "powertrain" -> spec.powertrain.name == value
            "fuelSystem" -> spec.fuelSystem.name == value
            "cooling" -> spec.cooling.name == value
            else -> true
        }
    }

    /** The task with its km interval for [engineClass]. */
    fun resolve(task: Task, engineClass: EngineClass): Task =
        task.kmByClass[engineClass.name]?.let { task.copy(intervalKm = it) } ?: task
}
