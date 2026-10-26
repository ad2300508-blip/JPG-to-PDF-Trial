@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package com.example

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.IconButton
import com.example.data.AppPreferences
import com.example.data.ThemeMode
import com.example.ui.theme.LocalMorphColors
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.HistoryRepository
import com.example.ui.ArchiveScreen
import com.example.ui.AudioLabScreen
import com.example.ui.GeminiHubScreen
import com.example.ui.HistoryScreen
import com.example.ui.CollageScreen
import com.example.ui.ImageLabScreen
import com.example.ui.MarkdownExporterScreen
import com.example.ui.PdfCompressScreen
import com.example.ui.PdfMergeScreen
import com.example.ui.PdfOrganizeScreen
import com.example.ui.PdfSecureScreen
import com.example.ui.PdfStampScreen
import com.example.ui.PdfSplitScreen
import com.example.ui.PdfStudioScreen
import com.example.ui.PdfToImagesScreen
import com.example.ui.SettingsScreen
import com.example.ui.components.IconTile
import com.example.ui.components.LocalToolBack
import com.example.ui.components.OmniTutorialDialog
import com.example.ui.home.BrandLogo
import com.example.ui.home.HomeScreen
import com.example.ui.home.OmniTool
import com.example.ui.home.ToolScreen
import com.example.ui.home.toolsFor
import com.example.ui.theme.Haptic
import com.example.ui.theme.LocalReduceMotion
import com.example.ui.theme.Motion
import com.example.ui.theme.OmniAccent
import com.example.ui.theme.OmniAccentSoft
import com.example.ui.theme.OmniBg
import com.example.ui.theme.OmniBorder
import com.example.ui.theme.OmniSurface
import com.example.ui.theme.OmniText
import com.example.ui.theme.OmniTextMuted
import com.example.ui.theme.RadiusMd
import com.example.ui.theme.enterAnimation
import com.example.ui.theme.motionSpec
import com.example.ui.theme.pressScale
import com.example.ui.theme.rememberHaptics
import com.example.viewmodel.ArchiveMode
import com.example.viewmodel.ArchiveViewModel
import com.example.viewmodel.AudioLabViewModel
import com.example.viewmodel.GeminiHubViewModel
import com.example.viewmodel.CollageViewModel
import com.example.viewmodel.ImageLabViewModel
import com.example.viewmodel.MarkdownExporterViewModel
import com.example.viewmodel.PdfCompressViewModel
import com.example.viewmodel.PdfMergeViewModel
import com.example.viewmodel.PdfPagesViewModel
import com.example.viewmodel.PdfSecureViewModel
import com.example.viewmodel.PdfSplitViewModel
import com.example.viewmodel.PdfStampViewModel
import com.example.viewmodel.PdfStudioViewModel

enum class MainTab(val label: String, val icon: ImageVector) {
    TOOLS("Strumenti", Icons.Default.GridView),
    GEMINI("Gemini", Icons.Default.AutoAwesome),
    HISTORY("Cronologia", Icons.Default.History),
    SETTINGS("Impostazioni", Icons.Default.Tune)
}

/** Files shared to the app from another app, waiting for the user to pick a tool. */
data class SharedFiles(val uris: List<Uri>, val mimeType: String)

/** What is on screen: a main tab, or a tool opened on top of the "Strumenti" tab. */
private data class AppDestination(val tab: MainTab, val tool: OmniTool?)

/** All tool view models, created once and shared by the shell. */
class ToolViewModels(
    val pdf: PdfStudioViewModel,
    val merge: PdfMergeViewModel,
    val image: ImageLabViewModel,
    val audio: AudioLabViewModel,
    val archive: ArchiveViewModel,
    val markdown: MarkdownExporterViewModel,
    val ocr: MarkdownExporterViewModel,
    val compress: PdfCompressViewModel,
    val organize: PdfPagesViewModel,
    val pdfImages: PdfPagesViewModel,
    val stamp: PdfStampViewModel,
    val secure: PdfSecureViewModel,
    val split: PdfSplitViewModel,
    val collage: CollageViewModel
)

