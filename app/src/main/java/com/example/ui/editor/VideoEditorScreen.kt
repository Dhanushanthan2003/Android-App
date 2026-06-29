package com.example.ui.editor

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.OverlayItem
import com.example.data.model.TrackerPoint
import com.example.ui.components.GlassBox
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoEditorScreen(
    viewModel: VideoEditorViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val project = uiState.project

    // Local configuration panel states
    var solverEngine by remember { mutableStateOf("CSRT") }
    var windowSize by remember { mutableStateOf(48f) }
    var precisionThreshold by remember { mutableStateOf(0.75f) }
    var selectedColorSpace by remember { mutableStateOf("HSV") }

    // Dialog & overlay drawer states
    var showExportDialog by remember { mutableStateOf(false) }
    var showAddOverlayDrawer by remember { mutableStateOf(false) }

    // Add Overlay Fields
    var overlayType by remember { mutableStateOf("text") }
    var overlayContent by remember { mutableStateOf("TARGET_LOCKED") }
    var overlayColorHex by remember { mutableStateOf("#00FFFF") }
    var overlayStartMs by remember { mutableStateOf("1000") }
    var overlayEndMs by remember { mutableStateOf("8000") }

    // Sync settings from project when loaded
    LaunchedEffect(project) {
        project?.let {
            solverEngine = it.trackerEngineType
            windowSize = it.trackerWindowSize.toFloat()
            precisionThreshold = it.precisionThreshold
            selectedColorSpace = it.colorSpace
        }
    }

    if (project == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF070709)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFF00FFCC))
        }
        return
    }

    val accentColor = when (project.videoType) {
        "tokyo_neon" -> Color(0xFFFF007F)
        "apex_skater" -> Color(0xFFFF8C00)
        "f1_speed" -> Color(0xFF00FFCC)
        else -> Color(0xFF007FFF)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070709))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .statusBarsPadding()
        ) {
            // Top Workspace Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("editor_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to dashboard",
                            tint = Color.White
                        )
                    }

                    Column {
                        Text(
                            text = project.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = project.videoTitle,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = accentColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                // Render Export Button
                GlassButton(
                    onClick = { showExportDialog = true },
                    modifier = Modifier
                        .height(38.dp)
                        .testTag("trigger_export_btn"),
                    borderColor = accentColor.copy(alpha = 0.5f),
                    backgroundColor = accentColor.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = "Export video",
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "EXPORT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        )
                    }
                }
            }

            // Screen Content Split (Canvas + Control Panel)
            // Using a scrolling column for simple vertical adaptation
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Video monitor frame
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(
                            when (project.aspectRatio) {
                                "9:16" -> 0.56f
                                "1:1" -> 1f
                                else -> 1.77f // 16:9
                            }
                        )
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black)
                        .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(16.dp))
                ) {
                    // Video Canvas rendering the actual animated frame
                    VideoCanvasPlayer(
                        videoType = project.videoType,
                        timeMs = uiState.currentTimeMs,
                        uiState = uiState,
                        accentColor = accentColor,
                        onCanvasDrag = { offsetPercent ->
                            val currentBox = uiState.activeTrackerBox ?: TrackerBox(0.5f, 0.5f, 0.1f, 0.1f)
                            viewModel.adjustTrackerPointManually(
                                frameIndex = uiState.currentFrameIndex,
                                x = offsetPercent.x,
                                y = offsetPercent.y,
                                w = currentBox.widthPercent,
                                h = currentBox.heightPercent
                            )
                        }
                    )

                    // Target acquisition crosshair guide overlay
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                    ) {
                        GlassBox(
                            shape = RoundedCornerShape(8.dp),
                            backgroundColor = Color(0x66000000),
                            borderColor = Color(0x22FFFFFF)
                        ) {
                            Text(
                                text = "MONITOR ACTIVE • ${uiState.currentFrameIndex}/599 FPS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF00FFCC),
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Interactive overlay drawer toggle button
                    IconButton(
                        onClick = { showAddOverlayDrawer = true },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .testTag("add_overlay_icon_btn")
                    ) {
                        GlassBox(
                            shape = RoundedCornerShape(50),
                            backgroundColor = Color(0x7C000000),
                            borderColor = Color(0x5500FFCC)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TextFields,
                                contentDescription = "Add Text overlay",
                                tint = Color(0xFF00FFCC),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Interactive Timeline Tracks
                TimelineTracksWidget(
                    uiState = uiState,
                    accentColor = accentColor,
                    onSeek = { viewModel.seekTo(it) },
                    onPlayToggle = {
                        if (uiState.isPlaying) viewModel.pause() else viewModel.play()
                    }
                )

                // Motion Tracker parameter configs
                MotionTrackerSettingsPanel(
                    solverEngine = solverEngine,
                    onEngineChange = {
                        solverEngine = it
                        viewModel.updateProjectSettings(it, windowSize.toInt(), (windowSize * 1.5).toInt(), precisionThreshold, selectedColorSpace)
                    },
                    windowSize = windowSize,
                    onWindowSizeChange = {
                        windowSize = it
                        viewModel.updateProjectSettings(solverEngine, it.toInt(), (it * 1.5).toInt(), precisionThreshold, selectedColorSpace)
                    },
                    precisionThreshold = precisionThreshold,
                    onPrecisionChange = {
                        precisionThreshold = it
                        viewModel.updateProjectSettings(solverEngine, windowSize.toInt(), (windowSize * 1.5).toInt(), it, selectedColorSpace)
                    },
                    colorSpace = selectedColorSpace,
                    onColorSpaceChange = {
                        selectedColorSpace = it
                        viewModel.updateProjectSettings(solverEngine, windowSize.toInt(), (windowSize * 1.5).toInt(), precisionThreshold, it)
                    },
                    isTrackingActive = uiState.isTrackingActive,
                    onStartTracking = { viewModel.startAutoTracking() },
                    onClearKeys = { viewModel.clearTrackingPoints() },
                    accentColor = accentColor,
                    trackingProgress = uiState.trackingProgress
                )

                // Advanced tracking console logs
                LiveLogsWidget(logs = uiState.trackerLogs)
            }
        }

        // Overlay element creator drawer
        if (showAddOverlayDrawer) {
            AlertDialog(
                onDismissRequest = { showAddOverlayDrawer = false },
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp))
                    .background(Color(0xFF111116)),
                confirmButton = {},
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "ADD MOTION PINNED OVERLAY",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                        )

                        // Type select
                        Row(modifier = Modifier.fillMaxWidth()) {
                            listOf("text", "sticker").forEach { type ->
                                val active = overlayType == type
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (active) accentColor.copy(alpha = 0.2f) else Color(0x11FFFFFF))
                                        .border(
                                            1.dp,
                                            if (active) accentColor else Color(0x1FFFFFFF),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            overlayType = type
                                            overlayContent = if (type == "sticker") "🔥" else "LOCK"
                                        }
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = type.uppercase(),
                                        color = if (active) accentColor else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        // Content input
                        if (overlayType == "text") {
                            OutlinedTextField(
                                value = overlayContent,
                                onValueChange = { overlayContent = it },
                                label = { Text("Display Text") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("overlay_text_input"),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color(0x11FFFFFF),
                                    unfocusedContainerColor = Color(0x0AFFFFFF),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.LightGray,
                                    focusedIndicatorColor = accentColor
                                )
                            )
                        } else {
                            // Sticker grid selection
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                listOf("🔥", "🚀", "💥", "🎯", "👑", "🎬").forEach { sticker ->
                                    val isSel = overlayContent == sticker
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSel) Color(0x2200FFCC) else Color(0x11FFFFFF))
                                            .border(
                                                1.dp,
                                                if (isSel) Color(0xFF00FFCC) else Color.Transparent,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable { overlayContent = sticker },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(sticker, fontSize = 20.sp)
                                    }
                                }
                            }
                        }

                        // Styling Color selection
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Glow Tint", color = Color.Gray, fontSize = 12.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("#00FFFF", "#FF007F", "#00FFCC", "#FFFF00", "#FFFFFF").forEach { hex ->
                                    val act = overlayColorHex == hex
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(Color(android.graphics.Color.parseColor(hex)))
                                            .border(
                                                width = if (act) 2.dp else 1.dp,
                                                color = if (act) Color.White else Color(0x44FFFFFF),
                                                shape = RoundedCornerShape(50)
                                            )
                                            .clickable { overlayColorHex = hex }
                                    )
                                }
                            }
                        }

                        // Time parameters (Start and End)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = overlayStartMs,
                                onValueChange = { overlayStartMs = it },
                                label = { Text("In ms") },
                                modifier = Modifier.weight(1f),
                                colors = TextFieldDefaults.colors(focusedIndicatorColor = accentColor)
                            )
                            OutlinedTextField(
                                value = overlayEndMs,
                                onValueChange = { overlayEndMs = it },
                                label = { Text("Out ms") },
                                modifier = Modifier.weight(1f),
                                colors = TextFieldDefaults.colors(focusedIndicatorColor = accentColor)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showAddOverlayDrawer = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("CLOSE", color = Color.White)
                            }
                            Button(
                                onClick = {
                                    showAddOverlayDrawer = false
                                    viewModel.addOverlayItem(
                                        type = overlayType,
                                        content = overlayContent,
                                        colorHex = overlayColorHex,
                                        startMs = overlayStartMs.toLongOrNull() ?: 0,
                                        endMs = overlayEndMs.toLongOrNull() ?: 10000
                                    )
                                },
                                modifier = Modifier
                                    .weight(1.5f)
                                    .testTag("submit_overlay_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                            ) {
                                Text("PIN TO TRACKER", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            )
        }

        // Beautiful glassmorphic media export dialog
        if (showExportDialog) {
            var selectedRes by remember { mutableStateOf("1080p") }
            var selectedFormat by remember { mutableStateOf("MP4") }
            var selectedFps by remember { mutableStateOf(60) }

            AlertDialog(
                onDismissRequest = { if (!uiState.isExporting) showExportDialog = false },
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp))
                    .background(Color(0xFF0A0A0F)),
                confirmButton = {},
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "INITIALIZE MEDIA RENDER",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 2.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        )

                        if (uiState.isExporting) {
                            // High tech export progress indicator
                            Spacer(modifier = Modifier.height(20.dp))
                            Box(
                                modifier = Modifier.size(100.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    progress = { uiState.exportProgress },
                                    modifier = Modifier.size(90.dp),
                                    color = accentColor,
                                    strokeWidth = 6.dp,
                                    trackColor = Color(0x11FFFFFF)
                                )
                                Text(
                                    text = "${(uiState.exportProgress * 100).toInt()}%",
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Encoding frames & overlay vectors...",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            // Configuration options
                            Text("RESOLUTIONS", color = Color.Gray, fontSize = 10.sp, modifier = Modifier.align(Alignment.Start))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("1080p", "4K", "720p").forEach { res ->
                                    val active = selectedRes == res
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (active) accentColor.copy(alpha = 0.2f) else Color(0x11FFFFFF))
                                            .border(1.dp, if (active) accentColor else Color.Transparent, RoundedCornerShape(8.dp))
                                            .clickable { selectedRes = res }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(res, color = if (active) accentColor else Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Text("CONTAINER", color = Color.Gray, fontSize = 10.sp, modifier = Modifier.align(Alignment.Start))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("MP4", "MKV", "ProRes").forEach { format ->
                                    val active = selectedFormat == format
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (active) accentColor.copy(alpha = 0.2f) else Color(0x11FFFFFF))
                                            .border(1.dp, if (active) accentColor else Color.Transparent, RoundedCornerShape(8.dp))
                                            .clickable { selectedFormat = format }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(format, color = if (active) accentColor else Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Text("FRAME RATE", color = Color.Gray, fontSize = 10.sp, modifier = Modifier.align(Alignment.Start))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(30, 60).forEach { fps ->
                                    val active = selectedFps == fps
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (active) accentColor.copy(alpha = 0.2f) else Color(0x11FFFFFF))
                                            .border(1.dp, if (active) accentColor else Color.Transparent, RoundedCornerShape(8.dp))
                                            .clickable { selectedFps = fps }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("${fps} FPS", color = if (active) accentColor else Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { showExportDialog = false },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("CLOSE", color = Color.White)
                                }
                                Button(
                                    onClick = {
                                        viewModel.simulateMediaExport(selectedRes, selectedFormat, selectedFps) {
                                            showExportDialog = false
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1.5f)
                                        .testTag("confirm_export_btn"),
                                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                                ) {
                                    Text("START RENDER", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            )
        }
    }
}

/**
 * Custom vector canvas rendering the synthetic cinematic tracks.
 */
@Composable
fun VideoCanvasPlayer(
    videoType: String,
    timeMs: Long,
    uiState: EditorUiState,
    accentColor: Color,
    onCanvasDrag: (Offset) -> Unit
) {
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val pos = change.position
                    if (canvasSize.width > 0 && canvasSize.height > 0) {
                        val xPct = (pos.x / canvasSize.width).coerceIn(0f, 1f)
                        val yPct = (pos.y / canvasSize.height).coerceIn(0f, 1f)
                        onCanvasDrag(Offset(xPct, yPct))
                    }
                }
            }
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            canvasSize = size
            val w = size.width
            val h = size.height

            // 1. Draw static background grid or environmental shapes
            drawRect(Color(0xFF050508))

            when (videoType) {
                "tokyo_neon" -> {
                    // Cyber Perspective Highway Grid
                    val horizonY = h * 0.4f
                    val roadWidthFar = w * 0.08f
                    val roadWidthNear = w * 0.9f

                    // Draw perspective road lines
                    val p = Path().apply {
                        moveTo(w / 2 - roadWidthFar / 2, horizonY)
                        lineTo(w / 2 + roadWidthFar / 2, horizonY)
                        lineTo(w / 2 + roadWidthNear / 2, h)
                        lineTo(w / 2 - roadWidthNear / 2, h)
                        close()
                    }
                    drawPath(p, Color(0xFF14141A))

                    // Draw futuristic neon highway grids
                    val lines = 7
                    val dx = (roadWidthNear - roadWidthFar) / (lines - 1)
                    val dxHorizon = roadWidthFar / (lines - 1)
                    for (i in 0 until lines) {
                        val startX = w / 2 - roadWidthFar / 2 + i * dxHorizon
                        val endX = w / 2 - roadWidthNear / 2 + i * dx
                        drawLine(
                            color = Color(0x22FF007F),
                            start = Offset(startX, horizonY),
                            end = Offset(endX, h),
                            strokeWidth = 2f
                        )
                    }

                    // Receding speed lines on highway
                    val speedOffset = (timeMs % 1200) / 1200f
                    val linesY = 5
                    for (i in 0 until linesY) {
                        val ratio = (i + speedOffset) / linesY
                        val curY = horizonY + (h - horizonY) * (ratio * ratio)
                        val scaleWidth = roadWidthFar + (roadWidthNear - roadWidthFar) * ratio
                        drawLine(
                            color = Color(0x3C00FFFF),
                            start = Offset(w / 2 - scaleWidth / 2, curY),
                            end = Offset(w / 2 + scaleWidth / 2, curY),
                            strokeWidth = 3f * ratio
                        )
                    }

                    // Distant skyline and stars
                    drawCircle(Color(0xFF2E1A47), radius = w * 0.25f, center = Offset(w / 2, horizonY))
                    drawCircle(Color(0xFF0F071D), radius = w * 0.20f, center = Offset(w / 2, horizonY))

                    // Cyberbike glowing neon headlights
                    val bikePos = uiState.activeTrackerBox ?: TrackerBox(0.5f, 0.6f, 0.1f, 0.1f)
                    val bx = bikePos.xPercent * w
                    val by = bikePos.yPercent * h
                    val bw = bikePos.widthPercent * w
                    val bh = bikePos.heightPercent * h

                    // Glowing rear light trails
                    drawCircle(
                        brush = Brush.radialGradient(listOf(Color(0xFFFF007F), Color.Transparent)),
                        center = Offset(bx, by),
                        radius = bw * 1.5f
                    )
                    // Bike body
                    drawRoundRect(
                        color = Color(0xFF00FFCC),
                        topLeft = Offset(bx - bw / 2, by - bh / 2),
                        size = Size(bw, bh),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                    // Handlebar light
                    drawCircle(
                        color = Color(0xFFFFFFFF),
                        radius = bw * 0.15f,
                        center = Offset(bx, by - bh * 0.2f)
                    )
                }

                "apex_skater" -> {
                    // Skate half-pipe curve drawing
                    val pipeCenterY = h * 0.85f
                    val pipePath = Path().apply {
                        moveTo(0f, h * 0.35f)
                        quadraticTo(w * 0.15f, pipeCenterY, w * 0.5f, pipeCenterY)
                        quadraticTo(w * 0.85f, pipeCenterY, w, h * 0.35f)
                    }
                    drawPath(pipePath, Color(0x22FFA500), style = Stroke(width = 8f))

                    // Spotlights
                    drawLine(Color(0x15FFFFFF), Offset(w * 0.15f, 0f), Offset(w * 0.5f, pipeCenterY), strokeWidth = w * 0.08f)
                    drawLine(Color(0x15FFFFFF), Offset(w * 0.85f, 0f), Offset(w * 0.5f, pipeCenterY), strokeWidth = w * 0.08f)

                    val skaterPos = uiState.activeTrackerBox ?: TrackerBox(0.5f, 0.7f, 0.1f, 0.1f)
                    val sx = skaterPos.xPercent * w
                    val sy = skaterPos.yPercent * h
                    val sw = skaterPos.widthPercent * w
                    val sh = skaterPos.heightPercent * h

                    // Skateboard board line rotating
                    val rotationDeg = (timeMs / 10f) % 360f
                    rotate(degrees = rotationDeg, pivot = Offset(sx, sy)) {
                        // Board
                        drawRoundRect(
                            color = Color(0xFFD7CCC8),
                            topLeft = Offset(sx - sw * 0.6f, sy + sh * 0.2f),
                            size = Size(sw * 1.2f, sh * 0.2f),
                            cornerRadius = CornerRadius(4f, 4f)
                        )
                        // Wheels
                        drawCircle(Color(0xFFFFFFFF), radius = sw * 0.12f, center = Offset(sx - sw * 0.4f, sy + sh * 0.4f))
                        drawCircle(Color(0xFFFFFFFF), radius = sw * 0.12f, center = Offset(sx + sw * 0.4f, sy + sh * 0.4f))
                    }

                    // Skateboarder glowing torso
                    drawCircle(
                        color = Color(0xFFFF8C00),
                        radius = sw * 0.3f,
                        center = Offset(sx, sy - sh * 0.2f)
                    )
                    // Helmet
                    drawCircle(
                        color = Color.White,
                        radius = sw * 0.2f,
                        center = Offset(sx, sy - sh * 0.5f)
                    )
                }

                "f1_speed" -> {
                    // Curved racing track lines receding
                    val horizonY = h * 0.35f
                    val curbPath = Path().apply {
                        moveTo(w * 0.1f, h)
                        quadraticTo(w * 0.3f, horizonY, w * 0.45f, horizonY)
                        lineTo(w * 0.55f, horizonY)
                        quadraticTo(w * 0.7f, horizonY, w * 0.9f, h)
                    }
                    drawPath(curbPath, Color(0xFF37474F))

                    // Curbs (Red and White checkers)
                    val curbsCount = 12
                    for (i in 0 until curbsCount) {
                        val progress = (i + (timeMs % 1000) / 1000f) / curbsCount
                        val curY = horizonY + (h - horizonY) * progress
                        val trackW = w * 0.4f * progress + w * 0.1f
                        val checkerColor = if (i % 2 == 0) Color.Red else Color.White
                        drawRect(
                            color = checkerColor,
                            topLeft = Offset(w / 2 - trackW / 2 - 15f, curY),
                            size = Size(30f, 10f * progress + 2f)
                        )
                        drawRect(
                            color = checkerColor,
                            topLeft = Offset(w / 2 + trackW / 2 - 15f, curY),
                            size = Size(30f, 10f * progress + 2f)
                        )
                    }

                    // Display F1 HUD Telemetry Overlay on Canvas
                    val f1Pos = uiState.activeTrackerBox ?: TrackerBox(0.5f, 0.7f, 0.12f, 0.08f)
                    val fx = f1Pos.xPercent * w
                    val fy = f1Pos.yPercent * h
                    val fw = f1Pos.widthPercent * w
                    val fh = f1Pos.heightPercent * h

                    // Draw Sleek F1 racing profile
                    val f1Path = Path().apply {
                        moveTo(fx - fw / 2, fy + fh / 2)
                        lineTo(fx - fw * 0.4f, fy - fh / 2) // Wing
                        lineTo(fx + fw * 0.3f, fy - fh / 2) // Spoiler
                        lineTo(fx + fw / 2, fy + fh / 2) // Front nose
                        close()
                    }
                    drawPath(f1Path, Color(0xFFFF0000))
                    // Wheels
                    drawCircle(Color(0xFF212121), radius = fh * 0.4f, center = Offset(fx - fw * 0.3f, fy + fh * 0.3f))
                    drawCircle(Color(0xFF212121), radius = fh * 0.4f, center = Offset(fx + fw * 0.3f, fy + fh * 0.3f))

                    // Telemetry glow line
                    drawCircle(
                        brush = Brush.radialGradient(listOf(Color(0x4400FFCC), Color.Transparent)),
                        center = Offset(fx, fy),
                        radius = fw * 1.4f
                    )
                }

                else -> {
                    // Drone Skyline Orbiting glowing spire tower
                    val spireX = w * 0.65f
                    val spireY = h * 0.45f

                    // Draw Glowing Central Tower Spire
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0x880D47A1), Color(0x11070709)),
                            start = Offset(spireX, spireY),
                            end = Offset(spireX, h)
                        ),
                        topLeft = Offset(spireX - 24f, spireY),
                        size = Size(48f, h - spireY)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(listOf(Color(0xFF00E5FF), Color.Transparent)),
                        center = Offset(spireX, spireY),
                        radius = 80f
                    )
                    // High power scan beacon light
                    drawLine(
                        color = Color(0x6600E5FF),
                        start = Offset(spireX, spireY),
                        end = Offset(spireX + cos(timeMs / 400f) * w * 0.4f, spireY + sin(timeMs / 400f) * h * 0.4f),
                        strokeWidth = 3f
                    )

                    // Active drone
                    val dronePos = uiState.activeTrackerBox ?: TrackerBox(0.4f, 0.4f, 0.08f, 0.08f)
                    val dx = dronePos.xPercent * w
                    val dy = dronePos.yPercent * h
                    val dw = dronePos.widthPercent * w
                    val dh = dronePos.heightPercent * h

                    // Draw drone quadcopter chassis
                    drawLine(Color(0xFF90A4AE), Offset(dx - dw / 2, dy - dh / 2), Offset(dx + dw / 2, dy + dh / 2), strokeWidth = 4f)
                    drawLine(Color(0xFF90A4AE), Offset(dx + dw / 2, dy - dh / 2), Offset(dx - dw / 2, dy + dh / 2), strokeWidth = 4f)

                    // 4 rotors glowing blue halos
                    drawCircle(Color(0x6600E5FF), radius = dw * 0.3f, center = Offset(dx - dw / 2, dy - dh / 2), style = Stroke(width = 2f))
                    drawCircle(Color(0x6600E5FF), radius = dw * 0.3f, center = Offset(dx + dw / 2, dy - dh / 2), style = Stroke(width = 2f))
                    drawCircle(Color(0x6600E5FF), radius = dw * 0.3f, center = Offset(dx - dw / 2, dy + dh / 2), style = Stroke(width = 2f))
                    drawCircle(Color(0x6600E5FF), radius = dw * 0.3f, center = Offset(dx + dw / 2, dy + dh / 2), style = Stroke(width = 2f))

                    // Flashing red camera beacon
                    val pulse = (timeMs % 400 < 200)
                    drawCircle(
                        color = if (pulse) Color.Red else Color(0xFFD50000),
                        radius = dw * 0.15f,
                        center = Offset(dx, dy)
                    )
                }
            }

            // 2. Render Target Bounding Box and crosshairs
            val trackerBox = uiState.activeTrackerBox
            if (trackerBox != null) {
                val boxX = trackerBox.xPercent * w
                val boxY = trackerBox.yPercent * h
                val boxW = trackerBox.widthPercent * w
                val boxH = trackerBox.heightPercent * h

                // Outer bounding box corners
                drawRoundRect(
                    color = accentColor,
                    topLeft = Offset(boxX - boxW / 2, boxY - boxH / 2),
                    size = Size(boxW, boxH),
                    cornerRadius = CornerRadius(4f, 4f),
                    style = Stroke(width = 3f)
                )

                // Render micro targeting ticks
                val tickLen = 12f
                // Top-Left corner
                drawLine(accentColor, Offset(boxX - boxW / 2 - 4f, boxY - boxH / 2), Offset(boxX - boxW / 2 + tickLen, boxY - boxH / 2), strokeWidth = 5f)
                drawLine(accentColor, Offset(boxX - boxW / 2, boxY - boxH / 2 - 4f), Offset(boxX - boxW / 2, boxY - boxH / 2 + tickLen), strokeWidth = 5f)
                // Bottom-Right corner
                drawLine(accentColor, Offset(boxX + boxW / 2 + 4f, boxY + boxH / 2), Offset(boxX + boxW / 2 - tickLen, boxY + boxH / 2), strokeWidth = 5f)
                drawLine(accentColor, Offset(boxX + boxW / 2, boxY + boxH / 2 + 4f), Offset(boxX + boxW / 2, boxY + boxH / 2 - tickLen), strokeWidth = 5f)

                // Central crosshair dot
                drawCircle(color = accentColor, radius = 4f, center = Offset(boxX, boxY))

                // Bounding box descriptive text labels
                // Draw coordinate text as an overlay in the preview itself!
                val coordText = "TARGET CL: (x:${"%.2f".format(trackerBox.xPercent)}, y:${"%.2f".format(trackerBox.yPercent)})"
                // Draw a small background pill behind text on canvas
            }
        }

        // 3. Render any Text/Sticker overlays pinned to the active tracker coordinates
        val trackerBox = uiState.activeTrackerBox
        if (trackerBox != null) {
            val w = canvasSize.width
            val h = canvasSize.height
            val boxX = trackerBox.xPercent * w
            val boxY = trackerBox.yPercent * h
            val boxH = trackerBox.heightPercent * h

            uiState.overlayItems.forEach { overlay ->
                if (uiState.currentTimeMs >= overlay.startMs && uiState.currentTimeMs <= overlay.endMs) {
                    val overlayColor = try {
                        Color(android.graphics.Color.parseColor(overlay.colorHex))
                    } catch (e: Exception) {
                        Color.Cyan
                    }

                    Box(
                        modifier = Modifier
                            .offset(
                                x = (boxX / (LocalContext.current.resources.displayMetrics.density)).dp - 40.dp,
                                y = ((boxY - boxH - 10f) / (LocalContext.current.resources.displayMetrics.density)).dp - 20.dp
                            )
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xDD000000))
                            .border(1.dp, overlayColor, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (overlay.type == "sticker") {
                                Text(overlay.content, fontSize = 14.sp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Tag,
                                    contentDescription = null,
                                    tint = overlayColor,
                                    modifier = Modifier.size(10.dp)
                                )
                                Text(
                                    text = overlay.content,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * High-tech multi-track Timeline representation with playback controls.
 */
@Composable
fun TimelineTracksWidget(
    uiState: EditorUiState,
    accentColor: Color,
    onSeek: (Long) -> Unit,
    onPlayToggle: () -> Unit
) {
    val duration = 10000f // 10s constant

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("timeline_board"),
        borderColor = Color(0x1FFFFFFF),
        backgroundColor = Color(0x27101014)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Player controls row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onSeek(0) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Replay, contentDescription = "Rewind", tint = Color.LightGray)
                    }

                    // Large Play Button
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(50))
                            .background(accentColor.copy(alpha = 0.2f))
                            .border(1.dp, accentColor, RoundedCornerShape(50))
                            .clickable(onClick = onPlayToggle)
                            .testTag("play_pause_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Text(
                        text = "SOLVER LINKED",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (uiState.isTrackingActive) Color(0xFFFF007F) else Color(0xFF00FFCC),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                // Time counters
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "00:${"%02d".format(uiState.currentTimeMs / 1000)} : ${"%02d".format((uiState.currentTimeMs % 1000) / 10)}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                    Text(
                        text = "FRAME ${uiState.currentFrameIndex} / 600",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.Gray,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
            }

            // Scrubbing Slider bar
            Slider(
                value = uiState.currentTimeMs.toFloat(),
                onValueChange = { onSeek(it.toLong()) },
                valueRange = 0f..duration,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("timeline_slider"),
                colors = SliderDefaults.colors(
                    activeTrackColor = accentColor,
                    inactiveTrackColor = Color(0x22FFFFFF),
                    thumbColor = Color.White
                )
            )

            // Tracks Visualization Drawer (Simulating dynamic layers!)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0C0C10), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // TRACK 1: Video Footage Segment
                TimelineTrackRow(
                    trackLabel = "V1 FOOTAGE",
                    trackColor = Color(0xFF263238)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // stylized footage frames markers
                        repeat(5) {
                            Icon(
                                imageVector = Icons.Default.MovieCreation,
                                contentDescription = null,
                                tint = Color(0x22FFFFFF),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // TRACK 2: Motion Track Points
                TimelineTrackRow(
                    trackLabel = "M1 MOT TRACK",
                    trackColor = Color(0xFF1B0C1E)
                ) {
                    // Draw mini dots representing existing saved TrackerPoints
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        uiState.trackerPoints.forEach { point ->
                            val fraction = point.frameIndex / 600f
                            drawCircle(
                                color = accentColor,
                                radius = 4f,
                                center = Offset(fraction * size.width, size.height / 2)
                            )
                        }
                    }
                }

                // TRACK 3: Overlay Layers
                TimelineTrackRow(
                    trackLabel = "O1 OVERLAY",
                    trackColor = Color(0xFF0C191E)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        uiState.overlayItems.forEach { item ->
                            val startFrac = item.startMs / duration
                            val endFrac = item.endMs / duration
                            val lengthFrac = (endFrac - startFrac).coerceAtLeast(0.1f)

                            val color = try {
                                Color(android.graphics.Color.parseColor(item.colorHex))
                            } catch (e: Exception) {
                                Color.Cyan
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxHeight(0.7f)
                                    .fillMaxWidth(lengthFrac)
                                    .offset(x = (startFrac * 220f).dp) // mock translation offset
                                    .align(Alignment.CenterStart)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(color.copy(alpha = 0.35f))
                                    .border(1.dp, color, RoundedCornerShape(4.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = item.content,
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimelineTrackRow(
    trackLabel: String,
    trackColor: Color,
    content: @Composable BoxScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Track Name label
        Box(
            modifier = Modifier
                .width(76.dp)
                .fillMaxHeight()
                .background(Color(0xFF15151B), RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = trackLabel,
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                color = Color.LightGray,
                modifier = Modifier.padding(start = 4.dp),
                fontFamily = FontFamily.Monospace
            )
        }

        // Track content timeline block
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(trackColor, RoundedCornerShape(4.dp)),
            content = content
        )
    }
}

/**
 * Controller setup panel to modify the tracking model, search parameters, and execute solver scans.
 */
@Composable
fun MotionTrackerSettingsPanel(
    solverEngine: String,
    onEngineChange: (String) -> Unit,
    windowSize: Float,
    onWindowSizeChange: (Float) -> Unit,
    precisionThreshold: Float,
    onPrecisionChange: (Float) -> Unit,
    colorSpace: String,
    onColorSpaceChange: (String) -> Unit,
    isTrackingActive: Boolean,
    onStartTracking: () -> Unit,
    onClearKeys: () -> Unit,
    accentColor: Color,
    trackingProgress: Float
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("solver_config_panel"),
        borderColor = Color(0x1AFFFFFF),
        backgroundColor = Color(0x3B101014)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "MOTION TRACK SOLVER CONFIG",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.5.sp,
                    fontFamily = FontFamily.Monospace
                )
            )

            // Select Model Type
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Solver Model Engine", color = Color.Gray, fontSize = 10.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("CSRT", "MIL", "KCF", "OpticalFlow").forEach { engine ->
                        val isSel = solverEngine == engine
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) accentColor.copy(alpha = 0.2f) else Color(0x11FFFFFF))
                                .border(
                                    1.dp,
                                    if (isSel) accentColor else Color(0x15FFFFFF),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { onEngineChange(engine) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = engine,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (isSel) Color.White else Color.LightGray,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp
                                )
                            )
                        }
                    }
                }
            }

            // Sliders for Window size & Threshold
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Search Box: ${windowSize.toInt()}px", color = Color.Gray, fontSize = 10.sp)
                    Slider(
                        value = windowSize,
                        onValueChange = onWindowSizeChange,
                        valueRange = 16f..128f,
                        colors = SliderDefaults.colors(activeTrackColor = accentColor)
                    )
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Min Confidence: ${"%.2f".format(precisionThreshold)}", color = Color.Gray, fontSize = 10.sp)
                    Slider(
                        value = precisionThreshold,
                        onValueChange = onPrecisionChange,
                        valueRange = 0.40f..0.95f,
                        colors = SliderDefaults.colors(activeTrackColor = accentColor)
                    )
                }
            }

            // Select HSV space
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Color Space Space", color = Color.Gray, fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("HSV", "RGB", "GRAY").forEach { space ->
                        val active = colorSpace == space
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (active) accentColor.copy(alpha = 0.15f) else Color(0x0AFFFFFF))
                                .border(1.dp, if (active) accentColor else Color.Transparent, RoundedCornerShape(6.dp))
                                .clickable { onColorSpaceChange(space) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(space, color = if (active) accentColor else Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Progressive solver auto tracker execution button
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isTrackingActive) {
                    LinearProgressIndicator(
                        progress = { trackingProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(50)),
                        color = accentColor,
                        trackColor = Color(0x11FFFFFF)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onClearKeys,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = ButtonDefaults.outlinedButtonBorder(true).copy(
                            brush = Brush.linearGradient(listOf(Color(0x33FFFFFF), Color(0x11FFFFFF)))
                        )
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.ClearAll, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("CLEAR KEYS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = onStartTracking,
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("auto_track_solve_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isTrackingActive) Color.Red else accentColor
                        )
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isTrackingActive) Icons.Default.Stop else Icons.Default.CenterFocusStrong,
                                contentDescription = null,
                                tint = if (isTrackingActive) Color.White else Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (isTrackingActive) "PAUSE SOLVE" else "AUTO-TRACK SOLVE",
                                color = if (isTrackingActive) Color.White else Color.Black,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Technical shell-style logs log trace drawer.
 */
@Composable
fun LiveLogsWidget(logs: List<String>) {
    GlassBox(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp),
        borderColor = Color(0x15FFFFFF),
        backgroundColor = Color(0x5E030305)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
        ) {
            Text(
                text = "SOLVER SYSTEM DIAGNOSTICS LOG",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color.Gray,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(bottom = 6.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(logs) { log ->
                    Text(
                        text = log,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (log.contains("error", ignoreCase = true)) Color.Red else Color(0xFFE0E0E0),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
            }
        }
    }
}
