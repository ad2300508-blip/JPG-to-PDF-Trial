package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoSizeSelectLarge
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.CompressionMode
import com.example.model.ImageLabItem
import com.example.model.ImageLabSettings
import com.example.model.ImageOutputFormat
import com.example.model.ScaleOption
import com.example.ui.theme.SuccessGreen
import com.example.viewmodel.ImageLabViewModel

@Composable
fun ImageLabScreen(
  viewModel: ImageLabViewModel,
  modifier: Modifier = Modifier
) {
  val items by viewModel.items.collectAsStateWithLifecycle()
  val settings by viewModel.settings.collectAsStateWithLifecycle()
  val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()
  val progressFraction by viewModel.progressFraction.collectAsStateWithLifecycle()
  val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
  val exportMessage by viewModel.exportMessage.collectAsStateWithLifecycle()

  val pickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenMultipleDocuments()
  ) { uris ->
    if (uris.isNotEmpty()) {
      viewModel.addUris(uris)
    }
  }

  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(exportMessage) {
    exportMessage?.let {
      snackbarHostState.showSnackbar(it)
      viewModel.clearExportMessage()
    }
  }

  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val isDualPane = maxWidth >= 760.dp

    if (isDualPane) {
      Row(modifier = Modifier.fillMaxSize()) {
        // Left Pane: Images List
        Box(
          modifier = Modifier
            .weight(1.15f)
            .fillMaxHeight()
            .padding(16.dp)
        ) {
          ImageLabListPane(
            items = items,
            onPickImages = { pickerLauncher.launch(arrayOf("image/*")) },
            onRemoveItem = { viewModel.removeItem(it) },
            onClearAll = { viewModel.clearAll() }
          )
        }

        // Right Pane: Settings & Control Panel
        Box(
          modifier = Modifier
            .weight(0.85f)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(16.dp)
        ) {
          ImageLabControlPanel(
            items = items,
            settings = settings,
            isProcessing = isProcessing,
            onSettingsUpdate = { viewModel.updateSettings(it) },
            onConvertAll = { viewModel.convertAll() },
            onExportAll = { viewModel.exportAllToDownloads() }
          )
        }
      }
    } else {
      // Mobile Single Column
      Column(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        ImageLabControlPanel(
          items = items,
          settings = settings,
          isProcessing = isProcessing,
          onSettingsUpdate = { viewModel.updateSettings(it) },
          onConvertAll = { viewModel.convertAll() },
          onExportAll = { viewModel.exportAllToDownloads() }
        )

        ImageLabListPane(
          items = items,
          onPickImages = { pickerLauncher.launch(arrayOf("image/*")) },
          onRemoveItem = { viewModel.removeItem(it) },
          onClearAll = { viewModel.clearAll() }
        )
      }
    }

    // Processing Dialog
    if (isProcessing) {
      AlertDialog(
        onDismissRequest = { /* Modal during processing */ },
        icon = {
          CircularProgressIndicator(modifier = Modifier.size(36.dp))
        },
        title = { Text("Converting Images...", fontWeight = FontWeight.Bold) },
        text = {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(statusMessage, style = MaterialTheme.typography.bodyMedium)
            LinearProgressIndicator(
              progress = { progressFraction },
              modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
            )
          }
        },
        confirmButton = { }
      )
    }

    Box(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(bottom = 16.dp)
    ) {
      SnackbarHost(hostState = snackbarHostState)
    }
  }
}

