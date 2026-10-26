package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Gif
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.service.GifFit
import com.example.service.GifSequence
import com.example.service.GifSettings
import com.example.ui.components.AnimatedText
import com.example.ui.components.Dropzone
import com.example.ui.components.FileListHeader
import com.example.ui.components.OmniActionBar
import com.example.ui.components.OmniProgressBanner
import com.example.ui.components.OmniSlider
import com.example.ui.components.OmniToast
import com.example.ui.components.OptionRow
import com.example.ui.components.OptionSection
import com.example.ui.components.RevealVertically
import com.example.ui.components.SegmentedControl
import com.example.ui.components.ToolLayout
import com.example.ui.components.formatBytes
import com.example.ui.components.openFile
import com.example.ui.components.rememberCameraCapture
import com.example.ui.theme.Haptic
import com.example.ui.theme.Motion
import com.example.ui.theme.OmniAccent
import com.example.ui.theme.OmniAccentSoft
import com.example.ui.theme.OmniBorder
import com.example.ui.theme.OmniDanger
import com.example.ui.theme.OmniOnAccent
import com.example.ui.theme.OmniSurface2
import com.example.ui.theme.OmniText
import com.example.ui.theme.OmniTextMuted
import com.example.ui.theme.RadiusSm
import com.example.ui.theme.TintImage
import com.example.ui.theme.motionSpec
import com.example.ui.theme.rememberHaptics
import com.example.util.OutputFolder
import com.example.util.OutputStorage
import com.example.viewmodel.GifFrameItem
import com.example.viewmodel.GifViewModel
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun GifMakerScreen(viewModel: GifViewModel, modifier: Modifier = Modifier) {
    val frames by viewModel.frames.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val result by viewModel.result.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val context = LocalContext.current
    val haptics = rememberHaptics()

    val pickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        viewModel.addImages(uris)
    }
    val launchCamera = rememberCameraCapture { uri -> viewModel.addImages(listOf(uri)) }

    var playing by remember { mutableStateOf(true) }
    var step by remember { mutableIntStateOf(0) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    val order = GifSequence.order(frames.size, settings.boomerang)
    if (step >= order.size) step = 0
    if (selectedId != null && frames.none { it.id == selectedId }) selectedId = null

    LaunchedEffect(playing, order.size, settings.frameMs, settings.loopForever) {
        while (playing && order.size > 1) {
            delay(settings.frameMs.toLong())
            if (!settings.loopForever && step == order.lastIndex) {
                playing = false
            } else {
                step = (step + 1) % order.size
            }
        }
    }

    val (outW, outH) = GifSequence.canvas(frames.firstOrNull()?.aspect ?: 1f, settings.maxSide)
    val totalSeconds = order.size * settings.frameMs / 1000f

    Column(modifier = modifier.fillMaxSize().testTag("gif_screen")) {
        RevealVertically(visible = errorMessage != null) {
            OmniToast(
                message = errorMessage.orEmpty(),
                isError = true,
                actionLabel = "Chiudi",
                onAction = { viewModel.dismissError() },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }
        RevealVertically(visible = result != null && progress == null) {
            val done = result
            OmniToast(
                message = "GIF salvata in ${OutputStorage.displayPath(OutputFolder.IMAGES)}" + (done?.let { " · ${formatBytes(it.sizeBytes)}" } ?: ""),
                actionLabel = "Apri",
                onAction = { done?.let { openFile(context, it.uri, "image/gif") } },
                onDismiss = { viewModel.dismissResult() },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }
        RevealVertically(visible = progress != null) {
            OmniProgressBanner(
                progress = progress ?: 0f,
                statusText = if ((progress ?: 0f) < 0.6f) "Preparo i fotogrammi…" else "Scrivo la GIF…",
                onCancel = { viewModel.cancel() },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }

        ToolLayout(
            title = "GIF animata",
            description = "Trasforma una serie di foto in una GIF",
            isEmpty = frames.isEmpty(),
            phoneTabTitles = listOf("Anteprima · ${frames.size}", "Opzioni"),
            emptyDropzone = {
                Dropzone(
                    icon = Icons.Default.Gif,
                    headline = "Scegli le foto da animare",
                    helpText = "Diventano i fotogrammi della GIF, nell'ordine in cui le scegli. Puoi cambiare velocità, dimensione e fare l'effetto avanti e indietro",
                    supportedFormats = "JPG · PNG · WEBP · HEIC · fino a ${GifViewModel.MAX_FRAMES} foto",
                    actionLabel = "Scegli foto",
                    tintColor = TintImage,
                    onAction = { pickerLauncher.launch("image/*") },
                    testTag = "gif_add_button",
                    secondaryIcon = Icons.Default.CameraAlt,
                    onSecondaryAction = launchCamera
                )
            },
            fileList = {
                Column(modifier = Modifier.fillMaxSize()) {
                    FileListHeader(
                        title = if (frames.size == 1) "1 fotogramma" else "${frames.size} fotogrammi",
                        onAdd = { pickerLauncher.launch("image/*") },
                        onClear = { viewModel.clearAll() },
                        addIcon = Icons.Default.AddPhotoAlternate
                    ) {
                        ReverseButton(enabled = frames.size > 1) {
                            haptics(Haptic.TICK)
                            step = 0
                            viewModel.reverse()
                        }
                    }
                    GifPlayer(
                        frames = frames,
                        order = order,
                        step = step,
                        playing = playing,
                        settings = settings,
                        onTogglePlay = {
                            haptics(Haptic.TAP)
                            if (!playing) {
                                selectedId = null
                                if (!settings.loopForever && step == order.lastIndex) step = 0
                            }
                            playing = !playing
                        },
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    FrameStrip(
                        frames = frames,
                        currentId = order.getOrNull(step)?.let { frames.getOrNull(it)?.id },
                        selectedId = selectedId,
                        onTap = { item ->
                            haptics(Haptic.TICK)
                            playing = false
                            selectedId = if (selectedId == item.id) null else item.id
                            val target = frames.indexOfFirst { it.id == item.id }
                            order.indexOf(target).takeIf { it >= 0 }?.let { step = it }
                        }
                    )
                    RevealVertically(visible = selectedId != null) {
                        val id = selectedId
                        val index = frames.indexOfFirst { it.id == id }
                        val canBack = index > 0
                        val canForward = index in 0 until frames.lastIndex
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "Fotogramma ${index + 1}",
                                fontSize = 13.sp,
                                color = OmniTextMuted,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { id?.let { viewModel.nudge(it, -1) } }, enabled = canBack, modifier = Modifier.size(40.dp)) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Sposta prima", tint = if (canBack) OmniText else OmniTextMuted)
                            }
                            IconButton(onClick = { id?.let { viewModel.nudge(it, 1) } }, enabled = canForward, modifier = Modifier.size(40.dp)) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Sposta dopo", tint = if (canForward) OmniText else OmniTextMuted)
                            }
                            IconButton(onClick = { id?.let { viewModel.remove(it) } }, modifier = Modifier.size(40.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Togli fotogramma", tint = OmniDanger)
                            }
                        }
                    }
                }
            },
            optionsPanel = {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OmniSlider(
                        label = "Durata di ogni foto",
                        value = settings.frameMs.toFloat(),
                        onValueChange = { v -> viewModel.updateSettings { it.copy(frameMs = (v / 50f).toInt() * 50) } },
                        valueRange = 100f..2000f,
                        valueFormatter = { seconds(it.toInt() / 50 * 50) }
                    )
                    OptionSection(title = "Dimensione (lato lungo)", hint = "Più piccola = file più leggero da mandare in chat") {
                        SegmentedControl(
                            options = GifSettings.SIZES.map { "$it px" },
                            selectedIndex = GifSettings.SIZES.indexOf(settings.maxSide).coerceAtLeast(0),
                            onOptionSelected = { idx -> viewModel.updateSettings { it.copy(maxSide = GifSettings.SIZES[idx]) } }
                        )
                    }
                    OptionSection(title = "Inquadratura", hint = if (settings.fit == GifFit.FILL) "Ritaglia i bordi per riempire il riquadro" else "Mostra tutta la foto con bordi bianchi") {
                        SegmentedControl(
                            options = GifFit.entries.map { it.label },
                            selectedIndex = settings.fit.ordinal,
                            onOptionSelected = { idx -> viewModel.updateSettings { it.copy(fit = GifFit.entries[idx]) } }
                        )
                    }
                    OptionRow(
                        label = "Avanti e indietro",
                        description = "Dopo l'ultima foto torna indietro fino alla prima, come un boomerang",
                        checked = settings.boomerang,
                        onCheckedChange = { on -> viewModel.updateSettings { it.copy(boomerang = on) } }
                    )
                    OptionRow(
                        label = "Ripeti all'infinito",
                        description = if (settings.loopForever) "La GIF ricomincia da capo" else "La GIF si ferma sull'ultima foto",
                        checked = settings.loopForever,
                        onCheckedChange = { on ->
                            viewModel.updateSettings { it.copy(loopForever = on) }
                            if (on) playing = true
                        }
                    )
                }
            },
            actionBar = {
                OmniActionBar(
                    summaryText = "${frames.size} foto · ${seconds((totalSeconds * 1000).toInt())}",
                    detailText = listOfNotNull("$outW × $outH px", "avanti e indietro".takeIf { settings.boomerang }, "una volta".takeIf { !settings.loopForever }).joinToString(" · "),
                    primaryButtonLabel = if (progress != null) "Creo…" else "Crea GIF",
                    isEnabled = frames.size >= 2 && progress == null,
                    disabledHelperText = if (frames.size < 2) "Aggiungi almeno 2 foto" else null,
                    onPrimaryAction = { viewModel.save() },
                    testTag = "gif_save_button"
                )
            }
        )
    }
}

