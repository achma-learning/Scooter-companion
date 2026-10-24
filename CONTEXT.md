# Scooter Companion (achma-learning/scooter-companion) — AI Context File
_Last synced: 2026-10-23 @ Phases 3–5 on `claude/happy-sagan-x0wx57` (achma-learning/scooter-companion#7)_

## 1. What This Is (Plain English)
- **In one sentence:** An offline Android app that tracks every cost of owning a scooter
  (purchase, fuel, maintenance, insurance, gear, accessories, fines). It shows monthly
  spending against the owner's own target, and keeps the service book and paperwork.
- **History:** it started as *Agility Companion*, a companion for one scooter (Kymco Agility
  50 4T, Marrakech). In October 2026 the repo was renamed from `kymco-agility-moto` and the
  app became generic. The old app's last APK (2.0.7) is archived in
  `achma-learning/kymco-agility-50-4T` (`apk-backup/`), the repo formerly named `moto`, which
  also hosts the Marrakech maintenance wiki (https://achma-learning.github.io/kymco-agility-50-4T/,
  capital T: the lowercase URL is a 404).
- **Who it's for:** scooter owners worldwide, optimised first for **Morocco** (50 cc, 125 cc,
  126–249 cc, maxi-scooters, electric). Country-specific data lives in region packs.
- **The owner's priorities:** simple and fast to log (the fuel form starts from the money
  paid, e.g. "30 dh"), an "intelligence report" view of spending without complication,
  a monthly target, and a save file that moves to another phone. Fully offline.
- **Open source:** MIT, public repo, community contributions welcome (`CONTRIBUTING.md`).
- **Vibe:** a polished personal tool with real CI, tested domain logic, tested Room
  migrations and a public save-file contract. Not a weekend hack.

## 2. How To Run It
- **Setup once:** JDK 17, Android SDK (compileSdk 35), Node 22 (frozen website tests),
  Python 3. The Gradle wrapper (8.7) is in `android-app/`.
- **Run dev (Android):** `cd android-app && ./gradlew assembleDebug` gives
  `android-app/app/build/outputs/apk/debug/app-debug.apk`, which installs as
  `io.github.achmalearning.scootercompanion.debug` next to a release install.
- **Tests:**
  - `cd android-app && ./gradlew testDebugUnitTest`: 48 tests (domain, migrations, save-file
    interop, every screen rendered, the wizard in EN/FR/AR). Add `-PscreenshotsDir=/tmp/shots`
    for a PNG of every screen.
  - From the root: `node --test web/tests/*.test.mjs` (16) and
    `python3 -m unittest discover -s scripts/fuel`.
- **Knowledge base:** edit `scripts/content/build_content.py`, run it, commit the JSON.
- **Release:** every push to `main` runs `.github/workflows/build-and-release.yml`. With the
  signing secrets it publishes GitHub Release `v3.0.<run>` with `scooter-companion.apk`;
  without them it builds a debug APK artifact and publishes nothing.
- **Release key:** the owner adds one secret, `RELEASE_KEYSTORE_PASSWORD`, then runs the
  *Create release key* workflow once. It commits `android-app/app/signing/release.jks.gpg`
  (the keystore, AES-256 encrypted with that password) and starts a release. Advanced
  alternative: `RELEASE_KEYSTORE_BASE64` (+ `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`).
  Build-time env: `APP_VERSION_CODE`, `APP_VERSION_NAME`.
- **Fuel price:** `.github/workflows/fuel-price.yml` refreshes `fuel-price.json` on the
  orphan `data` branch every Tuesday.

## 3. Tech Stack
- **Android:** Kotlin 1.9.24, JVM 17, minSdk 24, target/compile 35. AGP 8.5.2 with KSP.
  Compose BOM 2024.06.00, Material3 1.2.1, Navigation-Compose 2.7.7, Room 2.6.1 (schemas
  exported to `android-app/app/schemas/`), WorkManager 2.9.1, DataStore 1.1.1,
  kotlinx-serialization 1.6.3, AppCompat 1.7.0 (per-app language only). Tests: JUnit 4,
  Robolectric 4.13, room-testing.
- **No cloud, no Google services.** Firebase and ML Kit were removed in Phase 2 (APK 69 → 25
  MB). That also keeps the app eligible for F-Droid.
- **Other parts:** `scripts/` (Python 3, stdlib only), `web/` (the Agility Companion PWA,
  vanilla JS, **frozen**: bug fixes only).
