package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.ui.MainViewModel
import com.example.ui.MainViewModelFactory
import com.example.ui.Screen
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup edge to edge rendering
        enableEdgeToEdge()

        // Initialize local persistence
        val database = AppDatabase.getDatabase(applicationContext, lifecycleScope)
        val repository = AppRepository(database)
        
        setContent {
            MyApplicationTheme {
                // Instantiation of the single ViewModel using Provider Factory
                val viewModel: MainViewModel by viewModels { MainViewModelFactory(repository) }
                
                AppContent(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppContent(viewModel: MainViewModel) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()

    if (currentUser == null) {
        // Authentification Portal
        Scaffold(
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            LoginScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
        }
    } else {
        // Authenticated Session App Scaffold
        val userRole = currentUser?.role ?: "Consultation seule"

        // Filter navigation items dynamically matching the User Role Permissions
        val navItems = remember(userRole) {
            listOf(
                Screen.Dashboard to Icons.Default.Dashboard,
                Screen.Stocks to Icons.Default.Inventory,
                Screen.ITAssets to Icons.Default.Computer,
                Screen.Fournisseurs to Icons.Default.Business,
                Screen.AuditLogs to Icons.Default.History
            ).filter { (screen, _) ->
                when (userRole) {
                    "Administrateur" -> true
                    "Gestionnaire de stock" -> screen == Screen.Dashboard || screen == Screen.Stocks || screen == Screen.Fournisseurs
                    "Technicien informatique" -> screen == Screen.Dashboard || screen == Screen.ITAssets
                    "Consultation seule" -> true
                    else -> screen == Screen.Dashboard
                }
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = currentScreen.title,
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    actions = {
                        // Quick Profile role reminder text & Signout button
                        Text(
                            text = "[${currentUser?.nom}]",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        IconButton(onClick = { viewModel.logout() }) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = "Se Déconnecter",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    windowInsets = WindowInsets.navigationBars
                ) {
                    navItems.forEach { (screen, icon) ->
                        val isSelected = currentScreen.id == screen.id
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { viewModel.navigateTo(screen) },
                            icon = { Icon(icon, contentDescription = screen.title) },
                            label = { Text(screen.title, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentScreen) {
                    Screen.Dashboard -> DashboardScreen(viewModel = viewModel)
                    Screen.Stocks -> StocksScreen(viewModel = viewModel)
                    Screen.ITAssets -> ITAssetsScreen(viewModel = viewModel)
                    Screen.Fournisseurs -> FournisseursScreen(viewModel = viewModel)
                    Screen.AuditLogs -> AuditLogsScreen(viewModel = viewModel)
                    else -> DashboardScreen(viewModel = viewModel)
                }
            }
        }
    }
}
