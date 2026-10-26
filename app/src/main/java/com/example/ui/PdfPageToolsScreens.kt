package com.example.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FlipToBack
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AnimatedText
import com.example.ui.components.ContinueAction
import com.example.ui.components.ConversionResultView
import com.example.ui.components.FileCard
import com.example.ui.components.OmniActionBar
import com.example.ui.components.OmniSlider
import com.example.ui.components.OptionRow
import com.example.ui.components.OptionSection
import com.example.ui.components.RevealVertically
import com.example.ui.components.SegmentedControl
import com.example.ui.components.ShimmerProgressBar
import com.example.ui.components.formatBytes
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
import com.example.ui.theme.RadiusMd
import com.example.ui.theme.TintMerge
import com.example.ui.theme.TintPdf
import com.example.ui.theme.TintStamp
import com.example.ui.theme.enterAnimation
import com.example.ui.theme.motionSpec
import com.example.util.NotesSide
import com.example.util.NotesStyle
import com.example.util.OutputFolder
import com.example.util.OutputStorage
import com.example.util.PageMath
import com.example.viewmodel.BlankSensitivity
import com.example.viewmodel.DuplexMode
import com.example.viewmodel.LoadedPdf
import com.example.viewmodel.PageToolViewModel
import com.example.viewmodel.PdfBlankPagesViewModel
import com.example.viewmodel.PdfCropViewModel
import com.example.viewmodel.PdfDuplexViewModel
import com.example.viewmodel.PdfNotesViewModel
import com.example.viewmodel.PdfNupViewModel
import kotlin.math.roundToInt

/** Shared frame: picker, banners and the "file ready" screen with a "read it" follow-up. */
@Composable
private fun PageToolFrame(
    viewModel: PageToolViewModel,
    title: String,
    description: String,
    dropIcon: androidx.compose.ui.graphics.vector.ImageVector,
    dropHeadline: String,
    dropHelp: String,
    tint: Color,
    progressLabel: (Pair<Int, Int>) -> String,
    resultTitle: String,
    resultSubtitle: String,
    onOpenInReader: (Uri) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (LoadedPdf) -> Unit
) {
    val source by viewModel.source.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isWorking by viewModel.isWorking.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val resultUri by viewModel.resultUri.collectAsState()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.loadPdf(uri)
    }
    AnimatedContent(
        targetState = resultUri != null,
        transitionSpec = { stageTransition(if (initialState) 1 else 0, if (targetState) 1 else 0) },
        modifier = modifier,
        label = "page_tool_stage"
    ) { showResult ->
        val uri = resultUri
        if (showResult && uri != null) {
            ConversionResultView(
                title = resultTitle,
                subtitle = resultSubtitle,
                outputUri = uri,
                mimeType = "application/pdf",
                previewIcon = dropIcon,
                previewTint = tint,
                savedIn = "Salvato in ${OutputStorage.displayPath(OutputFolder.PDFS)}",
                onClose = { viewModel.dismissResult() },
                onNewConversion = { viewModel.clear() },
                continueActions = listOf(
                    ContinueAction("Leggi il PDF", Icons.AutoMirrored.Filled.MenuBook, TintPdf) { onOpenInReader(uri) }
                )
            )
        } else {
            SinglePdfScaffold(
                title = title,
                description = description,
                source = source,
                isLoading = isLoading,
                isWorking = isWorking,
                progress = progress,
                progressLabel = progressLabel(progress),
                errorMessage = errorMessage,
                onDismissError = { viewModel.dismissError() },
                onCancel = { viewModel.cancel() },
                onPick = { picker.launch("application/pdf") },
                onClear = { viewModel.clear() },
                dropIcon = dropIcon,
                dropHeadline = dropHeadline,
                dropHelp = dropHelp,
                tint = tint,
                content = content
            )
        }
    }
}

@Composable
private fun rememberThumbnail(viewModel: PageToolViewModel, page: Int, widthPx: Int = 220): Bitmap? {
    val bitmap by produceState<Bitmap?>(initialValue = null, viewModel, page, widthPx) { value = viewModel.thumbnail(page, widthPx) }
    return bitmap
}

// -------------------------------------------------------------------------------------------------
// Togli pagine bianche
// -------------------------------------------------------------------------------------------------

