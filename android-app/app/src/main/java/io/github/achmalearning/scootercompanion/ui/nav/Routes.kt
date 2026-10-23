package io.github.achmalearning.scootercompanion.ui.nav

import io.github.achmalearning.scootercompanion.data.model.PdfSource

sealed class Routes(val route: String) {
    // Bottom-bar tabs
    data object Garage : Routes("garage")
    data object Service : Routes("service")
    data object Manual : Routes("manual")
    data object Guide : Routes("guide")
    data object More : Routes("more")

    // Manual
    data object Reader : Routes("reader/{pdf}/{page}") {
        fun build(pdf: PdfSource, page: Int) = "reader/${pdf.name}/$page"
    }
    data object Contents : Routes("contents")
    data object Search : Routes("search")
    data object Bookmarks : Routes("bookmarks")

    // Garage
    data object Task : Routes("task/{id}") { fun build(id: String) = "task/$id" }
    data object LogService : Routes("log?entry={entry}&task={task}&milestone={milestone}&product={product}") {
        fun build(entry: Long? = null, task: String? = null, milestone: Int? = null, product: String? = null) =
            "log?entry=${entry ?: -1}&task=${task ?: ""}&milestone=${milestone ?: -1}&product=${android.net.Uri.encode(product ?: "")}"
    }
    data object Passport : Routes("passport")
    data object Fuel : Routes("fuel")
    data object Wallet : Routes("wallet")
    data object Profile : Routes("profile")
    data object Sos : Routes("sos")
    data object Settings : Routes("settings")
    data object Insurance : Routes("insurance")
    data object Expenses : Routes("expenses")
    data object Recommendations : Routes("recommendations")
    data object Stats : Routes("stats")

    // Guide
    data object Checklist : Routes("checklist")
    data object Troubleshoot : Routes("troubleshoot")
    data object TroubleTree : Routes("trouble/{id}") { fun build(id: String) = "trouble/$id" }
    data object Issues : Routes("issues")
    data object HowTo : Routes("howto")
    data object GuideDetail : Routes("howto/{id}") { fun build(id: String) = "howto/$id" }
    data object Specs : Routes("specs")
    data object Oils : Routes("oils")
    data object Seasons : Routes("seasons")
    data object Parts : Routes("parts")
    data object Gear : Routes("gear")
}
