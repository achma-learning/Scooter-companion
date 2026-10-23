# Privacy

This app has two modes. This document says exactly what each one does with your data, and
points at the code that enforces it so it isn't just a claim.

## Private Mode (default)

- No account. No Google Sign-In button is shown anywhere.
- The app never goes online by itself — not this app's code, not Firebase (which is never
  initialized in this mode at all). The only exceptions are downloads **you** start with a
  tap: "Today's fuel price" and "Check for updates", both described below.
- Bookmarks, recent pages, settings, the maintenance logbook and the rest of your garage
  (bike profile, odometer readings, fuel log, documents wallet) are stored only in the local
  Room database (`kymco_agility50.db`, private app storage).
- Service and document-expiry reminders are computed on the device by `ReminderWorker`
  (WorkManager, once a day) and shown as local notifications. They need no network and run the
  same in both modes; turn them off in Settings.
- Android's own cloud backup and device-to-device transfer are disabled for the whole app
  (`android:allowBackup="false"`, `android-app/app/src/main/res/xml/data_extraction_rules.xml`) — this
  isn't just an in-app promise, it's an OS-level setting.

## Sync Mode (opt-in)

- You sign in with Google (Firebase Authentication).
- The **only** data that syncs is the service logbook: for each service you log, its date,
  km, task ids, passport milestone, workshop name, cost, dealer-stamp flag and notes. It is
  written to `/users/{your-uid}/maintenanceEntries/{entryId}` in Cloud Firestore.
- The bike profile (plate, VIN…), odometer readings, fuel log, documents wallet, bookmarks,
  recent pages and settings stay local-only in Sync Mode too. They are never written to
  Firestore.
- The two bundled PDFs and their OCR search index are static app assets; they're never
  uploaded anywhere in either mode.
- Sync happens via a WorkManager job (`MaintenanceSyncWorker`) that runs when you add/edit/
  delete a logbook entry, or when you tap "Sync now".

## How this is enforced in code, not just documented

- `privacy/PrivacyManager.kt` — the single source of truth for which mode you're in
  (`AppMode.PRIVATE` or `AppMode.SYNC`), persisted via DataStore, defaulting to
  `PRIVATE` even before it has finished loading (fail closed).
- `privacy/NetworkGuard.kt` — every network-capable code path calls
  `NetworkGuard.isNetworkAllowed()` or wraps itself in `NetworkGuard.guard { ... }` before
  doing anything. `guard` refuses to run its block at all while Private Mode is on, and
  counts the attempt instead of silently allowing it — that counter is what a Privacy
  Dashboard / debug screen would read to show "0 network attempts".
- `auth/AuthManager.kt` — `FirebaseApp.initializeApp()` is only ever called after checking
  `PrivacyManager.isPrivate()` is false. Every other function in this file (`currentUser()`,
  `signInIntent()`, etc.) also checks it and returns null/no-ops if you're in Private Mode.
- `work/SyncScheduler.kt` — `scheduleSyncNow` / `schedulePeriodicSync` refuse to enqueue any
  WorkManager job while Private Mode is on; switching **into** Private Mode
  (`SettingsViewModel.switchToPrivateMode`) immediately signs you out and calls
  `SyncScheduler.cancelAll` to remove any already-scheduled sync work.
- `work/MaintenanceSyncWorker.kt` — the worker itself also checks
  `PrivacyManager.isPrivate()` as its very first line and returns success immediately if
  true, as a second layer in case anything upstream is ever wrong.
- `android-app/app/src/test/java/.../privacy/NetworkGuardTest.kt` — a unit test pinning down that
  `NetworkGuard.guard {}` never executes its block while in Private Mode (the default
  state), and that the attempt is counted rather than silently dropped.

## Backup files

Settings → Export writes your garage to a JSON file **you** choose where to save (Android's
file picker), and Import reads one back. The app never uploads it. The same file format is
used by the website, so you can move between the two.

## The website

The website (`web/`) keeps your garage in that browser's `localStorage` only. It has no
backend, no cookies, no analytics, and nothing is sent anywhere. It makes only two kinds of
third-party request, both triggered by you: the PDF viewer (pdf.js from
`cdnjs.cloudflare.com`), loaded when you open a manual page, and today's fuel price (see
above), fetched when you tap the button.

## Today's fuel price (both modes, only when you tap)

The fill-up form has a **Today's price** button. Tapping it downloads one small public file,
`fuel-price.json`, from `raw.githubusercontent.com` (this repository's `data` branch). The
file holds Morocco's national average gasoline price; a weekly GitHub Action
(`.github/workflows/fuel-price.yml`) copies it from GlobalPetrolPrices.com. Nothing about you
or your bike is sent: it's a plain GET with no account, no cookies and no identifiers, though
GitHub sees your IP address, as any website would.

- It only happens on a tap. No worker, timer or app start ever calls it.
- Android: `data/fuel/FuelPriceClient.kt` goes through `NetworkGuard.userRequested {}`, the
  single documented exception to the Private-Mode gate. It is counted in
  `NetworkGuard.userRequests`, apart from `attemptsWhilePrivate`, which still has to read 0.
  `NetworkGuardTest` pins both.
- Website: `web/js/fuelprice.js`, called only from the button's click handler.

Don't want it? Just don't tap it. You can always type the price on the pump.

## Check for updates (both modes, only when you tap)

Settings → App updates → **Check for updates** asks `api.github.com` which release of this
repository is the latest. If it's newer, **Download & install** fetches `agility-companion.apk`
from that GitHub release (with Android's DownloadManager), and **Install** hands it to the
system installer. Nothing about you is sent. GitHub sees your IP address and a generic
`agility-companion-android` user agent.

- It never checks by itself: there's no automatic or background update check.
- `data/update/UpdateChecker.kt` and `ApkUpdater.kt` go through `NetworkGuard.userRequested {}`,
  like the fuel price. They only download from this repository's own release files.
- Android itself only accepts the update if it's signed with the same key as the installed
  app, and installing over the app keeps all your local data.

## What's never collected, in either mode

No analytics, no crash reporting, no ads, no advertising ID, no unique device identifiers,
no location, no camera, no contacts.