- **External services:** GitHub Releases (APK + update check, tap-only),
  `raw.githubusercontent.com/achma-learning/scooter-companion/data/fuel-price.json`
  (tap-only), GlobalPetrolPrices.com (read by CI weekly). Nothing else.

## 4. Code Map (The Important Files Only)
**Android (`android-app/app/src/main/java/io/github/achmalearning/scootercompanion/`)**
- `MainActivity.kt` (an `AppCompatActivity`): NavHost, bottom bar (Home / Expenses / Service /
  Stats / More; Manual and Guide open from More), onboarding gate on `PrivacyManager.onboarded`.
- `AppContainer.kt`: hand-rolled DI singleton (repositories, `regionRepository`).
- `domain/` (pure Kotlin, unit-tested):
  - `ExpenseLedger.kt`: `SpendCategory`, `Spend`, `LedgerStats` (total, running, this month,
    last month, this year, avg/month, cost/km, months, by category, budget), last-month report.
  - `Insurance.kt`: `InsurancePolicy` (end = start + months − 1 day), `Coverage`
    (NONE / EXPIRED / EXPIRING under 20 days / OK).
  - `Vehicle.kt`: `Powertrain`, `FuelSystem`, `Cooling`, `EngineClass` (CC50, CC125, MID,
    MAXI, ELECTRIC), `VehicleSpec`, `enumOr`.
  - `ServicePlanner.kt`, `FuelStats.kt`: from Agility Companion (Agility 50 intervals).
  - `Dates.kt`: UTC-midnight millis; `monthKey` = year × 12 + month0.
- `data/repository/GarageRepository.kt`: combines every table into `GarageSnapshot`
  (profile, `spec`, entries, fuel, insurance, expenses, `spends`, `ledger`, `coverage`…);
  `spendsOf()` flattens all modules into payments; save-file export/import.
- `data/repository/PhotoStore.kt`: the owner's photo, one ≤1024 px JPEG in `filesDir`.
- `data/db/`: `ManualDatabase` (v3, file `scooter_companion.db`), `GarageEntities.kt`
  (`BikeProfileEntity`, `InsurancePolicyEntity`, `ExpenseEntity`, …), `Migrations.kt`
  (`MIGRATION_1_2`, `MIGRATION_2_3`).
- `data/backup/Backup.kt`: the save file, format `scooter-companion` v2; reads
  `agility-companion` v1.
- `data/region/RegionPack.kt`: region packs from `shared/assets/regions/<id>.json`
  (currency, brands, used-scooter sites, fuel chips per class, insurance hints, fines).
- `ui/screens/onboarding/OnboardingScreen.kt`: welcome + 3-step wizard; `LanguageChips`.
- `ui/screens/profile/ProfileForm.kt`: `ProfileDraft` + the type / purchase / details
  sections, shared by the wizard and `ProfileScreen.kt` ("My scooter").
- `ui/common/Language.kt`: `AppLanguage` (fr, en, ar-MA) via `AppCompatDelegate`.
- `ui/screens/garage/FuelScreens.kt`: fuel log + `FuelDialog` (chips from the region pack).
- `ui/screens/garage/OwnerScreens.kt`: passport (Agility only) and documents wallet.
- `ui/screens/money/MoneyTab.kt`: the Expenses tab, every `Spend` (fuel, service, insurance,
  expenses, purchase) by month with a 12-month chart; tapping a row opens it where it was
  logged (`Spend.source` / `refId`).
- `ui/screens/money/MoneyScreens.kt`: insurance (with the monthly cost chart from
  `Insurance.months`), expense dialog (fines), recommendations.
- `ui/screens/stats/StatsScreen.kt` + `ui/charts/Charts.kt`: the Stats tab and the chart kit.
- `domain/MaintenancePlan.kt`: Agility schedule or generic tasks/checklist per profile.
- `domain/Stats.kt`: chart data per range and `Insights`.
- `data/export/TextReport.kt`: the plain-text report for AI analysis.
- `data/update/`: tap-only updater (`achma-learning/scooter-companion`, `scooter-companion.apk`).
- `data/fuel/FuelPriceClient.kt`, `privacy/NetworkGuard.kt`: tap-only downloads.
- `work/ReminderWorker.kt`: daily local notifications (services, documents).
- `res/values{,-fr,-ar}/strings.xml`: new screens are translated; older screens are still
  English literals in code.

