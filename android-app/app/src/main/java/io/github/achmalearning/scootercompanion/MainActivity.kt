package io.github.achmalearning.scootercompanion

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.achmalearning.scootercompanion.data.model.PdfSource
import io.github.achmalearning.scootercompanion.data.repository.AppSettings
import io.github.achmalearning.scootercompanion.data.repository.ThemeMode
import io.github.achmalearning.scootercompanion.privacy.PrivacyManager
import io.github.achmalearning.scootercompanion.ui.nav.Routes
import io.github.achmalearning.scootercompanion.ui.screens.bookmarks.BookmarksScreen
import io.github.achmalearning.scootercompanion.ui.screens.contents.ContentsScreen
import io.github.achmalearning.scootercompanion.ui.screens.profile.BikeProfileScreen
import io.github.achmalearning.scootercompanion.ui.screens.garage.FuelScreen
import io.github.achmalearning.scootercompanion.ui.screens.garage.GarageNav
import io.github.achmalearning.scootercompanion.ui.screens.garage.GarageScreen
import io.github.achmalearning.scootercompanion.ui.screens.garage.PassportScreen
import io.github.achmalearning.scootercompanion.ui.screens.garage.WalletScreen
import io.github.achmalearning.scootercompanion.ui.screens.guide.ChecklistScreen
import io.github.achmalearning.scootercompanion.ui.screens.guide.FailureModesScreen
import io.github.achmalearning.scootercompanion.ui.screens.guide.GearScreen
import io.github.achmalearning.scootercompanion.ui.screens.guide.GuideDetailScreen
import io.github.achmalearning.scootercompanion.ui.screens.guide.GuideHubScreen
import io.github.achmalearning.scootercompanion.ui.screens.guide.GuideNav
import io.github.achmalearning.scootercompanion.ui.screens.guide.GuidesListScreen
import io.github.achmalearning.scootercompanion.ui.screens.guide.OilsScreen
import io.github.achmalearning.scootercompanion.ui.screens.guide.PartsScreen
import io.github.achmalearning.scootercompanion.ui.screens.guide.SeasonsScreen
import io.github.achmalearning.scootercompanion.ui.screens.guide.SpecsScreen
import io.github.achmalearning.scootercompanion.ui.screens.guide.TroubleTreeScreen
import io.github.achmalearning.scootercompanion.ui.screens.guide.TroubleshootScreen
import io.github.achmalearning.scootercompanion.ui.screens.home.ManualHubScreen
import io.github.achmalearning.scootercompanion.ui.screens.more.MoreNav
import io.github.achmalearning.scootercompanion.ui.screens.more.MoreScreen
import io.github.achmalearning.scootercompanion.ui.screens.more.SosScreen
import io.github.achmalearning.scootercompanion.ui.screens.money.ExpensesScreen
import io.github.achmalearning.scootercompanion.ui.screens.money.InsuranceScreen
import io.github.achmalearning.scootercompanion.ui.screens.money.RecommendationsScreen
import io.github.achmalearning.scootercompanion.ui.screens.stats.StatsScreen
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.ui.res.stringResource
import io.github.achmalearning.scootercompanion.ui.screens.onboarding.OnboardingScreen
import io.github.achmalearning.scootercompanion.ui.screens.reader.ReaderScreen
import io.github.achmalearning.scootercompanion.ui.screens.search.SearchScreen
import io.github.achmalearning.scootercompanion.ui.screens.service.LogServiceScreen
import io.github.achmalearning.scootercompanion.ui.screens.service.ServiceScreen
import io.github.achmalearning.scootercompanion.ui.screens.service.TaskDetailScreen
import io.github.achmalearning.scootercompanion.ui.screens.settings.SettingsScreen
import io.github.achmalearning.scootercompanion.ui.screens.splash.SplashScreen
import io.github.achmalearning.scootercompanion.ui.theme.ScooterTheme
import io.github.achmalearning.scootercompanion.work.ReminderWorker
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = AppContainer.getInstance(this)
        setContent {
            val settings by container.settingsRepository.settings.collectAsStateWithLifecycle(AppSettings())
            val dark = when (settings.theme) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            LaunchedEffect(settings.reminders) {
                if (settings.reminders) ReminderWorker.schedule(this@MainActivity) else ReminderWorker.cancel(this@MainActivity)
            }
            ScooterTheme(darkTheme = dark) { AppRoot() }
        }
    }
}

private data class BottomDest(val route: String, val label: Int, val icon: ImageVector)

