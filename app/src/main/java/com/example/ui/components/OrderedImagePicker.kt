package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.input.pointer.PointerEventPass
import com.example.ui.theme.OmniAccentOnInk
import com.example.ui.theme.OmniInk
import com.example.ui.theme.OmniOnInk
import kotlin.math.absoluteValue
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.itemsIndexed as rowItemsIndexed
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.spring
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import com.example.ui.theme.LocalReduceMotion
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.theme.Haptic
import com.example.ui.theme.Motion
import com.example.ui.theme.OmniAccent
import com.example.ui.theme.OmniAccentSoft
import com.example.ui.theme.OmniBg
import com.example.ui.theme.OmniBorder
import com.example.ui.theme.OmniOnAccent
import com.example.ui.theme.OmniSurface
import com.example.ui.theme.OmniSurface2
import com.example.ui.theme.OmniText
import com.example.ui.theme.OmniTextMuted
import com.example.ui.theme.RadiusMd
import com.example.ui.theme.motionSpec
import com.example.ui.theme.rememberHaptics
import com.example.util.GalleryImage
import com.example.util.MediaGallery

private const val ALL_ALBUMS = "Tutte"

/**
 * Returns a launcher for the in-app photo picker: the user taps photos, sees 1, 2, 3… on them and
 * gets the URIs back exactly in that order (system pickers do not guarantee it).
 * When gallery access is denied it falls back to [fallback] (usually the system photo picker).
 */
@Composable
fun rememberOrderedImagePicker(onPicked: (List<Uri>) -> Unit, fallback: () -> Unit): () -> Unit {
    val context = LocalContext.current
    var showPicker by rememberSaveable { mutableStateOf(false) }
    var galleryVersion by remember { mutableIntStateOf(0) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        galleryVersion++
        if (result.values.any { it } || MediaGallery.hasAccess(context)) showPicker = true else fallback()
    }
    if (showPicker) {
        OrderedImagePickerDialog(
            version = galleryVersion,
            onDismiss = { showPicker = false },
            onConfirm = { uris ->
                showPicker = false
                if (uris.isNotEmpty()) onPicked(uris)
            },
            onRequestMore = { permissionLauncher.launch(MediaGallery.permissions()) },
            onUseSystemPicker = {
                showPicker = false
                fallback()
            }
        )
    }
    return {
        if (MediaGallery.hasAccess(context)) showPicker = true else permissionLauncher.launch(MediaGallery.permissions())
    }
}

/**
 * Pure rules of drag-to-select, kept apart from the UI so they can be unit tested.
 * A drag that starts on an unselected photo adds every photo it crosses, in the order crossed;
 * one that starts on a selected photo removes them. Dragging back undoes what was crossed.
 */
internal object DragSelection {
    fun <T> apply(baseline: List<T>, crossed: List<T>, adding: Boolean): List<T> =
        if (adding) baseline + crossed.filter { it !in baseline } else baseline.filter { it !in crossed }

    /** Indices from [anchor] to [current], walked in the direction of the drag. */
    fun range(anchor: Int, current: Int): List<Int> =
        if (current >= anchor) (anchor..current).toList() else (anchor downTo current).toList()
}

/** "Oggi", "Ieri" or a short date, used to group the gallery by day. */
internal fun dayLabel(timestamp: Long, now: Long = System.currentTimeMillis()): String {
    val day = Calendar.getInstance().apply { timeInMillis = timestamp }
    val today = Calendar.getInstance().apply { timeInMillis = now }
    fun sameDay(a: Calendar, b: Calendar) = a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
    if (sameDay(day, today)) return "Oggi"
    val yesterday = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
    if (sameDay(day, yesterday)) return "Ieri"
    val pattern = if (day.get(Calendar.YEAR) == today.get(Calendar.YEAR)) "EEE d MMMM" else "d MMMM yyyy"
    return SimpleDateFormat(pattern, Locale.ITALIAN).format(day.time).replaceFirstChar { it.uppercase() }
}

private const val HEADER_PREFIX = "day:"

