package com.example.ui

import com.example.ui.theme.LocalReduceMotion
import com.example.ui.components.MarkdownText
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.scaleIn
import com.example.ui.theme.pressScale
import com.example.ui.theme.enterAnimation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.example.ui.components.RevealVertically
import androidx.compose.foundation.horizontalScroll
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedContent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import coil.compose.AsyncImage
import com.example.model.GeminiGenerationState
import com.example.model.GeminiPromptPreset
import com.example.model.GeminiTargetModel
import com.example.model.ModelQuotaUsage
import com.example.ui.components.OmniToast
import com.example.ui.components.SegmentedControl
import com.example.ui.components.ToolHeader
import com.example.ui.theme.OmniAccent
import com.example.ui.theme.OmniAccentSoft
import com.example.ui.theme.OmniBorder
import com.example.ui.theme.OmniDanger
import com.example.ui.theme.OmniOnAccent
import com.example.ui.theme.OmniSurface
import com.example.ui.theme.OmniSurface2
import com.example.ui.theme.OmniText
import com.example.ui.theme.OmniTextMuted
import com.example.ui.theme.RadiusCard
import com.example.ui.theme.RadiusMd
import com.example.ui.theme.RadiusSm
import com.example.ui.theme.TintAi
import com.example.viewmodel.GeminiHubViewModel

