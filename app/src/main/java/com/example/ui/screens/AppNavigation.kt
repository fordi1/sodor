package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.InvoiceViewModel

import android.content.Context
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    var hasSeenOnboarding by remember { mutableStateOf(sharedPrefs.getBoolean("hasSeenOnboarding", false)) }

    val invoiceViewModel: InvoiceViewModel = viewModel()
    
    // Bind to AppSettings database theme mode
    val appSettings by invoiceViewModel.settings.collectAsState()
    val themeMode = appSettings?.themeMode ?: "LIGHT"

    MyApplicationTheme(mode = themeMode) {
        // Force complete RTL Layout Direction across the whole UI
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            if (!hasSeenOnboarding) {
                OnboardingScreen(onFinish = {
                    sharedPrefs.edit().putBoolean("hasSeenOnboarding", true).apply()
                    hasSeenOnboarding = true
                })
            } else {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                Scaffold(
                bottomBar = {
                    // Hide bottom bar on Edit Page and Actions Page to provide maximum input breathing space
                    if (currentRoute != null && !currentRoute.startsWith("invoice_edit") && !currentRoute.startsWith("invoice_actions")) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 0.dp,
                            windowInsets = WindowInsets.navigationBars
                        ) {
                            NavigationBarItem(
                                selected = currentRoute == "dashboard",
                                onClick = { navController.navigate("dashboard") { popUpTo(0) } },
                                icon = { Icon(Icons.Default.Home, "داشبورد") },
                                label = { Text("داشبورد", style = MaterialTheme.typography.labelSmall) }
                            )
                            NavigationBarItem(
                                selected = currentRoute == "invoices",
                                onClick = { navController.navigate("invoices") { popUpTo(0) } },
                                icon = { Icon(Icons.Default.ReceiptLong, "فاکتورها") },
                                label = { Text("فاکتورها", style = MaterialTheme.typography.labelSmall) }
                            )
                            NavigationBarItem(
                                selected = currentRoute == "inventory",
                                onClick = { navController.navigate("inventory") { popUpTo(0) } },
                                icon = { Icon(Icons.Default.Inventory, "کالاها") },
                                label = { Text("کالاها", style = MaterialTheme.typography.labelSmall) }
                            )
                            NavigationBarItem(
                                selected = currentRoute == "customers",
                                onClick = { navController.navigate("customers") { popUpTo(0) } },
                                icon = { Icon(Icons.Default.Group, "مشتریان") },
                                label = { Text("مشتریان", style = MaterialTheme.typography.labelSmall) }
                            )
                            NavigationBarItem(
                                selected = currentRoute == "reports",
                                onClick = { navController.navigate("reports") { popUpTo(0) } },
                                icon = { Icon(Icons.Default.BarChart, "گزارش‌ها") },
                                label = { Text("گزارش‌ها", style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = "dashboard",
                    modifier = Modifier
                        .padding(innerPadding)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    composable("dashboard") {
                        DashboardScreen(navController, invoiceViewModel)
                    }
                    composable("invoices") {
                        InvoiceListScreen(navController, invoiceViewModel)
                    }
                    composable(
                        route = "invoice_edit/{invoiceId}?type={type}",
                        arguments = listOf(
                            navArgument("invoiceId") { type = NavType.StringType },
                            navArgument("type") { type = NavType.StringType; defaultValue = "INVOICE" }
                        )
                    ) { backStackEntry ->
                        val invIdStr = backStackEntry.arguments?.getString("invoiceId") ?: "new"
                        val initType = backStackEntry.arguments?.getString("type") ?: "INVOICE"
                        InvoiceEditorScreen(navController, invoiceViewModel, invIdStr, initType)
                    }
                    composable(
                        route = "invoice_actions/{invoiceId}",
                        arguments = listOf(
                            navArgument("invoiceId") { type = NavType.LongType }
                        )
                    ) { backStackEntry ->
                        val invId = backStackEntry.arguments?.getLong("invoiceId") ?: 0L
                        DocumentActionsScreen(navController, invoiceViewModel, invId)
                    }
                    composable("inventory") {
                        InventoryScreen(invoiceViewModel)
                    }
                    composable("customers") {
                        CustomerScreen(navController, invoiceViewModel)
                    }
                    composable(
                        route = "customer_detail/{customerId}",
                        arguments = listOf(navArgument("customerId") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val customerId = backStackEntry.arguments?.getLong("customerId") ?: 0L
                        CustomerDetailScreen(navController, invoiceViewModel, customerId)
                    }
                    composable("reports") {
                        ReportsScreen(invoiceViewModel)
                    }
                    composable(
                        route = "settings?tab={tab}",
                        arguments = listOf(navArgument("tab") { type = NavType.IntType; defaultValue = 0 })
                    ) { backStackEntry ->
                        val tab = backStackEntry.arguments?.getInt("tab") ?: 0
                        UnifiedSettingsScreen(navController, invoiceViewModel, tab)
                    }
                }
            }
        }
        }
    }
}
