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
import androidx.compose.material.icons.filled.AutoFixHigh
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
import com.example.ui.components.BeforeAfterCompareDialog
import com.example.ui.components.ConversionProgressDialog
import com.example.ui.components.DownloadSuccessDialog
import com.example.ui.components.ExtractedTextDialog
import com.example.ui.components.ImageGridItem
import com.example.ui.components.PdfSuccessCard
import com.example.ui.components.SettingsPanel
import com.example.ui.theme.PdfGreen
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

  val isBatchCleaning by viewModel.isBatchCleaning.collectAsState()
  val batchCleanProgress by viewModel.batchCleanProgress.collectAsState()
  val batchCleanStatusText by viewModel.batchCleanStatusText.collectAsState()

  var showSuccessDialog by remember { mutableStateOf(false) }
  var viewingOcrItem by remember { mutableStateOf<Pair<ImageItem, Int>?>(null) }
  var comparingImageItem by remember { mutableStateOf<Pair<ImageItem, Int>?>(null) }

  LaunchedEffect(Unit) {
    viewModel.initApiKey(context)
  }

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
          message = state.message,
          duration = SnackbarDuration.Long
        )
      }
      else -> {}
    }
  }

  // System Photo Picker launcher
  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickMultipleVisualMedia()
  ) { uris ->
    if (uris.isNotEmpty()) {
      viewModel.addImages(context, uris)
    }
  }

  // System Document Creation Launcher
  val createDocumentLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/pdf")
  ) { uri ->
    if (uri != null) {
      viewModel.saveToDestination(
        context = context,
        destinationUri = uri,
        onSuccess = {
          scope.launch {
            snackbarHostState.showSnackbar("PDF successfully saved to selected folder!")
          }
        },
        onError = { err: String ->
          scope.launch {
            snackbarHostState.showSnackbar("Failed to save: $err")
          }
        }
      )
    }
  }

  fun sharePdf(uri: Uri) {
    DownloadUtils.sharePdf(context, uri)
  }

  fun viewPdf(uri: Uri) {
    DownloadUtils.openPdf(context, uri)
  }

  Scaffold(
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      TopAppBar(
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
                  imageVector = Icons.Default.PictureAsPdf,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(22.dp)
                )
              }
            }

            Column {
              Text(
                text = "Image to PDF Studio",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Enterprise Document Suite",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        },
        actions = {
          if (images.isNotEmpty()) {
            Surface(
              shape = CircleShape,
              color = MaterialTheme.colorScheme.surfaceVariant,
              modifier = Modifier.padding(end = 8.dp)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Tablet,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(14.dp)
                )
                Text(
                  text = "${images.size} ${if (images.size == 1) "Page" else "Pages"}",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
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
              onRotateImageCw = { id -> viewModel.rotateImage(context, id, clockwise = true) },
              onRotateImageCcw = { id -> viewModel.rotateImage(context, id, clockwise = false) },
              onRotateSelected = { viewModel.rotateSelected(context, clockwise = true) },
              onSortByName = { ascending -> viewModel.sortByName(ascending) },
              onSortByPageNumber = {
                viewModel.sortByPageNumber(context) { msg ->
                  scope.launch { snackbarHostState.showSnackbar(msg) }
                }
              },
              onRemoveImage = { id -> viewModel.removeImage(id) },
              onMoveUp = { idx -> viewModel.moveUp(idx) },
              onMoveDown = { idx -> viewModel.moveDown(idx) },
              onClearAll = { viewModel.clearAll() },
              onViewExtractedText = { item, index -> viewingOcrItem = item to index },
              onToggleEnhance = { id -> viewModel.toggleImageAiEnhancement(context, id) },
              onCompareBeforeAfter = { item, index -> comparingImageItem = item to index },
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
                      onSuccess = { msg: String -> scope.launch { snackbarHostState.showSnackbar(msg) } },
                      onError = { err: String -> scope.launch { snackbarHostState.showSnackbar(err) } }
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
              isConverting = conversionState is ConversionState.Processing,
              isBatchCleaning = isBatchCleaning,
              batchCleanProgress = batchCleanProgress,
              batchCleanStatusText = batchCleanStatusText,
              onBatchCleanAllPages = { viewModel.batchCleanAllPages(context) },
              onSaveGeminiApiKey = { key -> viewModel.saveGeminiApiKey(context, key) }
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
                onRotateImageCw = { id -> viewModel.rotateImage(context, id, clockwise = true) },
                onRotateImageCcw = { id -> viewModel.rotateImage(context, id, clockwise = false) },
                onRotateSelected = { viewModel.rotateSelected(context, clockwise = true) },
                onSortByName = { ascending -> viewModel.sortByName(ascending) },
                onSortByPageNumber = {
                  viewModel.sortByPageNumber(context) { msg ->
                    scope.launch { snackbarHostState.showSnackbar(msg) }
                  }
                },
                onRemoveImage = { id -> viewModel.removeImage(id) },
                onMoveUp = { idx -> viewModel.moveUp(idx) },
                onMoveDown = { idx -> viewModel.moveDown(idx) },
                onClearAll = { viewModel.clearAll() },
                onViewExtractedText = { item, index -> viewingOcrItem = item to index },
                onToggleEnhance = { id -> viewModel.toggleImageAiEnhancement(context, id) },
                onCompareBeforeAfter = { item, index -> comparingImageItem = item to index },
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
                          onSuccess = { msg: String -> scope.launch { snackbarHostState.showSnackbar(msg) } },
                          onError = { err: String -> scope.launch { snackbarHostState.showSnackbar(err) } }
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
                  isConverting = conversionState is ConversionState.Processing,
                  isBatchCleaning = isBatchCleaning,
                  batchCleanProgress = batchCleanProgress,
                  batchCleanStatusText = batchCleanStatusText,
                  onBatchCleanAllPages = { viewModel.batchCleanAllPages(context) },
                  onSaveGeminiApiKey = { key -> viewModel.saveGeminiApiKey(context, key) }
                )
              }
            }
          }
        }
      }
    }

    // Modal OCR Extracted Text Dialog
    viewingOcrItem?.let { (item, index) ->
      ExtractedTextDialog(
        item = item,
        pageIndex = index,
        onDismiss = { viewingOcrItem = null }
      )
    }

    // Modal Before/After Studio Comparison Inspector Dialog
    comparingImageItem?.let { (item, index) ->
      val currentItem = images.find { it.id == item.id } ?: item
      BeforeAfterCompareDialog(
        item = currentItem,
        pageIndex = index,
        onDismiss = { comparingImageItem = null },
        onRevertToOriginal = { viewModel.revertImageToOriginal(currentItem.id) },
        onApplyEnhance = { viewModel.enhanceImage(context, currentItem.id) }
      )
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
            onSuccess = { msg: String -> scope.launch { snackbarHostState.showSnackbar(msg) } },
            onError = { err: String -> scope.launch { snackbarHostState.showSnackbar(err) } }
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

    // Conversion In-Progress Dialog
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
  onSortByPageNumber: () -> Unit = {},
  onRemoveImage: (String) -> Unit,
  onMoveUp: (Int) -> Unit,
  onMoveDown: (Int) -> Unit,
  onClearAll: () -> Unit,
  onViewExtractedText: (ImageItem, Int) -> Unit,
  onToggleEnhance: (String) -> Unit = {},
  onCompareBeforeAfter: (ImageItem, Int) -> Unit = { _, _ -> },
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
          text = if (images.isEmpty()) "No pages added yet" else "${images.size} ${if (images.size == 1) "image" else "images"} queued",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (images.isNotEmpty()) {
          Box {
            FilledTonalButton(
              onClick = { showSortMenu = true },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.testTag("sort_menu_button")
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
                text = { Text("Auto-Sort by Page Number (OCR)", fontWeight = FontWeight.Bold) },
                leadingIcon = { Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                onClick = {
                  onSortByPageNumber()
                  showSortMenu = false
                }
              )
              DropdownMenuItem(
                text = { Text("Sort by Name (A → Z)") },
                onClick = {
                  onSortByName(true)
                  showSortMenu = false
                }
              )
              DropdownMenuItem(
                text = { Text("Sort by Name (Z → A)") },
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
              onViewExtractedText = { onViewExtractedText(item, index) },
              onToggleEnhance = { onToggleEnhance(item.id) },
              onCompareBeforeAfter = { onCompareBeforeAfter(item, index) },
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
            .padding(bottom = 12.dp)
            .testTag("quick_action_bar")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            val allSelected = selectedIds.size == images.size && images.isNotEmpty()

            OutlinedButton(
              onClick = if (allSelected) onDeselectAll else onSelectAll,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.testTag("select_all_button")
            ) {
              Icon(
                imageVector = if (allSelected) Icons.Default.Deselect else Icons.Default.SelectAll,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(if (allSelected) "Deselect" else "Select All")
            }

            OutlinedButton(
              onClick = onReverseOrder,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.testTag("reverse_order_button")
            ) {
              Icon(
                imageVector = Icons.Default.SwapVert,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("Reverse")
            }

            if (selectedIds.isNotEmpty()) {
              OutlinedButton(
                onClick = onRotateSelected,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("rotate_selected_button")
              ) {
                Icon(
                  imageVector = Icons.Default.RotateRight,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Rotate (${selectedIds.size})")
              }

              Button(
                onClick = onDeleteSelected,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.error,
                  contentColor = MaterialTheme.colorScheme.onError
                ),
                modifier = Modifier.testTag("delete_selected_button")
              ) {
                Icon(
                  imageVector = Icons.Default.DeleteSweep,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Delete (${selectedIds.size})")
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
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
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
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.primaryContainer,
          modifier = Modifier.size(72.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Default.AddPhotoAlternate,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(36.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
          text = "No Pages Queued",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = "Import document photos, receipts, or screenshots.\nOrganize, rotate, smart-crop status bars, and export to PDF.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(22.dp))

        Button(
          onClick = onAddImagesClick,
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
          modifier = Modifier.height(48.dp)
        ) {
          Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Select Photos & Documents", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}