private val homeDest = BottomDest(Routes.Garage.route, R.string.nav_home, Icons.Filled.TwoWheeler)
private val serviceDest = BottomDest(Routes.Service.route, R.string.nav_service, Icons.Filled.Build)
private val statsDest = BottomDest(Routes.Stats.route, R.string.nav_stats, Icons.Filled.BarChart)
private val manualDest = BottomDest(Routes.Manual.route, R.string.nav_manual, Icons.AutoMirrored.Filled.MenuBook)
private val guideDest = BottomDest(Routes.Guide.route, R.string.nav_guide, Icons.Filled.Explore)
private val moreDest = BottomDest(Routes.More.route, R.string.nav_more, Icons.Filled.MoreHoriz)

/** The Agility's scanned manual and owner's guide get tabs; other scooters get the guide in More. */
private fun bottomDestinations(agility: Boolean) =
    if (agility) listOf(homeDest, serviceDest, statsDest, manualDest, moreDest)
    else listOf(homeDest, serviceDest, statsDest, moreDest)

@Composable
private fun AppRoot() {
    var showSplash by remember { mutableStateOf(true) }
    val onboarded by PrivacyManager.onboarded.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    when {
        showSplash -> SplashScreen(onFinished = { showSplash = false })
        !onboarded -> OnboardingScreen(onDone = { scope.launch { PrivacyManager.completeOnboarding() } })
        else -> MainNav()
    }
}

private fun NavHostController.switchTab(route: String) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

