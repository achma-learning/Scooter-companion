# Kymco Agility 50 — Owner Manual

Offline-first Android app for the KYMCO Agility 50 scooter, built from two scanned
documents you provided:

- `app/src/main/assets/owner_manual.pdf` — the English driver's manual (48 pages)
- `app/src/main/assets/passport.pdf` — the French/Arabic KYMCO passeport & maintenance
  booklet (16 pages)

Both PDFs' text was extracted once, offline, with Tesseract OCR (French + Arabic + English
language packs) at project-generation time and shipped as
`assets/manual_index.json` / `assets/passport_index.json`, so full-text search works
immediately on first launch with no on-device OCR delay. `assets/toc.json` mirrors each
document's actual printed structure.

## Two modes — that's the whole privacy model

The app has exactly two modes, chosen on first launch and changeable anytime in
**Settings**:

- **Private Mode** (default). No account, no network call of any kind, no Firebase
  initialization. Reading, search, bookmarks, and the maintenance logbook all work, stored
  only in the local Room database.
- **Sync Mode**. Sign in with Google. The **only** thing that syncs is the maintenance
  logbook (date / km / notes entries) — bookmarks, recent pages, and settings are always
  local-only regardless of mode. See `PRIVACY.md` for the full technical breakdown and
  where to read the enforcement code.

## Build

```
./gradlew assembleDebug
```
APK output: `app/build/outputs/apk/debug/app-debug.apk`

### About the Gradle wrapper

This project's `gradle-wrapper.jar` is a binary normally downloaded from
`services.gradle.org`, which wasn't reachable from the environment this project was
generated in. Before building for the first time, generate it once:

```
gradle wrapper --gradle-version 8.7 --distribution-type bin
```

(requires *some* local Gradle install — Android Studio ships one, or `brew install gradle` /
your package manager). `.github/workflows/build-apk.yml` does this automatically in CI, so
a push to this repo produces a downloadable `app-debug.apk` artifact without you needing
Gradle installed locally at all.

## Sync Mode / Firebase setup

Sync Mode is disabled until you plug in your own Firebase project — `app/google-services.json`
in this repo is a **placeholder** with dummy values (syntactically valid so the build
succeeds, but sign-in will fail until replaced).

1. **Create a Firebase project**: [console.firebase.google.com](https://console.firebase.google.com) → *Add project*.
2. **Register the Android app**: package name `com.example.kymcoagility50`. Download the
   real `google-services.json` and replace `app/google-services.json` with it.
3. **Enable Google Sign-In**: *Build → Authentication → Sign-in method → Google → Enable*.
4. **Enable Cloud Firestore**: *Build → Firestore Database → Create database* (production
   mode is fine).
5. **Set Firestore security rules** so each user can only read/write their own logbook:
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
6. Rebuild. Sync Mode's "Sign in with Google" button will now work, and the maintenance
   logbook will sync to `/users/{uid}/maintenanceEntries/*`.

Nothing else is ever written to Firestore — no bookmarks, no recent pages, no settings, no
PDF content.

## What's included vs. trimmed from the original brief

The original spec for this app also asked for a dealer-network directory, a bilingual/RTL
UI, and a quick-reference spec sheet. Per your later request, scope was narrowed to: PDF
reader (both documents) + search + bookmarks + the maintenance logbook + the two-mode
toggle. Those other screens aren't built, but nothing here blocks adding them later — the
`toc.json`/OCR-index structure already covers the passeport's dealer-network pages (13–16)
if you want to revisit that.

## Project layout

```
app/src/main/java/com/example/kymcoagility50/
  privacy/     PrivacyManager, NetworkGuard, AppMode — the enforcement core
  auth/        AuthManager (Firebase Auth + Google Sign-In, gated by PrivacyManager)
  work/        MaintenanceSyncWorker, SyncScheduler (WorkManager, Sync Mode only)
  data/        Room entities/DAOs, repositories, asset-JSON models
  pdf/         PdfPageRenderer (android.graphics.pdf.PdfRenderer wrapper)
  ocr/         (ML Kit fallback OCR lives in data/repository/OcrRepository.kt)
  ui/          Compose screens, nav graph, theme
```
