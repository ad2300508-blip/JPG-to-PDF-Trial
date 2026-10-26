package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.service.RotateSettings
import com.example.service.SocialBackground
import com.example.service.SocialSettings
import com.example.service.WatermarkSettings
import com.example.ui.components.AnimatedText
import com.example.ui.components.ContinueAction
import com.example.ui.components.ConversionResultView
import com.example.ui.components.Dropzone
import com.example.ui.components.FileListHeader
import com.example.ui.components.MorphTextField
import com.example.ui.components.OmniActionBar
import com.example.ui.components.OmniProgressBanner
import com.example.ui.components.OmniSlider
import com.example.ui.components.OmniToast
import com.example.ui.components.OptionRow
import com.example.ui.components.OptionSection
import com.example.ui.components.RevealVertically
import com.example.ui.components.SegmentedControl
import com.example.ui.components.ToolHeader
import com.example.ui.components.ToolLayout
import com.example.ui.components.openFile
import com.example.ui.components.rememberCameraCapture
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
import com.example.ui.theme.RadiusMd
import com.example.ui.theme.RadiusSm
import com.example.ui.theme.TintAudio
import com.example.ui.theme.TintImage
import com.example.ui.theme.enterAnimation
import com.example.ui.theme.motionSpec
import com.example.ui.theme.pressScale
import com.example.ui.theme.rememberHaptics
import com.example.util.OutputFolder
import com.example.util.OutputStorage
import com.example.util.PhotoMath
import com.example.util.SocialAspect
import com.example.util.WatermarkPosition
import com.example.viewmodel.BatchPhotoViewModel
import com.example.viewmodel.FilterPhoto
import com.example.viewmodel.PaletteViewModel
import com.example.viewmodel.PhotoWatermarkViewModel
import com.example.viewmodel.RecorderState
import com.example.viewmodel.RecorderViewModel
import com.example.viewmodel.RotatePhotosViewModel
import com.example.viewmodel.SocialFormatViewModel
import kotlin.math.roundToInt

/**
 * Frame of the batch photo tools: banners, picker, a big preview of one photo with a strip to
 * switch between them, the options panel and the "apply to N photos" bar.
 */
@Composable
private fun <S> BatchPhotoFrame(
    viewModel: BatchPhotoViewModel<S>,
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    dropHeadline: String,
    dropHelp: String,
    actionDetail: (S) -> String,
    preview: @Composable (FilterPhoto, S) -> Unit,
    options: @Composable (S) -> Unit,
    modifier: Modifier = Modifier
) {
    val photos by viewModel.photos.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val previewId by viewModel.previewId.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val savedCount by viewModel.savedCount.collectAsState()
    val lastSaved by viewModel.lastSaved.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris -> viewModel.addImages(uris) }
    val launchCamera = rememberCameraCapture { uri -> viewModel.addImages(listOf(uri)) }
    val current = photos.firstOrNull { it.id == previewId } ?: photos.firstOrNull()
    val blocking = viewModel.blockingReason(settings)

    Column(modifier = modifier.fillMaxSize()) {
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
                    if (count == 1 && uri != null) openFile(context, uri, null) else viewModel.dismissSaved()
                },
                onDismiss = { viewModel.dismissSaved() },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }
        RevealVertically(visible = progress != null) {
            OmniProgressBanner(
                progress = progress ?: 0f,
                statusText = "Elaboro le foto…",
                onCancel = { viewModel.cancel() },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }
        ToolLayout(
            title = title,
            description = description,
            isEmpty = photos.isEmpty(),
            phoneTabTitles = listOf("Anteprima · ${photos.size}", "Opzioni"),
            emptyDropzone = {
                Dropzone(
                    icon = icon,
                    headline = dropHeadline,
                    helpText = dropHelp,
                    supportedFormats = "JPG · PNG · WEBP · HEIC",
                    actionLabel = "Scegli foto",
                    tintColor = TintImage,
                    onAction = { picker.launch("image/*") },
                    secondaryIcon = Icons.Default.CameraAlt,
                    onSecondaryAction = launchCamera
                )
            },
            fileList = {
                Column(modifier = Modifier.fillMaxSize()) {
                    FileListHeader(
                        title = if (photos.size == 1) "1 foto" else "${photos.size} foto",
                        onAdd = { picker.launch("image/*") },
                        onClear = { viewModel.clearAll() },
                        addIcon = Icons.Default.AddPhotoAlternate
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (current != null) preview(current, settings)
                    }
                    if (photos.size > 1) {
                        Spacer(modifier = Modifier.height(10.dp))
                        BatchStrip(photos = photos, currentId = current?.id, onShow = viewModel::showPreview, onRemove = viewModel::remove)
                    }
                }
            },
            optionsPanel = {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) { options(settings) }
            },
            actionBar = {
                OmniActionBar(
                    summaryText = if (photos.size == 1) "1 foto" else "${photos.size} foto",
                    detailText = actionDetail(settings),
                    primaryButtonLabel = if (progress != null) "Salvo…" else if (photos.size == 1) "Salva foto" else "Applica a ${photos.size} foto",
                    isEnabled = photos.isNotEmpty() && progress == null && blocking == null,
                    disabledHelperText = blocking,
                    onPrimaryAction = { viewModel.applyAll() }
                )
            }
        )
    }
}

