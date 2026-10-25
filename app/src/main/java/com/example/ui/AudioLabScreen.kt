package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AudioBitrate
import com.example.model.AudioConvertItem
import com.example.model.AudioOutputFormat
import com.example.ui.components.Dropzone
import com.example.ui.components.OmniActionBar
import com.example.ui.components.OmniProgressBanner
import com.example.ui.components.OmniToast
import com.example.ui.components.OptionRow
import com.example.ui.components.RadioCard
import com.example.ui.components.SegmentedControl
import com.example.ui.components.ToolLayout
import com.example.ui.theme.OmniAccent
import com.example.ui.theme.OmniBorder
import com.example.ui.theme.OmniSurface
import com.example.ui.theme.OmniSurface2
import com.example.ui.theme.OmniText
import com.example.ui.theme.OmniTextMuted
import com.example.ui.theme.RadiusMd
import com.example.ui.theme.RadiusSm
import com.example.ui.theme.TintAudio
import com.example.viewmodel.AudioLabViewModel

@Composable
fun AudioLabScreen(
    viewModel: AudioLabViewModel,
    onTranscribeAudio: (Uri, String) -> Unit = { _, _ -> },
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

    val bitrates = listOf(
        AudioBitrate.KBPS_64,
        AudioBitrate.KBPS_128,
        AudioBitrate.KBPS_192,
        AudioBitrate.KBPS_256,
        AudioBitrate.KBPS_320
    )
    val bitrateLabels = listOf("64k", "128k*", "192k", "256k", "320k")
    val selectedBitrateIndex = bitrates.indexOf(settings.bitrate).coerceAtLeast(0)

    val convertedCount = items.count { it.convertedSizeBytes != null }
    val isAllConverted = items.isNotEmpty() && convertedCount == items.size

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("audio_lab_screen")
    ) {
        if (errorMessage != null) {
            OmniToast(
                message = errorMessage!!,
                isError = true,
                actionLabel = "Dismiss",
                onAction = { viewModel.dismissError() },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }

        if (isAllConverted) {
            OmniToast(
                message = "All ${items.size} audio files transcoded and saved to Music/OmniConvert",
                isError = false,
                actionLabel = "Clear",
                onAction = { viewModel.clearAll() },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }

        if (isTranscoding) {
            OmniProgressBanner(
                progress = progress,
                statusText = "Transcoding audio files...",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }

        ToolLayout(
            title = "Audio",
            description = "Transcode voice recordings and music tracks to M4A/AAC or lossless WAV.",
            isEmpty = items.isEmpty(),
            phoneTabTitles = listOf("Audio (${items.size})", "Options"),
            emptyDropzone = {
                Dropzone(
                    icon = Icons.Default.Audiotrack,
                    headline = "No audio loaded yet",
                    helpText = "Select audio files, voice notes, or lectures to transcode or send to Gemini AI for transcription",
                    supportedFormats = "MP3 · WAV · M4A · AAC · OGG · FLAC",
                    actionLabel = "Add Audio",
                    tintColor = TintAudio,
                    onAction = { pickerLauncher.launch("audio/*") },
                    testTag = "audio_add_button"
                )
            },
            fileList = {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${items.size} tracks loaded",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OmniText
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { pickerLauncher.launch("audio/*") },
                                modifier = Modifier
                                    .height(36.dp)
                                    .testTag("audio_add_more_button"),
                                border = BorderStroke(1.dp, OmniBorder),
                                shape = RoundedCornerShape(RadiusSm),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = OmniAccent)
                            ) {
                                Icon(Icons.Default.Audiotrack, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add More", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                            TextButton(
                                onClick = { viewModel.clearAll() },
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("Clear", color = OmniTextMuted, fontSize = 13.sp)
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(items, key = { it.id }) { item ->
                            AudioFileRow(
                                item = item,
                                onTranscribe = { onTranscribeAudio(item.uri, item.name) },
                                onRemove = { viewModel.removeItem(item.id) }
                            )
                        }
                    }
                }
            },
            optionsPanel = {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Output Format",
                        style = MaterialTheme.typography.titleMedium,
                        color = OmniText
                    )

                    // 2 Radio Cards for format
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        RadioCard(
                            title = "M4A / AAC-LC",
                            description = "High efficiency lossy format ideal for streaming and voice notes",
                            isSelected = settings.outputFormat == AudioOutputFormat.M4A_AAC,
                            onClick = { viewModel.updateSettings(settings.copy(outputFormat = AudioOutputFormat.M4A_AAC)) },
                            badgeText = "Recommended"
                        )
                        RadioCard(
                            title = "WAV (PCM 16-bit)",
                            description = "Uncompressed broadcast lossless quality for audio editing",
                            isSelected = settings.outputFormat == AudioOutputFormat.WAV_PCM_16BIT,
                            onClick = { viewModel.updateSettings(settings.copy(outputFormat = AudioOutputFormat.WAV_PCM_16BIT)) },
                            badgeText = "Lossless"
                        )
                    }

                    // Bitrate row (only for lossy M4A)
                    if (settings.outputFormat == AudioOutputFormat.M4A_AAC) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Encoding Bitrate",
                                    fontSize = 14.sp,
                                    color = OmniTextMuted
                                )
                                Text(
                                    text = settings.bitrate.label,
                                    fontSize = 14.sp,
                                    color = OmniAccent,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            SegmentedControl(
                                options = bitrateLabels,
                                selectedIndex = selectedBitrateIndex,
                                onOptionSelected = { idx ->
                                    viewModel.updateSettings(settings.copy(bitrate = bitrates[idx]))
                                }
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "* 128 kbps is recommended for podcasts, speech, and clean audio",
                                fontSize = 13.sp,
                                color = OmniTextMuted
                            )
                        }
                    }

                    // Mono switch
                    OptionRow(
                        label = "Downmix to mono",
                        description = "Combines stereo channels to reduce file size for speech",
                        checked = settings.forceMono,
                        onCheckedChange = { viewModel.updateSettings(settings.copy(forceMono = it)) }
                    )
                }
            },
            actionBar = {
                val formatLabel = if (settings.outputFormat == AudioOutputFormat.M4A_AAC) "M4A ${settings.bitrate.label}" else "WAV PCM"
                val monoLabel = if (settings.forceMono) " · Mono" else ""
                OmniActionBar(
                    summaryText = "${items.size} tracks · $formatLabel$monoLabel",
                    primaryButtonLabel = if (isTranscoding) "Transcoding..." else "Transcode Audio",
                    isEnabled = items.isNotEmpty() && !isTranscoding,
                    disabledHelperText = if (items.isEmpty()) "Add at least one audio file" else null,
                    onPrimaryAction = { viewModel.transcodeAll() },
                    testTag = "transcode_all_button"
                )
            }
        )
    }
}