@Composable
private fun MainNav() {
    val nav = rememberNavController()
    val backStackEntry by nav.currentBackStackEntryAsState()
    val current = backStackEntry?.destination
    val context = androidx.compose.ui.platform.LocalContext.current
    val agility = AppContainer.getInstance(context).garageRepository.snapshot
        .collectAsStateWithLifecycle(null).value?.isAgility == true
    val tabs = bottomDestinations(agility)
    val onTab = (tabs + guideDest).any { d -> current?.hierarchy?.any { it.route == d.route } == true }

    fun open(route: String) = nav.navigate(route)
    val back: () -> Unit = { nav.popBackStack() }
    val openReader: (PdfSource, Int) -> Unit = { pdf, page -> open(Routes.Reader.build(pdf, page)) }

    Scaffold(
        bottomBar = {
            if (onTab) NavigationBar {
                tabs.forEach { dest ->
                    val label = stringResource(dest.label)
                    NavigationBarItem(
                        selected = current?.hierarchy?.any { it.route == dest.route } == true,
                        onClick = { nav.switchTab(dest.route) },
                        icon = { Icon(dest.icon, contentDescription = label) },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(navController = nav, startDestination = Routes.Garage.route, modifier = Modifier.padding(padding)) {
            // ---- tabs
            composable(Routes.Garage.route) {
                GarageScreen(
                    GarageNav(
                        openTask = { open(Routes.Task.build(it)) },
                        logService = { open(Routes.LogService.build()) },
                        openService = { nav.switchTab(Routes.Service.route) },
                        openPassport = { open(Routes.Passport.route) },
                        openFuel = { open(Routes.Fuel.route) },
                        openWallet = { open(Routes.Wallet.route) },
                        openChecklist = { open(Routes.Checklist.route) },
                        openTroubleshoot = { open(Routes.Troubleshoot.route) },
                        openSos = { open(Routes.Sos.route) },
                        openProfile = { open(Routes.Profile.route) },
                        openSeasons = { open(Routes.Seasons.route) },
                        openExpenses = { open(Routes.Expenses.route) },
                        openInsurance = { open(Routes.Insurance.route) },
                        openStats = { nav.switchTab(Routes.Stats.route) }
                    )
                )
            }
            composable(Routes.Service.route) {
                ServiceScreen(openTask = { open(Routes.Task.build(it)) }, logService = { open(Routes.LogService.build(entry = it)) })
            }
            composable(Routes.Manual.route) {
                ManualHubScreen(
                    onOpen = openReader,
                    onOpenContents = { open(Routes.Contents.route) },
                    onOpenSearch = { open(Routes.Search.route) },
                    onOpenBookmarks = { open(Routes.Bookmarks.route) }
                )
            }
            composable(Routes.Guide.route) {
                GuideHubScreen(
                    GuideNav(
                        checklist = { open(Routes.Checklist.route) },
                        troubleshoot = { open(Routes.Troubleshoot.route) },
                        issues = { open(Routes.Issues.route) },
                        howTo = { open(Routes.HowTo.route) },
                        specs = { open(Routes.Specs.route) },
                        oils = { open(Routes.Oils.route) },
                        seasons = { open(Routes.Seasons.route) },
                        parts = { open(Routes.Parts.route) },
                        gear = { open(Routes.Gear.route) }
                    )
                )
            }
            composable(Routes.More.route) {
                MoreScreen(
                    MoreNav(
                        passport = { open(Routes.Passport.route) },
                        fuel = { open(Routes.Fuel.route) },
                        wallet = { open(Routes.Wallet.route) },
                        sos = { open(Routes.Sos.route) },
                        profile = { open(Routes.Profile.route) },
                        settings = { open(Routes.Settings.route) },
                        insurance = { open(Routes.Insurance.route) },
                        expenses = { open(Routes.Expenses.route) },
                        recommendations = { open(Routes.Recommendations.route) },
                        stats = { nav.switchTab(Routes.Stats.route) },
                        guide = { open(if (agility) Routes.Guide.route else Routes.Checklist.route) }
                    )
                )
            }

            // ---- manual
            composable(Routes.Contents.route) { ContentsScreen(onOpenPage = openReader) }
            composable(Routes.Search.route) { SearchScreen(onOpenResult = openReader) }
            composable(Routes.Bookmarks.route) { BookmarksScreen(onOpen = openReader) }
            composable(
                Routes.Reader.route,
                arguments = listOf(navArgument("pdf") { type = NavType.StringType }, navArgument("page") { type = NavType.IntType })
            ) { entry ->
                ReaderScreen(
                    source = PdfSource.valueOf(entry.arguments?.getString("pdf") ?: PdfSource.MANUAL.name),
                    startPage = entry.arguments?.getInt("page") ?: 1,
                    onBack = back
                )
            }

            // ---- garage
            composable(Routes.Task.route, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                TaskDetailScreen(
                    taskId = entry.arguments?.getString("id").orEmpty(),
                    onBack = back,
                    openGuide = { open(Routes.GuideDetail.build(it)) },
                    logService = { open(Routes.LogService.build(task = it)) }
                )
            }
            composable(
                Routes.LogService.route,
                arguments = listOf(
                    navArgument("entry") { type = NavType.LongType; defaultValue = -1L },
                    navArgument("task") { type = NavType.StringType; defaultValue = "" },
                    navArgument("milestone") { type = NavType.IntType; defaultValue = -1 },
                    navArgument("product") { type = NavType.StringType; defaultValue = "" }
                )
            ) { entry ->
                val args = entry.arguments
                LogServiceScreen(
                    entryId = args?.getLong("entry")?.takeIf { it >= 0 },
                    presetTask = args?.getString("task")?.takeIf { it.isNotBlank() },
                    presetMilestone = args?.getInt("milestone")?.takeIf { it >= 0 },
                    presetProduct = args?.getString("product")?.takeIf { it.isNotBlank() },
                    onDone = back
                )
            }
            composable(Routes.Passport.route) {
                PassportScreen(
                    onBack = back,
                    openProfile = { open(Routes.Profile.route) },
                    logMilestone = { open(Routes.LogService.build(milestone = it)) },
                    editEntry = { open(Routes.LogService.build(entry = it)) }
                )
            }
            composable(Routes.Fuel.route) { FuelScreen(onBack = back) }
            composable(Routes.Wallet.route) { WalletScreen(onBack = back) }
            composable(Routes.Profile.route) { BikeProfileScreen(onBack = back) }
            composable(Routes.Sos.route) { SosScreen(onBack = back, openInsurance = { open(Routes.Insurance.route) }) }
            composable(Routes.Insurance.route) { InsuranceScreen(onBack = back) }
            composable(Routes.Expenses.route) { ExpensesScreen(onBack = back) }
            composable(Routes.Recommendations.route) {
                RecommendationsScreen(onBack = back, logTyres = { open(Routes.LogService.build(product = it)) })
            }
            composable(Routes.Stats.route) { StatsScreen() }
            composable(Routes.Settings.route) { SettingsScreen(onBack = back) }

            // ---- guide
            composable(Routes.Checklist.route) { ChecklistScreen(onBack = back) }
            composable(Routes.Troubleshoot.route) { TroubleshootScreen(onBack = back, openTree = { open(Routes.TroubleTree.build(it)) }) }
            composable(Routes.TroubleTree.route, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                TroubleTreeScreen(entry.arguments?.getString("id").orEmpty(), onBack = back, openGuide = { open(Routes.GuideDetail.build(it)) })
            }
            composable(Routes.Issues.route) { FailureModesScreen(onBack = back) }
            composable(Routes.HowTo.route) { GuidesListScreen(onBack = back, openGuide = { open(Routes.GuideDetail.build(it)) }) }
            composable(Routes.GuideDetail.route, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                GuideDetailScreen(entry.arguments?.getString("id").orEmpty(), onBack = back)
            }
            composable(Routes.Specs.route) { SpecsScreen(onBack = back) }
            composable(Routes.Oils.route) { OilsScreen(onBack = back) }
            composable(Routes.Seasons.route) { SeasonsScreen(onBack = back) }
            composable(Routes.Parts.route) { PartsScreen(onBack = back) }
            composable(Routes.Gear.route) { GearScreen(onBack = back) }
        }
    }
}
