package com.example.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.sin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.EditingProject
import com.example.ui.components.GlassBox
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToEditor: (Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var projectName by remember { mutableStateOf("") }
    var selectedVideoType by remember { mutableStateOf("tokyo_neon") }
    var selectedAspectRatio by remember { mutableStateOf("16:9") }
    var showCreateDialog by remember { mutableStateOf(false) }

    val videoTemplates = remember {
        listOf(
            VideoTemplate("tokyo_neon", "Tokyo Neon", "Cyber motorcycle chase at night", Icons.Default.DirectionsBike, Color(0xFFFF007F)),
            VideoTemplate("apex_skater", "Apex Skater", "Parabolic skateboard stunt jump", Icons.Default.DirectionsRun, Color(0xFFFF8C00)),
            VideoTemplate("f1_speed", "F1 Speed", "Racing curves and telemetric drift", Icons.Default.DirectionsCar, Color(0xFF00FFCC)),
            VideoTemplate("drone_skyline", "Drone Orbit", "Rotational orbital cityscape spire", Icons.Default.LocationCity, Color(0xFF007FFF))
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070709))
            .drawBehind {
                // Futuristic background technical grid
                val gridSpacing = 48.dp.toPx()
                val paintColor = Color(0x06FFFFFF)
                var x = 0f
                while (x < size.width) {
                    drawLine(paintColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                    x += gridSpacing
                }
                var y = 0f
                while (y < size.height) {
                    drawLine(paintColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                    y += gridSpacing
                }

                // Cyberpunk ambient background glows
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x18FF007F), Color.Transparent),
                        center = Offset(size.width * 0.1f, size.height * 0.1f),
                        radius = size.width * 0.7f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x1500FFCC), Color.Transparent),
                        center = Offset(size.width * 0.9f, size.height * 0.8f),
                        radius = size.width * 0.6f
                    )
                )
            }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 40.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "DOZOPAR",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 6.sp,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                        Text(
                            text = "ADVANCED MOTION TRACKING ENGINE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 2.sp,
                                color = Color(0xFF00FFCC),
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    // Active Solver Badge
                    GlassBox(
                        modifier = Modifier.padding(4.dp),
                        shape = RoundedCornerShape(30.dp),
                        backgroundColor = Color(0x1C00FFCC),
                        borderColor = Color(0x4400FFCC)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color(0xFF00FFCC))
                            )
                            Text(
                                text = "SOLVER ONLINE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00FFCC)
                                )
                            )
                        }
                    }
                }
            }

            // Stats row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "SOLVER RUNS",
                        value = "${uiState.projects.size}",
                        icon = Icons.Default.PlayArrow,
                        accentColor = Color(0xFFFF007F)
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "TRACKED KEYS",
                        value = "${uiState.totalKeyframesCount}",
                        icon = Icons.Default.Timeline,
                        accentColor = Color(0xFF00FFCC)
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "RENDER CONF.",
                        value = if (uiState.averageConfidence > 0) "${"%.1f".format(uiState.averageConfidence)}%" else "--",
                        icon = Icons.Default.OfflineBolt,
                        accentColor = Color(0xFF007FFF)
                    )
                }
            }

            // Initialize Project Button
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("init_project_banner"),
                    onClick = { showCreateDialog = true },
                    borderColor = Color(0x2EFFFFFF),
                    backgroundColor = Color(0x2816161B)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Initialize New Motion Render",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Create a custom track project using our high-precision optical flow & keyframe tracker modules.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF9E9E9E)
                                )
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFF00FFCC))
                                .clickable { showCreateDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add project",
                                tint = Color.Black
                            )
                        }
                    }
                }
            }

            // Projects Header
            item {
                Text(
                    text = "ACTIVE RENDERS",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = Color(0xFF88888D),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    ),
                    modifier = Modifier.padding(top = 10.dp)
                )
            }

            // List of projects
            if (uiState.projects.isEmpty()) {
                item {
                    GlassBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        backgroundColor = Color(0x12FFFFFF)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.VideoSettings,
                                contentDescription = "No projects",
                                tint = Color(0x66FFFFFF),
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "No workspace projects active",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "Tap the button above to spawn an advanced motion tracking project.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF757575),
                                    textAlign = TextAlign.Center
                                )
                            )
                        }
                    }
                }
            } else {
                items(uiState.projects, key = { it.id }) { project ->
                    ProjectRowItem(
                        project = project,
                        onOpen = { onNavigateToEditor(project.id) },
                        onDelete = { viewModel.deleteProject(project.id) }
                    )
                }
            }
        }

        // Beautiful glassmorphic dialog to create new project
        if (showCreateDialog) {
            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp))
                    .background(Color(0xFF0F0F13)),
                confirmButton = {},
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "NEW MOTION TRACK SOLVER",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 2.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        )

                        // Project Name Input
                        OutlinedTextField(
                            value = projectName,
                            onValueChange = { projectName = it },
                            label = { Text("Project Engine ID") },
                            placeholder = { Text("e.g. Skyline Tracking Alpha") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("project_name_input"),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0x22FFFFFF),
                                unfocusedContainerColor = Color(0x11FFFFFF),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.LightGray,
                                focusedLabelColor = Color(0xFF00FFCC),
                                unfocusedLabelColor = Color.Gray,
                                focusedIndicatorColor = Color(0xFF00FFCC)
                            )
                        )

                        // Video templates header
                        Text(
                            text = "CHOOSE VISUAL MOTION TARGET",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.Gray,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )

                        // Visual grid templates list
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .height(200.dp)
                                .fillMaxWidth()
                        ) {
                            items(videoTemplates) { template ->
                                val isSelected = selectedVideoType == template.type
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) template.color.copy(alpha = 0.22f) else Color(0x11FFFFFF))
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) template.color else Color(0x1FFFFFFF),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { selectedVideoType = template.type }
                                        .padding(12.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(
                                            imageVector = template.icon,
                                            contentDescription = template.title,
                                            tint = if (isSelected) template.color else Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Text(
                                            text = template.title,
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                        Text(
                                            text = template.description,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = Color.LightGray,
                                                fontSize = 10.sp
                                            ),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        // Aspect ratio
                        Text(
                            text = "RENDER ASPECT RATIO",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.Gray,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("16:9", "9:16", "1:1").forEach { ratio ->
                                val isSelected = selectedAspectRatio == ratio
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color(0x2200FFCC) else Color(0x11FFFFFF))
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) Color(0xFF00FFCC) else Color(0x1FFFFFFF),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { selectedAspectRatio = ratio }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = ratio,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = if (isSelected) Color(0xFF00FFCC) else Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Trigger button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showCreateDialog = false },
                                modifier = Modifier.weight(1f),
                                border = ButtonDefaults.outlinedButtonBorder(true).copy(
                                    brush = Brush.linearGradient(listOf(Color.Gray, Color.DarkGray))
                                )
                            ) {
                                Text("CANCEL", color = Color.White)
                            }

                            Button(
                                onClick = {
                                    showCreateDialog = false
                                    viewModel.createNewProject(
                                        name = projectName,
                                        videoType = selectedVideoType,
                                        aspectRatio = selectedAspectRatio,
                                        onCreated = { newId ->
                                            onNavigateToEditor(newId)
                                        }
                                    )
                                },
                                modifier = Modifier
                                    .weight(1.5f)
                                    .testTag("submit_create_project"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF00FFCC)
                                )
                            ) {
                                Text("LOAD SOLVER", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            )
        }
    }
}

