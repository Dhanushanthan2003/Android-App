package com.example.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.EditingProject
import com.example.data.model.TrackerPoint
import com.example.data.model.OverlayItem
import com.example.data.repository.ProjectRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DashboardUiState(
    val projects: List<EditingProject> = emptyList(),
    val totalKeyframesCount: Int = 0,
    val averageConfidence: Float = 0f,
    val totalExportsCount: Int = 0
)

class DashboardViewModel(
    application: Application,
    private val repository: ProjectRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        seedInitialProjectsIfEmpty()
        loadDashboardStats()
    }

    private fun seedInitialProjectsIfEmpty() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.allProjects.first().let { current ->
                if (current.isEmpty()) {
                    // Seed 3 cinematic masterworks
                    val p1 = EditingProject(
                        name = "Project Tokyo Lights",
                        videoType = "tokyo_neon",
                        videoTitle = "Tokyo Neon Motorcycle Chase",
                        aspectRatio = "16:9",
                        durationMs = 10000
                    )
                    val p2 = EditingProject(
                        name = "Apex Halfpipe Trick",
                        videoType = "apex_skater",
                        videoTitle = "Skateboarder Gravity Shift",
                        aspectRatio = "9:16",
                        durationMs = 10000
                    )
                    val p3 = EditingProject(
                        name = "F1 Telemetry Scan",
                        videoType = "f1_speed",
                        videoTitle = "F1 Monzan Speed Run",
                        aspectRatio = "16:9",
                        durationMs = 10000
                    )

                    val id1 = repository.insertProject(p1)
                    val id2 = repository.insertProject(p2)
                    val id3 = repository.insertProject(p3)

                    // Seed some initial tracker coordinates for Project Tokyo Lights
                    val p1Points = (0..50).map { i ->
                        TrackerPoint(
                            projectId = id1,
                            frameIndex = i * 4,
                            xPercent = 0.5f + 0.15f * kotlin.math.sin(i * 0.2f),
                            yPercent = 0.42f + 0.007f * i,
                            widthPercent = 0.10f,
                            heightPercent = 0.08f,
                            confidence = 0.98f
                        )
                    }
                    repository.insertTrackerPoints(p1Points)

                    // Seed an initial overlay text
                    repository.insertOverlayItem(
                        OverlayItem(
                            projectId = id1,
                            type = "text",
                            content = "NEON BIKE 88",
                            colorHex = "#FF00FF",
                            fontSize = 18f,
                            startMs = 0,
                            endMs = 6000
                        )
                    )
                }
            }
        }
    }

    private fun loadDashboardStats() {
        viewModelScope.launch {
            repository.allProjects
                .combine(repository.allExportedVideos) { projs, exports ->
                    Pair(projs, exports)
                }
                .collect { (projs, exports) ->
                    // Calculate quick statistics across all projects
                    var totalKeys = 0
                    var totalConfSum = 0f
                    var pointsCount = 0

                    for (p in projs) {
                        val points = repository.getTrackerPointsList(p.id)
                        totalKeys += points.size
                        points.forEach {
                            totalConfSum += it.confidence
                            pointsCount++
                        }
                    }

                    val avgConf = if (pointsCount > 0) (totalConfSum / pointsCount) * 100f else 0f

                    _uiState.update {
                        it.copy(
                            projects = projs,
                            totalKeyframesCount = totalKeys,
                            averageConfidence = avgConf,
                            totalExportsCount = exports.size
                        )
                    }
                }
        }
    }

    fun createNewProject(name: String, videoType: String, aspectRatio: String, onCreated: (Int) -> Unit) {
        val title = when (videoType) {
            "tokyo_neon" -> "Tokyo Neon Motorcycle Chase"
            "apex_skater" -> "Skateboarder Gravity Shift"
            "f1_speed" -> "F1 Monzan Speed Run"
            else -> "Drone Skyline Orbit"
        }

        val newProj = EditingProject(
            name = if (name.isBlank()) "Untitled Motion Render" else name,
            videoType = videoType,
            videoTitle = title,
            aspectRatio = aspectRatio,
            durationMs = 10000
        )

        viewModelScope.launch(Dispatchers.IO) {
            val newId = repository.insertProject(newProj)
            // Seed 1 default overlay item
            repository.insertOverlayItem(
                OverlayItem(
                    projectId = newId,
                    type = "text",
                    content = "TRACK_OVERLAY",
                    colorHex = "#00FFFF",
                    fontSize = 14f,
                    startMs = 0,
                    endMs = 8000
                )
            )
            viewModelScope.launch(Dispatchers.Main) {
                onCreated(newId)
            }
        }
    }

    fun deleteProject(projectId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteProjectById(projectId)
        }
    }
}

class DashboardViewModelFactory(
    private val application: Application,
    private val repository: ProjectRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
