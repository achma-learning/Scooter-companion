package io.github.achmalearning.scootercompanion

import android.os.Bundle
import androidx.activity.ComponentActivity
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
import io.github.achmalearning.scootercompanion.ui.screens.garage.BikeProfileScreen
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

class MainActivity : ComponentActivity() {
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

private data class BottomDest(val route: String, val label: String, val icon: ImageVector)

private val bottomDestinations = listOf(
    BottomDest(Routes.Garage.route, "Garage", Icons.Filled.TwoWheeler),
    BottomDest(Routes.Service.route, "Service", Icons.Filled.Build),
    BottomDest(Routes.Manual.route, "Manual", Icons.AutoMirrored.Filled.MenuBook),
    BottomDest(Routes.Guide.route, "Guide", Icons.Filled.Explore),
    BottomDest(Routes.More.route, "More", Icons.Filled.MoreHoriz)
)

@Composable
private fun AppRoot() {
    var showSplash by remember { mutableStateOf(true) }
    val onboarded by PrivacyManager.onboarded.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    when {
        showSplash -> SplashScreen(onFinished = { showSplash = false })
        !onboarded -> OnboardingScreen(onStart = { scope.launch { PrivacyManager.completeOnboarding() } })
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
    val onTab = bottomDestinations.any { d -> current?.hierarchy?.any { it.route == d.route } == true }

    fun open(route: String) = nav.navigate(route)
    val back: () -> Unit = { nav.popBackStack() }
    val openReader: (PdfSource, Int) -> Unit = { pdf, page -> open(Routes.Reader.build(pdf, page)) }

    Scaffold(
        bottomBar = {
            if (onTab) NavigationBar {
                bottomDestinations.forEach { dest ->
                    NavigationBarItem(
                        selected = current?.hierarchy?.any { it.route == dest.route } == true,
                        onClick = { nav.switchTab(dest.route) },
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                        label = { Text(dest.label) }
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
                        openSeasons = { open(Routes.Seasons.route) }
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
                        settings = { open(Routes.Settings.route) }
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
                    navArgument("milestone") { type = NavType.IntType; defaultValue = -1 }
                )
            ) { entry ->
                val args = entry.arguments
                LogServiceScreen(
                    entryId = args?.getLong("entry")?.takeIf { it >= 0 },
                    presetTask = args?.getString("task")?.takeIf { it.isNotBlank() },
                    presetMilestone = args?.getInt("milestone")?.takeIf { it >= 0 },
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
            composable(Routes.Sos.route) { SosScreen(onBack = back, openWallet = { open(Routes.Wallet.route) }) }
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
