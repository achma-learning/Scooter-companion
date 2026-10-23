package io.github.achmalearning.scootercompanion.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The owner's scooter — a single row (id = 1). Dates are UTC-midnight epoch millis, money is
 * in the region's currency. The type columns are enum names (see domain/Vehicle.kt) so a value
 * added later never breaks an older row.
 */
@Entity(tableName = "bike_profile")
data class BikeProfileEntity(
    @PrimaryKey val id: Int = 1,
    val nickname: String = "",
    val plate: String = "",
    val vin: String = "",
    val engineNo: String = "",
    val color: String = "",
    val purchaseDate: Long? = null,
    val purchaseKm: Int = 0,
    val dealer: String = "",
    val warrantyMonths: Int = 24,
    @ColumnInfo(defaultValue = "") val brand: String = "",
    @ColumnInfo(defaultValue = "") val model: String = "",
    /** Engine displacement; 0 for an electric scooter. */
    @ColumnInfo(defaultValue = "0") val engineCc: Int = 0,
    @ColumnInfo(defaultValue = "PETROL") val powertrain: String = "PETROL",
    @ColumnInfo(defaultValue = "CARBURETOR") val fuelSystem: String = "CARBURETOR",
    @ColumnInfo(defaultValue = "AIR") val cooling: String = "AIR",
    val modelYear: Int? = null,
    @ColumnInfo(defaultValue = "0") val boughtUsed: Boolean = false,
    /** What the owner actually paid, new or used. Counts in the total spent. */
    val purchasePrice: Double? = null,
    /** Used bikes only: the same model's price new. A reference, never counted as spending. */
    val newPrice: Double? = null,
    /** Used bikes only: date of first registration (mise en circulation). */
    val firstRegistration: Long? = null,
    val previousOwners: Int? = null,
    /** File name of the owner's photo in filesDir; empty shows the default logo. */
    @ColumnInfo(defaultValue = "") val photo: String = "",
    /** Monthly spending target the owner tries not to exceed; null = none. */
    val monthlyBudget: Double? = null,
    /** Region pack id (shared/regions/<id>.json); decides currency, fuel chips, SOS numbers. */
    @ColumnInfo(defaultValue = "MA") val region: String = "MA"
)

@Entity(tableName = "odometer_readings")
data class OdometerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val km: Int
)

@Entity(tableName = "fuel_entries")
data class FuelEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val km: Int,
    val liters: Double,
    val cost: Double? = null,
    val full: Boolean = true
)

/** Insurance certificate, carte grise, licence, warranty… anything with an expiry date. */
@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val title: String,
    val number: String = "",
    val expiry: Long? = null,
    val notes: String = ""
)

/**
 * One insurance contract: when it starts, for how many months, and what the owner actually
 * paid for it. The end date is derived (see domain/Insurance.kt), never stored.
 */
@Entity(tableName = "insurance_policies")
data class InsurancePolicyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startDate: Long,
    val months: Int,
    val totalPaid: Double,
    val insurer: String = "",
    val policyNumber: String = "",
    val notes: String = ""
)

/**
 * Any other money spent on the scooter: accessories, riding gear, fines, parking, washing,
 * papers, charging, anything custom. [category] is an ExpenseCategory name; [quantity] is an
 * optional amount of something (kWh charged, items bought).
 */
@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val category: String,
    val title: String,
    val amount: Double,
    val km: Int? = null,
    val quantity: Double? = null,
    val notes: String = ""
)
