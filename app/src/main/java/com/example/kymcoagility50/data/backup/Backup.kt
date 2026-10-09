package com.example.kymcoagility50.data.backup

import kotlinx.serialization.Serializable

/**
 * Portable garage backup — the exact JSON the website exports/imports (web/js/store.js), so
 * the owner can move between the app and the site. Dates are ISO "yyyy-MM-dd".
 */
@Serializable
data class BackupFile(
    val format: String = FORMAT,
    val version: Int = 1,
    val exportedAt: String = "",
    val bike: BackupBike = BackupBike(),
    val odometer: List<BackupOdometer> = emptyList(),
    val services: List<BackupService> = emptyList(),
    val fuel: List<BackupFuel> = emptyList(),
    val documents: List<BackupDocument> = emptyList(),
    val preRide: BackupPreRide = BackupPreRide()
) {
    companion object { const val FORMAT = "agility-companion" }
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
    val warrantyMonths: Int = 24
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
    val notes: String = ""
)

@Serializable
data class BackupFuel(val date: String, val km: Int, val liters: Double, val cost: Double? = null, val full: Boolean = true)

@Serializable
data class BackupDocument(val type: String, val title: String, val number: String = "", val expiry: String? = null, val notes: String = "")

@Serializable data class BackupPreRide(val last: String? = null)
