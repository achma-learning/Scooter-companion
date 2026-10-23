# Scooter Companion

An offline Android app that tracks **every cost of owning a scooter**: the purchase, fuel,
maintenance, insurance, gear, accessories and fines. It shows where the money goes each month
and warns you before you go over your own monthly target. Built first for scooter owners in
**Morocco** (50 cc, 125 cc, bigger scooters, maxi-scooters, electric), and open to every
country through region packs.

It grew out of *Agility Companion*, a companion app for one scooter, the Kymco Agility 50 4T.
Its service planner, manual reader and fuel log are still inside. The interval knowledge comes
from the [Marrakech maintenance wiki](https://achma-learning.github.io/kymco-agility-50-4T/).

## What it does

- **Add your scooter**: brand, model, engine size, carburettor or injection, air or liquid
  cooling, petrol or electric. Bought new or used: the price you paid, and for a used scooter
  the price new (to see what you saved), first registration and previous owners, with links
  to used-scooter sites (Avito.ma, Leboncoin.fr). Plate, VIN, engine number, your own photo.
- **Fuel log**: tap the amount you paid (10 / 15 / 20 / 30 / 50 DH, more for bigger
  scooters) or Full tank; litres are worked out from the price per litre (or tap Today's
  price). Consumption in L/100 km and km per litre, cost per km.
- **Maintenance**: reminders tuned to your scooter's engine class and type (coolant for
  liquid-cooled, throttle body for injection, carburettor for carburetted, none of the engine
  jobs for electric). Log each service with the parts, the labour, the product used (e.g.
  Ipone R4000, Michelin City Grip 2), or any custom job.
- **Insurance**: contracts of 1, 3, 6, 12 or any number of months with the amount you actually
  paid. Approximate prices for Morocco are only a hint. A reminder every day once fewer than
  20 days of cover remain, until you enter the renewal.
- **Expenses**: accessories, riding gear, fines (150 / 400 / 700 DH and the reason), parking,
  washing, papers, charging, anything else.
- **Statistics**: spending per month against your target, running total, where the money goes,
  price per litre, consumption, a spending calendar, and plain-language insights. The home
  screen shows this month against the target and warns before you go over. A report of last
  month arrives on the 1st.
- **Recommendations**: City Grip 2 tyres, a top case sized for your scooter (29 / 33 / 35 L),
  an ECE 22.06 helmet, a riding vest, gloves, a lock. "I bought it" adds the expense.
- **Pre-ride checklist, documents wallet, SOS** (Moroccan emergency numbers including 5050
  for motorway assistance, what to do after an accident, your insurance details).
- **Save file**: export everything to one file, share it (WhatsApp, Quick Share, Bluetooth…)
  and import it on another phone. Agility Companion backups import too.
- **Report for AI**: all your costs as plain text with a suggested question, to paste into an
  AI assistant. Plate, VIN and policy numbers can be hidden.
- **Languages**: French, English and Arabic (right-to-left), switchable in the app.
- **Kymco Agility 50 4T owners** also keep the Marrakech maintenance schedule, the digital
  dealer passport, the scanned manual with search and the owner's guide.

## Get it

Open the latest [GitHub Release](../../releases/latest) on your phone and install
`scooter-companion.apk` (allow "install unknown apps" once). Later versions install from
inside the app: **Settings → App updates → Check for updates**.

