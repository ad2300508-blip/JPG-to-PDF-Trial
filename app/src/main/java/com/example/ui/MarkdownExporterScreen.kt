package com.example.ui

import com.example.ui.components.SegmentedControl
import com.example.ui.components.OptionSection
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LlmPromptWrapper
import com.example.model.MarkdownSourceItem
import com.example.ui.components.AnimatedText
import com.example.ui.components.Dropzone
import com.example.ui.components.FileListHeader
import com.example.ui.components.OmniProgressBanner
import com.example.ui.components.OmniToast
import com.example.ui.components.OptionRow
import com.example.ui.components.RadioCard
import com.example.ui.components.RevealVertically
import com.example.ui.components.ToolLayout
import com.example.ui.components.openFile
import com.example.ui.components.rememberCameraCapture
import com.example.ui.theme.Haptic
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
import com.example.ui.theme.RadiusPill
import com.example.ui.theme.RadiusSm
import com.example.ui.theme.TintAi
import com.example.ui.theme.TintOcr
import com.example.ui.theme.enterAnimation
import com.example.ui.theme.pressScale
import com.example.ui.theme.rememberHaptics
import com.example.util.OutputFolder
import com.example.util.OutputStorage
import com.example.viewmodel.MarkdownExporterViewModel

@Composable
fun MarkdownExporterScreen(
    viewModel: MarkdownExporterViewModel,
    onSendToGemini: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    ocrMode: Boolean = false
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
    val savedPdfUri by viewModel.savedPdfUri.collectAsState()
    val context = LocalContext.current
    val haptics = rememberHaptics()

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) viewModel.addFiles(uris)
    }
    val launchCamera = rememberCameraCapture { uri -> viewModel.addFiles(listOf(uri)) }
    val pickMime = if (ocrMode) "image/*" else "*/*"

    val totalTokens = compiledResult?.totalTokens ?: 0
    val totalChars = compiledResult?.totalChars ?: 0
    val documentCount = compiledResult?.docCount ?: 0
    val hasMorePages = items.any { it.hasMorePages }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("markdown_exporter_screen")
    ) {
        RevealVertically(visible = errorNotice != null) {
            OmniToast(
                message = errorNotice.orEmpty(),
                isError = true,
                actionLabel = "Chiudi",
                onAction = { viewModel.dismissErrorNotice() },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }
        RevealVertically(visible = copySuccess != null) {
            OmniToast(
                message = copySuccess.orEmpty(),
                onDismiss = { viewModel.dismissCopySuccess() },
                autoDismissMillis = 3000,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }
        RevealVertically(visible = savedUri != null || savedPdfUri != null) {
            val isPdf = savedPdfUri != null
            OmniToast(
                message = if (isPdf) "PDF salvato in ${OutputStorage.displayPath(OutputFolder.PDFS)}" else "Markdown salvato in ${OutputStorage.displayPath(OutputFolder.MARKDOWN)}",
                actionLabel = "Apri",
                onAction = {
                    val uri = savedPdfUri ?: savedUri
                    if (uri != null) openFile(context, uri, if (isPdf) "application/pdf" else "text/markdown")
                },
                onDismiss = { viewModel.dismissSaved() },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }
        RevealVertically(visible = isIngesting) {
            OmniProgressBanner(
                progress = extractionProgress,
                statusText = extractionStatusText.ifBlank { "Estraggo e formatto il testo…" },
                onCancel = { viewModel.cancelExtraction() },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }

        ToolLayout(
            title = if (ocrMode) "Immagine in testo" else "PDF in Markdown",
            description = if (ocrMode) "Riconosci il testo da foto e scansioni" else "Estrai testo pulito da PDF, Word, appunti e foto",
            isEmpty = items.isEmpty() && !isIngesting,
            phoneTabTitles = listOf("Anteprima", "Opzioni"),
            emptyDropzone = {
                Dropzone(
                    icon = if (ocrMode) Icons.Default.DocumentScanner else Icons.Default.SmartToy,
                    headline = if (ocrMode) "Fotografa o scegli un'immagine" else "Aggiungi i tuoi documenti",
                    helpText = if (ocrMode) {
                        "Riconosco il testo direttamente sul telefono, anche da libri e lavagne"
                    } else {
                        "Importa capitoli PDF, documenti Word o foto: estraggo un Markdown pulito pronto per l'AI"
                    },
                    supportedFormats = if (ocrMode) "JPG · PNG · WEBP · HEIC" else "PDF · DOCX · TXT · MD · JPG · PNG",
                    actionLabel = "Scegli file",
                    tintColor = if (ocrMode) TintOcr else TintAi,
                    onAction = { filePicker.launch(pickMime) },
                    testTag = "markdown_add_docs_button",
                    secondaryIcon = Icons.Default.DocumentScanner,
                    onSecondaryAction = launchCamera
                )
            },
            fileList = {
                Column(modifier = Modifier.fillMaxSize()) {
                    FileListHeader(
                        title = if (items.size == 1) "1 documento" else "${items.size} documenti",
                        onAdd = { filePicker.launch(pickMime) },
                        onClear = { viewModel.clearAll() },
                        addIcon = Icons.AutoMirrored.Filled.NoteAdd
                    )

                    // Loaded documents as removable chips (errors highlighted).
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items.forEachIndexed { index, item ->
                            DocumentChip(
                                item = item,
                                onRemove = { viewModel.removeItem(item.id) },
                                onParseRemaining = { viewModel.parseRemainingPages(item.id) },
                                modifier = Modifier.enterAnimation(index, key = item.id)
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        color = OmniSurface2,
                        border = BorderStroke(1.dp, OmniBorder),
                        shape = RoundedCornerShape(RadiusMd)
                    ) {
                        val text = compiledResult?.wrappedPrompt.orEmpty()
                        AnimatedContent(
                            targetState = text.isBlank(),
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "markdown_preview"
                        ) { blank ->
                            SelectionContainer {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                        .padding(14.dp)
                                ) {
                                    Text(
                                        text = if (blank) (if (isIngesting) "Estraggo il testo…" else "Nessun testo estratto") else text,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp,
                                        color = if (blank) OmniTextMuted else OmniText,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    AnimatedText(
                        text = "~$totalTokens token · $totalChars caratteri · $documentCount documenti",
                        fontSize = 13.sp,
                        color = OmniTextMuted
                    )
                }
            },
            optionsPanel = {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Estrazione", style = MaterialTheme.typography.titleMedium, color = OmniText)

                    OptionRow(
                        label = "Estrai tutte le pagine",
                        description = if (hasMorePages) "Ora sono state lette solo le prime 15 pagine dei PDF lunghi" else "Di base vengono lette le prime 15 pagine dei PDF lunghi",
                        checked = settings.parseAllPages,
                        onCheckedChange = { viewModel.toggleParseAllPages(it) }
                    )

                    OptionRow(
                        label = "Usa sempre l'OCR",
                        description = "Ignora il testo già presente nel PDF (utile se esce con caratteri strani)",
                        checked = settings.forceOcr,
                        onCheckedChange = { viewModel.setForceOcr(it) }
                    )

                    OptionRow(
                        label = "Togli intestazioni e piè di pagina",
                        description = "Elimina titoli ripetuti in cima o in fondo a ogni pagina e i numeri di pagina",
                        checked = settings.cleanMargins,
                        onCheckedChange = { viewModel.setCleanMargins(it) }
                    )

                    OptionSection(
                        title = "Separatori di pagina",
                        hint = when (settings.pageMarkers) {
                            com.example.service.PageMarkerStyle.COMMENT -> "Invisibili nel testo, ma l'AI sa da che pagina viene ogni parte"
                            com.example.service.PageMarkerStyle.HEADING -> "Un titolo «Pagina N» prima di ogni pagina"
                            com.example.service.PageMarkerStyle.NONE -> "Testo continuo, senza riferimenti alle pagine"
                        }
                    ) {
                        SegmentedControl(
                            options = com.example.service.PageMarkerStyle.entries.map { it.label },
                            selectedIndex = settings.pageMarkers.ordinal,
                            onOptionSelected = { viewModel.setPageMarkers(com.example.service.PageMarkerStyle.entries[it]) }
                        )
                    }

                    Text("Formato per l'AI", style = MaterialTheme.typography.titleMedium, color = OmniText)

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        RadioCard(
                            title = "Tag XML per Claude",
                            description = "Racchiude i documenti in tag <documents> per i modelli Anthropic",
                            isSelected = settings.promptWrapper == LlmPromptWrapper.CLAUDE_XML,
                            onClick = { viewModel.updateSettings(settings.copy(promptWrapper = LlmPromptWrapper.CLAUDE_XML)) }
                        )
                        RadioCard(
                            title = "Blocco di sistema ChatGPT",
                            description = "Sezioni formattate per ChatGPT",
                            isSelected = settings.promptWrapper == LlmPromptWrapper.CHATGPT_SYSTEM,
                            onClick = { viewModel.updateSettings(settings.copy(promptWrapper = LlmPromptWrapper.CHATGPT_SYSTEM)) }
                        )
                        RadioCard(
                            title = "Brief di contesto per Gemini",
                            description = "Ottimizzato con delimitatori gerarchici per Gemini",
                            isSelected = settings.promptWrapper == LlmPromptWrapper.GEMINI_BRIEF,
                            onClick = { viewModel.updateSettings(settings.copy(promptWrapper = LlmPromptWrapper.GEMINI_BRIEF)) },
                            badgeText = "Consigliato"
                        )
                        RadioCard(
                            title = "Markdown puro",
                            description = "Solo il testo, senza istruzioni per un modello",
                            isSelected = settings.promptWrapper == LlmPromptWrapper.RAW_MARKDOWN,
                            onClick = { viewModel.updateSettings(settings.copy(promptWrapper = LlmPromptWrapper.RAW_MARKDOWN)) }
                        )
                    }

                    OptionRow(
                        label = "Intestazione con metadati",
                        description = "Data di esportazione e numero di documenti",
                        checked = settings.includeMetadataHeader,
                        onCheckedChange = { viewModel.updateSettings(settings.copy(includeMetadataHeader = it)) }
                    )

                    OptionRow(
                        label = "Indice dei documenti",
                        description = "Elenco dei file con i token stimati (con più documenti)",
                        checked = settings.includeTableOfContents,
                        onCheckedChange = { viewModel.updateSettings(settings.copy(includeTableOfContents = it)) }
                    )
                }
            },
            actionBar = {
                val promptToSend = compiledResult?.wrappedPrompt.orEmpty()
                val ready = promptToSend.isNotBlank() && !isIngesting
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = OmniSurface,
                    border = BorderStroke(1.dp, OmniBorder),
                    shape = RoundedCornerShape(RadiusMd),
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SquareAction(Icons.Default.ContentCopy, "Copia", enabled = ready, testTag = "copy_markdown_button") {
                            haptics(Haptic.CONFIRM)
                            viewModel.copyPromptToClipboard()
                        }
                        SquareAction(Icons.Default.Save, "Salva .md", enabled = ready, testTag = "save_markdown_button") {
                            viewModel.saveAsMarkdown(viewModel.suggestedTitle())
                        }
                        SquareAction(Icons.Default.PictureAsPdf, "PDF", enabled = ready, testTag = "save_pdf_button") {
                            viewModel.saveAsPdf(viewModel.suggestedTitle())
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        val interaction = remember { MutableInteractionSource() }
                        Button(
                            onClick = {
                                haptics(Haptic.CONFIRM)
                                onSendToGemini(promptToSend)
                            },
                            enabled = ready,
                            interactionSource = interaction,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = OmniAccent,
                                contentColor = OmniOnAccent,
                                disabledContainerColor = OmniSurface2,
                                disabledContentColor = OmniTextMuted
                            ),
                            shape = RoundedCornerShape(RadiusSm),
                            modifier = Modifier
                                .height(52.dp)
                                .pressScale(interaction)
                                .testTag("send_to_gemini_button")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Gemini", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun SquareAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interaction,
        shape = RoundedCornerShape(RadiusSm),
        color = OmniSurface2,
        modifier = Modifier
            .height(52.dp)
            .widthIn(min = 52.dp)
            .pressScale(interaction, 0.9f)
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = label, tint = if (enabled) OmniText else OmniTextMuted.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
            Text(label, fontSize = 11.sp, color = if (enabled) OmniTextMuted else OmniTextMuted.copy(alpha = 0.5f), maxLines = 1)
        }
    }
}

@Composable
private fun DocumentChip(
    item: MarkdownSourceItem,
    onRemove: () -> Unit,
    onParseRemaining: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasError = item.error != null
    Surface(
        shape = RoundedCornerShape(RadiusPill),
        color = if (hasError) OmniDanger.copy(alpha = 0.1f) else OmniAccentSoft,
        border = BorderStroke(1.dp, if (hasError) OmniDanger.copy(alpha = 0.4f) else OmniAccent.copy(alpha = 0.25f)),
        modifier = modifier.animateContentSize()
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (hasError) {
                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = OmniDanger, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
            }
            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                Text(
                    item.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OmniText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 180.dp)
                )
                Text(
                    text = when {
                        hasError -> item.error.orEmpty()
                        item.hasMorePages -> "${item.parsedPages}/${item.pageCount} pagine · tocca per il resto"
                        item.pageCount > 1 -> "${item.pageCount} pagine · ~${item.estimatedTokens} token"
                        else -> "~${item.estimatedTokens} token"
                    },
                    fontSize = 11.sp,
                    color = if (hasError) OmniDanger else OmniTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 200.dp)
                )
            }
            if (item.hasMorePages) {
                IconButton(onClick = onParseRemaining, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.AutoMirrored.Filled.NoteAdd, contentDescription = "Estrai le pagine rimanenti", tint = OmniAccent, modifier = Modifier.size(16.dp))
                }
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Rimuovi", tint = OmniTextMuted, modifier = Modifier.size(16.dp))
            }
        }
    }
}
