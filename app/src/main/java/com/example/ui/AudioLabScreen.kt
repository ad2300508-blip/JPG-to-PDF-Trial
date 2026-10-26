package com.example.ui

import android.media.MediaPlayer
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AudioBitrate
import com.example.model.AudioConvertItem
import com.example.model.AudioOutputFormat
import com.example.ui.components.Dropzone
import com.example.ui.components.FileCard
import com.example.ui.components.FileListHeader
import com.example.ui.components.OmniActionBar
import com.example.ui.components.OmniProgressBanner
import com.example.ui.components.OmniToast
import com.example.ui.components.OptionRow
import com.example.ui.components.OptionSection
import com.example.ui.components.RadioCard
import com.example.ui.components.RevealVertically
import com.example.ui.components.SegmentedControl
import com.example.ui.components.ShimmerProgressBar
import com.example.ui.components.ToolLayout
import com.example.ui.components.formatBytes
import com.example.ui.components.openFile
import com.example.ui.theme.Haptic
import com.example.ui.theme.OmniAccent
import com.example.ui.theme.OmniDanger
import com.example.ui.theme.OmniSuccess
import com.example.ui.theme.OmniSurface2
import com.example.ui.theme.OmniText
import com.example.ui.theme.OmniTextMuted
import com.example.ui.theme.TintAudio
import com.example.ui.theme.enterAnimation
import com.example.ui.theme.rememberHaptics
import com.example.util.OutputFolder
import com.example.util.OutputStorage
import com.example.viewmodel.AudioLabViewModel
import kotlinx.coroutines.delay

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
    val currentName by viewModel.currentName.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val context = LocalContext.current
    val haptics = rememberHaptics()

    var expandedId by rememberSaveable { mutableStateOf<String?>(null) }
    val player = rememberPreviewPlayer()

    // Audio files and videos (the audio track is extracted).
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) viewModel.addAudioFiles(uris)
    }
    val pickAudio = { pickerLauncher.launch(arrayOf("audio/*", "video/*")) }

    val bitrates = AudioBitrate.entries
    val bitrateLabels = listOf("64k", "128k", "192k", "256k", "320k")
    val selectedBitrateIndex = bitrates.indexOf(settings.bitrate).coerceAtLeast(0)

    val valid = items.filter { it.durationMs > 0 }
    val convertedCount = items.count { it.convertedSizeBytes != null }
    val pending = valid.count { it.convertedSizeBytes == null }
    val isAllConverted = valid.isNotEmpty() && pending == 0

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("audio_lab_screen")
    ) {
        RevealVertically(visible = errorMessage != null) {
            OmniToast(
                message = errorMessage.orEmpty(),
                isError = true,
                actionLabel = "Chiudi",
                onAction = { viewModel.dismissError() },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }

        RevealVertically(visible = isAllConverted && !isTranscoding) {
            OmniToast(
                message = "${if (convertedCount == 1) "1 file audio salvato" else "$convertedCount file audio salvati"} in ${OutputStorage.displayPath(OutputFolder.AUDIO)}",
                actionLabel = "Svuota",
                onAction = { viewModel.clearAll() },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }

        RevealVertically(visible = isTranscoding) {
            OmniProgressBanner(
                progress = progress,
                statusText = if (currentName.isNotBlank()) "Converto $currentName…" else "Converto i file audio…",
                onCancel = { viewModel.cancel() },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }

        ToolLayout(
            title = "Converti audio",
            description = "Converti, taglia e alza il volume di lezioni e note vocali",
            isEmpty = items.isEmpty(),
            phoneTabTitles = listOf("Audio · ${items.size}", "Opzioni"),
            emptyDropzone = {
                Dropzone(
                    icon = Icons.Default.Audiotrack,
                    headline = "Aggiungi i tuoi file audio",
                    helpText = "Registrazioni, note vocali o lezioni, anche video: ne estraggo l'audio, lo taglio e lo converto",
                    supportedFormats = "MP3 · WAV · M4A · AAC · OGG · FLAC · MP4",
                    actionLabel = "Scegli file",
                    tintColor = TintAudio,
                    onAction = pickAudio,
                    testTag = "audio_add_button"
                )
            },
            fileList = {
                Column(modifier = Modifier.fillMaxSize()) {
                    FileListHeader(
                        title = if (items.size == 1) "1 traccia" else "${items.size} tracce",
                        onAdd = pickAudio,
                        onClear = {
                            player.stop()
                            viewModel.clearAll()
                        },
                        addIcon = Icons.Default.Audiotrack
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                            AudioFileRow(
                                item = item,
                                expanded = expandedId == item.id,
                                isPlaying = player.playingId == item.id,
                                playPosition = if (player.playingId == item.id) player.position else 0f,
                                onToggleExpand = { expandedId = if (expandedId == item.id) null else item.id },
                                onTogglePlay = {
                                    haptics(Haptic.TAP)
                                    player.toggle(item.id, item.outputUri ?: item.uri, item.trimStartMs ?: 0L)
                                },
                                onTrim = { start, end -> viewModel.setTrim(item.id, start, end) },
                                onTranscribe = { onTranscribeAudio(item.outputUri ?: item.uri, item.name) },
                                onOpenResult = { item.outputUri?.let { openFile(context, it, "audio/*") } },
                                onRemove = {
                                    if (player.playingId == item.id) player.stop()
                                    viewModel.removeItem(item.id)
                                },
                                modifier = Modifier
                                    .animateItem()
                                    .enterAnimation(index, key = item.id)
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
                        text = "Formato di uscita",
                        style = MaterialTheme.typography.titleMedium,
                        color = OmniText
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        RadioCard(
                            title = "M4A / AAC-LC",
                            description = "Compresso: ideale per lezioni, note vocali e Gemini",
                            isSelected = settings.outputFormat == AudioOutputFormat.M4A_AAC,
                            onClick = { viewModel.updateSettings(settings.copy(outputFormat = AudioOutputFormat.M4A_AAC)) },
                            badgeText = "Consigliato"
                        )
                        RadioCard(
                            title = "WAV (PCM 16 bit)",
                            description = "Senza perdita, ideale per il montaggio audio",
                            isSelected = settings.outputFormat == AudioOutputFormat.WAV_PCM_16BIT,
                            onClick = { viewModel.updateSettings(settings.copy(outputFormat = AudioOutputFormat.WAV_PCM_16BIT)) },
                            badgeText = "Senza perdita"
                        )
                    }

                    AnimatedVisibility(
                        visible = settings.outputFormat == AudioOutputFormat.M4A_AAC,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        OptionSection(title = "Bitrate · ${settings.bitrate.label}", hint = settings.bitrate.description + if (settings.bitrate == AudioBitrate.KBPS_64) " · perfetto per Gemini" else "") {
                            SegmentedControl(
                                options = bitrateLabels,
                                selectedIndex = selectedBitrateIndex,
                                onOptionSelected = { idx -> viewModel.updateSettings(settings.copy(bitrate = bitrates[idx])) }
                            )
                        }
                    }

                    OptionRow(
                        label = "Converti in mono",
                        description = "Unisce i canali stereo: metà peso per le registrazioni vocali",
                        checked = settings.forceMono,
                        onCheckedChange = { viewModel.updateSettings(settings.copy(forceMono = it)) }
                    )

                    OptionRow(
                        label = "Normalizza il volume",
                        description = "Alza il volume delle registrazioni troppo basse senza distorcere",
                        checked = settings.normalizeVolume,
                        onCheckedChange = { viewModel.updateSettings(settings.copy(normalizeVolume = it)) }
                    )

                    Text(
                        text = "Suggerimento: tocca una traccia per tagliarne l'inizio o la fine.",
                        fontSize = 13.sp,
                        color = OmniTextMuted
                    )
                }
            },
            actionBar = {
                val formatLabel = if (settings.outputFormat == AudioOutputFormat.M4A_AAC) "M4A ${settings.bitrate.label}" else "WAV PCM"
                val extras = listOfNotNull("Mono".takeIf { settings.forceMono }, "Volume normalizzato".takeIf { settings.normalizeVolume })
                OmniActionBar(
                    summaryText = if (items.size == 1) "1 traccia · $formatLabel" else "${items.size} tracce · $formatLabel",
                    detailText = extras.joinToString(" · ").ifBlank { null },
                    primaryButtonLabel = if (isTranscoding) "Conversione…" else if (isAllConverted) "Fatto" else "Converti",
                    isEnabled = pending > 0 && !isTranscoding,
                    disabledHelperText = when {
                        items.isEmpty() -> "Aggiungi almeno un file audio"
                        isAllConverted -> "Cambia le opzioni per convertire di nuovo"
                        else -> null
                    },
                    onPrimaryAction = {
                        player.stop()
                        viewModel.transcodeAll()
                    },
                    testTag = "transcode_all_button"
                )
            }
        )
    }
}

@Composable
private fun AudioFileRow(
    item: AudioConvertItem,
    expanded: Boolean,
    isPlaying: Boolean,
    playPosition: Float,
    onToggleExpand: () -> Unit,
    onTogglePlay: () -> Unit,
    onTrim: (Long?, Long?) -> Unit,
    onTranscribe: () -> Unit,
    onOpenResult: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val details = when {
        item.error != null && item.durationMs <= 0 -> item.error
        item.error != null -> item.error
        item.convertedSizeBytes != null -> "${formatDuration(item.effectiveDurationMs)} · ${formatBytes(item.originalSizeBytes)} → ${formatBytes(item.convertedSizeBytes)}"
        else -> listOfNotNull(
            formatDuration(item.effectiveDurationMs) + if (item.isTrimmed) " (tagliato)" else "",
            formatBytes(item.originalSizeBytes),
            "${item.sampleRate / 1000.0} kHz".takeIf { item.sampleRate > 0 },
            "${item.bitrateKbps} kbps".takeIf { item.bitrateKbps > 0 }
        ).joinToString(" · ")
    }

    FileCard(
        name = item.name,
        details = details,
        detailsColor = when {
            item.error != null -> OmniDanger
            item.convertedSizeBytes != null -> OmniSuccess
            else -> OmniTextMuted
        },
        icon = Icons.Default.Audiotrack,
        iconTint = TintAudio,
        highlighted = expanded,
        onClick = if (item.durationMs > 0) onToggleExpand else null,
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                when {
                    item.isProcessing -> CircularProgressIndicator(
                        progress = { item.progress },
                        modifier = Modifier.size(22.dp),
                        color = OmniAccent,
                        strokeWidth = 2.5.dp,
                        trackColor = OmniSurface2
                    )
                    item.convertedSizeBytes != null -> Icon(Icons.Default.CheckCircle, contentDescription = "Convertito", tint = OmniSuccess, modifier = Modifier.size(22.dp))
                    item.error != null -> Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = OmniDanger, modifier = Modifier.size(22.dp))
                }
                if (item.durationMs > 0) {
                    IconButton(onClick = onTogglePlay, modifier = Modifier.size(40.dp)) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pausa" else "Ascolta",
                            tint = OmniText,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    IconButton(
                        onClick = onTranscribe,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("transcribe_item_${item.id}")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Trascrivi con Gemini", tint = OmniAccent, modifier = Modifier.size(18.dp))
                    }
                }
            }
        },
        onRemove = onRemove,
        modifier = modifier,
        bottomContent = {
            Column {
                AnimatedVisibility(visible = isPlaying) {
                    ShimmerProgressBar(
                        progress = playPosition,
                        height = 4.dp,
                        color = TintAudio,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )
                }
                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically(tween(260)) + fadeIn(),
                    exit = shrinkVertically(tween(200)) + fadeOut()
                ) {
                    TrimEditor(item = item, onTrim = onTrim, onOpenResult = onOpenResult)
                }
            }
        }
    )
}

