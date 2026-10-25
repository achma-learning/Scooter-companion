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

    @Query("SELECT * FROM insurance_policies ORDER BY startDate DESC")
    fun observeInsurance(): Flow<List<InsurancePolicyEntity>>

    @Upsert
    suspend fun upsertInsurance(policy: InsurancePolicyEntity): Long

    @Delete
    suspend fun deleteInsurance(policy: InsurancePolicyEntity)

    @Query("SELECT * FROM expenses ORDER BY date DESC, id DESC")
    fun observeExpenses(): Flow<List<ExpenseEntity>>

    @Upsert
    suspend fun upsertExpense(expense: ExpenseEntity): Long

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    @Query("SELECT * FROM task_intervals")
    fun observeIntervals(): Flow<List<TaskIntervalEntity>>

    @Upsert
    suspend fun upsertInterval(interval: TaskIntervalEntity)

    @Query("DELETE FROM task_intervals WHERE taskId = :taskId")
    suspend fun deleteInterval(taskId: String)

    // --- Backup import (replace-all) ---

    @Query("DELETE FROM bike_profile")
    suspend fun clearProfile()

    @Query("DELETE FROM odometer_readings")
    suspend fun clearOdometer()

    @Query("DELETE FROM fuel_entries")
    suspend fun clearFuel()

    @Query("DELETE FROM documents")
    suspend fun clearDocuments()

    @Query("DELETE FROM insurance_policies")
    suspend fun clearInsurance()

    @Query("DELETE FROM expenses")
    suspend fun clearExpenses()

    @Query("DELETE FROM task_intervals")
    suspend fun clearIntervals()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllOdometer(items: List<OdometerEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllFuel(items: List<FuelEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllDocuments(items: List<DocumentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllInsurance(items: List<InsurancePolicyEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllExpenses(items: List<ExpenseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllIntervals(items: List<TaskIntervalEntity>)
}
