# Privacy

Scooter Companion has no account, no server and no cloud sync. This document says exactly what
it does with your data, and points at the code that enforces it.

## Everything stays on your phone

- Your scooter profile and photo, odometer readings, fuel log, service logbook, insurance
  contracts, expenses, documents, bookmarks and settings are stored only in the app's private
  storage: the Room database `scooter_companion.db`, DataStore preferences, and one JPEG
  (`scooter-photo.jpg`) if you chose a photo.
- The app never goes online by itself. The only network requests are the downloads **you**
  start with a tap, described below: today's fuel price, app updates, and a scooter manual.
- Reminders (services due, expiring documents) are computed on the phone by `ReminderWorker`
  (WorkManager, once a day) and shown as local notifications. You can turn them off in
  Settings.
- Android's own cloud backup and device-to-device transfer are disabled for the whole app
  (`android:allowBackup="false"`,
  `android-app/app/src/main/res/xml/data_extraction_rules.xml`). That is an OS-level setting,
  not only an in-app promise.
- The photo picker is Android's own: the app sees only the one picture you pick, and needs no
  storage permission.

## How this is enforced in code

- There is no networking library, analytics SDK or cloud SDK in the app. The downloads below
  use plain `HttpURLConnection` and Android's DownloadManager.
- `privacy/NetworkGuard.kt`: every network call goes through `NetworkGuard.userRequested {}`,
  which is called only from click handlers, never from a worker, app start or a
  `LaunchedEffect`. `NetworkGuardTest` pins its behaviour.

## Save files

Settings → Export writes everything to a JSON file in a place **you** choose (Android's file
picker), and Import reads one back. The app never uploads it. The file contains your data in
plain text, including plate, VIN and photo, so share it only with people you trust.

## Today's fuel price (only when you tap)

The fill-up form has a **Today's price** button. Tapping it downloads one small public file,
`fuel-price.json`, from `raw.githubusercontent.com` (this repository's `data` branch). The file
holds Morocco's national average gasoline price; a weekly GitHub Action
(`.github/workflows/fuel-price.yml`) copies it from GlobalPetrolPrices.com. Nothing about you or
your scooter is sent: it's a plain GET with no account, no cookies and no identifiers, though
GitHub sees your IP address, as any website would.

- Android: `data/fuel/FuelPriceClient.kt`, through `NetworkGuard.userRequested {}`.
- Don't want it? Don't tap it; type the price from the pump instead.

## Scooter manuals (only when you tap)

The app ships the specifications and maintenance schedules of the scooters in
[`scooter-data/`](scooter-data/), but none of their PDFs. On **More → Specs & documents** (and
in the Agility manual reader), **Download** fetches one PDF: from this repository's
`scooter-data` folder on `raw.githubusercontent.com` (the Agility 50 manual and passport,
checked against their SHA-256), or from the manufacturer's own website (e.g. Honda's owner's
manual on `honda.co.uk`). It is a plain GET; the site sees your IP address. The file is kept in
the app's private storage until you remove it.

- `data/models/ModelDocs.kt`, through `NetworkGuard.userRequested {}`.

## Check for updates (only when you tap)

Settings → App updates → **Check for updates** asks `api.github.com` which release of this
repository is the latest. If it's newer, **Download & install** fetches `scooter-companion.apk`
from that GitHub release and **Install** hands it to the system installer. Nothing about you is
sent; GitHub sees your IP address and the user agent `scooter-companion-android`.

- `data/update/UpdateChecker.kt` and `ApkUpdater.kt`, through `NetworkGuard.userRequested {}`.
  They only download from this repository's own release files.
- Android only accepts the update if it is signed with the same key as the installed app, and
  installing over the app keeps all your data.

## The Agility Companion website

The website in `web/` (frozen) keeps its data in that browser's `localStorage` only. It has no
backend, no cookies and no analytics. It makes two kinds of third-party request, both started by
you: pdf.js from `cdnjs.cloudflare.com` when you open a manual page, and today's fuel price.

## Never collected

No analytics, no crash reporting, no ads, no advertising ID, no device identifiers, no
location, no contacts, no camera access.