@Composable
fun PdfBlankPagesScreen(viewModel: PdfBlankPagesViewModel, modifier: Modifier = Modifier, onOpenInReader: (Uri) -> Unit = {}) {
    val ink by viewModel.ink.collectAsState()
    val scanning by viewModel.scanning.collectAsState()
    val scanProgress by viewModel.scanProgress.collectAsState()
    val sensitivity by viewModel.sensitivity.collectAsState()
    val overrides by viewModel.overrides.collectAsState()
    val isWorking by viewModel.isWorking.collectAsState()

    PageToolFrame(
        viewModel = viewModel,
        title = "Togli pagine bianche",
        description = "Trova ed elimina le pagine vuote",
        dropIcon = Icons.Default.LayersClear,
        dropHeadline = "Scegli il PDF da ripulire",
        dropHelp = "Utile per le scansioni fronte/retro: le pagine bianche vengono trovate da sole",
        tint = TintMerge,
        progressLabel = { "Salvo il PDF…" },
        resultTitle = "Pagine bianche tolte",
        resultSubtitle = "Il nuovo PDF contiene solo le pagine con qualcosa scritto.",
        onOpenInReader = onOpenInReader,
        modifier = modifier
    ) { pdf ->
        val inks = ink
        Column(modifier = Modifier.fillMaxSize()) {
            if (inks == null) {
                val (done, total) = scanProgress
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AnimatedText(
                        text = if (scanning && total > 0) "Controllo la pagina $done di $total…" else "Preparo l'analisi…",
                        fontSize = 15.sp,
                        color = OmniText
                    )
                    ShimmerProgressBar(progress = if (total > 0) done.toFloat() / total else 0f)
                }
            } else {
                val remove = viewModel.toRemove(inks, sensitivity, overrides)
                SegmentedControl(
                    options = BlankSensitivity.entries.map { it.label },
                    selectedIndex = sensitivity.ordinal,
                    onOptionSelected = { viewModel.setSensitivity(BlankSensitivity.entries[it]) }
                )
                Spacer(modifier = Modifier.height(10.dp))
                AnimatedText(
                    text = when (remove.size) {
                        0 -> "Nessuna pagina bianca trovata"
                        1 -> "1 pagina bianca su ${inks.size}"
                        else -> "${remove.size} pagine bianche su ${inks.size}"
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OmniText
                )
                Text("Tocca una pagina per tenerla o toglierla.", fontSize = 13.sp, color = OmniTextMuted)
                Spacer(modifier = Modifier.height(10.dp))
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 92.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(inks.size, key = { it }) { page ->
                        BlankPageTile(
                            viewModel = viewModel,
                            page = page,
                            removed = page in remove,
                            onToggle = { viewModel.toggle(page) }
                        )
                    }
                }
                OmniActionBar(
                    summaryText = if (remove.isEmpty()) "Niente da togliere" else "Tolgo ${remove.size} di ${inks.size} pagine",
                    detailText = formatBytes(pdf.sizeBytes),
                    primaryButtonLabel = if (isWorking) "Salvo…" else "Togli pagine",
                    isEnabled = !isWorking && remove.isNotEmpty() && remove.size < inks.size,
                    disabledHelperText = when {
                        remove.isEmpty() -> "Tocca una pagina per segnarla da togliere"
                        remove.size >= inks.size -> "Tieni almeno una pagina"
                        else -> null
                    },
                    onPrimaryAction = { viewModel.remove() },
                    testTag = "blank_remove_button"
                )
            }
        }
    }
}

@Composable
private fun BlankPageTile(viewModel: PdfBlankPagesViewModel, page: Int, removed: Boolean, onToggle: () -> Unit) {
    val thumb = rememberThumbnail(viewModel, page, 160)
    val scale by animateFloatAsState(if (removed) 0.9f else 1f, motionSpec(Motion.bouncy()), label = "blank_scale")
    val dim by animateFloatAsState(if (removed) 0.45f else 1f, motionSpec(Motion.gentle()), label = "blank_dim")
    val ring by animateColorAsState(if (removed) OmniDanger else OmniBorder, label = "blank_ring")
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.72f)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White)
                .border(2.dp, ring, RoundedCornerShape(10.dp))
                .clickable(onClickLabel = if (removed) "Tieni la pagina ${page + 1}" else "Togli la pagina ${page + 1}", onClick = onToggle),
            contentAlignment = Alignment.Center
        ) {
            if (thumb != null) {
                Image(
                    bitmap = thumb.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = dim }
                )
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = removed,
                enter = scaleIn(motionSpec(Motion.bouncy())) + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(OmniDanger),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = OmniOnAccent, modifier = Modifier.size(20.dp))
                }
            }
        }
        Text("${page + 1}", fontSize = 12.sp, color = if (removed) OmniDanger else OmniTextMuted, modifier = Modifier.padding(top = 4.dp))
    }
}

