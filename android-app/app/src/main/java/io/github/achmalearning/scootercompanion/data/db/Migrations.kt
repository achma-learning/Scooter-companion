package io.github.achmalearning.scootercompanion.data.db

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

/**
 * v2 → v3 (Scooter Companion): the profile gains the scooter's type, purchase and budget
 * columns, the logbook entry gains parts / labour / product / custom title, and the insurance
 * and expense tables are added. A v2 database only ever held a Kymco Agility 50 4T (the app
 * was built for that one scooter), so an existing profile row is filled in as that bike.
 * SQL copied from the exported schema in app/schemas/.../3.json.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `maintenance_entries` ADD COLUMN `partsCost` REAL")
        db.execSQL("ALTER TABLE `maintenance_entries` ADD COLUMN `laborCost` REAL")
        db.execSQL("ALTER TABLE `maintenance_entries` ADD COLUMN `product` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `maintenance_entries` ADD COLUMN `title` TEXT NOT NULL DEFAULT ''")
        listOf(
            "`brand` TEXT NOT NULL DEFAULT ''",
            "`model` TEXT NOT NULL DEFAULT ''",
            "`engineCc` INTEGER NOT NULL DEFAULT 0",
            "`powertrain` TEXT NOT NULL DEFAULT 'PETROL'",
            "`fuelSystem` TEXT NOT NULL DEFAULT 'CARBURETOR'",
            "`cooling` TEXT NOT NULL DEFAULT 'AIR'",
            "`modelYear` INTEGER",
            "`boughtUsed` INTEGER NOT NULL DEFAULT 0",
            "`purchasePrice` REAL",
            "`newPrice` REAL",
            "`firstRegistration` INTEGER",
            "`previousOwners` INTEGER",
            "`photo` TEXT NOT NULL DEFAULT ''",
            "`monthlyBudget` REAL",
            "`region` TEXT NOT NULL DEFAULT 'MA'"
        ).forEach { db.execSQL("ALTER TABLE `bike_profile` ADD COLUMN $it") }
        db.execSQL("UPDATE `bike_profile` SET `brand` = 'Kymco', `model` = 'Agility 50 4T', `engineCc` = 50")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `insurance_policies` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`startDate` INTEGER NOT NULL, `months` INTEGER NOT NULL, `totalPaid` REAL NOT NULL, " +
                "`insurer` TEXT NOT NULL, `policyNumber` TEXT NOT NULL, `notes` TEXT NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `expenses` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`date` INTEGER NOT NULL, `category` TEXT NOT NULL, `title` TEXT NOT NULL, `amount` REAL NOT NULL, " +
                "`km` INTEGER, `quantity` REAL, `notes` TEXT NOT NULL)"
        )
    }
}

/**
 * v3 → v4: the owner's own maintenance intervals (task_intervals). Nothing else changes.
 * SQL copied from the exported schema in app/schemas/.../4.json.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `task_intervals` (`taskId` TEXT NOT NULL, `intervalKm` INTEGER, " +
                "`intervalMonths` INTEGER, PRIMARY KEY(`taskId`))"
        )
    }
}