@Composable
fun OmniConvertApp(
    pdfViewModel: PdfStudioViewModel = viewModel(),
    geminiViewModel: GeminiHubViewModel = viewModel(),
    sharedFiles: SharedFiles? = null,
    onSharedFilesConsumed: () -> Unit = {}
) {
    val tools = ToolViewModels(
        pdf = pdfViewModel,
        merge = viewModel(),
        image = viewModel(),
        audio = viewModel(),
        archive = viewModel(),
        markdown = viewModel(key = "markdown"),
        ocr = viewModel(key = "ocr"),
        compress = viewModel(),
        organize = viewModel(key = "organize"),
        pdfImages = viewModel(key = "pdf_images"),
        stamp = viewModel(),
        secure = viewModel(),
        split = viewModel(),
        collage = viewModel()
    )

    var tabName by rememberSaveable { mutableStateOf(MainTab.TOOLS.name) }
    var toolName by rememberSaveable { mutableStateOf<String?>(null) }
    var showTutorial by rememberSaveable { mutableStateOf(false) }

    val tab = MainTab.valueOf(tabName)
    val activeTool = toolName?.let { name -> OmniTool.entries.firstOrNull { it.name == name } }
    val recents by HistoryRepository.entries.collectAsState()
    val haptics = rememberHaptics()

    fun openTool(tool: OmniTool) {
        if (tool.screen == ToolScreen.EXTRACT_ZIP) tools.archive.setMode(ArchiveMode.EXTRACT)
        if (tool.screen == ToolScreen.ARCHIVE) tools.archive.setMode(ArchiveMode.CREATE)
        tabName = MainTab.TOOLS.name
        toolName = tool.name
    }

    fun selectTab(newTab: MainTab) {
        if (newTab != tab || toolName != null) haptics(Haptic.TICK)
        toolName = null
        tabName = newTab.name
    }

    /** Hands files to a tool (from the share sheet or a "continue with" action) and opens it. */
    fun sendToTool(tool: OmniTool, uris: List<Uri>) {
        when (tool.screen) {
            ToolScreen.IMAGES_TO_PDF -> tools.pdf.addImages(uris)
            ToolScreen.MERGE_PDF -> tools.merge.addPdfDocuments(uris)
            ToolScreen.MARKDOWN -> if (tool == OmniTool.IMAGE_TO_TEXT) tools.ocr.addFiles(uris) else tools.markdown.addFiles(uris)
            ToolScreen.IMAGE_LAB -> tools.image.addImages(uris)
            ToolScreen.COLLAGE -> tools.collage.addImages(uris)
            ToolScreen.AUDIO_LAB -> tools.audio.addAudioFiles(uris)
            ToolScreen.ARCHIVE -> tools.archive.addFiles(uris)
            ToolScreen.COMPRESS_PDF -> uris.firstOrNull()?.let { tools.compress.loadPdf(it) }
            ToolScreen.ORGANIZE_PDF -> uris.firstOrNull()?.let { tools.organize.loadPdf(it) }
            ToolScreen.PDF_TO_IMAGES -> uris.firstOrNull()?.let { tools.pdfImages.loadPdf(it) }
            ToolScreen.EXTRACT_ZIP -> uris.firstOrNull()?.let { tools.archive.selectZipToExtract(it) }
            ToolScreen.STAMP_PDF -> uris.firstOrNull()?.let { tools.stamp.loadPdf(it) }
            ToolScreen.SECURE_PDF -> uris.firstOrNull()?.let { tools.secure.loadPdf(it) }
            ToolScreen.SPLIT_PDF -> uris.firstOrNull()?.let { tools.split.loadPdf(it) }
        }
        openTool(tool)
    }

    BackHandler(enabled = activeTool != null) { toolName = null }
    BackHandler(enabled = activeTool == null && tab != MainTab.TOOLS) { tabName = MainTab.TOOLS.name }

    val destination = AppDestination(tab, activeTool)

    val content: @Composable (AppDestination) -> Unit = { target ->
        if (target.tool != null) {
            CompositionLocalProvider(LocalToolBack provides { toolName = null }) {
                ToolContent(
                    tool = target.tool,
                    tools = tools,
                    onSendToTool = ::sendToTool,
                    onSendToGemini = { docText ->
                        geminiViewModel.setContextDocument(docText)
                        selectTab(MainTab.GEMINI)
                    },
                    onTranscribeAudio = { uri ->
                        geminiViewModel.attachAudio(uri)
                        selectTab(MainTab.GEMINI)
                    }
                )
            }
        } else {
            when (target.tab) {
                MainTab.TOOLS -> HomeScreen(
                    recents = recents,
                    onOpenTool = ::openTool,
                    onImagesForPdf = { uris -> sendToTool(OmniTool.IMAGES_TO_PDF, uris) },
                    onOpenHistory = { selectTab(MainTab.HISTORY) }
                )
                MainTab.GEMINI -> GeminiHubScreen(
                    viewModel = geminiViewModel,
                    onNavigateToMarkdown = { openTool(OmniTool.PDF_TO_MARKDOWN) }
                )
                MainTab.HISTORY -> HistoryScreen()
                MainTab.SETTINGS -> SettingsScreen(
                    onOpenGuide = { showTutorial = true },
                    onOpenGemini = { selectTab(MainTab.GEMINI) }
                )
            }
        }
    }

    val reduceMotion = LocalReduceMotion.current

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(OmniBg)
    ) {
        val isCompact = maxWidth < 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = OmniBg,
            bottomBar = {
                if (isCompact) {
                    AnimatedVisibility(
                        visible = activeTool == null,
                        enter = slideInVertically(motionSpec(Motion.gentle())) { it } + fadeIn(),
                        exit = slideOutVertically(tween(Motion.MEDIUM, easing = Motion.EmphasizedAccelerate)) { it } + fadeOut(tween(Motion.SHORT))
                    ) {
                        MorphBottomBar(selected = tab, onSelect = ::selectTab)
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
                    .imePadding()
            ) {
                if (!isCompact) {
                    MorphRail(selected = tab, onSelect = ::selectTab)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    AnimatedContent(
                        targetState = destination,
                        transitionSpec = {
                            if (reduceMotion) {
                                fadeIn(tween(1)) togetherWith fadeOut(tween(1))
                            } else {
                                destinationTransition(initialState, targetState)
                            }
                        },
                        label = "workspace_transition"
                    ) { target ->
                        content(target)
                    }
                }
            }
        }

        if (showTutorial) {
            OmniTutorialDialog(onDismiss = { showTutorial = false })
        }
    }

    if (sharedFiles != null) {
        SharedFilesSheet(
            files = sharedFiles,
            onPick = { tool ->
                haptics(Haptic.CONFIRM)
                sendToTool(tool, sharedFiles.uris)
                onSharedFilesConsumed()
            },
            onDismiss = onSharedFilesConsumed
        )
    }
}

