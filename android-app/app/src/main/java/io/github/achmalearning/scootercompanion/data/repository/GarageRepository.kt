package io.github.achmalearning.scootercompanion.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.withTransaction
import io.github.achmalearning.scootercompanion.data.backup.BackupBike
import io.github.achmalearning.scootercompanion.data.backup.BackupDocument
import io.github.achmalearning.scootercompanion.data.backup.BackupExpense
import io.github.achmalearning.scootercompanion.data.backup.BackupFile
import io.github.achmalearning.scootercompanion.data.backup.BackupFuel
import io.github.achmalearning.scootercompanion.data.backup.BackupInsurance
import io.github.achmalearning.scootercompanion.data.backup.BackupInterval
import io.github.achmalearning.scootercompanion.data.backup.BackupOdometer
import io.github.achmalearning.scootercompanion.data.backup.BackupPreRide
import io.github.achmalearning.scootercompanion.data.backup.BackupService
import io.github.achmalearning.scootercompanion.data.content.Content
import io.github.achmalearning.scootercompanion.data.content.Task
import io.github.achmalearning.scootercompanion.data.db.BikeProfileEntity
import io.github.achmalearning.scootercompanion.data.db.DocumentEntity
import io.github.achmalearning.scootercompanion.data.db.ExpenseEntity
import io.github.achmalearning.scootercompanion.data.db.InsurancePolicyEntity
import io.github.achmalearning.scootercompanion.data.db.FuelEntity
import io.github.achmalearning.scootercompanion.data.db.MaintenanceEntryEntity
import io.github.achmalearning.scootercompanion.data.db.ManualDatabase
import io.github.achmalearning.scootercompanion.data.db.OdometerEntity
import io.github.achmalearning.scootercompanion.data.db.TaskIntervalEntity
import io.github.achmalearning.scootercompanion.data.db.taskIds
import io.github.achmalearning.scootercompanion.data.models.ModelCatalog
import io.github.achmalearning.scootercompanion.data.models.ScooterModel
import io.github.achmalearning.scootercompanion.data.region.RegionPack
import io.github.achmalearning.scootercompanion.ui.components.Money
import io.github.achmalearning.scootercompanion.domain.Coverage
import io.github.achmalearning.scootercompanion.domain.Dates
import io.github.achmalearning.scootercompanion.domain.ExpenseLedger
import io.github.achmalearning.scootercompanion.domain.FuelFill
import io.github.achmalearning.scootercompanion.domain.FuelStats
import io.github.achmalearning.scootercompanion.domain.FuelSummary
import io.github.achmalearning.scootercompanion.domain.Insurance
import io.github.achmalearning.scootercompanion.domain.InsurancePolicy
import io.github.achmalearning.scootercompanion.domain.Interval
import io.github.achmalearning.scootercompanion.domain.LedgerStats
import io.github.achmalearning.scootercompanion.domain.MaintenancePlan
import io.github.achmalearning.scootercompanion.domain.MilestoneStatus
import io.github.achmalearning.scootercompanion.domain.ServicePlanner
import io.github.achmalearning.scootercompanion.domain.ServiceRecord
import io.github.achmalearning.scootercompanion.domain.Spend
import io.github.achmalearning.scootercompanion.domain.SpendCategory
import io.github.achmalearning.scootercompanion.domain.SpendSource
import io.github.achmalearning.scootercompanion.domain.TaskStatus
import io.github.achmalearning.scootercompanion.domain.VehicleSpec
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
    val insurance: List<InsurancePolicyEntity>,
    val expenses: List<ExpenseEntity>,
    val statuses: List<TaskStatus>,
    val milestones: List<MilestoneStatus>,
    /** The maintenance tasks that apply to this scooter, intervals resolved for its class. */
    val tasks: List<Task>,
    /** The Kymco Agility 50 4T: its own schedule, dealer passport and manual. */
    val isAgility: Boolean,
    val health: Int,
    val fuelSummary: FuelSummary,
    val lastPreRide: Long?,
    val today: Long,
    /** Every payment from every module, oldest first. */
    val spends: List<Spend>,
    val ledger: LedgerStats,
    val coverage: Coverage,
    val region: RegionPack = RegionPack.GENERIC,
    /** The scooter's model in scooter-data, when the catalogue knows it. */
    val model: ScooterModel? = null,
    /** The owner's own intervals, by task id. */
    val intervals: Map<String, TaskIntervalEntity> = emptyMap(),
    /** The tasks as the manufacturer or the app would schedule them, before the owner's intervals. */
    val defaultTasks: List<Task> = tasks
) {
    val warrantyEnd: Long? get() = profile.purchaseDate?.let { Dates.addMonths(it, profile.warrantyMonths) }
    val spec: VehicleSpec get() = VehicleSpec.parse(profile.powertrain, profile.engineCc, profile.fuelSystem, profile.cooling)
}

