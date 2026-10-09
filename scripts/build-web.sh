#!/usr/bin/env bash
# Assembles the static website into dist/web (or $1): web/ + the shared knowledge base +
# the PDFs/OCR indexes. The result is plain files — host it on GitHub Pages, Netlify,
# any static host, or open it with `npx http-server dist/web`.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="${1:-$ROOT/dist/web}"

rm -rf "$OUT"
mkdir -p "$OUT/data" "$OUT/docs"
cp -R "$ROOT/web/." "$OUT/"
rm -rf "$OUT/tests" "$OUT/package.json"
cp "$ROOT/shared/content/agility50.json" "$OUT/data/"
cp "$ROOT/shared/docs/"* "$OUT/docs/"

# Stamp the service-worker cache version so each deploy refreshes offline copies.
VERSION="$(git -C "$ROOT" rev-parse --short HEAD 2>/dev/null || date +%s)-$(date +%Y%m%d%H%M)"
sed -i.bak "s/__BUILD__/$VERSION/" "$OUT/sw.js" && rm "$OUT/sw.js.bak"
touch "$OUT/.nojekyll"
echo "Website built in $OUT (version $VERSION)"
