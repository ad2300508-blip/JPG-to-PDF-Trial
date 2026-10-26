package com.example.ui.home

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import com.example.ui.components.MorphBadge
import com.example.ui.components.formatBytes
import com.example.ui.components.formatRelativeTime
import com.example.ui.components.icon
import com.example.ui.components.tint
import com.example.ui.theme.Haptic
import com.example.ui.theme.LocalReduceMotion
import com.example.ui.theme.enterAnimation
import com.example.ui.theme.pressScale
import com.example.ui.theme.rememberHaptics
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HistoryEntry
import com.example.ui.components.HistoryRow
import com.example.ui.components.IconTile
import com.example.ui.components.MorphChipRow
import com.example.ui.components.mimeForKind
import com.example.ui.components.openFile
import com.example.ui.components.rememberCameraCapture
import com.example.ui.components.shareFile
import com.example.ui.theme.OmniAccent
import com.example.ui.theme.OmniAccentSoft
import com.example.ui.theme.OmniBorder
import com.example.ui.theme.OmniOnAccent
import com.example.ui.theme.OmniSurface
import com.example.ui.theme.OmniText
import com.example.ui.theme.OmniTextMuted
import com.example.ui.theme.RadiusCard
import com.example.ui.theme.RadiusMd
import com.example.ui.theme.RadiusPill

private const val ALL_FILTER = "Tutti"

@Composable
fun HomeScreen(
    recents: List<HistoryEntry>,
    onOpenTool: (OmniTool) -> Unit,
    onImagesForPdf: (List<Uri>) -> Unit,
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(ALL_FILTER) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) onImagesForPdf(uris)
    }
    val launchCamera = rememberCameraCapture { uri -> onImagesForPdf(listOf(uri)) }

    val filters = listOf(ALL_FILTER) + ToolCategory.entries.map { it.label }
    val normalizedQuery = query.trim().lowercase()
    val tools = OmniTool.entries.filter { tool ->
        (filter == ALL_FILTER || tool.categories.any { it.label == filter }) &&
            (normalizedQuery.isEmpty() ||
                tool.title.lowercase().contains(normalizedQuery) ||
                tool.keywords.contains(normalizedQuery))
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = maxWidth >= 840.dp
        val isTablet = maxWidth >= 600.dp

        Row(modifier = Modifier.fillMaxSize()) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = if (isTablet) 180.dp else 150.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .testTag("home_tools_grid"),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column {
                        if (!isTablet) {
                            BrandBar(onOpenHistory = onOpenHistory)
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                        if (isTablet) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    HomeTitle()
                                }
                                Spacer(modifier = Modifier.width(20.dp))
                                SearchField(query, { query = it }, Modifier.width(300.dp))
                            }
                        } else {
                            HomeTitle()
                            Spacer(modifier = Modifier.height(20.dp))
                            SearchField(query, { query = it }, Modifier.fillMaxWidth())
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        AnimatedVisibility(
                            visible = normalizedQuery.isEmpty(),
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column {
                                FeaturedHero(
                                    tool = OmniTool.IMAGES_TO_PDF,
                                    horizontal = isTablet,
                                    onChooseFiles = { imagePicker.launch("image/*") },
                                    onCamera = launchCamera,
                                    modifier = Modifier.enterAnimation(1)
                                )
                                if (!isWide && recents.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(24.dp))
                                    RecentsStrip(recents = recents, onOpenHistory = onOpenHistory)
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Strumenti",
                                style = MaterialTheme.typography.titleLarge,
                                color = OmniText,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${tools.size} strumenti",
                                fontSize = 14.sp,
                                color = OmniTextMuted
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        MorphChipRow(options = filters, selected = filter, onSelected = { filter = it })
                    }
                }

                itemsIndexed(tools, key = { _, tool -> tool.name }) { index, tool ->
                    ToolCard(
                        tool = tool,
                        highlighted = tool == OmniTool.IMAGES_TO_PDF,
                        onClick = { onOpenTool(tool) },
                        modifier = Modifier
                            .animateItem(placementSpec = spring(dampingRatio = 0.8f, stiffness = 400f))
                            .enterAnimation(index + 2, key = tool.name)
                    )
                }

                if (tools.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = "Nessuno strumento trovato per \"$query\"",
                            color = OmniTextMuted,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp)
                        )
                    }
                }
            }

            if (isWide) {
                RecentsPanel(
                    recents = recents,
                    onOpenHistory = onOpenHistory,
                    modifier = Modifier
                        .width(330.dp)
                        .fillMaxHeight()
                        .padding(top = 16.dp, end = 20.dp, bottom = 20.dp)
                )
            }
        }
    }
}

