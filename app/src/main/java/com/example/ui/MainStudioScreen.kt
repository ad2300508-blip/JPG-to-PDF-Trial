package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMerge
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.ArchiveViewModel
import com.example.viewmodel.AudioLabViewModel
import com.example.viewmodel.ImageLabViewModel
import com.example.viewmodel.PdfMergeViewModel
import com.example.viewmodel.PdfViewModel

enum class OmniWorkspace {
  PDF_STUDIO,
  IMAGE_LAB,
  AUDIO_LAB,
  ARCHIVE_ZIP
}

enum class PdfSubMode {
  IMAGE_TO_PDF,
  MERGE_PDFS
}

@Composable
fun MainStudioScreen(
  imagePdfViewModel: PdfViewModel,
  pdfMergeViewModel: PdfMergeViewModel,
  imageLabViewModel: ImageLabViewModel,
  audioLabViewModel: AudioLabViewModel,
  archiveViewModel: ArchiveViewModel,
  modifier: Modifier = Modifier
) {
  var currentWorkspace by rememberSaveable { mutableStateOf(OmniWorkspace.PDF_STUDIO) }
  var pdfSubMode by rememberSaveable { mutableStateOf(PdfSubMode.IMAGE_TO_PDF) }

  // Predictive BackHandler:
  // If in another workspace, return to PDF Studio.
  // If in PDF Studio and on MERGE_PDFS, return to IMAGE_TO_PDF.
  BackHandler(enabled = currentWorkspace != OmniWorkspace.PDF_STUDIO || pdfSubMode != PdfSubMode.IMAGE_TO_PDF) {
    if (currentWorkspace != OmniWorkspace.PDF_STUDIO) {
      currentWorkspace = OmniWorkspace.PDF_STUDIO
    } else {
      pdfSubMode = PdfSubMode.IMAGE_TO_PDF
    }
  }

  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val isTabletLandscape = maxWidth >= 840.dp

    if (isTabletLandscape) {
      // Tablet Landscape: Left Material 3 Navigation Rail + Workspace Canvas
      Row(modifier = Modifier.fillMaxSize()) {
        NavigationRail(
          containerColor = MaterialTheme.colorScheme.surface,
          header = {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier.padding(vertical = 12.dp)
            ) {
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(44.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Default.Transform,
                    contentDescription = "OmniConvert Studio",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                  )
                }
              }
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "OmniConvert",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
              )
            }
          },
          modifier = Modifier
            .fillMaxHeight()
            .testTag("tablet_navigation_rail")
        ) {
          NavigationRailItem(
            selected = currentWorkspace == OmniWorkspace.PDF_STUDIO,
            onClick = { currentWorkspace = OmniWorkspace.PDF_STUDIO },
            icon = { Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF Studio") },
            label = { Text("PDF Studio", fontWeight = FontWeight.SemiBold, fontSize = 11.sp) },
            modifier = Modifier.testTag("nav_rail_pdf_studio")
          )

          NavigationRailItem(
            selected = currentWorkspace == OmniWorkspace.IMAGE_LAB,
            onClick = { currentWorkspace = OmniWorkspace.IMAGE_LAB },
            icon = { Icon(Icons.Default.Image, contentDescription = "Image Lab") },
            label = { Text("Image Lab", fontWeight = FontWeight.SemiBold, fontSize = 11.sp) },
            modifier = Modifier.testTag("nav_rail_image_lab")
          )

          NavigationRailItem(
            selected = currentWorkspace == OmniWorkspace.AUDIO_LAB,
            onClick = { currentWorkspace = OmniWorkspace.AUDIO_LAB },
            icon = { Icon(Icons.Default.Audiotrack, contentDescription = "Audio Lab") },
            label = { Text("Audio Lab", fontWeight = FontWeight.SemiBold, fontSize = 11.sp) },
            modifier = Modifier.testTag("nav_rail_audio_lab")
          )

          NavigationRailItem(
            selected = currentWorkspace == OmniWorkspace.ARCHIVE_ZIP,
            onClick = { currentWorkspace = OmniWorkspace.ARCHIVE_ZIP },
            icon = { Icon(Icons.Default.FolderZip, contentDescription = "Archive ZIP") },
            label = { Text("Archive ZIP", fontWeight = FontWeight.SemiBold, fontSize = 11.sp) },
            modifier = Modifier.testTag("nav_rail_archive_zip")
          )
        }

        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
        ) {
          WorkspaceContent(
            workspace = currentWorkspace,
            pdfSubMode = pdfSubMode,
            onPdfSubModeChange = { pdfSubMode = it },
            imagePdfViewModel = imagePdfViewModel,
            pdfMergeViewModel = pdfMergeViewModel,
            imageLabViewModel = imageLabViewModel,
            audioLabViewModel = audioLabViewModel,
            archiveViewModel = archiveViewModel
          )
        }
      }
    } else {
      // Mobile / Compact: Content Canvas + M3 Bottom NavigationBar
      Scaffold(
        bottomBar = {
          NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            modifier = Modifier
              .windowInsetsPadding(WindowInsets.navigationBars)
              .testTag("bottom_navigation_bar")
          ) {
            NavigationBarItem(
              selected = currentWorkspace == OmniWorkspace.PDF_STUDIO,
              onClick = { currentWorkspace = OmniWorkspace.PDF_STUDIO },
              icon = { Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF Studio") },
              label = {
                Text(
                  "PDF",
                  fontWeight = if (currentWorkspace == OmniWorkspace.PDF_STUDIO) FontWeight.Bold else FontWeight.Normal,
                  fontSize = 11.sp
                )
              },
              modifier = Modifier.testTag("bottom_nav_pdf_studio")
            )

            NavigationBarItem(
              selected = currentWorkspace == OmniWorkspace.IMAGE_LAB,
              onClick = { currentWorkspace = OmniWorkspace.IMAGE_LAB },
              icon = { Icon(Icons.Default.Image, contentDescription = "Image Lab") },
              label = {
                Text(
                  "Images",
                  fontWeight = if (currentWorkspace == OmniWorkspace.IMAGE_LAB) FontWeight.Bold else FontWeight.Normal,
                  fontSize = 11.sp
                )
              },
              modifier = Modifier.testTag("bottom_nav_image_lab")
            )

            NavigationBarItem(
              selected = currentWorkspace == OmniWorkspace.AUDIO_LAB,
              onClick = { currentWorkspace = OmniWorkspace.AUDIO_LAB },
              icon = { Icon(Icons.Default.Audiotrack, contentDescription = "Audio Lab") },
              label = {
                Text(
                  "Audio",
                  fontWeight = if (currentWorkspace == OmniWorkspace.AUDIO_LAB) FontWeight.Bold else FontWeight.Normal,
                  fontSize = 11.sp
                )
              },
              modifier = Modifier.testTag("bottom_nav_audio_lab")
            )

            NavigationBarItem(
              selected = currentWorkspace == OmniWorkspace.ARCHIVE_ZIP,
              onClick = { currentWorkspace = OmniWorkspace.ARCHIVE_ZIP },
              icon = { Icon(Icons.Default.FolderZip, contentDescription = "Archive ZIP") },
              label = {
                Text(
                  "ZIP",
                  fontWeight = if (currentWorkspace == OmniWorkspace.ARCHIVE_ZIP) FontWeight.Bold else FontWeight.Normal,
                  fontSize = 11.sp
                )
              },
              modifier = Modifier.testTag("bottom_nav_archive_zip")
            )
          }
        },
        modifier = Modifier.fillMaxSize()
      ) { innerPadding ->
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        ) {
          WorkspaceContent(
            workspace = currentWorkspace,
            pdfSubMode = pdfSubMode,
            onPdfSubModeChange = { pdfSubMode = it },
            imagePdfViewModel = imagePdfViewModel,
            pdfMergeViewModel = pdfMergeViewModel,
            imageLabViewModel = imageLabViewModel,
            audioLabViewModel = audioLabViewModel,
            archiveViewModel = archiveViewModel
          )
        }
      }
    }
  }
}

