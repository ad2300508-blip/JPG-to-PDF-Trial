package com.example.ui

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.morphTextFieldColors
import com.example.ui.components.shareFile
import com.example.ui.theme.Motion
import com.example.ui.theme.OmniAccent
import com.example.ui.theme.OmniBorder
import com.example.ui.theme.OmniInk
import com.example.ui.theme.OmniOnAccent
import com.example.ui.theme.OmniSurface
import com.example.ui.theme.OmniSurface2
import com.example.ui.theme.OmniText
import com.example.ui.theme.OmniTextMuted
import com.example.ui.theme.RadiusPill
import com.example.ui.theme.RadiusSm
import com.example.ui.theme.TintPdf
import com.example.ui.theme.motionSpec
import com.example.util.PhotoFilterMath
import com.example.viewmodel.LoadedPdf
import com.example.viewmodel.PdfReaderViewModel
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

/** Plain colour inversion: white paper becomes black, text becomes light ("night" reading). */
private val INVERT = floatArrayOf(
    -1f, 0f, 0f, 0f, 255f,
    0f, -1f, 0f, 0f, 255f,
    0f, 0f, -1f, 0f, 255f,
    0f, 0f, 0f, 1f, 0f
)

@Composable
fun PdfReaderScreen(viewModel: PdfReaderViewModel, modifier: Modifier = Modifier) {
    val source by viewModel.source.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val night by viewModel.night.collectAsState()
    val showThumbnails by viewModel.showThumbnails.collectAsState()
    val context = LocalContext.current

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.loadPdf(uri)
    }

    Box(modifier = modifier.fillMaxSize()) {
        SinglePdfScaffold(
            title = "Leggi PDF",
            description = "Sfoglia, ingrandisci e leggi anche di notte",
            source = source,
            isLoading = isLoading,
            isWorking = false,
            progress = Pair(0, 0),
            progressLabel = "",
            errorMessage = errorMessage,
            onDismissError = { viewModel.dismissError() },
            onCancel = {},
            onPick = { picker.launch("application/pdf") },
            onClear = { viewModel.clear() },
            dropIcon = Icons.AutoMirrored.Filled.MenuBook,
            dropHeadline = "Scegli il PDF da leggere",
            dropHelp = "Scorri le pagine, ingrandisci con due dita o con un doppio tocco e attiva la modalità notte",
            tint = TintPdf,
            headerActions = {
                val pdf = source
                if (pdf != null) {
                    NightToggle(night = night, onToggle = { viewModel.toggleNight() })
                    IconButton(onClick = { shareFile(context, pdf.uri, "application/pdf") }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.Share, contentDescription = "Condividi il PDF", tint = OmniText)
                    }
                }
            }
        ) { pdf ->
            PdfReader(
                pdf = pdf,
                viewModel = viewModel,
                night = night,
                showThumbnails = showThumbnails
            )
        }
    }
}

@Composable
private fun NightToggle(night: Boolean, onToggle: () -> Unit) {
    val spin by animateFloatAsState(if (night) 360f else 0f, motionSpec(Motion.bouncy()), label = "night_spin")
    IconButton(onClick = onToggle, modifier = Modifier.size(40.dp)) {
        AnimatedContent(
            targetState = night,
            transitionSpec = { (scaleIn(initialScale = 0.4f) + fadeIn()) togetherWith (scaleOut(targetScale = 0.4f) + fadeOut()) },
            label = "night_icon"
        ) { isNight ->
            Icon(
                if (isNight) Icons.Default.LightMode else Icons.Default.DarkMode,
                contentDescription = if (isNight) "Modalità giorno" else "Modalità notte",
                tint = if (isNight) OmniAccent else OmniText,
                modifier = Modifier.graphicsLayer { rotationZ = spin }
            )
        }
    }
}

