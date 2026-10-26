package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.kindForFileName
import com.example.model.ArchiveItem
import com.example.service.ArchiveService
import com.example.ui.components.ConversionResultView
import com.example.ui.components.Dropzone
import com.example.ui.components.FileCard
import com.example.ui.components.FileListHeader
import com.example.ui.components.MorphTextField
import com.example.ui.components.OmniActionBar
import com.example.ui.components.OmniProgressBanner
import com.example.ui.components.OmniSlider
import com.example.ui.components.OmniToast
import com.example.ui.components.OptionSection
import com.example.ui.components.RevealVertically
import com.example.ui.components.SegmentedControl
import com.example.ui.components.SuccessBadge
import com.example.ui.components.ToolHeader
import com.example.ui.components.ToolLayout
import com.example.ui.components.formatBytes
import com.example.ui.components.icon
import com.example.ui.components.openFile
import com.example.ui.components.shareFiles
import com.example.ui.components.tint
import com.example.ui.theme.OmniAccent
import com.example.ui.theme.OmniBorder
import com.example.ui.theme.OmniOnAccent
import com.example.ui.theme.OmniSuccess
import com.example.ui.theme.OmniSurface
import com.example.ui.theme.OmniText
import com.example.ui.theme.OmniTextMuted
import com.example.ui.theme.RadiusMd
import com.example.ui.theme.TintZip
import com.example.ui.theme.enterAnimation
import com.example.util.OutputFolder
import com.example.util.OutputStorage
import com.example.viewmodel.ArchiveMode
import com.example.viewmodel.ArchiveViewModel

@Composable
fun ArchiveScreen(
    viewModel: ArchiveViewModel,
    modifier: Modifier = Modifier
) {
    val mode by viewModel.mode.collectAsState()
    val lastGeneratedUri by viewModel.lastGeneratedUri.collectAsState()

    AnimatedContent(
        targetState = when {
            lastGeneratedUri != null -> 2
            mode == ArchiveMode.EXTRACT -> 1
            else -> 0
        },
        transitionSpec = { stageTransition(initialState, targetState) },
        modifier = modifier,
        label = "archive_stage"
    ) { stage ->
        when (stage) {
            2 -> {
                val zipUri = lastGeneratedUri
                val items by viewModel.items.collectAsState()
                val size by viewModel.lastGeneratedSize.collectAsState()
                if (zipUri != null) {
                    ConversionResultView(
                        title = "Il tuo ZIP è pronto",
                        subtitle = "${items.size} file compressi in un archivio.",
                        outputUri = zipUri,
                        mimeType = "application/zip",
                        previewIcon = Icons.Default.Archive,
                        previewTint = TintZip,
                        originalBytes = items.sumOf { it.sizeBytes },
                        outputBytesOverride = size.takeIf { it > 0 },
                        detailPrefix = "${items.size} file",
                        savedIn = "Salvato in ${OutputStorage.displayPath(OutputFolder.ARCHIVES)}",
                        onClose = { viewModel.dismissSuccess() },
                        onNewConversion = { viewModel.clearAll() }
                    )
                }
            }
            1 -> ExtractView(viewModel)
            else -> CreateView(viewModel)
        }
    }
}

@Composable
private fun ModeSwitch(viewModel: ArchiveViewModel, mode: ArchiveMode, modifier: Modifier = Modifier) {
    SegmentedControl(
        options = listOf("Crea ZIP", "Estrai ZIP"),
        selectedIndex = mode.ordinal,
        onOptionSelected = { viewModel.setMode(ArchiveMode.entries[it]) },
        modifier = modifier
    )
}

