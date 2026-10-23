package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMerge
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.components.PdfDocumentCard
import com.example.ui.components.PdfMergeSettingsPanel
import com.example.ui.components.PdfMergeSuccessDialog
import com.example.viewmodel.MergeState
import com.example.viewmodel.PdfMergeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfMergeScreen(
  viewModel: PdfMergeViewModel,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }

  val pdfDocuments by viewModel.pdfDocuments.collectAsState()
  val settings by viewModel.settings.collectAsState()
  val mergeState by viewModel.mergeState.collectAsState()
  val isReadingFiles by viewModel.isReadingFiles.collectAsState()
  val totalCombinedPages by viewModel.totalCombinedPages.collectAsState()
  val formattedInputSize by viewModel.formattedTotalInputSize.collectAsState()

  // PDF Document File Picker
  val pdfPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenMultipleDocuments()
  ) { uris: List<Uri> ->
    if (uris.isNotEmpty()) {
      viewModel.addPdfs(context, uris)
    }
  }

  // Handle errors
  LaunchedEffect(mergeState) {
    if (mergeState is MergeState.Error) {
      snackbarHostState.showSnackbar(
        message = (mergeState as MergeState.Error).message,
        duration = SnackbarDuration.Long
      )
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        ),
        title = {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.primaryContainer,
              modifier = Modifier.size(38.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.CallMerge,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(22.dp)
                )
              }
            }

            Column {
              Text(
                text = "Merge PDFs",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Combine Multiple Documents",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        },
        actions = {
          if (pdfDocuments.isNotEmpty()) {
            Surface(
              shape = CircleShape,
              color = MaterialTheme.colorScheme.surfaceVariant,
              modifier = Modifier.padding(end = 6.dp)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.PictureAsPdf,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(14.dp)
                )
                Text(
                  text = "${pdfDocuments.size} PDFs",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary
                )
              }
            }

            IconButton(
              onClick = { viewModel.clearAll() },
              modifier = Modifier.testTag("clear_all_pdfs_button")
            ) {
              Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Clear All",
                tint = MaterialTheme.colorScheme.error
              )
            }
          }
        }
      )
    },
    snackbarHost = { SnackbarHost(snackbarHostState) },
    modifier = modifier.fillMaxSize()
  ) { paddingValues ->
    BoxWithConstraints(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
    ) {
      val isWideScreen = maxWidth >= 720.dp

      if (pdfDocuments.isEmpty() && !isReadingFiles) {
        // Empty State: Prominent PDF Import View
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
          contentAlignment = Alignment.Center
        ) {
          Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ),
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp)
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(80.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.UploadFile,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(44.dp)
                )
              }

              Text(
                text = "Merge Two or More PDFs",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
              )

              Text(
                text = "Select multiple PDF documents to join them into a single high-quality file. You can easily reorder pages, filter page ranges, and encrypt with a password.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
              )

              Spacer(modifier = Modifier.height(8.dp))

              Button(
                onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                  .height(52.dp)
                  .testTag("select_pdfs_button")
              ) {
                Icon(
                  imageVector = Icons.Default.Add,
                  contentDescription = null,
                  modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Select PDF Files",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }
        }
      } else if (isReadingFiles) {
        // Reading files indicator
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
          ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Text(
              text = "Reading PDF files & generating previews...",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      } else {
        // Active layout: Left list + Right configuration
        if (isWideScreen) {
          Row(modifier = Modifier.fillMaxSize()) {
            // Left Pane: Document list
            Box(
              modifier = Modifier
                .weight(1.1f)
                .fillMaxHeight()
            ) {
              PdfDocumentListPane(
                items = pdfDocuments,
                onAddMore = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                onMoveUp = { idx -> viewModel.moveUp(idx) },
                onMoveDown = { idx -> viewModel.moveDown(idx) },
                onRemove = { id -> viewModel.removePdf(id) },
                onUpdatePageRange = { id, range -> viewModel.updatePageRange(id, range) }
              )
            }

            // Right Pane: Settings & Merge trigger
            Box(
              modifier = Modifier
                .weight(0.9f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface)
                .border(
                  width = 1.dp,
                  color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            ) {
              PdfMergeSettingsPanel(
                settings = settings,
                onSettingsChanged = { updater -> viewModel.updateSettings(updater) },
                documentCount = pdfDocuments.size,
                totalCombinedPages = totalCombinedPages,
                formattedInputSize = formattedInputSize,
                onSortByName = { asc -> viewModel.sortByName(asc) },
                onSortBySize = { desc -> viewModel.sortBySize(desc) },
                onSortByPageCount = { desc -> viewModel.sortByPageCount(desc) },
                onMergeClick = { viewModel.startMerge(context) },
                isMerging = mergeState is MergeState.Merging
              )
            }
          }
        } else {
          // Compact / Phone layout: Tabbed navigation
          var selectedTab by remember { mutableIntStateOf(0) }
          Column(modifier = Modifier.fillMaxSize()) {
            PrimaryTabRow(
              selectedTabIndex = selectedTab,
              containerColor = MaterialTheme.colorScheme.surface
            ) {
              Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Documents (${pdfDocuments.size})", fontWeight = FontWeight.Bold) }
              )
              Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Merge Settings", fontWeight = FontWeight.Bold) }
              )
            }

            if (selectedTab == 0) {
              PdfDocumentListPane(
                items = pdfDocuments,
                onAddMore = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                onMoveUp = { idx -> viewModel.moveUp(idx) },
                onMoveDown = { idx -> viewModel.moveDown(idx) },
                onRemove = { id -> viewModel.removePdf(id) },
                onUpdatePageRange = { id, range -> viewModel.updatePageRange(id, range) }
              )
            } else {
              PdfMergeSettingsPanel(
                settings = settings,
                onSettingsChanged = { updater -> viewModel.updateSettings(updater) },
                documentCount = pdfDocuments.size,
                totalCombinedPages = totalCombinedPages,
                formattedInputSize = formattedInputSize,
                onSortByName = { asc -> viewModel.sortByName(asc) },
                onSortBySize = { desc -> viewModel.sortBySize(desc) },
                onSortByPageCount = { desc -> viewModel.sortByPageCount(desc) },
                onMergeClick = { viewModel.startMerge(context) },
                isMerging = mergeState is MergeState.Merging
              )
            }
          }
        }
      }
    }

    // Merging Progress Overlay Dialog
    val currentMergeState = mergeState
    if (currentMergeState is MergeState.Merging) {
      BasicAlertDialog(
        onDismissRequest = {},
        modifier = Modifier.testTag("merging_progress_dialog")
      ) {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          modifier = Modifier.padding(16.dp)
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
          ) {
            CircularProgressIndicator(
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(44.dp)
            )

            Text(
              text = "Merging Documents...",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )

            Text(
              text = "Processing ${currentMergeState.current} of ${currentMergeState.total}: \"${currentMergeState.currentFileName}\"",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center
            )

            LinearProgressIndicator(
              progress = { currentMergeState.current.toFloat() / currentMergeState.total.toFloat() },
              modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            )
          }
        }
      }
    }

    // Success Dialog
    if (currentMergeState is MergeState.Success) {
      PdfMergeSuccessDialog(
        mergedFile = currentMergeState.mergedFile,
        totalPages = currentMergeState.totalPages,
        fileSizeBytes = currentMergeState.fileSizeBytes,
        onDismiss = { viewModel.resetMergeState() },
        onShowSnackbar = { msg ->
          scope.launch { snackbarHostState.showSnackbar(msg) }
        }
      )
    }
  }
}

@Composable
private fun PdfDocumentListPane(
  items: List<com.example.model.PdfDocumentItem>,
  onAddMore: () -> Unit,
  onMoveUp: (Int) -> Unit,
  onMoveDown: (Int) -> Unit,
  onRemove: (String) -> Unit,
  onUpdatePageRange: (String, String) -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    modifier = modifier.fillMaxSize()
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Documents Order (${items.size})",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )

        FilledTonalButton(
          onClick = onAddMore,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.testTag("add_more_pdfs_button")
        ) {
          Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text("Add PDFs")
        }
      }
    }

    itemsIndexed(items, key = { _, it -> it.id }) { index, item ->
      PdfDocumentCard(
        item = item,
        index = index,
        totalCount = items.size,
        onMoveUp = { onMoveUp(index) },
        onMoveDown = { onMoveDown(index) },
        onRemove = { onRemove(item.id) },
        onUpdatePageRange = { range -> onUpdatePageRange(item.id, range) }
      )
    }
  }
}