@Composable
private fun BatchStrip(photos: List<FilterPhoto>, currentId: String?, onShow: (String) -> Unit, onRemove: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
        items(photos, key = { it.id }) { photo ->
            val active = photo.id == currentId
            val scale by animateFloatAsState(if (active) 1.06f else 0.94f, motionSpec(Motion.bouncy()), label = "batch_scale")
            val ring by animateColorAsState(if (active) OmniAccent else Color.Transparent, label = "batch_ring")
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

// -------------------------------------------------------------------------------------------------
// Formato social (con cornice)
// -------------------------------------------------------------------------------------------------

@Composable
fun SocialFormatScreen(viewModel: SocialFormatViewModel, modifier: Modifier = Modifier) {
    BatchPhotoFrame(
        viewModel = viewModel,
        title = "Formato social",
        description = "Adatta le foto a post e storie, con cornice",
        icon = Icons.Default.AspectRatio,
        dropHeadline = "Scegli le foto da adattare",
        dropHelp = "Quadrate, verticali o per le storie, senza tagliare niente: lo spazio intorno si riempie con uno sfondo sfocato o colorato",
        actionDetail = { s ->
            val (w, h) = PhotoMath.canvasFor(s.aspect, SocialSettings.LONG_SIDE)
            "${s.aspect.label} · $w×$h px · ${s.background.label}"
        },
        preview = { photo, s -> SocialPreview(photo, s) },
        options = { s ->
            OptionSection(title = "Formato") {
                SegmentedControl(
                    options = SocialAspect.entries.map { it.label },
                    selectedIndex = s.aspect.ordinal,
                    onOptionSelected = { i -> viewModel.updateSettings { it.copy(aspect = SocialAspect.entries[i]) } }
                )
            }
            AnimatedText(text = s.aspect.hint, fontSize = 13.sp, color = OmniTextMuted)
            OptionSection(title = "Sfondo") {
                SegmentedControl(
                    options = SocialBackground.entries.map { it.label },
                    selectedIndex = s.background.ordinal,
                    onOptionSelected = { i -> viewModel.updateSettings { it.copy(background = SocialBackground.entries[i]) } }
                )
            }
            RevealVertically(visible = s.background == SocialBackground.COLOR) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SocialSettings.COLORS.forEach { c ->
                        ColorDot(argb = c, selected = s.color == c) { viewModel.updateSettings { it.copy(color = c) } }
                    }
                }
            }
            OmniSlider(
                label = "Cornice",
                value = s.border * 100f,
                onValueChange = { v -> viewModel.updateSettings { it.copy(border = v / 100f) } },
                valueRange = 0f..15f,
                valueFormatter = { if (it < 0.5f) "Nessuna" else "${it.roundToInt()}%" }
            )
            OmniSlider(
                label = "Angoli arrotondati",
                value = s.corner * 100f,
                onValueChange = { v -> viewModel.updateSettings { it.copy(corner = v / 100f) } },
                valueRange = 0f..20f,
                valueFormatter = { if (it < 0.5f) "Squadrati" else "${it.roundToInt()}%" }
            )
        },
        modifier = modifier
    )
}

