package io.github.achmalearning.scootercompanion.domain

import io.github.achmalearning.scootercompanion.data.content.Content
import io.github.achmalearning.scootercompanion.data.content.Milestone
import io.github.achmalearning.scootercompanion.data.content.Task

/**
 * Which maintenance tasks and passport milestones apply to the owner's scooter.
 *
 * The Kymco Agility 50 4T keeps its own detailed schedule (the Marrakech wiki, the dealer
 * passport). Every other scooter gets the generic list, filtered by the profile (carburettor
 * or injection, air or liquid cooling, petrol or electric) with the km interval of its engine
 * class.
 */
object MaintenancePlan {

    fun isAgility50(brand: String, model: String, cc: Int): Boolean =
        brand.trim().equals("Kymco", ignoreCase = true) && model.contains("agility", ignoreCase = true) && cc in 1..50

    fun tasksFor(content: Content, spec: VehicleSpec, agility: Boolean): List<Task> =
        if (agility || content.genericTasks.isEmpty()) content.tasks
        else content.genericTasks.filter { applies(it, spec) }.map { resolve(it, spec.engineClass) }

    fun milestonesFor(content: Content, agility: Boolean): List<Milestone> =
        if (agility) content.milestones else emptyList()

    fun applies(task: Task, spec: VehicleSpec): Boolean = task.requires.all { (field, value) ->
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
