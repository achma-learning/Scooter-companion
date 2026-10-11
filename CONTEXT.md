# Agility Companion (kymco-agility-moto) — AI Context File
_Last synced: 2026-10-11 @ 1356a73 (+ fuel-price work on `claude/exciting-sagan-ay0rzy`)_

## 1. What This Is (Plain English)
- **In one sentence:** A phone app and a matching website that act as the "owner's
  companion" for one specific scooter, the Kymco Agility 50 4T. It tracks when each service
  is due, keeps the service book and paperwork, and holds the scanned owner's manual you can
  search.
- **Why it exists:** The official booklet doesn't fit how the bike is actually ridden in
  Marrakech, with heat, dust and stop-and-go traffic. The intervals come from the owner's own
  Marrakech maintenance wiki (https://achma-learning.github.io/moto/), which is stricter than
  the factory figures. The app turns that wiki into reminders.
- **The owner's favourite part:** the fuel log. The owner mostly tops up a fixed amount
  ("30 dh"), only sometimes fills the tank, so the fill-up form starts from the money paid.
  Litres are worked out from the price per litre, which can be fetched with a tap.
- **Who uses it:** The owner, plus anyone who installs the APK from GitHub Releases or uses
  the website. The repo is public and releases are public. Treat it as a small public tool:
  people's garage data lives on their device, so never break backups or migrations.
- **Vibe:** A polished personal tool. It has real CI, tests on both platforms, a
  privacy model enforced in code, and versioned DB migrations. It is not a weekend hack.

## 2. How To Run It
- **Setup once:**
  - JDK 17, Android SDK (compileSdk 35), Node 22, Python 3. The Gradle wrapper (8.7) is
    committed, so there's no separate Gradle install.
  - Nothing to copy. There is no `.env.example`. `app/google-services.json` is a placeholder
    that builds fine (see §6).
- **Run dev:**
  - Website: `scripts/build-web.sh && npx http-server dist/web`, then open the printed URL.
    The service worker only registers over `https:` (`web/js/app.js:95`), so offline mode
    won't show up on localhost http. That's expected.
  - Android: `./gradlew assembleDebug` gives you
    `app/build/outputs/apk/debug/app-debug.apk`. The debug build installs as
    `com.example.kymcoagility50.debug`, next to the release one.
- **Tests:**
  - `node --test web/tests/*.test.mjs` runs 16 tests and passes as of this sync.
  - `python3 -m unittest discover -s scripts/fuel` tests the fuel-price parser.
  - `./gradlew testDebugUnitTest` runs the Robolectric tests. Add
    `-PscreenshotsDir=/tmp/shots` to save a PNG of every screen.
- **Change the knowledge base:** edit `scripts/content/build_content.py`, then run
  `python3 scripts/content/build_content.py`. Commit the regenerated JSON.
- **Build / deploy:** you don't deploy by hand. Every push to `main` runs
  `.github/workflows/build-and-release.yml`, which tests everything, then builds and
  publishes a GitHub Release `v2.0.<run_number>` with `agility-companion.apk` and
  `agility-companion-web.zip`. GitHub Pages deploy is optional and only runs when the repo
  variable `DEPLOY_PAGES=true` is set.
- **Required env vars / secrets:** none for local dev. CI or build-time only:
  - `APP_VERSION_CODE` and `APP_VERSION_NAME`, read in `app/build.gradle.kts:15-16`.
  - Optional real signing key: `RELEASE_KEYSTORE_BASE64`, `RELEASE_KEYSTORE_PASSWORD`,
    `RELEASE_KEY_ALIAS` and `RELEASE_KEY_PASSWORD` (repo secrets). The workflow decodes the
    first into `RELEASE_KEYSTORE_PATH`.
  - Repo variable: `DEPLOY_PAGES`.
- **Fuel price:** run the *Fuel price* workflow once from the Actions tab after merging. It
  creates the `data` branch with `fuel-price.json`, then refreshes it every Tuesday.

## 3. Tech Stack
- **Language + runtime:** Kotlin 1.9.24 on JVM 17 (Android minSdk 24, target/compile 35).
  The website is vanilla JavaScript (ES modules, no build step). The content generator is
  Python 3, standard library only. CI uses Node 22.
- **Android libraries** (versions from `build.gradle.kts` and `app/build.gradle.kts`):
  - AGP 8.5.2, with KSP for Room.
  - Jetpack Compose BOM 2024.06.00, Material3 1.2.1, Compose compiler 1.5.14.
  - Navigation-Compose 2.7.7.
  - Room 2.6.1, with exported schemas in `app/schemas/`.
  - WorkManager 2.9.1, DataStore 1.1.1, kotlinx-serialization-json 1.6.3.
  - ML Kit text-recognition 16.0.0, the on-device bundled model.
  - Firebase BOM 33.1.2 (Auth + Firestore) and play-services-auth 21.2.0. These are used in
    Sync Mode only.
  - Tests use JUnit 4 and Robolectric 4.13.
- **Website libraries:** none installed. pdf.js 3.11.174 is lazy-loaded from cdnjs only when
  you open a manual page (`web/js/views-manual.js:5`). The icons are Lucide SVGs inlined in
  `web/js/icons.js`.
- **What kind of project:** one repo with two deliverables that share their data:
  - an Android app (`app/`)
  - an installable, offline-capable PWA (`web/`)
  - a shared data folder (`shared/`) that both consume
- **External services:**
  - Firebase Auth + Firestore, opt-in Sync Mode only, at
    `/users/{uid}/maintenanceEntries/*`.
  - cdnjs, for pdf.js on the web.
  - GitHub Releases and Pages, for distribution.
  - GlobalPetrolPrices.com, read weekly by CI only (CC BY-NC-ND 3.0, credited in the JSON).
  - `raw.githubusercontent.com/.../data/fuel-price.json`, fetched by the app and site only
    when the owner taps *Today's price*.
  - Nothing else. There are no analytics.

## 4. Code Map (The Important Files Only)
**Shared data (both platforms read these)**
- `scripts/content/build_content.py` — **the source of truth** for every interval, spec,
  guide, troubleshooting tree, price and emergency number. It ends with referential-integrity
  asserts (task, guide and tree-node ids must exist).
- `shared/content/agility50.json` — GENERATED output: 25 tasks, 25 milestones, 9 guides.
  Never hand-edit it.
- `shared/docs/` — `owner_manual.pdf` (48 p, ~10 MB), `passport.pdf` (16 p), the
  pre-computed OCR text `*_index.json`, `toc.json` and the covers.
- `shared/fixtures/backup-sample.json` — **the app↔web backup contract**. Both test suites
  load it.

**Android (`app/src/main/java/com/example/kymcoagility50/`)**
- `MainActivity.kt` — open this first. It has the whole Compose NavHost, the bottom bar
  (Garage / Service / Manual / Guide / More) and the onboarding gate.
- `KymcoApp.kt` → `privacy/PrivacyManager.kt` — initialized first thing, before anything
  can touch the network.
- `AppContainer.kt` — hand-rolled DI singleton that wires all repositories. There's no
  Hilt.
- `domain/ServicePlanner.kt` — the brain. It answers "what's due, overdue, soon", the health
  score and the passport stamp states. It is pure Kotlin.
- `domain/FuelStats.kt` — L/100 km (full-to-full; partial top-ups count inside a segment),
  MAD/km, average and last MAD/L.
- `domain/Dates.kt` — all dates are UTC-midnight epoch millis.
- `data/repository/GarageRepository.kt` — combines Room, the planner and the content into a
  `GarageSnapshot`. It also does JSON backup export/import.
- `data/db/ManualDatabase.kt` (v2) and `data/db/Migrations.kt` — Room DB
  `kymco_agility50.db`, with an explicit `MIGRATION_1_2`.
- `data/backup/Backup.kt` — `@Serializable` mirror of the backup JSON.
- `data/content/Content.kt` + `ContentRepository.kt` — typed model of `agility50.json`,
  parsed with `ignoreUnknownKeys`.
- `privacy/NetworkGuard.kt`, `auth/AuthManager.kt`, `work/SyncScheduler.kt` and
  `work/MaintenanceSyncWorker.kt` — the Sync Mode path, gated at every layer.
- `data/fuel/FuelPriceClient.kt` — tap-only download of `fuel-price.json` (plain
  `HttpURLConnection`, no new dependency).
- `ui/screens/garage/OwnerScreens.kt` → `FuelScreen` + `FuelDialog` — the fuel log and the
  amount-first form (amount chips, price/L, litres derived).
- `work/ReminderWorker.kt` — daily local notification for overdue services and expiring
  documents. It never touches the network.
- `pdf/PdfPageRenderer.kt` + `data/repository/OcrRepository.kt` — the PDF reader and the
  OCR search, which is seeded from the bundled JSON with an ML Kit fallback.
- `ui/screens/**` — one folder per area. `ui/nav/Routes.kt` lists every route.

**Website (`web/`)**
- `index.html` → `js/app.js` — the hash router (`ROUTES` table) and boot. Views are pure
  functions returning `{ html, mount? }`.
- `js/planner.js` — **JS twin of `ServicePlanner.kt` + `FuelStats.kt`**.
- `js/store.js` — the garage lives in localStorage under key `agility-companion:garage:v1`.
  The state shape *is* the backup format plus local `id`s.
- `js/views-garage.js` (biggest, 470 lines), `views-guide.js` and `views-manual.js` — the
  screens.
- `js/ui.js` — HTML helpers, including `esc()` for escaping.
- `js/config.js` — `APK_URL` for the "get the app" links and `FUEL_PRICE_URL`.
- `js/fuelprice.js` — web twin of `FuelPriceClient.kt`. `fuelDialog` in `views-garage.js` is
  the web twin of `FuelDialog`.
- `sw.js` — offline cache. `__BUILD__` gets stamped by `scripts/build-web.sh`.

**Pipeline**
- `.github/workflows/build-and-release.yml` — in order: content-staleness check → web tests
  → Android tests → release APK → web zip → Release (and optionally Pages).
- `.github/workflows/fuel-price.yml` + `scripts/fuel/fetch_price.py` — weekly scrape →
  commits `fuel-price.json` to the orphan `data` branch. It never touches `main`, so it
  never triggers a release.
- `scripts/build-web.sh` — copies `web/`, the content JSON and the docs into `dist/web`,
  strips the tests, and stamps the SW version.

## 5. Rules For Editing This Code
- **Never hand-edit `shared/content/agility50.json`.** Edit `build_content.py` and
  regenerate. CI runs the generator and fails on any `git diff` in that file.
- **Planner logic lives twice. Change both or neither.** That means
  `domain/ServicePlanner.kt` + `FuelStats.kt` and `web/js/planner.js`. Mirror every test too:
  `ServicePlannerTest.kt`/`FuelStatsTest.kt` and `web/tests/planner.test.mjs` cover the same
  scenarios.
- **The backup format is a public contract.** If you add a field to `Backup.kt`, give it a
  default value. Update `store.js` `exportBackup`/`importBackup` and the fixture, and keep
  `BackupInteropTest.kt` + `web/tests/store.test.mjs` green. Old backups must still import.
- **Room schema changes need a real `Migration`.** Bump `version` in `ManualDatabase.kt`,
  add `MIGRATION_x_y` in `Migrations.kt`, and commit the new exported schema JSON. There is
  no destructive fallback, and losing someone's logbook is the worst bug this app can have.
- **Every network-capable call goes through `NetworkGuard` / `PrivacyManager.isPrivate()`.**
  Never call `FirebaseApp.initializeApp` or Firestore outside `AuthManager` and the sync
  worker. See `PRIVACY.md`, which documents exactly where this is enforced. Keep it true.
- **The only exception is `NetworkGuard.userRequested {}`.** It's for public downloads the
  owner starts with a tap (today's fuel price). The owner chose "on tap only" over a
  setting or a Sync-only feature. Never call it from a worker, app start or a
  `LaunchedEffect`, and never send user data through it.
- **Sync Mode uploads only maintenance-log entries.** Never sync the bike profile, fuel,
  documents, bookmarks or settings.
- **Dates:** on Android, use UTC-midnight millis via `Dates`. On the web, use ISO
  `YYYY-MM-DD` strings computed in UTC. Backups always use ISO strings.
- **Website stays zero-dependency, no bundler, no framework.** `web/package.json` only sets
  `"type":"module"`. Any new third-party script must be loaded lazily from cdnjs and added to
  the `isPdfJs`-style allowlist in `sw.js` if it should work offline.
- **Escape user text with `esc()`** before putting it into view HTML. Every view builds
  HTML with template strings and `innerHTML`.
- **New web files must be added to `PRECACHE` in `sw.js`**, or they won't work offline.
- **The fuel form is twinned too.** It has the same amounts (20/30/50/100), the same
  "any two of paid / price / litres give the third" rules, and the same 5–40 MAD/L
  plausibility band in `fetch_price.py`, `fuelprice.js` and `FuelPriceClient.kt`.
  Price per litre is never stored. It's always `cost ÷ liters`, so backups and Room are
  unchanged.
- **Task, guide and tree-node ids are referenced across content, logbook entries and
  routes.** Renaming an id orphans every logged service that used it.

## 6. Fragile Bits & Landmines
**Works, but subtle**
- `PrivacyManager.isPrivate()` defaults to PRIVATE until DataStore finishes loading
  asynchronously (`privacy/PrivacyManager.kt:50-58`). This is intentional fail-closed
  behavior. A Sync-Mode user can briefly read as Private right after cold start. Don't
  "fix" this by defaulting to the stored value synchronously.
- Planner edge rules that look arbitrary but are deliberate, in both twins:
  - A time-only task that has never been logged is shown as "due once" (`neverTimed`), not
    counted from the purchase date.
  - "Soon" means within 15% of the km span (minimum 100 km) or 20% of the day span.
  - Milestones only become "missed" once they are 1,000 km past (`MISSED_TOLERANCE_KM`).
  - `addMonths` in JS clamps month-end days to match `java.util.Calendar`
    (`web/js/planner.js:16-24`).
- On Android, `MaintenanceEntryEntity.tasks` is stored as a comma-separated
  string, read through the `taskIds` extension. Never store ids with commas.
- Fuel text fields use `decimalText()` (BigDecimal, dot separator), not `"%.2f".format`. A
  French-locale phone prints `15,18`, which `toDoubleOrNull()` can't read back.
- `fetch_price.py` parses one English sentence on the GlobalPetrolPrices page. If they
  reword it, the weekly workflow fails loudly and the last good `fuel-price.json` stays
  published. Fix the regex; the apps need no change.
- Computed litres keep **4 decimals** (`round4` / `decimalText(scale = 4)`). At 2 decimals,
  30 MAD @ 15.18 → 1.98 L → reads back as 15.15 MAD/L, and the remembered price drifts down
  with every fill. On the web, the litres input therefore needs `step="any"`, or the browser
  silently blocks Save.
- New fill-ups default to **not** full. That's on purpose for a top-up rider: a 30 MAD
  partial logged as "full" would wreck the L/100 km figure.

**Hard-won fixes**
- **Committed sideload signing key** (`app/signing/sideload.keystore`, with the passwords in
  `app/build.gradle.kts`). It is public *on purpose*: every Release must be signed with the
  same key so it installs as an update. A per-runner debug key forced an uninstall, which
  wiped the user's garage. `.gitignore` has an explicit `!` exception for it. **Do not
  delete it, rotate it, or "secure" it** without a migration plan. Switching keys means users
  have to uninstall.
- **`app/google-services.json` is a placeholder.** The `google-services` plugin refuses to
  build without it. It must include the `.debug` client, or debug builds fail (fixed in
  `ef4bddf`). Sign-in fails until you replace it with a real one.
- **R8/minify is off** in release builds (`app/build.gradle.kts:47`). Room,
  kotlinx-serialization and Firebase use reflection. `proguard-rules.pro` exists but is
  untested.
- **`noCompress += "pdf"`**: the manual is ~10 MB of scanned images. `PdfPageRenderer`
  copies the asset into `filesDir` because `PdfRenderer` needs a real file descriptor, and
  it serializes `openPage()` with a mutex.
- **The web store only re-renders after any open `<dialog>` closes** (`web/js/app.js:80-90`).
  That's so a cancelled confirm doesn't wipe a half-filled form.
- **The service worker serves whole cached PDFs** even when pdf.js asks for byte ranges.

**Skip on cleanup passes**
- The duplicated planner code (`ServicePlanner.kt` and `planner.js`). It's intentional.
- `shared/docs/*_index.json` are the real OCR output used to seed search, not junk.
- The `INTERNET` permission in the manifest is used by Sync Mode and the tap-only fuel
  price.
- `NetworkGuard.attemptsWhilePrivate`, a counter that "should always be 0", is the privacy
  proof. It is pinned by `NetworkGuardTest`.
- Both `app/schemas/.../2.json` and `Migrations.kt` must stay.
- `rootProject.name = "Owner Manual"` in `settings.gradle.kts` is a leftover from the v0
  manual-reader. It's harmless; renaming it is optional.

**Docs vs code conflicts**
- Backup import on Android marks every imported logbook row `dirty = true`, so in Sync Mode
  an import pushes the whole logbook to Firestore. That's intentional, but worth knowing.
- `wipeGarage()` (Android) clears the profile, odometer, fuel, documents and pre-ride. It
  does **not** clear the maintenance log (`GarageRepository.kt:186`). The web `wipe()`
  clears everything. _Not yet figured out_ whether this difference is intentional.

## 7. Current State
- **Last shipped (2026-10-09/10, PR achma-learning/kymco-agility-moto#1):**
  - The v0 PDF manual reader became the full owner companion: garage, planner, passport,
    fuel, wallet, guide and SOS.
  - The PWA website was added with backup interop.
  - The CI release pipeline now publishes the APK and the web zip.
  - The Room v1→v2 migration shipped.
- **Working on now:** making the fuel log the star. It has an amount-first fill-up form
  (20/30/50/100 MAD chips), a tap-to-fetch national price per litre, MAD/L stats, and a
  hint to log a full tank now and then.
- **Next up:**
  - Run the *Fuel price* workflow once so the `data` branch exists.
  - Maybe a price-per-litre trend chart, and the fuel stats on the Garage dashboard.
  - Wire in a real Firebase project and turn on `DEPLOY_PAGES` (both still optional).

**Missing repo hygiene:** no `LICENSE` file and no `dependabot.yml`. There are no other AI
memory files (`CLAUDE.md`, `AGENTS.md`, etc.).

## 8. Update Protocol (Verbatim)
> **For the AI Assistant:** When asked to "Update CONTEXT.md":
> 1. Re-run Phase 0 — check for new `GEMINI.md` / `CLAUDE.md` / `.github/` files.
> 2. Re-scan the tree, manifests, and `.github/workflows/` for drift.
> 3. Read our recent conversation for new decisions, fragile bits discovered, or shifted goals.
> 4. Refresh the `_Last synced_` line with today's date and current commit SHA.
> 5. Rewrite — do not append. One clean source of truth. Preserve still-true content, revise the rest.
> 6. Keep §1 and §2 in plain English. Keep the file under ~350 lines.