/**
 * - Opening a tool: it slides in from the right over a slightly receding home.
 * - Closing a tool: the reverse.
 * - Switching tabs: a short horizontal "fade through" in the direction of the tab.
 */
private fun destinationTransition(from: AppDestination, to: AppDestination): ContentTransform {
    val opening = from.tool == null && to.tool != null
    val closing = from.tool != null && to.tool == null
    val transform = when {
        opening -> (slideInHorizontally(tween(Motion.LONG, easing = Motion.EmphasizedDecelerate)) { it / 4 } + fadeIn(tween(Motion.MEDIUM)))
            .togetherWith(scaleOut(targetScale = 0.94f, animationSpec = tween(Motion.LONG)) + fadeOut(tween(Motion.MEDIUM)))
        closing -> (scaleIn(initialScale = 0.94f, animationSpec = tween(Motion.LONG, easing = Motion.EmphasizedDecelerate)) + fadeIn(tween(Motion.MEDIUM)))
            .togetherWith(slideOutHorizontally(tween(Motion.MEDIUM, easing = Motion.EmphasizedAccelerate)) { it / 4 } + fadeOut(tween(Motion.MEDIUM)))
        else -> {
            val forward = to.tab.ordinal >= from.tab.ordinal
            (slideInHorizontally(tween(Motion.MEDIUM, easing = Motion.EmphasizedDecelerate)) { if (forward) it / 10 else -it / 10 } + fadeIn(tween(Motion.MEDIUM, delayMillis = 40)))
                .togetherWith(slideOutHorizontally(tween(Motion.SHORT)) { if (forward) -it / 14 else it / 14 } + fadeOut(tween(Motion.SHORT)))
        }
    }
    return ContentTransform(transform.targetContentEnter, transform.initialContentExit, sizeTransform = SizeTransform(clip = false))
}

