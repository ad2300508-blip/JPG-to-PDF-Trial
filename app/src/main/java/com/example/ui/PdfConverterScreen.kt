package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Tablet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import com.example.model.ImageItem
import com.example.service.PdfGeneratorService
import com.example.ui.components.ConversionProgressDialog
import com.example.ui.components.DownloadSuccessDialog
import com.example.ui.components.ImageGridItem
import com.example.ui.components.PdfSuccessCard
import com.example.ui.components.SettingsPanel
import com.example.ui.theme.PdfRed
import com.example.util.DownloadUtils
import com.example.viewmodel.ConversionState
import com.example.viewmodel.PdfViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfConverterScreen(
  viewModel: PdfViewModel,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }

  val images by viewModel.images.collectAsState()
  val selectedImageIds by viewModel.selectedImageIds.collectAsState()
  val settings by viewModel.settings.collectAsState()
  val conversionState by viewModel.conversionState.collectAsState()
  val rawSizeBytes by viewModel.totalRawSize.collectAsState()
  val estimatedPdfBytes by viewModel.estimatedPdfSize.collectAsState()
  val isReadingImages by viewModel.isReadingImages.collectAsState()

  var showSuccessDialog by remember { mutableStateOf(false) }

  // Listen to conversion state changes
  LaunchedEffect(conversionState) {
    when (val state = conversionState) {
      is ConversionState.Success -> {
        showSuccessDialog = true
        val result = snackbarHostState.showSnackbar(
          message = "PDF saved to ${state.savedPathDisplay}!",
          actionLabel = "OPEN",
          duration = SnackbarDuration.Long
        )
        if (result == SnackbarResult.ActionPerformed) {
          DownloadUtils.openPdf(context, state.contentUri)
        }
      }
      is ConversionState.Error -> {
        snackbarHostState.showSnackbar(
          message = "Error: ${state.message}",
          duration = SnackbarDuration.Long
        )
        viewModel.dismissDialog()
      }
      else -> {}
    }
  }

  // Native Photo Picker launcher (multiple photos selection)
  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickMultipleVisualMedia()
  ) { uris: List<Uri> ->
    if (uris.isNotEmpty()) {
      viewModel.addImages(context, uris)
    }
  }

  // Create document launcher for Save As (custom folder)
  val createDocumentLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/pdf")
  ) { destinationUri: Uri? ->
    if (destinationUri != null) {
      viewModel.saveToDestination(
        context = context,
        destinationUri = destinationUri,
        onSuccess = {
          scope.launch {
            snackbarHostState.showSnackbar("PDF successfully saved to selected folder!")
          }
        },
        onError = { msg ->
          scope.launch {
            snackbarHostState.showSnackbar("Failed to save: $msg")
          }
        }
      )
    }
  }

  // Share action helper
  val sharePdf: (Uri) -> Unit = { contentUri ->
    val success = DownloadUtils.sharePdf(context, contentUri)
    if (!success) {
      scope.launch {
        snackbarHostState.showSnackbar("Unable to open share sheet")
      }
    }
  }

  // View action helper
  val viewPdf: (Uri) -> Unit = { contentUri ->
    val success = DownloadUtils.openPdf(context, contentUri)
    if (!success) {
      scope.launch {
        snackbarHostState.showSnackbar("No PDF viewer app found. PDF is saved to Downloads folder.")
      }
    }
  }

  Scaffold(
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      TopAppBar(
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(PdfRed),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.PictureAsPdf,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(24.dp)
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "Image to PDF Studio",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Desktop-Grade Tablet Document Suite",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        },
        actions = {
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            modifier = Modifier.padding(end = 12.dp)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.Tablet,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "iPadOS & DeX Optimized",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
              )
            }
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface,
        ),
      )
    },
    modifier = modifier.fillMaxSize()
  ) { paddingValues ->
    BoxWithConstraints(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
    ) {
      val screenWidth = maxWidth
      val isWideScreen = screenWidth >= 720.dp

      if (isWideScreen) {
        // High-End Tablet Dashboard: True 52/48 Dual-Pane Master-Detail Split
        Row(
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 14.dp),
          horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
          // Left Pane: Pages & Layout Manager (52% width)
          Box(
            modifier = Modifier
              .weight(1.15f)
              .fillMaxHeight()
          ) {
            PagesLayoutManagerPane(
              images = images,
              selectedIds = selectedImageIds,
              isReadingImages = isReadingImages,
              onAddImagesClick = {
                photoPickerLauncher.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
              },
              onToggleSelect = { id -> viewModel.toggleSelectImage(id) },
              onSelectAll = { viewModel.selectAll() },
              onDeselectAll = { viewModel.deselectAll() },
              onDeleteSelected = { viewModel.deleteSelected() },
              onReverseOrder = { viewModel.reverseOrder() },
              onRotateImageCw = { id -> viewModel.rotateImage(id, clockwise = true) },
              onRotateImageCcw = { id -> viewModel.rotateImage(id, clockwise = false) },
              onRotateSelected = { viewModel.rotateSelected(clockwise = true) },
              onSortByName = { ascending -> viewModel.sortByName(ascending) },
              onRemoveImage = { id -> viewModel.removeImage(id) },
              onMoveUp = { idx -> viewModel.moveUp(idx) },
              onMoveDown = { idx -> viewModel.moveDown(idx) },
              onClearAll = { viewModel.clearAll() },
              gridColumns = if (screenWidth > 1100.dp) 3 else 2
            )
          }

          // Refined divider separator
          Box(
            modifier = Modifier
              .width(1.dp)
              .fillMaxHeight()
              .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
          )

          // Right Pane: Command & Export Center (48% width)
          Column(
            modifier = Modifier
              .weight(1.0f)
              .fillMaxHeight()
              .verticalScroll(rememberScrollState())
              .padding(bottom = 24.dp)
          ) {
            // Header Tag for Command Center
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Command & Export Center",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
              ) {
                Text(
                  text = "Live Dashboard",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.onSecondaryContainer,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
              }
            }

            when (val state = conversionState) {
              is ConversionState.Success -> {
                PdfSuccessCard(
                  result = state.result,
                  savedPathDisplay = state.savedPathDisplay,
                  autoDownloaded = state.autoDownloaded,
                  onDownloadToDownloads = {
                    viewModel.downloadToDownloads(
                      context = context,
                      onSuccess = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } },
                      onError = { err -> scope.launch { snackbarHostState.showSnackbar(err) } }
                    )
                  },
                  onSaveToDevice = {
                    createDocumentLauncher.launch(state.result.fileName)
                  },
                  onSharePdf = {
                    sharePdf(state.contentUri)
                  },
                  onViewPdf = {
                    viewPdf(state.contentUri)
                  },
                  onReset = {
                    viewModel.resetSuccess()
                  }
                )
                Spacer(modifier = Modifier.height(20.dp))
              }
              else -> {}
            }

            SettingsPanel(
              settings = settings,
              onSettingsChanged = { updater -> viewModel.updateSettings(updater) },
              imageCount = images.size,
              rawSizeBytes = rawSizeBytes,
              estimatedPdfBytes = estimatedPdfBytes,
              onConvertClick = {
                viewModel.startConversion(context)
              },
              isConverting = conversionState is ConversionState.Processing
            )
          }
        }
      } else {
        // Compact Phone / Narrow Split View: Tabbed Architecture
        var selectedTab by remember { mutableIntStateOf(0) }
        Column(modifier = Modifier.fillMaxSize()) {
          PrimaryTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface
          ) {
            Tab(
              selected = selectedTab == 0,
              onClick = { selectedTab = 0 },
              text = { Text("Pages (${images.size})", fontWeight = FontWeight.Bold) },
              icon = { Icon(Icons.Default.Collections, contentDescription = null) }
            )
            Tab(
              selected = selectedTab == 1,
              onClick = { selectedTab = 1 },
              text = { Text("Command & Export", fontWeight = FontWeight.Bold) },
              icon = { Icon(Icons.Default.Settings, contentDescription = null) }
            )
          }

          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(16.dp)
          ) {
            if (selectedTab == 0) {
              PagesLayoutManagerPane(
                images = images,
                selectedIds = selectedImageIds,
                isReadingImages = isReadingImages,
                onAddImagesClick = {
                  photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                  )
                },
                onToggleSelect = { id -> viewModel.toggleSelectImage(id) },
                onSelectAll = { viewModel.selectAll() },
                onDeselectAll = { viewModel.deselectAll() },
                onDeleteSelected = { viewModel.deleteSelected() },
                onReverseOrder = { viewModel.reverseOrder() },
                onRotateImageCw = { id -> viewModel.rotateImage(id, clockwise = true) },
                onRotateImageCcw = { id -> viewModel.rotateImage(id, clockwise = false) },
                onRotateSelected = { viewModel.rotateSelected(clockwise = true) },
                onSortByName = { ascending -> viewModel.sortByName(ascending) },
                onRemoveImage = { id -> viewModel.removeImage(id) },
                onMoveUp = { idx -> viewModel.moveUp(idx) },
                onMoveDown = { idx -> viewModel.moveDown(idx) },
                onClearAll = { viewModel.clearAll() },
                gridColumns = 2
              )
            } else {
              Column(
                modifier = Modifier
                  .fillMaxSize()
                  .verticalScroll(rememberScrollState())
              ) {
                when (val state = conversionState) {
                  is ConversionState.Success -> {
                    PdfSuccessCard(
                      result = state.result,
                      savedPathDisplay = state.savedPathDisplay,
                      autoDownloaded = state.autoDownloaded,
                      onDownloadToDownloads = {
                        viewModel.downloadToDownloads(
                          context = context,
                          onSuccess = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } },
                          onError = { err -> scope.launch { snackbarHostState.showSnackbar(err) } }
                        )
                      },
                      onSaveToDevice = {
                        createDocumentLauncher.launch(state.result.fileName)
                      },
                      onSharePdf = {
                        sharePdf(state.contentUri)
                      },
                      onViewPdf = {
                        viewPdf(state.contentUri)
                      },
                      onReset = {
                        viewModel.resetSuccess()
                      }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                  }
                  else -> {}
                }

                SettingsPanel(
                  settings = settings,
                  onSettingsChanged = { updater -> viewModel.updateSettings(updater) },
                  imageCount = images.size,
                  rawSizeBytes = rawSizeBytes,
                  estimatedPdfBytes = estimatedPdfBytes,
                  onConvertClick = {
                    viewModel.startConversion(context)
                  },
                  isConverting = conversionState is ConversionState.Processing
                )
              }
            }
          }
        }
      }
    }

    // Modal Success Dialog when conversion completes
    if (showSuccessDialog && conversionState is ConversionState.Success) {
      val successState = conversionState as ConversionState.Success
      DownloadSuccessDialog(
        result = successState.result,
        savedPathDisplay = successState.savedPathDisplay,
        onOpenPdf = {
          viewPdf(successState.contentUri)
        },
        onSharePdf = {
          sharePdf(successState.contentUri)
        },
        onDownloadAgain = {
          viewModel.downloadToDownloads(
            context = context,
            onSuccess = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } },
            onError = { err -> scope.launch { snackbarHostState.showSnackbar(err) } }
          )
        },
        onSaveAs = {
          createDocumentLauncher.launch(successState.result.fileName)
        },
        onDismiss = {
          showSuccessDialog = false
        }
      )
    }

    // Progress Dialog during generation
    when (val state = conversionState) {
      is ConversionState.Processing -> {
        ConversionProgressDialog(
          current = state.current,
          total = state.total,
          status = state.status
        )
      }
      else -> {}
    }
  }
}

