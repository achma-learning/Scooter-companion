package com.example.kymcoagility50.data.model

/** Which bundled PDF a page/bookmark/search-result belongs to. */
enum class PdfSource(val assetName: String, val indexAssetName: String, val displayName: String) {
    MANUAL("owner_manual.pdf", "manual_index.json", "Driver's Manual"),
    PASSPORT("passport.pdf", "passport_index.json", "Passeport / Maintenance")
}
