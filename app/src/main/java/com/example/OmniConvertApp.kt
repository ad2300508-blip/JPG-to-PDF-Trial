package com.example

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.automirrored.filled.MergeType
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.ArchiveScreen
import com.example.ui.AudioLabScreen
import com.example.ui.GeminiHubScreen
import com.example.ui.ImageLabScreen
import com.example.ui.MarkdownExporterScreen
import com.example.ui.PdfMergeScreen
import com.example.ui.PdfStudioScreen
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.PdfPurple
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate800
import com.example.ui.theme.SurfaceCard
import com.example.viewmodel.ArchiveViewModel
import com.example.viewmodel.AudioLabViewModel
import com.example.viewmodel.GeminiHubViewModel
import com.example.viewmodel.ImageLabViewModel
import com.example.viewmodel.MarkdownExporterViewModel
import com.example.viewmodel.PdfMergeViewModel
import com.example.viewmodel.PdfStudioViewModel

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset

enum class WorkspaceDestination(
    val title: String,
    val icon: ImageVector,
    val accentColor: Color
) {
    PDF_STUDIO("PDF Studio", Icons.Default.PictureAsPdf, CyanAccent),
    PDF_MERGE("PDF Merge", Icons.AutoMirrored.Filled.MergeType, PdfPurple),
    IMAGE_LAB("Image Lab", Icons.Default.Image, EmeraldAccent),
    AUDIO_LAB("Audio Lab", Icons.Default.Audiotrack, IndigoAccent),
    ARCHIVE_ZIP("Archive ZIP", Icons.Default.Archive, Color(0xFFF59E0B)),
    AI_MARKDOWN("AI Markdown", Icons.Default.SmartToy, CyanAccent),
    GEMINI_HUB("Gemini AI", Icons.Default.AutoAwesome, AmberAccent)
}