@Composable
private fun OrderedImagePickerDialog(
    version: Int,
    onDismiss: () -> Unit,
    onConfirm: (List<Uri>) -> Unit,
    onRequestMore: () -> Unit,
    onUseSystemPicker: () -> Unit
) {
    val context = LocalContext.current
    val haptics = rememberHaptics()
    var images by remember { mutableStateOf<List<GalleryImage>?>(null) }
    val selected = remember { mutableStateListOf<Uri>() }
    var album by rememberSaveable { mutableStateOf(ALL_ALBUMS) }
    var oldestFirst by rememberSaveable { mutableStateOf(false) }
    var cellSize by rememberSaveable { mutableFloatStateOf(104f) }
    var previewIndex by remember { mutableStateOf<Int?>(null) }
    val partial = remember(version) { MediaGallery.isPartialAccess(context) }
    val gridState = rememberLazyGridState()

    LaunchedEffect(version) { images = MediaGallery.load(context) }

    fun replaceSelection(newList: List<Uri>) {
        selected.clear()
        selected.addAll(newList)
    }

    fun toggle(uri: Uri) {
        // Looked up at tap time: two quick taps can arrive before recomposition.
        val current = selected.indexOf(uri)
        if (current >= 0) {
            selected.removeAt(current)
            haptics(Haptic.TICK)
        } else {
            selected.add(uri)
            haptics(Haptic.TAP)
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = OmniBg) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                val list = images
                val visible = remember(list, album, oldestFirst) {
                    list.orEmpty()
                        .let { all -> if (album == ALL_ALBUMS) all else all.filter { it.album == album } }
                        .let { if (oldestFirst) it.asReversed() else it }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 8.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Chiudi", tint = OmniText) }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Scegli le pagine", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = OmniText)
                        AnimatedText(
                            text = if (selected.isEmpty()) "Tocca le foto nell'ordine delle pagine: 1, 2, 3…" else "${selected.size} selezionate · tocca di nuovo per togliere",
                            fontSize = 13.sp,
                            color = OmniTextMuted
                        )
                    }
                    AnimatedContent(
                        targetState = selected.isEmpty(),
                        transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.9f)) togetherWith (fadeOut() + scaleOut(targetScale = 0.9f)) },
                        label = "picker_bulk_action"
                    ) { empty ->
                        if (empty) {
                            TextButton(onClick = {
                                replaceSelection(visible.map { it.uri })
                                haptics(Haptic.CONFIRM)
                            }, enabled = visible.isNotEmpty()) { Text("Seleziona tutte", color = OmniAccent) }
                        } else {
                            TextButton(onClick = { selected.clear() }) { Text("Azzera", color = OmniAccent) }
                        }
                    }
                }

                val albums = remember(list) {
                    listOf(ALL_ALBUMS) + list.orEmpty().groupingBy { it.album }.eachCount().entries
                        .sortedByDescending { it.value }.take(12).map { it.key }.filter { it != ALL_ALBUMS }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (albums.size > 2) {
                            MorphChipRow(options = albums, selected = album, onSelected = { album = it })
                        }
                    }
                    SortToggle(oldestFirst = oldestFirst, onToggle = {
                        oldestFirst = !oldestFirst
                        haptics(Haptic.TAP)
                    })
                }
                if (partial) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(RadiusMd))
                            .background(OmniAccentSoft)
                            .padding(start = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Vedi solo le foto che hai consentito", fontSize = 13.sp, color = OmniText, modifier = Modifier.weight(1f))
                        TextButton(onClick = onRequestMore) { Text("Aggiungi", color = OmniAccent, fontWeight = FontWeight.SemiBold) }
                    }
                }
                DragHint(visible = list != null && list.isNotEmpty() && selected.size in 1..2)

                Box(modifier = Modifier.weight(1f)) {
                    when {
                        list == null -> CircularProgressIndicator(color = OmniAccent, modifier = Modifier.align(Alignment.Center))
                        list.isEmpty() -> EmptyState(
                            icon = Icons.Default.PhotoLibrary,
                            title = "Nessuna foto trovata",
                            message = "Prova con il selettore di Android o scatta una foto.",
                            modifier = Modifier.align(Alignment.Center)
                        )
                        else -> GalleryGrid(
                            images = visible,
                            selected = selected,
                            gridState = gridState,
                            onToggle = ::toggle,
                            onSelectionChange = ::replaceSelection,
                            onDragStep = { haptics(Haptic.TICK) },
                            cellSize = cellSize,
                            onCellSizeChange = { cellSize = it },
                            onPreview = { previewIndex = it }
                        )
                    }
                }

                SelectionTray(
                    selected = selected,
                    onRemove = { uri ->
                        selected.remove(uri)
                        haptics(Haptic.TICK)
                    },
                    onMove = { from, to ->
                        selected.add(to, selected.removeAt(from))
                        haptics(Haptic.TICK)
                    }
                )

                Surface(color = OmniSurface, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onUseSystemPicker) {
                            Text("Selettore di Android", color = OmniTextMuted, fontSize = 13.sp, textAlign = TextAlign.Start)
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        AnimatedVisibility(visible = selected.size > 1, enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
                            IconButton(onClick = {
                                replaceSelection(selected.reversed())
                                haptics(Haptic.TAP)
                            }) {
                                Icon(Icons.Default.SwapVert, contentDescription = "Inverti ordine", tint = OmniText)
                            }
                        }
                        Button(
                            onClick = {
                                haptics(Haptic.CONFIRM)
                                onConfirm(selected.toList())
                            },
                            enabled = selected.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = OmniAccent,
                                contentColor = OmniOnAccent,
                                disabledContainerColor = OmniSurface2,
                                disabledContentColor = OmniTextMuted
                            ),
                            modifier = Modifier.height(48.dp)
                        ) {
                            AnimatedText(
                                text = when (selected.size) {
                                    0 -> "Aggiungi"
                                    1 -> "Aggiungi 1 pagina"
                                    else -> "Aggiungi ${selected.size} pagine"
                                },
                                fontWeight = FontWeight.SemiBold,
                                color = if (selected.isEmpty()) OmniTextMuted else OmniOnAccent
                            )
                        }
                    }
                }
            }
        }
        val start = previewIndex
        val list = images
        if (start != null && list != null) {
            val shown = list
                .let { all -> if (album == ALL_ALBUMS) all else all.filter { it.album == album } }
                .let { if (oldestFirst) it.asReversed() else it }
            PhotoPreview(
                images = shown,
                startIndex = start.coerceIn(0, (shown.size - 1).coerceAtLeast(0)),
                selected = selected,
                onToggle = ::toggle,
                onDismiss = { previewIndex = null }
            )
        }
    }
}

