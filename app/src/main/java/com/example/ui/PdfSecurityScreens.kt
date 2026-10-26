package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.PageNumberPosition
import com.example.service.PageNumberStyle
import com.example.service.StampColor
import com.example.service.StampConfig
import com.example.service.WatermarkPosition
import com.example.ui.components.AnimatedText
import com.example.ui.components.ContinueAction
import com.example.ui.components.ConversionResultView
import com.example.ui.components.MorphChipRow
import com.example.ui.components.MorphTextField
import com.example.ui.components.OmniActionBar
import com.example.ui.components.OmniSlider
import com.example.ui.components.OptionRow
import com.example.ui.components.OptionSection
import com.example.ui.components.RevealVertically
import com.example.ui.components.SegmentedControl
import com.example.ui.components.formatBytes
import com.example.ui.theme.Haptic
import com.example.ui.theme.Motion
import com.example.ui.theme.OmniAccent
import com.example.ui.theme.OmniAccentSoft
import com.example.ui.theme.OmniBorder
import com.example.ui.theme.OmniSurface2
import com.example.ui.theme.OmniText
import com.example.ui.theme.OmniTextMuted
import com.example.ui.theme.RadiusMd
import com.example.ui.theme.TintMerge
import com.example.ui.theme.TintPdf
import com.example.ui.theme.TintSecure
import com.example.ui.theme.TintStamp
import com.example.ui.theme.enterAnimation
import com.example.ui.theme.motionSpec
import com.example.ui.theme.rememberHaptics
import com.example.util.OutputFolder
import com.example.util.OutputStorage
import com.example.viewmodel.PdfSecureViewModel
import com.example.viewmodel.PdfStampViewModel
import com.example.viewmodel.SecureMode
import kotlin.math.atan2
import kotlin.math.hypot

// -------------------------------------------------------------------------------------------------
// Filigrana e numeri di pagina
// -------------------------------------------------------------------------------------------------

private val WatermarkPresets = listOf("BOZZA", "RISERVATO", "COPIA", "NON DIFFONDERE")

@Composable
fun PdfStampScreen(
    viewModel: PdfStampViewModel,
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
    val config by viewModel.config.collectAsState()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.loadPdf(uri)
    }

    AnimatedContent(
        targetState = resultUri != null,
        transitionSpec = { stageTransition(if (initialState) 1 else 0, if (targetState) 1 else 0) },
        modifier = modifier,
        label = "stamp_stage"
    ) { showResult ->
        val uri = resultUri
        if (showResult && uri != null) {
            ConversionResultView(
                title = if (config.hasWatermark) "Filigrana applicata" else "Pagine numerate",
                subtitle = listOfNotNull(
                    config.text.takeIf { config.hasWatermark }?.let { "«$it» su ogni pagina" },
                    config.numberStyle.takeIf { config.hasNumbers }?.let { "numeri ${it.label}" }
                ).joinToString(" · ").replaceFirstChar { it.uppercase() },
                outputUri = uri,
                mimeType = "application/pdf",
                previewIcon = Icons.Default.WaterDrop,
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
                title = "Filigrana e numeri",
                description = "Timbra ogni pagina con un testo o la numerazione",
                source = source,
                isLoading = isLoading,
                isWorking = isWorking,
                progress = progress,
                progressLabel = "Timbro la pagina ${progress.first} di ${progress.second}…",
                errorMessage = errorMessage,
                onDismissError = { viewModel.dismissError() },
                onCancel = { viewModel.cancel() },
                onPick = { picker.launch("application/pdf") },
                onClear = { viewModel.clear() },
                dropIcon = Icons.Default.WaterDrop,
                dropHeadline = "Scegli il PDF da timbrare",
                dropHelp = "Aggiungi «BOZZA», il tuo nome o i numeri di pagina a verifiche e appunti",
                tint = TintStamp
            ) { pdf ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StampPreview(
                        loadKey = pdf.uri,
                        config = config,
                        loadPage = { width -> viewModel.previewPage(width) },
                        modifier = Modifier.enterAnimation(1)
                    )
                    StampOptions(config = config, onChange = viewModel::update)
                    OmniActionBar(
                        summaryText = "${pdf.pageCount} pagine · ${formatBytes(pdf.sizeBytes)}",
                        detailText = listOfNotNull(
                            "Filigrana".takeIf { config.hasWatermark },
                            "Numeri".takeIf { config.hasNumbers }
                        ).joinToString(" + ").ifEmpty { null },
                        primaryButtonLabel = if (isWorking) "Applico…" else "Applica",
                        isEnabled = !isWorking && !config.isEmpty,
                        disabledHelperText = if (config.isEmpty) "Scrivi una filigrana o scegli i numeri di pagina" else null,
                        onPrimaryAction = { viewModel.apply() },
                        testTag = "stamp_pdf_button"
                    )
                }
            }
        }
    }
}