@Composable
private fun TrimEditor(item: AudioConvertItem, onTrim: (Long?, Long?) -> Unit, onOpenResult: () -> Unit) {
    val duration = item.durationMs.coerceAtLeast(1L).toFloat()
    var range by remember(item.id) {
        mutableStateOf((item.trimStartMs ?: 0L).toFloat()..(item.trimEndMs ?: item.durationMs).toFloat())
    }
    Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ContentCut, contentDescription = null, tint = OmniAccent, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Taglia", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = OmniText, modifier = Modifier.weight(1f))
            Text(
                "${formatDuration(range.start.toLong())} – ${formatDuration(range.endInclusive.toLong())}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = OmniAccent
            )
        }
        RangeSlider(
            value = range,
            onValueChange = { range = it },
            onValueChangeFinished = {
                onTrim(range.start.toLong(), range.endInclusive.toLong())
            },
            valueRange = 0f..duration,
            colors = SliderDefaults.colors(
                thumbColor = OmniAccent,
                activeTrackColor = OmniAccent,
                inactiveTrackColor = OmniSurface2
            )
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Durata finale ${formatDuration((range.endInclusive - range.start).toLong())}",
                fontSize = 12.sp,
                color = OmniTextMuted,
                modifier = Modifier.weight(1f)
            )
            if (item.isTrimmed) {
                TextButton(onClick = {
                    range = 0f..duration
                    onTrim(null, null)
                }) { Text("Ripristina", fontSize = 13.sp, color = OmniTextMuted) }
            }
            if (item.outputUri != null) {
                TextButton(onClick = onOpenResult) { Text("Apri risultato", fontSize = 13.sp, color = OmniAccent) }
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%d:%02d".format(minutes, seconds)
}