@Composable
private fun PagesLayoutManagerPane(
  images: List<ImageItem>,
  selectedIds: Set<String>,
  isReadingImages: Boolean,
  onAddImagesClick: () -> Unit,
  onToggleSelect: (String) -> Unit,
  onSelectAll: () -> Unit,
  onDeselectAll: () -> Unit,
  onDeleteSelected: () -> Unit,
  onReverseOrder: () -> Unit,
  onRotateImageCw: (String) -> Unit,
  onRotateImageCcw: (String) -> Unit,
  onRotateSelected: () -> Unit,
  onSortByName: (Boolean) -> Unit,
  onRemoveImage: (String) -> Unit,
  onMoveUp: (Int) -> Unit,
  onMoveDown: (Int) -> Unit,
  onClearAll: () -> Unit,
  gridColumns: Int,
  modifier: Modifier = Modifier,
) {
  var showSortMenu by remember { mutableStateOf(false) }

  Column(modifier = modifier.fillMaxSize()) {
    // Top Bar: Title & Primary Management Actions
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(
          text = "Pages & Layout Manager",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = if (images.isNotEmpty()) "${images.size} pages • ${ImageItem.formatBytes(images.sumOf { it.sizeBytes })} raw" else "No pages added yet",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (images.isNotEmpty()) {
          // Sort Menu Trigger
          Box {
            FilledTonalButton(
              onClick = { showSortMenu = true },
              shape = RoundedCornerShape(12.dp),
            ) {
              Icon(Icons.Default.SortByAlpha, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Sort")
            }

            DropdownMenu(
              expanded = showSortMenu,
              onDismissRequest = { showSortMenu = false }
            ) {
              DropdownMenuItem(
                text = { Text("Sort by Name (A-Z)") },
                onClick = {
                  onSortByName(true)
                  showSortMenu = false
                }
              )
              DropdownMenuItem(
                text = { Text("Sort by Name (Z-A)") },
                onClick = {
                  onSortByName(false)
                  showSortMenu = false
                }
              )
              DropdownMenuItem(
                text = { Text("Reverse Page Order") },
                onClick = {
                  onReverseOrder()
                  showSortMenu = false
                }
              )
            }
          }

          FilledTonalButton(
            onClick = onClearAll,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.testTag("clear_all_images_button")
          ) {
            Icon(Icons.Default.ClearAll, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Clear")
          }
        }

        Button(
          onClick = onAddImagesClick,
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
          modifier = Modifier.testTag("add_images_button")
        ) {
          Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Add Images", fontWeight = FontWeight.Bold)
        }
      }
    }

    // Grid or Empty State
    Box(modifier = Modifier.weight(1f)) {
      if (images.isEmpty()) {
        EmptyImagesPlaceholder(
          onAddImagesClick = onAddImagesClick,
          modifier = Modifier.fillMaxSize()
        )
      } else {
        LazyVerticalGrid(
          columns = GridCells.Fixed(gridColumns),
          contentPadding = PaddingValues(bottom = 80.dp),
          horizontalArrangement = Arrangement.spacedBy(14.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp),
          modifier = Modifier
            .fillMaxSize()
            .testTag("images_grid")
        ) {
          itemsIndexed(
            items = images,
            key = { _, item -> item.id }
          ) { index, item ->
            ImageGridItem(
              item = item,
              index = index,
              totalCount = images.size,
              isSelected = item.id in selectedIds,
              onToggleSelect = { onToggleSelect(item.id) },
              onRotateClockwise = { onRotateImageCw(item.id) },
              onRotateCounterClockwise = { onRotateImageCcw(item.id) },
              onMoveUp = { onMoveUp(index) },
              onMoveDown = { onMoveDown(index) },
              onRemove = { onRemoveImage(item.id) },
            )
          }
        }
      }

      // Quick Batch Action Bottom Bar
      if (images.isNotEmpty()) {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
          shadowElevation = 8.dp,
          modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(bottom = 8.dp)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            // Select All / Deselect Toggle
            val allSelected = selectedIds.size == images.size && images.isNotEmpty()
            FilledTonalButton(
              onClick = if (allSelected) onDeselectAll else onSelectAll,
              shape = RoundedCornerShape(10.dp)
            ) {
              Icon(
                imageVector = if (allSelected) Icons.Default.Deselect else Icons.Default.SelectAll,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = if (allSelected) "Deselect (${selectedIds.size})" else if (selectedIds.isNotEmpty()) "Select All (${selectedIds.size})" else "Select All",
                style = MaterialTheme.typography.labelMedium
              )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              // Rotate Selected
              FilledTonalButton(
                onClick = onRotateSelected,
                enabled = selectedIds.isNotEmpty(),
                shape = RoundedCornerShape(10.dp)
              ) {
                Icon(Icons.Default.RotateRight, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Rotate 90°", style = MaterialTheme.typography.labelMedium)
              }

              // Reverse Order
              FilledTonalButton(
                onClick = onReverseOrder,
                shape = RoundedCornerShape(10.dp)
              ) {
                Icon(Icons.Default.SwapVert, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reverse", style = MaterialTheme.typography.labelMedium)
              }

              // Delete Selected
              OutlinedButton(
                onClick = onDeleteSelected,
                enabled = selectedIds.isNotEmpty(),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(
                  1.dp,
                  if (selectedIds.isNotEmpty()) PdfRed else MaterialTheme.colorScheme.outlineVariant
                )
              ) {
                Icon(
                  Icons.Default.DeleteSweep,
                  contentDescription = null,
                  tint = if (selectedIds.isNotEmpty()) PdfRed else MaterialTheme.colorScheme.outline,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "Delete (${selectedIds.size})",
                  color = if (selectedIds.isNotEmpty()) PdfRed else MaterialTheme.colorScheme.outline,
                  style = MaterialTheme.typography.labelMedium
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
private fun EmptyImagesPlaceholder(
  onAddImagesClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier,
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(32.dp),
      contentAlignment = Alignment.Center
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Box(
          modifier = Modifier
            .size(96.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.AddPhotoAlternate,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(50.dp)
          )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
          text = "Pages & Layout Manager",
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = "Select JPG or PNG photos to assemble your PDF document. Rotate pages, reorder visually, and fine-tune downscaling in real time.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.fillMaxWidth(0.75f),
          textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
          onClick = onAddImagesClick,
          shape = RoundedCornerShape(16.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
          ),
          modifier = Modifier
            .height(52.dp)
            .testTag("empty_state_add_button")
        ) {
          Icon(
            imageVector = Icons.Default.AddPhotoAlternate,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Choose Images via Photo Picker",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
          )
        }

        Spacer(modifier = Modifier.height(30.dp))

        Row(
          horizontalArrangement = Arrangement.spacedBy(14.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          HighlightPill("Visual Rotation")
          HighlightPill("Multi-Select Batching")
          HighlightPill("Instant Savings Gauge")
        }
      }
    }
  }
}

@Composable
private fun HighlightPill(text: String) {
  Surface(
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        imageVector = Icons.Default.CheckCircle,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(14.dp)
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface
      )
    }
  }
}
