package com.example.ui.editor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.EditingProject
import com.example.data.model.TrackerPoint
import com.example.data.model.OverlayItem
import com.example.data.model.ExportedVideo
import com.example.data.repository.ProjectRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class TrackerBox(
    val xPercent: Float,
    val yPercent: Float,
    val widthPercent: Float,
    val heightPercent: Float
)

data class EditorUiState(
    val project: EditingProject? = null,
    val trackerPoints: List<TrackerPoint> = emptyList(),
    val overlayItems: List<OverlayItem> = emptyList(),
    val isPlaying: Boolean = false,
    val currentTimeMs: Long = 0,
    val currentFrameIndex: Int = 0,
    val isTrackingActive: Boolean = false,
    val trackingProgress: Float = 0f,
    val trackerLogs: List<String> = emptyList(),
    val selectedOverlayId: Int? = null,
    val activeTrackerBox: TrackerBox? = null,
    val isExporting: Boolean = false,
    val exportProgress: Float = 0f,
    val exportedVideos: List<ExportedVideo> = emptyList()
)

class VideoEditorViewModel(
    application: Application,
    private val repository: ProjectRepository,
    private val projectId: Int
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private var playbackJob: Job? = null
    private var trackingJob: Job? = null

    init {
        loadProjectData()
        loadExportedVideos()
        addLog("Dozopar motion tracker engine v2.8 ready.")
    }

    private fun loadProjectData() {
        viewModelScope.launch {
            repository.getProjectById(projectId)
                .combine(repository.getTrackerPoints(projectId)) { proj, points ->
                    Pair(proj, points)
                }
                .combine(repository.getOverlayItems(projectId)) { (proj, points), items ->
                    Triple(proj, points, items)
                }
                .collect { (proj, points, items) ->
                    if (proj != null) {
                        _uiState.update {
                            it.copy(
                                project = proj,
                                trackerPoints = points,
                                overlayItems = items,
                                // Initialize active tracker box if not present
                                activeTrackerBox = it.activeTrackerBox ?: getTrueObjectPosition(
                                    proj.videoType,
                                    it.currentTimeMs
                                )
                            )
                        }
                    }
                }
        }
    }

    private fun loadExportedVideos() {
        viewModelScope.launch {
            repository.allExportedVideos.collect { videos ->
                _uiState.update { it.copy(exportedVideos = videos) }
            }
        }
    }

    fun play() {
        if (_uiState.value.isPlaying) return
        _uiState.update { it.copy(isPlaying = true) }
        addLog("Playback started.")

        playbackJob = viewModelScope.launch(Dispatchers.Main) {
            val startTime = System.currentTimeMillis() - _uiState.value.currentTimeMs
            while (isActive && _uiState.value.isPlaying) {
                val elapsed = System.currentTimeMillis() - startTime
                val nextTime = elapsed % 10000 // loop back after 10s
                val frameIndex = (nextTime / 16.67f).toInt().coerceIn(0, 599)

                // If dynamic auto-tracking is active, record a point!
                if (_uiState.value.isTrackingActive) {
                    recordTrackingPoint(frameIndex, nextTime)
                }

                val currentObjPos = _uiState.value.project?.let {
                    getTrueObjectPosition(it.videoType, nextTime)
                }

                _uiState.update {
                    it.copy(
                        currentTimeMs = nextTime,
                        currentFrameIndex = frameIndex,
                        activeTrackerBox = currentObjPos
                    )
                }
                delay(16) // ~60fps
            }
        }
    }

    fun pause() {
        _uiState.update { it.copy(isPlaying = false, isTrackingActive = false) }
        playbackJob?.cancel()
        playbackJob = null
        addLog("Playback paused.")
    }

    fun seekTo(timeMs: Long) {
        val clampedTime = timeMs.coerceIn(0, 10000)
        val frameIndex = (clampedTime / 16.67f).toInt().coerceIn(0, 599)
        val currentObjPos = _uiState.value.project?.let {
            getTrueObjectPosition(it.videoType, clampedTime)
        }
        _uiState.update {
            it.copy(
                currentTimeMs = clampedTime,
                currentFrameIndex = frameIndex,
                activeTrackerBox = currentObjPos
            )
        }
        addLog("Seeked to ${clampedTime}ms (Frame $frameIndex)")
    }

    /**
     * Toggles offline frame-by-frame advanced tracking simulation.
     */
    fun startAutoTracking() {
        if (_uiState.value.isTrackingActive) {
            _uiState.update { it.copy(isTrackingActive = false) }
            addLog("Auto-tracking paused.")
            return
        }

        pause()
        _uiState.update { it.copy(isTrackingActive = true, trackingProgress = 0f) }
        val proj = _uiState.value.project ?: return
        addLog("Initializing track solver [Engine: ${proj.trackerEngineType}]...")

        trackingJob = viewModelScope.launch(Dispatchers.IO) {
            val startFrame = _uiState.value.currentFrameIndex
            val listToSave = mutableListOf<TrackerPoint>()
            addLog("Executing forward pass on ${proj.videoTitle} from frame $startFrame")

            for (f in startFrame..599) {
                if (!_uiState.value.isTrackingActive) break
                val tMs = (f * 16.67f).toLong()

                // Calculate standard true coordinates
                val truePos = getTrueObjectPosition(proj.videoType, tMs)

                // Simulate search drift & confidence calculations based on parameters
                val driftFactor = when (proj.trackerEngineType) {
                    "CSRT" -> 0.01f
                    "MIL" -> 0.02f
                    "KCF" -> 0.03f
                    else -> 0.015f // OpticalFlow
                } * (64f / proj.trackerWindowSize.toFloat())

                // Random noise representing tracking confidence
                val randomOffset = Random.nextFloat() * driftFactor - (driftFactor / 2)
                val noiseY = Random.nextFloat() * driftFactor - (driftFactor / 2)
                val baseConfidence = proj.precisionThreshold + 0.1f + Random.nextFloat() * 0.12f
                val finalConfidence = (baseConfidence - (f - startFrame) * 0.0003f).coerceIn(0.1f, 0.99f)

                val trackedX = (truePos.xPercent + randomOffset).coerceIn(0f, 1f)
                val trackedY = (truePos.yPercent + noiseY).coerceIn(0f, 1f)

                val point = TrackerPoint(
                    projectId = projectId,
                    frameIndex = f,
                    xPercent = trackedX,
                    yPercent = trackedY,
                    widthPercent = truePos.widthPercent,
                    heightPercent = truePos.heightPercent,
                    confidence = finalConfidence
                )
                listToSave.add(point)

                // Keep UI updated
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            currentFrameIndex = f,
                            currentTimeMs = tMs,
                            activeTrackerBox = TrackerBox(trackedX, trackedY, truePos.widthPercent, truePos.heightPercent),
                            trackingProgress = (f - startFrame).toFloat() / (600 - startFrame).toFloat()
                        )
                    }
                    if (f % 30 == 0) {
                        addLog("[Engine: ${proj.trackerEngineType}] Frame $f: locked at (${"%.2f".format(trackedX)}, ${"%.2f".format(trackedY)}) - Conf: ${"%.2f".format(finalConfidence)}")
                    }
                }
                delay(12) // simulate real frame computation
            }

            // Save results to Database
            repository.insertTrackerPoints(listToSave)
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(isTrackingActive = false, trackingProgress = 1f) }
                addLog("Tracking completed. Successfully rendered ${listToSave.size} motion keys!")
            }
        }
    }

    private fun recordTrackingPoint(frameIndex: Int, timeMs: Long) {
        val proj = _uiState.value.project ?: return
        val truePos = getTrueObjectPosition(proj.videoType, timeMs)
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertTrackerPoint(
                TrackerPoint(
                    projectId = projectId,
                    frameIndex = frameIndex,
                    xPercent = truePos.xPercent,
                    yPercent = truePos.yPercent,
                    widthPercent = truePos.widthPercent,
                    heightPercent = truePos.heightPercent,
                    confidence = 0.98f
                )
            )
        }
    }

    fun clearTrackingPoints() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTrackerPointsForProject(projectId)
            withContext(Dispatchers.Main) {
                addLog("Cleared all motion keyframes.")
            }
        }
    }

    fun addOverlayItem(type: String, content: String, colorHex: String, startMs: Long, endMs: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertOverlayItem(
                OverlayItem(
                    projectId = projectId,
                    type = type,
                    content = content,
                    colorHex = colorHex,
                    startMs = startMs,
                    endMs = endMs
                )
            )
            withContext(Dispatchers.Main) {
                addLog("Overlay element '$content' added on timeline ($startMs - $endMs).")
            }
        }
    }

    fun deleteOverlayItem(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteOverlayItemById(id)
            withContext(Dispatchers.Main) {
                addLog("Removed overlay element.")
            }
        }
    }

    fun updateProjectSettings(
        engine: String,
        windowSize: Int,
        searchRange: Int,
        threshold: Float,
        colorSpace: String
    ) {
        val currentProj = _uiState.value.project ?: return
        val updated = currentProj.copy(
            trackerEngineType = engine,
            trackerWindowSize = windowSize,
            searchRange = searchRange,
            precisionThreshold = threshold,
            colorSpace = colorSpace,
            lastModified = System.currentTimeMillis()
        )
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertProject(updated)
            withContext(Dispatchers.Main) {
                addLog("Updated motion tracker configs -> $engine, WS: ${windowSize}px, Range: ${searchRange}px, Space: $colorSpace")
            }
        }
    }

    fun adjustTrackerPointManually(frameIndex: Int, x: Float, y: Float, w: Float, h: Float) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertTrackerPoint(
                TrackerPoint(
                    projectId = projectId,
                    frameIndex = frameIndex,
                    xPercent = x.coerceIn(0f, 1f),
                    yPercent = y.coerceIn(0f, 1f),
                    widthPercent = w.coerceIn(0.01f, 0.5f),
                    heightPercent = h.coerceIn(0.01f, 0.5f),
                    confidence = 1.0f // Manual points are 100% trusted
                )
            )
            withContext(Dispatchers.Main) {
                addLog("Keyframe manually updated at Frame $frameIndex: (${"%.2f".format(x)}, ${"%.2f".format(y)})")
            }
        }
    }

    fun simulateMediaExport(resolution: String, format: String, fps: Int, onComplete: () -> Unit) {
        if (_uiState.value.isExporting) return
        _uiState.update { it.copy(isExporting = true, exportProgress = 0f) }
        addLog("Export initialized: $resolution @${fps}fps ($format)")

        viewModelScope.launch(Dispatchers.IO) {
            for (p in 1..100) {
                delay(30)
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(exportProgress = p / 100f) }
                }
            }

            val sizeMb = when (resolution) {
                "4K" -> 245.4f
                "1080p" -> 72.8f
                else -> 31.2f
            }

            val newExport = ExportedVideo(
                projectId = projectId,
                projectName = _uiState.value.project?.name ?: "Dozopar Project",
                videoType = _uiState.value.project?.videoType ?: "tokyo_neon",
                resolution = resolution,
                format = format,
                fps = fps,
                fileSizeMb = sizeMb
            )

            repository.insertExportedVideo(newExport)

            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(isExporting = false, exportProgress = 1f) }
                addLog("Render completed! File saved successfully (${"%.1f".format(sizeMb)} MB)")
                onComplete()
            }
        }
    }

    fun deleteExportedVideo(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteExportedVideoById(id)
            withContext(Dispatchers.Main) {
                addLog("Deleted rendered export.")
            }
        }
    }

    private fun addLog(message: String) {
        val timeStamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        _uiState.update {
            it.copy(
                trackerLogs = (listOf("[$timeStamp] $message") + it.trackerLogs).take(35)
            )
        }
    }

    /**
     * Deterministic true position simulator for objects in cinematic tracks.
     */
    fun getTrueObjectPosition(videoType: String, timeMs: Long): TrackerBox {
        val t = timeMs.coerceIn(0, 10000).toFloat()
        return when (videoType) {
            "tokyo_neon" -> {
                // Neon Bike riding down perspectival freeway
                // Recedes back (small) and speeds close (large)
                val norm = t / 10000f
                val z = cos(norm * Math.PI.toFloat() / 2f) // 1 at start, 0 at end
                val scale = 1f - z // 0 far away, 1 close
                val x = 0.5f + 0.28f * sin(t / 950f) * scale
                val y = 0.42f + 0.43f * scale
                val w = 0.05f + 0.18f * scale
                val h = 0.04f + 0.15f * scale
                TrackerBox(x, y, w, h)
            }
            "apex_skater" -> {
                // Skateboarding parabolic arc back and forth
                val norm = t / 10000f
                val cycle = sin(t / 1400f) // oscillating -1 to 1
                val x = 0.5f + 0.38f * cycle
                val y = 0.82f - 0.52f * (cycle * cycle) // high point at peak
                val w = 0.09f + 0.03f * sin(t / 250f)
                val h = 0.11f + 0.03f * cos(t / 250f)
                TrackerBox(x, y, w, h)
            }
            "f1_speed" -> {
                // High-speed racing car straightaway acceleration and drift curve
                val phase = t / 10000f
                if (phase < 0.35f) {
                    val p = phase / 0.35f
                    val x = 0.05f + 0.85f * p
                    val y = 0.72f
                    TrackerBox(x, y, 0.12f, 0.08f)
                } else if (phase < 0.75f) {
                    val p = (phase - 0.35f) / 0.40f
                    val x = 0.9f - 0.8f * p
                    val y = 0.72f + 0.14f * sin(p * 2f * Math.PI.toFloat())
                    val scale = 1f + 0.4f * sin(p * Math.PI.toFloat())
                    TrackerBox(x, y, 0.12f * scale, 0.08f * scale)
                } else {
                    val p = (phase - 0.75f) / 0.25f
                    val scale = 1f - p * 0.7f
                    val x = 0.1f + 0.4f * p
                    val y = 0.72f - 0.35f * p
                    TrackerBox(x, y, 0.12f * scale, 0.08f * scale)
                }
            }
            else -> {
                // Drone Skyline Orbiting central skyscraper tip
                val theta = t / 1200f
                val x = 0.5f + 0.26f * cos(theta)
                val y = 0.46f + 0.16f * sin(2f * theta)
                TrackerBox(x, y, 0.07f, 0.07f)
            }
        }
    }
}

class VideoEditorViewModelFactory(
    private val application: Application,
    private val repository: ProjectRepository,
    private val projectId: Int
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VideoEditorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VideoEditorViewModel(application, repository, projectId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
