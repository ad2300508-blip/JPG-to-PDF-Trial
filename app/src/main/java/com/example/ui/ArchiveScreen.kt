package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.ArchiveFileItem
import com.example.model.ArchiveResult
import com.example.model.ArchiveSettings
import com.example.model.ZipCompressionLevel
import com.example.ui.theme.SuccessGreen
import com.example.util.DownloadUtils
import com.example.viewmodel.ArchiveViewModel

@Composable
fun ArchiveScreen(
  viewModel: ArchiveViewModel,
  modifier: Modifier = Modifier
) {
  val items by viewModel.items.collectAsStateWithLifecycle()
  val settings by viewModel.settings.collectAsStateWithLifecycle()
  val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()
  val progressFraction by viewModel.progressFraction.collectAsStateWithLifecycle()
  val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
  val lastResult by viewModel.lastResult.collectAsStateWithLifecycle()
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
        // Left Pane: File list
        Box(
          modifier = Modifier
            .weight(1.15f)
            .fillMaxHeight()
            .padding(16.dp)
        ) {
          ArchiveListPane(
            items = items,
            onPickFiles = { pickerLauncher.launch(arrayOf("*/*")) },
            onRemoveItem = { viewModel.removeItem(it) },
            onClearAll = { viewModel.clearAll() }
          )
        }

        // Right Pane: Settings & Compression Controls
        Box(
          modifier = Modifier
            .weight(0.85f)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(16.dp)
        ) {
          ArchiveControlPanel(
            items = items,
            settings = settings,
            isProcessing = isProcessing,
            lastResult = lastResult,
            onSettingsUpdate = { viewModel.updateSettings(it) },
            onCreateArchive = { viewModel.createArchive() }
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
        ArchiveControlPanel(
          items = items,
          settings = settings,
          isProcessing = isProcessing,
          lastResult = lastResult,
          onSettingsUpdate = { viewModel.updateSettings(it) },
          onCreateArchive = { viewModel.createArchive() }
        )

        ArchiveListPane(
          items = items,
          onPickFiles = { pickerLauncher.launch(arrayOf("*/*")) },
          onRemoveItem = { viewModel.removeItem(it) },
          onClearAll = { viewModel.clearAll() }
        )
      }
    }

    if (isProcessing) {
      AlertDialog(
        onDismissRequest = { },
        icon = { CircularProgressIndicator(modifier = Modifier.size(36.dp)) },
        title = { Text("Creating ZIP Archive...", fontWeight = FontWeight.Bold) },
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
private fun ArchiveListPane(
  items: List<ArchiveFileItem>,
  onPickFiles: () -> Unit,
  onRemoveItem: (String) -> Unit,
  onClearAll: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(modifier = modifier.fillMaxSize()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(
          text = "Archive Queue (${items.size})",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold
        )
        if (items.isNotEmpty()) {
          val totalBytes = items.sumOf { it.sizeBytes }
          Text(
            text = "Total Uncompressed: ${ArchiveFileItem.formatBytes(totalBytes)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (items.isNotEmpty()) {
          OutlinedButton(
            onClick = onClearAll,
            modifier = Modifier.testTag("archive_clear_all")
          ) {
            Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Clear")
          }
        }
        Button(
          onClick = onPickFiles,
          modifier = Modifier.testTag("archive_pick_files")
        ) {
          Icon(Icons.Default.PostAdd, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(if (items.isEmpty()) "Select Files" else "Add Files")
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    if (items.isEmpty()) {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
              imageVector = Icons.Default.FolderZip,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(44.dp)
            )
          }

          Spacer(modifier = Modifier.height(20.dp))

          Text(
            text = "Multi-Format Lossless Archiver",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
          )

          Spacer(modifier = Modifier.height(8.dp))

          Text(
            text = "Bundle images, PDFs, audio, and documents into a clean, portable .ZIP archive with customizable deflate compression levels.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
          )

          Spacer(modifier = Modifier.height(24.dp))

          Button(
            onClick = onPickFiles,
            modifier = Modifier.testTag("empty_state_pick_archive_files_button")
          ) {
            Icon(Icons.Default.PostAdd, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Select Files to Archive")
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
          ArchiveItemCard(
            item = item,
            onRemove = { onRemoveItem(item.id) }
          )
        }
      }
    }
  }
}

@Composable
private fun ArchiveItemCard(
  item: ArchiveFileItem,
  onRemove: () -> Unit
) {
  val icon: ImageVector = when {
    item.mimeType.contains("pdf", ignoreCase = true) -> Icons.Default.PictureAsPdf
    item.mimeType.startsWith("image/", ignoreCase = true) -> Icons.Default.Image
    item.mimeType.startsWith("audio/", ignoreCase = true) -> Icons.Default.Audiotrack
    item.mimeType.contains("zip", ignoreCase = true) -> Icons.Default.FolderZip
    item.mimeType.contains("text", ignoreCase = true) -> Icons.Default.Description
    else -> Icons.AutoMirrored.Filled.InsertDriveFile
  }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("archive_item_${item.id}"),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(16.dp),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(50.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(26.dp)
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

        Text(
          text = "${item.displaySize} • ${item.mimeType}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      IconButton(
        onClick = onRemove,
        modifier = Modifier.testTag("remove_archive_item_${item.id}")
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
private fun ArchiveControlPanel(
  items: List<ArchiveFileItem>,
  settings: ArchiveSettings,
  isProcessing: Boolean,
  lastResult: ArchiveResult?,
  onSettingsUpdate: ((ArchiveSettings) -> ArchiveSettings) -> Unit,
  onCreateArchive: () -> Unit
) {
  val context = LocalContext.current

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Text(
      text = "Archive Settings",
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.Bold
    )

    // Archive Name
    Card(
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      shape = RoundedCornerShape(16.dp)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = "ZIP File Name",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
          value = settings.archiveName,
          onValueChange = { onSettingsUpdate { s -> s.copy(archiveName = it) } },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("archive_name_field"),
          shape = RoundedCornerShape(10.dp)
        )
      }
    }

    // Compression Level Selector
    Card(
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      shape = RoundedCornerShape(16.dp)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = "Deflate Compression Level",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(10.dp))

        ZipCompressionLevel.values().forEach { level ->
          val isSelected = settings.compressionLevel == level
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp)
              .clip(RoundedCornerShape(12.dp))
              .clickable { onSettingsUpdate { it.copy(compressionLevel = level) } }
              .testTag("archive_level_${level.name}")
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Text(
                text = level.title,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = level.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }
    }

    // Creation Button
    Button(
      onClick = onCreateArchive,
      enabled = items.isNotEmpty() && !isProcessing,
      modifier = Modifier
        .fillMaxWidth()
        .height(52.dp)
        .testTag("create_archive_button"),
      shape = RoundedCornerShape(14.dp)
    ) {
      Icon(Icons.Default.Archive, contentDescription = null)
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = if (items.isEmpty()) "Add Files to Compress" else "Create Lossless ZIP (${items.size} Files)",
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp
      )
    }

    // Last Result Card with Open / Share
    if (lastResult != null) {
      Card(
        colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.border(1.5.dp, SuccessGreen.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Archive Created Successfully!",
              fontWeight = FontWeight.Bold,
              color = SuccessGreen
            )
          }

          Spacer(modifier = Modifier.height(8.dp))

          Text(
            text = "${lastResult.fileName}\nUncompressed: ${ArchiveFileItem.formatBytes(lastResult.originalTotalBytes)} ➔ ZIP: ${ArchiveFileItem.formatBytes(lastResult.compressedBytes)} (${if (lastResult.savingsPercentage >= 0) "-${lastResult.savingsPercentage}%" else "+0%"})",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
          )

          Spacer(modifier = Modifier.height(12.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = {
                val fileUri = FileProvider.getUriForFile(
                  context,
                  "${context.packageName}.fileprovider",
                  lastResult.file
                )
                DownloadUtils.openFile(context, fileUri, "application/zip")
              },
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(10.dp)
            ) {
              Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Open ZIP")
            }

            OutlinedButton(
              onClick = {
                val fileUri = FileProvider.getUriForFile(
                  context,
                  "${context.packageName}.fileprovider",
                  lastResult.file
                )
                DownloadUtils.shareFile(context, fileUri, "application/zip", "Share ZIP Archive")
              },
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(10.dp)
            ) {
              Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Share")
            }
          }
        }
      }
    }
  }
}
