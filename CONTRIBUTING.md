# Contributing

This app is becoming **Scooter Companion**: an offline expense tracker and maintenance log for
scooter owners anywhere, starting with Morocco. Issues and pull requests are welcome.

## Layout

| Folder | What |
|---|---|
| `android-app/` | The Android app (Kotlin, Jetpack Compose, Room). Everything Gradle lives here. |
| `shared/` | Data the app ships with: the knowledge base JSON, the backup fixture, the Agility manuals. |
| `scripts/` | The knowledge-base generator and the weekly fuel-price scraper. |
| `web/` | The Agility Companion website. **Frozen**: bug fixes only, new features go to the Android app. |

## Build and test

Needs JDK 17 and the Android SDK (compileSdk 35). The Gradle wrapper is committed.

```bash
cd android-app
./gradlew testDebugUnitTest   # unit + Robolectric screen tests
./gradlew assembleDebug       # app/build/outputs/apk/debug/app-debug.apk
```

From the repo root: `node --test web/tests/*.test.mjs` and
`python3 -m unittest discover -s scripts/fuel`.

## Rules that protect users' data

- People's whole spending history lives on their phone. **Never break a backup or a database
  migration.** Room schema changes need a real `Migration` and the exported schema JSON;
  new backup fields need a default value, and old backup files must still import.
- The app works offline. Network calls happen only when the user taps a button for public data
  (today's fuel price, check for updates). Never send user data anywhere.
- Never hand-edit `shared/content/*.json`: edit `scripts/content/build_content.py` and
  regenerate. CI fails if the JSON is stale.

Read `CONTEXT.md` before a larger change: it lists the fragile parts and why they are that way.

## Pull requests

Keep them focused, add or update tests for logic changes, and make sure CI is green.
