package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextOverflow
import com.example.ui.components.OmniPhoneTabSelector
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AudioBitrate
import com.example.model.AudioConversionSettings
import com.example.model.AudioConvertItem
import com.example.model.AudioOutputFormat
import com.example.ui.components.OmniBadge
import com.example.ui.components.OmniCard
import com.example.ui.components.OmniEmptyState
import com.example.ui.components.OmniErrorBanner
import com.example.ui.components.OmniHeader
import com.example.ui.components.OmniLinearProgress
import com.example.ui.components.OmniStatPill
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.RoseAccent
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceElevated
import com.example.viewmodel.AudioLabViewModel

@Composable
fun AudioLabScreen(
    viewModel: AudioLabViewModel,
    onTranscribeAudio: (android.net.Uri, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val items by viewModel.items.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val isTranscoding by viewModel.isTranscoding.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addAudioFiles(uris)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("audio_lab_screen")
    ) {
        OmniHeader(
            workspaceTitle = "Audio Lab",
            categoryBadge = "Transcoder & Compressor",
            badgeColor = IndigoAccent,
            itemCount = items.size,
            isProcessing = isTranscoding,
            actions = {
                OutlinedButton(
                    onClick = { pickerLauncher.launch("audio/*") },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = IndigoAccent),
                    modifier = Modifier.testTag("audio_add_button")
                ) {
                    Icon(Icons.Default.Audiotrack, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Audio")
                }
                if (items.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = { viewModel.clearAll() }) {
                        Icon(Icons.Default.ClearAll, contentDescription = "Clear All", tint = Slate400)
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (errorMessage != null) {
            OmniErrorBanner(message = errorMessage!!, onDismiss = { viewModel.dismissError() })
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (isTranscoding) {
            OmniLinearProgress(progress = progress, statusText = "Transcoding audio tracks...")
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Responsive Layout: Tablet (>= 600.dp) side-by-side, Phone (< 600.dp) 2-Tab Switcher
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val isTablet = maxWidth >= 600.dp
            if (isTablet) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(0.42f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceCard)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        AudioQueuePane(
                            items = items,
                            onPickAudio = { pickerLauncher.launch("audio/*") },
                            onTranscribeAudio = onTranscribeAudio,
                            onRemoveItem = { viewModel.removeItem(it) }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(0.58f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceCard)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                            .padding(18.dp)
                    ) {
                        AudioSettingsPane(
                            items = items,
                            settings = settings,
                            isTranscoding = isTranscoding,
                            onUpdateSettings = { viewModel.updateSettings(it) },
                            onTranscodeAll = { viewModel.transcodeAll() }
                        )
                    }
                }
            } else {
                var selectedPhoneTab by rememberSaveable { mutableIntStateOf(0) }
                Column(modifier = Modifier.fillMaxSize()) {
                    OmniPhoneTabSelector(
                        tabs = listOf("Tracks (${items.size})", "Audio Settings"),
                        selectedIndex = selectedPhoneTab,
                        onTabSelected = { selectedPhoneTab = it },
                        activeColor = IndigoAccent
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceCard)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        if (selectedPhoneTab == 0) {
                            AudioQueuePane(
                                items = items,
                                onPickAudio = { pickerLauncher.launch("audio/*") },
                                onTranscribeAudio = onTranscribeAudio,
                                onRemoveItem = { viewModel.removeItem(it) }
                            )
                        } else {
                            AudioSettingsPane(
                                items = items,
                                settings = settings,
                                isTranscoding = isTranscoding,
                                onUpdateSettings = { viewModel.updateSettings(it) },
                                onTranscodeAll = { viewModel.transcodeAll() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioQueuePane(
    items: List<AudioConvertItem>,
    onPickAudio: () -> Unit,
    onTranscribeAudio: (android.net.Uri, String) -> Unit,
    onRemoveItem: (String) -> Unit
) {
    if (items.isEmpty()) {
        OmniEmptyState(
            icon = Icons.Default.MusicNote,
            title = "No Audio Files Added",
            description = "Import MP3, WAV, M4A, FLAC, or OGG tracks to transcode with native Android MediaCodec."
        ) {
            Button(
                onClick = onPickAudio,
                colors = ButtonDefaults.buttonColors(containerColor = IndigoAccent, contentColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(48.dp)
            ) {
                Icon(Icons.Default.Audiotrack, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Import Audio Files", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(items, key = { it.id }) { item ->
                AudioItemCard(
                    item = item,
                    onTranscribe = { onTranscribeAudio(item.uri, item.mimeType) },
                    onRemove = { onRemoveItem(item.id) }
                )
            }
        }
    }
}

@Composable
private fun AudioSettingsPane(
    items: List<AudioConvertItem>,
    settings: AudioConversionSettings,
    isTranscoding: Boolean,
    onUpdateSettings: (AudioConversionSettings) -> Unit,
    onTranscodeAll: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "AUDIO CODEC & ENCODING",
                style = MaterialTheme.typography.labelSmall,
                color = Slate400
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Lossless vs Lossy Mode Selector
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AudioOutputFormat.values().forEach { fmt ->
                    val selected = settings.outputFormat == fmt
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) IndigoAccent.copy(alpha = 0.2f) else SurfaceElevated)
                            .border(1.dp, if (selected) IndigoAccent else BorderSubtle, RoundedCornerShape(10.dp))
                            .clickable { onUpdateSettings(settings.copy(outputFormat = fmt)) }
                            .padding(14.dp)
                            .testTag("audio_format_${fmt.name}")
                    ) {
                        Column {
                            Text(
                                text = fmt.displayName,
                                color = if (selected) IndigoAccent else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (fmt.isLossless) "Bit-perfect uncompressed PCM" else "High-efficiency AAC compression",
                                color = Slate400,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Bitrate options for AAC
            if (settings.outputFormat == AudioOutputFormat.M4A_AAC) {
                Text(
                    text = "AAC Bitrate Preset",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AudioBitrate.values().forEach { b ->
                        val selected = settings.bitrate == b
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) IndigoAccent.copy(alpha = 0.15f) else SurfaceElevated)
                                .border(1.dp, if (selected) IndigoAccent else BorderSubtle, RoundedCornerShape(8.dp))
                            .clickable { onUpdateSettings(settings.copy(bitrate = b)) }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .testTag("bitrate_${b.kbps}")
                        ) {
                            Text(
                                text = b.label,
                                color = if (selected) IndigoAccent else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Force mono switch
                OmniCard(borderColor = BorderSubtle, backgroundColor = SurfaceElevated) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Downmix to Mono",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Reduces file size by another 50% for voice recordings and lectures.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Slate400
                            )
                        }
                        Switch(
                            checked = settings.forceMono,
                            onCheckedChange = { onUpdateSettings(settings.copy(forceMono = it)) },
                            colors = SwitchDefaults.colors(checkedThumbColor = IndigoAccent, checkedTrackColor = Slate800)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Bar
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OmniStatPill(label = "Tracks", value = "${items.size}")
                OmniStatPill(label = "Format", value = settings.outputFormat.extension.uppercase(), valueColor = IndigoAccent)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onTranscodeAll,
                enabled = items.isNotEmpty() && !isTranscoding,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("transcode_audio_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = IndigoAccent,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isTranscoding) "TRANSCODING..." else "TRANSCODE & SAVE AUDIO",
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun AudioItemCard(
    item: AudioConvertItem,
    onTranscribe: () -> Unit,
    onRemove: () -> Unit
) {
    OmniCard(borderColor = BorderSubtle, backgroundColor = SurfaceElevated) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Slate800),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.MusicNote, contentDescription = null, tint = IndigoAccent, modifier = Modifier.size(24.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val sec = item.durationMs / 1000
                    val min = sec / 60
                    val rem = sec % 60
                    OmniBadge(text = "%02d:%02d".format(min, rem), color = Slate400)
                    OmniBadge(text = "${item.originalSizeBytes / 1024} KB", color = Slate400)

                    if (item.convertedSizeBytes != null) {
                        OmniBadge(text = "CONVERTED (${item.convertedSizeBytes / 1024} KB)", color = IndigoAccent)
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onTranscribe, modifier = Modifier.size(32.dp).testTag("transcribe_item_${item.id}")) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "AI Transcribe", tint = CyanAccent, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = RoseAccent, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
