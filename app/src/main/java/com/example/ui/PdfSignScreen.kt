package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.SignPages
import com.example.ui.components.ContinueAction
import com.example.ui.components.ConversionResultView
import com.example.ui.components.OmniActionBar
import com.example.ui.components.OmniSlider
import com.example.ui.components.OptionSection
import com.example.ui.components.SegmentedControl
import com.example.ui.components.formatBytes
import com.example.ui.theme.Haptic
import com.example.ui.theme.LocalReduceMotion
import com.example.ui.theme.Motion
import com.example.ui.theme.OmniAccent
import com.example.ui.theme.OmniBorder
import com.example.ui.theme.OmniSurface
import com.example.ui.theme.OmniSurface2
import com.example.ui.theme.OmniText
import com.example.ui.theme.OmniTextMuted
import com.example.ui.theme.RadiusMd
import com.example.ui.theme.TintPdf
import com.example.ui.theme.TintSecure
import com.example.ui.theme.TintStamp
import com.example.ui.theme.enterAnimation
import com.example.ui.theme.motionSpec
import com.example.ui.theme.rememberHaptics
import com.example.util.InkPoint
import com.example.util.OutputFolder
import com.example.util.OutputStorage
import com.example.util.SignatureInk
import com.example.viewmodel.PdfSignViewModel
import com.example.viewmodel.SignInk
import kotlinx.coroutines.launch

private val PadStroke = 3.dp

@Composable
fun PdfSignScreen(
    viewModel: PdfSignViewModel,
    modifier: Modifier = Modifier,
    onContinueWithCompress: (Uri) -> Unit = {},
    onContinueWithProtect: (Uri) -> Unit = {}
) {
    val source by viewModel.source.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isWorking by viewModel.isWorking.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val resultUri by viewModel.resultUri.collectAsState()
    val strokes by viewModel.strokes.collectAsState()
    val pages by viewModel.pages.collectAsState()
    val placement by viewModel.placement.collectAsState()
    val ink by viewModel.ink.collectAsState()
    val strokePx = with(LocalDensity.current) { PadStroke.toPx() }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.loadPdf(uri)
    }

    AnimatedContent(
        targetState = resultUri != null,
        transitionSpec = { stageTransition(if (initialState) 1 else 0, if (targetState) 1 else 0) },
        modifier = modifier,
        label = "sign_stage"
    ) { showResult ->
        val uri = resultUri
        if (showResult && uri != null) {
            ConversionResultView(
                title = "PDF firmato",
                subtitle = when (pages) {
                    SignPages.ALL -> "Firma aggiunta su ogni pagina"
                    SignPages.FIRST -> "Firma aggiunta sulla prima pagina"
                    SignPages.LAST -> "Firma aggiunta sull'ultima pagina"
                },
                outputUri = uri,
                mimeType = "application/pdf",
                previewIcon = Icons.Default.Draw,
                previewTint = TintStamp,
                detailPrefix = source?.let { "${it.pageCount} pagine" },
                savedIn = "Salvato in ${OutputStorage.displayPath(OutputFolder.PDFS)}",
                onClose = { viewModel.dismissResult() },
                onNewConversion = { viewModel.clear() },
                continueActions = listOf(
                    ContinueAction("Proteggi con password", Icons.Default.Lock, TintSecure) { onContinueWithProtect(uri) },
                    ContinueAction("Comprimi il PDF", Icons.Default.Compress, TintPdf) { onContinueWithCompress(uri) }
                )
            )
        } else {
            SinglePdfScaffold(
                title = "Firma PDF",
                description = "Disegna la tua firma e mettila sul documento",
                source = source,
                isLoading = isLoading,
                isWorking = isWorking,
                progress = progress,
                progressLabel = "Firmo la pagina ${progress.first} di ${progress.second}…",
                errorMessage = errorMessage,
                onDismissError = { viewModel.dismissError() },
                onCancel = { viewModel.cancel() },
                onPick = { picker.launch("application/pdf") },
                onClear = { viewModel.clear() },
                dropIcon = Icons.Default.Draw,
                dropHeadline = "Scegli il PDF da firmare",
                dropHelp = "Giustificazioni, autorizzazioni per le gite, moduli della scuola",
                tint = TintStamp
            ) { pdf ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OptionSection(title = "La tua firma", hint = "Firma col dito nel riquadro. Puoi annullare l'ultimo tratto.") {
                        SignaturePad(
                            strokes = strokes,
                            inkColor = Color(ink.argb),
                            onStroke = viewModel::addStroke,
                            onUndo = viewModel::undoStroke,
                            onClear = viewModel::clearInk,
                            modifier = Modifier.enterAnimation(1)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Inchiostro", fontSize = 13.sp, color = OmniTextMuted)
                            SignInk.entries.forEach { choice ->
                                InkSwatch(choice = choice, selected = choice == ink, onClick = { viewModel.setInk(choice) })
                            }
                        }
                    }

                    OptionSection(title = "Dove firmare", hint = "Trascina la firma sulla pagina per spostarla") {
                        SegmentedControl(
                            options = SignPages.entries.map { it.label },
                            selectedIndex = pages.ordinal,
                            onOptionSelected = { viewModel.setPages(SignPages.entries[it]) }
                        )
                    }

                    PlacementPreview(
                        loadKey = pdf.uri to pages,
                        strokes = strokes,
                        inkColor = Color(ink.argb),
                        strokeWidthPx = strokePx,
                        centerX = placement.centerX,
                        centerY = placement.centerY,
                        widthFraction = placement.widthFraction,
                        loadPage = { width -> viewModel.previewPage(width) },
                        onMove = { dx, dy ->
                            viewModel.updatePlacement {
                                it.copy(centerX = (it.centerX + dx).coerceIn(0.05f, 0.95f), centerY = (it.centerY + dy).coerceIn(0.03f, 0.97f))
                            }
                        },
                        modifier = Modifier.enterAnimation(2)
                    )

                    OmniSlider(
                        label = "Dimensione firma",
                        value = placement.widthFraction * 100f,
                        onValueChange = { v -> viewModel.updatePlacement { it.copy(widthFraction = v / 100f) } },
                        valueRange = 12f..70f,
                        valueFormatter = { "${it.toInt()}% della pagina" }
                    )

                    OmniActionBar(
                        summaryText = "${pdf.pageCount} pagine · ${formatBytes(pdf.sizeBytes)}",
                        detailText = when (pages) {
                            SignPages.ALL -> "Firma su ${pdf.pageCount} pagine"
                            else -> "Firma sulla ${pages.label.lowercase()} pagina"
                        },
                        primaryButtonLabel = if (isWorking) "Firmo…" else "Firma il PDF",
                        isEnabled = !isWorking && strokes.isNotEmpty(),
                        disabledHelperText = if (strokes.isEmpty()) "Disegna prima la firma" else null,
                        onPrimaryAction = { viewModel.apply(strokePx) },
                        testTag = "sign_pdf_button"
                    )
                }
            }
        }
    }
}