@Composable
private fun SocialPreview(photo: FilterPhoto, s: SocialSettings) {
    val ratio by animateFloatAsState(s.aspect.ratio, motionSpec(Motion.gentle()), label = "social_ratio")
    val border by animateFloatAsState(s.border, motionSpec(Motion.gentle()), label = "social_border")
    val corner by animateFloatAsState(s.corner, motionSpec(Motion.gentle()), label = "social_corner")
    val bg by animateColorAsState(Color(s.color), motionSpec(tween(Motion.MEDIUM)), label = "social_bg")
    val blurAlpha by animateFloatAsState(if (s.background == SocialBackground.BLUR) 1f else 0f, motionSpec(tween(Motion.MEDIUM)), label = "social_blur")
    BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val w = minOf(maxWidth, maxHeight * ratio)
        val h = w / ratio
        val inset = minOf(w, h) * border
        Box(
            modifier = Modifier
                .size(w, h)
                .clip(RoundedCornerShape(6.dp))
                .background(bg)
                .border(1.dp, OmniBorder, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Blur needs Android 12+; older phones still get the zoomed, veiled photo behind.
            AsyncImage(
                model = photo.uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = blurAlpha }
                    .then(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Modifier.blur(24.dp) else Modifier)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = blurAlpha * 0.15f }
                    .background(Color.Black)
            )
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inset),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = photo.uri,
                    contentDescription = photo.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(minOf(maxWidth, maxHeight) * corner))
                )
            }
        }
    }
}

@Composable
private fun ColorDot(argb: Long, selected: Boolean, onClick: () -> Unit) {
    val fill = Color(argb)
    val scale by animateFloatAsState(if (selected) 1f else 0.86f, motionSpec(Motion.bouncy()), label = "dot_scale")
    val ring by animateColorAsState(if (selected) OmniAccent else OmniBorder, label = "dot_ring")
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
            .background(fill)
            .clickable(onClickLabel = "Colore ${PhotoMath.hex(argb.toInt())}", onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.animation.AnimatedVisibility(visible = selected, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
            Icon(Icons.Default.Check, contentDescription = null, tint = if (PhotoMath.luma(argb.toInt()) > 140) Color.Black else Color.White, modifier = Modifier.size(18.dp))
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Filigrana foto
// -------------------------------------------------------------------------------------------------

@Composable
fun PhotoWatermarkScreen(viewModel: PhotoWatermarkViewModel, modifier: Modifier = Modifier) {
    BatchPhotoFrame(
        viewModel = viewModel,
        title = "Filigrana foto",
        description = "Firma le tue foto con un testo",
        icon = Icons.Default.TextFields,
        dropHeadline = "Scegli le foto da firmare",
        dropHelp = "Aggiungi il tuo nome o una scritta in un angolo, al centro o ripetuta su tutta la foto",
        actionDetail = { s -> if (s.text.isBlank()) "Scrivi il testo" else "«${s.text.trim()}» · ${s.position.label.lowercase()}" },
        preview = { photo, s -> WatermarkPreview(photo, s) },
        options = { s ->
            MorphTextField(
                value = s.text,
                onValueChange = { v -> viewModel.updateSettings { it.copy(text = v.take(60)) } },
                label = "Testo",
                placeholder = "Es. © Alex 2026"
            )
            OptionSection(title = "Posizione") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    WatermarkPosition.entries.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { pos ->
                                PositionChip(label = pos.label, selected = s.position == pos, modifier = Modifier.weight(1f)) {
                                    viewModel.updateSettings { it.copy(position = pos) }
                                }
                            }
                        }
                    }
                }
            }
            OmniSlider(
                label = "Dimensione",
                value = s.size * 100f,
                onValueChange = { v -> viewModel.updateSettings { it.copy(size = v / 100f) } },
                valueRange = 2f..15f,
                valueFormatter = { "${it.roundToInt()}%" }
            )
            OmniSlider(
                label = "Opacità",
                value = s.opacity * 100f,
                onValueChange = { v -> viewModel.updateSettings { it.copy(opacity = v / 100f) } },
                valueRange = 10f..100f,
                valueFormatter = { "${it.roundToInt()}%" }
            )
            OptionRow(
                label = "Testo chiaro",
                description = if (s.light) "Bianco con ombra, per foto scure o colorate" else "Nero, per foto chiare e documenti",
                checked = s.light,
                onCheckedChange = { on -> viewModel.updateSettings { it.copy(light = on) } }
            )
        },
        modifier = modifier
    )
}