/**
 * The owner's side of the app: scooter profile, odometer, fuel log, documents, insurance,
 * expenses, pre-ride check, and the spending ledger built from all of them. Local-only.
 */
class GarageRepository(
    private val context: Context,
    private val db: ManualDatabase,
    private val regionProvider: (String) -> RegionPack = { RegionPack.GENERIC },
    private val models: ModelCatalog? = null,
    private val contentProvider: () -> Content
) {
    private val dao = db.garageDao()
    private val logDao = db.maintenanceEntryDao()
    private val PRE_RIDE_KEY = longPreferencesKey("last_pre_ride")
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true; encodeDefaults = true }
    val photos = PhotoStore(context)

    val lastPreRide: Flow<Long?> = context.garageStore.data.map { it[PRE_RIDE_KEY] }

    val snapshot: Flow<GarageSnapshot> = combine(
        combine(dao.observeProfile(), logDao.observeAll(), dao.observeOdometer(), ::Triple),
        combine(dao.observeFuel(), dao.observeDocuments(), lastPreRide, ::Triple),
        combine(dao.observeInsurance(), dao.observeExpenses(), dao.observeIntervals(), ::Triple)
    ) { (profile, entries, odometer), (fuel, documents, preRide), (insurance, expenses, intervals) ->
        build(profile, entries, odometer, fuel, documents, preRide, insurance, expenses, intervals)
    }

    private fun build(
        profileOrNull: BikeProfileEntity?,
        entries: List<MaintenanceEntryEntity>,
        odometer: List<OdometerEntity>,
        fuel: List<FuelEntity>,
        documents: List<DocumentEntity>,
        preRide: Long?,
        insurance: List<InsurancePolicyEntity> = emptyList(),
        expenses: List<ExpenseEntity> = emptyList(),
        intervals: List<TaskIntervalEntity> = emptyList()
    ): GarageSnapshot {
        val content = contentProvider()
        val profile = profileOrNull ?: BikeProfileEntity()
        val region = regionProvider(profile.region)
        // Plain money formatting (formatMoney) reads the symbol from here.
        Money.symbol = region.currencySymbol
        val today = Dates.today()
        val records = entries.map { it.toRecord() }
        val currentKm = ServicePlanner.currentKm(
            profile.purchaseKm, entries.map { it.km }, odometer.map { it.km }, fuel.map { it.km }
        )
        val spec = VehicleSpec.parse(profile.powertrain, profile.engineCc, profile.fuelSystem, profile.cooling)
        val model = models?.match(profile.brand, profile.model, profile.engineCc)
        val agility = model?.id == ModelCatalog.AGILITY_50 || MaintenancePlan.isAgility50(profile.brand, profile.model, profile.engineCc)
        val manufacturer = model?.schedule.orEmpty().mapValues { (_, v) -> Interval(v.km, v.months, v.breakInKm) }
        val own = intervals.associateBy { it.taskId }
        val defaultTasks = MaintenancePlan.tasksFor(content, spec, agility, manufacturer)
        val tasks = MaintenancePlan.tasksFor(content, spec, agility, manufacturer, own.mapValues { (_, v) -> Interval(v.intervalKm, v.intervalMonths) })
        val statuses = ServicePlanner.statuses(
            tasks, records, currentKm, today, profile.purchaseKm, profile.purchaseDate
        )
        val spends = spendsOf(profile, entries, fuel, insurance, expenses)
        val ownedSince = profile.purchaseDate ?: spends.minOfOrNull { it.date }
        return GarageSnapshot(
            profile = profile,
            hasProfile = profileOrNull != null,
            currentKm = currentKm,
            entries = entries,
            odometer = odometer,
            fuel = fuel,
            documents = documents,
            insurance = insurance,
            expenses = expenses,
            statuses = statuses,
            milestones = ServicePlanner.milestones(MaintenancePlan.milestonesFor(content, agility), records, currentKm),
            tasks = tasks,
            isAgility = agility,
            health = ServicePlanner.healthScore(statuses),
            fuelSummary = FuelStats.summarize(fuel.map { FuelFill(it.date, it.km, it.liters, it.cost, it.full) }),
            lastPreRide = preRide,
            today = today,
            spends = spends,
            ledger = ExpenseLedger.stats(
                spends, today, ownedSince, profile.monthlyBudget, kmDriven = currentKm - profile.purchaseKm
            ),
            coverage = Insurance.coverage(insurance.map { it.toPolicy() }, today),
            region = region,
            model = model,
            intervals = own,
            defaultTasks = defaultTasks
        )
    }

    suspend fun current(): GarageSnapshot = snapshot.first()

    suspend fun saveProfile(profile: BikeProfileEntity) = dao.upsertProfile(profile.copy(id = 1))

    suspend fun addOdometer(km: Int, date: Long = Dates.today()) = dao.insertOdometer(OdometerEntity(date = date, km = km))

    suspend fun saveFuel(entry: FuelEntity) = dao.upsertFuel(entry)
    suspend fun deleteFuel(entry: FuelEntity) = dao.deleteFuel(entry)

    suspend fun saveDocument(doc: DocumentEntity) = dao.upsertDocument(doc)
    suspend fun deleteDocument(doc: DocumentEntity) = dao.deleteDocument(doc)

    suspend fun saveInsurance(policy: InsurancePolicyEntity) = dao.upsertInsurance(policy)
    suspend fun deleteInsurance(policy: InsurancePolicyEntity) = dao.deleteInsurance(policy)

    suspend fun saveExpense(expense: ExpenseEntity) = dao.upsertExpense(expense)
    suspend fun deleteExpense(expense: ExpenseEntity) = dao.deleteExpense(expense)

    /** The owner's own interval for [taskId]; both null goes back to the default. */
    suspend fun setInterval(taskId: String, km: Int?, months: Int?) =
        if (km == null && months == null) dao.deleteInterval(taskId) else dao.upsertInterval(TaskIntervalEntity(taskId, km, months))

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
            bike = BackupBike(
                nickname = p.nickname, plate = p.plate, vin = p.vin, engineNo = p.engineNo, color = p.color,
                purchaseDate = p.purchaseDate?.let(Dates::toIso), purchaseKm = p.purchaseKm, dealer = p.dealer,
                warrantyMonths = p.warrantyMonths, brand = p.brand, model = p.model, engineCc = p.engineCc,
                powertrain = p.powertrain, fuelSystem = p.fuelSystem, cooling = p.cooling, modelYear = p.modelYear,
                boughtUsed = p.boughtUsed, purchasePrice = p.purchasePrice, newPrice = p.newPrice,
                firstRegistration = p.firstRegistration?.let(Dates::toIso), previousOwners = p.previousOwners,
                monthlyBudget = p.monthlyBudget, region = p.region,
                photoJpeg = if (p.photo.isNotBlank()) photos.toBase64() else ""
            ),
            odometer = s.odometer.map { BackupOdometer(Dates.toIso(it.date), it.km) },
            services = s.entries.map {
                BackupService(
                    Dates.toIso(Dates.startOfDay(it.date)), it.km, it.taskIds.toList(), it.milestoneKm, it.shop, it.cost,
                    it.stamped, it.note, it.partsCost, it.laborCost, it.product, it.title
                )
            },
            fuel = s.fuel.map { BackupFuel(Dates.toIso(it.date), it.km, it.liters, it.cost, it.full) },
            documents = s.documents.map { BackupDocument(it.type, it.title, it.number, it.expiry?.let(Dates::toIso), it.notes) },
            preRide = BackupPreRide(s.lastPreRide?.let(Dates::toIso)),
            insurance = s.insurance.map { BackupInsurance(Dates.toIso(it.startDate), it.months, it.totalPaid, it.insurer, it.policyNumber, it.notes) },
            expenses = s.expenses.map { BackupExpense(Dates.toIso(it.date), it.category, it.title, it.amount, it.km, it.quantity, it.notes) },
            intervals = s.intervals.values.map { BackupInterval(it.taskId, it.intervalKm, it.intervalMonths) }
        )
        return json.encodeToString(BackupFile.serializer(), file)
    }

    /**
     * Replaces the whole garage with the backup's content. Reads Scooter Companion files and
     * Agility Companion ones (which only ever described a Kymco Agility 50 4T). Returns a
     * short summary.
     */
    suspend fun importJson(text: String, maintenance: MaintenanceRepository): String {
        val file = json.decodeFromString(BackupFile.serializer(), text)
        require(file.format == BackupFile.FORMAT || file.format == BackupFile.LEGACY_FORMAT) { "Not a Scooter Companion backup" }
        val b = file.bike.let {
            if (file.format == BackupFile.LEGACY_FORMAT && it.brand.isBlank()) it.copy(brand = "Kymco", model = "Agility 50 4T", engineCc = 50) else it
        }
        val photo = photos.restore(b.photoJpeg)
        db.withTransaction {
            dao.clearProfile(); dao.clearOdometer(); dao.clearFuel(); dao.clearDocuments(); dao.clearInsurance(); dao.clearExpenses(); dao.clearIntervals()
            dao.upsertProfile(
                BikeProfileEntity(
                    id = 1, nickname = b.nickname, plate = b.plate, vin = b.vin, engineNo = b.engineNo, color = b.color,
                    purchaseDate = Dates.fromIso(b.purchaseDate), purchaseKm = b.purchaseKm, dealer = b.dealer,
                    warrantyMonths = b.warrantyMonths, brand = b.brand, model = b.model, engineCc = b.engineCc,
                    powertrain = b.powertrain, fuelSystem = b.fuelSystem, cooling = b.cooling, modelYear = b.modelYear,
                    boughtUsed = b.boughtUsed, purchasePrice = b.purchasePrice, newPrice = b.newPrice,
                    firstRegistration = Dates.fromIso(b.firstRegistration), previousOwners = b.previousOwners,
                    photo = photo, monthlyBudget = b.monthlyBudget, region = b.region
                )
            )
            dao.insertAllOdometer(file.odometer.mapNotNull { o -> Dates.fromIso(o.date)?.let { OdometerEntity(date = it, km = o.km) } })
            dao.insertAllFuel(file.fuel.mapNotNull { f -> Dates.fromIso(f.date)?.let { FuelEntity(date = it, km = f.km, liters = f.liters, cost = f.cost, full = f.full) } })
            dao.insertAllDocuments(file.documents.map { DocumentEntity(type = it.type, title = it.title, number = it.number, expiry = Dates.fromIso(it.expiry), notes = it.notes) })
            dao.insertAllInsurance(file.insurance.mapNotNull { i ->
                Dates.fromIso(i.start)?.let { InsurancePolicyEntity(startDate = it, months = i.months, totalPaid = i.totalPaid, insurer = i.insurer, policyNumber = i.policyNumber, notes = i.notes) }
            })
            dao.insertAllExpenses(file.expenses.mapNotNull { x ->
                Dates.fromIso(x.date)?.let { ExpenseEntity(date = it, category = x.category, title = x.title, amount = x.amount, km = x.km, quantity = x.quantity, notes = x.notes) }
            })
            dao.insertAllIntervals(file.intervals.filter { it.km != null || it.months != null }.map { TaskIntervalEntity(it.task, it.km, it.months) })
        }
        maintenance.replaceAll(file.services.mapNotNull { s ->
            Dates.fromIso(s.date)?.let { day ->
                MaintenanceEntryEntity(
                    km = s.km, date = day, note = s.notes, tasks = s.tasks.joinToString(","), milestoneKm = s.milestone,
                    shop = s.shop, cost = s.cost, partsCost = s.partsCost, laborCost = s.laborCost, product = s.product,
                    title = s.title, stamped = s.stamped, updatedAt = System.currentTimeMillis(), dirty = false
                )
            }
        })
        context.garageStore.edit { prefs ->
            val last = Dates.fromIso(file.preRide.last)
            if (last != null) prefs[PRE_RIDE_KEY] = last else prefs.remove(PRE_RIDE_KEY)
        }
        return "${file.services.size} services, ${file.fuel.size} fill-ups, ${file.insurance.size} insurance contracts, " +
            "${file.expenses.size} expenses imported"
    }

    suspend fun wipeGarage() {
        db.withTransaction { dao.clearProfile(); dao.clearOdometer(); dao.clearFuel(); dao.clearDocuments(); dao.clearInsurance(); dao.clearExpenses(); dao.clearIntervals() }
        photos.delete()
        context.garageStore.edit { it.remove(PRE_RIDE_KEY) }
    }
}