/** Finger drawing area: a dashed line and "Firma qui" until the first stroke; ink fades out on clear. */
@Composable
private fun SignaturePad(
    strokes: List<List<InkPoint>>,
    inkColor: Color,
    onStroke: (List<InkPoint>) -> Unit,
    onUndo: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()
    val reduce = LocalReduceMotion.current
    val current = remember { mutableStateListOf<InkPoint>() }
    val inkAlpha = remember { Animatable(1f) }
    val latestOnStroke by rememberUpdatedState(onStroke)
    val color by animateColorAsState(inkColor, label = "ink_color")
    val guide = OmniBorder
    val empty = strokes.isEmpty() && current.isEmpty()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(190.dp)
            .clip(RoundedCornerShape(RadiusMd))
            .background(OmniSurface)
            .border(1.dp, OmniBorder, RoundedCornerShape(RadiusMd))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            current.clear()
                            current.add(InkPoint(offset.x, offset.y))
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            current.add(InkPoint(change.position.x, change.position.y))
                        },
                        onDragEnd = {
                            latestOnStroke(current.toList())
                            current.clear()
                            haptics(Haptic.TICK)
                        },
                        onDragCancel = { current.clear() }
                    )
                }
        ) {
            val baseline = size.height * 0.72f
            drawLine(
                color = guide,
                start = Offset(24.dp.toPx(), baseline),
                end = Offset(size.width - 24.dp.toPx(), baseline),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
            )
            val stroke = Stroke(width = PadStroke.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            (strokes + listOf(current.toList())).forEach { points ->
                if (points.isNotEmpty()) {
                    drawPath(SignatureInk.path(points).asComposePath(), color = color, alpha = inkAlpha.value, style = stroke)
                }
            }
        }
        AnimatedVisibility(
            visible = empty,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 30.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Draw, contentDescription = null, tint = OmniTextMuted, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Firma qui", fontSize = 15.sp, color = OmniTextMuted)
            }
        }
        AnimatedVisibility(
            visible = strokes.isNotEmpty(),
            enter = fadeIn() + scaleIn(initialScale = 0.8f),
            exit = fadeOut() + scaleOut(targetScale = 0.8f),
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Row {
                TextButton(onClick = {
                    onUndo()
                    haptics(Haptic.TAP)
                }) {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null, tint = OmniAccent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Annulla", color = OmniAccent, fontSize = 13.sp)
                }
                TextButton(onClick = {
                    scope.launch {
                        if (!reduce) inkAlpha.animateTo(0f, tween(220))
                        onClear()
                        inkAlpha.snapTo(1f)
                    }
                    haptics(Haptic.REJECT)
                }) { Text("Cancella", color = OmniAccent, fontSize = 13.sp) }
            }
        }
    }
}

