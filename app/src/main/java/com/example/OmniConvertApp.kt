package com.example

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Image
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
import com.example.ui.ImageLabScreen
import com.example.ui.MarkdownExporterScreen
import com.example.ui.PdfStudioScreen
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.SurfaceCard
import com.example.viewmodel.ArchiveViewModel
import com.example.viewmodel.AudioLabViewModel
import com.example.viewmodel.ImageLabViewModel
import com.example.viewmodel.MarkdownExporterViewModel
import com.example.viewmodel.PdfStudioViewModel

enum class WorkspaceDestination(
    val title: String,
    val icon: ImageVector,
    val accentColor: Color
) {
    PDF_STUDIO("PDF Studio", Icons.Default.PictureAsPdf, CyanAccent),
    IMAGE_LAB("Image Lab", Icons.Default.Image, EmeraldAccent),
    AUDIO_LAB("Audio Lab", Icons.Default.Audiotrack, IndigoAccent),
    ARCHIVE_ZIP("Archive ZIP", Icons.Default.Archive, Color(0xFFF59E0B)),
    AI_MARKDOWN("AI Markdown", Icons.Default.SmartToy, CyanAccent)
}

@Composable
fun OmniConvertApp(
    pdfViewModel: PdfStudioViewModel = viewModel(),
    imageViewModel: ImageLabViewModel = viewModel(),
    audioViewModel: AudioLabViewModel = viewModel(),
    archiveViewModel: ArchiveViewModel = viewModel(),
    markdownViewModel: MarkdownExporterViewModel = viewModel()
) {
    var currentWorkspace by remember { mutableStateOf(WorkspaceDestination.PDF_STUDIO) }

    BackHandler(enabled = currentWorkspace != WorkspaceDestination.PDF_STUDIO) {
        currentWorkspace = WorkspaceDestination.PDF_STUDIO
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Sleek Material 3 Navigation Rail for Tablets
            NavigationRail(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(104.dp)
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
                            .padding(top = 16.dp, bottom = 20.dp)
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
                when (currentWorkspace) {
                    WorkspaceDestination.PDF_STUDIO -> PdfStudioScreen(viewModel = pdfViewModel)
                    WorkspaceDestination.IMAGE_LAB -> ImageLabScreen(viewModel = imageViewModel)
                    WorkspaceDestination.AUDIO_LAB -> AudioLabScreen(viewModel = audioViewModel)
                    WorkspaceDestination.ARCHIVE_ZIP -> ArchiveScreen(viewModel = archiveViewModel)
                    WorkspaceDestination.AI_MARKDOWN -> MarkdownExporterScreen(viewModel = markdownViewModel)
                }
            }
        }
    }
}
