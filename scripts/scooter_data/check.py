#!/usr/bin/env python3
"""Checks every scooter-data/<brand>/<model>/model.json (format in scooter-data/README.md).

Run from anywhere: python3 scripts/scooter_data/check.py. Exits non-zero on the first broken
file, with every problem it found in that file.
"""
import hashlib
import json
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
DATA = ROOT / "scooter-data"
CONTENT = ROOT / "shared" / "content" / "agility50.json"

FIGURES = {"powerKw", "powerRpm", "torqueNm", "torqueRpm", "weightKg", "weightKind", "seatMm", "tankL",
           "wheelbaseMm", "consumptionL100", "topSpeedKmh"}

ENUMS = {
    "powertrain": {"PETROL", "ELECTRIC"},
    "fuelSystem": {"CARBURETOR", "INJECTION"},
    "cooling": {"AIR", "LIQUID"},
}


def problems(path: pathlib.Path, task_ids: set) -> list:
    out = []
    try:
        m = json.loads(path.read_text(encoding="utf-8"))
    except ValueError as e:
        return [f"not valid JSON: {e}"]
    folder = path.parent.relative_to(DATA).as_posix()
    if m.get("schema") != 1:
        out.append("schema must be 1")
    if m.get("id") != folder:
        out.append(f'id must be "{folder}" (the folder path), not "{m.get("id")}"')
    for field in ("brand", "model"):
        if not str(m.get(field, "")).strip():
            out.append(f"{field} is missing")
    engine = m.get("engine", {})
    if not isinstance(engine.get("cc"), int) or engine["cc"] < 0:
        out.append("engine.cc must be a whole number (0 for electric)")
    electric = engine.get("powertrain") == "ELECTRIC"
    for field, allowed in ENUMS.items():
        # An electric scooter has no fuel system and may not say how its motor is cooled.
        if electric and field != "powertrain" and field not in engine:
            continue
        if engine.get(field) not in allowed:
            out.append(f"engine.{field} must be one of {sorted(allowed)}")
    if engine.get("powertrain") == "PETROL" and engine.get("cc", 0) <= 0:
        out.append("a petrol scooter needs engine.cc")
    if "verified" in m and not isinstance(m["verified"], bool):
        out.append("verified must be true or false")
    for key, value in m.get("figures", {}).items():
        if key not in FIGURES:
            out.append(f'figures: unknown "{key}" (known: {", ".join(sorted(FIGURES))})')
        elif key == "weightKind":
            if value not in ("dry", "kerb", "wet", "unstated"):
                out.append('figures.weightKind must be "dry", "kerb", "wet" or "unstated" (the maker doesn\'t say)')
        elif not isinstance(value, (int, float)) or value <= 0:
            out.append(f"figures.{key} must be a positive number")
    if "weightKg" in m.get("figures", {}) and "weightKind" not in m["figures"]:
        out.append("figures.weightKind is needed with weightKg (dry, kerb or wet)")
    for i, s in enumerate(m.get("specs", [])):
        if not s.get("key") or not str(s.get("value", "")).strip():
            out.append(f"specs[{i}] needs a key and a value")
    for task, iv in m.get("schedule", {}).items():
        if task not in task_ids:
            out.append(f'schedule: unknown task "{task}"')
        if not any(isinstance(iv.get(k), int) and iv[k] > 0 for k in ("km", "months")):
            out.append(f'schedule["{task}"] needs km and/or months (positive whole numbers)')
    for i, d in enumerate(m.get("docs", [])):
        if not d.get("id") or not d.get("title", {}).get("en"):
            out.append(f"docs[{i}] needs an id and an English title")
        if bool(d.get("url")) == bool(d.get("file")):
            out.append(f"docs[{i}] needs exactly one of url (official site) or file (in this folder)")
        if d.get("url") and not d["url"].startswith("https://"):
            out.append(f"docs[{i}].url must be https")
        if d.get("file"):
            f = path.parent / d["file"]
            if not f.is_file():
                out.append(f"docs[{i}].file {d['file']} doesn't exist")
            else:
                if d.get("bytes") != f.stat().st_size:
                    out.append(f"docs[{i}].bytes should be {f.stat().st_size}")
                digest = hashlib.sha256(f.read_bytes()).hexdigest()
                if d.get("sha256") != digest:
                    out.append(f"docs[{i}].sha256 should be {digest}")
    if not m.get("sources"):
        out.append("sources: say where the figures come from")
    return out


def main() -> int:
    content = json.loads(CONTENT.read_text(encoding="utf-8"))
    task_ids = {t["id"] for t in content["genericTasks"]}
    files = sorted(DATA.glob("*/*/model.json"))
    if not files:
        print("no scooter-data/<brand>/<model>/model.json found")
        return 1
    ids = {}
    for path in files:
        errs = problems(path, task_ids)
        if errs:
            print(f"{path.relative_to(ROOT)}:")
            for e in errs:
                print(f"  - {e}")
            return 1
        ids[path.parent.relative_to(DATA).as_posix()] = path
    stray = [p for p in DATA.rglob("model.json") if p not in files]
    if stray:
        print("model.json must sit exactly two folders deep (brand/model):", *stray, sep="\n  ")
        return 1
    print(f"{len(files)} scooter models OK: {', '.join(ids)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