@Composable
private fun PdfReader(pdf: LoadedPdf, viewModel: PdfReaderViewModel, night: Boolean, showThumbnails: Boolean) {
    val total = pdf.pageCount.coerceAtLeast(1)
    val startPage = remember(pdf.uri) { viewModel.currentPage.value.coerceIn(0, total - 1) }
    val pagerState = rememberPagerState(initialPage = startPage) { total }
    val scope = rememberCoroutineScope()
    var zoomed by remember { mutableStateOf(false) }
    var askPage by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect {
            viewModel.setCurrentPage(it)
            zoomed = false
        }
    }

    // Night mode fades the page colours from normal to inverted instead of flashing.
    val nightAmount by animateFloatAsState(if (night) 1f else 0f, motionSpec(tween(Motion.LONG)), label = "night_amount")
    val colorFilter = if (nightAmount > 0.001f) {
        ColorFilter.colorMatrix(ColorMatrix(PhotoFilterMath.lerp(PhotoFilterMath.IDENTITY, INVERT, nightAmount)))
    } else {
        null
    }
    val backdrop by animateColorAsState(if (night) OmniInk else OmniSurface2, motionSpec(tween(Motion.LONG)), label = "reader_backdrop")

    Column(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(RadiusSm))
                .background(backdrop)
        ) {
            val widthPx = with(LocalDensity.current) { maxWidth.roundToPx() }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1,
                userScrollEnabled = !zoomed,
                pageSpacing = 12.dp,
                key = { it }
            ) { page ->
                ZoomablePage(
                    index = page,
                    widthPx = widthPx,
                    isCurrent = page == pagerState.currentPage,
                    viewModel = viewModel,
                    colorFilter = colorFilter,
                    onZoomChange = { if (page == pagerState.currentPage) zoomed = it },
                    modifier = Modifier.graphicsLayer {
                        // Neighbouring pages shrink and fade slightly while you swipe.
                        val offset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue.coerceIn(0f, 1f)
                        val s = 1f - 0.08f * offset
                        scaleX = s
                        scaleY = s
                        alpha = 1f - 0.45f * offset
                    }
                )
            }
        }
        AnimatedVisibility(
            visible = showThumbnails,
            enter = expandVertically(motionSpec(Motion.gentle())) + fadeIn(),
            exit = shrinkVertically(tween(Motion.MEDIUM)) + fadeOut(tween(Motion.SHORT))
        ) {
            ThumbnailStrip(
                total = total,
                current = pagerState.currentPage,
                viewModel = viewModel,
                onSelect = { scope.launch { pagerState.animateScrollToPage(it) } }
            )
        }
        ReaderControls(
            current = pagerState.currentPage,
            total = total,
            thumbnailsVisible = showThumbnails,
            onPrevious = { scope.launch { pagerState.animateScrollToPage((pagerState.currentPage - 1).coerceAtLeast(0)) } },
            onNext = { scope.launch { pagerState.animateScrollToPage((pagerState.currentPage + 1).coerceAtMost(total - 1)) } },
            onAskPage = { askPage = true },
            onToggleThumbnails = { viewModel.toggleThumbnails() }
        )
    }

    if (askPage) {
        GoToPageDialog(
            total = total,
            current = pagerState.currentPage,
            onGo = { target ->
                askPage = false
                scope.launch { pagerState.animateScrollToPage(target) }
            },
            onDismiss = { askPage = false }
        )
    }
}

/** One page: pinch or double-tap to zoom, drag to pan while zoomed; at 1× the pager keeps swiping. */
@Composable
private fun ZoomablePage(
    index: Int,
    widthPx: Int,
    isCurrent: Boolean,
    viewModel: PdfReaderViewModel,
    colorFilter: ColorFilter?,
    onZoomChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val bitmap by produceState<Bitmap?>(initialValue = null, index, widthPx) {
        value = viewModel.page(index, (widthPx * 1.3f).roundToInt())
    }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val scope = rememberCoroutineScope()
    val zoomSpec = motionSpec<Float>(Motion.gentle())
    val latestOnZoom by rememberUpdatedState(onZoomChange)

    LaunchedEffect(isCurrent) {
        if (!isCurrent) {
            scale = 1f
            offset = Offset.Zero
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val boxW = constraints.maxWidth.toFloat()
        val boxH = constraints.maxHeight.toFloat()
        fun clamp(o: Offset, s: Float): Offset {
            val maxX = boxW * (s - 1f) / 2f
            val maxY = boxH * (s - 1f) / 2f
            return Offset(o.x.coerceIn(-maxX, maxX), o.y.coerceIn(-maxY, maxY))
        }
        val page = bitmap
        if (page == null) {
            CircularProgressIndicator(color = OmniAccent, strokeWidth = 3.dp, modifier = Modifier.size(36.dp))
        } else {
            Image(
                bitmap = page.asImageBitmap(),
                contentDescription = "Pagina ${index + 1}",
                contentScale = ContentScale.Fit,
                colorFilter = colorFilter,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(onDoubleTap = { tap ->
                            val from = scale
                            val to = if (from > 1.05f) 1f else 2.5f
                            val startOffset = offset
                            val targetOffset = if (to == 1f) Offset.Zero else clamp((Offset(boxW / 2f, boxH / 2f) - tap) * (to - 1f), to)
                            latestOnZoom(to > 1f)
                            scope.launch {
                                animate(0f, 1f, animationSpec = zoomSpec) { t, _ ->
                                    scale = from + (to - from) * t
                                    offset = startOffset + (targetOffset - startOffset) * t
                                }
                            }
                        })
                    }
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            do {
                                val event = awaitPointerEvent()
                                val fingers = event.changes.count { it.pressed }
                                if (fingers >= 2 || scale > 1.01f) {
                                    val newScale = (scale * event.calculateZoom()).coerceIn(1f, 5f)
                                    offset = clamp(offset + event.calculatePan(), newScale)
                                    scale = newScale
                                    latestOnZoom(newScale > 1.01f)
                                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                                }
                            } while (event.changes.any { it.pressed })
                        }
                    }
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
            )
        }
    }
}