/** Plays the frames at the chosen speed, with a soft crossfade and a loop progress bar. */
@Composable
private fun GifPlayer(
    frames: List<GifFrameItem>,
    order: List<Int>,
    step: Int,
    playing: Boolean,
    settings: GifSettings,
    onTogglePlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val current = order.getOrNull(step)?.let { frames.getOrNull(it) }
    val aspect = (frames.firstOrNull()?.aspect ?: 1f).coerceIn(0.25f, 4f)
    val loopProgress by animateFloatAsState(
        if (order.isEmpty()) 0f else (step + 1f) / order.size,
        motionSpec(tween(if (step == 0) 0 else Motion.SHORT)),
        label = "gif_loop"
    )
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        // Leave room for the progress bar and the caption under the picture.
        val roomH = if (maxHeight == Dp.Infinity) maxWidth else (maxHeight - 34.dp).coerceAtLeast(80.dp)
        val boxW = minOf(maxWidth, roomH * aspect)
        val boxH = boxW / aspect
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(boxW, boxH)
                    .clip(RoundedCornerShape(RadiusSm))
                    // The GIF itself has a white background (letterbox colour), whatever the app theme.
                    .background(Color.White)
                    .border(1.dp, OmniBorder, RoundedCornerShape(RadiusSm))
                    .clickable(onClickLabel = if (playing) "Metti in pausa" else "Riproduci", onClick = onTogglePlay)
            ) {
                Crossfade(
                    targetState = current,
                    animationSpec = motionSpec(tween((settings.frameMs / 3).coerceAtMost(220))),
                    label = "gif_frame"
                ) { item ->
                    if (item != null) {
                        AsyncImage(
                            model = item.uri,
                            contentDescription = item.name,
                            contentScale = if (settings.fit == GifFit.FILL) ContentScale.Crop else ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                PlayBadge(playing = playing, modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { loopProgress },
                modifier = Modifier.size(width = boxW, height = 4.dp).clip(CircleShape),
                color = OmniAccent,
                trackColor = OmniAccentSoft
            )
            AnimatedText(
                text = if (order.isEmpty()) "" else "Fotogramma ${(order.getOrNull(step) ?: 0) + 1} di ${frames.size}" + if (playing) "" else " · in pausa",
                fontSize = 12.sp,
                color = OmniTextMuted,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun PlayBadge(playing: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(36.dp).clip(CircleShape).background(OmniAccent),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = playing,
            transitionSpec = { (scaleIn(initialScale = 0.6f) + fadeIn()).togetherWith(scaleOut(targetScale = 0.6f) + fadeOut()) },
            label = "gif_play_icon"
        ) { isPlaying ->
            Icon(
                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = OmniOnAccent,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/** Thumbnails of every frame; the one on screen pops up while the GIF plays. */
@Composable
private fun FrameStrip(
    frames: List<GifFrameItem>,
    currentId: String?,
    selectedId: String?,
    onTap: (GifFrameItem) -> Unit
) {
    val listState = rememberLazyListState()
    val currentIndex = frames.indexOfFirst { it.id == currentId }
    LaunchedEffect(currentIndex) {
        if (currentIndex >= 0 && listState.layoutInfo.visibleItemsInfo.none { it.index == currentIndex }) {
            listState.animateScrollToItem(currentIndex)
        }
    }
    LazyRow(
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 6.dp)
    ) {
        itemsIndexed(frames, key = { _, item -> item.id }) { _, item ->
            val active = item.id == currentId
            val selected = item.id == selectedId
            val scale by animateFloatAsState(if (active) 1.08f else 0.94f, motionSpec(Motion.bouncy()), label = "strip_scale")
            val ring by animateColorAsState(
                when {
                    selected -> OmniText
                    active -> OmniAccent
                    else -> Color.Transparent
                },
                label = "strip_ring"
            )
            Box(
                modifier = Modifier
                    .animateItem()
                    .size(60.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(RoundedCornerShape(10.dp))
                    .background(OmniSurface2)
                    .border(2.dp, ring, RoundedCornerShape(10.dp))
                    .clickable(onClickLabel = "Scegli ${item.name}") { onTap(item) }
            ) {
                AsyncImage(model = item.uri, contentDescription = item.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

/** Reverse-order button whose arrows flip half a turn on every press. */
@Composable
private fun ReverseButton(enabled: Boolean, onClick: () -> Unit) {
    var turns by remember { mutableFloatStateOf(0f) }
    val rotation by animateFloatAsState(turns * 180f, motionSpec(Motion.bouncy()), label = "reverse_flip")
    IconButton(
        onClick = {
            turns += 1f
            onClick()
        },
        enabled = enabled,
        modifier = Modifier.size(40.dp)
    ) {
        Icon(
            Icons.Default.SwapHoriz,
            contentDescription = "Inverti l'ordine",
            tint = if (enabled) OmniAccent else OmniTextMuted,
            modifier = Modifier.size(20.dp).graphicsLayer { rotationY = rotation }
        )
    }
}

/** "0,6 s", "1,25 s", "2 s". */
private fun seconds(ms: Int): String =
    String.format(Locale.ITALY, "%.2f", ms / 1000f).trimEnd('0').trimEnd(',') + " s"