@Composable
private fun PositionChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val container by animateColorAsState(if (selected) OmniAccentSoft else OmniSurface2, label = "pos_bg")
    val ring by animateColorAsState(if (selected) OmniAccent else Color.Transparent, label = "pos_ring")
    val interaction = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        interactionSource = interaction,
        shape = RoundedCornerShape(RadiusSm),
        color = container,
        border = BorderStroke(1.5.dp, ring),
        modifier = modifier
            .height(48.dp)
            .pressScale(interaction, 0.95f)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 6.dp)) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (selected) OmniAccent else OmniText, maxLines = 2)
        }
    }
}

@Composable
private fun WatermarkPreview(photo: FilterPhoto, s: WatermarkSettings) {
    val (hBias, vBias) = when (s.position) {
        WatermarkPosition.TOP_LEFT -> -1f to -1f
        WatermarkPosition.TOP_RIGHT -> 1f to -1f
        WatermarkPosition.BOTTOM_LEFT -> -1f to 1f
        WatermarkPosition.BOTTOM_RIGHT -> 1f to 1f
        WatermarkPosition.CENTER, WatermarkPosition.TILED -> 0f to 0f
    }
    val hx by animateFloatAsState(hBias, motionSpec(Motion.gentle()), label = "wm_h")
    val vy by animateFloatAsState(vBias, motionSpec(Motion.gentle()), label = "wm_v")
    val alpha by animateFloatAsState(s.opacity, motionSpec(tween(Motion.SHORT)), label = "wm_alpha")
    val text = s.text.trim().ifBlank { "Il tuo testo" }
    val textColor = if (s.light) Color.White else Color.Black
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(RadiusSm))
            .background(OmniSurface2)
    ) {
        AsyncImage(model = photo.uri, contentDescription = photo.name, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
        val fontSize = (minOf(maxWidth, maxHeight).value * s.size).sp
        AnimatedContent(
            targetState = s.position == WatermarkPosition.TILED,
            transitionSpec = { fadeIn(tween(Motion.MEDIUM)) togetherWith fadeOut(tween(Motion.SHORT)) },
            label = "wm_mode"
        ) { tiled ->
            if (tiled) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            rotationZ = -30f
                            this.alpha = alpha
                            scaleX = 1.6f
                            scaleY = 1.6f
                        },
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    repeat(7) { row ->
                        Text(
                            text = List(4) { text }.joinToString("     "),
                            color = textColor,
                            fontSize = fontSize,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier.padding(start = if (row % 2 == 0) 0.dp else 40.dp)
                        )
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                    Text(
                        text = text,
                        color = textColor,
                        fontSize = fontSize,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier
                            .align(BiasAlignment(hx, vy))
                            .graphicsLayer { this.alpha = alpha }
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Ruota e specchia
// -------------------------------------------------------------------------------------------------

@Composable
fun RotatePhotosScreen(viewModel: RotatePhotosViewModel, modifier: Modifier = Modifier) {
    BatchPhotoFrame(
        viewModel = viewModel,
        title = "Ruota e specchia",
        description = "Raddrizza più foto in un colpo",
        icon = Icons.Default.Transform,
        dropHeadline = "Scegli le foto da ruotare",
        dropHelp = "Ruota di 90° o specchia tutte le foto scelte, ad esempio le scansioni venute storte",
        actionDetail = { s -> describeRotation(s) },
        preview = { photo, s -> RotatePreview(photo, s) },
        options = { s ->
            Text("Ogni tocco si applica a tutte le foto.", fontSize = 13.sp, color = OmniTextMuted)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RotateButton(Icons.Default.RotateLeft, "Ruota a sinistra", Modifier.weight(1f)) { viewModel.turn(clockwise = false) }
                RotateButton(Icons.Default.RotateRight, "Ruota a destra", Modifier.weight(1f)) { viewModel.turn(clockwise = true) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RotateButton(Icons.Default.Flip, "Specchia ↔", Modifier.weight(1f)) { viewModel.flip(horizontal = true) }
                RotateButton(Icons.Default.Flip, "Specchia ↕", Modifier.weight(1f), iconRotation = 90f) { viewModel.flip(horizontal = false) }
            }
            RevealVertically(visible = !s.isIdentity) {
                OutlinedButton(
                    onClick = { viewModel.reset() },
                    shape = RoundedCornerShape(RadiusMd),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, tint = OmniTextMuted)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ricomincia", color = OmniTextMuted)
                }
            }
        },
        modifier = modifier
    )
}

private fun describeRotation(s: RotateSettings): String {
    val degrees = ((s.degrees % 360) + 360) % 360
    return listOfNotNull(
        if (degrees != 0) "ruotate di $degrees°" else null,
        if (s.flipHorizontal) "specchiate ↔" else null,
        if (s.flipVertical) "specchiate ↕" else null
    ).joinToString(" · ").ifBlank { "Nessuna modifica" }
}

@Composable
private fun RotateButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    iconRotation: Float = 0f,
    onClick: () -> Unit
) {
    val haptics = rememberHaptics()
    val interaction = remember { MutableInteractionSource() }
    Surface(
        onClick = {
            haptics(Haptic.TICK)
            onClick()
        },
        interactionSource = interaction,
        shape = RoundedCornerShape(RadiusMd),
        color = OmniSurface2,
        modifier = modifier
            .height(64.dp)
            .pressScale(interaction, 0.94f)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = null, tint = OmniAccent, modifier = Modifier.graphicsLayer { rotationZ = iconRotation })
            Text(label, fontSize = 12.sp, color = OmniText)
        }
    }
}