@Composable
private fun ReaderControls(
    current: Int,
    total: Int,
    thumbnailsVisible: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onAskPage: () -> Unit,
    onToggleThumbnails: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious, enabled = current > 0, modifier = Modifier.size(44.dp)) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Pagina precedente", tint = if (current > 0) OmniText else OmniTextMuted)
        }
        Spacer(modifier = Modifier.weight(1f))
        Surface(
            onClick = onAskPage,
            shape = RoundedCornerShape(RadiusPill),
            color = OmniSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, OmniBorder),
            modifier = Modifier.height(40.dp)
        ) {
            Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Pagina ", fontSize = 14.sp, color = OmniTextMuted)
                AnimatedContent(
                    targetState = current,
                    transitionSpec = {
                        val forward = targetState > initialState
                        (slideInVertically { if (forward) it else -it } + fadeIn()) togetherWith
                            (slideOutVertically { if (forward) -it else it } + fadeOut())
                    },
                    label = "reader_page_number"
                ) { page ->
                    Text("${page + 1}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = OmniText)
                }
                Text(" di $total", fontSize = 14.sp, color = OmniTextMuted)
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = onToggleThumbnails, modifier = Modifier.size(44.dp)) {
            Icon(
                Icons.Default.ViewCarousel,
                contentDescription = if (thumbnailsVisible) "Nascondi miniature" else "Mostra miniature",
                tint = if (thumbnailsVisible) OmniAccent else OmniText
            )
        }
        IconButton(onClick = onNext, enabled = current < total - 1, modifier = Modifier.size(44.dp)) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Pagina successiva", tint = if (current < total - 1) OmniText else OmniTextMuted)
        }
    }
}

/** Small page previews; the one on screen is outlined and slightly larger. */
@Composable
private fun ThumbnailStrip(total: Int, current: Int, viewModel: PdfReaderViewModel, onSelect: (Int) -> Unit) {
    val listState = rememberLazyListState()
    LaunchedEffect(current) { listState.animateScrollToItem((current - 2).coerceAtLeast(0)) }
    LazyRow(
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 10.dp, horizontal = 2.dp)
    ) {
        items(total, key = { it }) { index ->
            val selected = index == current
            val scale by animateFloatAsState(if (selected) 1.08f else 0.94f, motionSpec(Motion.bouncy()), label = "thumb_scale")
            val ring by animateColorAsState(if (selected) OmniAccent else OmniBorder, label = "thumb_ring")
            val thumb by produceState<Bitmap?>(initialValue = null, index) { value = viewModel.page(index, THUMB_WIDTH) }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(width = 52.dp, height = 72.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .clip(RoundedCornerShape(8.dp))
                        .background(OmniSurface2)
                        .border(2.dp, ring, RoundedCornerShape(8.dp))
                        .clickable(onClickLabel = "Vai alla pagina ${index + 1}") { onSelect(index) },
                    contentAlignment = Alignment.Center
                ) {
                    val bmp = thumb
                    if (bmp != null) {
                        Image(bitmap = bmp.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    }
                }
                Text("${index + 1}", fontSize = 11.sp, color = if (selected) OmniAccent else OmniTextMuted, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun GoToPageDialog(total: Int, current: Int, onGo: (Int) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("${current + 1}") }
    var slider by remember { mutableFloatStateOf((current + 1).toFloat()) }
    val typed = text.toIntOrNull()
    val valid = typed != null && typed in 1..total
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = OmniSurface,
        title = { Text("Vai alla pagina", color = OmniText) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { value ->
                        text = value.filter { it.isDigit() }.take(5)
                        text.toIntOrNull()?.takeIf { it in 1..total }?.let { slider = it.toFloat() }
                    },
                    singleLine = true,
                    isError = text.isNotEmpty() && !valid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = { Text("Da 1 a $total", color = OmniTextMuted) },
                    colors = morphTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (total > 1) {
                    Slider(
                        value = slider,
                        onValueChange = {
                            slider = it
                            text = "${it.roundToInt()}"
                        },
                        valueRange = 1f..total.toFloat(),
                        colors = SliderDefaults.colors(thumbColor = OmniAccent, activeTrackColor = OmniAccent)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { typed?.let { onGo(it - 1) } }, enabled = valid) {
                Text("Vai", color = if (valid) OmniAccent else OmniTextMuted, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla", color = OmniTextMuted) }
        }
    )
}

private const val THUMB_WIDTH = 160
