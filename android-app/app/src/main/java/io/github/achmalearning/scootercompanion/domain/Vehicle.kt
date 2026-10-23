package io.github.achmalearning.scootercompanion.domain

/** How the scooter is powered. Electric scooters have no fuel log and no oil tasks. */
enum class Powertrain { PETROL, ELECTRIC }

/** Carburettor scooters need carb cleaning; injection ones get a throttle-body clean instead. */
enum class FuelSystem { CARBURETOR, INJECTION }

/** Liquid-cooled engines add coolant checks, coolant changes and radiator care. */
enum class Cooling { AIR, LIQUID }

/**
 * The size class that tunes defaults: fuel amount chips, insurance reference price,
 * recommended top-case size, maintenance intervals.
 */
enum class EngineClass {
    ELECTRIC,
    /** Up to 50 cc. */
    CC50,
    /** 51 – 125 cc. */
    CC125,
    /** 126 – 249 cc. */
    MID,
    /** Maxi-scooters, 250 cc and up. */
    MAXI;

    companion object {
        fun of(powertrain: Powertrain, cc: Int): EngineClass = when {
            powertrain == Powertrain.ELECTRIC -> ELECTRIC
            cc <= 50 -> CC50
            cc <= 125 -> CC125
            cc < 250 -> MID
            else -> MAXI
        }
    }
}

/** The scooter's type, read from the profile's stored enum names. */
data class VehicleSpec(
    val powertrain: Powertrain = Powertrain.PETROL,
    val cc: Int = 50,
    val fuelSystem: FuelSystem = FuelSystem.CARBURETOR,
    val cooling: Cooling = Cooling.AIR
) {
    val engineClass: EngineClass get() = EngineClass.of(powertrain, cc)
    val isElectric: Boolean get() = powertrain == Powertrain.ELECTRIC

    companion object {
        fun parse(powertrain: String, cc: Int, fuelSystem: String, cooling: String) = VehicleSpec(
            powertrain = enumOr(powertrain, Powertrain.PETROL),
            cc = cc,
            fuelSystem = enumOr(fuelSystem, FuelSystem.CARBURETOR),
            cooling = enumOr(cooling, Cooling.AIR)
        )
    }
}

/** Parses a stored enum name, falling back when it is unknown (e.g. written by a newer app). */
inline fun <reified E : Enum<E>> enumOr(name: String?, fallback: E): E =
    enumValues<E>().firstOrNull { it.name == name } ?: fallback
