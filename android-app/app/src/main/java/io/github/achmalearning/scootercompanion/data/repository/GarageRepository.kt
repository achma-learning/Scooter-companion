package io.github.achmalearning.scootercompanion.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.withTransaction
import io.github.achmalearning.scootercompanion.data.backup.BackupBike
import io.github.achmalearning.scootercompanion.data.backup.BackupDocument
import io.github.achmalearning.scootercompanion.data.backup.BackupFile
import io.github.achmalearning.scootercompanion.data.backup.BackupFuel
import io.github.achmalearning.scootercompanion.data.backup.BackupOdometer
import io.github.achmalearning.scootercompanion.data.backup.BackupPreRide
import io.github.achmalearning.scootercompanion.data.backup.BackupService
import io.github.achmalearning.scootercompanion.data.content.Content
import io.github.achmalearning.scootercompanion.data.db.BikeProfileEntity
import io.github.achmalearning.scootercompanion.data.db.DocumentEntity
import io.github.achmalearning.scootercompanion.data.db.FuelEntity
import io.github.achmalearning.scootercompanion.data.db.MaintenanceEntryEntity
import io.github.achmalearning.scootercompanion.data.db.ManualDatabase
import io.github.achmalearning.scootercompanion.data.db.OdometerEntity
import io.github.achmalearning.scootercompanion.data.db.taskIds
import io.github.achmalearning.scootercompanion.domain.Dates
import io.github.achmalearning.scootercompanion.domain.FuelFill
import io.github.achmalearning.scootercompanion.domain.FuelStats
import io.github.achmalearning.scootercompanion.domain.FuelSummary
import io.github.achmalearning.scootercompanion.domain.MilestoneStatus
import io.github.achmalearning.scootercompanion.domain.ServicePlanner
import io.github.achmalearning.scootercompanion.domain.ServiceRecord
import io.github.achmalearning.scootercompanion.domain.TaskStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val Context.garageStore by preferencesDataStore(name = "garage_prefs")

/** Everything the dashboard needs, recomputed whenever any garage table changes. */
data class GarageSnapshot(
    val profile: BikeProfileEntity,
    val hasProfile: Boolean,
    val currentKm: Int,
    val entries: List<MaintenanceEntryEntity>,
    val odometer: List<OdometerEntity>,
    val fuel: List<FuelEntity>,
    val documents: List<DocumentEntity>,
    val statuses: List<TaskStatus>,
    val milestones: List<MilestoneStatus>,
    val health: Int,
    val fuelSummary: FuelSummary,
    val lastPreRide: Long?,
    val today: Long
) {
    val warrantyEnd: Long? get() = profile.purchaseDate?.let { Dates.addMonths(it, profile.warrantyMonths) }
}

/**
 * The owner's side of the app: bike profile, odometer, fuel log, documents, pre-ride check.
 * All local-only, in both modes — the logbook (MaintenanceRepository) is still the only data
 * Sync Mode ever uploads.
 */
