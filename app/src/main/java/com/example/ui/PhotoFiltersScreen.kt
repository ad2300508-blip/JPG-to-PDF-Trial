package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.components.Dropzone
import com.example.ui.components.FileListHeader
import com.example.ui.components.OmniActionBar
import com.example.ui.components.OmniProgressBanner
import com.example.ui.components.OmniSlider
import com.example.ui.components.OmniToast
import com.example.ui.components.OptionSection
import com.example.ui.components.RevealVertically
import com.example.ui.components.ToolLayout
import com.example.ui.components.openFile
import com.example.ui.components.rememberCameraCapture
import com.example.ui.theme.Haptic
import com.example.ui.theme.LocalReduceMotion
import com.example.ui.theme.Motion
import com.example.ui.theme.OmniAccent
import com.example.ui.theme.OmniBorder
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
import com.example.util.PhotoFilter
import com.example.util.PhotoFilterMath
import com.example.viewmodel.FilterPhoto
import com.example.viewmodel.PhotoFiltersViewModel
import kotlinx.coroutines.launch

@Composable
fun PhotoFiltersScreen(viewModel: PhotoFiltersViewModel, modifier: Modifier = Modifier) {
    val photos by viewModel.photos.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val intensity by viewModel.intensity.collectAsState()
    val previewId by viewModel.previewId.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val savedCount by viewModel.savedCount.collectAsState()
    val lastSaved by viewModel.lastSaved.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val context = LocalContext.current
    val haptics = rememberHaptics()

    val pickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        viewModel.addImages(uris)
    }
    val launchCamera = rememberCameraCapture { uri -> viewModel.addImages(listOf(uri)) }
    val preview = photos.firstOrNull { it.id == previewId } ?: photos.firstOrNull()

    Column(modifier = modifier.fillMaxSize().testTag("filters_screen")) {
        RevealVertically(visible = errorMessage != null) {
            OmniToast(
                message = errorMessage.orEmpty(),
                isError = true,
                actionLabel = "Chiudi",
                onAction = { viewModel.dismissError() },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }
        RevealVertically(visible = savedCount != null && progress == null) {
            val count = savedCount ?: 0
            OmniToast(
                message = (if (count == 1) "1 foto salvata" else "$count foto salvate") + " in ${OutputStorage.displayPath(OutputFolder.IMAGES)}",
                actionLabel = if (count == 1) "Apri" else "Chiudi",
                onAction = {
                    val uri = lastSaved
                    if (count == 1 && uri != null) openFile(context, uri, "image/jpeg") else viewModel.dismissSaved()
                },
                onDismiss = { viewModel.dismissSaved() },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }
        RevealVertically(visible = progress != null) {
            OmniProgressBanner(
                progress = progress ?: 0f,
                statusText = "Applico il filtro ${filter.label}…",
                onCancel = { viewModel.cancel() },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }

        ToolLayout(
            title = "Filtri foto",
            description = "Dai un nuovo look alle tue foto",
            isEmpty = photos.isEmpty(),
            phoneTabTitles = listOf("Anteprima · ${photos.size}", "Filtri"),
            emptyDropzone = {
                Dropzone(
                    icon = Icons.Default.AutoFixHigh,
                    headline = "Scegli le foto da ritoccare",
                    helpText = "Vivace, Caldo, Freddo, Pastello, Seppia, B/N e Noir, con intensità regolabile e confronto prima/dopo",
                    supportedFormats = "JPG · PNG · WEBP · HEIC",
                    actionLabel = "Scegli foto",
                    tintColor = TintImage,
                    onAction = { pickerLauncher.launch("image/*") },
                    testTag = "filters_add_button",
                    secondaryIcon = Icons.Default.CameraAlt,
                    onSecondaryAction = launchCamera
                )
            },
            fileList = {
                Column(modifier = Modifier.fillMaxSize()) {
                    FileListHeader(
                        title = if (photos.size == 1) "1 foto" else "${photos.size} foto",
                        onAdd = { pickerLauncher.launch("image/*") },
                        onClear = { viewModel.clearAll() },
                        addIcon = Icons.Default.AddPhotoAlternate
                    )
                    if (preview != null) {
                        BeforeAfterPreview(
                            photo = preview,
                            filter = filter,
                            intensity = intensity,
                            modifier = Modifier.fillMaxWidth().weight(1f)
                        )
                    }
                    if (photos.size > 1) {
                        Spacer(modifier = Modifier.height(10.dp))
                        PhotoStrip(
                            photos = photos,
                            previewId = preview?.id,
                            onShow = { viewModel.showPreview(it) },
                            onRemove = { viewModel.remove(it) }
                        )
                    }
                }
            },
            optionsPanel = {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OptionSection(title = "Filtro") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            PhotoFilter.entries.chunked(4).forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    row.forEach { option ->
                                        FilterChipTile(
                                            photo = preview,
                                            filter = option,
                                            selected = option == filter,
                                            onClick = {
                                                haptics(Haptic.TICK)
                                                viewModel.selectFilter(option)
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    repeat(4 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                                }
                            }
                        }
                    }
                    RevealVertically(visible = filter != PhotoFilter.ORIGINAL) {
                        OmniSlider(
                            label = "Intensità",
                            value = intensity * 100f,
                            onValueChange = { viewModel.setIntensity(it / 100f) },
                            valueRange = 0f..100f,
                            valueFormatter = { "${it.toInt()}%" }
                        )
                    }
                    Text(
                        "Trascina la linea sull'anteprima per confrontare prima e dopo.",
                        fontSize = 13.sp,
                        color = OmniTextMuted
                    )
                }
            },
            actionBar = {
                OmniActionBar(
                    summaryText = "${photos.size} foto · ${filter.label}",
                    detailText = if (filter == PhotoFilter.ORIGINAL) "Scegli un filtro" else "Intensità ${(intensity * 100).toInt()}% · JPG",
                    primaryButtonLabel = if (progress != null) "Salvo…" else if (photos.size == 1) "Salva foto" else "Applica a ${photos.size} foto",
                    isEnabled = photos.isNotEmpty() && progress == null && filter != PhotoFilter.ORIGINAL,
                    disabledHelperText = if (filter == PhotoFilter.ORIGINAL && photos.isNotEmpty()) "Scegli un filtro diverso da Originale" else null,
                    onPrimaryAction = { viewModel.applyAll() },
                    testTag = "filters_apply_button"
                )
            }
        )
    }
}

/**
 * Original on the left, filtered on the right of a draggable divider. Switching filter morphs the
 * colours from the old look to the new one instead of jumping.
 */
@Composable
private fun BeforeAfterPreview(photo: FilterPhoto, filter: PhotoFilter, intensity: Float, modifier: Modifier = Modifier) {
    val reduceMotion = LocalReduceMotion.current
    val scope = rememberCoroutineScope()
    val target = PhotoFilterMath.lerp(PhotoFilterMath.IDENTITY, filter.matrix, intensity)

    // Morph between the matrix on screen and the new filter.
    val morph = remember { Animatable(1f) }
    var from by remember { mutableStateOf(target) }
    val shown = PhotoFilterMath.lerp(from, target, morph.value)
    val latestShown = remember { arrayOf(shown) }
    latestShown[0] = shown
    LaunchedEffect(filter) {
        from = latestShown[0]
        morph.snapTo(0f)
        if (reduceMotion) morph.snapTo(1f) else morph.animateTo(1f, tween(Motion.LONG, easing = Motion.Standard))
    }

    // Divider position (0..1); a little sweep on each new photo hints that it can be dragged.
    val split = remember { Animatable(0.5f) }
    LaunchedEffect(photo.id) {
        if (!reduceMotion) {
            split.animateTo(0.68f, tween(Motion.MEDIUM, easing = Motion.Standard))
            split.animateTo(0.5f, Motion.gentle())
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(RadiusSm))
            .background(OmniSurface2)
            .border(1.dp, OmniBorder, RoundedCornerShape(RadiusSm))
            .pointerInput(Unit) {
                detectTapGestures { offset -> scope.launch { split.animateTo((offset.x / size.width).coerceIn(0f, 1f), Motion.gentle()) } }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    scope.launch { split.snapTo((change.position.x / size.width).coerceIn(0f, 1f)) }
                }
            }
    ) {
        val width = maxWidth
        AsyncImage(
            model = photo.uri,
            contentDescription = "${photo.name}, originale",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
        AsyncImage(
            model = photo.uri,
            contentDescription = "${photo.name}, con filtro",
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.colorMatrix(ColorMatrix(shown)),
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    clipRect(left = size.width * split.value) { this@drawWithContent.drawContent() }
                }
        )
        // Divider line and handle.
        Box(
            modifier = Modifier
                .offset(x = width * split.value - 1.dp)
                .width(2.dp)
                .fillMaxHeight()
                .background(Color.White)
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = width * split.value - 18.dp)
                .size(36.dp)
                .clip(CircleShape)
                .background(OmniAccent),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = OmniOnAccent, modifier = Modifier.size(20.dp))
        }
        SideLabel("Prima", visible = split.value > 0.18f, modifier = Modifier.align(Alignment.TopStart).padding(10.dp))
        SideLabel(filter.label, visible = split.value < 0.82f, modifier = Modifier.align(Alignment.TopEnd).padding(10.dp))
    }
}

