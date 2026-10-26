package com.example.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.PdfCompressionLevel
import com.example.service.PdfImageFormat
import com.example.ui.components.AnimatedText
import com.example.ui.components.ContinueAction
import com.example.ui.components.ConversionResultView
import com.example.ui.components.Dropzone
import com.example.ui.components.FileCard
import com.example.ui.components.OmniActionBar
import com.example.ui.components.OmniProgressBanner
import com.example.ui.components.OmniToast
import com.example.ui.components.OptionRow
import com.example.ui.components.OptionSection
import com.example.ui.components.RadioCard
import com.example.ui.components.RevealVertically
import com.example.ui.components.SegmentedControl
import com.example.ui.components.ToolHeader
import com.example.ui.components.formatBytes
import com.example.ui.theme.Haptic
import com.example.ui.theme.LocalReduceMotion
import com.example.ui.theme.Motion
import com.example.ui.theme.OmniAccent
import com.example.ui.theme.OmniAccentSoft
import com.example.ui.theme.OmniBorder
import com.example.ui.theme.OmniDanger
import com.example.ui.theme.OmniOnAccent
import com.example.ui.theme.OmniSurface
import com.example.ui.theme.OmniSurface2
import com.example.ui.theme.OmniText
import com.example.ui.theme.OmniTextMuted
import com.example.ui.theme.RadiusCard
import com.example.ui.theme.RadiusLg
import com.example.ui.theme.RadiusMd
import com.example.ui.theme.RadiusPill
import com.example.ui.theme.RadiusSm
import com.example.ui.theme.TintImage
import com.example.ui.theme.TintMerge
import com.example.ui.theme.TintPdf
import com.example.ui.theme.enterAnimation
import com.example.ui.theme.motionSpec
import com.example.ui.theme.pressScale
import com.example.ui.theme.rememberHaptics
import com.example.util.OutputFolder
import com.example.util.OutputStorage
import com.example.viewmodel.LoadedPdf
import com.example.viewmodel.PageSlot
import com.example.viewmodel.PdfCompressViewModel
import com.example.viewmodel.PdfPagesViewModel

// -------------------------------------------------------------------------------------------------
// Comprimi PDF
// -------------------------------------------------------------------------------------------------