@Composable
private fun StampOptions(config: StampConfig, onChange: ((StampConfig) -> StampConfig) -> Unit) {
    val haptics = rememberHaptics()
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OptionSection(title = "Filigrana", hint = if (config.text.isBlank()) "Lascia vuoto per aggiungere solo i numeri di pagina" else null) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MorphTextField(
                    value = config.text,
                    onValueChange = { value -> onChange { it.copy(text = value.take(60)) } },
                    placeholder = "Es. BOZZA, il tuo nome…",
                    testTag = "stamp_text_field"
                )
                MorphChipRow(
                    options = WatermarkPresets,
                    selected = config.text,
                    onSelected = { value -> onChange { it.copy(text = value) } }
                )
            }
        }
        RevealVertically(visible = config.hasWatermark) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SegmentedControl(
                    options = WatermarkPosition.entries.map { it.label },
                    selectedIndex = config.position.ordinal,
                    onOptionSelected = { index -> onChange { it.copy(position = WatermarkPosition.entries[index]) } }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Colore", color = OmniTextMuted, fontSize = 14.sp, modifier = Modifier.width(64.dp))
                    StampColor.entries.forEach { color ->
                        ColorSwatch(
                            color = color.toCompose(),
                            selected = color == config.color,
                            label = color.label,
                            onClick = {
                                haptics(Haptic.TICK)
                                onChange { it.copy(color = color) }
                            }
                        )
                    }
                }
                OmniSlider(
                    label = "Opacità",
                    value = config.opacity * 100f,
                    onValueChange = { value -> onChange { it.copy(opacity = value / 100f) } },
                    valueRange = 5f..100f,
                    valueFormatter = { "${it.toInt()}%" }
                )
                OmniSlider(
                    label = "Dimensione",
                    value = config.fontSize,
                    onValueChange = { value -> onChange { it.copy(fontSize = value) } },
                    valueRange = 16f..120f,
                    valueFormatter = { "${it.toInt()} pt" }
                )
            }
        }
        OptionSection(title = "Numeri di pagina") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MorphChipRow(
                    options = PageNumberStyle.entries.map { it.label },
                    selected = config.numberStyle.label,
                    onSelected = { label -> onChange { c -> c.copy(numberStyle = PageNumberStyle.entries.first { it.label == label }) } }
                )
                RevealVertically(visible = config.hasNumbers) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        MorphChipRow(
                            options = PageNumberPosition.entries.map { it.label },
                            selected = config.numberPosition.label,
                            onSelected = { label -> onChange { c -> c.copy(numberPosition = PageNumberPosition.entries.first { it.label == label }) } }
                        )
                        OmniSlider(
                            label = "Inizia da",
                            value = config.startNumber.toFloat(),
                            onValueChange = { value -> onChange { it.copy(startNumber = value.toInt().coerceAtLeast(1)) } },
                            valueRange = 1f..50f,
                            steps = 48
                        )
                        OptionRow(
                            label = "Salta la copertina",
                            description = "La prima pagina resta senza numero",
                            checked = config.skipFirstPage,
                            onCheckedChange = { value -> onChange { it.copy(skipFirstPage = value) } }
                        )
                    }
                }
            }
        }
    }
}

private fun StampColor.toCompose(): Color = Color(red = r, green = g, blue = b)

