package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesomeMosaic
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.example.service.CollageBackground
import com.example.service.CollageFormat
import com.example.service.CollageSettings
import com.example.ui.components.AnimatedText
import com.example.ui.components.Dropzone
import com.example.ui.components.FileListHeader
import com.example.ui.components.OmniActionBar
import com.example.ui.components.OmniProgressBanner
import com.example.ui.components.OmniSlider
import com.example.ui.components.OmniToast
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
import com.example.ui.theme.OmniBorder
import com.example.ui.theme.OmniOnAccent
import com.example.ui.theme.OmniSurface2
import com.example.ui.theme.OmniText
import com.example.ui.theme.OmniTextMuted
import com.example.ui.theme.RadiusSm
import com.example.ui.theme.TintImage
import com.example.ui.theme.enterAnimation
import com.example.ui.theme.motionSpec
import com.example.ui.theme.rememberHaptics
import com.example.util.CollageLayout
import com.example.util.CollagePlanner
import com.example.util.OutputFolder
import com.example.util.OutputStorage
import com.example.viewmodel.CollageItem
import com.example.viewmodel.CollageViewModel
import kotlin.math.roundToInt

@Composable
fun CollageScreen(viewModel: CollageViewModel, modifier: Modifier = Modifier) {
    val items by viewModel.items.collectAsState()
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
    var selectedId by remember { mutableStateOf<String?>(null) }
    if (selectedId != null && items.none { it.id == selectedId }) selectedId = null

    val plan = CollagePlanner.plan(settings.layout, items.map { it.aspect }, settings.gap)
    val scale = CollagePlanner.scaleFor(plan, OUTPUT_SIDE)
    val outW = (plan.width * scale).roundToInt()
    val outH = (plan.height * scale).roundToInt()

    Column(modifier = modifier.fillMaxSize().testTag("collage_screen")) {
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
                message = "Collage salvato in ${OutputStorage.displayPath(OutputFolder.IMAGES)}" + (done?.let { " · ${formatBytes(it.sizeBytes)}" } ?: ""),
                actionLabel = "Apri",
                onAction = { done?.let { openFile(context, it.uri, it.mimeType) } },
                onDismiss = { viewModel.dismissResult() },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }
        RevealVertically(visible = progress != null) {
            OmniProgressBanner(
                progress = progress ?: 0f,
                statusText = "Compongo il collage…",
                onCancel = { viewModel.cancel() },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }

        ToolLayout(
            title = "Collage foto",
            description = "Unisci più foto in un'unica immagine",
            isEmpty = items.isEmpty(),
            phoneTabTitles = listOf("Anteprima · ${items.size}", "Opzioni"),
            emptyDropzone = {
                Dropzone(
                    icon = Icons.Default.AutoAwesomeMosaic,
                    headline = "Scegli le foto da unire",
                    helpText = "In colonna, in riga o a griglia, con spazi, angoli arrotondati e sfondo a scelta",
                    supportedFormats = "JPG · PNG · WEBP · HEIC · fino a ${CollageViewModel.MAX_PHOTOS} foto",
                    actionLabel = "Scegli foto",
                    tintColor = TintImage,
                    onAction = { pickerLauncher.launch("image/*") },
                    testTag = "collage_add_button",
                    secondaryIcon = Icons.Default.CameraAlt,
                    onSecondaryAction = launchCamera
                )
            },
            fileList = {
                Column(modifier = Modifier.fillMaxSize()) {
                    FileListHeader(
                        title = if (items.size == 1) "1 foto" else "${items.size} foto",
                        onAdd = { pickerLauncher.launch("image/*") },
                        onClear = { viewModel.clearAll() },
                        addIcon = Icons.Default.AddPhotoAlternate
                    ) {
                        ShuffleButton(enabled = items.size > 1) {
                            haptics(Haptic.TICK)
                            selectedId = null
                            viewModel.shuffle()
                        }
                    }
                    AnimatedText(
                        text = when {
                            items.size < 2 -> "Aggiungi un'altra foto per comporre il collage"
                            selectedId == null -> "Tocca due foto per scambiarle di posto"
                            else -> "Ora tocca la foto con cui scambiarla"
                        },
                        fontSize = 13.sp,
                        color = OmniTextMuted,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    CollagePreview(
                        items = items,
                        settings = settings,
                        selectedId = selectedId,
                        onTap = { tapped ->
                            val current = selectedId
                            when {
                                current == null -> {
                                    haptics(Haptic.TICK)
                                    selectedId = tapped
                                }
                                current == tapped -> selectedId = null
                                else -> {
                                    haptics(Haptic.CONFIRM)
                                    viewModel.swap(current, tapped)
                                    selectedId = null
                                }
                            }
                        },
                        onRemove = { viewModel.remove(it) },
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    )
                }
            },
            optionsPanel = {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OptionSection(title = "Disposizione") {
                        SegmentedControl(
                            options = CollageLayout.entries.map { it.label },
                            selectedIndex = settings.layout.ordinal,
                            onOptionSelected = { idx -> viewModel.updateSettings { it.copy(layout = CollageLayout.entries[idx]) } }
                        )
                    }
                    OmniSlider(
                        label = "Spazio tra le foto",
                        value = settings.gap * 100f,
                        onValueChange = { v -> viewModel.updateSettings { it.copy(gap = v / 100f) } },
                        valueRange = 0f..10f,
                        valueFormatter = { if (it < 0.5f) "Nessuno" else "${it.roundToInt()}%" }
                    )
                    OmniSlider(
                        label = "Angoli arrotondati",
                        value = settings.corner * 100f,
                        onValueChange = { v -> viewModel.updateSettings { it.copy(corner = v / 100f) } },
                        valueRange = 0f..15f,
                        valueFormatter = { if (it < 0.5f) "Squadrati" else "${it.roundToInt()}%" }
                    )
                    OptionSection(title = "Sfondo", hint = "Si vede negli spazi tra le foto e dietro gli angoli") {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            CollageBackground.entries.forEach { bg ->
                                BackgroundSwatch(bg, selected = settings.background == bg) {
                                    viewModel.updateSettings { it.copy(background = bg) }
                                }
                            }
                        }
                    }
                    OptionSection(title = "Formato", hint = if (settings.format == CollageFormat.PNG) "Senza perdita, file più pesante" else "Leggero, ideale da condividere") {
                        SegmentedControl(
                            options = CollageFormat.entries.map { it.label },
                            selectedIndex = settings.format.ordinal,
                            onOptionSelected = { idx -> viewModel.updateSettings { it.copy(format = CollageFormat.entries[idx]) } }
                        )
                    }
                }
            },
            actionBar = {
                OmniActionBar(
                    summaryText = "${items.size} foto · ${settings.layout.label}",
                    detailText = "$outW × $outH px · ${settings.format.label} · ${settings.background.label}",
                    primaryButtonLabel = if (progress != null) "Creo…" else "Crea collage",
                    isEnabled = items.size >= 2 && progress == null,
                    disabledHelperText = if (items.size < 2) "Aggiungi almeno 2 foto" else null,
                    onPrimaryAction = { viewModel.save() },
                    testTag = "collage_save_button"
                )
            }
        )
    }
}

