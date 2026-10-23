#!/usr/bin/env python3
"""Fetches Morocco's national average gasoline (Octane-95) price and writes fuel-price.json.

Run weekly by .github/workflows/fuel-price.yml, which publishes the file on the `data`
branch. The app and the website download that one file when the owner taps "Today's price",
so the page is parsed in exactly one place, and the reader's request goes to GitHub, never to
the price site.

Source: GlobalPetrolPrices.com (CC BY-NC-ND 3.0, attribution kept in the JSON). It's one
national weekly average; a station's real price can differ by brand and city.

    python3 scripts/fuel/fetch_price.py [out.json]
"""
import datetime
import json
import pathlib
import re
import sys
import urllib.request

SOURCE_URL = "https://www.globalpetrolprices.com/Morocco/gasoline_prices/"

# "The current gasoline price in  Morocco is  MAD 15.18 per liter or USD 1.52 per liter and
# was updated on 05-Oct-2026."  Group 1 = MAD price, group 2 = date as dd-Mon-yyyy.
SENTENCE = re.compile(
    r"current gasoline price in\s+Morocco is\s+MAD\s+(\d+(?:\.\d+)?)\s+per liter.*?updated on\s+(\d{2}-[A-Z][a-z]{2}-\d{4})",
    re.S,
)
# Pump prices outside this band (MAD/L) mean the page changed, not the market.
MIN_PRICE, MAX_PRICE = 5.0, 40.0


def parse(html: str) -> dict:
    m = SENTENCE.search(html)
    if not m:
        raise ValueError("price sentence not found; the page layout probably changed")
    price = float(m.group(1))
    if not MIN_PRICE <= price <= MAX_PRICE:
        raise ValueError(f"implausible price {price} MAD/L")
    as_of = datetime.datetime.strptime(m.group(2), "%d-%b-%Y").date().isoformat()
    return {
        "format": "agility-fuel-price",
        "version": 1,
        "country": "MA",
        "fuel": "gasoline-95",
        "currency": "MAD",
        "pricePerLiter": price,
        "asOf": as_of,
        "source": "GlobalPetrolPrices.com",
        "sourceUrl": SOURCE_URL,
        "license": "CC BY-NC-ND 3.0",
        "note": "National weekly average; your station's price may differ.",
    }


def main() -> None:
    out = pathlib.Path(sys.argv[1] if len(sys.argv) > 1 else "fuel-price.json")
    req = urllib.request.Request(SOURCE_URL, headers={"User-Agent": "scooter-companion-fuel-bot (weekly; github.com/achma-learning/scooter-companion)"})
    with urllib.request.urlopen(req, timeout=30) as res:
        html = res.read().decode("utf-8", "replace")
    data = parse(html)
    out.write_text(json.dumps(data, indent=1) + "\n", encoding="utf-8")
    print(f"wrote {out}: {data['pricePerLiter']} MAD/L as of {data['asOf']}")


if __name__ == "__main__":
    main()