@Composable
private fun ColorSwatch(color: Color, selected: Boolean, label: String, onClick: () -> Unit) {
    val scale by animateFloatAsState(if (selected) 1.12f else 1f, motionSpec(Motion.bouncy()), label = "swatch_scale")
    val ring by animateColorAsState(if (selected) OmniAccent else OmniBorder, label = "swatch_ring")
    Box(
        modifier = Modifier
            .size(34.dp)
            .scale(scale)
            .clip(CircleShape)
            .border(2.dp, ring, CircleShape)
            .padding(4.dp)
            .clip(CircleShape)
            .background(color)
            .clickable(onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(targetState = selected, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "swatch_check") { isSelected ->
            if (isSelected) Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}

/** First page of the PDF with an on-screen approximation of the stamp, updated live. */
@Composable
private fun StampPreview(
    loadKey: Any,
    config: StampConfig,
    loadPage: suspend (Int) -> android.graphics.Bitmap?,
    modifier: Modifier = Modifier
) {
    val widthPx = with(LocalDensity.current) { 280.dp.roundToPx() }
    val page by produceState<android.graphics.Bitmap?>(initialValue = null, loadKey) { value = loadPage(widthPx) }
    val textMeasurer = rememberTextMeasurer()
    val opacity by animateFloatAsState(config.opacity, motionSpec(Motion.gentle()), label = "stamp_opacity")
    val fontSize by animateFloatAsState(config.fontSize, motionSpec(Motion.gentle()), label = "stamp_size")

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
            Box(modifier = Modifier.height(220.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = OmniAccent, strokeWidth = 3.dp, modifier = Modifier.size(28.dp))
            }
        } else {
            val ratio = bitmap.width.toFloat() / bitmap.height
            Box(
                modifier = Modifier
                    .heightIn(max = 300.dp)
                    .aspectRatio(ratio)
                    .clip(RoundedCornerShape(6.dp))
                    .border(1.dp, OmniBorder, RoundedCornerShape(6.dp))
            ) {
                Image(
                    bitmap = remember(bitmap) { bitmap.asImageBitmap() },
                    contentDescription = "Anteprima della prima pagina",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // PdfRenderer pages are rendered at a fixed width; assume A4 to map points to pixels.
                    val pageWidthPt = if (ratio > 1f) 842f else 595f
                    val scale = size.width / pageWidthPt
                    if (config.hasWatermark) drawWatermarkPreview(textMeasurer, config.copy(opacity = opacity, fontSize = fontSize), scale)
                    if (config.hasNumbers) drawNumberPreview(textMeasurer, config, scale)
                }
            }
        }
    }
}

private fun DrawScope.drawWatermarkPreview(measurer: TextMeasurer, config: StampConfig, scale: Float) {
    val color = config.color.toCompose().copy(alpha = config.opacity.coerceIn(0.05f, 1f))
    var fontPx = config.fontSize * scale
    val base = TextStyle(fontWeight = FontWeight.Bold, color = color)
    var layout = measurer.measure(config.text.trim(), base.copy(fontSize = fontPx.toSp()))
    val maxWidth = if (config.position == WatermarkPosition.DIAGONAL) hypot(size.width, size.height) * 0.8f else size.width * 0.85f
    if (config.position != WatermarkPosition.TILED && layout.size.width > maxWidth) {
        fontPx *= maxWidth / layout.size.width
        layout = measurer.measure(config.text.trim(), base.copy(fontSize = fontPx.toSp()))
    }
    val w = layout.size.width.toFloat()
    val h = layout.size.height.toFloat()

    fun place(cx: Float, cy: Float, degrees: Float) {
        rotate(degrees, pivot = Offset(cx, cy)) {
            drawText(layout, topLeft = Offset(cx - w / 2f, cy - h / 2f))
        }
    }

    when (config.position) {
        WatermarkPosition.DIAGONAL -> {
            val degrees = -Math.toDegrees(atan2(size.height.toDouble(), size.width.toDouble())).toFloat()
            place(size.width / 2f, size.height / 2f, degrees)
        }
        WatermarkPosition.CENTER -> place(size.width / 2f, size.height / 2f, 0f)
        WatermarkPosition.TOP -> place(size.width / 2f, fontPx * 1.2f, 0f)
        WatermarkPosition.TILED -> {
            val stepX = (w + fontPx * 2f).coerceAtLeast(80f * scale)
            val stepY = (fontPx * 4f).coerceAtLeast(60f * scale)
            var row = 0
            var y = size.height - stepY / 2f
            while (y > -stepY) {
                var x = if (row % 2 == 0) stepX / 2f else 0f
                while (x < size.width + stepX) {
                    place(x, y, -30f)
                    x += stepX
                }
                y -= stepY
                row++
            }
        }
    }
}

private fun DrawScope.drawNumberPreview(measurer: TextMeasurer, config: StampConfig, scale: Float) {
    val text = config.numberStyle.format(config.startNumber, config.startNumber + 9)
    if (config.skipFirstPage) return
    val layout = measurer.measure(text, TextStyle(fontSize = (10f * scale).toSp(), color = Color.DarkGray))
    val margin = 22f * scale
    val x = when (config.numberPosition) {
        PageNumberPosition.BOTTOM_CENTER -> (size.width - layout.size.width) / 2f
        else -> size.width - margin - layout.size.width
    }
    val y = when (config.numberPosition) {
        PageNumberPosition.TOP_RIGHT -> margin
        else -> size.height - margin - layout.size.height
    }
    drawText(layout, topLeft = Offset(x, y))
}

// -------------------------------------------------------------------------------------------------
// Proteggi / sblocca
// -------------------------------------------------------------------------------------------------

@Composable
fun PdfSecureScreen(
    viewModel: PdfSecureViewModel,
    modifier: Modifier = Modifier,
    onContinueWithOrganize: (Uri) -> Unit = {},
    onContinueWithStamp: (Uri) -> Unit = {}
) {
    val source by viewModel.source.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isWorking by viewModel.isWorking.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val resultUri by viewModel.resultUri.collectAsState()
    val security by viewModel.security.collectAsState()
    val mode by viewModel.mode.collectAsState()
    val options by viewModel.options.collectAsState()
    val confirm by viewModel.confirm.collectAsState()
    val currentPassword by viewModel.currentPassword.collectAsState()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.loadPdf(uri)
    }

    AnimatedContent(
        targetState = resultUri != null,
        transitionSpec = { stageTransition(if (initialState) 1 else 0, if (targetState) 1 else 0) },
        modifier = modifier,
        label = "secure_stage"
    ) { showResult ->
        val uri = resultUri
        if (showResult && uri != null) {
            val protected = mode == SecureMode.PROTECT
            ConversionResultView(
                title = if (protected) "PDF protetto" else "PDF sbloccato",
                subtitle = if (protected) "Serve la password per aprirlo. Non dimenticarla: non si può recuperare." else "Ora si apre senza password in tutti gli strumenti.",
                outputUri = uri,
                mimeType = "application/pdf",
                previewIcon = if (protected) Icons.Default.Lock else Icons.Default.LockOpen,
                previewTint = TintSecure,
                savedIn = "Salvato in ${OutputStorage.displayPath(OutputFolder.PDFS)}",
                onClose = { viewModel.dismissResult() },
                onNewConversion = { viewModel.clear() },
                continueActions = if (protected) emptyList() else listOf(
                    ContinueAction("Organizza le pagine", Icons.Default.ViewModule, TintMerge) { onContinueWithOrganize(uri) },
                    ContinueAction("Aggiungi una filigrana", Icons.Default.WaterDrop, TintStamp) { onContinueWithStamp(uri) }
                )
            )
        } else {
            SinglePdfScaffold(
                title = "Proteggi PDF",
                description = "Aggiungi o togli la password a un PDF",
                source = source,
                isLoading = isLoading,
                isWorking = isWorking,
                progress = progress,
                progressLabel = if (mode == SecureMode.PROTECT) "Cifro il documento…" else "Rimuovo la protezione…",
                errorMessage = errorMessage,
                onDismissError = { viewModel.dismissError() },
                onCancel = { viewModel.cancel() },
                onPick = { picker.launch("application/pdf") },
                onClear = { viewModel.clear() },
                dropIcon = Icons.Default.Lock,
                dropHeadline = "Scegli il PDF",
                dropHelp = "Proteggilo con una password o sblocca un PDF di cui conosci la password",
                tint = TintSecure
            ) { pdf ->
                val info = security
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SecurityStatusCard(
                        checking = info == null,
                        encrypted = info?.isEncrypted == true,
                        needsPassword = info?.needsPassword == true,
                        modifier = Modifier.enterAnimation(1)
                    )
                    SegmentedControl(
                        options = SecureMode.entries.map { it.label },
                        selectedIndex = mode.ordinal,
                        onOptionSelected = { viewModel.setMode(SecureMode.entries[it]) }
                    )
                    RevealVertically(visible = info?.needsPassword == true) {
                        PasswordField(
                            value = currentPassword,
                            onValueChange = viewModel::setCurrentPassword,
                            label = "Password attuale del PDF",
                            testTag = "secure_current_password"
                        )
                    }
                    AnimatedContent(
                        targetState = mode,
                        transitionSpec = { stageTransition(initialState.ordinal, targetState.ordinal) },
                        label = "secure_mode"
                    ) { currentMode ->
                        if (currentMode == SecureMode.PROTECT) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                PasswordField(
                                    value = options.password,
                                    onValueChange = viewModel::setPassword,
                                    label = "Nuova password",
                                    supportingText = PasswordStrength.of(options.password).label.takeIf { options.password.isNotEmpty() },
                                    testTag = "secure_new_password"
                                )
                                PasswordStrengthBar(options.password)
                                PasswordField(
                                    value = confirm,
                                    onValueChange = viewModel::setConfirm,
                                    label = "Ripeti la password",
                                    isError = confirm.isNotEmpty() && confirm != options.password,
                                    supportingText = if (confirm.isNotEmpty() && confirm != options.password) "Le password non coincidono" else null,
                                    testTag = "secure_confirm_password"
                                )
                                OptionSection(title = "Cosa può fare chi apre il PDF") {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OptionRow("Stampare", "Consenti la stampa del documento", options.allowPrinting, viewModel::setAllowPrinting)
                                        OptionRow("Copiare il testo", "Consenti di selezionare e copiare", options.allowCopying, viewModel::setAllowCopying)
                                        OptionRow("Modificare", "Consenti modifiche e riordino pagine", options.allowEditing, viewModel::setAllowEditing)
                                    }
                                }
                            }
                        } else {
                            AnimatedText(
                                text = when {
                                    info?.isEncrypted == false -> "Questo PDF non ha nessuna protezione da togliere."
                                    info?.needsPassword == true -> "Inserisci la password: salverò una copia che si apre senza."
                                    else -> "Il PDF ha solo restrizioni (stampa, copia): le rimuovo in un tocco."
                                },
                                fontSize = 14.sp,
                                color = OmniTextMuted
                            )
                        }
                    }
                    val reason = remember(security, mode, options, confirm, currentPassword) { viewModel.blockingReason() }
                    OmniActionBar(
                        summaryText = pdf.name,
                        detailText = formatBytes(pdf.sizeBytes),
                        primaryButtonLabel = when {
                            isWorking -> "Attendi…"
                            mode == SecureMode.PROTECT -> "Proteggi"
                            else -> "Sblocca"
                        },
                        isEnabled = !isWorking && reason == null,
                        disabledHelperText = reason,
                        onPrimaryAction = { viewModel.run() },
                        testTag = "secure_pdf_button"
                    )
                }
            }
        }
    }
}

