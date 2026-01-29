package ru.vibe.containerinspector.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import ru.vibe.containerinspector.ui.screens.*
import ru.vibe.containerinspector.viewmodel.MainViewModel

sealed class Screen(val route: String) {
    object Auth : Screen("auth")
    object Dashboard : Screen("dashboard")
    object OCRScan : Screen("ocr_scan")
    object OCRConfirm : Screen("ocr_confirm")
    object Inspection : Screen("inspection")
    object Summary : Screen("summary")
    object History : Screen("history")
    object Profile : Screen("profile")
    object AdminLogin : Screen("admin_login")
    object Settings : Screen("settings")
}

@Composable
fun NavGraph(navController: NavHostController, viewModel: MainViewModel) {
    NavHost(
        navController = navController,
        startDestination = Screen.Auth.route
    ) {
        composable(Screen.Auth.route) {
            AuthScreen(
                viewModel = viewModel,
                onAuthSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(viewModel) {
                navController.navigate(Screen.Auth.route) {
                    popUpTo(Screen.Settings.route) { inclusive = true }
                }
            }
        }
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                viewModel = viewModel,
                onStartInspection = { report ->
                    viewModel.startInspection(report)
                    navController.navigate(Screen.Inspection.route)
                },
                onNewScan = { navController.navigate(Screen.OCRScan.route) },
                onNavigateToHistory = { navController.navigate(Screen.History.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) }
            )
        }
        composable(Screen.History.route) {
            HistoryScreen(
                viewModel = viewModel,
                onNavigateToDashboard = { navController.navigate(Screen.Dashboard.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onNewScan = { navController.navigate(Screen.OCRScan.route) }
            )
        }
        composable(Screen.Profile.route) {
            ProfileScreen(
                viewModel = viewModel,
                onNavigateToDashboard = { navController.navigate(Screen.Dashboard.route) },
                onNavigateToHistory = { navController.navigate(Screen.History.route) },
                onNavigateToAdmin = { navController.navigate(Screen.AdminLogin.route) },
                onLogout = {
                    viewModel.resetSession()
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNewScan = { navController.navigate(Screen.OCRScan.route) }
            )
        }
        composable(Screen.OCRScan.route) {
            OCRScanScreen(viewModel) {
                navController.navigate(Screen.OCRConfirm.route)
            }
        }
        composable(Screen.OCRConfirm.route) {
            OCRConfirmScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onConfirm = { 
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Inspection.route) {
            InspectionScreen(viewModel) {
                navController.navigate(Screen.Summary.route)
            }
        }
        composable(Screen.Summary.route) {
            SummaryScreen(viewModel) {
                viewModel.resetSession()
                navController.navigate(Screen.Dashboard.route) {
                    popUpTo(Screen.Dashboard.route) { inclusive = true }
                }
            }
        }
    }
}