@Composable
fun GeminiHubScreen(
    viewModel: GeminiHubViewModel,
    onNavigateToMarkdown: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val selectedModel by viewModel.selectedModel.collectAsState()
    val smartRouting by viewModel.smartRouting.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val contextDocument by viewModel.contextDocument.collectAsState()
    val audioUri by viewModel.audioAttachmentUri.collectAsState()
    val imageUri by viewModel.imageAttachmentUri.collectAsState()
    val genState by viewModel.generationState.collectAsState()
    val quotaUsages by viewModel.quotaUsages.collectAsState()
    val apiKeyStatus by viewModel.apiKeyStatus.collectAsState()
    val latency by viewModel.connectionLatency.collectAsState()
    val maskedApiKey by viewModel.maskedApiKey.collectAsState()
    val showKeyDialog by viewModel.showApiKeyDialog.collectAsState()
    val keyInput by viewModel.apiKeyInput.collectAsState()
    val copyNotice by viewModel.copiedNotice.collectAsState()

    var showModelMenu by remember { mutableStateOf(false) }
    var showUsageModal by remember { mutableStateOf(false) }
    var showThinkingTrace by remember { mutableStateOf(true) }
    var keyInputValue by remember(keyInput) { mutableStateOf(keyInput) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.attachImage(uri)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("gemini_hub_screen")
    ) {
        // Page Header: title (28px) + one muted line of description
        ToolHeader(
            title = "Gemini",
            description = "Motore AI con fallback automatico e pipeline di studio."
        )

        // Control strip: Model picker dropdown + Usage popover + Smart routing pill + Key vault button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Model picker dropdown
            Box {
                Surface(
                    onClick = { showModelMenu = true },
                    shape = RoundedCornerShape(RadiusSm),
                    color = OmniSurface2,
                    border = BorderStroke(1.dp, OmniBorder),
                    modifier = Modifier
                        .height(44.dp)
                        .testTag("model_picker_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedModel.displayName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OmniText
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = OmniAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = showModelMenu,
                    onDismissRequest = { showModelMenu = false },
                    modifier = Modifier.background(OmniSurface).border(1.dp, OmniBorder, RoundedCornerShape(RadiusSm))
                ) {
                    GeminiTargetModel.values().forEach { model ->
                        DropdownMenuItem(
                            text = {
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Text(
                                        text = model.displayName,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (model == selectedModel) OmniAccent else OmniText
                                    )
                                    Text(
                                        text = "${model.tier.label} · ${model.categoryBadge}",
                                        fontSize = 12.sp,
                                        color = OmniTextMuted
                                    )
                                }
                            },
                            onClick = {
                                viewModel.selectModel(model)
                                showModelMenu = false
                            }
                        )
                    }
                }
            }

            // Right utility actions: Usage popover + Smart Routing toggle + Key Vault
            Spacer(modifier = Modifier.width(8.dp))
            Row(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Usage popover button
                OutlinedButton(
                    onClick = { showUsageModal = true },
                    modifier = Modifier
                        .height(44.dp)
                        .testTag("usage_popover_button"),
                    border = BorderStroke(1.dp, OmniBorder),
                    shape = RoundedCornerShape(RadiusSm),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = OmniText)
                ) {
                    Icon(Icons.Default.DataUsage, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Utilizzo", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }

                // Smart Router Toggle
                val routingBg by animateColorAsState(if (smartRouting) OmniAccentSoft else OmniSurface2, label = "routing_bg")
                val routingBorder by animateColorAsState(if (smartRouting) OmniAccent else OmniBorder, label = "routing_border")
                Surface(
                    onClick = { viewModel.toggleSmartRouting(!smartRouting) },
                    shape = RoundedCornerShape(RadiusSm),
                    color = routingBg,
                    border = BorderStroke(1.dp, routingBorder),
                    modifier = Modifier
                        .height(44.dp)
                        .testTag("smart_router_toggle")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = if (smartRouting) OmniAccent else OmniTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (smartRouting) "Fallback automatico" else "Manuale",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (smartRouting) OmniAccent else OmniTextMuted
                        )
                    }
                }

                // API Key Vault Button
                Surface(
                    onClick = { viewModel.openApiKeyDialog() },
                    shape = RoundedCornerShape(RadiusSm),
                    color = OmniSurface2,
                    border = BorderStroke(1.dp, OmniBorder),
                    modifier = Modifier
                        .height(44.dp)
                        .testTag("api_key_status_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = if (apiKeyStatus == "CONNECTED") OmniAccent else OmniTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (apiKeyStatus == "CONNECTED") "Chiave collegata" else "API Key",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (apiKeyStatus == "CONNECTED") OmniAccent else OmniText
                        )
                    }
                }
            }
        }

        // Banners
        RevealVertically(visible = genState.error != null) {
            OmniToast(
                message = genState.error.orEmpty(),
                isError = true,
                actionLabel = "Chiudi",
                onAction = { viewModel.dismissError() },
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        RevealVertically(visible = genState.fallbackNotice != null) {
            OmniToast(
                message = genState.fallbackNotice.orEmpty(),
                isError = false,
                actionLabel = "OK",
                onAction = { viewModel.dismissFallbackNotice() },
                modifier = Modifier.padding(bottom = 8.dp),
                autoDismissMillis = 6000L
            )
        }
        RevealVertically(visible = copyNotice != null) {
            OmniToast(
                message = copyNotice.orEmpty(),
                isError = false,
                actionLabel = "OK",
                onAction = { viewModel.dismissCopiedNotice() },
                modifier = Modifier.padding(bottom = 8.dp),
                autoDismissMillis = 4000L
            )
        }

        // Main Responsive Layout: Tablet Dual-Pane vs Phone 2-Tab Switcher
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val isTablet = maxWidth >= 600.dp

            if (isTablet) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Left Column: Pipelines & Prompt input (max 380dp)
                    Box(
                        modifier = Modifier
                            .width(360.dp)
                            .fillMaxHeight()
                    ) {
                        GeminiInputAndPipelinesPane(
                            pipelines = viewModel.studyPipelines,
                            inputText = inputText,
                            contextDocument = contextDocument,
                            imageUri = imageUri,
                            audioUri = audioUri,
                            isGenerating = genState.isGenerating,
                            selectedModel = selectedModel,
                            onUpdateInputText = { viewModel.updateInputText(it) },
                            onPickImage = { imagePicker.launch("image/*") },
                            onClearImage = { viewModel.clearImageAttachment() },
                            onClearAudio = { viewModel.clearAudioAttachment() },
                            onClearContext = { viewModel.clearContextDocument() },
                            onRunPipeline = { viewModel.applyPreset(it) },
                            onGenerate = { viewModel.generateCurrent() }
                        )
                    }

                    // Right Column: Output & Actions
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        GeminiOutputCanvas(
                            genState = genState,
                            showThinkingTrace = showThinkingTrace,
                            onToggleThinkingTrace = { showThinkingTrace = !showThinkingTrace },
                            onStopGeneration = { viewModel.stopGeneration() },
                            onCopyOutput = { viewModel.copyOutputToClipboard() },
                            onExportMarkdown = { viewModel.exportMarkdownFile() },
                            onConvertToPdf = { viewModel.convertOutputToPdf() },
                            onClearOutput = { viewModel.clearOutput() }
                        )
                    }
                }
            } else {
                // Phone 2-Tab Switcher
                var phoneTab by rememberSaveable { mutableIntStateOf(0) }

                Column(modifier = Modifier.fillMaxSize()) {
                    SegmentedControl(
                        options = listOf("Pipeline e prompt", "Risposta AI"),
                        selectedIndex = phoneTab,
                        onOptionSelected = { phoneTab = it },
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    AnimatedContent(
                        targetState = phoneTab,
                        transitionSpec = { stageTransition(initialState, targetState) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        label = "gemini_phone_tab"
                    ) { tabIndex ->
                        if (tabIndex == 0) {
                            GeminiInputAndPipelinesPane(
                                pipelines = viewModel.studyPipelines,
                                inputText = inputText,
                                contextDocument = contextDocument,
                                imageUri = imageUri,
                                audioUri = audioUri,
                                isGenerating = genState.isGenerating,
                                selectedModel = selectedModel,
                                onUpdateInputText = { viewModel.updateInputText(it) },
                                onPickImage = { imagePicker.launch("image/*") },
                                onClearImage = { viewModel.clearImageAttachment() },
                                onClearAudio = { viewModel.clearAudioAttachment() },
                                onClearContext = { viewModel.clearContextDocument() },
                                onRunPipeline = {
                                    viewModel.applyPreset(it)
                                    phoneTab = 1
                                },
                                onGenerate = {
                                    viewModel.generateCurrent()
                                    phoneTab = 1
                                }
                            )
                        } else {
                            GeminiOutputCanvas(
                                genState = genState,
                                showThinkingTrace = showThinkingTrace,
                                onToggleThinkingTrace = { showThinkingTrace = !showThinkingTrace },
                                onStopGeneration = { viewModel.stopGeneration() },
                                onCopyOutput = { viewModel.copyOutputToClipboard() },
                                onExportMarkdown = { viewModel.exportMarkdownFile() },
                                onConvertToPdf = { viewModel.convertOutputToPdf() },
                                onClearOutput = { viewModel.clearOutput() }
                            )
                        }
                    }
                }
            }
        }
    }

    // Usage Popover Modal
    if (showUsageModal) {
        AlertDialog(
            onDismissRequest = { showUsageModal = false },
            containerColor = OmniSurface,
            shape = RoundedCornerShape(RadiusCard),
            title = {
                Text(
                    text = "Quota gratuita e utilizzo",
                    style = MaterialTheme.typography.titleLarge,
                    color = OmniText
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Utilizzo in tempo reale dei 10 modelli gratuiti (si azzera ogni giorno alle 00:00 UTC).",
                        fontSize = 13.sp,
                        color = OmniTextMuted
                    )

                    quotaUsages.forEach { usage ->
                        Surface(
                            shape = RoundedCornerShape(RadiusSm),
                            color = OmniSurface2,
                            border = BorderStroke(1.dp, OmniBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = usage.model.displayName,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = OmniText
                                    )
                                    Text(
                                        text = usage.model.tpmQuotaLabel,
                                        fontSize = 12.sp,
                                        color = OmniTextMuted
                                    )
                                }
                                Text(
                                    text = "${usage.usedRpd} / ${usage.model.maxRpd} RPD",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (usage.isThrottled) OmniDanger else OmniAccent
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showUsageModal = false },
                    colors = ButtonDefaults.buttonColors(containerColor = OmniAccent, contentColor = OmniOnAccent),
                    shape = RoundedCornerShape(RadiusSm)
                ) {
                    Text("Chiudi", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    // API Key Vault Dialog
    if (showKeyDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.closeApiKeyDialog() },
            containerColor = OmniSurface,
            shape = RoundedCornerShape(RadiusCard),
            title = {
                Text(
                    text = "Chiave API Gemini",
                    style = MaterialTheme.typography.titleLarge,
                    color = OmniText
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Inserisci la tua chiave API di Google AI Studio. Viene salvata solo su questo dispositivo.",
                        fontSize = 13.sp,
                        color = OmniTextMuted
                    )

                    if (maskedApiKey.isNotEmpty()) {
                        Text(
                            text = "Chiave attuale: $maskedApiKey",
                            fontSize = 13.sp,
                            color = OmniAccent,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    OutlinedTextField(
                        value = keyInputValue,
                        onValueChange = { keyInputValue = it },
                        label = { Text("Incolla la chiave API (AIzaSy...)", fontSize = 14.sp) },
                        placeholder = { Text("AIzaSy...", fontSize = 14.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("api_key_input_field"),
                        shape = RoundedCornerShape(RadiusSm)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveApiKey(keyInputValue)
                        viewModel.testApiKey()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OmniAccent, contentColor = OmniOnAccent),
                    shape = RoundedCornerShape(RadiusSm)
                ) {
                    Text("Verifica e salva", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeApiKeyDialog() }) {
                    Text("Annulla", color = OmniTextMuted, fontSize = 14.sp)
                }
            }
        )
    }
}

@Composable
private fun GeminiInputAndPipelinesPane(
    pipelines: List<GeminiPromptPreset>,
    inputText: String,
    contextDocument: String?,
    imageUri: Uri?,
    audioUri: Uri?,
    isGenerating: Boolean,
    selectedModel: GeminiTargetModel,
    onUpdateInputText: (String) -> Unit,
    onPickImage: () -> Unit,
    onClearImage: () -> Unit,
    onClearAudio: () -> Unit,
    onClearContext: () -> Unit,
    onRunPipeline: (GeminiPromptPreset) -> Unit,
    onGenerate: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Pipeline di studio",
            style = MaterialTheme.typography.titleMedium,
            color = OmniText
        )

        // 5 Pipeline cards: icon in tinted tile, title, one-line description, 64px high
        pipelines.forEachIndexed { index, preset ->
            val icon = when (preset.id) {
                "ai_ocr_post_correction" -> Icons.Default.AutoAwesome
                "executive_summary" -> Icons.Default.Description
                "oral_exam_simulator" -> Icons.Default.QuestionAnswer
                "flashcards_concept_map" -> Icons.Default.TableChart
                else -> Icons.Default.Psychology
            }

            val pipelineInteraction = remember { MutableInteractionSource() }
            Surface(
                onClick = { onRunPipeline(preset) },
                enabled = !isGenerating,
                interactionSource = pipelineInteraction,
                shape = RoundedCornerShape(RadiusMd),
                color = OmniSurface2,
                border = BorderStroke(1.dp, OmniBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .enterAnimation(index)
                    .pressScale(pipelineInteraction, 0.97f)
                    .testTag("pipeline_preset_${preset.id}")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(TintAi.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = OmniAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = preset.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OmniText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = preset.subtitle,
                            fontSize = 13.sp,
                            color = OmniTextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Context Attachment Indicators
        RevealVertically(visible = contextDocument != null || imageUri != null || audioUri != null) {
            Surface(
                shape = RoundedCornerShape(RadiusSm),
                color = OmniSurface2,
                border = BorderStroke(1.dp, OmniBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (imageUri != null) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(OmniSurface)
                            ) {
                                AsyncImage(model = imageUri, contentDescription = null, modifier = Modifier.fillMaxSize())
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Immagine allegata", fontSize = 13.sp, color = OmniAccent)
                        } else if (audioUri != null) {
                            Icon(Icons.Default.GraphicEq, contentDescription = null, tint = OmniAccent, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Audio pronto per la trascrizione", fontSize = 13.sp, color = OmniAccent)
                        } else {
                            Icon(Icons.Default.Description, contentDescription = null, tint = OmniAccent, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Documento di contesto caricato", fontSize = 13.sp, color = OmniAccent)
                        }
                    }

                    IconButton(
                        onClick = {
                            if (imageUri != null) onClearImage()
                            else if (audioUri != null) onClearAudio()
                            else onClearContext()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Rimuovi", tint = OmniTextMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Custom Query prompt input
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Prompt / domanda",
                    fontSize = 14.sp,
                    color = OmniTextMuted,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                TextButton(
                    onClick = onPickImage,
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Allega", fontSize = 13.sp, color = OmniAccent)
                }
            }

            OutlinedTextField(
                value = inputText,
                onValueChange = onUpdateInputText,
                placeholder = { Text("Fai domande di studio o dai istruzioni precise…", color = OmniTextMuted, fontSize = 14.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .testTag("gemini_prompt_input"),
                shape = RoundedCornerShape(RadiusSm),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = OmniSurface2,
                    unfocusedContainerColor = OmniSurface2,
                    focusedBorderColor = OmniAccent,
                    unfocusedBorderColor = OmniBorder,
                    focusedTextColor = OmniText,
                    unfocusedTextColor = OmniText
                )
            )
        }

        Button(
            onClick = onGenerate,
            enabled = !isGenerating && (inputText.isNotBlank() || contextDocument != null || imageUri != null || audioUri != null),
            colors = ButtonDefaults.buttonColors(
                containerColor = OmniAccent,
                contentColor = OmniOnAccent,
                disabledContainerColor = OmniSurface2,
                disabledContentColor = OmniTextMuted
            ),
            shape = RoundedCornerShape(RadiusSm),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("run_gemini_button")
        ) {
            AnimatedContent(targetState = isGenerating, transitionSpec = { stageTransition(if (initialState) 1 else 0, if (targetState) 1 else 0) }, label = "run_label") { generating ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (generating) {
                        CircularProgressIndicator(color = OmniTextMuted, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (generating) "Gemini sta scrivendo…" else "Esegui", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun GeminiOutputCanvas(
    genState: GeminiGenerationState,
    showThinkingTrace: Boolean,
    onToggleThinkingTrace: () -> Unit,
    onStopGeneration: () -> Unit,
    onCopyOutput: () -> Unit,
    onExportMarkdown: () -> Unit,
    onConvertToPdf: () -> Unit,
    onClearOutput: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(RadiusCard)),
        color = OmniSurface,
        border = BorderStroke(1.dp, OmniBorder),
        shape = RoundedCornerShape(RadiusCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Top action bar of the output canvas: Copy / Export / PDF icon buttons with labels
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Left telemetry tags
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = genState.modelUsed.displayName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OmniAccent
                    )
                    if (genState.latencyMs > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${genState.latencyMs} ms" + if (genState.estimatedTokens > 0) " · ~${genState.estimatedTokens} token" else "",
                            fontSize = 13.sp,
                            color = OmniTextMuted
                        )
                    }
                }

                // Result actions: Copy, Export, PDF
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onCopyOutput,
                        enabled = genState.outputText.isNotEmpty(),
                        modifier = Modifier.height(38.dp),
                        border = BorderStroke(1.dp, OmniBorder),
                        shape = RoundedCornerShape(RadiusSm),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = OmniText)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copia", fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = onExportMarkdown,
                        enabled = genState.outputText.isNotEmpty(),
                        modifier = Modifier.height(38.dp),
                        border = BorderStroke(1.dp, OmniBorder),
                        shape = RoundedCornerShape(RadiusSm),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = OmniText)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Esporta", fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = onConvertToPdf,
                        enabled = genState.outputText.isNotEmpty(),
                        modifier = Modifier.height(38.dp),
                        border = BorderStroke(1.dp, OmniBorder),
                        shape = RoundedCornerShape(RadiusSm),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = OmniText)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("PDF", fontSize = 13.sp)
                    }

                    AnimatedVisibility(visible = genState.isGenerating, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
                        Button(
                            onClick = onStopGeneration,
                            colors = ButtonDefaults.buttonColors(containerColor = OmniDanger, contentColor = Color.White),
                            shape = RoundedCornerShape(RadiusSm),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Annulla", fontSize = 13.sp)
                        }
                    }
                }
            }

            // Progress indicator while generating
            RevealVertically(visible = genState.isGenerating) {
                Column {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape),
                        color = OmniAccent,
                        trackColor = OmniSurface2
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            // Extended cognitive thinking trace collapsible
            if (genState.thinkingTrace.isNotEmpty()) {
                Surface(
                    onClick = onToggleThinkingTrace,
                    shape = RoundedCornerShape(RadiusSm),
                    color = OmniSurface2,
                    border = BorderStroke(1.dp, OmniBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Psychology, contentDescription = null, tint = OmniAccent, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Traccia del ragionamento",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = OmniAccent
                                )
                            }
                            Icon(
                                imageVector = if (showThinkingTrace) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = OmniTextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        AnimatedVisibility(visible = showThinkingTrace) {
                            Text(
                                text = genState.thinkingTrace,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = OmniTextMuted,
                                lineHeight = 16.sp,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }

            // Main output canvas area
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(RadiusSm)),
                color = OmniSurface2,
                border = BorderStroke(1.dp, OmniBorder),
                shape = RoundedCornerShape(RadiusSm)
            ) {
                val outputScroll = rememberScrollState()
                // Follow the answer while it streams in.
                LaunchedEffect(genState.outputText.length, genState.isGenerating) {
                    if (genState.isGenerating && outputScroll.maxValue > 0) outputScroll.animateScrollTo(outputScroll.maxValue)
                }
                if (genState.outputText.isEmpty() && genState.isGenerating) {
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(0.55f, 0.95f, 0.85f, 0.9f, 0.6f).forEachIndexed { index, fraction ->
                            SkeletonLine(fraction = fraction, delayMillis = index * 120)
                        }
                    }
                } else if (genState.outputText.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = OmniTextMuted,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Area risposta AI pronta",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OmniText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Scegli una pipeline di studio o scrivi un prompt per ottenere risposte, tabelle e schede.",
                            fontSize = 14.sp,
                            color = OmniTextMuted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.widthIn(max = 420.dp)
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(outputScroll)
                            .padding(14.dp)
                    ) {
                        SelectionContainer {
                            MarkdownText(markdown = genState.outputText, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
    }
}

/** Pulsing placeholder line shown while waiting for the first tokens. */
@Composable
private fun SkeletonLine(fraction: Float, delayMillis: Int) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(700, delayMillis = delayMillis), RepeatMode.Reverse),
        label = "skeleton_alpha"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth(fraction)
            .height(12.dp)
            .clip(CircleShape)
            .background(OmniBorder.copy(alpha = if (LocalReduceMotion.current) 0.6f else alpha))
    )
}