@Composable
private fun SecurityStatusCard(checking: Boolean, encrypted: Boolean, needsPassword: Boolean, modifier: Modifier = Modifier) {
    val tint by animateColorAsState(if (encrypted) TintSecure else TintPdf, label = "security_tint")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RadiusMd))
            .background(OmniAccentSoft)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedContent(targetState = Triple(checking, encrypted, needsPassword), transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "security_icon") { (isChecking, isEncrypted, _) ->
            Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                if (isChecking) {
                    CircularProgressIndicator(color = tint, strokeWidth = 2.5.dp, modifier = Modifier.size(22.dp))
                } else {
                    Icon(if (isEncrypted) Icons.Default.Lock else Icons.Default.LockOpen, contentDescription = null, tint = tint)
                }
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        AnimatedText(
            text = when {
                checking -> "Controllo la protezione…"
                needsPassword -> "Protetto: serve una password per aprirlo"
                encrypted -> "Ha restrizioni, ma si apre senza password"
                else -> "Nessuna protezione: chiunque può aprirlo"
            },
            fontSize = 14.sp,
            color = OmniText
        )
    }
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    testTag: String,
    isError: Boolean = false,
    supportingText: String? = null
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    MorphTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        isError = isError,
        supportingText = supportingText,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (visible) "Nascondi password" else "Mostra password",
                    tint = OmniTextMuted
                )
            }
        },
        testTag = testTag
    )
}