/** Tiny single-track player for previewing files in the list. */
private class PreviewPlayer(private val context: android.content.Context) {
    var playingId by mutableStateOf<String?>(null)
        private set
    var position by mutableFloatStateOf(0f)
        private set
    private var player: MediaPlayer? = null

    fun toggle(id: String, uri: Uri, startMs: Long) {
        if (playingId == id) {
            stop()
            return
        }
        stop()
        runCatching {
            player = MediaPlayer().apply {
                setDataSource(context, uri)
                setOnCompletionListener { stop() }
                prepare()
                if (startMs > 0) seekTo(startMs.toInt())
                start()
            }
            playingId = id
        }.onFailure { stop() }
    }

    fun tick() {
        val p = player ?: return
        runCatching {
            if (p.duration > 0) position = p.currentPosition.toFloat() / p.duration
        }
    }

    fun stop() {
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
        playingId = null
        position = 0f
    }
}

@Composable
private fun rememberPreviewPlayer(): PreviewPlayer {
    val context = LocalContext.current
    val player = remember { PreviewPlayer(context.applicationContext) }
    DisposableEffect(Unit) {
        onDispose { player.stop() }
    }
    LaunchedEffect(player.playingId) {
        while (player.playingId != null) {
            player.tick()
            delay(200)
        }
    }
    return player
}
