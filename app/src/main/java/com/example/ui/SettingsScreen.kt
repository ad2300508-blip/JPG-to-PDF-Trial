package com.example.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import com.example.ui.components.AnimatedText
import com.example.ui.components.OptionRow
import com.example.ui.components.formatBytes
import com.example.ui.theme.Haptic
import com.example.ui.theme.Motion
import com.example.ui.theme.motionSpec
import com.example.ui.theme.pressScale
import com.example.ui.theme.rememberHaptics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.example.model.PdfQuality
import com.example.ui.components.SegmentedControl
import com.example.ui.theme.AccentChoice
import com.example.ui.theme.LocalMorphColors
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.data.AppPreferences
import com.example.data.HistoryRepository
import com.example.data.ThemeMode
import com.example.ui.components.IconTile
import com.example.ui.components.SectionLabel
import com.example.ui.theme.MorphDarkColors
import com.example.ui.theme.MorphLightColors
import com.example.ui.theme.OmniAccent
import com.example.ui.theme.OmniAccentSoft
import com.example.ui.theme.OmniBorder
import com.example.ui.theme.OmniDanger
import com.example.ui.theme.OmniOnAccent
import com.example.ui.theme.OmniSurface
import com.example.ui.theme.OmniText
import com.example.ui.theme.OmniTextMuted
import com.example.ui.theme.RadiusCard
import com.example.ui.theme.RadiusMd
import com.example.ui.theme.TintAi
import com.example.ui.theme.TintPdf
import com.example.ui.theme.TintZip
import android.os.Build
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.Brush
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import com.example.ui.theme.LocalReduceMotion
import com.example.ui.theme.DynamicSwatchColors