// -------------------------------------------------------------------------------------------------
// Più pagine per foglio
// -------------------------------------------------------------------------------------------------

@Composable
fun PdfNupScreen(viewModel: PdfNupViewModel, modifier: Modifier = Modifier, onOpenInReader: (Uri) -> Unit = {}) {
    val perSheet by viewModel.perSheet.collectAsState()
    val frames by viewModel.frames.collectAsState()
    val isWorking by viewModel.isWorking.collectAsState()
    val options = listOf(2, 4)

    PageToolFrame(
        viewModel = viewModel,
        title = "Più pagine per foglio",
        description = "Stampa risparmiando carta",
        dropIcon = Icons.Default.Dashboard,
        dropHeadline = "Scegli il PDF da impaginare",
        dropHelp = "Metti 2 o 4 pagine su ogni foglio A4: perfetto per stampare dispense e slide",
        tint = TintMerge,
        progressLabel = { (c, t) -> "Impagino il foglio $c di $t…" },
        resultTitle = "PDF impaginato",
        resultSubtitle = "Ogni foglio A4 contiene $perSheet pagine del documento.",
        onOpenInReader = onOpenInReader,
        modifier = modifier
    ) { pdf ->
        val sheets = (pdf.pageCount + perSheet - 1) / perSheet
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SegmentedControl(
                options = options.map { "$it per foglio" },
                selectedIndex = options.indexOf(perSheet).coerceAtLeast(0),
                onOptionSelected = { viewModel.setPerSheet(options[it]) }
            )
            NupPreview(viewModel = viewModel, perSheet = perSheet, frames = frames, pageCount = pdf.pageCount)
            AnimatedText(
                text = "${pdf.pageCount} pagine → $sheets ${if (sheets == 1) "foglio" else "fogli"} A4 ${if (perSheet == 2) "orizzontali" else "verticali"}",
                fontSize = 14.sp,
                color = OmniTextMuted
            )
            OptionRow(
                label = "Bordo attorno alle pagine",
                description = "Una cornice sottile aiuta a ritagliare o a distinguere le slide",
                checked = frames,
                onCheckedChange = viewModel::setFrames
            )
            OmniActionBar(
                summaryText = "$sheets ${if (sheets == 1) "foglio" else "fogli"} invece di ${pdf.pageCount}",
                detailText = "Testo e disegni restano nitidi",
                primaryButtonLabel = if (isWorking) "Impagino…" else "Crea PDF",
                isEnabled = !isWorking && pdf.pageCount > 1,
                disabledHelperText = if (pdf.pageCount <= 1) "Serve un PDF con almeno 2 pagine" else null,
                onPrimaryAction = { viewModel.run() },
                testTag = "nup_button"
            )
        }
    }
}

