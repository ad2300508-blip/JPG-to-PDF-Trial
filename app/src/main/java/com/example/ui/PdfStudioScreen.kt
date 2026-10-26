package com.example.ui

import androidx.compose.animation.ExitTransition
import androidx.compose.animation.EnterTransition
import com.example.data.AppPreferences
import com.example.ui.components.rememberOrderedImagePicker
import androidx.activity.result.PickVisualMediaRequest
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.example.model.PdfQuality
import com.example.ui.components.AnimatedText
import com.example.ui.components.CropDialog
import com.example.ui.components.MorphTextField
import com.example.ui.components.OptionSection
import com.example.ui.components.RevealVertically
import com.example.ui.theme.Haptic
import com.example.ui.theme.LocalReduceMotion
import com.example.ui.theme.Motion
import com.example.ui.theme.enterAnimation
import com.example.ui.theme.motionSpec
import com.example.ui.theme.pressScale
import com.example.ui.theme.rememberHaptics
import com.example.util.OutputFolder
import com.example.util.OutputStorage
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MergeType
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.PdfDocumentItem
import com.example.model.PdfExportConfig
import com.example.ui.components.ContinueAction
import com.example.ui.components.ConversionResultView
import com.example.ui.components.Dropzone
import com.example.ui.components.IconTile
import com.example.ui.components.OmniActionBar
import com.example.ui.components.OmniToast
import com.example.ui.components.OptionRow
import com.example.ui.components.SegmentedControl
import com.example.ui.components.ToolHeader
import com.example.ui.components.ToolLayout
import com.example.ui.components.formatBytes
import com.example.ui.components.rememberCameraCapture
import com.example.ui.theme.OmniAccent
import com.example.ui.theme.OmniBg
import com.example.ui.theme.OmniAccentSoft
import com.example.ui.theme.OmniBorder
import com.example.ui.theme.OmniDanger
import com.example.ui.theme.OmniOnAccent
import com.example.ui.theme.OmniSuccess
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
import com.example.viewmodel.PdfStudioViewModel

@Composable
fun PdfStudioScreen(
    viewModel: PdfStudioViewModel,
    modifier: Modifier = Modifier,
    onContinueWithMerge: (Uri) -> Unit = {},
    onContinueWithMarkdown: (Uri) -> Unit = {},
    onContinueWithCompress: (Uri) -> Unit = {}
) {
    val items by viewModel.items.collectAsState()
    val config by viewModel.config.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val progress by viewModel.generationProgress.collectAsState()
    val lastGeneratedUri by viewModel.lastGeneratedUri.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val stage = when {
        lastGeneratedUri != null -> StudioStage.RESULT
        isGenerating -> StudioStage.PROGRESS
        else -> StudioStage.EDITOR
    }
    AnimatedContent(
        targetState = stage,
        transitionSpec = { stageTransition(initialState.ordinal, targetState.ordinal) },
        modifier = modifier,
        label = "pdf_studio_stage"
    ) { target ->
        when (target) {
            StudioStage.RESULT -> {
                val resultUri = lastGeneratedUri
                if (resultUri != null) {
                    ConversionResultView(
                        title = "Il tuo PDF è pronto",
                        subtitle = if (items.size == 1) "1 immagine trasformata in PDF." else "${items.size} immagini unite in un unico file.",
                        outputUri = resultUri,
                        mimeType = "application/pdf",
                        previewIcon = Icons.Default.Description,
                        previewTint = TintImage,
                        originalBytes = items.sumOf { it.sizeBytes },
                        detailPrefix = if (items.size == 1) "1 pagina" else "${items.size} pagine",
                        onClose = { viewModel.backToEditor() },
                        onNewConversion = { viewModel.clearAll() },
                        savedIn = "Salvato in ${OutputStorage.displayPath(OutputFolder.PDFS)}",
                        continueActions = listOf(
                            ContinueAction("Unisci con altri PDF", Icons.AutoMirrored.Filled.MergeType, TintMerge) {
                                onContinueWithMerge(resultUri)
                            },
                            ContinueAction("Comprimi ancora", Icons.Default.Compress, TintPdf) {
                                onContinueWithCompress(resultUri)
                            },
                            ContinueAction("Estrai il testo (Markdown)", Icons.Default.TextSnippet, TintPdf) {
                                onContinueWithMarkdown(resultUri)
                            }
                        )
                    )
                }
            }
            StudioStage.PROGRESS -> GenerationProgressView(
                items = items,
                config = config,
                current = progress.first,
                total = progress.second.coerceAtLeast(items.size),
                onCancel = { viewModel.cancelGeneration() }
            )
            StudioStage.EDITOR -> PdfEditor(
                viewModel = viewModel,
                items = items,
                config = config,
                errorMessage = errorMessage
            )
        }
    }
}