data class VideoTemplate(
    val type: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color
) {
    GlassBox(
        modifier = modifier.height(96.dp),
        borderColor = accentColor.copy(alpha = 0.2f),
        backgroundColor = Color(0x2B121215)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.LightGray,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    )
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace
                )
            )
        }
    }
}

@Composable
fun ProjectRowItem(
    project: EditingProject,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    val accentColor = when (project.videoType) {
        "tokyo_neon" -> Color(0xFFFF007F)
        "apex_skater" -> Color(0xFFFF8C00)
        "f1_speed" -> Color(0xFF00FFCC)
        else -> Color(0xFF007FFF)
    }

    val icon = when (project.videoType) {
        "tokyo_neon" -> Icons.Default.DirectionsBike
        "apex_skater" -> Icons.Default.DirectionsRun
        "f1_speed" -> Icons.Default.DirectionsCar
        else -> Icons.Default.LocationCity
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("project_card_${project.id}"),
        onClick = onOpen,
        borderColor = Color(0x1BFFFFFF),
        backgroundColor = Color(0x3B101014)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Visual Preview Avatar
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF141419))
                    .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .drawBehind {
                        // Drawing decorative futuristic audio/visual geometric waves
                        val lines = 4
                        val hStep = size.width / (lines + 1)
                        for (i in 1..lines) {
                            val x = i * hStep
                            val waveHeight = size.height * (0.3f + 0.5f * sin(i * 1.5f))
                            drawLine(
                                color = accentColor.copy(alpha = 0.35f),
                                start = Offset(x, size.height / 2 - waveHeight / 2),
                                end = Offset(x, size.height / 2 + waveHeight / 2),
                                strokeWidth = 3f
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = project.aspectRatio,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                    Box(modifier = Modifier.size(4.dp).clip(RoundedCornerShape(50)).background(Color.Gray))
                    Text(
                        text = project.trackerEngineType,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = accentColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                }

                Text(
                    text = "Created: " + java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(project.createdAt)),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.DarkGray,
                        fontSize = 10.sp
                    )
                )
            }

            // Quick Delete
            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_project_btn_${project.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete project",
                    tint = Color(0xFFE57373)
                )
            }
        }
    }
}
