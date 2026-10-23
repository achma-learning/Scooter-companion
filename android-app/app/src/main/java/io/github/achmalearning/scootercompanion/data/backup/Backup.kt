package io.github.achmalearning.scootercompanion.data.backup

import kotlinx.serialization.Serializable

/**
 * The portable save file: export on one phone, import on another. Dates are ISO "yyyy-MM-dd".
 *
 * Version 2 (format "scooter-companion") is Scooter Companion's. Version 1 (format
 * "agility-companion") is what Agility Companion and its website export; it still imports,
 * every field added since has a default. Never remove or rename a field: old files must
 * keep importing.
 */
@Serializable
data class BackupFile(
    val format: String = FORMAT,
    val version: Int = VERSION,
    val exportedAt: String = "",
    val bike: BackupBike = BackupBike(),
    val odometer: List<BackupOdometer> = emptyList(),
    val services: List<BackupService> = emptyList(),
    val fuel: List<BackupFuel> = emptyList(),
    val documents: List<BackupDocument> = emptyList(),
    val preRide: BackupPreRide = BackupPreRide(),
    val insurance: List<BackupInsurance> = emptyList(),
    val expenses: List<BackupExpense> = emptyList()
) {
    companion object {
        const val FORMAT = "scooter-companion"
        const val VERSION = 2
        /** Agility Companion's backups (app 2.0.x and the website). */
        const val LEGACY_FORMAT = "agility-companion"
    }
}

@Serializable
data class BackupBike(
    val nickname: String = "",
    val plate: String = "",
    val vin: String = "",
    val engineNo: String = "",
    val color: String = "",
    val purchaseDate: String? = null,
    val purchaseKm: Int = 0,
    val dealer: String = "",
    val warrantyMonths: Int = 24,
    val brand: String = "",
    val model: String = "",
    val engineCc: Int = 0,
    val powertrain: String = "PETROL",
    val fuelSystem: String = "CARBURETOR",
    val cooling: String = "AIR",
    val modelYear: Int? = null,
    val boughtUsed: Boolean = false,
    val purchasePrice: Double? = null,
    val newPrice: Double? = null,
    val firstRegistration: String? = null,
    val previousOwners: Int? = null,
    val monthlyBudget: Double? = null,
    val region: String = "MA",
    /** The owner's scooter photo as base64 JPEG; empty for the default logo. */
    val photoJpeg: String = ""
)

@Serializable data class BackupOdometer(val date: String, val km: Int)

@Serializable
data class BackupService(
    val date: String,
    val km: Int,
    val tasks: List<String> = emptyList(),
    val milestone: Int? = null,
    val shop: String = "",
    val cost: Double? = null,
    val stamped: Boolean = false,
    val notes: String = "",
    val partsCost: Double? = null,
    val laborCost: Double? = null,
    val product: String = "",
    val title: String = ""
)

@Serializable
data class BackupFuel(val date: String, val km: Int, val liters: Double, val cost: Double? = null, val full: Boolean = true)

@Serializable
data class BackupDocument(val type: String, val title: String, val number: String = "", val expiry: String? = null, val notes: String = "")

@Serializable data class BackupPreRide(val last: String? = null)

@Serializable
data class BackupInsurance(
    val start: String,
    val months: Int,
    val totalPaid: Double,
    val insurer: String = "",
    val policyNumber: String = "",
    val notes: String = ""
)

@Serializable
data class BackupExpense(
    val date: String,
    val category: String,
    val title: String,
    val amount: Double,
    val km: Int? = null,
    val quantity: Double? = null,
    val notes: String = ""
)