fun MaintenanceEntryEntity.toRecord() = ServiceRecord(
    date = Dates.startOfDay(date), km = km, tasks = taskIds, milestoneKm = milestoneKm, stamped = stamped, shop = shop
)

fun InsurancePolicyEntity.toPolicy() = InsurancePolicy(startDate, months, totalPaid)

/** Flattens every module into one list of payments; the ledger only ever reads this. */
fun spendsOf(
    profile: BikeProfileEntity,
    entries: List<MaintenanceEntryEntity>,
    fuel: List<FuelEntity>,
    insurance: List<InsurancePolicyEntity>,
    expenses: List<ExpenseEntity>
): List<Spend> = buildList {
    val firstDay = (entries.map { it.date } + fuel.map { it.date } + insurance.map { it.startDate } + expenses.map { it.date })
        .minOrNull()
    profile.purchasePrice?.takeIf { it > 0 }?.let { price ->
        // A price without a date counts from the first thing logged, so totals stay right.
        val day = profile.purchaseDate ?: firstDay ?: Dates.today()
        add(Spend(day, SpendCategory.PURCHASE, price, listOf(profile.brand, profile.model).filter { it.isNotBlank() }.joinToString(" "), SpendSource.PURCHASE))
    }
    fuel.forEach { f ->
        f.cost?.takeIf { it > 0 }?.let { add(Spend(f.date, SpendCategory.FUEL, it, "%.2f L".format(java.util.Locale.US, f.liters), SpendSource.FUEL, f.id)) }
    }
    entries.forEach { e ->
        e.cost?.takeIf { it > 0 }?.let { add(Spend(Dates.startOfDay(e.date), SpendCategory.MAINTENANCE, it, e.title.ifBlank { e.product }, SpendSource.SERVICE, e.id)) }
    }
    insurance.forEach { p -> if (p.totalPaid > 0) add(Spend(p.startDate, SpendCategory.INSURANCE, p.totalPaid, p.insurer, SpendSource.INSURANCE, p.id)) }
    expenses.forEach { x ->
        if (x.amount > 0) add(Spend(x.date, ExpenseLedger.categoryOf(x.category), x.amount, x.title, SpendSource.EXPENSE, x.id))
    }
}.sortedBy { it.date }
