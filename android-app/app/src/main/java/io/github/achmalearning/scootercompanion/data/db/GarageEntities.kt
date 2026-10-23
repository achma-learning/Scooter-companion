package io.github.achmalearning.scootercompanion.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** The owner's scooter identity — a single row (id = 1). Dates are UTC-midnight epoch millis. */
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
    val warrantyMonths: Int = 24
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