/** A sheet that turns landscape/portrait while the first pages glide into their slots. */
@Composable
private fun NupPreview(viewModel: PdfNupViewModel, perSheet: Int, frames: Boolean, pageCount: Int) {
    val (sheetW, sheetH) = PageMath.nUpSheet(perSheet)
    val aspect by animateFloatAsState(sheetW / sheetH, motionSpec(Motion.gentle()), label = "nup_aspect")
    BoxWithConstraints(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val boxW = if (aspect >= 1f) maxWidth * 0.9f else maxWidth * 0.62f
        val boxH = boxW / aspect
        Box(
            modifier = Modifier
                .size(boxW, boxH)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White)
                .border(1.dp, OmniBorder, RoundedCornerShape(6.dp))
        ) {
            val slots = PageMath.nUpSlots(perSheet, sheetW, sheetH, margin = 18f, gap = 12f)
            val scale = boxW / sheetW
            for (i in 0 until 4) {
                val slot = slots.getOrNull(i)
                val visible = slot != null && i < pageCount
                // Slots are bottom-left based (PDF); the preview is top-left based.
                val target = slot ?: PageMath.Box(sheetW / 2f, sheetH / 2f, 0f, 0f)
                val x by animateDpAsState(scale * target.x, motionSpec(Motion.gentle()), label = "nup_x$i")
                val y by animateDpAsState(scale * (sheetH - target.y - target.h), motionSpec(Motion.gentle()), label = "nup_y$i")
                val w by animateDpAsState(scale * target.w, motionSpec(Motion.gentle()), label = "nup_w$i")
                val h by animateDpAsState(scale * target.h, motionSpec(Motion.gentle()), label = "nup_h$i")
                val alpha by animateFloatAsState(if (visible) 1f else 0f, motionSpec(tween(Motion.MEDIUM)), label = "nup_a$i")
                val thumb = rememberThumbnail(viewModel, i.coerceAtMost((pageCount - 1).coerceAtLeast(0)), 200)
                Box(
                    modifier = Modifier
                        .offset(x, y)
                        .size(w.coerceAtLeast(0.dp), h.coerceAtLeast(0.dp))
                        .graphicsLayer { this.alpha = alpha },
                    contentAlignment = Alignment.Center
                ) {
                    if (thumb != null) {
                        Image(
                            bitmap = thumb.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .then(if (frames) Modifier.border(1.dp, OmniBorder) else Modifier)
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Spazio per appunti
// -------------------------------------------------------------------------------------------------

@Composable
fun PdfNotesScreen(viewModel: PdfNotesViewModel, modifier: Modifier = Modifier, onOpenInReader: (Uri) -> Unit = {}) {
    val side by viewModel.side.collectAsState()
    val style by viewModel.style.collectAsState()
    val isWorking by viewModel.isWorking.collectAsState()

    PageToolFrame(
        viewModel = viewModel,
        title = "Spazio per appunti",
        description = "Pagine più piccole con righe a lato",
        dropIcon = Icons.Default.NoteAlt,
        dropHeadline = "Scegli il PDF su cui prendere appunti",
        dropHelp = "Ogni pagina si rimpicciolisce e accanto o sotto compaiono righe o quadretti da stampare",
        tint = TintStamp,
        progressLabel = { (c, t) -> "Preparo la pagina $c di $t…" },
        resultTitle = "PDF per appunti pronto",
        resultSubtitle = "Stampalo e scrivi accanto a ogni pagina.",
        onOpenInReader = onOpenInReader,
        modifier = modifier
    ) { pdf ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OptionSection(title = "Dove") {
                SegmentedControl(
                    options = NotesSide.entries.map { it.label },
                    selectedIndex = side.ordinal,
                    onOptionSelected = { viewModel.setSide(NotesSide.entries[it]) }
                )
            }
            OptionSection(title = "Stile") {
                SegmentedControl(
                    options = NotesStyle.entries.map { it.label },
                    selectedIndex = style.ordinal,
                    onOptionSelected = { viewModel.setStyle(NotesStyle.entries[it]) }
                )
            }
            NotesPreview(viewModel = viewModel, side = side, style = style)
            OmniActionBar(
                summaryText = "${pdf.pageCount} pagine con spazio per appunti",
                detailText = "${side.label} · ${style.label}",
                primaryButtonLabel = if (isWorking) "Preparo…" else "Crea PDF",
                isEnabled = !isWorking,
                onPrimaryAction = { viewModel.run() },
                testTag = "notes_button"
            )
        }
    }
}

@Composable
private fun NotesPreview(viewModel: PdfNotesViewModel, side: NotesSide, style: NotesStyle) {
    val sheetW = PageMath.A4_SHORT
    val sheetH = PageMath.A4_LONG
    val (page, notes) = PageMath.notesLayout(sheetW, sheetH, side, margin = sheetW * 0.04f)
    val thumb = rememberThumbnail(viewModel, 0, 240)
    val lineColor = OmniAccent.copy(alpha = 0.35f)
    // Lines are "drawn" again from the top whenever the layout or the style changes.
    val reveal = remember { Animatable(1f) }
    val reduce = LocalReduceMotion.current
    LaunchedEffect(side, style) {
        if (!reduce) {
            reveal.snapTo(0f)
            reveal.animateTo(1f, tween(Motion.LONG, easing = Motion.EmphasizedDecelerate))
        }
    }
    BoxWithConstraints(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val boxW = maxWidth * 0.62f
        val scale = boxW / sheetW
        val boxH = scale * sheetH
        val px by animateDpAsState(scale * page.x, motionSpec(Motion.gentle()), label = "notes_px")
        val py by animateDpAsState(scale * (sheetH - page.y - page.h), motionSpec(Motion.gentle()), label = "notes_py")
        val pw by animateDpAsState(scale * page.w, motionSpec(Motion.gentle()), label = "notes_pw")
        val ph by animateDpAsState(scale * page.h, motionSpec(Motion.gentle()), label = "notes_ph")
        Box(
            modifier = Modifier
                .size(boxW, boxH)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White)
                .border(1.dp, OmniBorder, RoundedCornerShape(6.dp))
        ) {
            AnimatedContent(
                targetState = Triple(side, style, notes),
                transitionSpec = { fadeIn(tween(Motion.MEDIUM)) togetherWith fadeOut(tween(Motion.SHORT)) },
                label = "notes_area"
            ) { (_, currentStyle, area) ->
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val s = size.width / sheetW
                    val left = area.x * s
                    val top = (sheetH - area.y - area.h) * s
                    val w = area.w * s
                    val h = area.h * s * reveal.value
                    drawRect(lineColor, Offset(left, top), Size(w, area.h * s), style = Stroke(width = 1.dp.toPx()))
                    when (currentStyle) {
                        NotesStyle.LINES -> {
                            var y = top + 20f * s
                            while (y < top + h) {
                                drawLine(lineColor, Offset(left, y), Offset(left + w, y), strokeWidth = 1f)
                                y += 20f * s
                            }
                        }
                        NotesStyle.GRID -> {
                            var y = top
                            while (y < top + h) {
                                drawLine(lineColor, Offset(left, y), Offset(left + w, y), strokeWidth = 1f)
                                y += 14f * s
                            }
                            var x = left
                            while (x < left + w) {
                                drawLine(lineColor, Offset(x, top), Offset(x, top + h), strokeWidth = 1f)
                                x += 14f * s
                            }
                        }
                        NotesStyle.BLANK -> Unit
                    }
                }
            }
            Box(
                modifier = Modifier
                    .offset(px, py)
                    .size(pw.coerceAtLeast(0.dp), ph.coerceAtLeast(0.dp))
                    .border(1.dp, OmniBorder)
            ) {
                if (thumb != null) {
                    Image(bitmap = thumb.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Unisci fronte/retro
// -------------------------------------------------------------------------------------------------

@Composable
fun PdfDuplexScreen(viewModel: PdfDuplexViewModel, modifier: Modifier = Modifier, onOpenInReader: (Uri) -> Unit = {}) {
    val backs by viewModel.backs.collectAsState()
    val mode by viewModel.mode.collectAsState()
    val backsReversed by viewModel.backsReversed.collectAsState()
    val isWorking by viewModel.isWorking.collectAsState()
    val backPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.loadBacks(uri)
    }

    PageToolFrame(
        viewModel = viewModel,
        title = "Unisci fronte/retro",
        description = "Rimetti in ordine le scansioni a due facciate",
        dropIcon = Icons.Default.FlipToBack,
        dropHeadline = "Scegli il PDF con i fronti",
        dropHelp = "Hai scansionato prima tutti i fronti e poi tutti i retro? Morph li alterna nell'ordine giusto",
        tint = TintMerge,
        progressLabel = { (c, t) -> "Sistemo la pagina $c di $t…" },
        resultTitle = "Fronte e retro uniti",
        resultSubtitle = "Le pagine ora si alternano come nel documento originale.",
        onOpenInReader = onOpenInReader,
        modifier = modifier
    ) { pdf ->
        val order = viewModel.order()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SegmentedControl(
                options = DuplexMode.entries.map { it.label },
                selectedIndex = mode.ordinal,
                onOptionSelected = { viewModel.setMode(DuplexMode.entries[it]) }
            )
            AnimatedContent(
                targetState = mode,
                transitionSpec = { stageTransition(initialState.ordinal, targetState.ordinal) },
                label = "duplex_mode"
            ) { current ->
                if (current == DuplexMode.TWO_FILES) {
                    val b = backs
                    if (b == null) {
                        OutlinedButton(
                            onClick = { backPicker.launch("application/pdf") },
                            shape = RoundedCornerShape(RadiusMd),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = OmniAccent)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scegli il PDF con i retro", color = OmniAccent, fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        FileCard(
                            name = b.name,
                            details = "Retro · ${b.pageCount} pagine",
                            icon = Icons.Default.FlipToBack,
                            iconTint = TintMerge,
                            trailingContent = {
                                TextButton(onClick = { backPicker.launch("application/pdf") }) { Text("Cambia", color = OmniAccent) }
                            },
                            onRemove = { viewModel.clearBacks() },
                            modifier = Modifier.enterAnimation(0, key = b.uri)
                        )
                    }
                } else {
                    Text(
                        "Nel PDF ci sono prima tutti i fronti e poi tutti i retro (${pdf.pageCount} pagine in tutto).",
                        fontSize = 14.sp,
                        color = OmniTextMuted
                    )
                }
            }
            OptionRow(
                label = "Retro al contrario",
                description = if (backsReversed) "Il primo retro scansionato è quello dell'ultima pagina (pila girata)" else "I retro sono nello stesso ordine dei fronti",
                checked = backsReversed,
                onCheckedChange = viewModel::setBacksReversed
            )
            DuplexOrderPreview(order)
            OmniActionBar(
                summaryText = if (order.isEmpty()) "Manca qualcosa" else "${order.size} pagine in ordine",
                detailText = "F = fronte · R = retro",
                primaryButtonLabel = if (isWorking) "Unisco…" else "Unisci",
                isEnabled = !isWorking && order.isNotEmpty(),
                disabledHelperText = when {
                    mode == DuplexMode.TWO_FILES && backs == null -> "Scegli anche il PDF dei retro"
                    order.isEmpty() -> "Servono almeno 2 pagine"
                    else -> null
                },
                onPrimaryAction = { viewModel.run() },
                testTag = "duplex_button"
            )
        }
    }
}

/** The resulting order as chips (F1 R1 F2 R2 …), appearing one after the other. */
@Composable
private fun DuplexOrderPreview(order: List<Pair<Int, Int>>) {
    if (order.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Ordine finale", style = MaterialTheme.typography.titleMedium, color = OmniText)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            itemsIndexed(order.take(60), key = { i, item -> "$i-${item.first}-${item.second}" }) { index, (side, page) ->
                val front = side == 0
                Box(
                    modifier = Modifier
                        .enterAnimation(index, key = "duplex_${index}_${side}_$page")
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (front) OmniAccentSoft else OmniSurface2)
                        .border(1.dp, if (front) OmniAccent else OmniBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${if (front) "F" else "R"}${page + 1}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (front) OmniAccent else OmniText
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Ritaglia margini
// -------------------------------------------------------------------------------------------------

@Composable
fun PdfCropScreen(viewModel: PdfCropViewModel, modifier: Modifier = Modifier, onOpenInReader: (Uri) -> Unit = {}) {
    val margins by viewModel.margins.collectAsState()
    val linked by viewModel.linked.collectAsState()
    val autoMessage by viewModel.autoMessage.collectAsState()
    val isWorking by viewModel.isWorking.collectAsState()

    PageToolFrame(
        viewModel = viewModel,
        title = "Ritaglia margini",
        description = "Togli i bordi bianchi da tutte le pagine",
        dropIcon = Icons.Default.Crop,
        dropHeadline = "Scegli il PDF da ritagliare",
        dropHelp = "Pagine più grandi da leggere sul telefono, senza perdere nitidezza",
        tint = TintStamp,
        progressLabel = { (c, t) -> "Ritaglio la pagina $c di $t…" },
        resultTitle = "PDF ritagliato",
        resultSubtitle = "Tutte le pagine hanno i nuovi margini.",
        onOpenInReader = onOpenInReader,
        modifier = modifier
    ) { pdf ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CropPreview(viewModel = viewModel, margins = margins)
            OutlinedButton(
                onClick = { viewModel.auto() },
                shape = RoundedCornerShape(RadiusMd),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = OmniAccent)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Trova i margini da solo", color = OmniAccent, fontWeight = FontWeight.SemiBold)
            }
            RevealVertically(visible = autoMessage != null) {
                Text(autoMessage.orEmpty(), fontSize = 13.sp, color = OmniTextMuted)
            }
            OptionRow(
                label = "Stesso margine su tutti i lati",
                description = if (linked) "Un solo cursore per i quattro lati" else "Regola ogni lato a parte",
                checked = linked,
                onCheckedChange = viewModel::setLinked
            )
            val percent: (Float) -> String = { "${(it * 100).roundToInt()}%" }
            AnimatedContent(
                targetState = linked,
                transitionSpec = { fadeIn(tween(Motion.MEDIUM)) togetherWith fadeOut(tween(Motion.SHORT)) },
                label = "crop_sliders"
            ) { isLinked ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isLinked) {
                        OmniSlider(label = "Margine", value = margins[0], onValueChange = { viewModel.setMargin(0, it) }, valueRange = 0f..PdfCropViewModel.MAX_MARGIN, valueFormatter = percent)
                    } else {
                        listOf("Sinistra", "Sopra", "Destra", "Sotto").forEachIndexed { side, label ->
                            OmniSlider(label = label, value = margins[side], onValueChange = { viewModel.setMargin(side, it) }, valueRange = 0f..PdfCropViewModel.MAX_MARGIN, valueFormatter = percent)
                        }
                    }
                }
            }
            val valid = margins[0] + margins[2] < 0.95f && margins[1] + margins[3] < 0.95f
            OmniActionBar(
                summaryText = "Ritaglio ${pdf.pageCount} pagine",
                detailText = "Sinistra ${percent(margins[0])} · Sopra ${percent(margins[1])} · Destra ${percent(margins[2])} · Sotto ${percent(margins[3])}",
                primaryButtonLabel = if (isWorking) "Ritaglio…" else "Ritaglia",
                isEnabled = !isWorking && valid && margins.any { it > 0.001f },
                disabledHelperText = if (margins.all { it <= 0.001f }) "Imposta almeno un margine" else null,
                onPrimaryAction = { viewModel.run() },
                testTag = "crop_button"
            )
        }
    }
}

/** First page with the area that will be cut away darkened; the frame glides as margins change. */
@Composable
private fun CropPreview(viewModel: PdfCropViewModel, margins: FloatArray) {
    val thumb = rememberThumbnail(viewModel, 0, 400)
    val l by animateFloatAsState(margins[0], motionSpec(Motion.gentle()), label = "crop_l")
    val t by animateFloatAsState(margins[1], motionSpec(Motion.gentle()), label = "crop_t")
    val r by animateFloatAsState(margins[2], motionSpec(Motion.gentle()), label = "crop_r")
    val b by animateFloatAsState(margins[3], motionSpec(Motion.gentle()), label = "crop_b")
    val accent = OmniAccent
    val shade = OmniText.copy(alpha = 0.45f)
    val aspect = thumb?.let { it.width.toFloat() / it.height } ?: 0.707f
    BoxWithConstraints(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val boxW: Dp = minOf(maxWidth * 0.7f, 320.dp)
        Box(
            modifier = Modifier
                .size(boxW, boxW / aspect)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White)
                .border(1.dp, OmniBorder, RoundedCornerShape(6.dp))
        ) {
            if (thumb != null) {
                Image(bitmap = thumb.asImageBitmap(), contentDescription = "Prima pagina", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
            }
            Canvas(modifier = Modifier.fillMaxSize()) {
                val left = size.width * l
                val top = size.height * t
                val right = size.width * (1f - r)
                val bottom = size.height * (1f - b)
                drawRect(shade, Offset.Zero, Size(size.width, top))
                drawRect(shade, Offset(0f, bottom), Size(size.width, size.height - bottom))
                drawRect(shade, Offset(0f, top), Size(left, bottom - top))
                drawRect(shade, Offset(right, top), Size(size.width - right, bottom - top))
                drawRect(
                    color = accent,
                    topLeft = Offset(left, top),
                    size = Size((right - left).coerceAtLeast(0f), (bottom - top).coerceAtLeast(0f)),
                    style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f)))
                )
            }
        }
    }
}