/** The photo turns and flips with springs exactly as it will be saved. */
@Composable
private fun RotatePreview(photo: FilterPhoto, s: RotateSettings) {
    val rotation by animateFloatAsState(s.degrees.toFloat(), motionSpec(Motion.bouncy()), label = "rot_angle")
    val flipX by animateFloatAsState(if (s.flipHorizontal) -1f else 1f, motionSpec(Motion.gentle()), label = "rot_flip_x")
    val flipY by animateFloatAsState(if (s.flipVertical) -1f else 1f, motionSpec(Motion.gentle()), label = "rot_flip_y")
    val sideways = ((s.degrees % 180) + 180) % 180 == 90
    // Shrink a little while sideways so a landscape photo turned upright still fits the box.
    val fit by animateFloatAsState(if (sideways) 0.72f else 1f, motionSpec(Motion.gentle()), label = "rot_fit")
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(RadiusSm))
            .background(OmniSurface2),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = photo.uri,
            contentDescription = photo.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .graphicsLayer {
                    rotationZ = rotation
                    scaleX = flipX * fit
                    scaleY = flipY * fit
                }
        )
    }
}

// -------------------------------------------------------------------------------------------------
// Palette colori
// -------------------------------------------------------------------------------------------------

@Composable
fun PaletteScreen(viewModel: PaletteViewModel, modifier: Modifier = Modifier) {
    val photo by viewModel.photo.collectAsState()
    val colors by viewModel.colors.collectAsState()
    val count by viewModel.count.collectAsState()
    val analyzing by viewModel.analyzing.collectAsState()
    val savedUri by viewModel.savedUri.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val clipboard = LocalClipboardManager.current
    val haptics = rememberHaptics()
    var copied by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> if (uri != null) viewModel.load(uri) }
    val counts = listOf(4, 6, 8)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        ToolHeader(title = "Palette colori", description = "I colori principali di una foto")
        RevealVertically(visible = errorMessage != null) {
            OmniToast(message = errorMessage.orEmpty(), isError = true, actionLabel = "Chiudi", onAction = { viewModel.dismissError() }, modifier = Modifier.padding(bottom = 8.dp))
        }
        RevealVertically(visible = copied != null) {
            OmniToast(message = "Copiato ${copied.orEmpty()}", onDismiss = { copied = null }, autoDismissMillis = 1600, modifier = Modifier.padding(bottom = 8.dp))
        }
        RevealVertically(visible = savedUri != null) {
            OmniToast(
                message = "Palette salvata in ${OutputStorage.displayPath(OutputFolder.IMAGES)}",
                actionLabel = "Apri",
                onAction = { savedUri?.let { openFile(context, it, "image/png") } },
                onDismiss = { viewModel.dismissSaved() },
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        val current = photo
        AnimatedContent(
            targetState = current == null,
            transitionSpec = { stageTransition(if (initialState) 0 else 1, if (targetState) 0 else 1) },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            label = "palette_stage"
        ) { empty ->
            if (empty || current == null) {
                Dropzone(
                    icon = Icons.Default.Palette,
                    headline = "Scegli una foto",
                    helpText = "Trova i colori principali, con il codice da copiare per presentazioni, disegni e grafiche",
                    supportedFormats = "JPG · PNG · WEBP · HEIC",
                    actionLabel = "Scegli foto",
                    tintColor = TintImage,
                    onAction = { picker.launch("image/*") }
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(RadiusMd))
                            .background(OmniSurface2)
                            .clickable(onClickLabel = "Cambia foto") { picker.launch("image/*") }
                    ) {
                        AsyncImage(model = current.uri, contentDescription = current.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    }
                    SegmentedControl(
                        options = counts.map { "$it colori" },
                        selectedIndex = counts.indexOf(count).coerceAtLeast(0),
                        onOptionSelected = { viewModel.setCount(counts[it]) }
                    )
                    ShareBar(colors)
                    if (analyzing) {
                        Text("Cerco i colori…", fontSize = 14.sp, color = OmniTextMuted)
                    }
                    colors.forEachIndexed { index, (color, share) ->
                        Swatch(
                            argb = color,
                            share = share,
                            modifier = Modifier.enterAnimation(index, key = "swatch_${count}_$color"),
                            onCopy = {
                                val hex = PhotoMath.hex(color)
                                clipboard.setText(AnnotatedString(hex))
                                haptics(Haptic.CONFIRM)
                                copied = hex
                            }
                        )
                    }
                    OmniActionBar(
                        summaryText = "${colors.size} colori",
                        detailText = "Tocca un colore per copiarne il codice",
                        primaryButtonLabel = "Salva palette",
                        isEnabled = colors.isNotEmpty() && !analyzing,
                        onPrimaryAction = { viewModel.save() }
                    )
                }
            }
        }
    }
}