private enum class StudioStage { EDITOR, PROGRESS, RESULT }

/** Forward stages slide/scale in, going back fades the previous stage away. */
internal fun stageTransition(from: Int, to: Int): ContentTransform {
    // Read directly: transition specs are not composable, so LocalReduceMotion is out of reach here.
    if (AppPreferences.reduceMotion.value) return ContentTransform(EnterTransition.None, ExitTransition.None)
    val forward = to > from
    val enter = fadeIn(tween(Motion.MEDIUM, delayMillis = 60)) +
        scaleIn(initialScale = if (forward) 0.94f else 1.04f, animationSpec = tween(Motion.LONG, easing = Motion.EmphasizedDecelerate))
    val exit = fadeOut(tween(Motion.SHORT)) +
        scaleOut(targetScale = if (forward) 1.04f else 0.96f, animationSpec = tween(Motion.MEDIUM))
    return ContentTransform(enter, exit, sizeTransform = SizeTransform(clip = false))
}

@Composable
private fun PdfEditor(
    viewModel: PdfStudioViewModel,
    items: List<PdfDocumentItem>,
    config: PdfExportConfig,
    errorMessage: String?,
    modifier: Modifier = Modifier
) {
    var showMetadataModal by rememberSaveable { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var cropTargetId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedIndex = items.indexOfFirst { it.id == selectedId }
    val lastRemoved by viewModel.lastRemoved.collectAsState()
    val haptics = rememberHaptics()

    // The system photo picker numbers the photos as you tap them and returns them in that order,
    // so the PDF pages follow the selection order. The file browser cannot guarantee it.
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) viewModel.addImages(uris)
    }
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) viewModel.addImages(uris)
    }
    // In-app gallery: photos are numbered in tap order and come back exactly in that order.
    val pickFromGallery = rememberOrderedImagePicker(
        onPicked = { viewModel.addImages(it) },
        fallback = { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    )
    val launchCamera = rememberCameraCapture { uri -> viewModel.addImages(listOf(uri)) }

    val pageSizes = listOf("A4", "Letter", "Original")
    val pageSizeLabels = listOf("A4", "Lettera", "Originale")
    val selectedPageSizeIndex = pageSizes.indexOf(config.pageSize).coerceAtLeast(0)

    val orientations = listOf("Auto", "Portrait", "Landscape")
    val orientationLabels = listOf("Auto", "Verticale", "Orizzontale")
    val selectedOrientationIndex = orientations.indexOf(config.orientation).coerceAtLeast(0)

    val marginLabels = listOf("Nessuno", "Normale", "Ampio")
    val marginValues = listOf(0, 10, 20)
    val selectedMarginIndex = marginValues.indexOf(config.marginMm).let { if (it >= 0) it else 1 }

    val estimatedBytes = estimatePdfBytes(items, config)
    val pageSizeLabel = pageSizeLabels[selectedPageSizeIndex]

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("pdf_studio_screen")
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
        RevealVertically(visible = lastRemoved != null) {
            OmniToast(
                message = "Pagina eliminata",
                actionLabel = "Annulla",
                onAction = {
                    haptics(Haptic.TAP)
                    viewModel.undoRemove()
                },
                onDismiss = { viewModel.dismissUndo() },
                autoDismissMillis = 5000,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }

        ToolLayout(
            title = "Immagini in PDF",
            description = "Scansioni e foto in un PDF pulito",
            isEmpty = items.isEmpty(),
            phoneTabTitles = listOf("Pagine · ${items.size}", "Opzioni"),
            headerActions = {
                if (items.size > 1) {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Altre azioni", tint = OmniText)
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Ordina per nome") },
                                leadingIcon = { Icon(Icons.Default.SortByAlpha, contentDescription = null) },
                                onClick = { showMenu = false; viewModel.sortByName() }
                            )
                            DropdownMenuItem(
                                text = { Text("Ordina per data di scatto") },
                                leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null) },
                                onClick = { showMenu = false; viewModel.sortByDateTaken() }
                            )
                            DropdownMenuItem(
                                text = { Text("Inverti ordine") },
                                leadingIcon = { Icon(Icons.Default.SwapVert, contentDescription = null) },
                                onClick = { showMenu = false; viewModel.reverseOrder() }
                            )
                            DropdownMenuItem(
                                text = { Text("Ruota tutte le pagine") },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = null) },
                                onClick = { showMenu = false; viewModel.rotateAll() }
                            )
                        }
                    }
                }
            },
            emptyDropzone = {
                Dropzone(
                    icon = Icons.Default.AddPhotoAlternate,
                    headline = "Aggiungi le tue immagini",
                    helpText = "Scegli scansioni, foto o pagine di quaderno: le unisco in un PDF impaginato.",
                    supportedFormats = "JPG · PNG · WEBP · HEIC",
                    actionLabel = "Scegli foto",
                    tintColor = OmniAccent,
                    onAction = pickFromGallery,
                    testTag = "pdf_add_images_button",
                    secondaryIcon = Icons.Default.CameraAlt,
                    onSecondaryAction = launchCamera,
                    footer = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Le pagine seguono l'ordine in cui tocchi le foto (1, 2, 3…)",
                                fontSize = 13.sp,
                                color = OmniTextMuted,
                                textAlign = TextAlign.Center
                            )
                            TextButton(onClick = { pickerLauncher.launch("image/*") }) {
                                Text("Oppure scegli da File", color = OmniAccent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                )
            },
            fileList = {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tocca una pagina per modificarla. Usa le frecce per riordinarla.",
                            fontSize = 14.sp,
                            color = OmniTextMuted,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = {
                            selectedId = null
                            viewModel.clearAll()
                        }) {
                            Text("Svuota", color = OmniTextMuted, fontSize = 13.sp)
                        }
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 100.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                            PageTile(
                                item = item,
                                pageNumber = index + 1,
                                selected = item.id == selectedId,
                                onClick = {
                                    haptics(Haptic.TICK)
                                    selectedId = if (selectedId == item.id) null else item.id
                                },
                                modifier = Modifier
                                    .animateItem(
                                        fadeInSpec = tween(Motion.MEDIUM),
                                        placementSpec = spring(dampingRatio = 0.8f, stiffness = 380f),
                                        fadeOutSpec = tween(Motion.SHORT)
                                    )
                                    .enterAnimation(index, key = item.id)
                            )
                        }
                        item(key = "add_tile") {
                            AddPageTile(
                                modifier = Modifier.animateItem(),
                                onAdd = pickFromGallery,
                                onCamera = launchCamera
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = selectedIndex >= 0,
                        enter = slideInVertically(motionSpec(Motion.gentle())) { it } + fadeIn(),
                        exit = slideOutVertically(tween(Motion.SHORT)) { it } + fadeOut(tween(Motion.SHORT))
                    ) {
                        val selected = items.getOrNull(selectedIndex)
                        if (selected != null) {
                            Column {
                                Spacer(modifier = Modifier.height(10.dp))
                                PageToolbar(
                                    item = selected,
                                    canMoveLeft = selectedIndex > 0,
                                    canMoveRight = selectedIndex < items.size - 1,
                                    onRotate = { viewModel.rotateItem(selected.id) },
                                    onCrop = { cropTargetId = selected.id },
                                    onSplit = { viewModel.toggleSpreadSplit(selected.id) },
                                    onClean = { viewModel.toggleBinarization(selected.id) },
                                    onDuplicate = { viewModel.duplicateItem(selected.id) },
                                    onMoveLeft = { viewModel.moveItem(selectedIndex, selectedIndex - 1) },
                                    onMoveRight = { viewModel.moveItem(selectedIndex, selectedIndex + 1) },
                                    onDelete = {
                                        haptics(Haptic.REJECT)
                                        viewModel.removeItem(selected.id)
                                        selectedId = null
                                    }
                                )
                            }
                        }
                    }
                }
            },
            optionsPanel = {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Text(
                        text = "Impostazioni",
                        style = MaterialTheme.typography.titleLarge,
                        color = OmniText
                    )

                    Column {
                        MorphTextField(
                            value = config.title,
                            onValueChange = { viewModel.updateConfig(config.copy(title = it)) },
                            label = "Nome del PDF",
                            placeholder = "Documento",
                            testTag = "pdf_title_input"
                        )
                        OptionHint("Usato come nome del file e titolo nei metadati")
                    }

                    OptionSection(title = "Qualità", hint = config.quality.description) {
                        SegmentedControl(
                            options = PdfQuality.entries.map { it.label },
                            selectedIndex = config.quality.ordinal,
                            onOptionSelected = { idx -> viewModel.updateConfig(config.copy(quality = PdfQuality.entries[idx])) }
                        )
                    }

                    Column {
                        OptionLabel("Formato pagina")
                        SegmentedControl(
                            options = pageSizeLabels,
                            selectedIndex = selectedPageSizeIndex,
                            onOptionSelected = { idx ->
                                viewModel.updateConfig(config.copy(pageSize = pageSizes[idx]))
                            }
                        )
                        OptionHint(
                            when (config.pageSize) {
                                "Original" -> "Dimensioni originali dell'immagine, senza margini"
                                "Letter" -> "Formato USA (216 × 279 mm)"
                                else -> "Formato standard internazionale (210 × 297 mm)"
                            }
                        )
                    }

                    Column {
                        OptionLabel("Orientamento")
                        SegmentedControl(
                            options = orientationLabels,
                            selectedIndex = selectedOrientationIndex,
                            onOptionSelected = { idx ->
                                viewModel.updateConfig(config.copy(orientation = orientations[idx]))
                            }
                        )
                        OptionHint(
                            when (config.orientation) {
                                "Landscape" -> "Tutte le pagine in orizzontale"
                                "Portrait" -> "Tutte le pagine in verticale"
                                else -> "Le immagini orizzontali ricevono automaticamente pagine orizzontali"
                            }
                        )
                    }

                    if (config.pageSize != "Original") {
                        Column {
                            OptionLabel("Margini")
                            SegmentedControl(
                                options = marginLabels,
                                selectedIndex = selectedMarginIndex,
                                onOptionSelected = { idx ->
                                    viewModel.updateConfig(config.copy(marginMm = marginValues[idx]))
                                }
                            )
                            OptionHint("Spazio bianco attorno all'immagine (0, 10 o 20 mm)")
                        }
                    }

                    OptionRow(
                        label = "Pulizia automatica",
                        description = "Sbianca lo sfondo, toglie ombre e dominanti gialle, rende il testo più nitido",
                        checked = config.autoContrastClean,
                        onCheckedChange = { viewModel.updateConfig(config.copy(autoContrastClean = it)) }
                    )

                    OptionRow(
                        label = "Bianco e nero",
                        description = "Pagine in scala di grigi: file più leggero, ideale per appunti",
                        checked = config.grayscale,
                        onCheckedChange = { viewModel.updateConfig(config.copy(grayscale = it)) }
                    )

                    OptionRow(
                        label = "Numeri di pagina",
                        description = "Aggiunge il numero in fondo a ogni pagina",
                        checked = config.pageNumbers,
                        onCheckedChange = { viewModel.updateConfig(config.copy(pageNumbers = it)) }
                    )

                    var protect by rememberSaveable { mutableStateOf(config.password.isNotEmpty()) }
                    OptionRow(
                        label = "Proteggi con password",
                        description = "Serve la password per aprire il PDF",
                        checked = protect,
                        onCheckedChange = {
                            protect = it
                            if (!it) viewModel.updateConfig(config.copy(password = ""))
                        }
                    )
                    AnimatedVisibility(visible = protect) {
                        var visiblePassword by remember { mutableStateOf(false) }
                        MorphTextField(
                            value = config.password,
                            onValueChange = { viewModel.updateConfig(config.copy(password = it)) },
                            placeholder = "Password",
                            visualTransformation = if (visiblePassword) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { visiblePassword = !visiblePassword }) {
                                    Icon(
                                        if (visiblePassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (visiblePassword) "Nascondi" else "Mostra",
                                        tint = OmniTextMuted
                                    )
                                }
                            },
                            isError = config.password.isEmpty(),
                            supportingText = if (config.password.isEmpty()) "Scrivi una password o disattiva la protezione" else "Non dimenticarla: non si può recuperare"
                        )
                    }

                    Surface(
                        onClick = { showMetadataModal = true },
                        shape = RoundedCornerShape(RadiusMd),
                        color = OmniSurface2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("open_metadata_modal_button")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = OmniAccent, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "Modifica metadati",
                                fontSize = 14.sp,
                                color = OmniText,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            if (config.author.isNotBlank() || config.keywords.isNotBlank()) {
                                Text("Impostati", fontSize = 12.sp, color = OmniAccent, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(RadiusMd),
                        color = OmniSurface2,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Dimensione stimata", fontSize = 14.sp, color = OmniTextMuted, modifier = Modifier.weight(1f))
                            AnimatedText("~${formatBytes(estimatedBytes)}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = OmniText)
                        }
                    }
                }
            },
            actionBar = {
                OmniActionBar(
                    summaryText = if (items.size == 1) "1 pagina" else "${items.size} pagine",
                    detailText = "$pageSizeLabel · ${config.quality.label} · ~${formatBytes(estimatedBytes)}",
                    primaryButtonLabel = "Crea PDF",
                    isEnabled = items.isNotEmpty(),
                    disabledHelperText = "Aggiungi almeno un'immagine",
                    onPrimaryAction = {
                        selectedId = null
                        viewModel.generatePdf()
                    },
                    testTag = "generate_pdf_button"
                )
            }
        )
    }

    val cropTarget = items.firstOrNull { it.id == cropTargetId }
    if (cropTarget?.uri != null) {
        CropDialog(
            uri = cropTarget.uri,
            rotationDegrees = cropTarget.rotationDegrees,
            initial = cropTarget.cropRectNormalized,
            onDismiss = { cropTargetId = null },
            onConfirm = { rect ->
                haptics(Haptic.CONFIRM)
                viewModel.setCrop(cropTarget.id, rect)
                cropTargetId = null
            }
        )
    }

    if (showMetadataModal) {
        PdfMetadataDialog(
            currentConfig = config,
            onDismiss = { showMetadataModal = false },
            onSave = { updated ->
                viewModel.updateConfig(updated)
                showMetadataModal = false
            }
        )
    }
}