/** Tip shown after the first taps, until the user has picked a few photos. */
@Composable
private fun DragHint(visible: Boolean) {
    AnimatedVisibility(visible = visible, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.TouchApp, contentDescription = null, tint = OmniAccent, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Tieni premuto e trascina per selezionarne tante; in basso trascina per riordinare", fontSize = 12.sp, color = OmniTextMuted)
        }
    }
}

/**
 * Photo grid grouped by day. Long-press then drag selects (or deselects) every photo crossed,
 * in the order crossed; the grid scrolls by itself near the top and bottom edges.
 */
@Composable
private fun GalleryGrid(
    images: List<GalleryImage>,
    selected: List<Uri>,
    gridState: LazyGridState,
    onToggle: (Uri) -> Unit,
    onSelectionChange: (List<Uri>) -> Unit,
    onDragStep: () -> Unit,
    cellSize: Float,
    onCellSizeChange: (Float) -> Unit,
    onPreview: (Int) -> Unit
) {
    val reduce = LocalReduceMotion.current
    val currentCellSize by androidx.compose.runtime.rememberUpdatedState(cellSize)
    val groups = remember(images) { images.groupBy { dayLabel(it.dateTaken) }.toList() }
    val indexOfKey = remember(images) { images.withIndex().associate { (i, img) -> img.uri.toString() to i } }

    // Drag state lives in plain remembered holders: the gesture reads and writes them every frame.
    val drag = remember { DragState() }
    var autoScroll by remember { mutableFloatStateOf(0f) }
    val currentSelected by androidx.compose.runtime.rememberUpdatedState(selected)
    val currentImages by androidx.compose.runtime.rememberUpdatedState(images)

    LaunchedEffect(autoScroll) {
        if (autoScroll == 0f) return@LaunchedEffect
        while (isActive) {
            gridState.scrollBy(autoScroll)
            delay(16)
        }
    }

    fun indexAt(position: Offset): Int? = gridState.layoutInfo.visibleItemsInfo.firstOrNull { info ->
        position.x >= info.offset.x && position.x < info.offset.x + info.size.width &&
            position.y >= info.offset.y && position.y < info.offset.y + info.size.height
    }?.let { indexOfKey[it.key as? String] }

    fun update(index: Int) {
        val anchor = drag.anchor ?: return
        if (index == drag.last) return
        drag.last = index
        val crossed = DragSelection.range(anchor, index).mapNotNull { currentImages.getOrNull(it)?.uri }
        onSelectionChange(DragSelection.apply(drag.baseline, crossed, drag.adding))
        onDragStep()
    }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Adaptive(minSize = cellSize.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxSize()
            // Pinch with two fingers to show bigger or smaller photos. Read in the Initial pass so the
            // grid does not scroll while pinching; single-finger gestures pass through untouched.
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.changes.count { it.pressed } >= 2) {
                            val zoom = event.calculateZoom()
                            if (zoom != 1f) {
                                onCellSizeChange((currentCellSize * zoom).coerceIn(MIN_CELL_DP, MAX_CELL_DP))
                                event.changes.forEach { it.consume() }
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
            .pointerInput(images) {
                val edge = 64.dp.toPx()
                val maxSpeed = 22.dp.toPx()
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        val index = indexAt(offset) ?: return@detectDragGesturesAfterLongPress
                        val uri = currentImages[index].uri
                        drag.anchor = index
                        drag.last = -1
                        drag.baseline = currentSelected.toList()
                        drag.adding = uri !in currentSelected
                        update(index)
                    },
                    onDrag = { change, _ ->
                        if (drag.anchor == null) return@detectDragGesturesAfterLongPress
                        val y = change.position.y
                        autoScroll = when {
                            reduce -> 0f
                            y < edge -> -maxSpeed * (1f - (y / edge).coerceIn(0f, 1f))
                            y > size.height - edge -> maxSpeed * (1f - ((size.height - y) / edge).coerceIn(0f, 1f))
                            else -> 0f
                        }
                        indexAt(change.position)?.let(::update)
                    },
                    onDragEnd = {
                        drag.anchor = null
                        autoScroll = 0f
                    },
                    onDragCancel = {
                        drag.anchor = null
                        autoScroll = 0f
                    }
                )
            }
    ) {
        groups.forEach { (label, dayImages) ->
            item(key = HEADER_PREFIX + label, span = { GridItemSpan(maxLineSpan) }) {
                DayHeader(
                    label = label,
                    allSelected = dayImages.all { it.uri in selected },
                    onToggleDay = {
                        val uris = dayImages.map { it.uri }
                        onSelectionChange(if (uris.all { it in selected }) selected.filter { it !in uris } else selected + uris.filter { it !in selected })
                        onDragStep()
                    },
                    modifier = Modifier.animateItem()
                )
            }
            items(dayImages, key = { it.uri.toString() }) { image ->
                val order = selected.indexOf(image.uri)
                GalleryCell(
                    image = image,
                    order = if (order >= 0) order + 1 else null,
                    onClick = { onToggle(image.uri) },
                    onPreview = { indexOfKey[image.uri.toString()]?.let(onPreview) },
                    compact = cellSize < 86f,
                    modifier = Modifier.animateItem(placementSpec = spring(dampingRatio = 0.85f, stiffness = 500f))
                )
            }
        }
    }
}

