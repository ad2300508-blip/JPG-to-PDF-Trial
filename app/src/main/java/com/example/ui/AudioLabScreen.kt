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
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.AudioBitrate
import com.example.model.AudioLabItem
import com.example.model.AudioLabSettings
import com.example.model.AudioOutputMode
import com.example.ui.theme.SuccessGreen
import com.example.viewmodel.AudioLabViewModel

@Composable
fun AudioLabScreen(
  viewModel: AudioLabViewModel,
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
        // Left Pane: Audio Files List
        Box(
          modifier = Modifier
            .weight(1.15f)
            .fillMaxHeight()
            .padding(16.dp)
        ) {
          AudioLabListPane(
            items = items,
            onPickAudio = { pickerLauncher.launch(arrayOf("audio/*")) },
            onRemoveItem = { viewModel.removeItem(it) },
            onClearAll = { viewModel.clearAll() }
          )
        }

        // Right Pane: Settings Command Center
        Box(
          modifier = Modifier
            .weight(0.85f)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(16.dp)
        ) {
          AudioLabControlPanel(
            items = items,
            settings = settings,
            isProcessing = isProcessing,
            onSettingsUpdate = { viewModel.updateSettings(it) },
            onTranscodeAll = { viewModel.transcodeAll() },
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
        AudioLabControlPanel(
          items = items,
          settings = settings,
          isProcessing = isProcessing,
          onSettingsUpdate = { viewModel.updateSettings(it) },
          onTranscodeAll = { viewModel.transcodeAll() },
          onExportAll = { viewModel.exportAllToDownloads() }
        )

        AudioLabListPane(
          items = items,
          onPickAudio = { pickerLauncher.launch(arrayOf("audio/*")) },
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
        title = { Text("Transcoding Audio...", fontWeight = FontWeight.Bold) },
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
private fun AudioLabListPane(
  items: List<AudioLabItem>,
  onPickAudio: () -> Unit,
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
          text = "Audio Queue (${items.size})",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold
        )
        if (items.isNotEmpty()) {
          val totalOriginal = items.sumOf { it.sizeBytes }
          val totalConverted = items.sumOf { it.convertedSizeBytes }
          val subtitle = if (totalConverted > 0) {
            "Total: ${AudioLabItem.formatBytes(totalOriginal)} ➔ ${AudioLabItem.formatBytes(totalConverted)}"
          } else {
            "Total: ${AudioLabItem.formatBytes(totalOriginal)}"
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
            modifier = Modifier.testTag("audio_lab_clear_all")
          ) {
            Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Clear")
          }
        }
        Button(
          onClick = onPickAudio,
          modifier = Modifier.testTag("audio_lab_pick_audio")
        ) {
          Icon(Icons.Default.LibraryMusic, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(if (items.isEmpty()) "Select Audio" else "Add Audio")
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
              imageVector = Icons.Default.Audiotrack,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(44.dp)
            )
          }

          Spacer(modifier = Modifier.height(20.dp))

          Text(
            text = "Native Audio Transcoder",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
          )

          Spacer(modifier = Modifier.height(8.dp))

          Text(
            text = "Batch compress voice recordings and music without quality loss.\nPowered 100% natively by Android MediaCodec hardware.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
          )

          Spacer(modifier = Modifier.height(24.dp))

          Button(
            onClick = onPickAudio,
            modifier = Modifier.testTag("empty_state_pick_audio_button")
          ) {
            Icon(Icons.Default.LibraryMusic, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Select Audio Files")
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
          AudioLabItemCard(
            item = item,
            onRemove = { onRemoveItem(item.id) }
          )
        }
      }
    }
  }
}

@Composable
private fun AudioLabItemCard(
  item: AudioLabItem,
  onRemove: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("audio_lab_item_${item.id}"),
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
          .size(54.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.GraphicEq,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(28.dp)
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

        val channelLabel = if (item.channels == 1) "Mono" else "Stereo"
        val sampleRateLabel = "${item.sampleRate / 1000.0} kHz"
        Text(
          text = "${item.displayDuration} • $sampleRateLabel • $channelLabel • ${item.displayOriginalSize}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (item.convertedSizeBytes > 0) {
          Spacer(modifier = Modifier.height(6.dp))
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
        modifier = Modifier.testTag("remove_audio_lab_item_${item.id}")
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
private fun AudioLabControlPanel(
  items: List<AudioLabItem>,
  settings: AudioLabSettings,
  isProcessing: Boolean,
  onSettingsUpdate: ((AudioLabSettings) -> AudioLabSettings) -> Unit,
  onTranscodeAll: () -> Unit,
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
      text = "Audio Settings",
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.Bold
    )

    // Mode Selector: M4A/AAC vs Lossless WAV
    TabRow(
      selectedTabIndex = if (settings.mode == AudioOutputMode.LOSSY_M4A_AAC) 0 else 1,
      containerColor = MaterialTheme.colorScheme.surface,
      modifier = Modifier.clip(RoundedCornerShape(12.dp))
    ) {
      Tab(
        selected = settings.mode == AudioOutputMode.LOSSY_M4A_AAC,
        onClick = { onSettingsUpdate { it.copy(mode = AudioOutputMode.LOSSY_M4A_AAC) } },
        text = { Text("M4A / AAC (Lossy)", fontWeight = FontWeight.SemiBold) }
      )
      Tab(
        selected = settings.mode == AudioOutputMode.LOSSLESS_WAV_PCM,
        onClick = { onSettingsUpdate { it.copy(mode = AudioOutputMode.LOSSLESS_WAV_PCM) } },
        text = { Text("WAV PCM (Lossless)", fontWeight = FontWeight.SemiBold) }
      )
    }

    if (settings.mode == AudioOutputMode.LOSSY_M4A_AAC) {
      // Bitrate Presets Card
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "Target Bitrate",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
          )
          Spacer(modifier = Modifier.height(10.dp))

          AudioBitrate.values().forEach { bitrate ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              FilterChip(
                selected = settings.bitrate == bitrate,
                onClick = { onSettingsUpdate { it.copy(bitrate = bitrate) } },
                label = { Text(bitrate.label, fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth()
              )
            }
          }
        }
      }
    } else {
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
            text = "Uncompressed 16-bit PCM WAV: Bit-perfect studio fidelity with universal compatibility across all digital audio workstations.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
          )
        }
      }
    }

    // Channels Downmix Card
    Card(
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      shape = RoundedCornerShape(16.dp)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Force Mono Audio",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
          )
          Text(
            text = "Averages stereo channels into 1 channel. Cuts file size by 50% for speech, meetings, and voice notes.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Switch(
          checked = settings.forceMono,
          onCheckedChange = { onSettingsUpdate { s -> s.copy(forceMono = it) } }
        )
      }
    }

    // Action Buttons
    Button(
      onClick = onTranscodeAll,
      enabled = items.isNotEmpty() && !isProcessing,
      modifier = Modifier
        .fillMaxWidth()
        .height(52.dp)
        .testTag("audio_lab_transcode_all_button"),
      shape = RoundedCornerShape(14.dp)
    ) {
      Icon(Icons.Default.PlayArrow, contentDescription = null)
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = if (items.isEmpty()) "Add Audio to Transcode" else "Transcode All (${items.size} Tracks)",
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
          .testTag("audio_lab_export_all_button"),
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
