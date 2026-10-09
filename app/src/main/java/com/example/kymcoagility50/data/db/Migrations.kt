package com.example.kymcoagility50.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v1 → v2: the logbook entry gains task ids / milestone / shop / cost / dealer-stamp, and the
 * garage tables (profile, odometer, fuel, documents) are added. Existing entries are kept.
 * SQL copied from the exported schema in app/schemas/.../2.json.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `maintenance_entries` ADD COLUMN `tasks` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `maintenance_entries` ADD COLUMN `milestoneKm` INTEGER")
        db.execSQL("ALTER TABLE `maintenance_entries` ADD COLUMN `shop` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `maintenance_entries` ADD COLUMN `cost` REAL")
        db.execSQL("ALTER TABLE `maintenance_entries` ADD COLUMN `stamped` INTEGER NOT NULL DEFAULT 0")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `bike_profile` (`id` INTEGER NOT NULL, `nickname` TEXT NOT NULL, " +
                "`plate` TEXT NOT NULL, `vin` TEXT NOT NULL, `engineNo` TEXT NOT NULL, `color` TEXT NOT NULL, " +
                "`purchaseDate` INTEGER, `purchaseKm` INTEGER NOT NULL, `dealer` TEXT NOT NULL, " +
                "`warrantyMonths` INTEGER NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `odometer_readings` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`date` INTEGER NOT NULL, `km` INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `fuel_entries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`date` INTEGER NOT NULL, `km` INTEGER NOT NULL, `liters` REAL NOT NULL, `cost` REAL, `full` INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `documents` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`type` TEXT NOT NULL, `title` TEXT NOT NULL, `number` TEXT NOT NULL, `expiry` INTEGER, `notes` TEXT NOT NULL)"
        )
    }
}
