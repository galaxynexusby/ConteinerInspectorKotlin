package ru.vibe.containerinspector.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import ru.vibe.containerinspector.ui.screens.*
import ru.vibe.containerinspector.viewmodel.MainViewModel

sealed class Screen(val route: String) {
    object Auth : Screen("auth")
    object Dashboard : Screen("dashboard")
    object OCRScan : Screen("ocr_scan")
    object Inspection : Screen("inspection")
    object Summary : Screen("summary")
}

@Composable
fun NavGraph(navController: NavHostController, viewModel: MainViewModel) {
    NavHost(
        navController = navController,
        startDestination = Screen.Auth.route
    ) {
        composable(Screen.Auth.route) {
            AuthScreen(viewModel) {
                navController.navigate(Screen.Dashboard.route) {
                    popUpTo(Screen.Auth.route) { inclusive = true }
                }
            }
        }
        composable(Screen.Dashboard.route) {
            DashboardScreen(viewModel) {
                navController.navigate(Screen.OCRScan.route)
            }
        }
        composable(Screen.OCRScan.route) {
            OCRScanScreen(viewModel) {
                navController.navigate(Screen.Inspection.route)
            }
        }
        composable(Screen.Inspection.route) {
            InspectionScreen(viewModel) {
                navController.navigate(Screen.Summary.route)
            }
        }
        composable(Screen.Summary.route) {
            SummaryScreen(viewModel) {
                viewModel.resetSession() // Reset for next inspection
                navController.navigate(Screen.Dashboard.route) {
                    popUpTo(Screen.Dashboard.route) { inclusive = true }
                }
            }
        }
    }
}