/** Live preview: every photo glides to its new place when the layout, spacing or order changes. */
@Composable
private fun CollagePreview(
    items: List<CollageItem>,
    settings: CollageSettings,
    selectedId: String?,
    onTap: (String) -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val plan = CollagePlanner.plan(settings.layout, items.map { it.aspect }, settings.gap)
    val background by animateColorAsState(Color(settings.background.argb), motionSpec(Motion.gentle()), label = "collage_bg")
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val fit = minOf(maxWidth / plan.width, maxHeight / plan.height)
        val boxW by animateDpAsState(fit * plan.width, motionSpec(Motion.gentle()), label = "collage_w")
        val boxH by animateDpAsState(fit * plan.height, motionSpec(Motion.gentle()), label = "collage_h")
        val corner = fit * (settings.corner * CollagePlanner.UNIT)
        Box(
            modifier = Modifier
                .size(boxW.coerceAtLeast(0.dp), boxH.coerceAtLeast(0.dp))
                .clip(RoundedCornerShape(6.dp))
                .background(background)
                .border(1.dp, OmniBorder, RoundedCornerShape(6.dp))
        ) {
            items.forEachIndexed { index, item ->
                val cell = plan.cells.getOrNull(index) ?: return@forEachIndexed
                key(item.id) {
                    CollageTile(
                        item = item,
                        x = fit * cell.x,
                        y = fit * cell.y,
                        width = fit * cell.width,
                        height = fit * cell.height,
                        corner = corner,
                        index = index,
                        selected = item.id == selectedId,
                        dimmed = selectedId != null && item.id != selectedId,
                        onTap = { onTap(item.id) },
                        onRemove = { onRemove(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CollageTile(
    item: CollageItem,
    x: Dp,
    y: Dp,
    width: Dp,
    height: Dp,
    corner: Dp,
    index: Int,
    selected: Boolean,
    dimmed: Boolean,
    onTap: () -> Unit,
    onRemove: () -> Unit
) {
    val spec = motionSpec<Dp>(Motion.gentle())
    val ax by animateDpAsState(x, spec, label = "tile_x")
    val ay by animateDpAsState(y, spec, label = "tile_y")
    val aw by animateDpAsState(width, spec, label = "tile_w")
    val ah by animateDpAsState(height, spec, label = "tile_h")
    val ac by animateDpAsState(corner, spec, label = "tile_corner")
    val lift by animateFloatAsState(if (selected) 0.92f else 1f, motionSpec(Motion.bouncy()), label = "tile_lift")
    val fade by animateFloatAsState(if (dimmed) 0.75f else 1f, motionSpec(Motion.gentle()), label = "tile_dim")
    val ring by animateColorAsState(if (selected) OmniAccent else Color.Transparent, label = "tile_ring")
    val shape = RoundedCornerShape(ac.coerceAtLeast(0.dp))
    Box(
        modifier = Modifier
            .offset(ax, ay)
            .size(aw.coerceAtLeast(0.dp), ah.coerceAtLeast(0.dp))
            .zIndex(if (selected) 1f else 0f)
            .enterAnimation(index, key = item.id)
            .graphicsLayer {
                scaleX = lift
                scaleY = lift
                alpha = fade
            }
            .clip(shape)
            .background(OmniSurface2)
            .border(3.dp, ring, shape)
            .clickable(onClickLabel = if (selected) "Annulla selezione" else "Seleziona ${item.name}", onClick = onTap)
    ) {
        AsyncImage(
            model = item.uri,
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        AnimatedVisibility(
            visible = selected,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(OmniAccent),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = OmniOnAccent, modifier = Modifier.size(20.dp))
            }
        }
        AnimatedVisibility(
            visible = selected,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            IconButton(onClick = onRemove, modifier = Modifier.size(40.dp)) {
                Box(
                    modifier = Modifier.size(26.dp).clip(CircleShape).background(OmniText),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Togli dal collage", tint = OmniSurface2, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

/** Shuffle button whose icon spins a full turn on every press. */
@Composable
private fun ShuffleButton(enabled: Boolean, onClick: () -> Unit) {
    var turns by remember { mutableFloatStateOf(0f) }
    val rotation by animateFloatAsState(turns * 360f, motionSpec(Motion.bouncy()), label = "shuffle_spin")
    IconButton(
        onClick = {
            turns += 1f
            onClick()
        },
        enabled = enabled,
        modifier = Modifier.size(40.dp)
    ) {
        Icon(
            Icons.Default.Shuffle,
            contentDescription = "Mischia",
            tint = if (enabled) OmniAccent else OmniTextMuted,
            modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = rotation }
        )
    }
}

@Composable
private fun BackgroundSwatch(choice: CollageBackground, selected: Boolean, onClick: () -> Unit) {
    val ring by animateColorAsState(if (selected) OmniAccent else OmniBorder, label = "bg_ring")
    val scale by animateFloatAsState(if (selected) 1f else 0.86f, motionSpec(Motion.bouncy()), label = "bg_scale")
    val fill = Color(choice.argb)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .border(2.dp, ring, RoundedCornerShape(RadiusSm))
                .padding(4.dp)
                .clip(RoundedCornerShape(RadiusSm - 4.dp))
                .background(fill)
                .clickable(onClickLabel = "Sfondo ${choice.label}", onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.animation.AnimatedVisibility(visible = selected, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
                // Contrast against the swatch itself, not the app theme.
                Icon(Icons.Default.Check, contentDescription = null, tint = if (fill.luminance() > 0.5f) Color.Black else Color.White, modifier = Modifier.size(18.dp))
            }
        }
        Text(choice.label, fontSize = 11.sp, color = if (selected) OmniText else OmniTextMuted, modifier = Modifier.padding(top = 4.dp))
    }
}

private const val OUTPUT_SIDE = 3072