/** Rough output weight: per-page cap of the chosen quality, lighter when converted to grey. */
private fun estimatePdfBytes(items: List<PdfDocumentItem>, config: PdfExportConfig): Long {
    val cap = config.quality.bytesPerPageCap
    val perPage = items.sumOf { item -> if (item.sizeBytes > 0) minOf(item.sizeBytes, cap) else cap / 2 }
    val factor = (if (config.grayscale) 0.7 else 1.0) * (if (config.autoContrastClean) 0.9 else 1.0)
    return (perPage * factor).toLong()
}

@Composable
private fun OptionLabel(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = OmniText,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun OptionHint(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        color = OmniTextMuted,
        modifier = Modifier.padding(top = 6.dp)
    )
}

@Composable
private fun PageTile(
    item: PdfDocumentItem,
    pageNumber: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(RadiusLg)
    val interaction = remember { MutableInteractionSource() }
    val lift by animateFloatAsState(if (selected) 1.04f else 1f, motionSpec(Motion.bouncy()), label = "tile_lift")
    val borderColor by animateColorAsState(if (selected) OmniAccent else OmniBorder, label = "tile_border")
    // Always turn clockwise (270° -> 360°) instead of spinning back to 0°.
    val reduce = LocalReduceMotion.current
    val rotationAnim = remember { Animatable(item.rotationDegrees.toFloat()) }
    LaunchedEffect(item.rotationDegrees) {
        val delta = ((item.rotationDegrees - rotationAnim.value % 360f) + 360f) % 360f
        if (delta == 0f) return@LaunchedEffect
        if (reduce) rotationAnim.snapTo(rotationAnim.value + delta)
        else rotationAnim.animateTo(rotationAnim.value + delta, spring(dampingRatio = 0.7f, stiffness = 300f))
    }
    val displayRotation = rotationAnim.value

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.78f)
            .graphicsLayer {
                scaleX = lift
                scaleY = lift
            }
            .pressScale(interaction, 0.94f)
            .shadow(if (selected) 10.dp else 0.dp, shape, ambientColor = OmniAccent, spotColor = OmniAccent)
            .clip(shape)
            .background(OmniSurface)
            .border(if (selected) 2.dp else 1.dp, borderColor, shape)
            .clickable(interactionSource = interaction, indication = LocalIndication.current, onClick = onClick)
            .testTag("pdf_page_tile_$pageNumber")
    ) {
        AsyncImage(
            model = item.uri,
            contentDescription = "Pagina $pageNumber",
            contentScale = ContentScale.Crop,
            alignment = when {
                item.isSplitSpread && item.splitPart == 1 -> Alignment.CenterStart
                item.isSplitSpread -> Alignment.CenterEnd
                else -> Alignment.Center
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp)
                .clip(RoundedCornerShape(RadiusSm))
                .background(OmniSurface2)
                .graphicsLayer { rotationZ = displayRotation }
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp)
                .size(26.dp)
                .clip(CircleShape)
                .background(OmniText),
            contentAlignment = Alignment.Center
        ) {
            AnimatedText("$pageNumber", color = OmniBg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        val badges = buildList {
            if (item.isSplitSpread) add(if (item.splitPart == 1) "Sx" else "Dx")
            if (item.isCropped) add("Ritaglio")
            if (item.isBinarized) add("B/N")
        }
        androidx.compose.animation.AnimatedVisibility(
            visible = badges.isNotEmpty(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp),
            enter = scaleIn(motionSpec(Motion.bouncy())) + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            Text(
                text = badges.joinToString(" · "),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = OmniOnAccent,
                modifier = Modifier
                    .clip(RoundedCornerShape(RadiusPill))
                    .background(OmniAccent)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = selected,
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

@Composable
private fun AddPageTile(onAdd: () -> Unit, onCamera: () -> Unit, modifier: Modifier = Modifier) {
    val dash = OmniBorder
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.78f)
            .pressScale(interaction, 0.95f)
            .clip(RoundedCornerShape(RadiusLg))
            .drawBehind {
                drawRoundRect(
                    color = dash,
                    cornerRadius = CornerRadius(20.dp.toPx()),
                    style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
                )
            }
            .clickable(interactionSource = interaction, indication = LocalIndication.current, onClick = onAdd)
            .testTag("pdf_add_more_button"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Add, contentDescription = null, tint = OmniAccent, modifier = Modifier.size(30.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text("Aggiungi", color = OmniAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
            onClick = onCamera,
            shape = CircleShape,
            color = OmniAccentSoft,
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Scatta foto", tint = OmniAccent, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun PageToolbar(
    item: PdfDocumentItem,
    canMoveLeft: Boolean,
    canMoveRight: Boolean,
    onRotate: () -> Unit,
    onCrop: () -> Unit,
    onSplit: () -> Unit,
    onClean: () -> Unit,
    onDuplicate: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(RadiusCard),
        color = OmniSurface,
        border = BorderStroke(1.dp, OmniBorder),
        shadowElevation = 6.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolbarAction(Icons.AutoMirrored.Filled.ArrowBack, "Sposta a sinistra", enabled = canMoveLeft, onClick = onMoveLeft, iconOnly = true)
            ToolbarAction(Icons.AutoMirrored.Filled.ArrowForward, "Sposta a destra", enabled = canMoveRight, onClick = onMoveRight, iconOnly = true)
            ToolbarAction(Icons.AutoMirrored.Filled.RotateRight, "Ruota", onClick = onRotate)
            ToolbarAction(Icons.Default.Crop, "Ritaglia", active = item.isCropped, onClick = onCrop)
            ToolbarAction(Icons.Default.MenuBook, "Dividi", active = item.isSplitSpread, onClick = onSplit)
            ToolbarAction(Icons.Default.AutoFixHigh, "B/N", active = item.isBinarized, onClick = onClean)
            ToolbarAction(Icons.Default.ContentCopy, "Duplica", onClick = onDuplicate)
            ToolbarAction(Icons.Default.DeleteOutline, "Elimina", tint = OmniDanger, onClick = onDelete)
        }
    }
}

@Composable
private fun ToolbarAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    active: Boolean = false,
    iconOnly: Boolean = false,
    tint: Color = OmniText
) {
    val color = when {
        !enabled -> OmniTextMuted.copy(alpha = 0.4f)
        active -> OmniAccent
        else -> tint
    }
    val interaction = remember { MutableInteractionSource() }
    val container by animateColorAsState(if (active) OmniAccentSoft else Color.Transparent, label = "toolbar_bg")
    Surface(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interaction,
        shape = RoundedCornerShape(RadiusMd),
        color = container,
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
private fun GenerationProgressView(
    items: List<PdfDocumentItem>,
    config: PdfExportConfig,
    current: Int,
    total: Int,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val safeTotal = total.coerceAtLeast(1)
    val doneCount = (current - 1).coerceIn(0, safeTotal)
    val fraction = ((current - 0.5f) / safeTotal).coerceIn(0.02f, 0.98f)
    val animated by animateFloatAsState(targetValue = fraction, label = "pdf_progress")
    val remainingSeconds = ((safeTotal - doneCount) * 1.5f).toInt().coerceAtLeast(1)

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isTablet = maxWidth >= 600.dp
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            ToolHeader(
                title = "Creazione PDF",
                description = "${config.title.ifBlank { "Documento" }} · ${items.size} file"
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally)
                    .widthIn(max = if (isTablet) 720.dp else 600.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Surface(
                        shape = RoundedCornerShape(RadiusCard + 8.dp),
                        color = OmniSurface,
                        border = BorderStroke(1.dp, OmniBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 24.dp, horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularPercent(fraction = animated, caption = "~$remainingSeconds s")
                            Spacer(modifier = Modifier.height(18.dp))
                            Text(
                                text = if (doneCount >= safeTotal - 1 && current >= safeTotal) "Salvo il PDF…" else "Elaboro la pagina $current di $safeTotal…",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = OmniText,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (config.autoContrastClean) "Pulizia di contrasto e sfondo" else "Impaginazione delle immagini",
                                fontSize = 14.sp,
                                color = OmniTextMuted
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val chips = buildList {
                                    add(
                                        when (config.pageSize) {
                                            "Letter" -> "Lettera"
                                            "Original" -> "Originale"
                                            else -> config.pageSize
                                        }
                                    )
                                    add(
                                        when (config.orientation) {
                                            "Portrait" -> "Verticale"
                                            "Landscape" -> "Orizzontale"
                                            else -> "Orientamento auto"
                                        }
                                    )
                                    if (config.autoContrastClean) add("Pulizia automatica")
                                }
                                chips.forEach { chip ->
                                    Text(
                                        text = chip,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OmniAccent,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(RadiusPill))
                                            .background(OmniAccentSoft)
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                    val state = when {
                        index < doneCount -> FileState.DONE
                        index == doneCount -> FileState.WORKING
                        else -> FileState.QUEUED
                    }
                    FileProgressRow(item = item, state = state)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally)
                    .widthIn(max = if (isTablet) 720.dp else 600.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    shape = RoundedCornerShape(RadiusMd),
                    border = BorderStroke(1.dp, OmniBorder),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = OmniSurface, contentColor = OmniText),
                    modifier = Modifier
                        .height(56.dp)
                        .testTag("pdf_cancel_button")
                ) {
                    Text("Annulla", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = {},
                    enabled = false,
                    shape = RoundedCornerShape(RadiusMd),
                    colors = ButtonDefaults.buttonColors(
                        disabledContainerColor = OmniSurface2,
                        disabledContentColor = OmniTextMuted
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = OmniTextMuted
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Sto elaborando…", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun CircularPercent(fraction: Float, caption: String) {
    val accent = OmniAccent
    val track = OmniSurface2
    Box(
        modifier = Modifier
            .size(210.dp)
            .clip(CircleShape)
            .background(OmniAccentSoft.copy(alpha = 0.6f))
            .padding(18.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 14.dp.toPx()
            drawArc(
                color = track,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = stroke)
            )
            drawArc(
                color = accent,
                startAngle = -90f,
                sweepAngle = 360f * fraction,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${(fraction * 100).toInt()}", fontSize = 56.sp, fontWeight = FontWeight.Bold, color = OmniText)
                Text(
                    "%",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OmniText,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }
            Text(caption, fontSize = 14.sp, color = OmniTextMuted)
        }
    }
}

private enum class FileState { DONE, WORKING, QUEUED }

@Composable
private fun FileProgressRow(item: PdfDocumentItem, state: FileState) {
    Surface(
        shape = RoundedCornerShape(RadiusCard),
        color = OmniSurface,
        border = BorderStroke(1.dp, OmniBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(icon = Icons.Default.Image, tint = OmniTextMuted, size = 44.dp)
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = OmniText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatBytes(item.sizeBytes) + " · " + when (state) {
                            FileState.DONE -> "Completato"
                            FileState.WORKING -> "In elaborazione"
                            FileState.QUEUED -> "In coda"
                        },
                        fontSize = 13.sp,
                        color = OmniTextMuted
                    )
                }
                when (state) {
                    FileState.DONE -> Icon(Icons.Default.CheckCircle, contentDescription = null, tint = OmniSuccess)
                    FileState.WORKING -> CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.5.dp,
                        color = OmniAccent
                    )
                    FileState.QUEUED -> Icon(Icons.Default.Schedule, contentDescription = null, tint = OmniTextMuted)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (state == FileState.WORKING) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = OmniAccent,
                    trackColor = OmniSurface2
                )
            } else {
                LinearProgressIndicator(
                    progress = { if (state == FileState.DONE) 1f else 0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = OmniSuccess,
                    trackColor = OmniSurface2,
                    drawStopIndicator = {}
                )
            }
        }
    }
}

@Composable
private fun PdfMetadataDialog(
    currentConfig: PdfExportConfig,
    onDismiss: () -> Unit,
    onSave: (PdfExportConfig) -> Unit
) {
    var title by rememberSaveable { mutableStateOf(currentConfig.title) }
    var author by rememberSaveable { mutableStateOf(currentConfig.author) }
    var subject by rememberSaveable { mutableStateOf(currentConfig.subject) }
    var keywords by rememberSaveable { mutableStateOf(currentConfig.keywords) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = OmniSurface,
        shape = RoundedCornerShape(RadiusLg),
        title = {
            Text(
                text = "Metadati del documento",
                style = MaterialTheme.typography.titleLarge,
                color = OmniText
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titolo", fontSize = 14.sp) },
                    placeholder = { Text("es. Appunti di Storia cap. 1", fontSize = 14.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("metadata_title_input"),
                    shape = RoundedCornerShape(RadiusSm)
                )
                OutlinedTextField(
                    value = author,
                    onValueChange = { author = it },
                    label = { Text("Autore", fontSize = 14.sp) },
                    placeholder = { Text("es. Maria Rossi", fontSize = 14.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("metadata_author_input"),
                    shape = RoundedCornerShape(RadiusSm)
                )
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Oggetto", fontSize = 14.sp) },
                    placeholder = { Text("es. Storia dell'arte rinascimentale", fontSize = 14.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("metadata_subject_input"),
                    shape = RoundedCornerShape(RadiusSm)
                )
                OutlinedTextField(
                    value = keywords,
                    onValueChange = { keywords = it },
                    label = { Text("Parole chiave", fontSize = 14.sp) },
                    placeholder = { Text("es. studio, storia, arte", fontSize = 14.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("metadata_keywords_input"),
                    shape = RoundedCornerShape(RadiusSm)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val custom = author.isNotBlank() || subject.isNotBlank() || keywords.isNotBlank()
                    // Typing metadata here is an explicit choice, so it overrides the "remove metadata" default.
                    onSave(currentConfig.copy(title = title, author = author, subject = subject, keywords = keywords, stripMetadata = currentConfig.stripMetadata && !custom))
                },
                colors = ButtonDefaults.buttonColors(containerColor = OmniAccent, contentColor = OmniOnAccent),
                shape = RoundedCornerShape(RadiusSm),
                modifier = Modifier.height(44.dp)
            ) {
                Text("Salva", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.height(44.dp)) {
                Text("Annulla", color = OmniTextMuted, fontSize = 14.sp)
            }
        }
    )
}
