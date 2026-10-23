# Scooter Companion

An offline Android app that tracks **every cost of owning a scooter**: the purchase, fuel,
maintenance, insurance, gear, accessories and fines. It shows where the money goes each month
and warns you before you go over your own monthly target. Built first for scooter owners in
**Morocco** (50 cc, 125 cc, bigger scooters, maxi-scooters, electric), and open to every
country through region packs.

It grew out of *Agility Companion*, a companion app for one scooter, the Kymco Agility 50 4T.
Its service planner, manual reader and fuel log are still inside. The interval knowledge comes
from the [Marrakech maintenance wiki](https://achma-learning.github.io/kymco-agility-50-4T/).

> **Status:** being rebuilt into Scooter Companion. Done: new app identity, scooter profile
> and add-your-scooter wizard, spending ledger and insurance rules, save file v2,
> French / English / Arabic. In progress: the fuel, maintenance, insurance, expense and
> statistics screens. See [CONTEXT.md](CONTEXT.md) §7.

## What it does

- **Add your scooter**: brand, model, engine size, carburettor or injection, air or liquid
  cooling, petrol or electric. Bought new or used: the price you paid, and for a used scooter
  the price new (to see what you saved), first registration and previous owners. Links to
  used-scooter sites (Avito.ma, Leboncoin.fr). Plate, VIN, engine number, your own photo.
- **Spending ledger**: total spent with and without the purchase price, this month, last month,
  this year, average per month owned, cost per km. Months are projected from recurring costs
  only, so one insurance payment doesn't predict a disaster.
- **Insurance**: contracts of 1, 3, 6, 12 or any number of months with the amount you actually
  paid. Approximate prices for Morocco are shown as a hint only. Reminder every day once fewer
  than 20 days of cover remain, until you enter the renewal.
- **Carried over from Agility Companion**: the service planner (tuned for the Agility 50 4T
  for now), the fuel log built around the amount paid, the documents wallet with expiry
  countdown, the pre-ride checklist, the Agility manual reader with search, the owner's
  guide and the SOS numbers.
- **Save file**: export everything to one JSON file and import it on another phone. Agility
  Companion backups import too.
- **Languages**: French, English and Arabic (right-to-left), switchable in the app.

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

**Signing (required for releases).** Every release must be signed with the same private key,
or phones refuse to install it as an update. Create the key once and keep a copy somewhere
safe; losing it means every user has to reinstall.

```bash
keytool -genkeypair -v -keystore scooter-release.jks -alias scooter -keyalg RSA -keysize 4096 -validity 10000
base64 -w0 scooter-release.jks   # paste the output into RELEASE_KEYSTORE_BASE64
```

Then add the repository secrets `RELEASE_KEYSTORE_BASE64`, `RELEASE_KEYSTORE_PASSWORD`,
`RELEASE_KEY_ALIAS` (`scooter`) and `RELEASE_KEY_PASSWORD`. Without them, CI still tests
everything and uploads a debug APK as a workflow artifact, but publishes no release.

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
