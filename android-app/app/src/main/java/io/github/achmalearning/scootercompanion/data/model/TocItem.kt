package io.github.achmalearning.scootercompanion.data.model

import kotlinx.serialization.Serializable

@Serializable
data class TocItem(
    val title: String,
    val page: Int,
    val children: List<TocItem> = emptyList()
)

@Serializable
data class TocDocument(
    val manual: List<TocItem>,
    val passport: List<TocItem>
) {
    fun forPdf(pdf: PdfSource): List<TocItem> = when (pdf) {
        PdfSource.MANUAL -> manual
        PdfSource.PASSPORT -> passport
    }
}