@Composable
private fun ImageLabListPane(
  items: List<ImageLabItem>,
  onPickImages: () -> Unit,
  onRemoveItem: (String) -> Unit,
  onClearAll: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(modifier = modifier.fillMaxSize()) {
    // Header
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(
          text = "Selected Images (${items.size})",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold
        )
        if (items.isNotEmpty()) {
          val totalOriginal = items.sumOf { it.originalSizeBytes }
          val totalConverted = items.sumOf { it.convertedSizeBytes }
          val subtitle = if (totalConverted > 0) {
            "Total: ${ImageLabItem.formatBytes(totalOriginal)} ➔ ${ImageLabItem.formatBytes(totalConverted)}"
          } else {
            "Total Original: ${ImageLabItem.formatBytes(totalOriginal)}"
          }
          Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (items.isNotEmpty()) {
          OutlinedButton(
            onClick = onClearAll,
            modifier = Modifier.testTag("image_lab_clear_all")
          ) {
            Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Clear")
          }
        }
        Button(
          onClick = onPickImages,
          modifier = Modifier.testTag("image_lab_pick_images")
        ) {
          Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(if (items.isEmpty()) "Select Images" else "Add More")
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    if (items.isEmpty()) {
      // Empty State
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp)
      ) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
          verticalArrangement = Arrangement.Center,
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Box(
            modifier = Modifier
              .size(80.dp)
              .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Image,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(44.dp)
            )
          }

          Spacer(modifier = Modifier.height(20.dp))

          Text(
            text = "Batch Image Transcoder",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
          )

          Spacer(modifier = Modifier.height(8.dp))

          Text(
            text = "Convert multiple photos between JPEG, PNG, and WebP.\nToggle lossy compression or 100% mathematical lossless preservation.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
          )

          Spacer(modifier = Modifier.height(24.dp))

          Button(
            onClick = onPickImages,
            modifier = Modifier.testTag("empty_state_pick_images_button")
          ) {
            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Choose Images to Convert")
          }
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        items(items, key = { it.id }) { item ->
          ImageLabItemCard(
            item = item,
            onRemove = { onRemoveItem(item.id) }
          )
        }
      }
    }
  }
}