@Composable
private fun SideLabel(text: String, visible: Boolean, modifier: Modifier = Modifier) {
    androidx.compose.animation.AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut(), modifier = modifier) {
        Text(
            text,
            fontSize = 12.sp,
            color = Color.White,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                // Photo overlay: a dark scrim reads on any picture, whatever the app theme.
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

/** A thumbnail of the current photo with the filter applied; the chosen one pops and gets a tick. */
@Composable
private fun FilterChipTile(photo: FilterPhoto?, filter: PhotoFilter, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scale by animateFloatAsState(if (selected) 1f else 0.92f, motionSpec(Motion.bouncy()), label = "filter_scale")
    val ring by animateColorAsState(if (selected) OmniAccent else Color.Transparent, label = "filter_ring")
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(RoundedCornerShape(12.dp))
                .background(OmniSurface2)
                .border(3.dp, ring, RoundedCornerShape(12.dp))
                .clickable(onClickLabel = "Filtro ${filter.label}", onClick = onClick)
        ) {
            if (photo != null) {
                AsyncImage(
                    model = photo.uri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    colorFilter = if (filter == PhotoFilter.ORIGINAL) null else ColorFilter.colorMatrix(ColorMatrix(filter.matrix)),
                    modifier = Modifier.fillMaxSize()
                )
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = selected,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut(),
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
            ) {
                Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(OmniAccent), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = OmniOnAccent, modifier = Modifier.size(14.dp))
                }
            }
        }
        Text(
            filter.label,
            fontSize = 12.sp,
            color = if (selected) OmniText else OmniTextMuted,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun PhotoStrip(photos: List<FilterPhoto>, previewId: String?, onShow: (String) -> Unit, onRemove: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
        items(photos, key = { it.id }) { photo ->
            val active = photo.id == previewId
            val scale by animateFloatAsState(if (active) 1.06f else 0.94f, motionSpec(Motion.bouncy()), label = "photo_scale")
            val ring by animateColorAsState(if (active) OmniAccent else Color.Transparent, label = "photo_ring")
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
                    .clickable(onClickLabel = "Mostra ${photo.name}") { onShow(photo.id) }
            ) {
                AsyncImage(model = photo.uri, contentDescription = photo.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                androidx.compose.animation.AnimatedVisibility(
                    visible = active,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut(),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    IconButton(onClick = { onRemove(photo.id) }, modifier = Modifier.size(28.dp)) {
                        Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(OmniText), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Close, contentDescription = "Togli ${photo.name}", tint = OmniSurface2, modifier = Modifier.size(12.dp))
                        }
                    }
                }
            }
        }
    }
}