@Composable
private fun InkSwatch(choice: SignInk, selected: Boolean, onClick: () -> Unit) {
    val ring by animateColorAsState(if (selected) OmniText else Color.Transparent, label = "ink_ring")
    val scale by animateFloatAsState(if (selected) 1f else 0.86f, motionSpec(Motion.bouncy()), label = "ink_scale")
    Box(
        modifier = Modifier
            .size(40.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .border(2.dp, ring, CircleShape)
            .padding(4.dp)
            .clip(CircleShape)
            .background(Color(choice.argb))
            .clickable(onClickLabel = "Inchiostro ${choice.label}", onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(visible = selected, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
            Icon(Icons.Default.Check, contentDescription = null, tint = OmniSurface, modifier = Modifier.size(18.dp))
        }
    }
}

/** The target page with the signature on top; drag the signature to move it. */
@Composable
private fun PlacementPreview(
    loadKey: Any,
    strokes: List<List<InkPoint>>,
    inkColor: Color,
    strokeWidthPx: Float,
    centerX: Float,
    centerY: Float,
    widthFraction: Float,
    loadPage: suspend (Int) -> android.graphics.Bitmap?,
    onMove: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val widthPx = with(LocalDensity.current) { 300.dp.roundToPx() }
    val page by produceState<android.graphics.Bitmap?>(initialValue = null, loadKey) { value = loadPage(widthPx) }
    var dragging by remember { mutableStateOf(false) }
    val lift by animateFloatAsState(if (dragging) 1.08f else 1f, motionSpec(Motion.bouncy()), label = "sign_lift")
    val animatedWidth by animateFloatAsState(widthFraction, motionSpec(Motion.gentle()), label = "sign_width")
    val latestOnMove by rememberUpdatedState(onMove)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RadiusMd))
            .background(OmniSurface2)
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        val bitmap = page
        if (bitmap == null) {
            Box(modifier = Modifier.height(240.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = OmniAccent, strokeWidth = 3.dp, modifier = Modifier.size(28.dp))
            }
        } else {
            val ratio = bitmap.width.toFloat() / bitmap.height
            BoxWithConstraints(
                modifier = Modifier
                    .heightIn(max = 380.dp)
                    .aspectRatio(ratio)
                    .clip(RoundedCornerShape(6.dp))
                    .border(1.dp, OmniBorder, RoundedCornerShape(6.dp))
            ) {
                Image(
                    bitmap = remember(bitmap) { bitmap.asImageBitmap() },
                    contentDescription = "Pagina da firmare",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
                val density = LocalDensity.current
                val boxW = with(density) { maxWidth.toPx() }
                val boxH = with(density) { maxHeight.toPx() }
                val aspect = SignatureInk.aspect(strokes, strokeWidthPx)
                val sigW = boxW * animatedWidth
                val sigH = sigW / aspect
                val left = (centerX * boxW - sigW / 2f).coerceIn(0f, (boxW - sigW).coerceAtLeast(0f))
                val top = (centerY * boxH - sigH / 2f).coerceIn(0f, (boxH - sigH).coerceAtLeast(0f))
                Box(
                    modifier = Modifier
                        .offset { IntOffset(left.toInt(), top.toInt()) }
                        .size(with(density) { sigW.toDp() }, with(density) { sigH.toDp() })
                        .graphicsLayer {
                            scaleX = lift
                            scaleY = lift
                        }
                        .shadow(if (dragging) 8.dp else 0.dp, RoundedCornerShape(6.dp))
                        .border(1.dp, if (dragging) OmniAccent else OmniAccent.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                        .pointerInput(boxW, boxH) {
                            detectDragGestures(
                                onDragStart = { dragging = true },
                                onDragEnd = { dragging = false },
                                onDragCancel = { dragging = false },
                                onDrag = { change, amount ->
                                    change.consume()
                                    latestOnMove(amount.x / boxW, amount.y / boxH)
                                }
                            )
                        }
                ) {
                    val bounds = SignatureInk.bounds(strokes)
                    if (bounds == null) {
                        Icon(
                            Icons.Default.OpenWith,
                            contentDescription = "Sposta la firma",
                            tint = OmniAccent,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(20.dp)
                        )
                    } else {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val pad = strokeWidthPx
                            val contentW = (bounds[2] - bounds[0]) + pad * 2
                            val scale = size.width / contentW
                            val stroke = Stroke(width = strokeWidthPx * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            strokes.forEach { points ->
                                drawPath(SignatureInk.path(points, pad - bounds[0], pad - bounds[1], scale).asComposePath(), color = inkColor, style = stroke)
                            }
                        }
                    }
                }
            }
        }
    }
}
