package com.example.kymcoagility50.domain

data class FuelFill(val date: Long, val km: Int, val liters: Double, val cost: Double?, val full: Boolean)

data class FuelSummary(
    /** Average over full-to-full segments, null until two full fill-ups exist. */
    val litersPer100: Double?,
    val lastLitersPer100: Double?,
    val costPerKm: Double?,
    val totalCost: Double,
    val totalLiters: Double,
    val distance: Int
)

object FuelStats {
    /**
     * Full-tank method: consumption between two full fill-ups = litres added after the first
     * one (including partial top-ups in between) ÷ distance. Partial fills alone can't be
     * measured.
     */
    fun summarize(fills: List<FuelFill>): FuelSummary {
        val sorted = fills.sortedWith(compareBy<FuelFill> { it.km }.thenBy { it.date })
        var segLiters = 0.0
        var segDistance = 0
        var lastSegment: Double? = null
        var anchor: FuelFill? = null
        var pending = 0.0
        for (f in sorted) {
            if (anchor == null) {
                if (f.full) anchor = f
                continue
            }
            pending += f.liters
            if (f.full) {
                val d = f.km - anchor.km
                if (d > 0) {
                    segLiters += pending
                    segDistance += d
                    lastSegment = pending * 100 / d
                }
                anchor = f
                pending = 0.0
            }
        }
        val totalCost = sorted.sumOf { it.cost ?: 0.0 }
        val distance = if (sorted.size >= 2) sorted.last().km - sorted.first().km else 0
        // Cost of the first fill bought fuel used *before* the log started, so exclude it.
        val costAfterFirst = sorted.drop(1).sumOf { it.cost ?: 0.0 }
        return FuelSummary(
            litersPer100 = if (segDistance > 0) segLiters * 100 / segDistance else null,
            lastLitersPer100 = lastSegment,
            costPerKm = if (distance > 0 && costAfterFirst > 0) costAfterFirst / distance else null,
            totalCost = totalCost,
            totalLiters = sorted.sumOf { it.liters },
            distance = distance
        )
    }
}
