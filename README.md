# Agility Companion — Kymco Agility 50 4T

A digital owner's companion for the **Kymco Agility 50 4T**, in the spirit of the SYM, My Honda+
and BMW Motorrad apps: your scooter's dashboard, service book, paperwork and manual in one
place. Every interval is tuned for **Marrakech heat, Saharan dust and stop-and-go traffic**,
from the [Marrakech maintenance wiki](https://achma-learning.github.io/moto/).

It ships two ways, with the same data and features:

| | Android app (`app/`) | Website (`web/`) |
|---|---|---|
| Runs | Installed APK, fully offline | Any browser; installable, works offline |
| Stores your garage | On the phone (Room) | In that browser (localStorage) |
| Reminders | Daily notification for due services & expiring documents | Shown on the dashboard |
| Move data | Settings → Export / Import | Settings → Export / Import (same JSON file) |

## What's inside

- **Garage**: nickname, plate, odometer, a health score, the next services due, documents that
  are expiring, a tip for the current season, and quick actions.
- **Service planner**: 25 maintenance tasks with Marrakech and factory intervals side by side,
  break-in milestones (300 · 1,000 · 2,000 · 3,000 km), and limits by km *or* by time. Due-soon
  and overdue are tracked per task, and you can log one tap as "done today" or record full
  details.
- **Digital passport**: the Kymco booklet's stamp schedule (plus the wiki's 2,000 / 8,000 /
  10,000 km checks) shown as stamps. Each one is marked stamped by a dealer, done, next, or
  missed. It also shows the warranty end date and the bike's identity (plate, VIN, dealer).
- **Fuel log**: full-tank L/100 km, cost per km and total spend.
- **Documents wallet**: insurance, carte grise, licence and warranty, with an expiry countdown.
- **Manual**: the scanned Driver's Manual (48 p.) and Maintenance Passport (16 p.), with
  full-text search over their OCR text (FR/AR/EN), a table of contents and bookmarks.
- **Owner's guide**:
  - pre-ride and weekly checklist
  - troubleshooting decision trees (won't start, power loss, noises, overheating)
  - known failure modes, ranked
  - 9 step-by-step how-tos
  - specs and torque table
  - oil strategy by season
  - seasonal and storage advice
  - parts and prices in MAD, with resale timing
  - riding gear
- **SOS**: Moroccan emergency numbers (tap to dial), what to do after an accident (declare it to
  your insurer within 5 days), your insurance number, and approved dealers in Maps.

## Get it

**Android**: open the latest [GitHub Release](../../releases/latest) on your phone and install
`agility-companion.apk` (allow "install unknown apps" once). Each new release installs over the
previous one and keeps your data.

**Website**: download `agility-companion-web.zip` from the same release, unzip it, and upload
the files to any static host (GitHub Pages, Netlify, Cloudflare Pages, your own server). It
is plain HTML/CSS/JS with no server code. To publish it on this repo's GitHub Pages
automatically, go to *Settings → Pages* and set *Source = GitHub Actions*, then add the
repository variable `DEPLOY_PAGES=true`.

## Releases

`.github/workflows/build-and-release.yml` runs on every push to `main` (and on `v*` tags):

1. Regenerates the knowledge base and fails if `shared/content/agility50.json` is stale.
2. Runs the website tests and the Android unit tests. These cover the planner, app ↔ web
   backup interop, and a Robolectric pass that renders every screen.
3. Builds the release APK (`versionCode` = run number) and the website zip.
4. Publishes a GitHub Release `v2.0.<run>` with both files attached. Pull requests get the
   same files as workflow artifacts.

**Signing**: without secrets, the APK is signed with the committed *sideload* key
(`app/signing/sideload.keystore`). That keeps every release updatable in place, but the key is
public, so don't use it for the Play Store. To sign with your own key, add the repository
secrets `RELEASE_KEYSTORE_BASE64`, `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS` and
`RELEASE_KEY_PASSWORD`. Changing keys later means one uninstall, so export a backup first.

## Project layout

```
shared/
  content/agility50.json   knowledge base (tasks, milestones, specs, guides, trees…) — GENERATED
  docs/                    owner_manual.pdf, passport.pdf, OCR indexes, toc.json, covers
  fixtures/                backup-sample.json — the app ↔ web backup contract, tested on both sides
scripts/
  content/build_content.py source of the knowledge base → writes shared/content/agility50.json
  build-web.sh             assembles the website into dist/web
app/                       Android app (Kotlin, Jetpack Compose, Room, WorkManager)
  …/domain/                ServicePlanner, FuelStats, Dates — pure Kotlin, unit-tested
  …/data/                  Room (garage + logbook), content & backup models, repositories
  …/ui/screens/            garage, service, guide, home (manual), more, settings, reader…
  …/work/                  ReminderWorker (local), MaintenanceSyncWorker (Sync Mode only)
web/                       Website (vanilla ES modules, no build step, no framework)
  js/planner.js            JavaScript twin of ServicePlanner.kt — keep them in sync
  js/store.js              localStorage + backup import/export
  js/views-*.js            screens; js/app.js is the hash router
  sw.js                    offline cache
```

The app bundles `shared/docs` and `shared/content` as assets (`sourceSets` in
`app/build.gradle.kts`). The website build copies the same files, so both always ship the same
data.

## Develop

```bash
# Knowledge base: edit the generator, never the JSON
python3 scripts/content/build_content.py

# Website: build + serve locally
scripts/build-web.sh && npx http-server dist/web
node --test web/tests/*.test.mjs

# Android
./gradlew assembleDebug            # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest        # add -PscreenshotsDir=/tmp/shots to save a PNG per screen
```

To change an interval: edit `tasks` in `build_content.py`, regenerate, and run both test suites.
The planner logic lives twice (`ServicePlanner.kt` and `web/js/planner.js`), and the two test
files cover the same scenarios to keep them in agreement.

## Two modes — the privacy model

On first launch the app asks for **Private Mode** (default) or **Sync Mode**:

- **Private Mode**: no account, no network call of any kind, and Firebase is never
  initialised. Everything works offline.
- **Sync Mode**: sign in with Google. Only the service logbook syncs (date, km, tasks,
  milestone, shop, cost, stamp, notes). The bike profile, fuel log, documents, bookmarks and
  settings always stay local.

See [PRIVACY.md](PRIVACY.md) for what each mode does and where it's enforced in code.

### Sync Mode / Firebase setup

`app/google-services.json` is a **placeholder** (valid enough to build; sign-in fails until you
replace it).

1. Create a Firebase project and register the Android app `com.example.kymcoagility50` (add
   `com.example.kymcoagility50.debug` too for debug builds). Download the real
   `google-services.json` into `app/`.
2. Enable **Authentication → Google** and **Firestore**.
3. Firestore rules, so each user only reaches their own logbook:
   ```
   rules_version = '2';
   service cloud.firestore {
     match /databases/{database}/documents {
       match /users/{userId}/{document=**} {
         allow read, write: if request.auth != null && request.auth.uid == userId;
       }
     }
   }
   ```

Entries sync to `/users/{uid}/maintenanceEntries/*`. Nothing else is ever written.

## Sources

Intervals, prices and procedures come from the
[Kymco Agility 50 4T Marrakech wiki](https://achma-learning.github.io/moto/)
([repo](https://github.com/achma-learning/moto)). Factory figures come from the official
Driver's Manual (KN10EM, p.39–40) and the KN10 workshop manual. Icons on the website are from
[Lucide](https://lucide.dev) (ISC). This project is not affiliated with KYMCO.