@Composable
fun SettingsScreen(
    onOpenGuide: () -> Unit,
    onOpenGemini: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenWhatsNew: () -> Unit = {}
) {
    val themeMode by AppPreferences.themeMode.collectAsState()
    val reduceMotion by AppPreferences.reduceMotion.collectAsState()
    val systemReduceMotion by AppPreferences.systemReduceMotion.collectAsState()
    val haptics by AppPreferences.haptics.collectAsState()
    val accent by AppPreferences.accent.collectAsState()
    val defaultQuality by AppPreferences.defaultQuality.collectAsState()
    val defaultPageSize by AppPreferences.defaultPageSize.collectAsState()
    val autoClean by AppPreferences.autoClean.collectAsState()
    val stripMetadata by AppPreferences.stripMetadata.collectAsState()
    val pureBlack by AppPreferences.pureBlack.collectAsState()
    val dynamicAccent by AppPreferences.dynamicAccent.collectAsState()
    val cacheCleanup by AppPreferences.cacheCleanup.collectAsState()
    var confirmReset by remember { mutableStateOf(false) }
    val historyEntries by HistoryRepository.entries.collectAsState()
    val historyCount = historyEntries.size
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var cacheBytes by remember { mutableLongStateOf(-1L) }
    LaunchedEffect(Unit) { cacheBytes = withContext(Dispatchers.IO) { dirSize(context.cacheDir) } }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .testTag("settings_screen")
        ) {
            Text("Impostazioni", style = MaterialTheme.typography.displayLarge, color = OmniText)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Personalizza l'aspetto e il comportamento dell'app.", fontSize = 16.sp, color = OmniTextMuted)

            if (historyEntries.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                UsageStats(historyEntries)
            }

            Spacer(modifier = Modifier.height(24.dp))
            SectionLabel("Aspetto")
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ThemeMode.entries.forEach { mode ->
                    ThemeOptionCard(
                        mode = mode,
                        selected = mode == themeMode,
                        onClick = { AppPreferences.setThemeMode(mode) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            AnimatedText(themeMode.description, fontSize = 13.sp, color = OmniTextMuted)

            Spacer(modifier = Modifier.height(16.dp))
            SettingsGroup {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)) {
                    Text("Colore principale", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OmniText)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        AccentChoice.entries.forEach { choice ->
                            AccentSwatch(
                                choice = choice,
                                selected = choice == accent && !dynamicAccent,
                                onClick = {
                                    AppPreferences.setDynamicAccent(false)
                                    AppPreferences.setAccent(choice)
                                }
                            )
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            DynamicSwatch(selected = dynamicAccent, onClick = { AppPreferences.setDynamicAccent(true) })
                        }
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Spacer(modifier = Modifier.height(8.dp))
                        AnimatedText(
                            if (dynamicAccent) "Il colore segue lo sfondo del telefono" else "Con l'ultimo cerchio il colore segue lo sfondo del telefono",
                            fontSize = 12.sp,
                            color = OmniTextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            SettingsGroup {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                    OptionRow(
                        label = "Riduci animazioni",
                        description = if (systemReduceMotion) "Già attive: le animazioni sono disattivate nelle impostazioni del telefono" else "Transizioni istantanee, utile su telefoni lenti o se le animazioni danno fastidio",
                        checked = reduceMotion,
                        onCheckedChange = { AppPreferences.setReduceMotion(it) }
                    )
                    OptionRow(
                        label = "Nero puro",
                        description = "Con il tema scuro usa sfondi neri: risparmia batteria sugli schermi OLED",
                        checked = pureBlack,
                        onCheckedChange = { AppPreferences.setPureBlack(it) }
                    )
                    OptionRow(
                        label = "Vibrazione",
                        description = "Leggera risposta tattile su tocchi e conversioni completate",
                        checked = haptics,
                        onCheckedChange = { AppPreferences.setHaptics(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
            SectionLabel("Conversione")
            Spacer(modifier = Modifier.height(10.dp))
            SettingsGroup {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Column {
                        Text("Qualità predefinita", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OmniText)
                        Text("Per i nuovi PDF creati da immagini", fontSize = 13.sp, color = OmniTextMuted)
                        Spacer(modifier = Modifier.height(10.dp))
                        SegmentedControl(
                            options = PdfQuality.entries.map { it.label },
                            selectedIndex = defaultQuality.ordinal,
                            onOptionSelected = { AppPreferences.setDefaultQuality(PdfQuality.entries[it]) }
                        )
                    }
                    Column {
                        Text("Formato pagina", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OmniText)
                        Text("Per Immagini in PDF", fontSize = 13.sp, color = OmniTextMuted)
                        Spacer(modifier = Modifier.height(10.dp))
                        val sizes = listOf("A4", "Letter", "Original")
                        SegmentedControl(
                            options = listOf("A4", "Lettera", "Originale"),
                            selectedIndex = sizes.indexOf(defaultPageSize).coerceAtLeast(0),
                            onOptionSelected = { AppPreferences.setDefaultPageSize(sizes[it]) }
                        )
                    }
                    Column {
                        OptionRow(
                            label = "Contrasto e pulizia automatici",
                            description = "Migliora le scansioni",
                            checked = autoClean,
                            onCheckedChange = { AppPreferences.setAutoClean(it) }
                        )
                        OptionRow(
                            label = "Rimuovi metadati",
                            description = "Autore, parole chiave e date personalizzate restano fuori dal PDF",
                            checked = stripMetadata,
                            onCheckedChange = { AppPreferences.setStripMetadata(it) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
            SectionLabel("Generale")
            Spacer(modifier = Modifier.height(10.dp))
            SettingsGroup {
                SettingsRow(
                    icon = Icons.Default.AutoAwesome,
                    tint = TintAi,
                    title = "Gemini e chiave API",
                    subtitle = "Modelli, quota giornaliera e chiave di Google AI Studio",
                    onClick = onOpenGemini
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Default.TipsAndUpdates,
                    tint = TintPdf,
                    title = "Guida rapida",
                    subtitle = "Rivedi il tour delle funzioni principali",
                    onClick = onOpenGuide
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Default.Folder,
                    tint = TintZip,
                    title = "Cartella di salvataggio",
                    subtitle = "Download/Morph · PDFs, Images, Audio, Archives, Markdown",
                    onClick = null
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
            SectionLabel("Dati")
            Spacer(modifier = Modifier.height(10.dp))
            SettingsGroup {
                SettingsRow(
                    icon = Icons.Default.DeleteSweep,
                    tint = OmniDanger,
                    title = "Svuota cronologia",
                    subtitle = if (historyCount == 0) "La cronologia è vuota" else "$historyCount elementi · i file restano in Download",
                    onClick = if (historyCount == 0) null else ({ HistoryRepository.clear() })
                )
                SettingsDivider()
                Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                    OptionRow(
                        label = "Pulizia automatica",
                        description = "All'avvio toglie i file temporanei più vecchi di un giorno",
                        checked = cacheCleanup,
                        onCheckedChange = { AppPreferences.setCacheCleanup(it) }
                    )
                }
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Default.CleaningServices,
                    tint = TintZip,
                    title = "Svuota cache",
                    subtitle = when {
                        cacheBytes < 0 -> "Calcolo…"
                        cacheBytes == 0L -> "Nessun file temporaneo"
                        else -> "${formatBytes(cacheBytes)} di foto scattate e file temporanei"
                    },
                    onClick = if (cacheBytes <= 0) null else ({
                        scope.launch {
                            withContext(Dispatchers.IO) { context.cacheDir.listFiles()?.forEach { it.deleteRecursively() } }
                            cacheBytes = withContext(Dispatchers.IO) { dirSize(context.cacheDir) }
                            Toast.makeText(context, "Cache svuotata", Toast.LENGTH_SHORT).show()
                        }
                    })
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            SettingsGroup {
                SettingsRow(
                    icon = Icons.Default.SettingsBackupRestore,
                    tint = OmniDanger,
                    title = "Ripristina impostazioni",
                    subtitle = "Tema, colori e opzioni tornano come all'inizio; preferiti e cronologia restano",
                    onClick = { confirmReset = true }
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
            SectionLabel("Info")
            Spacer(modifier = Modifier.height(10.dp))
            SettingsGroup {
                SettingsRow(
                    icon = Icons.Default.Celebration,
                    tint = OmniAccent,
                    title = "Novità di questa versione",
                    subtitle = "Cosa è cambiato in Morph ${BuildConfig.VERSION_NAME}",
                    onClick = onOpenWhatsNew
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Default.Info,
                    tint = OmniAccent,
                    title = "Morph",
                    subtitle = "Versione ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) · conversioni 100% sul dispositivo",
                    onClick = null
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Default.Person,
                    tint = OmniAccent,
                    title = "Autore",
                    subtitle = "Alessio Delfino",
                    onClick = null
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            containerColor = OmniSurface,
            title = { Text("Ripristinare le impostazioni?", color = OmniText) },
            text = { Text("Tema, colore, animazioni e opzioni di conversione tornano ai valori iniziali.", color = OmniTextMuted) },
            confirmButton = {
                TextButton(onClick = {
                    AppPreferences.resetToDefaults()
                    confirmReset = false
                    Toast.makeText(context, "Impostazioni ripristinate", Toast.LENGTH_SHORT).show()
                }) { Text("Ripristina", color = OmniDanger) }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text("Annulla", color = OmniTextMuted) }
            }
        )
    }
}

/** "Follow the wallpaper" swatch: a soft rainbow that turns into a tick when chosen. */
@Composable
private fun DynamicSwatch(selected: Boolean, onClick: () -> Unit) {
    val ring by animateColorAsState(if (selected) OmniText else Color.Transparent, label = "dynamic_ring")
    val scale by animateFloatAsState(if (selected) 1f else 0.9f, motionSpec(Motion.bouncy()), label = "dynamic_scale")
    val reduce = LocalReduceMotion.current
    val turn = rememberInfiniteTransition(label = "dynamic_turn")
    val angle by turn.animateFloat(0f, if (reduce || !selected) 0f else 360f, infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "dynamic_angle")
    Box(
        modifier = Modifier
            .size(46.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .border(2.dp, ring, CircleShape)
            .padding(5.dp)
            .clip(CircleShape)
            .graphicsLayer { rotationZ = angle }
            .background(Brush.sweepGradient(DynamicSwatchColors))
            .clickable(onClickLabel = "Colore dallo sfondo", role = Role.RadioButton, onClick = onClick)
            .semantics { this.selected = selected; contentDescription = "Colore dallo sfondo" },
        contentAlignment = Alignment.Center
    ) {
        if (selected) Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun AccentSwatch(choice: AccentChoice, selected: Boolean, onClick: () -> Unit) {
    val dark = LocalMorphColors.current.isDark
    val color = if (dark) choice.dark else choice.light
    val ring by animateColorAsState(if (selected) OmniText else Color.Transparent, label = "swatch_ring")
    val scale by animateFloatAsState(if (selected) 1f else 0.9f, motionSpec(Motion.bouncy()), label = "swatch_scale")
    Box(
        modifier = Modifier
            .size(46.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .border(2.dp, ring, CircleShape)
            .padding(5.dp)
            .clip(CircleShape)
            .background(color)
            .clickable(onClickLabel = choice.label, role = Role.RadioButton, onClick = onClick)
            .semantics { this.selected = selected; contentDescription = choice.label }
            .testTag("accent_${choice.name.lowercase()}"),
        contentAlignment = Alignment.Center
    ) {
        if (selected) Icon(Icons.Default.Check, contentDescription = null, tint = if (dark) choice.darkOn else Color.White, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun ThemeOptionCard(
    mode: ThemeMode,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = when (mode) {
        ThemeMode.SYSTEM -> Icons.Default.PhoneAndroid
        ThemeMode.LIGHT -> Icons.Default.LightMode
        ThemeMode.DARK -> Icons.Default.DarkMode
    }
    val interaction = remember { MutableInteractionSource() }
    val borderColor by animateColorAsState(if (selected) OmniAccent else OmniBorder, label = "theme_border")
    val lift by animateFloatAsState(if (selected) 1.03f else 1f, motionSpec(Motion.bouncy()), label = "theme_lift")
    val hapticsFn = rememberHaptics()
    Surface(
        onClick = {
            if (!selected) hapticsFn(Haptic.TICK)
            onClick()
        },
        interactionSource = interaction,
        shape = RoundedCornerShape(RadiusCard),
        color = OmniSurface,
        border = BorderStroke(if (selected) 2.dp else 1.dp, borderColor),
        modifier = modifier
            .graphicsLayer {
                scaleX = lift
                scaleY = lift
            }
            .pressScale(interaction, 0.95f)
            .testTag("theme_option_${mode.name.lowercase()}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            ThemePreview(mode)
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = if (selected) OmniAccent else OmniTextMuted, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = mode.label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = OmniText,
                    modifier = Modifier.weight(1f)
                )
                AnimatedVisibility(
                    visible = selected,
                    enter = scaleIn(motionSpec(Motion.bouncy())) + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(OmniAccent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = OmniOnAccent, modifier = Modifier.size(12.dp))
                    }
                }
            }
        }
    }
}

/** Miniature of the Morph home in the given theme (system = half light / half dark). */
@Composable
private fun ThemePreview(mode: ThemeMode) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(shape)
            .border(1.dp, OmniBorder, shape)
    ) {
        val palettes = when (mode) {
            ThemeMode.SYSTEM -> listOf(MorphLightColors, MorphDarkColors)
            ThemeMode.LIGHT -> listOf(MorphLightColors)
            ThemeMode.DARK -> listOf(MorphDarkColors)
        }
        palettes.forEach { colors ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .background(colors.bg)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    Modifier
                        .size(width = 28.dp, height = 6.dp)
                        .clip(CircleShape)
                        .background(colors.text)
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(colors.accentSoft)
                ) {
                    Box(
                        Modifier
                            .padding(4.dp)
                            .size(width = 20.dp, height = 8.dp)
                            .clip(CircleShape)
                            .background(colors.accent)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(2) {
                        Box(
                            Modifier
                                .weight(1f)
                                .height(12.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(colors.surface)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(RadiusCard),
        color = OmniSurface,
        border = BorderStroke(1.dp, OmniBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column { content() }
    }
}

@Composable
private fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 72.dp)
            .height(1.dp)
            .background(OmniBorder)
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)?
) {
    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconTile(icon = icon, tint = tint, size = 42.dp)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = OmniText)
                Text(subtitle, fontSize = 13.sp, color = OmniTextMuted)
            }
            if (onClick != null) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = OmniTextMuted)
            }
        }
    }
    if (onClick != null) {
        val interaction = remember { MutableInteractionSource() }
        Surface(
            onClick = onClick,
            interactionSource = interaction,
            color = Color.Transparent,
            shape = RoundedCornerShape(RadiusMd),
            modifier = Modifier.pressScale(interaction, 0.98f)
        ) { content() }
    } else {
        content()
    }
}

private fun dirSize(dir: java.io.File): Long =
    dir.walkBottomUp().filter { it.isFile }.sumOf { it.length() }

/** Three animated counters computed from the history: files made, total size, favourite tool. */
@Composable
private fun UsageStats(entries: List<com.example.data.HistoryEntry>) {
    val weekAgo = remember { System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000 }
    val thisWeek = entries.count { it.timestamp >= weekAgo }
    val totalBytes = entries.sumOf { it.sizeBytes.coerceAtLeast(0L) }
    val topTool = entries.groupingBy { it.toolLabel }.eachCount().maxByOrNull { it.value }?.key ?: "—"

    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val files by androidx.compose.animation.core.animateIntAsState(
        if (started) entries.size else 0,
        animationSpec = com.example.ui.theme.motionSpec(androidx.compose.animation.core.tween(900, easing = com.example.ui.theme.Motion.EmphasizedDecelerate)),
        label = "stats_files"
    )
    val week by androidx.compose.animation.core.animateIntAsState(
        if (started) thisWeek else 0,
        animationSpec = com.example.ui.theme.motionSpec(androidx.compose.animation.core.tween(900, delayMillis = 120, easing = com.example.ui.theme.Motion.EmphasizedDecelerate)),
        label = "stats_week"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatTile(value = "$files", label = if (files == 1) "file creato" else "file creati", modifier = Modifier.weight(1f))
        StatTile(value = "$week", label = "questa settimana", modifier = Modifier.weight(1f))
        StatTile(value = formatBytes(totalBytes), label = "in totale", modifier = Modifier.weight(1f))
    }
    Spacer(modifier = Modifier.height(8.dp))
    AnimatedText("Strumento preferito: $topTool", fontSize = 13.sp, color = OmniTextMuted)
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(RadiusMd))
            .background(OmniAccentSoft)
            .padding(horizontal = 12.dp, vertical = 14.dp)
    ) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = OmniAccent, maxLines = 1)
        Text(label, fontSize = 12.sp, color = OmniTextMuted, maxLines = 1)
    }
}