@Composable
private fun CreateView(viewModel: ArchiveViewModel) {
    val items by viewModel.items.collectAsState()
    val config by viewModel.config.collectAsState()
    val isArchiving by viewModel.isArchiving.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val progressLabel by viewModel.progressLabel.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) viewModel.addFiles(uris)
    }

    val compressionLevels = listOf("Nessuna", "Veloce", "Bilanciata", "Massima")
    val compressionValues = listOf(0, 1, 6, 9)
    val selectedCompressionIndex = when {
        config.compressionLevel == 0 -> 0
        config.compressionLevel <= 3 -> 1
        config.compressionLevel <= 7 -> 2
        else -> 3
    }
    val totalSize = items.sumOf { it.sizeBytes }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("archive_screen")
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

        RevealVertically(visible = isArchiving) {
            val p = if (progress.second > 0) progress.first.toFloat() / progress.second.toFloat() else 0f
            OmniProgressBanner(
                progress = p,
                statusText = progressLabel.ifBlank { "Comprimo il file ${progress.first} di ${progress.second}…" },
                onCancel = { viewModel.cancel() },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }

        ToolLayout(
            title = "Archivio ZIP",
            description = "Comprimi documenti e immagini in un archivio",
            isEmpty = items.isEmpty(),
            phoneTabTitles = listOf("File · ${items.size}", "Opzioni"),
            emptyDropzone = {
                Column {
                    ModeSwitch(viewModel, ArchiveMode.CREATE)
                    Spacer(modifier = Modifier.height(12.dp))
                    Dropzone(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Archive,
                        headline = "Aggiungi i tuoi file",
                        helpText = "Scegli i file da comprimere in un archivio ZIP",
                        supportedFormats = "Tutti i tipi di file",
                        actionLabel = "Scegli file",
                        tintColor = TintZip,
                        onAction = { pickerLauncher.launch("*/*") },
                        testTag = "archive_add_button"
                    )
                }
            },
            fileList = {
                Column(modifier = Modifier.fillMaxSize()) {
                    FileListHeader(
                        title = "${items.size} file · ${formatBytes(totalSize)}",
                        onAdd = { pickerLauncher.launch("*/*") },
                        onClear = { viewModel.clearAll() },
                        addIcon = Icons.Default.UploadFile
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                            ArchiveFileRow(
                                item = item,
                                onRemove = { viewModel.removeItem(item.id) },
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
                        text = "Opzioni archivio",
                        style = MaterialTheme.typography.titleMedium,
                        color = OmniText
                    )

                    MorphTextField(
                        value = config.zipFileName,
                        onValueChange = { viewModel.updateConfig(config.copy(zipFileName = it)) },
                        label = "Nome archivio",
                        placeholder = "Archivio",
                        testTag = "archive_filename_input"
                    )

                    OptionSection(
                        title = "Compressione",
                        hint = when (selectedCompressionIndex) {
                            0 -> "Solo archiviazione: velocissimo, ideale per foto e video già compressi"
                            1 -> "Veloce, file un po' più grande"
                            2 -> "Il giusto equilibrio tra velocità e peso"
                            else -> "File più piccolo, richiede più tempo"
                        }
                    ) {
                        SegmentedControl(
                            options = compressionLevels,
                            selectedIndex = selectedCompressionIndex,
                            onOptionSelected = { idx -> viewModel.updateConfig(config.copy(compressionLevel = compressionValues[idx])) }
                        )
                    }

                    OmniSlider(
                        label = "Livello preciso",
                        value = config.compressionLevel.toFloat(),
                        onValueChange = { viewModel.updateConfig(config.copy(compressionLevel = it.toInt())) },
                        valueRange = 0f..9f,
                        steps = 8,
                        valueFormatter = { "Livello ${it.toInt()}" }
                    )
                }
            },
            actionBar = {
                OmniActionBar(
                    summaryText = "${items.size} file · ${formatBytes(totalSize)}",
                    detailText = "Compressione ${compressionLevels[selectedCompressionIndex].lowercase()}",
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
private fun ExtractView(viewModel: ArchiveViewModel) {
    val zip by viewModel.zipToExtract.collectAsState()
    val entries by viewModel.zipEntries.collectAsState()
    val extracted by viewModel.extracted.collectAsState()
    val isWorking by viewModel.isArchiving.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val progressLabel by viewModel.progressLabel.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val context = LocalContext.current

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.selectZipToExtract(uri)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        RevealVertically(visible = errorMessage != null) {
            OmniToast(
                message = errorMessage.orEmpty(),
                isError = true,
                actionLabel = "Chiudi",
                onAction = { viewModel.dismissError() },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }
        RevealVertically(visible = isWorking) {
            val p = if (progress.second > 0) progress.first.toFloat() / progress.second.toFloat() else 0f
            OmniProgressBanner(
                progress = p,
                statusText = "Estraggo ${progressLabel.ifBlank { "i file" }}…",
                onCancel = { viewModel.cancel() },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }

        Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp)) {
            ToolHeader(title = "Archivio ZIP", description = "Apri un archivio e salva i file che contiene")
        }
        ModeSwitch(viewModel, ArchiveMode.EXTRACT, Modifier.padding(horizontal = 20.dp))
        Spacer(modifier = Modifier.height(12.dp))

        val current = zip
        when {
            current == null -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Dropzone(
                    icon = Icons.Default.Unarchive,
                    headline = "Scegli un archivio ZIP",
                    helpText = "Vedrai l'elenco dei file e potrai salvarli tutti in Download",
                    supportedFormats = "ZIP",
                    actionLabel = "Scegli ZIP",
                    tintColor = TintZip,
                    onAction = { picker.launch("application/zip") },
                    testTag = "extract_pick_button"
                )
            }
            extracted.isNotEmpty() -> ExtractResult(
                files = extracted,
                onShareAll = { shareFiles(context, extracted.map { it.uri }, "*/*") },
                onOpen = { file -> openFile(context, file.uri, null) },
                onDone = { viewModel.clearZipToExtract() },
                modifier = Modifier.weight(1f)
            )
            else -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 20.dp)
            ) {
                FileCard(
                    name = current.name,
                    details = if (entries.isEmpty()) "Leggo l'archivio…" else "${entries.size} file · ${formatBytes(current.sizeBytes)}",
                    icon = Icons.Default.FolderZip,
                    iconTint = TintZip,
                    onRemove = { viewModel.clearZipToExtract() },
                    modifier = Modifier.enterAnimation(0, key = current.id)
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    itemsIndexed(entries, key = { i, e -> "$i${e.first}" }) { index, entry ->
                        val kind = kindForFileName(entry.first)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .enterAnimation(index, key = entry.first)
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(kind.icon(), contentDescription = null, tint = kind.tint(), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(entry.first, fontSize = 14.sp, color = OmniText, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            if (entry.second > 0) Text(formatBytes(entry.second), fontSize = 12.sp, color = OmniTextMuted)
                        }
                    }
                }
                OmniActionBar(
                    summaryText = if (entries.size == 1) "1 file da estrarre" else "${entries.size} file da estrarre",
                    detailText = "In ${OutputStorage.displayPath(OutputFolder.ARCHIVES)}",
                    primaryButtonLabel = if (isWorking) "Estraggo…" else "Estrai tutto",
                    isEnabled = entries.isNotEmpty() && !isWorking,
                    onPrimaryAction = { viewModel.extractZip() },
                    testTag = "extract_zip_button",
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun ExtractResult(
    files: List<ArchiveService.ExtractedFile>,
    onShareAll: () -> Unit,
    onOpen: (ArchiveService.ExtractedFile) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                SuccessBadge(size = 84.dp)
                Text(
                    if (files.size == 1) "1 file estratto" else "${files.size} file estratti",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = OmniText,
                    modifier = Modifier.enterAnimation(2)
                )
                Text(
                    "Salvati in ${OutputStorage.displayPath(OutputFolder.ARCHIVES)}",
                    fontSize = 14.sp,
                    color = OmniTextMuted,
                    modifier = Modifier.enterAnimation(3)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .widthIn(max = 480.dp)
                        .fillMaxWidth()
                        .enterAnimation(4),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onShareAll,
                        colors = ButtonDefaults.buttonColors(containerColor = OmniAccent, contentColor = OmniOnAccent),
                        shape = RoundedCornerShape(RadiusMd),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Condividi tutti", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onDone,
                        shape = RoundedCornerShape(RadiusMd),
                        border = BorderStroke(1.dp, OmniBorder),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = OmniSurface, contentColor = OmniText),
                        modifier = Modifier.height(52.dp)
                    ) { Text("Fatto", fontWeight = FontWeight.Bold) }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        itemsIndexed(files, key = { i, f -> "$i${f.name}" }) { index, file ->
            val kind = kindForFileName(file.name)
            FileCard(
                name = file.name,
                details = formatBytes(file.sizeBytes),
                icon = kind.icon(),
                iconTint = kind.tint(),
                onClick = { onOpen(file) },
                trailingContent = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = OmniSuccess, modifier = Modifier.size(20.dp)) },
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .enterAnimation(index + 5, key = file.uri.toString())
            )
        }
    }
}

@Composable
private fun ArchiveFileRow(
    item: ArchiveItem,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val kind = kindForFileName(item.name)
    FileCard(
        name = item.name,
        details = formatBytes(item.sizeBytes),
        icon = if (kind == com.example.data.HistoryKind.OTHER) Icons.Default.Description else kind.icon(),
        iconTint = if (kind == com.example.data.HistoryKind.OTHER) TintZip else kind.tint(),
        onRemove = onRemove,
        modifier = modifier
    )
}