@Composable
fun PdfCompressScreen(
    viewModel: PdfCompressViewModel,
    modifier: Modifier = Modifier,
    onContinueWithMerge: (Uri) -> Unit = {},
    onContinueWithMarkdown: (Uri) -> Unit = {}
) {
    val source by viewModel.source.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isWorking by viewModel.isWorking.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val resultUri by viewModel.resultUri.collectAsState()
    val result by viewModel.result.collectAsState()
    val level by viewModel.level.collectAsState()
    val grayscale by viewModel.grayscale.collectAsState()
    val removeMetadata by viewModel.removeMetadata.collectAsState()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.loadPdf(uri)
    }

    AnimatedContent(
        targetState = resultUri != null,
        transitionSpec = { stageTransition(if (initialState) 1 else 0, if (targetState) 1 else 0) },
        modifier = modifier,
        label = "compress_stage"
    ) { showResult ->
        val uri = resultUri
        val res = result
        if (showResult && uri != null) {
            val saved = if (res != null && res.originalBytes > 0) ((res.originalBytes - res.outputBytes) * 100 / res.originalBytes).toInt() else 0
            ConversionResultView(
                title = if (saved > 0) "−$saved%: PDF più leggero" else "PDF ottimizzato",
                subtitle = if (res != null && res.imagesRecompressed > 0) "${res.imagesRecompressed} immagini ricompresse, testo intatto." else "Il documento era già ottimizzato.",
                outputUri = uri,
                mimeType = "application/pdf",
                previewIcon = Icons.Default.Compress,
                previewTint = TintPdf,
                originalBytes = res?.originalBytes ?: 0L,
                outputBytesOverride = res?.outputBytes,
                detailPrefix = source?.let { "${it.pageCount} pagine" },
                savedIn = "Salvato in ${OutputStorage.displayPath(OutputFolder.PDFS)}",
                onClose = { viewModel.dismissResult() },
                onNewConversion = { viewModel.clear() },
                continueActions = listOf(
                    ContinueAction("Unisci con altri PDF", Icons.Default.ViewModule, TintMerge) { onContinueWithMerge(uri) },
                    ContinueAction("Estrai il testo (Markdown)", Icons.Default.TextSnippet, TintPdf) { onContinueWithMarkdown(uri) }
                )
            )
        } else {
            SinglePdfScaffold(
                title = "Comprimi PDF",
                description = "Riduci il peso dei PDF scansionati",
                source = source,
                isLoading = isLoading,
                isWorking = isWorking,
                progress = progress,
                progressLabel = "Comprimo la pagina ${progress.first} di ${progress.second}…",
                errorMessage = errorMessage,
                onDismissError = { viewModel.dismissError() },
                onCancel = { viewModel.cancel() },
                onPick = { picker.launch("application/pdf") },
                onClear = { viewModel.clear() },
                dropIcon = Icons.Default.Compress,
                dropHeadline = "Scegli il PDF da comprimere",
                dropHelp = "Funziona al meglio con scansioni e PDF pieni di foto: il testo resta perfetto",
                tint = TintPdf
            ) { pdf ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Livello di compressione", style = MaterialTheme.typography.titleMedium, color = OmniText)
                    PdfCompressionLevel.entries.forEachIndexed { index, option ->
                        RadioCard(
                            title = option.label,
                            description = option.description,
                            isSelected = option == level,
                            onClick = { viewModel.setLevel(option) },
                            badgeText = if (option == PdfCompressionLevel.MEDIUM) "Consigliato" else null,
                            modifier = Modifier.enterAnimation(index + 1)
                        )
                    }
                    OptionRow(
                        label = "Bianco e nero",
                        description = "Converte le immagini in scala di grigi: ancora più leggero",
                        checked = grayscale,
                        onCheckedChange = { viewModel.setGrayscale(it) }
                    )
                    OptionRow(
                        label = "Rimuovi metadati",
                        description = "Elimina autore, parole chiave e dati XMP",
                        checked = removeMetadata,
                        onCheckedChange = { viewModel.setRemoveMetadata(it) }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OmniActionBar(
                        summaryText = "${pdf.pageCount} pagine · ${formatBytes(pdf.sizeBytes)}",
                        detailText = "Compressione ${level.label.lowercase()}" + if (grayscale) " · B/N" else "",
                        primaryButtonLabel = if (isWorking) "Comprimo…" else "Comprimi",
                        isEnabled = !isWorking,
                        onPrimaryAction = { viewModel.compress() },
                        testTag = "compress_pdf_button"
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Organizza pagine / PDF in immagini (same page grid)
// -------------------------------------------------------------------------------------------------

@Composable
fun PdfOrganizeScreen(
    viewModel: PdfPagesViewModel,
    modifier: Modifier = Modifier,
    onContinueWithCompress: (Uri) -> Unit = {}
) {
    PdfPagesScreen(viewModel = viewModel, imagesMode = false, modifier = modifier, onContinueWithCompress = onContinueWithCompress)
}

@Composable
fun PdfToImagesScreen(viewModel: PdfPagesViewModel, modifier: Modifier = Modifier) {
    PdfPagesScreen(viewModel = viewModel, imagesMode = true, modifier = modifier)
}

@Composable
private fun PdfPagesScreen(
    viewModel: PdfPagesViewModel,
    imagesMode: Boolean,
    modifier: Modifier = Modifier,
    onContinueWithCompress: (Uri) -> Unit = {}
) {
    val source by viewModel.source.collectAsState()
    val pages by viewModel.pages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isWorking by viewModel.isWorking.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val resultUri by viewModel.resultUri.collectAsState()
    val resultCount by viewModel.resultCount.collectAsState()
    val resultBytes by viewModel.resultBytes.collectAsState()
    val imageFormat by viewModel.imageFormat.collectAsState()
    val dpi by viewModel.dpi.collectAsState()
    val asZip by viewModel.asZip.collectAsState()
    val haptics = rememberHaptics()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.loadPdf(uri)
    }
    val selectedCount = pages.count { it.selected }

    AnimatedContent(
        targetState = resultUri != null,
        transitionSpec = { stageTransition(if (initialState) 1 else 0, if (targetState) 1 else 0) },
        modifier = modifier,
        label = "pages_stage"
    ) { showResult ->
        val uri = resultUri
        if (showResult && uri != null) {
            if (imagesMode) {
                ConversionResultView(
                    title = if (asZip) "Le tue immagini sono pronte" else "$resultCount immagini pronte",
                    subtitle = if (asZip) "$resultCount pagine raccolte in uno ZIP." else "Salvate in ${OutputStorage.displayPath(OutputFolder.IMAGES)}.",
                    outputUri = uri,
                    mimeType = if (asZip) "application/zip" else imageFormat.mimeType,
                    previewIcon = Icons.Default.Collections,
                    previewTint = TintImage,
                    outputBytesOverride = resultBytes.takeIf { it > 0 },
                    detailPrefix = "$resultCount pagine · $dpi DPI",
                    savedIn = "Salvato in ${OutputStorage.displayPath(if (asZip) OutputFolder.ARCHIVES else OutputFolder.IMAGES)}",
                    onClose = { viewModel.dismissResult() },
                    onNewConversion = { viewModel.clear() }
                )
            } else {
                ConversionResultView(
                    title = "Il tuo PDF è pronto",
                    subtitle = if (resultCount == 1) "1 pagina salvata nel nuovo PDF." else "$resultCount pagine salvate nel nuovo PDF.",
                    outputUri = uri,
                    mimeType = "application/pdf",
                    previewIcon = Icons.Default.ViewModule,
                    previewTint = TintMerge,
                    detailPrefix = "$resultCount pagine",
                    savedIn = "Salvato in ${OutputStorage.displayPath(OutputFolder.PDFS)}",
                    onClose = { viewModel.dismissResult() },
                    onNewConversion = { viewModel.clear() },
                    continueActions = listOf(
                        ContinueAction("Comprimi il PDF", Icons.Default.Compress, TintPdf) { onContinueWithCompress(uri) }
                    )
                )
            }
        } else {
            var showMenu by remember { mutableStateOf(false) }
            SinglePdfScaffold(
                title = if (imagesMode) "PDF in immagini" else "Organizza pagine",
                description = if (imagesMode) "Salva le pagine come JPG o PNG" else "Riordina, ruota, elimina o estrai pagine",
                source = source,
                isLoading = isLoading,
                isWorking = isWorking,
                progress = progress,
                progressLabel = if (imagesMode) "Esporto la pagina ${progress.first} di ${progress.second}…" else "Salvo la pagina ${progress.first} di ${progress.second}…",
                errorMessage = errorMessage,
                onDismissError = { viewModel.dismissError() },
                onCancel = { viewModel.cancel() },
                onPick = { picker.launch("application/pdf") },
                onClear = { viewModel.clear() },
                dropIcon = if (imagesMode) Icons.Default.Collections else Icons.Default.ViewModule,
                dropHeadline = "Scegli un PDF",
                dropHelp = if (imagesMode) "Ogni pagina diventa un'immagine nitida, pronta da condividere" else "Vedrai tutte le pagine: toccale per selezionarle",
                tint = if (imagesMode) TintImage else TintMerge,
                headerActions = {
                    if (source != null) {
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Altre azioni", tint = OmniText)
                            }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(text = { Text("Seleziona tutte") }, onClick = { showMenu = false; viewModel.selectAll(true) })
                                DropdownMenuItem(text = { Text("Pagine dispari") }, onClick = { showMenu = false; viewModel.selectParity(odd = true) })
                                DropdownMenuItem(text = { Text("Pagine pari") }, onClick = { showMenu = false; viewModel.selectParity(odd = false) })
                                DropdownMenuItem(text = { Text("Deseleziona") }, onClick = { showMenu = false; viewModel.selectAll(false) })
                                if (!imagesMode) {
                                    DropdownMenuItem(
                                        text = { Text("Inverti ordine") },
                                        leadingIcon = { Icon(Icons.Default.SwapVert, contentDescription = null) },
                                        onClick = { showMenu = false; viewModel.reverse() }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Ripristina originale") },
                                        leadingIcon = { Icon(Icons.Default.Restore, contentDescription = null) },
                                        onClick = { showMenu = false; viewModel.resetPages() }
                                    )
                                }
                            }
                        }
                    }
                }
            ) { pdf ->
                Column(modifier = Modifier.fillMaxSize()) {
                    AnimatedText(
                        text = when {
                            selectedCount > 0 -> "$selectedCount selezionate su ${pages.size}"
                            imagesMode -> "Tocca le pagine da esportare (nessuna = tutte)"
                            else -> "${pages.size} pagine · tocca per selezionare"
                        },
                        fontSize = 14.sp,
                        color = OmniTextMuted,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 104.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        itemsIndexed(pages, key = { _, slot -> slot.key }) { index, slot ->
                            PdfPageTile(
                                viewModel = viewModel,
                                slot = slot,
                                position = index + 1,
                                onClick = {
                                    haptics(Haptic.TICK)
                                    viewModel.toggleSelected(slot.key)
                                },
                                modifier = Modifier
                                    .animateItem(placementSpec = spring(dampingRatio = 0.8f, stiffness = 380f))
                                    .enterAnimation(index, key = "${pdf.uri}${slot.key}")
                            )
                        }
                    }

                    if (imagesMode) {
                        ImagesOptions(
                            format = imageFormat,
                            dpi = dpi,
                            asZip = asZip,
                            onFormat = viewModel::setImageFormat,
                            onDpi = viewModel::setDpi,
                            onZip = viewModel::setAsZip
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        val count = if (selectedCount > 0) selectedCount else pages.size
                        OmniActionBar(
                            summaryText = if (count == 1) "1 pagina → ${imageFormat.label}" else "$count pagine → ${imageFormat.label}",
                            detailText = "$dpi DPI" + if (asZip) " · in uno ZIP" else "",
                            primaryButtonLabel = if (isWorking) "Esporto…" else "Esporta",
                            isEnabled = !isWorking && pages.isNotEmpty(),
                            onPrimaryAction = { viewModel.exportImages() },
                            testTag = "export_images_button"
                        )
                    } else {
                        AnimatedVisibility(
                            visible = selectedCount > 0,
                            enter = fadeIn() + scaleIn(initialScale = 0.9f),
                            exit = fadeOut() + scaleOut(targetScale = 0.9f)
                        ) {
                            PagesToolbar(
                                selectedCount = selectedCount,
                                canMove = selectedCount == 1,
                                onRotate = { viewModel.rotateSelected() },
                                onDelete = {
                                    haptics(Haptic.REJECT)
                                    viewModel.deleteSelected()
                                },
                                onMoveLeft = {
                                    val i = pages.indexOfFirst { it.selected }
                                    viewModel.move(i, i - 1)
                                },
                                onMoveRight = {
                                    val i = pages.indexOfFirst { it.selected }
                                    viewModel.move(i, i + 1)
                                },
                                onExtract = { viewModel.exportPdf(onlySelected = true) }
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        val changed = pages.size != pdf.pageCount || pages.withIndex().any { (i, p) -> p.sourceIndex != i || p.rotation != 0 }
                        OmniActionBar(
                            summaryText = "${pages.size} pagine",
                            detailText = if (changed) "Modifiche da salvare" else "Nessuna modifica",
                            primaryButtonLabel = if (isWorking) "Salvo…" else "Salva PDF",
                            isEnabled = !isWorking && pages.isNotEmpty() && changed,
                            disabledHelperText = if (pages.isEmpty()) "Hai eliminato tutte le pagine" else "Ruota, sposta o elimina qualche pagina",
                            onPrimaryAction = { viewModel.exportPdf(onlySelected = false) },
                            testTag = "save_organized_button"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ImagesOptions(
    format: PdfImageFormat,
    dpi: Int,
    asZip: Boolean,
    onFormat: (PdfImageFormat) -> Unit,
    onDpi: (Int) -> Unit,
    onZip: (Boolean) -> Unit
) {
    val dpis = listOf(96, 150, 220, 300)
    Surface(
        shape = RoundedCornerShape(RadiusCard),
        color = OmniSurface,
        border = BorderStroke(1.dp, OmniBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    OptionSection(title = "Formato") {
                        SegmentedControl(
                            options = PdfImageFormat.entries.map { it.label },
                            selectedIndex = format.ordinal,
                            onOptionSelected = { onFormat(PdfImageFormat.entries[it]) }
                        )
                    }
                }
                Box(modifier = Modifier.weight(1.4f)) {
                    OptionSection(title = "Risoluzione (DPI)") {
                        SegmentedControl(
                            options = dpis.map { it.toString() },
                            selectedIndex = dpis.indexOf(dpi).coerceAtLeast(0),
                            onOptionSelected = { onDpi(dpis[it]) }
                        )
                    }
                }
            }
            OptionRow(
                label = "Raccogli in uno ZIP",
                description = "Un unico file da condividere invece di tante immagini",
                checked = asZip,
                onCheckedChange = onZip
            )
        }
    }
}

@Composable
private fun PagesToolbar(
    selectedCount: Int,
    canMove: Boolean,
    onRotate: () -> Unit,
    onDelete: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onExtract: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(RadiusCard),
        color = OmniSurface,
        border = BorderStroke(1.dp, OmniBorder),
        shadowElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PagesToolbarAction(Icons.AutoMirrored.Filled.ArrowBack, "Sposta a sinistra", enabled = canMove, iconOnly = true, onClick = onMoveLeft)
            PagesToolbarAction(Icons.AutoMirrored.Filled.ArrowForward, "Sposta a destra", enabled = canMove, iconOnly = true, onClick = onMoveRight)
            PagesToolbarAction(Icons.AutoMirrored.Filled.RotateRight, "Ruota", onClick = onRotate)
            PagesToolbarAction(Icons.Default.PictureAsPdf, if (selectedCount == 1) "Estrai" else "Estrai $selectedCount", onClick = onExtract)
            PagesToolbarAction(Icons.Default.DeleteOutline, "Elimina", tint = OmniDanger, onClick = onDelete)
        }
    }
}

@Composable
private fun PagesToolbarAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    iconOnly: Boolean = false,
    tint: Color = OmniText
) {
    val interaction = remember { MutableInteractionSource() }
    val color = if (enabled) tint else OmniTextMuted.copy(alpha = 0.4f)
    Surface(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interaction,
        shape = RoundedCornerShape(RadiusMd),
        color = Color.Transparent,
        modifier = Modifier
            .height(48.dp)
            .pressScale(interaction, 0.9f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (iconOnly) 10.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(22.dp))
            if (!iconOnly) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(label, color = color, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun PdfPageTile(
    viewModel: PdfPagesViewModel,
    slot: PageSlot,
    position: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val thumbnail by produceState<Bitmap?>(initialValue = null, slot.sourceIndex) {
        value = viewModel.thumbnail(slot.sourceIndex)
    }
    val shape = RoundedCornerShape(RadiusLg)
    val interaction = remember { MutableInteractionSource() }
    val borderColor by animateColorAsState(if (slot.selected) OmniAccent else OmniBorder, label = "page_border")
    val lift by animateFloatAsState(if (slot.selected) 0.94f else 1f, motionSpec(Motion.bouncy()), label = "page_lift")
    val reduce = LocalReduceMotion.current
    val rotation = remember { Animatable(slot.rotation.toFloat()) }
    LaunchedEffect(slot.rotation) {
        val delta = ((slot.rotation - rotation.value % 360f) + 360f) % 360f
        if (delta == 0f) return@LaunchedEffect
        if (reduce) rotation.snapTo(rotation.value + delta) else rotation.animateTo(rotation.value + delta, spring(dampingRatio = 0.7f, stiffness = 300f))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.74f)
            .pressScale(interaction, 0.94f)
            .clip(shape)
            .background(if (slot.selected) OmniAccentSoft else OmniSurface)
            .border(if (slot.selected) 2.dp else 1.dp, borderColor, shape)
            .clickable(interactionSource = interaction, indication = LocalIndication.current, onClick = onClick)
            .testTag("pdf_page_$position")
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .graphicsLayer {
                    scaleX = lift
                    scaleY = lift
                    rotationZ = rotation.value
                },
            contentAlignment = Alignment.Center
        ) {
            val bmp = thumbnail
            AnimatedContent(targetState = bmp != null, transitionSpec = { fadeIn(tween(Motion.MEDIUM)) togetherWith fadeOut() }, label = "thumb") { loaded ->
                if (loaded && bmp != null) {
                    Image(
                        bitmap = remember(bmp) { bmp.asImageBitmap() },
                        contentDescription = "Pagina ${slot.sourceIndex + 1}",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(RadiusSm))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(RadiusSm))
                            .background(OmniSurface2),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = OmniTextMuted)
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp)
                .clip(RoundedCornerShape(RadiusPill))
                .background(Color(0xFF111827))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text("${slot.sourceIndex + 1}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        AnimatedVisibility(
            visible = slot.selected,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
            enter = scaleIn(motionSpec(Motion.bouncy())) + fadeIn(),
            exit = scaleOut(tween(Motion.SHORT)) + fadeOut(tween(Motion.SHORT))
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(OmniAccent),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = OmniOnAccent, modifier = Modifier.size(16.dp))
            }
        }
    }
}

/** Shared frame of the single-PDF tools: header, banners, picker dropzone and the loaded file card. */
@Composable
internal fun SinglePdfScaffold(
    title: String,
    description: String,
    source: LoadedPdf?,
    isLoading: Boolean,
    isWorking: Boolean,
    progress: Pair<Int, Int>,
    progressLabel: String,
    errorMessage: String?,
    onDismissError: () -> Unit,
    onCancel: () -> Unit,
    onPick: () -> Unit,
    onClear: () -> Unit,
    dropIcon: ImageVector,
    dropHeadline: String,
    dropHelp: String,
    tint: Color,
    headerActions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
    content: @Composable (LoadedPdf) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        RevealVertically(visible = errorMessage != null) {
            OmniToast(
                message = errorMessage.orEmpty(),
                isError = true,
                actionLabel = "Chiudi",
                onAction = onDismissError,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }
        RevealVertically(visible = isWorking) {
            val p = if (progress.second > 0) progress.first.toFloat() / progress.second else 0f
            OmniProgressBanner(
                progress = p,
                statusText = progressLabel,
                onCancel = onCancel,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            ToolHeader(title = title, description = description, actions = headerActions)
            AnimatedContent(
                targetState = source,
                transitionSpec = { stageTransition(if (initialState == null) 0 else 1, if (targetState == null) 0 else 1) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                label = "single_pdf_source"
            ) { pdf ->
                if (pdf == null) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Dropzone(
                            icon = dropIcon,
                            headline = dropHeadline,
                            helpText = dropHelp,
                            supportedFormats = "PDF",
                            actionLabel = if (isLoading) "Apro il PDF…" else "Scegli PDF",
                            tintColor = tint,
                            onAction = onPick,
                            testTag = "single_pdf_pick_button"
                        )
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        FileCard(
                            name = pdf.name,
                            details = if (pdf.pageCount > 0) "${pdf.pageCount} pagine · ${formatBytes(pdf.sizeBytes)}" else "Protetto da password · ${formatBytes(pdf.sizeBytes)}",
                            icon = Icons.Default.PictureAsPdf,
                            iconTint = tint,
                            trailingContent = {
                                TextButton(onClick = onPick) { Text("Cambia", color = OmniAccent, fontWeight = FontWeight.SemiBold) }
                            },
                            onRemove = onClear,
                            modifier = Modifier
                                .padding(bottom = 12.dp)
                                .enterAnimation(0, key = pdf.uri)
                        )
                        Box(modifier = Modifier.weight(1f)) { content(pdf) }
                    }
                }
            }
        }
    }
}