/** One bar split in proportion to how much of the photo each colour covers; segments grow in. */
@Composable
private fun ShareBar(colors: List<Pair<Int, Float>>) {
    if (colors.isEmpty()) return
    val reduce = LocalReduceMotion.current
    val growth = remember(colors) { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(colors) { if (!reduce) growth.animateTo(1f, tween(Motion.LONG, easing = Motion.EmphasizedDecelerate)) }
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(22.dp)
            .clip(RoundedCornerShape(11.dp))
    ) {
        var x = 0f
        colors.forEach { (color, share) ->
            val w = size.width * share * growth.value
            drawRect(Color(color), Offset(x, 0f), Size(w, size.height))
            x += w
        }
    }
}

@Composable
private fun Swatch(argb: Int, share: Float, onCopy: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    Surface(
        onClick = onCopy,
        interactionSource = interaction,
        shape = RoundedCornerShape(RadiusMd),
        color = OmniSurface,
        border = BorderStroke(1.dp, OmniBorder),
        modifier = modifier
            .fillMaxWidth()
            .pressScale(interaction, 0.97f)
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(argb))
                    .border(1.dp, OmniBorder, RoundedCornerShape(12.dp))
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(PhotoMath.hex(argb), fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = OmniText)
                val r = (argb shr 16) and 0xFF
                val g = (argb shr 8) and 0xFF
                val b = argb and 0xFF
                Text("RGB $r, $g, $b · ${(share * 100).roundToInt()}% della foto", fontSize = 12.sp, color = OmniTextMuted)
            }
            Text("Copia", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OmniAccent)
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Registra audio
// -------------------------------------------------------------------------------------------------