class GarageRepository(
    private val context: Context,
    private val db: ManualDatabase,
    private val contentProvider: () -> Content
) {
    private val dao = db.garageDao()
    private val logDao = db.maintenanceEntryDao()
    private val PRE_RIDE_KEY = longPreferencesKey("last_pre_ride")
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true; encodeDefaults = true }

    val lastPreRide: Flow<Long?> = context.garageStore.data.map { it[PRE_RIDE_KEY] }

    val snapshot: Flow<GarageSnapshot> = combine(
        combine(dao.observeProfile(), logDao.observeAll(), dao.observeOdometer(), ::Triple),
        combine(dao.observeFuel(), dao.observeDocuments(), lastPreRide, ::Triple)
    ) { (profile, entries, odometer), (fuel, documents, preRide) ->
        build(profile, entries, odometer, fuel, documents, preRide)
    }

    private fun build(
        profileOrNull: BikeProfileEntity?,
        entries: List<MaintenanceEntryEntity>,
        odometer: List<OdometerEntity>,
        fuel: List<FuelEntity>,
        documents: List<DocumentEntity>,
        preRide: Long?
    ): GarageSnapshot {
        val content = contentProvider()
        val profile = profileOrNull ?: BikeProfileEntity()
        val today = Dates.today()
        val records = entries.map { it.toRecord() }
        val currentKm = ServicePlanner.currentKm(
            profile.purchaseKm, entries.map { it.km }, odometer.map { it.km }, fuel.map { it.km }
        )
        val statuses = ServicePlanner.statuses(
            content.tasks, records, currentKm, today, profile.purchaseKm, profile.purchaseDate
        )
        return GarageSnapshot(
            profile = profile,
            hasProfile = profileOrNull != null,
            currentKm = currentKm,
            entries = entries,
            odometer = odometer,
            fuel = fuel,
            documents = documents,
            statuses = statuses,
            milestones = ServicePlanner.milestones(content.milestones, records, currentKm),
            health = ServicePlanner.healthScore(statuses),
            fuelSummary = FuelStats.summarize(fuel.map { FuelFill(it.date, it.km, it.liters, it.cost, it.full) }),
            lastPreRide = preRide,
            today = today
        )
    }

    suspend fun current(): GarageSnapshot = snapshot.first()

    suspend fun saveProfile(profile: BikeProfileEntity) = dao.upsertProfile(profile.copy(id = 1))

    suspend fun addOdometer(km: Int, date: Long = Dates.today()) = dao.insertOdometer(OdometerEntity(date = date, km = km))

    suspend fun saveFuel(entry: FuelEntity) = dao.upsertFuel(entry)
    suspend fun deleteFuel(entry: FuelEntity) = dao.deleteFuel(entry)

    suspend fun saveDocument(doc: DocumentEntity) = dao.upsertDocument(doc)
    suspend fun deleteDocument(doc: DocumentEntity) = dao.deleteDocument(doc)

    suspend fun markPreRideDone(day: Long = Dates.today()) {
        context.garageStore.edit { it[PRE_RIDE_KEY] = day }
    }

    // ---------------------------------------------------------------- backup

    suspend fun exportJson(): String {
        val s = current()
        val p = s.profile
        val file = BackupFile(
            exportedAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                .apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date()),
            bike = BackupBike(p.nickname, p.plate, p.vin, p.engineNo, p.color, p.purchaseDate?.let(Dates::toIso), p.purchaseKm, p.dealer, p.warrantyMonths),
            odometer = s.odometer.map { BackupOdometer(Dates.toIso(it.date), it.km) },
            services = s.entries.map {
                BackupService(Dates.toIso(Dates.startOfDay(it.date)), it.km, it.taskIds.toList(), it.milestoneKm, it.shop, it.cost, it.stamped, it.note)
            },
            fuel = s.fuel.map { BackupFuel(Dates.toIso(it.date), it.km, it.liters, it.cost, it.full) },
            documents = s.documents.map { BackupDocument(it.type, it.title, it.number, it.expiry?.let(Dates::toIso), it.notes) },
            preRide = BackupPreRide(s.lastPreRide?.let(Dates::toIso))
        )
        return json.encodeToString(BackupFile.serializer(), file)
    }

    /** Replaces the whole garage with the backup's content. Returns a short summary. */
    suspend fun importJson(text: String, maintenance: MaintenanceRepository): String {
        val file = json.decodeFromString(BackupFile.serializer(), text)
        require(file.format == BackupFile.FORMAT) { "Not an Agility Companion backup" }
        db.withTransaction {
            dao.clearProfile(); dao.clearOdometer(); dao.clearFuel(); dao.clearDocuments()
            val b = file.bike
            dao.upsertProfile(
                BikeProfileEntity(1, b.nickname, b.plate, b.vin, b.engineNo, b.color, Dates.fromIso(b.purchaseDate), b.purchaseKm, b.dealer, b.warrantyMonths)
            )
            dao.insertAllOdometer(file.odometer.mapNotNull { o -> Dates.fromIso(o.date)?.let { OdometerEntity(date = it, km = o.km) } })
            dao.insertAllFuel(file.fuel.mapNotNull { f -> Dates.fromIso(f.date)?.let { FuelEntity(date = it, km = f.km, liters = f.liters, cost = f.cost, full = f.full) } })
            dao.insertAllDocuments(file.documents.map { DocumentEntity(type = it.type, title = it.title, number = it.number, expiry = Dates.fromIso(it.expiry), notes = it.notes) })
        }
        maintenance.replaceAll(file.services.mapNotNull { s ->
            Dates.fromIso(s.date)?.let { day ->
                MaintenanceEntryEntity(
                    km = s.km, date = day, note = s.notes, tasks = s.tasks.joinToString(","), milestoneKm = s.milestone,
                    shop = s.shop, cost = s.cost, stamped = s.stamped, updatedAt = System.currentTimeMillis(), dirty = true
                )
            }
        })
        context.garageStore.edit { prefs ->
            val last = Dates.fromIso(file.preRide.last)
            if (last != null) prefs[PRE_RIDE_KEY] = last else prefs.remove(PRE_RIDE_KEY)
        }
        return "${file.services.size} services, ${file.fuel.size} fill-ups, ${file.documents.size} documents imported"
    }

    suspend fun wipeGarage() {
        db.withTransaction { dao.clearProfile(); dao.clearOdometer(); dao.clearFuel(); dao.clearDocuments() }
        context.garageStore.edit { it.remove(PRE_RIDE_KEY) }
    }
}

fun MaintenanceEntryEntity.toRecord() = ServiceRecord(
    date = Dates.startOfDay(date), km = km, tasks = taskIds, milestoneKm = milestoneKm, stamped = stamped, shop = shop
)