**Shared data**
- `shared/assets/regions/MA.json`: the Morocco pack. Bundled as assets (`regions/…`).
- `shared/content/agility50.json`: GENERATED by `scripts/content/build_content.py`.
- `shared/docs/`: Agility manual and passport PDFs with OCR indexes.
- `shared/fixtures/backup-sample.json` (Agility v1, also used by the web tests) and
  `backup-sample-v2.json` (Scooter Companion).

**Pipeline:** `build-and-release.yml` (content check → fuel parser tests → web tests →
Android tests → signed release or debug artifact → Release), `fuel-price.yml`.

## 5. Rules For Editing This Code
- **Never lose someone's data.** Room schema changes need a version bump, a real
  `Migration`, the exported schema JSON, and a case in `MigrationTest`. No destructive
  fallback.
- **The save file is a public contract.** New fields get defaults; never rename or remove
  one. Both fixtures must keep importing (`BackupInteropTest`). Bump `VERSION` only for a
  breaking change, and keep reading older versions.
- **Every money statistic reads the ledger** (`GarageSnapshot.spends` / `ledger`). Don't sum
  tables in a screen. New spending sources go into `spendsOf()`.
- **Money that is "a reference" is never spending**: `newPrice` and region insurance prices
  are hints. Only what the owner paid counts.
- **Profile edits go through `ProfileDraft.toEntity()`**, which copies onto the stored row.
  Building a fresh `BikeProfileEntity` in a screen wipes the fields that screen doesn't show.
- **Country-specific values belong in a region pack**, not in code.
- **New UI text goes in string resources, in all three languages** (`values`, `values-fr`,
  `values-ar`). Arabic is `ar-MA` so digits stay Western.
- **Network only from a tap, through `NetworkGuard.userRequested {}`.** Never from a worker,
  app start or `LaunchedEffect`. Never send user data.
- **Dates:** UTC-midnight millis via `Dates`; ISO `yyyy-MM-dd` in save files.
- **Never hand-edit `shared/content/agility50.json`**; CI fails on a stale file.
- **`web/` is frozen.** Bug fixes only; it keeps the Agility v1 format.
- **Task, guide and tree-node ids** are stored in logbook entries; renaming one orphans them.

## 6. Fragile Bits & Landmines
**Works, but subtle**
- **Signing.** Releases are signed with the private key in `signing/release.jks.gpg`
  (encrypted, decrypted in CI with the `RELEASE_KEYSTORE_PASSWORD` secret; alias `scooter`,
  key password = keystore password). Every release must use the same key or updates fail to
  install, so *Create release key* refuses to run once the file exists. Never delete or
  replace that file; losing the password means every user reinstalls. The old public
  sideload key was deleted on purpose.