@Composable
private fun BrandBar(onOpenHistory: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        BrandLogo(size = 44)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "OmniConvert",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = OmniText,
            modifier = Modifier.weight(1f)
        )
        Surface(
            onClick = onOpenHistory,
            shape = CircleShape,
            color = OmniSurface,
            border = BorderStroke(1.dp, OmniBorder),
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.History, contentDescription = "Cronologia", tint = OmniText)
            }
        }
    }
}

@Composable
fun BrandLogo(size: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size * 0.3f).dp))
            .background(OmniAccent),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.SwapHoriz,
            contentDescription = "OmniConvert",
            tint = OmniOnAccent,
            modifier = Modifier.size((size * 0.58f).dp)
        )
    }
}

@Composable
private fun HomeTitle() {
    val hour = remember { java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) }
    val greeting = when (hour) {
        in 5..12 -> "Buongiorno"
        in 13..17 -> "Buon pomeriggio"
        else -> "Buonasera"
    }
    Text(
        text = greeting.uppercase(),
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = OmniAccent,
        modifier = Modifier.enterAnimation(0)
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = "Cosa vuoi convertire?",
        style = MaterialTheme.typography.displayLarge,
        color = OmniText
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = "Scegli uno strumento, poi i tuoi file.",
        fontSize = 16.sp,
        color = OmniTextMuted
    )
}

@Composable
private fun SearchField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(RadiusPill + 4.dp),
        color = OmniSurface,
        border = BorderStroke(1.dp, OmniBorder),
        modifier = modifier.height(54.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = OmniText, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text("Cerca uno strumento", color = OmniTextMuted, fontSize = 16.sp)
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(color = OmniText, fontSize = 16.sp),
                    cursorBrush = SolidColor(OmniAccent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_search_field")
                )
            }
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Cancella ricerca", tint = OmniTextMuted, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun FeaturedHero(
    tool: OmniTool,
    horizontal: Boolean,
    onChooseFiles: () -> Unit,
    onCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dash = OmniAccent.copy(alpha = 0.35f)
    val reduce = LocalReduceMotion.current
    val transition = rememberInfiniteTransition(label = "hero")
    val shift by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (reduce) 0f else 1f,
        animationSpec = infiniteRepeatable(tween(5000, easing = LinearEasing), RepeatMode.Reverse),
        label = "hero_gradient"
    )
    val soft = OmniAccentSoft
    val accent = OmniAccent
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RadiusCard + 8.dp))
            .drawBehind {
                drawRect(soft)
                // Slow drifting glow behind the hero.
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(accent.copy(alpha = 0.18f), Color.Transparent),
                        center = Offset(size.width * (0.2f + 0.6f * shift), size.height * 0.2f),
                        radius = size.maxDimension * 0.6f
                    ),
                    radius = size.maxDimension,
                    center = Offset(size.width * (0.2f + 0.6f * shift), size.height * 0.2f)
                )
            }
            .then(
                if (horizontal) Modifier.drawBehind {
                    drawRoundRect(
                        color = dash,
                        cornerRadius = CornerRadius(28.dp.toPx()),
                        style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
                    )
                } else Modifier
            )
            .padding(if (horizontal) 28.dp else 20.dp)
    ) {
        if (horizontal) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(icon = tool.icon, tint = OmniAccent, size = 76.dp, filled = true)
                Spacer(modifier = Modifier.width(24.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(tool.title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = OmniText)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("JPG, PNG o HEIC. Le unisci in un unico PDF.", fontSize = 15.sp, color = OmniTextMuted)
                }
                Spacer(modifier = Modifier.width(16.dp))
                CameraButton(onClick = onCamera, withLabel = true)
                Spacer(modifier = Modifier.width(10.dp))
                ChooseFilesButton(onClick = onChooseFiles, modifier = Modifier.height(54.dp))
            }
        } else {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconTile(icon = tool.icon, tint = OmniAccent, size = 60.dp, filled = true)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(tool.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = OmniText)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(tool.description, fontSize = 14.sp, color = OmniTextMuted)
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("JPG", "PNG", "HEIC").forEach { format ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(RadiusPill))
                                .background(OmniSurface)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(format, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OmniText)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ChooseFilesButton(
                        onClick = onChooseFiles,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    CameraButton(onClick = onCamera, withLabel = false)
                }
            }
        }
    }
}

