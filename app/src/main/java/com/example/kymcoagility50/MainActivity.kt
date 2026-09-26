package com.example.kymcoagility50

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.kymcoagility50.data.model.PdfSource
import com.example.kymcoagility50.privacy.AppMode
import com.example.kymcoagility50.privacy.PrivacyManager
import com.example.kymcoagility50.ui.nav.Routes
import com.example.kymcoagility50.ui.screens.bookmarks.BookmarksScreen
import com.example.kymcoagility50.ui.screens.contents.ContentsScreen
import com.example.kymcoagility50.ui.screens.home.HomeScreen
import com.example.kymcoagility50.ui.screens.maintenance.MaintenanceScreen
import com.example.kymcoagility50.ui.screens.onboarding.OnboardingScreen
import com.example.kymcoagility50.ui.screens.reader.ReaderScreen
import com.example.kymcoagility50.ui.screens.search.SearchScreen
import com.example.kymcoagility50.ui.screens.settings.SettingsScreen
import com.example.kymcoagility50.ui.screens.splash.SplashScreen
import com.example.kymcoagility50.ui.theme.KymcoTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KymcoTheme { AppRoot() }
        }
    }
}

private data class BottomDest(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val bottomDestinations = listOf(
    BottomDest(Routes.Home.route, "Home", Icons.Filled.Home),
    BottomDest(Routes.Contents.route, "Contents", Icons.Filled.MenuBook),
    BottomDest(Routes.Search.route, "Search", Icons.Filled.Search),
    BottomDest(Routes.Bookmarks.route, "Bookmarks", Icons.Filled.Bookmark),
    BottomDest(Routes.Maintenance.route, "Logbook", Icons.Filled.Build),
    BottomDest(Routes.Settings.route, "Settings", Icons.Filled.Settings)
)

@Composable
private fun AppRoot() {
    var showSplash by remember { mutableStateOf(true) }
    val onboarded by PrivacyManager.onboarded.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScopeCompat()

    when {
        showSplash -> SplashScreen(onFinished = { showSplash = false })
        !onboarded -> OnboardingScreen(onChoose = { mode ->
            scope.launch { PrivacyManager.completeOnboarding(mode) }
        })
        else -> MainNav()
    }
}

@Composable
private fun rememberCoroutineScopeCompat() = androidx.compose.runtime.rememberCoroutineScope()

@Composable
private fun MainNav() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination
            NavigationBar {
                bottomDestinations.forEach { dest ->
                    NavigationBarItem(
                        selected = currentRoute?.hierarchy?.any { it.route == dest.route } == true,
                        onClick = {
                            navController.navigate(dest.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                        label = { Text(dest.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.Home.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.Home.route) {
                HomeScreen(
                    onOpenManual = { page -> navController.navigate(Routes.Reader.build(PdfSource.MANUAL, page)) },
                    onOpenPassport = { page -> navController.navigate(Routes.Reader.build(PdfSource.PASSPORT, page)) },
                    onOpenContents = { navController.navigate(Routes.Contents.route) },
                    onOpenSearch = { navController.navigate(Routes.Search.route) },
                    onOpenBookmarks = { navController.navigate(Routes.Bookmarks.route) },
                    onOpenMaintenance = { navController.navigate(Routes.Maintenance.route) }
                )
            }
            composable(Routes.Contents.route) {
                ContentsScreen(onOpenPage = { pdf, page -> navController.navigate(Routes.Reader.build(pdf, page)) })
            }
            composable(Routes.Search.route) {
                SearchScreen(onOpenResult = { pdf, page -> navController.navigate(Routes.Reader.build(pdf, page)) })
            }
            composable(Routes.Bookmarks.route) {
                BookmarksScreen(onOpen = { pdf, page -> navController.navigate(Routes.Reader.build(pdf, page)) })
            }
            composable(Routes.Maintenance.route) { MaintenanceScreen() }
            composable(Routes.Settings.route) { SettingsScreen() }
            composable(
                route = Routes.Reader.route,
                arguments = listOf(
                    androidx.navigation.navArgument("pdf") { type = androidx.navigation.NavType.StringType },
                    androidx.navigation.navArgument("page") { type = androidx.navigation.NavType.IntType }
                )
            ) { backStackEntry ->
                val pdfArg = backStackEntry.arguments?.getString("pdf") ?: PdfSource.MANUAL.name
                val page = backStackEntry.arguments?.getInt("page") ?: 1
                ReaderScreen(
                    source = PdfSource.valueOf(pdfArg),
                    startPage = page,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