- **The updater compares `versionCode`s** read from `<!-- versionCode: N -->` in the release
  body (fallback: the tag's last number). Keep that line and the asset name
  `scooter-companion.apk`. GitHub may spell the repo `Scooter-companion`; the URL check
  ignores case.
- **Agility Companion 2.0.x can't update to this app** (different ID and key). Its own
  updater also rejects the renamed repo's URLs. Users move with Export → Import.
- **Month projection** extrapolates only `SpendCategory.recurring` costs (fuel, parking,
  washing, charging). One-offs count once.
- **Insurance coverage** follows renewals that start by the day after the current end; a gap
  is not hidden. Under 20 days left = daily reminder (the reminder itself is Phase 3).
- **A purchase price without a date** is placed on the first logged day, so totals stay right.
- **Fuel text fields** use `decimalText()` (BigDecimal, dot), not `"%.2f".format`: a French
  phone prints `15,18`. `NumberField` turns commas into dots.
- **Computed litres keep 4 decimals**, or the remembered price per litre drifts down.
- **New fill-ups default to not full**: a 30 dh top-up logged as full wrecks L/100 km.
- **Planner rules** (both twins): never-logged time-only tasks are "due once"; "soon" is
  within 15% of the km span (min 100 km) or 20% of the days; milestones are "missed" only
  1,000 km past.
- **`MaintenanceEntryEntity.tasks`** is comma-separated; ids must never contain commas.
  Its `remoteId` / `dirty` / `deleted` columns are leftovers of the removed sync.
- **Room schemas ride in debug assets** so Robolectric's `MigrationTestHelper` can read them
  (`sourceSets.debug` in `app/build.gradle.kts`). Release APKs don't carry them.
- **There is no `1.json` schema**: version 1 was never exported, so only v2 → v3 is replayed.
- `fetch_price.py` parses one English sentence; if the site rewords it, the weekly job fails
  loudly and the last good price stays published.

**Hard-won fixes**
- **R8/minify is off** in release builds (Room and kotlinx-serialization reflection).
- **`noCompress += "pdf"`**; `PdfPageRenderer` copies the PDF into `filesDir` and serializes
  `openPage()` with a mutex.
- **The wizard's step survives recreation** (`rememberSaveable`), because changing the
  language recreates the activity.

**Skip on cleanup passes**
- The duplicated planner code (`ServicePlanner.kt` and `web/js/planner.js`): intentional.
- `shared/docs/*_index.json`: the real OCR output that powers manual search.
- `INTERNET` and `REQUEST_INSTALL_PACKAGES` permissions: the fuel price and the updater.
- `android-app/app/schemas/.../{2,3}.json` and `Migrations.kt` must stay.

## 7. Current State
- **Shipped:** Agility Companion 2.0.x; Phase 1 (`android-app/`, MIT, achma-learning/scooter-companion#4);
  Phase 2 (identity, profile, ledger, save file v2, wizard, FR/EN/AR,
  achma-learning/scooter-companion#5); one-password release signing
  (achma-learning/scooter-companion#6). First signed release: v3.0.14.
- **This PR (achma-learning/scooter-companion#7), phases 3–5:**
  - fuel chips per engine class from the region pack, Full tank chip, km/L;
  - generic maintenance (`genericTasks` in the knowledge base, `MaintenancePlan`), service
    log with parts / labour / product / custom title;
  - insurance, expenses (fines with presets and reasons), recommendations, SOS from the region
    pack (5050 ADM added), generic pre-ride checklist (`genericChecklist`);
  - home money card (this month vs target, projection, averages, cost/km, insurance line);
  - Stats tab (`domain/Stats.kt`, `ui/charts/Charts.kt`): stacked monthly bars with the
    target, cumulative line, category table, price per litre, consumption, 26-week calendar,
    insights; palette validated with the dataviz validator for light and dark;
  - ReminderWorker: daily insurance notification under 20 days or expired; last month's
    report on the first run of each month;
  - `.txt` report for AI (`data/export/TextReport.kt`, identifiers hidden by default),
    share sheet for the save file and the report (FileProvider `cache/exports`);
  - translated: onboarding, profile, fuel, service, insurance, expenses, stats, home, More,
    SOS, checklist, wallet, settings, notifications.
- **Expense tracker PR:** Expenses tab in the bottom bar listing every payment from every
  module; insurance as a monthly cost (now per month / per day, average, paid per year, bars
  per month against the typical price); fuel spending per month on the fuel log.
- **Home-screen widget** (`widget/ScooterWidget.kt`, `res/layout/widget_scooter.xml`): plain
  RemoteViews, no extra library. Redrawn from `GarageRepository.snapshot` while the app runs
  (`ScooterWidget.watch` from MainActivity), by ReminderWorker once a day and by the launcher
  every 12 h. Three progress bars (ok / soon / due) because RemoteViews can't recolour one
  before Android 12. `formatMoney` / `formatKm` add a left-to-right mark in Arabic so
  "400 DH" doesn't show as "DH 400".
- **Still English only:** the Agility-only screens (passport, manual reader, guide hub and its
  content: troubleshooting, how-tos, specs, parts, seasons). They come from the Agility
  knowledge base; translating them means adding fields to `build_content.py`.
- **Ideas next:** more region packs (via CONTRIBUTING), per-task interval overrides by the
  owner.

## 8. Update Protocol (Verbatim)
> **For the AI Assistant:** When asked to "Update CONTEXT.md":
> 1. Re-run Phase 0 — check for new `GEMINI.md` / `CLAUDE.md` / `.github/` files.
> 2. Re-scan the tree, manifests, and `.github/workflows/` for drift.
> 3. Read our recent conversation for new decisions, fragile bits discovered, or shifted goals.
> 4. Refresh the `_Last synced_` line with today's date and current commit SHA.
> 5. Rewrite — do not append. One clean source of truth. Preserve still-true content, revise the rest.
> 6. Keep §1 and §2 in plain English. Keep the file under ~350 lines.