@Composable
private fun ChooseFilesButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = OmniAccent, contentColor = OmniOnAccent),
        shape = RoundedCornerShape(RadiusMd),
        contentPadding = PaddingValues(horizontal = 24.dp),
        modifier = modifier.testTag("home_choose_files_button")
    ) {
        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text("Scegli file", fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CameraButton(onClick: () -> Unit, withLabel: Boolean) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(if (withLabel) RadiusPill + 4.dp else RadiusMd),
        color = OmniSurface,
        modifier = Modifier
            .height(if (withLabel) 54.dp else 56.dp)
            .then(if (withLabel) Modifier else Modifier.width(56.dp))
            .testTag("home_camera_button")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (withLabel) 20.dp else 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = "Fotocamera", tint = OmniAccent, modifier = Modifier.size(22.dp))
            if (withLabel) {
                Spacer(modifier = Modifier.width(8.dp))
                Text("Fotocamera", color = OmniAccent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ToolCard(tool: OmniTool, highlighted: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val haptics = rememberHaptics()
    Surface(
        onClick = {
            haptics(Haptic.TAP)
            onClick()
        },
        interactionSource = interaction,
        shape = RoundedCornerShape(RadiusCard),
        color = if (highlighted) OmniAccentSoft.copy(alpha = 0.55f) else OmniSurface,
        border = BorderStroke(if (highlighted) 1.5.dp else 1.dp, if (highlighted) OmniAccent else OmniBorder),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 150.dp)
            .pressScale(interaction, 0.95f)
            .testTag("tool_card_${tool.name.lowercase()}")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                IconTile(icon = tool.icon, tint = if (highlighted) OmniAccent else tool.tint, size = 48.dp, filled = highlighted)
                Spacer(modifier = Modifier.weight(1f))
                if (tool.isNew) MorphBadge("Nuovo")
            }
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = tool.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = OmniText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = tool.subtitle,
                fontSize = 13.sp,
                color = OmniTextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Phone-only horizontal strip with the last converted files. */
@Composable
private fun RecentsStrip(recents: List<HistoryEntry>, onOpenHistory: () -> Unit) {
    val context = LocalContext.current
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Recenti", style = MaterialTheme.typography.titleLarge, color = OmniText, modifier = Modifier.weight(1f))
            TextButton(onClick = onOpenHistory) {
                Text("Vedi tutti", color = OmniAccent, fontWeight = FontWeight.Bold)
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            itemsIndexed(recents.take(6), key = { _, e -> e.id }) { index, entry ->
                val interaction = remember { MutableInteractionSource() }
                Surface(
                    onClick = { openFile(context, Uri.parse(entry.uri), mimeForKind(entry.kind)) },
                    interactionSource = interaction,
                    shape = RoundedCornerShape(RadiusMd + 2.dp),
                    color = OmniSurface,
                    border = BorderStroke(1.dp, OmniBorder),
                    modifier = Modifier
                        .width(210.dp)
                        .enterAnimation(index, key = entry.id)
                        .pressScale(interaction)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconTile(icon = entry.kind.icon(), tint = entry.kind.tint(), size = 40.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(entry.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = OmniText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                listOf(formatBytes(entry.sizeBytes), formatRelativeTime(entry.timestamp)).filter { it.isNotBlank() }.joinToString(" · "),
                                fontSize = 12.sp,
                                color = OmniTextMuted,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentsPanel(
    recents: List<HistoryEntry>,
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Surface(
        shape = RoundedCornerShape(RadiusCard + 8.dp),
        color = OmniSurface,
        border = BorderStroke(1.dp, OmniBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Recenti", style = MaterialTheme.typography.titleLarge, color = OmniText)
            Spacer(modifier = Modifier.height(12.dp))
            if (recents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "I file che converti appariranno qui.",
                        color = OmniTextMuted,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(recents.take(8), key = { it.id }) { entry ->
                        HistoryRow(
                            modifier = Modifier.animateItem(),
                            entry = entry,
                            framed = false,
                            onOpen = { openFile(context, Uri.parse(entry.uri), mimeForKind(entry.kind)) },
                            onShare = { shareFile(context, Uri.parse(entry.uri), mimeForKind(entry.kind)) },
                            onDelete = null
                        )
                    }
                }
            }
            TextButton(onClick = onOpenHistory, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Vedi tutta la cronologia", color = OmniAccent, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}
