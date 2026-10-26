package com.example.ui.home

import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppPreferences
import com.example.data.HistoryEntry
import com.example.ui.components.HistoryRow
import com.example.ui.components.IconTile
import com.example.ui.components.MorphBadge
import com.example.ui.components.MorphChipRow
import com.example.ui.components.formatBytes
import com.example.ui.components.formatRelativeTime
import com.example.ui.components.icon
import com.example.ui.components.mimeForKind
import com.example.ui.components.openFile
import com.example.ui.components.rememberCameraCapture
import com.example.ui.components.rememberOrderedImagePicker
import com.example.ui.components.shareFile
import com.example.ui.components.tint
import com.example.ui.theme.Haptic
import com.example.ui.theme.LocalReduceMotion
import com.example.ui.theme.Motion
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
import com.example.ui.theme.enterAnimation
import com.example.ui.theme.motionSpec
import com.example.ui.theme.pressScale
import com.example.ui.theme.rememberHaptics
import com.example.util.SearchText
import kotlinx.coroutines.launch

private const val ALL_FILTER = "Tutti"
private const val FAVORITES_FILTER = "Preferiti"

/** A titled block of tool cards on the home grid. */
private data class HomeSection(val id: String, val title: String, val tools: List<OmniTool>)

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
    var peekToolName by rememberSaveable { mutableStateOf<String?>(null) }

    // Photo picker: pages follow the order in which the photos are tapped.
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        if (uris.isNotEmpty()) onImagesForPdf(uris)
    }
    val launchCamera = rememberCameraCapture { uri -> onImagesForPdf(listOf(uri)) }
    val orderedPicker = rememberOrderedImagePicker(
        onPicked = onImagesForPdf,
        fallback = { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    )

    // Voice search, offered only when the phone has a speech recogniser.
    val context = LocalContext.current
    val voiceIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "it-IT")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Che strumento cerchi?")
        }
    }
    val canUseVoice = remember { voiceIntent.resolveActivity(context.packageManager) != null }
    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { query = it }
    }

    val onVoice: (() -> Unit)? = if (canUseVoice) {
        {
            try {
                voiceLauncher.launch(voiceIntent)
            } catch (_: android.content.ActivityNotFoundException) {
            }
        }
    } else {
        null
    }

    val favorites by AppPreferences.favoriteTools.collectAsState()
    val haptics = rememberHaptics()
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()

    if (filter == FAVORITES_FILTER && favorites.isEmpty()) filter = ALL_FILTER
    val trimmedQuery = query.trim()
    val searching = trimmedQuery.isNotEmpty()

    val chips = remember(favorites.isEmpty()) {
        buildList {
            add(ALL_FILTER)
            if (favorites.isNotEmpty()) add(FAVORITES_FILTER)
            addAll(ToolGroup.entries.map { it.chip })
        }
    }
    val chipCounts = remember(favorites) {
        buildMap {
            put(ALL_FILTER, OmniTool.entries.size)
            put(FAVORITES_FILTER, favorites.size)
            ToolGroup.entries.forEach { group -> put(group.chip, OmniTool.entries.count { it.group == group }) }
        }
    }
    val matches = remember(trimmedQuery) {
        val folded = SearchText.fold(trimmedQuery)
        OmniTool.entries.filter { SearchText.matches(it.searchIndex, folded) }
    }
    val recentTools = remember(recents) {
        recents.sortedByDescending { it.timestamp }.mapNotNull { toolForHistoryLabel(it.toolLabel) }.distinct().take(5)
    }
    val sections = remember(searching, matches, filter, favorites) {
        val favoriteTools = OmniTool.entries.filter { it.name in favorites }
        fun groupSection(group: ToolGroup) = HomeSection(group.name, group.label, OmniTool.entries.filter { it.group == group })
        when {
            searching -> listOf(HomeSection("results", if (matches.size == 1) "1 risultato" else "${matches.size} risultati", matches))
            filter == FAVORITES_FILTER -> listOf(HomeSection("favorites", "Preferiti", favoriteTools))
            filter == ALL_FILTER -> buildList {
                if (favoriteTools.isNotEmpty()) add(HomeSection("favorites", "Preferiti", favoriteTools))
                ToolGroup.entries.forEach { add(groupSection(it)) }
            }
            else -> ToolGroup.entries.filter { it.chip == filter }.map { groupSection(it) }
        }.filter { it.tools.isNotEmpty() || it.id == "results" }
    }
    val showScrollTop by remember { derivedStateOf { gridState.firstVisibleItemIndex > 3 } }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = maxWidth >= 840.dp
        val isTablet = maxWidth >= 600.dp

        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Adaptive(minSize = if (isTablet) 180.dp else 150.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("home_tools_grid"),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 88.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item(key = "home_header", span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
                        Column {
                            if (!isTablet) {
                                BrandBar(onOpenHistory = onOpenHistory)
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                            val search: @Composable (Modifier) -> Unit = { m ->
                                SearchField(
                                    value = query,
                                    onValueChange = { query = it },
                                    onVoice = onVoice,
                                    modifier = m
                                )
                            }
                            if (isTablet) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) { HomeTitle() }
                                    Spacer(modifier = Modifier.width(20.dp))
                                    search(Modifier.width(300.dp))
                                }
                            } else {
                                HomeTitle()
                                Spacer(modifier = Modifier.height(20.dp))
                                search(Modifier.fillMaxWidth())
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            AnimatedVisibility(
                                visible = !searching,
                                enter = expandVertically(motionSpec(Motion.gentle())) + fadeIn(),
                                exit = shrinkVertically(tween(Motion.MEDIUM)) + fadeOut(tween(Motion.SHORT))
                            ) {
                                Column {
                                    FeaturedHero(
                                        tool = OmniTool.IMAGES_TO_PDF,
                                        horizontal = isTablet,
                                        onChooseFiles = orderedPicker,
                                        onCamera = launchCamera,
                                        modifier = Modifier.enterAnimation(1)
                                    )
                                    if (recentTools.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(24.dp))
                                        RecentToolsRow(tools = recentTools, onOpen = onOpenTool)
                                    }
                                    if (!isWide && recents.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(24.dp))
                                        RecentsStrip(recents = recents, onOpenHistory = onOpenHistory)
                                    }
                                    Spacer(modifier = Modifier.height(24.dp))
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AnimatedContent(
                                    targetState = searching,
                                    transitionSpec = { fadeIn(tween(Motion.MEDIUM)) togetherWith fadeOut(tween(Motion.SHORT)) },
                                    modifier = Modifier.weight(1f),
                                    label = "home_title"
                                ) { isSearching ->
                                    Text(
                                        text = if (isSearching) "Risultati" else "Strumenti",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = OmniText
                                    )
                                }
                                Text(
                                    text = "${OmniTool.entries.size} strumenti",
                                    fontSize = 14.sp,
                                    color = OmniTextMuted
                                )
                            }
                            AnimatedVisibility(
                                visible = !searching,
                                enter = expandVertically(motionSpec(Motion.gentle())) + fadeIn(),
                                exit = shrinkVertically(tween(Motion.MEDIUM)) + fadeOut(tween(Motion.SHORT))
                            ) {
                                Column {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    MorphChipRow(
                                        options = chips,
                                        selected = filter,
                                        onSelected = { filter = it },
                                        counts = chipCounts
                                    )
                                    AnimatedVisibility(
                                        visible = favorites.isEmpty(),
                                        enter = expandVertically() + fadeIn(),
                                        exit = shrinkVertically() + fadeOut()
                                    ) {
                                        Text(
                                            text = "Tieni premuto uno strumento per vederne i dettagli e fissarlo tra i preferiti",
                                            fontSize = 13.sp,
                                            color = OmniTextMuted,
                                            modifier = Modifier.padding(top = 10.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    sections.forEach { section ->
                        if (!searching) {
                            item(key = "section_${section.id}", span = { GridItemSpan(maxLineSpan) }, contentType = "section") {
                                SectionHeader(
                                    title = section.title,
                                    count = section.tools.size,
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }
                        itemsIndexed(
                            items = section.tools,
                            key = { _, tool -> "${section.id}_${tool.name}" },
                            contentType = { _, _ -> "tool" }
                        ) { index, tool ->
                            ToolCard(
                                tool = tool,
                                highlighted = tool == OmniTool.IMAGES_TO_PDF && !searching,
                                isFavorite = tool.name in favorites,
                                query = if (searching) trimmedQuery else "",
                                onClick = { onOpenTool(tool) },
                                onLongClick = {
                                    haptics(Haptic.CONFIRM)
                                    peekToolName = tool.name
                                },
                                modifier = Modifier
                                    .animateItem(placementSpec = spring(dampingRatio = 0.8f, stiffness = 400f))
                                    .enterAnimation(index + 2, key = "${section.id}_${tool.name}")
                            )
                        }
                    }

                    if (searching && matches.isEmpty()) {
                        item(key = "no_results", span = { GridItemSpan(maxLineSpan) }, contentType = "empty") {
                            NoResults(
                                query = trimmedQuery,
                                onSuggestion = { query = it },
                                onShowAll = { query = "" },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = showScrollTop,
                    enter = scaleIn(motionSpec(Motion.bouncy())) + fadeIn(),
                    exit = scaleOut(tween(Motion.SHORT)) + fadeOut(tween(Motion.SHORT)),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(20.dp)
                ) {
                    ScrollTopButton {
                        haptics(Haptic.TICK)
                        scope.launch { gridState.animateScrollToItem(0) }
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

    val peekTool = peekToolName?.let { name -> OmniTool.entries.firstOrNull { it.name == name } }
    if (peekTool != null) {
        ToolPeekSheet(
            tool = peekTool,
            isFavorite = peekTool.name in favorites,
            onOpen = {
                peekToolName = null
                onOpenTool(peekTool)
            },
            onToggleFavorite = {
                haptics(Haptic.CONFIRM)
                AppPreferences.toggleFavorite(peekTool.name)
            },
            onDismiss = { peekToolName = null }
        )
    }
}

/** Accent bar + section title + tool count. */
@Composable
private fun SectionHeader(title: String, count: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = 18.dp)
                .clip(CircleShape)
                .background(OmniAccent)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = OmniText,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = if (count == 1) "1 strumento" else "$count strumenti",
            fontSize = 13.sp,
            color = OmniTextMuted
        )
    }
}

/** Shortcuts to the tools used last (from the history), most recent first. */
@Composable
private fun RecentToolsRow(tools: List<OmniTool>, onOpen: (OmniTool) -> Unit) {
    Column {
        Text("Usati di recente", style = MaterialTheme.typography.titleLarge, color = OmniText)
        Spacer(modifier = Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(tools, key = { _, tool -> tool.name }) { index, tool ->
                val interaction = remember { MutableInteractionSource() }
                Surface(
                    onClick = { onOpen(tool) },
                    interactionSource = interaction,
                    shape = RoundedCornerShape(RadiusPill),
                    color = OmniSurface,
                    border = BorderStroke(1.dp, OmniBorder),
                    modifier = Modifier
                        .height(44.dp)
                        .animateItem()
                        .enterAnimation(index, key = "recent_tool_${tool.name}")
                        .pressScale(interaction, 0.94f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(tool.icon, contentDescription = null, tint = tool.tint, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(tool.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = OmniText, maxLines = 1)
                    }
                }
            }
        }
    }
}

/** Shown when the search finds nothing: a gently wobbling icon and a few searches to try. */
@Composable
private fun NoResults(query: String, onSuggestion: (String) -> Unit, onShowAll: () -> Unit, modifier: Modifier = Modifier) {
    val reduce = LocalReduceMotion.current
    val transition = rememberInfiniteTransition(label = "no_results")
    val angle by transition.animateFloat(
        initialValue = if (reduce) 0f else -10f,
        targetValue = if (reduce) 0f else 10f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "no_results_wobble"
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(OmniAccentSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.SearchOff,
                contentDescription = null,
                tint = OmniAccent,
                modifier = Modifier
                    .size(34.dp)
                    .graphicsLayer { rotationZ = angle }
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Nessuno strumento per \"$query\"",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = OmniText,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text("Prova con una di queste parole:", fontSize = 14.sp, color = OmniTextMuted)
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("pdf", "foto", "firma", "zip", "audio").forEachIndexed { index, word ->
                val interaction = remember { MutableInteractionSource() }
                Surface(
                    onClick = { onSuggestion(word) },
                    interactionSource = interaction,
                    shape = RoundedCornerShape(RadiusPill),
                    color = OmniSurface,
                    border = BorderStroke(1.dp, OmniBorder),
                    modifier = Modifier
                        .enterAnimation(index, key = "suggest_$word")
                        .pressScale(interaction, 0.92f)
                ) {
                    Text(word, fontSize = 14.sp, color = OmniText, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                }
            }
        }
        TextButton(onClick = onShowAll, modifier = Modifier.padding(top = 6.dp)) {
            Text("Mostra tutti gli strumenti", color = OmniAccent, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ScrollTopButton(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        interactionSource = interaction,
        shape = CircleShape,
        color = OmniAccent,
        shadowElevation = 6.dp,
        modifier = Modifier
            .size(52.dp)
            .pressScale(interaction, 0.9f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Torna su", tint = OmniOnAccent, modifier = Modifier.size(28.dp))
        }
    }
}

/** Long-press preview: what the tool does, which files it takes, open it or pin it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToolPeekSheet(
    tool: OmniTool,
    isFavorite: Boolean,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val reduce = LocalReduceMotion.current
    val pop = remember { Animatable(if (reduce) 1f else 0.6f) }
    LaunchedEffect(tool) { if (!reduce) pop.animateTo(1f, Motion.bouncy()) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = OmniSurface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(
                    icon = tool.icon,
                    tint = tool.tint,
                    size = 64.dp,
                    filled = true,
                    modifier = Modifier.graphicsLayer {
                        scaleX = pop.value
                        scaleY = pop.value
                    }
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    MorphBadge(tool.group.label)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(tool.title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = OmniText)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(tool.description, fontSize = 16.sp, color = OmniText)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Accetta: ${acceptedFiles(tool)}", fontSize = 14.sp, color = OmniTextMuted)
            Spacer(modifier = Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onOpen,
                    colors = ButtonDefaults.buttonColors(containerColor = OmniAccent, contentColor = OmniOnAccent),
                    shape = RoundedCornerShape(RadiusMd),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) {
                    Text("Apri", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onToggleFavorite,
                    shape = RoundedCornerShape(RadiusMd),
                    border = BorderStroke(1.dp, OmniBorder),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) {
                    AnimatedContent(
                        targetState = isFavorite,
                        transitionSpec = { (scaleIn(motionSpec(Motion.bouncy())) + fadeIn()) togetherWith (scaleOut() + fadeOut()) },
                        label = "peek_star"
                    ) { fav ->
                        Icon(
                            if (fav) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            tint = OmniAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isFavorite) "Nei preferiti" else "Preferito", color = OmniText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

private fun acceptedFiles(tool: OmniTool): String {
    val kinds = tool.accepts.map { prefix ->
        when {
            prefix.isEmpty() -> "qualsiasi file"
            prefix == "application/pdf" -> "PDF"
            prefix.startsWith("image/") -> "immagini"
            prefix.startsWith("audio/") -> "audio"
            prefix.startsWith("video/") -> "video"
            prefix.startsWith("text/") -> "testo"
            prefix.contains("zip") -> "ZIP"
            prefix.contains("wordprocessingml") -> "Word"
            else -> prefix
        }
    }.distinct()
    return if (kinds.isEmpty()) "file creati nell'app" else kinds.joinToString(", ")
}

/** [text] with the parts matching [query] in accent colour on a soft background. */
private fun highlightedTitle(text: String, query: String, color: Color, background: Color): AnnotatedString {
    val ranges = SearchText.highlights(text, query)
    if (ranges.isEmpty()) return AnnotatedString(text)
    return buildAnnotatedString {
        append(text)
        ranges.forEach { r -> addStyle(SpanStyle(color = color, background = background), r.first, r.last + 1) }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun ToolCard(
    tool: OmniTool,
    highlighted: Boolean,
    isFavorite: Boolean,
    query: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val haptics = rememberHaptics()
    // The icon hops and tilts while the card is pressed.
    val pressed by interaction.collectIsPressedAsState()
    val iconScale by animateFloatAsState(if (pressed) 1.14f else 1f, motionSpec(Motion.bouncy()), label = "tool_icon_scale")
    val iconTilt by animateFloatAsState(if (pressed) -8f else 0f, motionSpec(Motion.bouncy()), label = "tool_icon_tilt")
    val accent = OmniAccent
    val accentSoft = OmniAccentSoft
    val title = remember(tool.title, query, accent, accentSoft) { highlightedTitle(tool.title, query, accent, accentSoft) }
    Surface(
        shape = RoundedCornerShape(RadiusCard),
        color = if (highlighted) OmniAccentSoft.copy(alpha = 0.55f) else OmniSurface,
        border = BorderStroke(if (highlighted) 1.5.dp else 1.dp, if (highlighted) OmniAccent else OmniBorder),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 150.dp)
            .pressScale(interaction, 0.95f)
            .clip(RoundedCornerShape(RadiusCard))
            .combinedClickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                onClickLabel = "Apri ${tool.title}",
                onLongClickLabel = "Dettagli e preferiti",
                onLongClick = onLongClick,
                onClick = {
                    haptics(Haptic.TAP)
                    onClick()
                }
            )
            .testTag("tool_card_${tool.name.lowercase()}")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                IconTile(
                    icon = tool.icon,
                    tint = if (highlighted) OmniAccent else tool.tint,
                    size = 48.dp,
                    filled = highlighted,
                    modifier = Modifier.graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                        rotationZ = iconTilt
                    }
                )
                Spacer(modifier = Modifier.weight(1f))
                AnimatedVisibility(
                    visible = isFavorite,
                    enter = scaleIn(motionSpec(Motion.bouncy())) + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Icon(Icons.Default.Star, contentDescription = "Preferito", tint = OmniAccent, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = title,
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


@Composable
private fun BrandBar(onOpenHistory: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        BrandLogo(size = 44)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "Morph",
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
            contentDescription = "Morph",
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
private fun SearchField(value: String, onValueChange: (String) -> Unit, onVoice: (() -> Unit)?, modifier: Modifier = Modifier) {
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
            AnimatedContent(
                targetState = value.isNotEmpty(),
                transitionSpec = { (scaleIn(motionSpec(Motion.bouncy())) + fadeIn()) togetherWith (scaleOut() + fadeOut()) },
                label = "search_trailing"
            ) { hasText ->
                when {
                    hasText -> IconButton(onClick = { onValueChange("") }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Cancella ricerca", tint = OmniTextMuted, modifier = Modifier.size(18.dp))
                    }
                    onVoice != null -> IconButton(onClick = onVoice, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.Mic, contentDescription = "Cerca con la voce", tint = OmniAccent, modifier = Modifier.size(20.dp))
                    }
                    else -> Spacer(modifier = Modifier.size(40.dp))
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
