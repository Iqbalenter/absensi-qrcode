package com.example.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.presentation.auth.LoginScreen
import com.example.presentation.admin.dashboard.AdminDashboardScreen
import com.example.presentation.intern.dashboard.InternDashboardScreen
import com.example.presentation.intern.scanner.ScannerScreen
import com.example.presentation.admin.session.CreateSessionScreen
import com.example.presentation.admin.session.SessionDetailScreen
import com.example.presentation.admin.session.SessionHistoryScreen
import com.example.presentation.admin.interns.ManageInternsScreen
import com.example.presentation.admin.report.LaporanScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                onLoginSuccess = { role ->
                    if (role == "ADMIN" || role == "HRD") {
                        navController.navigate("admin_dashboard") {
                            popUpTo("login") { inclusive = true }
                        }
                    } else {
                        navController.navigate("intern_dashboard") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                }
            )
        }
        
        composable("admin_dashboard") {
            AdminDashboardScreen(
                onNavigateToCreateSession = { navController.navigate("create_session") },
                onNavigateToSessionDetail = { sessionId -> navController.navigate("session_detail/$sessionId") },
                onNavigateToManageInterns = { navController.navigate("manage_interns") },
                onNavigateToSessionHistory = { navController.navigate("session_history") },
                onNavigateToLaporan = { navController.navigate("laporan") },
                onLogout = {
                    navController.navigate("login") {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            )
        }

        composable("manage_interns") {
            ManageInternsScreen(onBack = { navController.popBackStack() })
        }

        composable("laporan") {
            LaporanScreen(onBack = { navController.popBackStack() })
        }

        composable("session_history") {
            SessionHistoryScreen(
                onBack = { navController.popBackStack() },
                onSessionClick = { sessionId -> navController.navigate("session_detail/$sessionId") }
            )
        }

        composable("create_session") {
            CreateSessionScreen(
                onBack = { navController.popBackStack() },
                onSessionCreated = { sessionId ->
                    navController.popBackStack()
                    navController.navigate("session_detail/$sessionId")
                }
            )
        }
        
        composable("session_detail/{sessionId}") { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            SessionDetailScreen(
                sessionId = sessionId,
                onBack = { navController.popBackStack() }
            )
        }
        
        composable("intern_dashboard") {
            InternDashboardScreen(
                onNavigateToScanner = { navController.navigate("scanner") },
                onLogout = {
                    navController.navigate("login") {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            )
        }
        
        composable("scanner") {
            ScannerScreen(
                onBack = { navController.popBackStack() },
                onScanSuccess = {
                    navController.popBackStack()
                }
            )
        }
    }
}