@Composable
private fun ToolContent(
    tool: OmniTool,
    tools: ToolViewModels,
    onSendToTool: (OmniTool, List<Uri>) -> Unit,
    onSendToGemini: (String) -> Unit,
    onTranscribeAudio: (Uri) -> Unit
) {
    val toMerge: (Uri) -> Unit = { onSendToTool(OmniTool.MERGE_PDF, listOf(it)) }
    val toMarkdown: (Uri) -> Unit = { onSendToTool(OmniTool.PDF_TO_MARKDOWN, listOf(it)) }
    val toCompress: (Uri) -> Unit = { onSendToTool(OmniTool.COMPRESS_PDF, listOf(it)) }
    val toOrganize: (Uri) -> Unit = { onSendToTool(OmniTool.ORGANIZE_PDF, listOf(it)) }
    val toStamp: (Uri) -> Unit = { onSendToTool(OmniTool.STAMP_PDF, listOf(it)) }
    val toSecure: (Uri) -> Unit = { onSendToTool(OmniTool.SECURE_PDF, listOf(it)) }

    when (tool.screen) {
        ToolScreen.IMAGES_TO_PDF -> PdfStudioScreen(
            viewModel = tools.pdf,
            onContinueWithMerge = toMerge,
            onContinueWithMarkdown = toMarkdown,
            onContinueWithCompress = toCompress
        )
        ToolScreen.MERGE_PDF -> PdfMergeScreen(
            viewModel = tools.merge,
            onContinueWithMarkdown = toMarkdown,
            onContinueWithCompress = toCompress,
            onContinueWithOrganize = toOrganize
        )
        ToolScreen.MARKDOWN -> if (tool == OmniTool.IMAGE_TO_TEXT) {
            MarkdownExporterScreen(viewModel = tools.ocr, onSendToGemini = onSendToGemini, ocrMode = true)
        } else {
            MarkdownExporterScreen(viewModel = tools.markdown, onSendToGemini = onSendToGemini)
        }
        ToolScreen.IMAGE_LAB -> ImageLabScreen(viewModel = tools.image)
        ToolScreen.COLLAGE -> CollageScreen(viewModel = tools.collage)
        ToolScreen.AUDIO_LAB -> AudioLabScreen(
            viewModel = tools.audio,
            onTranscribeAudio = { uri, _ -> onTranscribeAudio(uri) }
        )
        ToolScreen.ARCHIVE, ToolScreen.EXTRACT_ZIP -> ArchiveScreen(viewModel = tools.archive)
        ToolScreen.COMPRESS_PDF -> PdfCompressScreen(
            viewModel = tools.compress,
            onContinueWithMerge = toMerge,
            onContinueWithMarkdown = toMarkdown
        )
        ToolScreen.ORGANIZE_PDF -> PdfOrganizeScreen(viewModel = tools.organize, onContinueWithCompress = toCompress)
        ToolScreen.PDF_TO_IMAGES -> PdfToImagesScreen(viewModel = tools.pdfImages)
        ToolScreen.STAMP_PDF -> PdfStampScreen(viewModel = tools.stamp, onContinueWithCompress = toCompress, onContinueWithProtect = toSecure)
        ToolScreen.SECURE_PDF -> PdfSecureScreen(viewModel = tools.secure, onContinueWithOrganize = toOrganize, onContinueWithStamp = toStamp)
        ToolScreen.SPLIT_PDF -> PdfSplitScreen(viewModel = tools.split, onContinueWithOrganize = toOrganize)
    }
}

