package com.healthmate.app.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ListAlt
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.healthmate.app.features.dashboard.DashboardScreen
import com.healthmate.app.features.records.RecordListScreen
import com.healthmate.app.features.records.form.RecordFormScreen
import com.healthmate.app.features.records.form.RecordFormViewModel
import com.healthmate.app.features.settings.SettingsScreen
import kotlinx.coroutines.launch

private object Routes {
    const val DASHBOARD = "dashboard"
    const val RECORDS = "records"
    const val SETTINGS = "settings"
    const val FORM = "form?${RecordFormViewModel.ARG_RECORD_ID}={${RecordFormViewModel.ARG_RECORD_ID}}"

    fun form(recordId: Long? = null) =
        if (recordId == null) "form" else "form?${RecordFormViewModel.ARG_RECORD_ID}=$recordId"
}

private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    DASHBOARD(Routes.DASHBOARD, "Dashboard", Icons.Rounded.Dashboard),
    RECORDS(Routes.RECORDS, "Records", Icons.AutoMirrored.Rounded.ListAlt),
    SETTINGS(Routes.SETTINGS, "Settings", Icons.Rounded.Settings),
}

/** App shell: bottom navigation, the Add button, snackbars and the screens. */
@Composable
fun HealthMateApp() {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val onTopLevelScreen = Tab.entries.any { it.route == currentRoute }
    val showAddButton = currentRoute == Routes.DASHBOARD || currentRoute == Routes.RECORDS

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (onTopLevelScreen) {
                NavigationBar {
                    Tab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (showAddButton) {
                ExtendedFloatingActionButton(
                    onClick = { navController.navigate(Routes.form()) },
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text("Add record") },
                )
            }
        },
        // Each screen draws its own top bar, so only the bottom bar is padded here.
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.DASHBOARD,
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding),
        ) {
            composable(Routes.DASHBOARD) {
                DashboardScreen()
            }
            composable(Routes.RECORDS) {
                RecordListScreen(
                    snackbarHostState = snackbarHostState,
                    onRecordClick = { record -> navController.navigate(Routes.form(record.id)) },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(snackbarHostState = snackbarHostState)
            }
            composable(
                route = Routes.FORM,
                arguments = listOf(
                    navArgument(RecordFormViewModel.ARG_RECORD_ID) {
                        type = NavType.LongType
                        defaultValue = RecordFormViewModel.NO_ID
                    },
                ),
            ) {
                RecordFormScreen(
                    onBack = { navController.popBackStack() },
                    onFinished = { message ->
                        navController.popBackStack()
                        scope.launch { snackbarHostState.showSnackbar(message) }
                    },
                )
            }
        }
    }
}
