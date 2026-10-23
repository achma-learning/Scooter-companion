package io.github.achmalearning.scootercompanion.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface GarageDao {
    @Query("SELECT * FROM bike_profile WHERE id = 1")
    fun observeProfile(): Flow<BikeProfileEntity?>

    @Upsert
    suspend fun upsertProfile(profile: BikeProfileEntity)

    @Query("SELECT * FROM odometer_readings ORDER BY km DESC, date DESC")
    fun observeOdometer(): Flow<List<OdometerEntity>>

    @Insert
    suspend fun insertOdometer(reading: OdometerEntity): Long

    @Query("SELECT * FROM fuel_entries ORDER BY km DESC, date DESC")
    fun observeFuel(): Flow<List<FuelEntity>>

    @Upsert
    suspend fun upsertFuel(entry: FuelEntity): Long

    @Delete
    suspend fun deleteFuel(entry: FuelEntity)

    @Query("SELECT * FROM documents ORDER BY expiry IS NULL, expiry ASC")
    fun observeDocuments(): Flow<List<DocumentEntity>>

    @Upsert
    suspend fun upsertDocument(doc: DocumentEntity): Long

    @Delete
    suspend fun deleteDocument(doc: DocumentEntity)

    // --- Backup import (replace-all) ---

    @Query("DELETE FROM bike_profile")
    suspend fun clearProfile()

    @Query("DELETE FROM odometer_readings")
    suspend fun clearOdometer()

    @Query("DELETE FROM fuel_entries")
    suspend fun clearFuel()

    @Query("DELETE FROM documents")
    suspend fun clearDocuments()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllOdometer(items: List<OdometerEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllFuel(items: List<FuelEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllDocuments(items: List<DocumentEntity>)
}