**Coming from Agility Companion?** Scooter Companion is a new app (new name, new signing key),
so it installs next to the old one instead of replacing it. In Agility Companion: **Settings →
Export**. In Scooter Companion, on the first screen: **I have a save file**. Then uninstall
Agility Companion. Its last APK is kept in
[kymco-agility-50-4T/apk-backup](https://github.com/achma-learning/kymco-agility-50-4T/tree/main/apk-backup).

## Privacy

No account, no analytics, no ads. Everything stays on the phone. The app goes online only when
you tap **Today's price** (fuel) or **Check for updates**. Details: [PRIVACY.md](PRIVACY.md).

## Releases

`.github/workflows/build-and-release.yml` runs on every push to `main` (and on `v*` tags):

1. Regenerates the knowledge base and fails if `shared/content/agility50.json` is stale.
2. Runs the fuel-price parser tests, the website tests and the Android unit tests (domain
   logic, Room migrations, save-file interop, every screen rendered, the wizard in three
   languages).
3. Builds the release APK (`versionCode` = run number) and publishes a GitHub Release
   `v3.0.<run>` with `scooter-companion.apk`.

**Turning on releases (once, about 2 minutes, works from a phone).** Releases are signed with
the app's own key, so that each new version installs as an update. GitHub creates and keeps
that key for you:

1. Invent a long password (at least 20 characters, for example 5 or 6 random words) and save
   it in your notes or password manager. Don't share it.
2. Open **Settings → Secrets and variables → Actions → New repository secret**. Name:
   `RELEASE_KEYSTORE_PASSWORD`. Secret: your password. Click **Add secret**.
3. Open **Actions → Create release key → Run workflow**.

GitHub then creates the key, stores it in the repo encrypted with your password
(`android-app/app/signing/release.jks.gpg`, AES-256) and publishes the first release. Every
later push to `main` is released automatically. Keep the password: without it no future
release can install as an update, and users would have to reinstall. Until step 3, CI still
tests everything and uploads a debug APK as a workflow artifact, but publishes no release.

*Advanced:* to use a keystore of your own instead, put it in the secret
`RELEASE_KEYSTORE_BASE64` (base64), with `RELEASE_KEYSTORE_PASSWORD`, and `RELEASE_KEY_ALIAS` /
`RELEASE_KEY_PASSWORD` if they differ from `scooter` and the keystore password.

**Fuel price**: `.github/workflows/fuel-price.yml` runs weekly. It reads Morocco's national
average gasoline price from GlobalPetrolPrices.com (`scripts/fuel/fetch_price.py`) and commits
`fuel-price.json` to the `data` branch, which the app downloads only on a tap.

## Project layout

```
android-app/                 the Android app; run Gradle from here
  app/src/main/java/io/github/achmalearning/scootercompanion/
    domain/                  ExpenseLedger, Insurance, Vehicle, ServicePlanner, FuelStats, Dates (pure Kotlin)
    data/                    Room (garage, logbook, insurance, expenses), backup format, region packs, repositories
    ui/screens/              onboarding, profile, garage, service, guide, manual, more, settings…
    work/ReminderWorker.kt   daily local reminders
  app/schemas/               exported Room schemas, one per database version
shared/
  assets/regions/MA.json     Morocco region pack (currency, brands, fuel chips, insurance hints, fines)
  content/agility50.json     Agility 50 knowledge base, GENERATED by scripts/content/build_content.py
  docs/                      Agility manual and passport PDFs with their OCR indexes
  fixtures/                  save-file fixtures: backup-sample.json (Agility v1) and backup-sample-v2.json
scripts/                     knowledge-base generator, weekly fuel-price scraper, website build
web/                         the Agility Companion website (frozen: bug fixes only)
```

## Develop

```bash
cd android-app
./gradlew testDebugUnitTest   # add -PscreenshotsDir=/tmp/shots to save a PNG per screen
./gradlew assembleDebug       # app/build/outputs/apk/debug/app-debug.apk
```

From the repo root: `python3 scripts/content/build_content.py` (regenerate the knowledge base),
`node --test web/tests/*.test.mjs`, `python3 -m unittest discover -s scripts/fuel`.

**Add your country**: copy `shared/assets/regions/MA.json` to `<country code>.json` and change
the currency, brands, fuel amount chips, insurance hints and fine presets. See
[CONTRIBUTING.md](CONTRIBUTING.md).

## License and sources

[MIT](LICENSE). Intervals, prices and procedures for the Agility 50 come from the
[Kymco Agility 50 4T Marrakech wiki](https://achma-learning.github.io/kymco-agility-50-4T/)
([repo](https://github.com/achma-learning/kymco-agility-50-4T)). Factory figures come from the
official Driver's Manual (KN10EM, p.39–40) and the KN10 workshop manual. Not affiliated with
KYMCO or any other manufacturer.