@Composable
private fun ImageLabItemCard(
  item: ImageLabItem,
  onRemove: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("image_lab_item_${item.id}"),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(16.dp),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Thumbnail
      val context = LocalContext.current
      Box(
        modifier = Modifier
          .size(68.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(MaterialTheme.colorScheme.surfaceVariant)
      ) {
        AsyncImage(
          model = ImageRequest.Builder(context)
            .data(item.convertedFile ?: item.uri)
            .crossfade(true)
            .build(),
          contentDescription = item.fileName,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize()
        )
      }

      Spacer(modifier = Modifier.width(14.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = item.fileName,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Dimension and Original Size
        Text(
          text = if (item.originalWidth > 0) "${item.originalWidth}x${item.originalHeight} px • ${item.displayOriginalSize}" else item.displayOriginalSize,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Converted Result Badge
        if (item.convertedSizeBytes > 0) {
          Spacer(modifier = Modifier.height(6.dp))
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = SuccessGreen.copy(alpha = 0.15f),
              modifier = Modifier.border(1.dp, SuccessGreen.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Default.CheckCircle,
                  contentDescription = null,
                  tint = SuccessGreen,
                  modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "${item.displayConvertedSize} (${if (item.savingsPercentage >= 0) "-${item.savingsPercentage}%" else "+${-item.savingsPercentage}%"})",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = SuccessGreen
                )
              }
            }

            if (item.convertedWidth > 0) {
              Text(
                text = "${item.convertedWidth}x${item.convertedHeight} px",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }

        if (item.error != null) {
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Error: ${item.error}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error
          )
        }
      }

      IconButton(
        onClick = onRemove,
        modifier = Modifier.testTag("remove_image_lab_item_${item.id}")
      ) {
        Icon(
          imageVector = Icons.Default.Close,
          contentDescription = "Remove",
          tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}

@Composable
private fun ImageLabControlPanel(
  items: List<ImageLabItem>,
  settings: ImageLabSettings,
  isProcessing: Boolean,
  onSettingsUpdate: ((ImageLabSettings) -> ImageLabSettings) -> Unit,
  onConvertAll: () -> Unit,
  onExportAll: () -> Unit
) {
  val hasConverted = items.any { it.convertedFile != null }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Text(
      text = "Conversion Settings",
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.Bold
    )

    // Mode Selector: Lossy vs Lossless
    TabRow(
      selectedTabIndex = if (settings.mode == CompressionMode.LOSSY) 0 else 1,
      containerColor = MaterialTheme.colorScheme.surface,
      modifier = Modifier.clip(RoundedCornerShape(12.dp))
    ) {
      Tab(
        selected = settings.mode == CompressionMode.LOSSY,
        onClick = { onSettingsUpdate { it.copy(mode = CompressionMode.LOSSY) } },
        text = { Text("Lossy Compression", fontWeight = FontWeight.SemiBold) }
      )
      Tab(
        selected = settings.mode == CompressionMode.LOSSLESS,
        onClick = { onSettingsUpdate { it.copy(mode = CompressionMode.LOSSLESS) } },
        text = { Text("Lossless (100%)", fontWeight = FontWeight.SemiBold) }
      )
    }

    // Format Selector
    Card(
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      shape = RoundedCornerShape(16.dp)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = "Target Format",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (settings.mode == CompressionMode.LOSSY) {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
              selected = settings.lossyFormat == ImageOutputFormat.JPEG,
              onClick = { onSettingsUpdate { it.copy(lossyFormat = ImageOutputFormat.JPEG) } },
              label = { Text("JPEG (.jpg)") }
            )
            FilterChip(
              selected = settings.lossyFormat == ImageOutputFormat.WEBP_LOSSY,
              onClick = { onSettingsUpdate { it.copy(lossyFormat = ImageOutputFormat.WEBP_LOSSY) } },
              label = { Text("WEBP (.webp)") }
            )
          }
        } else {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
              selected = settings.losslessFormat == ImageOutputFormat.WEBP_LOSSLESS,
              onClick = { onSettingsUpdate { it.copy(losslessFormat = ImageOutputFormat.WEBP_LOSSLESS) } },
              label = { Text("WEBP Lossless") }
            )
            FilterChip(
              selected = settings.losslessFormat == ImageOutputFormat.PNG,
              onClick = { onSettingsUpdate { it.copy(losslessFormat = ImageOutputFormat.PNG) } },
              label = { Text("PNG (.png)") }
            )
          }
        }
      }
    }

    // Lossy Specific Settings (Quality & Resolution Scaling)
    if (settings.mode == CompressionMode.LOSSY) {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Quality",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold
            )
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = MaterialTheme.colorScheme.primaryContainer
            ) {
              Text(
                text = "${settings.quality}%",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }

          Slider(
            value = settings.quality.toFloat(),
            onValueChange = { onSettingsUpdate { s -> s.copy(quality = it.toInt()) } },
            valueRange = 1f..100f,
            steps = 99,
            modifier = Modifier.fillMaxWidth().testTag("image_lab_quality_slider")
          )

          HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

          Text(
            text = "Resolution Scaling",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
          )
          Spacer(modifier = Modifier.height(8.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            ScaleOption.values().forEach { scale ->
              FilterChip(
                selected = settings.scaleOption == scale,
                onClick = { onSettingsUpdate { it.copy(scaleOption = scale) } },
                label = { Text(scale.label, fontSize = 12.sp) }
              )
            }
          }

          HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Strip EXIF Metadata",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
              )
              Text(
                text = "Removes camera model, GPS tags, and timestamps for smaller size and privacy",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Switch(
              checked = settings.stripExif,
              onCheckedChange = { onSettingsUpdate { s -> s.copy(stripExif = it) } }
            )
          }
        }
      }
    } else {
      // Lossless explanation card
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp)
      ) {
        Row(
          modifier = Modifier.padding(16.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.HighQuality,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
          )
          Spacer(modifier = Modifier.width(12.dp))
          Text(
            text = "100% Mathematical Fidelity: Bit-for-bit lossless encoding with zero generation loss or compression artifacts.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
          )
        }
      }
    }

    // Action Buttons
    Button(
      onClick = onConvertAll,
      enabled = items.isNotEmpty() && !isProcessing,
      modifier = Modifier
        .fillMaxWidth()
        .height(52.dp)
        .testTag("image_lab_convert_all_button"),
      shape = RoundedCornerShape(14.dp)
    ) {
      Icon(Icons.Default.PlayArrow, contentDescription = null)
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = if (items.isEmpty()) "Add Images to Convert" else "Convert All (${items.size} Images)",
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp
      )
    }

    if (hasConverted) {
      Button(
        onClick = onExportAll,
        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .testTag("image_lab_export_all_button"),
        shape = RoundedCornerShape(14.dp)
      ) {
        Icon(Icons.Default.Download, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Save All to Downloads",
          fontWeight = FontWeight.Bold,
          color = Color.White
        )
      }
    }
  }
}