internal enum class PasswordStrength(val label: String, val level: Int) {
    WEAK("Password debole", 1),
    FAIR("Password discreta", 2),
    GOOD("Password buona", 3),
    STRONG("Password forte", 4);

    companion object {
        fun of(password: String): PasswordStrength {
            var score = 0
            if (password.length >= 8) score++
            if (password.length >= 12) score++
            if (password.any { it.isDigit() } && password.any { it.isLetter() }) score++
            if (password.any { it.isUpperCase() } && password.any { it.isLowerCase() }) score++
            if (password.any { !it.isLetterOrDigit() }) score++
            return when {
                password.length < 6 || score <= 1 -> WEAK
                score == 2 -> FAIR
                score == 3 -> GOOD
                else -> STRONG
            }
        }
    }
}

@Composable
private fun PasswordStrengthBar(password: String) {
    val strength = PasswordStrength.of(password)
    val fraction by animateFloatAsState(if (password.isEmpty()) 0f else strength.level / 4f, motionSpec(Motion.gentle()), label = "strength")
    val color by animateColorAsState(
        when (strength) {
            PasswordStrength.WEAK -> MaterialTheme.colorScheme.error
            PasswordStrength.FAIR -> TintStamp
            PasswordStrength.GOOD -> TintPdf
            PasswordStrength.STRONG -> TintSecure
        },
        label = "strength_color"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(CircleShape)
            .background(OmniSurface2)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(6.dp)
                .clip(CircleShape)
                .background(color)
        )
    }
}