@Composable
fun OmniConvertApp(
    pdfViewModel: PdfStudioViewModel = viewModel(),
    pdfMergeViewModel: PdfMergeViewModel = viewModel(),
    imageViewModel: ImageLabViewModel = viewModel(),
    audioViewModel: AudioLabViewModel = viewModel(),
    archiveViewModel: ArchiveViewModel = viewModel(),
    markdownViewModel: MarkdownExporterViewModel = viewModel(),
    geminiViewModel: GeminiHubViewModel = viewModel()
) {
    var currentWorkspace by remember { mutableStateOf(WorkspaceDestination.PDF_STUDIO) }

    BackHandler(enabled = currentWorkspace != WorkspaceDestination.PDF_STUDIO) {
        currentWorkspace = WorkspaceDestination.PDF_STUDIO
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isCompact = maxWidth < 600.dp

        if (isCompact) {
            // Phone Handheld Layout: Top Brand Bar + Content + Bottom Scrollable Navigation Bar
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = MaterialTheme.colorScheme.background,
                topBar = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .background(SurfaceCard)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Slate800)
                                    .border(1.dp, CyanAccent.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "OC",
                                    color = CyanAccent,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "OmniConvert Studio",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(currentWorkspace.accentColor.copy(alpha = 0.15f))
                                .border(1.dp, currentWorkspace.accentColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = currentWorkspace.title.uppercase(),
                                color = currentWorkspace.accentColor,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                bottomBar = {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding(),
                        color = SurfaceCard,
                        border = BorderStroke(1.dp, BorderSubtle),
                        tonalElevation = 8.dp
                    ) {
                        ScrollableTabRow(
                            selectedTabIndex = currentWorkspace.ordinal,
                            containerColor = SurfaceCard,
                            contentColor = currentWorkspace.accentColor,
                            edgePadding = 8.dp,
                            indicator = { tabPositions ->
                                if (currentWorkspace.ordinal < tabPositions.size) {
                                    TabRowDefaults.SecondaryIndicator(
                                        modifier = Modifier.tabIndicatorOffset(tabPositions[currentWorkspace.ordinal]),
                                        color = currentWorkspace.accentColor,
                                        height = 3.dp
                                    )
                                }
                            },
                            divider = {}
                        ) {
                            WorkspaceDestination.values().forEach { destination ->
                                val isSelected = currentWorkspace == destination
                                Tab(
                                    selected = isSelected,
                                    onClick = { currentWorkspace = destination },
                                    modifier = Modifier
                                        .height(58.dp)
                                        .testTag("phone_nav_${destination.name.lowercase()}"),
                                    text = {
                                        Text(
                                            text = destination.title,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) destination.accentColor else Slate400,
                                            maxLines = 1
                                        )
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = destination.icon,
                                            contentDescription = destination.title,
                                            tint = if (isSelected) destination.accentColor else Slate400,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    WorkspaceContent(
                        workspace = currentWorkspace,
                        pdfViewModel = pdfViewModel,
                        pdfMergeViewModel = pdfMergeViewModel,
                        imageViewModel = imageViewModel,
                        audioViewModel = audioViewModel,
                        archiveViewModel = archiveViewModel,
                        markdownViewModel = markdownViewModel,
                        geminiViewModel = geminiViewModel,
                        onNavigate = { currentWorkspace = it }
                    )
                }
            }
        } else {
            // Tablet Landscape / Expanded Layout: Left Navigation Rail + Right Content
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = MaterialTheme.colorScheme.background
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    NavigationRail(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(108.dp)
                            .border(
                                width = 1.dp,
                                color = BorderSubtle,
                                shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
                            )
                            .testTag("tablet_navigation_rail"),
                        containerColor = SurfaceCard,
                        header = {
                            Box(
                                modifier = Modifier
                                    .padding(top = 16.dp, bottom = 16.dp)
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(Slate800)
                                    .border(1.dp, CyanAccent.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "OC",
                                    color = CyanAccent,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                        }
                    ) {
                        WorkspaceDestination.values().forEach { destination ->
                            val isSelected = currentWorkspace == destination
                            NavigationRailItem(
                                selected = isSelected,
                                onClick = { currentWorkspace = destination },
                                icon = {
                                    Icon(
                                        imageVector = destination.icon,
                                        contentDescription = destination.title,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = destination.title,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontFamily = FontFamily.Default,
                                        maxLines = 1
                                    )
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = destination.accentColor,
                                    selectedTextColor = destination.accentColor,
                                    indicatorColor = destination.accentColor.copy(alpha = 0.15f),
                                    unselectedIconColor = Slate400,
                                    unselectedTextColor = Slate400
                                ),
                                modifier = Modifier
                                    .padding(vertical = 2.dp)
                                    .testTag("nav_item_${destination.name.lowercase()}")
                            )
                        }
                    }

                    // Main Content Area
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        WorkspaceContent(
                            workspace = currentWorkspace,
                            pdfViewModel = pdfViewModel,
                            pdfMergeViewModel = pdfMergeViewModel,
                            imageViewModel = imageViewModel,
                            audioViewModel = audioViewModel,
                            archiveViewModel = archiveViewModel,
                            markdownViewModel = markdownViewModel,
                            geminiViewModel = geminiViewModel,
                            onNavigate = { currentWorkspace = it }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkspaceContent(
    workspace: WorkspaceDestination,
    pdfViewModel: PdfStudioViewModel,
    pdfMergeViewModel: PdfMergeViewModel,
    imageViewModel: ImageLabViewModel,
    audioViewModel: AudioLabViewModel,
    archiveViewModel: ArchiveViewModel,
    markdownViewModel: MarkdownExporterViewModel,
    geminiViewModel: GeminiHubViewModel,
    onNavigate: (WorkspaceDestination) -> Unit
) {
    when (workspace) {
        WorkspaceDestination.PDF_STUDIO -> PdfStudioScreen(viewModel = pdfViewModel)
        WorkspaceDestination.PDF_MERGE -> PdfMergeScreen(viewModel = pdfMergeViewModel)
        WorkspaceDestination.IMAGE_LAB -> ImageLabScreen(viewModel = imageViewModel)
        WorkspaceDestination.AUDIO_LAB -> AudioLabScreen(
            viewModel = audioViewModel,
            onTranscribeAudio = { uri, _ ->
                geminiViewModel.attachAudio(uri)
                onNavigate(WorkspaceDestination.GEMINI_HUB)
            }
        )
        WorkspaceDestination.ARCHIVE_ZIP -> ArchiveScreen(viewModel = archiveViewModel)
        WorkspaceDestination.AI_MARKDOWN -> MarkdownExporterScreen(
            viewModel = markdownViewModel,
            onSendToGemini = { docText ->
                geminiViewModel.setContextDocument(docText)
                onNavigate(WorkspaceDestination.GEMINI_HUB)
            }
        )
        WorkspaceDestination.GEMINI_HUB -> GeminiHubScreen(
            viewModel = geminiViewModel,
            onNavigateToMarkdown = {
                onNavigate(WorkspaceDestination.AI_MARKDOWN)
            }
        )
    }
}
