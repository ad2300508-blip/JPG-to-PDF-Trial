@file:OptIn(ExperimentalLayoutApi::class)

package com.example

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.example.ui.ImageLabScreen
import com.example.ui.MarkdownExporterScreen
import com.example.ui.PdfMergeScreen
import com.example.ui.PdfStudioScreen
import com.example.ui.SettingsScreen
import com.example.ui.components.LocalToolBack
import com.example.ui.components.OmniTutorialDialog
import com.example.ui.home.BrandLogo
import com.example.ui.home.HomeScreen
import com.example.ui.home.OmniTool
import com.example.ui.home.ToolScreen
import com.example.ui.theme.OmniAccent
import com.example.ui.theme.OmniAccentSoft
import com.example.ui.theme.OmniBg
import com.example.ui.theme.OmniBorder
import com.example.ui.theme.OmniSurface
import com.example.ui.theme.OmniText
import com.example.ui.theme.OmniTextMuted
import com.example.viewmodel.ArchiveViewModel
import com.example.viewmodel.AudioLabViewModel
import com.example.viewmodel.GeminiHubViewModel
import com.example.viewmodel.ImageLabViewModel
import com.example.viewmodel.MarkdownExporterViewModel
import com.example.viewmodel.PdfMergeViewModel
import com.example.viewmodel.PdfStudioViewModel

enum class MainTab(val label: String, val icon: ImageVector) {
    TOOLS("Strumenti", Icons.Default.GridView),
    GEMINI("Gemini", Icons.Default.AutoAwesome),
    HISTORY("Cronologia", Icons.Default.History),
    SETTINGS("Impostazioni", Icons.Default.Tune)
}

/** What is on screen: a main tab, or a tool opened on top of the "Strumenti" tab. */
private data class AppDestination(val tab: MainTab, val tool: OmniTool?)

@Composable
fun OmniConvertApp(
    pdfViewModel: PdfStudioViewModel = viewModel(),
    pdfMergeViewModel: PdfMergeViewModel = viewModel(),
    imageViewModel: ImageLabViewModel = viewModel(),
    audioViewModel: AudioLabViewModel = viewModel(),
    archiveViewModel: ArchiveViewModel = viewModel(),
    markdownViewModel: MarkdownExporterViewModel = viewModel(),
    geminiViewModel: GeminiHubViewModel = viewModel(),
    openPdfToolSignal: Int = 0
) {
    var tabName by rememberSaveable { mutableStateOf(MainTab.TOOLS.name) }
    var toolName by rememberSaveable { mutableStateOf<String?>(null) }
    var showTutorial by rememberSaveable { mutableStateOf(false) }

    val tab = MainTab.valueOf(tabName)
    val activeTool = toolName?.let { name -> OmniTool.entries.firstOrNull { it.name == name } }
    val recents by HistoryRepository.entries.collectAsState()

    fun openTool(tool: OmniTool) {
        tabName = MainTab.TOOLS.name
        toolName = tool.name
    }

    fun selectTab(newTab: MainTab) {
        toolName = null
        tabName = newTab.name
    }

    // Images shared from other apps open the "Immagini in PDF" tool directly.
    LaunchedEffect(openPdfToolSignal) {
        if (openPdfToolSignal > 0) openTool(OmniTool.IMAGES_TO_PDF)
    }

    BackHandler(enabled = activeTool != null) { toolName = null }
    BackHandler(enabled = activeTool == null && tab != MainTab.TOOLS) { tabName = MainTab.TOOLS.name }

    val destination = AppDestination(tab, activeTool)

    val content: @Composable (AppDestination) -> Unit = { target ->
        if (target.tool != null) {
            CompositionLocalProvider(LocalToolBack provides { toolName = null }) {
                ToolContent(
                    tool = target.tool,
                    pdfViewModel = pdfViewModel,
                    pdfMergeViewModel = pdfMergeViewModel,
                    imageViewModel = imageViewModel,
                    audioViewModel = audioViewModel,
                    archiveViewModel = archiveViewModel,
                    markdownViewModel = markdownViewModel,
                    onOpenTool = ::openTool,
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
                    onImagesForPdf = { uris ->
                        pdfViewModel.addImages(uris)
                        openTool(OmniTool.IMAGES_TO_PDF)
                    },
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
                if (isCompact && activeTool == null) {
                    MorphBottomBar(selected = tab, onSelect = ::selectTab)
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
                            fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(160))
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
}

@Composable
private fun ToolContent(
    tool: OmniTool,
    pdfViewModel: PdfStudioViewModel,
    pdfMergeViewModel: PdfMergeViewModel,
    imageViewModel: ImageLabViewModel,
    audioViewModel: AudioLabViewModel,
    archiveViewModel: ArchiveViewModel,
    markdownViewModel: MarkdownExporterViewModel,
    onOpenTool: (OmniTool) -> Unit,
    onSendToGemini: (String) -> Unit,
    onTranscribeAudio: (Uri) -> Unit
) {
    val continueWithMerge: (Uri) -> Unit = { uri ->
        pdfMergeViewModel.addPdfDocuments(listOf(uri))
        onOpenTool(OmniTool.MERGE_PDF)
    }
    val continueWithMarkdown: (Uri) -> Unit = { uri ->
        markdownViewModel.addFiles(listOf(uri))
        onOpenTool(OmniTool.PDF_TO_MARKDOWN)
    }

    when (tool.screen) {
        ToolScreen.IMAGES_TO_PDF -> PdfStudioScreen(
            viewModel = pdfViewModel,
            onContinueWithMerge = continueWithMerge,
            onContinueWithMarkdown = continueWithMarkdown
        )
        ToolScreen.MERGE_PDF -> PdfMergeScreen(
            viewModel = pdfMergeViewModel,
            onContinueWithMarkdown = continueWithMarkdown
        )
        ToolScreen.MARKDOWN -> MarkdownExporterScreen(
            viewModel = markdownViewModel,
            onSendToGemini = onSendToGemini
        )
        ToolScreen.IMAGE_LAB -> ImageLabScreen(viewModel = imageViewModel)
        ToolScreen.AUDIO_LAB -> AudioLabScreen(
            viewModel = audioViewModel,
            onTranscribeAudio = { uri, _ -> onTranscribeAudio(uri) }
        )
        ToolScreen.ARCHIVE -> ArchiveScreen(viewModel = archiveViewModel)
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
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.testTag("nav_item_${tab.name.lowercase()}")
    ) {
        Column(
            modifier = Modifier.padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(width = 60.dp, height = 34.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(if (selected) OmniAccentSoft else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = tab.label,
                    tint = if (selected) OmniAccent else OmniTextMuted,
                    modifier = Modifier.size(22.dp)
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
