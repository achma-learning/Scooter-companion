# Privacy

This app has two modes. This document says exactly what each one does with your data, and
points at the code that enforces it so it isn't just a claim.

## Private Mode (default)

- No account. No Google Sign-In button is shown anywhere.
- No network request of any kind is made — not by this app's code, not by Firebase (which
  is never initialized in this mode at all).
- Bookmarks, recent pages, settings, the maintenance logbook and the rest of your garage
  (bike profile, odometer readings, fuel log, documents wallet) are stored only in the local
  Room database (`kymco_agility50.db`, private app storage).
- Service and document-expiry reminders are computed on the device by `ReminderWorker`
  (WorkManager, once a day) and shown as local notifications. They need no network and run the
  same in both modes; turn them off in Settings.
- Android's own cloud backup and device-to-device transfer are disabled for the whole app
  (`android:allowBackup="false"`, `app/src/main/res/xml/data_extraction_rules.xml`) — this
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
- `app/src/test/java/.../privacy/NetworkGuardTest.kt` — a unit test pinning down that
  `NetworkGuard.guard {}` never executes its block while in Private Mode (the default
  state), and that the attempt is counted rather than silently dropped.

## Backup files

Settings → Export writes your garage to a JSON file **you** choose where to save (Android's
file picker), and Import reads one back. The app never uploads it. The same file format is
used by the website, so you can move between the two.

## The website

The website (`web/`) keeps your garage in that browser's `localStorage` only. It has no
backend, no cookies, no analytics, and nothing is sent anywhere. The only third-party request
is the PDF viewer (pdf.js from `cdnjs.cloudflare.com`), loaded when you open a manual page.

## What's never collected, in either mode

No analytics, no crash reporting, no ads, no advertising ID, no unique device identifiers,
no location, no camera, no contacts.