private class DragState {
    var anchor: Int? = null
    var last: Int = -1
    var baseline: List<Uri> = emptyList()
    var adding: Boolean = true
}

@Composable
private fun DayHeader(label: String, allSelected: Boolean, onToggleDay: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 10.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = OmniText, modifier = Modifier.weight(1f))
        TextButton(onClick = onToggleDay) {
            AnimatedText(
                text = if (allSelected) "Togli" else "Seleziona",
                fontSize = 13.sp,
                color = OmniAccent
            )
        }
    }
}

/** The chosen pages in order, as a strip of thumbnails; tapping one removes it. */
@Composable
private fun SelectionTray(selected: List<Uri>, onRemove: (Uri) -> Unit, onMove: (Int, Int) -> Unit) {
    val context = LocalContext.current
    val rowState = rememberLazyListState()
    // Long-press a thumbnail and drag it sideways to change the page order.
    var draggingKey by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val currentSelected by androidx.compose.runtime.rememberUpdatedState(selected)
    LaunchedEffect(selected.size) {
        if (selected.isNotEmpty()) rowState.animateScrollToItem(selected.lastIndex)
    }
    AnimatedVisibility(visible = selected.isNotEmpty(), enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
        LazyRow(
            state = rowState,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(OmniSurface2.copy(alpha = 0.6f))
                .pointerInput(Unit) {
                    fun itemAt(x: Float) = rowState.layoutInfo.visibleItemsInfo.firstOrNull { x >= it.offset && x < it.offset + it.size }
                    detectDragGesturesAfterLongPress(
                        onDragStart = { offset ->
                            draggingKey = itemAt(offset.x)?.key as? String
                            dragOffset = 0f
                        },
                        onDrag = { change, amount ->
                            val key = draggingKey ?: return@detectDragGesturesAfterLongPress
                            change.consume()
                            dragOffset += amount.x
                            val items = rowState.layoutInfo.visibleItemsInfo
                            val current = items.firstOrNull { it.key == key } ?: return@detectDragGesturesAfterLongPress
                            val center = current.offset + current.size / 2f + dragOffset
                            val target = items.firstOrNull { center >= it.offset && center < it.offset + it.size }
                            if (target != null && target.key != key) {
                                val from = currentSelected.indexOfFirst { it.toString() == key }
                                val to = currentSelected.indexOfFirst { it.toString() == target.key }
                                if (from >= 0 && to >= 0) {
                                    onMove(from, to)
                                    // The item jumped to its new slot: keep it under the finger.
                                    dragOffset += current.offset - target.offset
                                }
                            }
                        },
                        onDragEnd = { draggingKey = null; dragOffset = 0f },
                        onDragCancel = { draggingKey = null; dragOffset = 0f }
                    )
                }
        ) {
            rowItemsIndexed(selected, key = { _, uri -> uri.toString() }) { index, uri ->
                val isDragging = draggingKey == uri.toString()
                val lift by animateFloatAsState(if (isDragging) 1.12f else 1f, motionSpec(Motion.bouncy()), label = "tray_lift")
                Box(
                    modifier = Modifier
                        .then(if (isDragging) Modifier.zIndex(1f) else Modifier.animateItem())
                        .graphicsLayer {
                            translationX = if (isDragging) dragOffset else 0f
                            scaleX = lift
                            scaleY = lift
                        }
                        .size(56.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(OmniSurface)
                        .clickable(onClickLabel = "Togli la pagina ${index + 1}") { onRemove(uri) }
                ) {
                    AsyncImage(
                        model = remember(uri) { ImageRequest.Builder(context).data(uri).size(160).build() },
                        contentDescription = "Pagina ${index + 1}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(3.dp)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(OmniText),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedText("${index + 1}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = OmniBg)
                    }
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        tint = OmniOnAccent,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(3.dp)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(OmniAccent)
                            .padding(2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun GalleryCell(
    image: GalleryImage,
    order: Int?,
    onClick: () -> Unit,
    onPreview: () -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isSelected = order != null
    val inset by animateDpAsState(if (isSelected) 6.dp else 0.dp, motionSpec(Motion.snappy()), label = "cell_inset")
    val border by animateColorAsState(if (isSelected) OmniAccent else OmniBorder.copy(alpha = 0f), label = "cell_border")
    val dim by animateFloatAsState(if (isSelected) 0.9f else 1f, motionSpec(Motion.snappy()), label = "cell_scale")
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(OmniSurface2)
            .border(3.dp, border, RoundedCornerShape(10.dp))
            .clickable(onClickLabel = if (isSelected) "Togli la pagina $order" else "Aggiungi come pagina", onClick = onClick)
    ) {
        AsyncImage(
            model = remember(image.uri) {
                ImageRequest.Builder(context).data(image.uri).size(320).crossfade(true).build()
            },
            contentDescription = image.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                // The spring overshoots below 0 when a photo is deselected; negative padding crashes.
                .padding(inset.coerceAtLeast(0.dp))
                .clip(RoundedCornerShape(8.dp))
                .scale(dim)
        )
        AnimatedVisibility(
            visible = isSelected,
            enter = scaleIn(motionSpec(Motion.bouncy())) + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(OmniAccent)
                    .border(2.dp, OmniOnAccent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                AnimatedText("${order ?: ""}", color = OmniOnAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
        // Small cells skip the preview button: it would cover most of the photo.
        AnimatedVisibility(
            visible = !compact,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomStart)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clickable(onClickLabel = "Anteprima", onClick = onPreview),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.OpenInFull,
                    contentDescription = "Anteprima",
                    tint = OmniOnInk,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(OmniInk.copy(alpha = 0.55f))
                        .padding(5.dp)
                )
            }
        }
    }
}

private const val MIN_CELL_DP = 72f
private const val MAX_CELL_DP = 200f

@Composable
private fun SortToggle(oldestFirst: Boolean, onToggle: () -> Unit) {
    val rotation by animateFloatAsState(if (oldestFirst) 180f else 0f, motionSpec(Motion.bouncy()), label = "sort_rotation")
    TextButton(onClick = onToggle) {
        Icon(
            Icons.Default.ArrowDownward,
            contentDescription = null,
            tint = OmniAccent,
            modifier = Modifier
                .size(16.dp)
                .graphicsLayer { rotationZ = rotation }
        )
        Spacer(modifier = Modifier.width(4.dp))
        AnimatedText(if (oldestFirst) "Meno recenti" else "Più recenti", fontSize = 13.sp, color = OmniAccent)
    }
}

/** Full-screen viewer: swipe between photos and add or remove the current one as a page. */
@Composable
private fun PhotoPreview(
    images: List<GalleryImage>,
    startIndex: Int,
    selected: List<Uri>,
    onToggle: (Uri) -> Unit,
    onDismiss: () -> Unit
) {
    if (images.isEmpty()) return
    val context = LocalContext.current
    val pager = rememberPagerState(initialPage = startIndex) { images.size }
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        AnimatedVisibility(visible = shown, enter = fadeIn() + scaleIn(initialScale = 0.92f), exit = fadeOut()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(OmniInk)
            ) {
                HorizontalPager(state = pager, beyondViewportPageCount = 1, modifier = Modifier.fillMaxSize()) { page ->
                    val image = images[page]
                    AsyncImage(
                        model = remember(image.uri) { ImageRequest.Builder(context).data(image.uri).size(2048).crossfade(true).build() },
                        contentDescription = image.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                // Pages shrink and fade a little while they slide away.
                                val offset = ((pager.currentPage - page) + pager.currentPageOffsetFraction).absoluteValue.coerceIn(0f, 1f)
                                val scale = 1f - 0.08f * offset
                                scaleX = scale
                                scaleY = scale
                                alpha = 1f - 0.4f * offset
                            }
                    )
                }
                val current = images[pager.currentPage.coerceIn(0, images.lastIndex)]
                val order = selected.indexOf(current.uri)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Chiudi anteprima", tint = OmniOnInk) }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(current.name, color = OmniOnInk, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        AnimatedText("${pager.currentPage + 1} di ${images.size} · ${dayLabel(current.dateTaken)}", fontSize = 12.sp, color = OmniOnInk.copy(alpha = 0.7f))
                    }
                }
                Button(
                    onClick = { onToggle(current.uri) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (order >= 0) OmniOnInk.copy(alpha = 0.16f) else OmniAccentOnInk,
                        contentColor = if (order >= 0) OmniOnInk else OmniInk
                    ),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(24.dp)
                        .height(52.dp)
                ) {
                    AnimatedText(
                        text = if (order >= 0) "Togli pagina ${order + 1}" else "Aggiungi come pagina ${selected.size + 1}",
                        fontWeight = FontWeight.SemiBold,
                        color = if (order >= 0) OmniOnInk else OmniInk
                    )
                }
            }
        }
    }
}
