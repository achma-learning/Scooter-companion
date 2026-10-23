package io.github.achmalearning.scootercompanion.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ManualIndexPage(
    val page: Int,
    val text: String
)

@Serializable
data class ManualIndex(
    val pageCount: Int,
    val pages: List<ManualIndexPage>
)
