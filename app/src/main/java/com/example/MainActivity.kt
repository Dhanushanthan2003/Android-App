package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.database.AppDatabase
import com.example.data.repository.ProjectRepository
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.dashboard.DashboardViewModel
import com.example.ui.dashboard.DashboardViewModelFactory
import com.example.ui.editor.VideoEditorScreen
import com.example.ui.editor.VideoEditorViewModel
import com.example.ui.editor.VideoEditorViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Core Offline Persistence Layer (Room)
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = ProjectRepository(database.projectDao())

        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "dashboard",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        // Landing Project Dashboard
                        composable("dashboard") {
                            val dashboardViewModel: DashboardViewModel = viewModel(
                                factory = DashboardViewModelFactory(application, repository)
                            )
                            DashboardScreen(
                                viewModel = dashboardViewModel,
                                onNavigateToEditor = { projectId ->
                                    navController.navigate("editor/$projectId")
                                }
                            )
                        }

                        // Advanced Video Tracking Editor Workstation
                        composable(
                            route = "editor/{projectId}",
                            arguments = listOf(navArgument("projectId") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val projectId = backStackEntry.arguments?.getInt("projectId") ?: 0
                            val editorViewModel: VideoEditorViewModel = viewModel(
                                factory = VideoEditorViewModelFactory(application, repository, projectId)
                            )
                            VideoEditorScreen(
                                viewModel = editorViewModel,
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