@Composable
private fun AudioFileRow(
    item: AudioConvertItem,
    onTranscribe: () -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RadiusMd)),
        color = OmniSurface,
        border = BorderStroke(1.dp, OmniBorder),
        shape = RoundedCornerShape(RadiusMd)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(RadiusSm))
                    .background(TintAudio.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Audiotrack,
                    contentDescription = null,
                    tint = TintAudio,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OmniText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val durationSec = item.durationMs / 1000
                    val min = durationSec / 60
                    val sec = durationSec % 60
                    val durStr = "%d:%02d".format(min, sec)

                    val sizeKb = item.originalSizeBytes / 1024
                    val sizeLabel = if (sizeKb > 1024) "${"%.1f".format(sizeKb / 1024f)} MB" else "$sizeKb KB"

                    Text(
                        text = "$durStr · $sizeLabel · ${item.sampleRate} Hz",
                        fontSize = 13.sp,
                        color = OmniTextMuted
                    )

                    if (item.convertedSizeBytes != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        val convKb = item.convertedSizeBytes / 1024
                        val convLabel = if (convKb > 1024) "${"%.1f".format(convKb / 1024f)} MB" else "$convKb KB"
                        Text(
                            text = "→ $convLabel",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = OmniAccent
                        )
                    }
                }
            }

            if (item.isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = OmniAccent,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Quick Gemini transcribe shortcut
            IconButton(
                onClick = onTranscribe,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("transcribe_item_${item.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Transcribe with Gemini",
                    tint = OmniAccent,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove track",
                    tint = OmniTextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
