package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LlmPromptWrapper
import com.example.model.MarkdownSourceItem
import com.example.ui.components.Dropzone
import com.example.ui.components.OmniProgressBanner
import com.example.ui.components.OmniToast
import com.example.ui.components.OptionRow
import com.example.ui.components.RadioCard
import com.example.ui.components.ToolLayout
import com.example.ui.theme.OmniAccent
import com.example.ui.theme.OmniBorder
import com.example.ui.theme.OmniOnAccent
import com.example.ui.theme.OmniSurface
import com.example.ui.theme.OmniSurface2
import com.example.ui.theme.OmniText
import com.example.ui.theme.OmniTextMuted
import com.example.ui.theme.RadiusCard
import com.example.ui.theme.RadiusMd
import com.example.ui.theme.RadiusSm
import com.example.ui.theme.TintAi
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

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addFiles(uris)
        }
    }

    val totalTokens = compiledResult?.totalTokens ?: 0
    val totalChars = compiledResult?.totalChars ?: 0
    val documentCount = compiledResult?.docCount ?: 0

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("markdown_exporter_screen")
    ) {
        if (errorNotice != null) {
            OmniToast(
                message = errorNotice!!,
                isError = true,
                actionLabel = "Chiudi",
                onAction = { viewModel.dismissErrorNotice() },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }

        if (copySuccess != null) {
            OmniToast(
                message = copySuccess!!,
                isError = false,
                actionLabel = "OK",
                onAction = { viewModel.dismissCopySuccess() },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }

        if (savedUri != null) {
            OmniToast(
                message = "File Markdown salvato in Download/OmniConvert/Markdown",
                isError = false,
                actionLabel = "Svuota",
                onAction = { viewModel.clearAll() },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }

        if (isIngesting) {
            OmniProgressBanner(
                progress = extractionProgress,
                statusText = extractionStatusText.ifBlank { "Estraggo e formatto il Markdown…" },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }

        ToolLayout(
            title = "PDF in Markdown",
            description = "Estrai testo pulito da PDF, appunti e foto",
            isEmpty = items.isEmpty(),
            phoneTabTitles = listOf("Anteprima", "Formati prompt"),
            emptyDropzone = {
                Dropzone(
                    icon = Icons.Default.SmartToy,
                    headline = "Aggiungi i tuoi documenti",
                    helpText = "Importa capitoli PDF, appunti o foto: estraggo un Markdown pulito pronto per l'AI",
                    supportedFormats = "PDF · TXT · MD · DOCX",
                    actionLabel = "Scegli file",
                    tintColor = TintAi,
                    onAction = { filePicker.launch("*/*") },
                    testTag = "markdown_add_docs_button"
                )
            },
            fileList = {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header row with count & Add More button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${items.size} documents loaded",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OmniText
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { filePicker.launch("*/*") },
                                modifier = Modifier
                                    .height(36.dp)
                                    .testTag("markdown_add_more_button"),
                                border = BorderStroke(1.dp, OmniBorder),
                                shape = RoundedCornerShape(RadiusSm),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = OmniAccent)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.NoteAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Aggiungi", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                            TextButton(
                                onClick = { viewModel.clearAll() },
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("Svuota", color = OmniTextMuted, fontSize = 13.sp)
                            }
                        }
                    }

                    // Monospace preview box with line wrap
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(RadiusMd)),
                        color = OmniSurface2,
                        border = BorderStroke(1.dp, OmniBorder),
                        shape = RoundedCornerShape(RadiusMd)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(14.dp)
                        ) {
                            Text(
                                text = compiledResult?.wrappedPrompt?.ifBlank { "Compilo il Markdown strutturato…" } ?: "Importa documenti per generare il Markdown",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                color = OmniText,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Stats in ONE muted line under preview
                    Text(
                        text = "$totalTokens tokens · $totalChars characters · $documentCount documents",
                        fontSize = 13.sp,
                        color = OmniTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
                    Text(
                        text = "Formato prompt AI",
                        style = MaterialTheme.typography.titleMedium,
                        color = OmniText
                    )

                    // Radio cards for wrappers
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
                            description = "Markdown pulito senza istruzioni specifiche per un modello",
                            isSelected = settings.promptWrapper == LlmPromptWrapper.RAW_MARKDOWN,
                            onClick = { viewModel.updateSettings(settings.copy(promptWrapper = LlmPromptWrapper.RAW_MARKDOWN)) }
                        )
                    }

                    OptionRow(
                        label = "Includi intestazione con metadati",
                        description = "Include nomi dei file e numero di pagine",
                        checked = settings.includeMetadataHeader,
                        onCheckedChange = { viewModel.updateSettings(settings.copy(includeMetadataHeader = it)) }
                    )

                    OptionRow(
                        label = "Genera indice",
                        description = "Crea un indice con i titoli principali",
                        checked = settings.includeTableOfContents,
                        onCheckedChange = { viewModel.updateSettings(settings.copy(includeTableOfContents = it)) }
                    )
                }
            },
            actionBar = {
                val promptToSend = compiledResult?.wrappedPrompt.orEmpty()
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(RadiusMd)),
                    color = OmniSurface,
                    border = BorderStroke(1.dp, OmniBorder),
                    shape = RoundedCornerShape(RadiusMd)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left secondary actions: Copy and Save
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { viewModel.copyPromptToClipboard() },
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("copy_markdown_button"),
                                border = BorderStroke(1.dp, OmniBorder),
                                shape = RoundedCornerShape(RadiusSm),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = OmniText)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copia", fontSize = 14.sp)
                            }

                            OutlinedButton(
                                onClick = { viewModel.saveAsMarkdown("Exported_Context") },
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("save_markdown_button"),
                                border = BorderStroke(1.dp, OmniBorder),
                                shape = RoundedCornerShape(RadiusSm),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = OmniText)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Salva .md", fontSize = 14.sp)
                            }
                        }

                        // Primary action: Send to AI Studio
                        Button(
                            onClick = {
                                onSendToGemini(promptToSend)
                            },
                            enabled = promptToSend.isNotBlank() && !isIngesting,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = OmniAccent,
                                contentColor = OmniOnAccent,
                                disabledContainerColor = OmniSurface2,
                                disabledContentColor = OmniTextMuted
                            ),
                            shape = RoundedCornerShape(RadiusSm),
                            modifier = Modifier
                                .height(52.dp)
                                .testTag("send_to_gemini_button")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Invia a Gemini", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        )
    }
}
