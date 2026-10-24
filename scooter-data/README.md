# Scooter data

One folder per scooter model: `scooter-data/<brand>/<model>/`. Everything the app knows about a
model lives there, so adding a scooter means adding a folder.

```
scooter-data/
  kymco/agility-50-4t/
    model.json        specs, maintenance schedule, documents (required)
    docs/             PDFs and their search indexes (optional)
  honda/sh125i/model.json
  sym/symphony-st-125-lc/model.json
```

**What ships in the app.** Every file except PDFs is bundled in the APK at build time, so picking
a model works offline. PDFs are never bundled: the app downloads one only when the owner taps
**Download** on the model's page (see `PRIVACY.md`).

## model.json

```jsonc
{
  "schema": 1,
  "id": "honda/sh125i",                 // must equal the folder path
  "brand": "Honda",                     // as owners write it
  "model": "SH125i",
  "years": "2020-",                     // optional
  "code": "SH125AD",                    // optional: factory model code
  "aliases": ["SH 125", "SH125i"],      // other ways owners type the model
  "engine": {
    "cc": 125,                          // 0 for electric
    "powertrain": "PETROL",             // PETROL | ELECTRIC
    "fuelSystem": "INJECTION",          // CARBURETOR | INJECTION
    "cooling": "LIQUID"                 // AIR | LIQUID
  },
  "verified": false,                    // optional: figures from spec databases, not the maker
  "figures": {                          // numbers the app compares between models
    "powerKw": 9.6, "powerRpm": 8250, "torqueNm": 12, "torqueRpm": 6500,
    "weightKg": 138, "weightKind": "kerb",   // dry | kerb | wet, as the maker states it
    "seatMm": 799, "tankL": 7, "wheelbaseMm": 1350, "consumptionL100": 2.2, "topSpeedKmh": 98
  },
  "specs": [                            // shown in this order
    {"key": "engineOil", "value": "SAE 10W-30, JASO MB"}
  ],
  "schedule": {                         // manufacturer intervals, by maintenance task id
    "engine-oil": {"km": 6000, "months": 12, "breakInKm": [1000]}
  },
  "docs": [                             // downloaded on demand
    {"id": "owner-manual", "title": {"en": "Owner's manual"},
     "url": "https://…/manual.pdf",     // the manufacturer's official file…
     "file": "docs/manual.pdf",         // …or a file in this folder (then give sha256)
     "bytes": 8120272, "sha256": "…", "lang": "en"}
  ],
  "note": {"en": "…", "fr": "…", "ar": "…"},   // optional, shown on the model's page
  "sources": [{"title": "Owner's manual p.76", "url": "https://…"}]
}
```

**Spec keys** with a translated label in the app: `engine`, `power`, `engineOil`,
`engineOilQty`, `gearOil`, `coolant`, `sparkPlug`, `valves`, `tyreFront`, `tyreRear`,
`pressureFront`, `pressureRear`, `brakeFront`, `brakeRear`, `transmission`, `brakeFluid`,
`battery`, `fuel`, `idle`. Any other key is shown as written.

**Schedule task ids** are the app's maintenance tasks (`genericTasks` in
`shared/content/agility50.json`): `engine-oil`, `oil-filter`, `gear-oil`, `air-filter-clean`,
`air-filter-replace`, `cvt-foam`, `carb-clean`, `fuel-filter`, `throttle-body`, `plug-replace`,
`valves`, `coolant-replace`, `radiator-clean`, `cvt-inspect`, `rollers`, `belt`, `clutch`,
`brake-pads`, `brake-fluid`, `tyre-rear`, `tyre-front`, `battery`, `throttle-cable`, `bolts`.
A task the schedule doesn't name keeps the app's usual interval for the engine size.

## Rules

- **Only figures from a source you can link**: the owner's manual, the manufacturer's site.
  Put the page numbers in `sources`. Leave a field out rather than guess; the app then tells the
  owner to check their booklet. For a model the maker no longer lists, a spec database will do:
  keep only the figures every source agrees on and set `"verified": false`.
- **Manuals**: link the manufacturer's official PDF with `url`. Commit a PDF to `docs/` only if
  it may be redistributed, and give its `sha256` (the app checks it after downloading).
- Run `python3 scripts/scooter_data/check.py` before opening a pull request; CI runs it too.
