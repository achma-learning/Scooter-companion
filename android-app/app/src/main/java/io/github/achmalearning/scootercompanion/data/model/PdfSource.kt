package io.github.achmalearning.scootercompanion.data.model

/** The Agility 50 4T's folder in scooter-data, bundled as assets without its PDFs. */
const val AGILITY_DOCS = "scooter-data/kymco/agility-50-4t/docs"

/**
 * Which Agility document a page/bookmark/search-result belongs to. Its id matches the doc id in
 * scooter-data/kymco/agility-50-4t/model.json; the PDF itself is downloaded on demand.
 */
enum class PdfSource(val indexAssetName: String, val displayName: String) {
    MANUAL("$AGILITY_DOCS/manual_index.json", "Driver's Manual"),
    PASSPORT("$AGILITY_DOCS/passport_index.json", "Passeport / Maintenance")
}
