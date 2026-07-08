package com.xxmemory.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.xxmemory.app.ui.home.HomeScreen
import com.xxmemory.app.ui.import.ImportScreen
import com.xxmemory.app.ui.review.ReviewScreen
import com.xxmemory.app.ui.statistics.StatisticsScreen
import com.xxmemory.app.ui.settings.SettingsScreen
import com.xxmemory.app.ui.theme.Primary
import com.xxmemory.app.ui.theme.TextSecondary

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Home : Screen("home", "主页", Icons.Filled.Home, Icons.Outlined.Home)
    object Import : Screen("import", "导入", Icons.Filled.FileUpload, Icons.Outlined.FileUpload)
    object Review : Screen("review", "复习", Icons.Filled.School, Icons.Outlined.School)
    object Statistics : Screen("statistics", "统计", Icons.Filled.BarChart, Icons.Outlined.BarChart)
    object Settings : Screen("settings", "设置", Icons.Filled.Settings, Icons.Outlined.Settings)
}

val bottomNavItems = listOf(
    Screen.Home, Screen.Import, Screen.Review, Screen.Statistics, Screen.Settings
)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            BottomNavBar(navController)
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(onNavigateToReview = {
                    navController.navigate(Screen.Review.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                    }
                })
            }
            composable(Screen.Import.route) {
                ImportScreen()
            }
            composable(Screen.Review.route) {
                ReviewScreen()
            }
            composable(Screen.Statistics.route) {
                StatisticsScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
        }
    }
}

@Composable
private fun BottomNavBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar(
        containerColor = androidx.compose.ui.graphics.Color.White
    ) {
        bottomNavItems.forEach { screen ->
            val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                        contentDescription = screen.title
                    )
                },
                label = { Text(text = screen.title) },
                selected = selected,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Primary,
                    selectedTextColor = Primary,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary
                )
            )
        }
    }
}