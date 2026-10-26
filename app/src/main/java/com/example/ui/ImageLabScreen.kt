package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.ImageConvertItem
import com.example.model.ImageFormat
import com.example.ui.components.Dropzone
import com.example.ui.components.FileCard
import com.example.ui.components.FileListHeader
import com.example.ui.components.OmniActionBar
import com.example.ui.components.OmniProgressBanner
import com.example.ui.components.OmniSlider
import com.example.ui.components.OmniToast
import com.example.ui.components.OptionRow
import com.example.ui.components.OptionSection
import com.example.ui.components.RadioCard
import com.example.ui.components.RevealVertically
import com.example.ui.components.SegmentedControl
import com.example.ui.components.ToolLayout
import com.example.ui.components.formatBytes
import com.example.ui.components.openFile
import com.example.ui.components.rememberCameraCapture
import com.example.ui.theme.OmniAccent
import com.example.ui.theme.OmniDanger
import com.example.ui.theme.OmniSuccess
import com.example.ui.theme.OmniText
import com.example.ui.theme.OmniTextMuted
import com.example.ui.theme.TintImage
import com.example.ui.theme.enterAnimation
import com.example.util.OutputFolder
import com.example.util.OutputStorage
import com.example.viewmodel.ImageLabViewModel

@Composable
fun ImageLabScreen(
    viewModel: ImageLabViewModel,
    modifier: Modifier = Modifier
) {
    val items by viewModel.items.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val context = LocalContext.current

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) viewModel.addImages(uris)
    }
    val launchCamera = rememberCameraCapture { uri -> viewModel.addImages(listOf(uri)) }

    val scaleOptions = listOf("100%", "75%", "50%", "25%")
    val scaleValues = listOf(100, 75, 50, 25)
    val selectedScaleIndex = scaleValues.indexOf(settings.resolutionScalePercent).coerceAtLeast(0)
    val maxSideOptions = listOf("Nessuno", "1080", "1600", "2560")
    val maxSideValues = listOf(0, 1080, 1600, 2560)
    val selectedMaxSide = maxSideValues.indexOf(settings.maxSidePx).coerceAtLeast(0)

    val convertible = items.filter { it.originalWidth > 0 }
    val convertedCount = items.count { it.convertedSizeBytes != null }
    val isAllConverted = convertible.isNotEmpty() && convertible.all { it.convertedSizeBytes != null }
    val savedBytes = items.filter { it.convertedSizeBytes != null }.sumOf { it.originalSizeBytes - (it.convertedSizeBytes ?: 0L) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("image_lab_screen")
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

        RevealVertically(visible = isAllConverted && !isProcessing) {
            OmniToast(
                message = buildString {
                    append(if (convertedCount == 1) "1 immagine salvata" else "$convertedCount immagini salvate")
                    append(" in ").append(OutputStorage.displayPath(OutputFolder.IMAGES))
                    if (savedBytes > 0) append(" · −").append(formatBytes(savedBytes))
                },
                actionLabel = "Svuota",
                onAction = { viewModel.clearAll() },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }

        RevealVertically(visible = isProcessing) {
            val total = convertible.size.coerceAtLeast(1)
            OmniProgressBanner(
                progress = convertedCount.toFloat() / total,
                statusText = "Converto ${(convertedCount + 1).coerceAtMost(total)} di $total immagini…",
                onCancel = { viewModel.cancel() },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }

        ToolLayout(
            title = "Converti immagini",
            description = "Cambia formato, comprimi e ridimensiona",
            isEmpty = items.isEmpty(),
            phoneTabTitles = listOf("Immagini · ${items.size}", "Opzioni"),
            emptyDropzone = {
                Dropzone(
                    icon = Icons.Default.Image,
                    headline = "Aggiungi le tue immagini",
                    helpText = "Scegli le immagini da convertire in JPEG, WebP o PNG con qualità e dimensioni personalizzate",
                    supportedFormats = "JPG · PNG · WEBP · HEIC · BMP",
                    actionLabel = "Scegli file",
                    tintColor = TintImage,
                    onAction = { pickerLauncher.launch("image/*") },
                    testTag = "image_add_button",
                    secondaryIcon = Icons.Default.CameraAlt,
                    onSecondaryAction = launchCamera
                )
            },
            fileList = {
                Column(modifier = Modifier.fillMaxSize()) {
                    FileListHeader(
                        title = if (items.size == 1) "1 immagine" else "${items.size} immagini",
                        onAdd = { pickerLauncher.launch("image/*") },
                        onClear = { viewModel.clearAll() },
                        addIcon = Icons.Default.AddPhotoAlternate
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                            ImageFileRow(
                                item = item,
                                onOpenResult = { item.outputUri?.let { openFile(context, it, settings.targetFormat.mimeType) } },
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
                        text = "Formato e qualità",
                        style = MaterialTheme.typography.titleMedium,
                        color = OmniText
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        RadioCard(
                            title = "JPEG",
                            description = "Formato universale, compatibile ovunque",
                            isSelected = settings.targetFormat == ImageFormat.JPEG,
                            onClick = { viewModel.updateSettings(settings.copy(targetFormat = ImageFormat.JPEG)) },
                            badgeText = "Con perdita"
                        )
                        RadioCard(
                            title = "WebP (con perdita)",
                            description = "25-35% più leggero del JPEG a parità di qualità",
                            isSelected = settings.targetFormat == ImageFormat.WEBP_LOSSY,
                            onClick = { viewModel.updateSettings(settings.copy(targetFormat = ImageFormat.WEBP_LOSSY)) },
                            badgeText = "Consigliato"
                        )
                        RadioCard(
                            title = "PNG",
                            description = "Senza perdita, con trasparenza",
                            isSelected = settings.targetFormat == ImageFormat.PNG,
                            onClick = { viewModel.updateSettings(settings.copy(targetFormat = ImageFormat.PNG)) },
                            badgeText = "Senza perdita"
                        )
                        RadioCard(
                            title = "WebP (senza perdita)",
                            description = "Senza perdita, più leggero del PNG",
                            isSelected = settings.targetFormat == ImageFormat.WEBP_LOSSLESS,
                            onClick = { viewModel.updateSettings(settings.copy(targetFormat = ImageFormat.WEBP_LOSSLESS)) },
                            badgeText = "Senza perdita"
                        )
                    }

                    AnimatedVisibility(
                        visible = !settings.targetFormat.isLossless,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        OmniSlider(
                            label = "Qualità",
                            value = settings.qualityPercent.toFloat(),
                            onValueChange = { viewModel.updateSettings(settings.copy(qualityPercent = it.toInt())) },
                            valueRange = 10f..100f,
                            valueFormatter = { "${it.toInt()}%" }
                        )
                    }

                    OptionSection(title = "Risoluzione") {
                        SegmentedControl(
                            options = scaleOptions,
                            selectedIndex = selectedScaleIndex,
                            onOptionSelected = { idx ->
                                viewModel.updateSettings(settings.copy(resolutionScalePercent = scaleValues[idx]))
                            }
                        )
                    }

                    OptionSection(title = "Lato massimo (px)", hint = "Utile per caricare foto su siti con limiti di dimensione") {
                        SegmentedControl(
                            options = maxSideOptions,
                            selectedIndex = selectedMaxSide,
                            onOptionSelected = { idx -> viewModel.updateSettings(settings.copy(maxSidePx = maxSideValues[idx])) }
                        )
                    }

                    OptionRow(
                        label = "Rimuovi metadati EXIF",
                        description = if (settings.stripExif) "Rimuove posizione GPS e dati della fotocamera" else "Mantiene data, fotocamera e GPS (solo JPEG)",
                        checked = settings.stripExif,
                        onCheckedChange = { viewModel.updateSettings(settings.copy(stripExif = it)) }
                    )
                }
            },
            actionBar = {
                val formatName = when (settings.targetFormat) {
                    ImageFormat.JPEG -> "JPEG"
                    ImageFormat.WEBP_LOSSY -> "WebP"
                    ImageFormat.PNG -> "PNG"
                    ImageFormat.WEBP_LOSSLESS -> "WebP LL"
                }
                val pending = convertible.count { it.convertedSizeBytes == null }
                OmniActionBar(
                    summaryText = if (items.size == 1) "1 immagine · $formatName" else "${items.size} immagini · $formatName",
                    detailText = listOfNotNull(
                        "${settings.qualityPercent}%".takeIf { !settings.targetFormat.isLossless },
                        "${settings.resolutionScalePercent}% risoluzione",
                        "max ${settings.maxSidePx}px".takeIf { settings.maxSidePx > 0 }
                    ).joinToString(" · "),
                    primaryButtonLabel = if (isProcessing) "Conversione…" else if (pending == 0 && convertible.isNotEmpty()) "Fatto" else "Converti",
                    isEnabled = pending > 0 && !isProcessing,
                    disabledHelperText = when {
                        items.isEmpty() -> "Aggiungi almeno un'immagine"
                        pending == 0 && convertible.isNotEmpty() -> "Cambia le opzioni per convertire di nuovo"
                        else -> null
                    },
                    onPrimaryAction = { viewModel.convertAll() },
                    testTag = "convert_all_images_button"
                )
            }
        )
    }
}

@Composable
private fun ImageFileRow(
    item: ImageConvertItem,
    onOpenResult: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dims = if (item.originalWidth > 0) " · ${item.originalWidth}×${item.originalHeight}" else ""
    val details = when {
        item.error != null -> item.error
        item.convertedSizeBytes != null -> {
            val saved = item.originalSizeBytes - item.convertedSizeBytes
            val pct = if (item.originalSizeBytes > 0) (saved * 100 / item.originalSizeBytes).toInt() else 0
            "${formatBytes(item.originalSizeBytes)} → ${formatBytes(item.convertedSizeBytes)}" + if (pct > 0) " (−$pct%)" else ""
        }
        else -> formatBytes(item.originalSizeBytes) + dims
    }
    FileCard(
        name = item.name,
        details = details,
        detailsColor = when {
            item.error != null -> OmniDanger
            item.convertedSizeBytes != null -> OmniSuccess
            else -> OmniTextMuted
        },
        thumbnail = {
            AsyncImage(
                model = item.uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        },
        onClick = if (item.outputUri != null) onOpenResult else null,
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                when {
                    item.isProcessing -> CircularProgressIndicator(modifier = Modifier.size(20.dp), color = OmniAccent, strokeWidth = 2.dp)
                    item.convertedSizeBytes != null -> Icon(Icons.Default.CheckCircle, contentDescription = "Convertita", tint = OmniSuccess, modifier = Modifier.size(22.dp).enterAnimation(0, key = "done_${item.id}"))
                    item.error != null -> Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = OmniDanger, modifier = Modifier.size(22.dp))
                }
                if (item.outputUri != null) {
                    IconButton(onClick = onOpenResult, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.OpenInNew, contentDescription = "Apri", tint = OmniTextMuted, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(modifier = Modifier.width(2.dp))
            }
        },
        onRemove = onRemove,
        modifier = modifier
    )
}
