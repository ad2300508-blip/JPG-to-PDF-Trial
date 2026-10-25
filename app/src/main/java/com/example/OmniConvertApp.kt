package com.example

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.ui.text.style.TextOverflow

enum class WorkspaceDestination(
    val title: String,
    val icon: ImageVector,
    val tintColor: Color
) {
    PDF_STUDIO("Images to PDF", Icons.Default.PictureAsPdf, com.example.ui.theme.TintPdf),
    PDF_MERGE("Merge PDF", Icons.AutoMirrored.Filled.MergeType, com.example.ui.theme.TintMerge),
    IMAGE_LAB("Images", Icons.Default.Image, com.example.ui.theme.TintImage),
    AUDIO_LAB("Audio", Icons.Default.Audiotrack, com.example.ui.theme.TintAudio),
    ARCHIVE_ZIP("ZIP", Icons.Default.Archive, com.example.ui.theme.TintZip),
    AI_MARKDOWN("Markdown for AI", Icons.Default.SmartToy, com.example.ui.theme.TintAi),
    GEMINI_HUB("Gemini", Icons.Default.AutoAwesome, com.example.ui.theme.TintAi)
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
            // Phone Handheld Layout: Full-Width Workspace with Bottom Navigation Bar
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding(),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        tonalElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            WorkspaceDestination.values().forEach { destination ->
                                val isSelected = currentWorkspace == destination
                                Surface(
                                    onClick = { currentWorkspace = destination },
                                    shape = RoundedCornerShape(24.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("phone_nav_${destination.name.lowercase()}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(destination.tintColor.copy(alpha = 0.14f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = destination.icon,
                                                contentDescription = destination.title,
                                                tint = destination.tintColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = destination.title,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
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
            // Tablet Landscape Layout: 88dp wide Navigation Rail + Right Content
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = MaterialTheme.colorScheme.background
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(innerPadding)
                ) {
                    NavigationRail(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(88.dp)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
                            )
                            .testTag("tablet_navigation_rail"),
                        containerColor = MaterialTheme.colorScheme.surface,
                        header = {
                            Column(
                                modifier = Modifier.padding(top = 16.dp, bottom = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "OmniConvert",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    ) {
                        WorkspaceDestination.values().forEach { destination ->
                            val isSelected = currentWorkspace == destination
                            NavigationRailItem(
                                selected = isSelected,
                                onClick = { currentWorkspace = destination },
                                icon = {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(destination.tintColor.copy(alpha = 0.14f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = destination.icon,
                                            contentDescription = destination.title,
                                            tint = destination.tintColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                },
                                label = {
                                    Text(
                                        text = destination.title,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
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