@Composable
fun RecorderScreen(
    viewModel: RecorderViewModel,
    modifier: Modifier = Modifier,
    onConvert: (Uri) -> Unit = {},
    onTranscribe: (Uri) -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val elapsed by viewModel.elapsedMs.collectAsState()
    val levels by viewModel.levels.collectAsState()
    val savedUri by viewModel.savedUri.collectAsState()
    val saving by viewModel.saving.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val context = LocalContext.current
    val haptics = rememberHaptics()
    var permissionDenied by remember { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionDenied = !granted
        if (granted) viewModel.start()
    }
    val startRecording = {
        haptics(Haptic.CONFIRM)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            viewModel.start()
        } else {
            permission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    AnimatedContent(
        targetState = savedUri != null,
        transitionSpec = { stageTransition(if (initialState) 1 else 0, if (targetState) 1 else 0) },
        modifier = modifier,
        label = "recorder_stage"
    ) { saved ->
        val uri = savedUri
        if (saved && uri != null) {
            ConversionResultView(
                title = "Registrazione salvata",
                subtitle = "Durata ${formatClock(elapsed)} · M4A (AAC)",
                outputUri = uri,
                mimeType = "audio/mp4",
                previewIcon = Icons.Default.Mic,
                previewTint = TintAudio,
                savedIn = "Salvato in ${OutputStorage.displayPath(OutputFolder.AUDIO)}",
                onClose = { viewModel.dismissSaved() },
                onNewConversion = { viewModel.dismissSaved() },
                continueActions = listOf(
                    ContinueAction("Converti o taglia", Icons.Default.Audiotrack, TintAudio) { onConvert(uri) },
                    ContinueAction("Trascrivi con Gemini", Icons.Default.AutoAwesome, OmniAccent) { onTranscribe(uri) }
                )
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ToolHeader(title = "Registra audio", description = "Registra lezioni e note vocali")
                RevealVertically(visible = errorMessage != null) {
                    OmniToast(message = errorMessage.orEmpty(), isError = true, actionLabel = "Chiudi", onAction = { viewModel.dismissError() }, modifier = Modifier.padding(bottom = 8.dp))
                }
                RevealVertically(visible = permissionDenied) {
                    OmniToast(
                        message = "Serve il permesso del microfono: attivalo nelle impostazioni del telefono",
                        isError = true,
                        actionLabel = "Chiudi",
                        onAction = { permissionDenied = false },
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                Spacer(modifier = Modifier.weight(0.4f))
                AnimatedContent(
                    targetState = formatClock(elapsed),
                    transitionSpec = { fadeIn(tween(90)) togetherWith fadeOut(tween(90)) },
                    label = "recorder_clock"
                ) { clock ->
                    Text(clock, fontSize = 52.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = OmniText)
                }
                AnimatedText(
                    text = when (state) {
                        RecorderState.IDLE -> if (saving) "Salvo…" else "Tocca il microfono per iniziare"
                        RecorderState.RECORDING -> "Sto registrando"
                        RecorderState.PAUSED -> "In pausa"
                    },
                    fontSize = 15.sp,
                    color = if (state == RecorderState.RECORDING) OmniDanger else OmniTextMuted
                )
                Spacer(modifier = Modifier.height(24.dp))
                Waveform(levels = levels, active = state == RecorderState.RECORDING)
                Spacer(modifier = Modifier.weight(0.6f))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                    SmallRoundButton(
                        icon = Icons.Default.Delete,
                        label = "Scarta",
                        enabled = state != RecorderState.IDLE,
                        onClick = { viewModel.discard() }
                    )
                    RecordButton(
                        state = state,
                        onClick = {
                            when (state) {
                                RecorderState.IDLE -> startRecording()
                                RecorderState.RECORDING -> viewModel.pause()
                                RecorderState.PAUSED -> viewModel.resume()
                            }
                        }
                    )
                    SmallRoundButton(
                        icon = Icons.Default.Stop,
                        label = "Salva",
                        enabled = state != RecorderState.IDLE,
                        onClick = {
                            haptics(Haptic.CONFIRM)
                            viewModel.stop()
                        }
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text("Le registrazioni restano sul telefono, in ${OutputStorage.displayPath(OutputFolder.AUDIO)}.", fontSize = 12.sp, color = OmniTextMuted)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

/** Big round button: pulses while recording, morphs between mic / pause / play. */
@Composable
private fun RecordButton(state: RecorderState, onClick: () -> Unit) {
    val reduce = LocalReduceMotion.current
    val transition = rememberInfiniteTransition(label = "record_pulse")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (reduce) 1f else 1.25f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "record_pulse_scale"
    )
    val recording = state == RecorderState.RECORDING
    val ringScale = if (recording) pulse else 1f
    val color by animateColorAsState(if (state == RecorderState.IDLE) OmniAccent else OmniDanger, label = "record_color")
    val size by animateDpAsState(if (recording) 92.dp else 84.dp, motionSpec(Motion.bouncy()), label = "record_size")
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
        Box(
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    scaleX = ringScale
                    scaleY = ringScale
                    alpha = if (recording) 0.25f else 0f
                }
                .clip(CircleShape)
                .background(color)
        )
        val interaction = remember { MutableInteractionSource() }
        Surface(
            onClick = onClick,
            interactionSource = interaction,
            shape = CircleShape,
            color = color,
            shadowElevation = 6.dp,
            modifier = Modifier
                .size(size)
                .pressScale(interaction, 0.92f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                AnimatedContent(
                    targetState = state,
                    transitionSpec = { (scaleIn(initialScale = 0.5f) + fadeIn()) togetherWith (scaleOut(targetScale = 0.5f) + fadeOut()) },
                    label = "record_icon"
                ) { s ->
                    Icon(
                        imageVector = when (s) {
                            RecorderState.IDLE -> Icons.Default.Mic
                            RecorderState.RECORDING -> Icons.Default.Pause
                            RecorderState.PAUSED -> Icons.Default.PlayArrow
                        },
                        contentDescription = when (s) {
                            RecorderState.IDLE -> "Inizia a registrare"
                            RecorderState.RECORDING -> "Pausa"
                            RecorderState.PAUSED -> "Riprendi"
                        },
                        tint = OmniOnAccent,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SmallRoundButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    val alpha by animateFloatAsState(if (enabled) 1f else 0.35f, label = "small_round_alpha")
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.graphicsLayer { this.alpha = alpha }) {
        Surface(onClick = onClick, enabled = enabled, shape = CircleShape, color = OmniSurface2, modifier = Modifier.size(56.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, tint = OmniText)
            }
        }
        Text(label, fontSize = 12.sp, color = OmniTextMuted, modifier = Modifier.padding(top = 6.dp))
    }
}

/** Rolling bars of the latest input levels; flat dots while idle. */
@Composable
private fun Waveform(levels: List<Float>, active: Boolean) {
    val accent = OmniAccent
    val muted = OmniBorder
    val glow by animateFloatAsState(if (active) 1f else 0.4f, motionSpec(tween(Motion.MEDIUM)), label = "wave_glow")
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
    ) {
        val bars = RecorderViewModel.LEVELS
        val gap = size.width / bars
        val barW = gap * 0.55f
        val offset = bars - levels.size
        for (i in 0 until bars) {
            val level = levels.getOrNull(i - offset) ?: 0f
            val h = (size.height * (0.06f + 0.94f * level)).coerceAtLeast(barW)
            val x = i * gap + (gap - barW) / 2f
            drawRoundRect(
                color = if (level > 0.02f) accent.copy(alpha = glow) else muted,
                topLeft = Offset(x, (size.height - h) / 2f),
                size = Size(barW, h),
                cornerRadius = CornerRadius(barW / 2f)
            )
        }
    }
}

private fun formatClock(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val tenths = (ms % 1000) / 100
    return String.format(java.util.Locale.ITALY, "%02d:%02d,%d", minutes, seconds, tenths)
}
