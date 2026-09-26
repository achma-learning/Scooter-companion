package com.example.kymcoagility50.ui.nav

import com.example.kymcoagility50.data.model.PdfSource

sealed class Routes(val route: String) {
    data object Splash : Routes("splash")
    data object Onboarding : Routes("onboarding")
    data object Home : Routes("home")
    data object Reader : Routes("reader/{pdf}/{page}") {
        fun build(pdf: PdfSource, page: Int) = "reader/${pdf.name}/$page"
    }
    data object Contents : Routes("contents")
    data object Search : Routes("search")
    data object Bookmarks : Routes("bookmarks")
    data object Maintenance : Routes("maintenance")
    data object Settings : Routes("settings")
}
