package com.example.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tablet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.ui.components.ImageGridItem
import com.example.ui.components.PdfSuccessCard
import com.example.ui.components.SettingsPanel
import com.example.ui.theme.PdfRed
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
  val settings by viewModel.settings.collectAsState()
  val conversionState by viewModel.conversionState.collectAsState()
  val rawSizeBytes by viewModel.totalRawSize.collectAsState()
  val estimatedPdfBytes by viewModel.estimatedPdfSize.collectAsState()
  val isReadingImages by viewModel.isReadingImages.collectAsState()

  // Native Photo Picker launcher (multiple photos selection)
  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickMultipleVisualMedia()
  ) { uris: List<Uri> ->
    if (uris.isNotEmpty()) {
      viewModel.addImages(context, uris)
    }
  }

  // Create document launcher for Save to Device
  val createDocumentLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/pdf")
  ) { destinationUri: Uri? ->
    if (destinationUri != null) {
      viewModel.saveToDestination(
        context = context,
        destinationUri = destinationUri,
        onSuccess = {
          scope.launch {
            snackbarHostState.showSnackbar("PDF successfully saved to device!")
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
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
      type = "application/pdf"
      putExtra(Intent.EXTRA_STREAM, contentUri)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(sendIntent, "Share Generated PDF"))
  }

  // View action helper
  val viewPdf: (Uri) -> Unit = { contentUri ->
    try {
      val viewIntent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(contentUri, "application/pdf")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
      context.startActivity(Intent.createChooser(viewIntent, "Open PDF"))
    } catch (_: Exception) {
      scope.launch {
        snackbarHostState.showSnackbar("No PDF viewer app found on device")
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
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(PdfRed),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.PictureAsPdf,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(22.dp)
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "Image to PDF",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Native Converter & Compressor",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        },
        actions = {
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
            modifier = Modifier.padding(end = 12.dp)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.Tablet,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Tablet Optimized",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
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
        // Tablet Large Screen Layout: Side-by-side Two-Pane
        Row(
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
          horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
          // Left Pane: Image selection, grid preview, reordering, photo picker (56% width)
          Box(
            modifier = Modifier
              .weight(1.25f)
              .fillMaxHeight()
          ) {
            LeftImagePane(
              images = images,
              isReadingImages = isReadingImages,
              onAddImagesClick = {
                photoPickerLauncher.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
              },
              onRemoveImage = { id -> viewModel.removeImage(id) },
              onMoveUp = { idx -> viewModel.moveUp(idx) },
              onMoveDown = { idx -> viewModel.moveDown(idx) },
              onClearAll = { viewModel.clearAll() },
              gridColumns = if (screenWidth > 1050.dp) 3 else 2
            )
          }

          // Vertical divider line
          Box(
            modifier = Modifier
              .width(1.dp)
              .fillMaxHeight()
              .background(MaterialTheme.colorScheme.outlineVariant)
          )

          // Right Pane: Settings, Live Estimator, Convert & Export actions (44% width)
          Column(
            modifier = Modifier
              .weight(1.0f)
              .fillMaxHeight()
              .verticalScroll(rememberScrollState())
              .padding(bottom = 24.dp)
          ) {
            when (val state = conversionState) {
              is ConversionState.Success -> {
                PdfSuccessCard(
                  result = state.result,
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
        // Compact Phone / Split view: Tabbed Layout
        var selectedTab by remember { mutableIntStateOf(0) }
        Column(modifier = Modifier.fillMaxSize()) {
          PrimaryTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface
          ) {
            Tab(
              selected = selectedTab == 0,
              onClick = { selectedTab = 0 },
              text = { Text("Images (${images.size})") },
              icon = { Icon(Icons.Default.Collections, contentDescription = null) }
            )
            Tab(
              selected = selectedTab == 1,
              onClick = { selectedTab = 1 },
              text = { Text("Settings & Export") },
              icon = { Icon(Icons.Default.Settings, contentDescription = null) }
            )
          }

          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(16.dp)
          ) {
            if (selectedTab == 0) {
              LeftImagePane(
                images = images,
                isReadingImages = isReadingImages,
                onAddImagesClick = {
                  photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                  )
                },
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

    // Progress Dialog during generation
    when (val state = conversionState) {
      is ConversionState.Processing -> {
        ConversionProgressDialog(
          current = state.current,
          total = state.total,
          status = state.status
        )
      }
      is ConversionState.Error -> {
        scope.launch {
          snackbarHostState.showSnackbar("Error: ${state.message}")
          viewModel.dismissDialog()
        }
      }
      else -> {}
    }
  }
}

@Composable
private fun LeftImagePane(
  images: List<ImageItem>,
  isReadingImages: Boolean,
  onAddImagesClick: () -> Unit,
  onRemoveImage: (String) -> Unit,
  onMoveUp: (Int) -> Unit,
  onMoveDown: (Int) -> Unit,
  onClearAll: () -> Unit,
  gridColumns: Int,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxSize()) {
    // Toolbar Header: Image Count & Action Buttons
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(
          text = "Selected Images (${images.size})",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
        if (images.isNotEmpty()) {
          Text(
            text = "Total raw: ${ImageItem.formatBytes(images.sumOf { it.sizeBytes })}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (images.isNotEmpty()) {
          FilledTonalButton(
            onClick = onClearAll,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.testTag("clear_all_images_button")
          ) {
            Icon(
              imageVector = Icons.Default.ClearAll,
              contentDescription = null,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Clear All")
          }
        }

        Button(
          onClick = onAddImagesClick,
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
          ),
          modifier = Modifier.testTag("add_images_button")
        ) {
          Icon(
            imageVector = Icons.Default.AddPhotoAlternate,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text("Add Images", fontWeight = FontWeight.Bold)
        }
      }
    }

    if (images.isEmpty()) {
      // Empty State
      EmptyImagesPlaceholder(
        onAddImagesClick = onAddImagesClick,
        modifier = Modifier.fillMaxSize()
      )
    } else {
      // Images Grid
      LazyVerticalGrid(
        columns = GridCells.Fixed(gridColumns),
        contentPadding = PaddingValues(bottom = 24.dp),
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
            onMoveUp = { onMoveUp(index) },
            onMoveDown = { onMoveDown(index) },
            onRemove = { onRemoveImage(item.id) },
          )
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
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    )
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
            modifier = Modifier.size(52.dp)
          )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
          text = "No Images Added Yet",
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = "Choose JPG or PNG photos from your gallery to assemble into a crisp, compressed PDF document.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.fillMaxWidth(0.7f),
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
            .height(50.dp)
            .testTag("empty_state_add_button")
        ) {
          Icon(
            imageVector = Icons.Default.AddPhotoAlternate,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Select Images via Photo Picker",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
          )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Feature highlight chips
        Row(
          horizontalArrangement = Arrangement.spacedBy(16.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          HighlightPill("EXIF Auto-Rotate")
          HighlightPill("Smart Compression")
          HighlightPill("A4 & Fit Layouts")
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
