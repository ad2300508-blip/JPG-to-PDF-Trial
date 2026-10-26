package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ArchiveItem
import com.example.ui.components.ContinueAction
import com.example.ui.components.ConversionResultView
import com.example.ui.components.Dropzone
import com.example.ui.components.OmniActionBar
import com.example.ui.components.OmniProgressBanner
import com.example.ui.components.OmniSlider
import com.example.ui.components.OmniToast
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
import com.example.ui.theme.TintZip
import com.example.viewmodel.ArchiveViewModel

@Composable
fun ArchiveScreen(
    viewModel: ArchiveViewModel,
    modifier: Modifier = Modifier
) {
    val items by viewModel.items.collectAsState()
    val config by viewModel.config.collectAsState()
    val isArchiving by viewModel.isArchiving.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val lastGeneratedUri by viewModel.lastGeneratedUri.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val zipUri = lastGeneratedUri
    if (zipUri != null) {
        ConversionResultView(
            title = "Il tuo ZIP è pronto",
            subtitle = "${items.size} file compressi in un archivio.",
            outputUri = zipUri,
            mimeType = "application/zip",
            previewIcon = Icons.Default.Archive,
            previewTint = com.example.ui.theme.TintZip,
            originalBytes = items.sumOf { it.sizeBytes },
            detailPrefix = "${items.size} file",
            onClose = { viewModel.dismissSuccess() },
            onNewConversion = { viewModel.clearAll() },
            modifier = modifier
        )
        return
    }

    var showAdvanced by rememberSaveable { mutableStateOf(false) }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addFiles(uris)
        }
    }

    val compressionLevels = listOf("Veloce", "Bilanciato", "Massima")
    val compressionValues = listOf(1, 6, 9)
    val selectedCompressionIndex = when {
        config.compressionLevel <= 3 -> 0
        config.compressionLevel <= 7 -> 1
        else -> 2
    }

    val totalSize = items.sumOf { it.sizeBytes }
    val totalSizeMb = totalSize / (1024f * 1024f)
    val sizeText = if (totalSizeMb >= 1.0f) "%.1f MB".format(totalSizeMb) else "${totalSize / 1024} KB"

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("archive_screen")
    ) {
        if (errorMessage != null) {
            OmniToast(
                message = errorMessage!!,
                isError = true,
                actionLabel = "Chiudi",
                onAction = { viewModel.dismissError() },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }

        if (isArchiving) {
            val p = if (progress.second > 0) progress.first.toFloat() / progress.second.toFloat() else 0f
            OmniProgressBanner(
                progress = p,
                statusText = "Comprimo il file ${progress.first} di ${progress.second}…",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }

        ToolLayout(
            title = "Crea archivio ZIP",
            description = "Comprimi documenti e immagini in un archivio ZIP",
            isEmpty = items.isEmpty(),
            phoneTabTitles = listOf("File · ${items.size}", "Opzioni"),
            emptyDropzone = {
                Dropzone(
                    icon = Icons.Default.Archive,
                    headline = "Aggiungi i tuoi file",
                    helpText = "Scegli i file da comprimere in un archivio ZIP",
                    supportedFormats = "Tutti i tipi di file",
                    actionLabel = "Scegli file",
                    tintColor = TintZip,
                    onAction = { pickerLauncher.launch("*/*") },
                    testTag = "archive_add_button"
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
                            text = "${items.size} files · $sizeText",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OmniText
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { pickerLauncher.launch("*/*") },
                                modifier = Modifier
                                    .height(36.dp)
                                    .testTag("archive_add_more_button"),
                                border = BorderStroke(1.dp, OmniBorder),
                                shape = RoundedCornerShape(RadiusSm),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = OmniAccent)
                            ) {
                                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Aggiungi", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                            TextButton(
                                onClick = { viewModel.clearAll() },
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("Svuota", color = OmniTextMuted, fontSize = 13.sp)
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
                            ArchiveFileRow(
                                item = item,
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
                        text = "Opzioni archivio",
                        style = MaterialTheme.typography.titleMedium,
                        color = OmniText
                    )

                    // Archive file name
                    Column {
                        Text(
                            text = "Nome archivio",
                            fontSize = 14.sp,
                            color = OmniTextMuted,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = config.zipFileName,
                            onValueChange = { viewModel.updateConfig(config.copy(zipFileName = it)) },
                            placeholder = { Text("OmniArchive", color = OmniTextMuted, fontSize = 14.sp) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("archive_filename_input"),
                            shape = RoundedCornerShape(RadiusSm),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = OmniSurface2,
                                unfocusedContainerColor = OmniSurface2,
                                focusedBorderColor = OmniAccent,
                                unfocusedBorderColor = OmniBorder,
                                focusedTextColor = OmniText,
                                unfocusedTextColor = OmniText
                            )
                        )
                    }

                    // 3-step compression segmented control
                    Column {
                        Text(
                            text = "Livello di compressione",
                            fontSize = 14.sp,
                            color = OmniTextMuted,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        SegmentedControl(
                            options = compressionLevels,
                            selectedIndex = selectedCompressionIndex,
                            onOptionSelected = { idx ->
                                viewModel.updateConfig(config.copy(compressionLevel = compressionValues[idx]))
                            }
                        )
                    }

                    // Advanced collapsible
                    Surface(
                        onClick = { showAdvanced = !showAdvanced },
                        shape = RoundedCornerShape(RadiusSm),
                        color = OmniSurface2,
                        border = BorderStroke(1.dp, OmniBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Impostazioni avanzate di compressione",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = OmniText
                            )
                            Icon(
                                imageVector = if (showAdvanced) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = OmniTextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    AnimatedVisibility(visible = showAdvanced) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(RadiusSm))
                                .background(OmniSurface2)
                                .border(1.dp, OmniBorder, RoundedCornerShape(RadiusSm))
                                .padding(14.dp)
                        ) {
                            OmniSlider(
                                label = "Livello numerico",
                                value = config.compressionLevel.toFloat(),
                                onValueChange = { viewModel.updateConfig(config.copy(compressionLevel = it.toInt())) },
                                valueRange = 0f..9f,
                                valueFormatter = {
                                    val lvl = it.toInt()
                                    when (lvl) {
                                        0 -> "0 (Store only)"
                                        1 -> "1 (Fastest)"
                                        6 -> "6 (Balanced standard)"
                                        9 -> "9 (Maximum compression)"
                                        else -> "Livello $lvl"
                                    }
                                }
                            )
                        }
                    }
                }
            },
            actionBar = {
                val compLabel = compressionLevels[selectedCompressionIndex]
                OmniActionBar(
                    summaryText = "${items.size} files · $sizeText · $compLabel",
                    primaryButtonLabel = if (isArchiving) "Creo lo ZIP…" else "Crea ZIP",
                    isEnabled = items.isNotEmpty() && !isArchiving,
                    disabledHelperText = if (items.isEmpty()) "Aggiungi almeno un file" else null,
                    onPrimaryAction = { viewModel.createArchive() },
                    testTag = "create_archive_button"
                )
            }
        )
    }
}

@Composable
private fun ArchiveFileRow(
    item: ArchiveItem,
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
                    .size(44.dp)
                    .clip(RoundedCornerShape(RadiusSm))
                    .background(TintZip.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = TintZip,
                    modifier = Modifier.size(22.dp)
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
                val sizeKb = item.sizeBytes / 1024
                val sizeLabel = if (sizeKb > 1024) "${"%.1f".format(sizeKb / 1024f)} MB" else "$sizeKb KB"
                Text(
                    text = sizeLabel,
                    fontSize = 13.sp,
                    color = OmniTextMuted
                )
            }

            IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Rimuovi file",
                    tint = OmniTextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