@Composable
private fun SharedFilesSheet(files: SharedFiles, onPick: (OmniTool) -> Unit, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val candidates = remember(files) { toolsFor(files.mimeType) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = OmniSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = if (files.uris.size == 1) "Cosa vuoi fare con questo file?" else "Cosa vuoi fare con questi ${files.uris.size} file?",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = OmniText
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text("Scegli uno strumento: i file sono già pronti.", fontSize = 14.sp, color = OmniTextMuted)
            Spacer(modifier = Modifier.height(16.dp))
            candidates.forEachIndexed { index, tool ->
                val interaction = remember { MutableInteractionSource() }
                Surface(
                    onClick = { onPick(tool) },
                    interactionSource = interaction,
                    shape = RoundedCornerShape(RadiusMd),
                    color = Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .enterAnimation(index)
                        .pressScale(interaction, 0.98f)
                        .testTag("share_tool_${tool.name.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconTile(icon = tool.icon, tint = tool.tint, size = 46.dp)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(tool.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OmniText)
                            Text(tool.description, fontSize = 13.sp, color = OmniTextMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = OmniTextMuted)
                    }
                }
            }
        }
    }
}

@Composable
private fun MorphBottomBar(selected: MainTab, onSelect: (MainTab) -> Unit) {
    Surface(color = OmniSurface, modifier = Modifier.fillMaxWidth()) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(OmniBorder)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                MainTab.entries.forEach { item ->
                    NavItem(
                        tab = item,
                        selected = item == selected,
                        onClick = { onSelect(item) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MorphRail(selected: MainTab, onSelect: (MainTab) -> Unit) {
    Surface(
        color = OmniSurface,
        modifier = Modifier
            .width(100.dp)
            .fillMaxHeight()
            .testTag("tablet_navigation_rail")
    ) {
        Row {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                BrandLogo(size = 52)
                Spacer(modifier = Modifier.height(18.dp))
                MainTab.entries.forEach { item ->
                    NavItem(tab = item, selected = item == selected, onClick = { onSelect(item) })
                }
                Spacer(modifier = Modifier.weight(1f))
                val darkNow = LocalMorphColors.current.isDark
                IconButton(
                    onClick = { AppPreferences.setThemeMode(if (darkNow) ThemeMode.LIGHT else ThemeMode.DARK) },
                    modifier = Modifier.testTag("rail_theme_toggle")
                ) {
                    Icon(
                        imageVector = if (darkNow) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = if (darkNow) "Passa al tema chiaro" else "Passa al tema scuro",
                        tint = OmniTextMuted
                    )
                }
            }
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(OmniBorder)
            )
        }
    }
}

@Composable
private fun NavItem(
    tab: MainTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val pillWidth by animateDpAsState(if (selected) 60.dp else 36.dp, motionSpec(spring(dampingRatio = 0.6f, stiffness = 500f)), label = "nav_pill")
    val pillColor by animateColorAsState(if (selected) OmniAccentSoft else Color.Transparent, label = "nav_pill_color")
    val iconTint by animateColorAsState(if (selected) OmniAccent else OmniTextMuted, label = "nav_icon")
    val iconScale by animateFloatAsState(if (selected) 1.12f else 1f, motionSpec(spring(dampingRatio = 0.4f, stiffness = 400f)), label = "nav_icon_scale")
    Surface(
        onClick = onClick,
        interactionSource = interaction,
        color = Color.Transparent,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .pressScale(interaction, 0.92f)
            .testTag("nav_item_${tab.name.lowercase()}")
    ) {
        Column(
            modifier = Modifier.padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(width = pillWidth, height = 34.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(pillColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = tab.label,
                    tint = iconTint,
                    modifier = Modifier
                        .size(22.dp)
                        .graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                        }
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = tab.label,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) OmniText else OmniTextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