@Composable
private fun WorkspaceContent(
  workspace: OmniWorkspace,
  pdfSubMode: PdfSubMode,
  onPdfSubModeChange: (PdfSubMode) -> Unit,
  imagePdfViewModel: PdfViewModel,
  pdfMergeViewModel: PdfMergeViewModel,
  imageLabViewModel: ImageLabViewModel,
  audioLabViewModel: AudioLabViewModel,
  archiveViewModel: ArchiveViewModel,
  modifier: Modifier = Modifier
) {
  when (workspace) {
    OmniWorkspace.PDF_STUDIO -> {
      Column(modifier = modifier.fillMaxSize()) {
        // PDF Studio Sub-Mode Tab Header
        Surface(
          color = MaterialTheme.colorScheme.surface,
          tonalElevation = 1.dp,
          modifier = Modifier.fillMaxWidth()
        ) {
          TabRow(
            selectedTabIndex = if (pdfSubMode == PdfSubMode.IMAGE_TO_PDF) 0 else 1,
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
          ) {
            Tab(
              selected = pdfSubMode == PdfSubMode.IMAGE_TO_PDF,
              onClick = { onPdfSubModeChange(PdfSubMode.IMAGE_TO_PDF) },
              icon = { Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp)) },
              text = { Text("Images to PDF (Cleaner & Splitter)", fontWeight = FontWeight.SemiBold) },
              modifier = Modifier.testTag("pdf_tab_image_to_pdf")
            )
            Tab(
              selected = pdfSubMode == PdfSubMode.MERGE_PDFS,
              onClick = { onPdfSubModeChange(PdfSubMode.MERGE_PDFS) },
              icon = { Icon(Icons.AutoMirrored.Filled.CallMerge, contentDescription = null, modifier = Modifier.size(18.dp)) },
              text = { Text("Merge PDF Documents", fontWeight = FontWeight.SemiBold) },
              modifier = Modifier.testTag("pdf_tab_merge_pdfs")
            )
          }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
          when (pdfSubMode) {
            PdfSubMode.IMAGE_TO_PDF -> {
              PdfConverterScreen(viewModel = imagePdfViewModel)
            }
            PdfSubMode.MERGE_PDFS -> {
              PdfMergeScreen(viewModel = pdfMergeViewModel)
            }
          }
        }
      }
    }
    OmniWorkspace.IMAGE_LAB -> {
      ImageLabScreen(viewModel = imageLabViewModel)
    }
    OmniWorkspace.AUDIO_LAB -> {
      AudioLabScreen(viewModel = audioLabViewModel)
    }
    OmniWorkspace.ARCHIVE_ZIP -> {
      ArchiveScreen(viewModel = archiveViewModel)
    }
  }
}
