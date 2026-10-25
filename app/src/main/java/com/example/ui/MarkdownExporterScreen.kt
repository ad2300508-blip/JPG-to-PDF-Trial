package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CompiledMarkdownResult
import com.example.model.LlmPromptWrapper
import com.example.model.MarkdownExportSettings
import com.example.model.MarkdownSourceItem
import com.example.ui.components.OmniBadge
import com.example.ui.components.OmniCard
import com.example.ui.components.OmniEmptyState
import com.example.ui.components.OmniErrorBanner
import com.example.ui.components.OmniHeader
import com.example.ui.components.OmniLinearProgress
import com.example.ui.components.OmniPhoneTabSelector
import com.example.ui.components.OmniStatPill
import com.example.ui.components.OmniSuccessBanner
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.RoseAccent
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceElevated
import com.example.viewmodel.MarkdownExporterViewModel

@Composable
fun MarkdownExporterScreen(
    viewModel: MarkdownExporterViewModel,
    onSendToGemini: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val items by viewModel.items.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val isIngesting by viewModel.isIngesting.collectAsState()
    val extractionProgress by viewModel.extractionProgress.collectAsState()
    val extractionStatusText by viewModel.extractionStatusText.collectAsState()
    val compiledResult by viewModel.compiledResult.collectAsState()
    val errorNotice by viewModel.errorNotice.collectAsState()
    val copySuccess by viewModel.copySuccessMessage.collectAsState()
    val savedUri by viewModel.savedUri.collectAsState()

    var phoneTabIndex by remember { mutableIntStateOf(0) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addFiles(uris)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("markdown_exporter_screen")
    ) {
        OmniHeader(
            workspaceTitle = "AI Markdown",
            categoryBadge = "Document Intelligence & OCR",
            badgeColor = CyanAccent,
            itemCount = items.size,
            isProcessing = isIngesting,
            actions = {
                OutlinedButton(
                    onClick = { filePicker.launch("*/*") },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent),
                    modifier = Modifier.testTag("markdown_add_docs_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.NoteAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Import Docs / PDFs", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                }
                if (items.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = { viewModel.clearAll() }) {
                        Icon(Icons.Default.ClearAll, contentDescription = "Clear All", tint = Slate400)
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Requirement 4: "PARSE ALL PAGES" Scope Toggle Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceElevated)
                .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val isAllPages = settings.parseAllPages
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (!isAllPages) CyanAccent.copy(alpha = 0.2f) else Color.Transparent)
                    .border(1.dp, if (!isAllPages) CyanAccent.copy(alpha = 0.6f) else Color.Transparent, RoundedCornerShape(8.dp))
                    .clickable { viewModel.toggleParseAllPages(false) }
                    .padding(vertical = 8.dp)
                    .testTag("toggle_preview_15_pages"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Quick Preview (15 Pages)",
                    fontSize = 11.sp,
                    fontWeight = if (!isAllPages) FontWeight.Bold else FontWeight.Medium,
                    color = if (!isAllPages) CyanAccent else Slate400,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isAllPages) CyanAccent.copy(alpha = 0.2f) else Color.Transparent)
                    .border(1.dp, if (isAllPages) CyanAccent.copy(alpha = 0.6f) else Color.Transparent, RoundedCornerShape(8.dp))
                    .clickable { viewModel.toggleParseAllPages(true) }
                    .padding(vertical = 8.dp)
                    .testTag("toggle_full_document_extraction"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Full Document (All Pages)",
                    fontSize = 11.sp,
                    fontWeight = if (isAllPages) FontWeight.Bold else FontWeight.Medium,
                    color = if (isAllPages) CyanAccent else Slate400,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        val hasRemaining = items.any { it.hasMorePages }
        if (hasRemaining && !isIngesting) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { viewModel.parseAllDocumentsRemaining() },
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent.copy(alpha = 0.15f), contentColor = CyanAccent),
                border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .testTag("parse_all_remaining_button")
            ) {
                Text(
                    text = "Extract All Remaining Pages (${items.count { it.hasMorePages }} docs) →",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (errorNotice != null) {
            OmniErrorBanner(message = errorNotice!!, onDismiss = { viewModel.dismissErrorNotice() })
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (copySuccess != null) {
            OmniSuccessBanner(message = copySuccess!!, onDismiss = { viewModel.dismissCopySuccess() })
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (savedUri != null) {
            OmniSuccessBanner(message = "Saved to Downloads/OmniConvert/Markdown", onDismiss = { viewModel.clearAll() })
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (isIngesting) {
            OmniLinearProgress(
                progress = extractionProgress,
                statusText = extractionStatusText,
                onCancel = { viewModel.cancelExtraction() }
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val isCompact = maxWidth < 600.dp

            if (isCompact) {
                // Phone Layout: Tabbed Switcher (Sources vs Prompt Architect)
                Column(modifier = Modifier.fillMaxSize()) {
                    OmniPhoneTabSelector(
                        selectedTabIndex = phoneTabIndex,
                        tabs = listOf("Sources (${items.size})", "AI Prompt & Export"),
                        accentColor = CyanAccent,
                        onTabSelected = { phoneTabIndex = it },
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    if (phoneTabIndex == 0) {
                        MarkdownSourcesPane(
                            items = items,
                            onPickFiles = { filePicker.launch("*/*") },
                            viewModel = viewModel,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            onNavigateToExport = if (items.isNotEmpty()) { { phoneTabIndex = 1 } } else null
                        )
                    } else {
                        MarkdownPromptArchitectPane(
                            settings = settings,
                            compiledResult = compiledResult,
                            isIngesting = isIngesting,
                            viewModel = viewModel,
                            onSendToGemini = onSendToGemini,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                    }
                }
            } else {
                // Dual Pane Tablet Layout: Left 38% Sources, Right 62% Prompt Architect
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    MarkdownSourcesPane(
                        items = items,
                        onPickFiles = { filePicker.launch("*/*") },
                        viewModel = viewModel,
                        modifier = Modifier
                            .weight(0.38f)
                            .fillMaxHeight(),
                        onNavigateToExport = null
                    )

                    MarkdownPromptArchitectPane(
                        settings = settings,
                        compiledResult = compiledResult,
                        isIngesting = isIngesting,
                        viewModel = viewModel,
                        onSendToGemini = onSendToGemini,
                        modifier = Modifier
                            .weight(0.62f)
                            .fillMaxHeight()
                    )
                }
            }
        }
    }
}

@Composable
private fun MarkdownSourcesPane(
    items: List<MarkdownSourceItem>,
    onPickFiles: () -> Unit,
    viewModel: MarkdownExporterViewModel,
    modifier: Modifier = Modifier,
    onNavigateToExport: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceCard)
            .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        if (items.isEmpty()) {
            OmniEmptyState(
                icon = Icons.Default.SmartToy,
                title = "No Ingested Sources",
                description = "Add PDFs, book screenshots, text files, or notes. They are parsed into structured Markdown with OCR and token analysis."
            ) {
                Button(
                    onClick = onPickFiles,
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(48.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.NoteAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Import Files", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        SourceDocCard(
                            item = item,
                            onParseRemaining = { viewModel.parseRemainingPages(item.id) },
                            onRemove = { viewModel.removeItem(item.id) }
                        )
                    }
                }

                if (onNavigateToExport != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onNavigateToExport,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SurfaceElevated,
                            contentColor = CyanAccent
                        ),
                        border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Configure Prompt & Export →", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun MarkdownPromptArchitectPane(
    settings: MarkdownExportSettings,
    compiledResult: CompiledMarkdownResult?,
    isIngesting: Boolean,
    viewModel: MarkdownExporterViewModel,
    onSendToGemini: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceCard)
            .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
            .padding(18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "LLM PROMPT ARCHITECT & FORMATTING",
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate400,
                    maxLines = 1,
                    softWrap = false
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Prompt Wrapper Selection Grid
                Text(
                    text = "Target AI Prompt Wrapper",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    softWrap = false
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LlmPromptWrapper.values().forEach { wrapper ->
                        val selected = settings.promptWrapper == wrapper
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) CyanAccent.copy(alpha = 0.15f) else SurfaceElevated)
                                .border(1.dp, if (selected) CyanAccent else BorderSubtle, RoundedCornerShape(10.dp))
                                .clickable { viewModel.updateSettings(settings.copy(promptWrapper = wrapper)) }
                                .padding(12.dp)
                                .testTag("wrapper_${wrapper.name}")
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = wrapper.displayName,
                                        color = if (selected) CyanAccent else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (selected) {
                                        OmniBadge(text = "ACTIVE", color = CyanAccent)
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = wrapper.description,
                                    color = Slate400,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Markdown Live Code Preview Box
                Text(
                    text = "Compiled Markdown Output Preview",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    softWrap = false
                )
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceElevated)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    val text = compiledResult?.wrappedPrompt ?: "No compiled content. Ingest documents to generate preview."
                    Text(
                        text = text,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = if (compiledResult != null) Slate300 else Slate400,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom Action & Telemetry Deck
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val tokens = compiledResult?.totalTokens ?: 0
                    val chars = compiledResult?.totalChars ?: 0
                    OmniStatPill(label = "Est. Tokens", value = "~$tokens", valueColor = CyanAccent)
                    OmniStatPill(label = "Characters", value = "$chars", valueColor = Slate400)
                    OmniStatPill(label = "Documents", value = "${compiledResult?.docCount ?: 0}", valueColor = EmeraldAccent)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 1-Tap Direct Integration with Gemini AI Studio
                Button(
                    onClick = {
                        val content = compiledResult?.rawMarkdown ?: ""
                        if (content.isNotEmpty()) onSendToGemini(content)
                    },
                    enabled = compiledResult != null && !isIngesting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("send_to_gemini_hub_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AmberAccent,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Send Extracted Markdown to Gemini AI Studio",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.copyPromptToClipboard() },
                        enabled = compiledResult != null && !isIngesting,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("copy_prompt_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanAccent,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "COPY TO CLIPBOARD",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Button(
                        onClick = { viewModel.saveAsMarkdown("OmniContext") },
                        enabled = compiledResult != null && !isIngesting,
                        modifier = Modifier
                            .weight(0.6f)
                            .height(46.dp)
                            .testTag("save_markdown_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SurfaceElevated,
                            contentColor = CyanAccent
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SAVE .MD",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceDocCard(
    item: MarkdownSourceItem,
    onParseRemaining: () -> Unit = {},
    onRemove: () -> Unit
) {
    val isError = item.error != null
    OmniCard(
        borderColor = if (isError) RoseAccent.copy(alpha = 0.5f) else BorderSubtle,
        backgroundColor = SurfaceElevated
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isError) RoseAccent.copy(alpha = 0.2f) else Slate800),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isError) Icons.Default.ErrorOutline else Icons.Default.Description,
                    contentDescription = null,
                    tint = if (isError) RoseAccent else CyanAccent,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (isError) {
                    Text(
                        text = item.error ?: "Failed to parse",
                        color = RoseAccent,
                        fontSize = 11.sp,
                        maxLines = 2
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OmniBadge(text = "~${item.estimatedTokens} TOKENS", color = CyanAccent)
                        OmniBadge(text = "${item.sizeBytes / 1024} KB", color = Slate400)
                        if (item.pageCount > 1) {
                            OmniBadge(text = "${item.parsedPages}/${item.pageCount} PAGES", color = IndigoAccent)
                        }
                    }
                    if (item.hasMorePages) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "PARSE REMAINING PAGES (${item.pageCount - item.parsedPages} MORE)",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(CyanAccent.copy(alpha = 0.1f))
                                .clickable { onParseRemaining() }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                .testTag("parse_remaining_${item.id}")
                        )
                    }
                }
            }

            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = RoseAccent, modifier = Modifier.size(18.dp))
            }
        }
    }
}
