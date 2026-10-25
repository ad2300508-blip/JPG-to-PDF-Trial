package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
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
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.example.model.GeminiModelTier
import com.example.model.GeminiPromptPreset
import com.example.model.GeminiTargetModel
import com.example.model.ModelQuotaUsage
import com.example.ui.components.OmniBadge
import com.example.ui.components.OmniCard
import com.example.ui.components.OmniErrorBanner
import com.example.ui.components.OmniHeader
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
            .padding(16.dp)
            .testTag("gemini_hub_screen")
    ) {
        // Top Header
        OmniHeader(
            workspaceTitle = "Gemini AI Hub",
            categoryBadge = "Free-Tier Multi-Model Suite",
            badgeColor = CyanAccent,
            itemCount = GeminiTargetModel.values().size,
            isProcessing = genState.isGenerating,
            actions = {
                // Key Vault Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (apiKeyStatus == "CONNECTED") EmeraldAccent.copy(alpha = 0.15f) else AmberAccent.copy(alpha = 0.15f))
                        .border(1.dp, if (apiKeyStatus == "CONNECTED") EmeraldAccent.copy(alpha = 0.4f) else AmberAccent.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .clickable { viewModel.openApiKeyDialog() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("api_key_status_pill")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = if (apiKeyStatus == "CONNECTED") EmeraldAccent else AmberAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (apiKeyStatus == "CONNECTED") "CONNECTED (${latency ?: 0}ms)" else if (apiKeyStatus == "TESTING") "TESTING..." else "KEY VAULT",
                            color = if (apiKeyStatus == "CONNECTED") EmeraldAccent else AmberAccent,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(onClick = { viewModel.testApiKey(); viewModel.refreshQuotaUsages() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh Status", tint = Slate400, modifier = Modifier.size(20.dp))
                }
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Top Quota & Model Telemetry Bar
        TopQuotaTelemetryBar(
            selectedModel = selectedModel,
            smartRouting = smartRouting,
            quotaUsages = quotaUsages,
            estimatedTokens = genState.estimatedTokens,
            onToggleSmartRouting = { viewModel.toggleSmartRouting(it) },
            onSelectModel = { viewModel.selectModel(it) }
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (genState.error != null) {
            OmniErrorBanner(message = genState.error!!, onDismiss = { viewModel.clearOutput() })
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (genState.fallbackNotice != null) {
            OmniCard(
                borderColor = AmberAccent.copy(alpha = 0.6f),
                backgroundColor = AmberAccent.copy(alpha = 0.1f),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = genState.fallbackNotice!!,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = AmberAccent,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (copyNotice != null) {
            OmniSuccessBanner(message = copyNotice!!, onDismiss = { viewModel.dismissCopiedNotice() })
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Responsive Dual Pane / 2-Tab Switcher
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val isTablet = maxWidth >= 600.dp
            if (isTablet) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(0.40f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceCard)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        GeminiStudyToolsPane(
                            selectedModel = selectedModel,
                            inputText = inputText,
                            contextDocument = contextDocument,
                            audioUri = audioUri,
                            imageUri = imageUri,
                            isGenerating = genState.isGenerating,
                            pipelines = viewModel.studyPipelines,
                            onSelectModel = { viewModel.selectModel(it) },
                            onClearContextDocument = { viewModel.clearContextDocument() },
                            onClearAudioAttachment = { viewModel.clearAudioAttachment() },
                            onClearImageAttachment = { viewModel.clearImageAttachment() },
                            onPickImage = { imagePicker.launch("image/*") },
                            onApplyPipeline = { viewModel.applyPreset(it) },
                            onUpdateInputText = { viewModel.updateInputText(it) },
                            onGenerate = { viewModel.generateCurrent() }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(0.60f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceCard)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                            .padding(18.dp)
                    ) {
                        GeminiOutputAndStreamPane(
                            genState = genState,
                            showThinkingTrace = showThinkingTrace,
                            onToggleThinkingTrace = { showThinkingTrace = !showThinkingTrace },
                            onStopGeneration = { viewModel.stopGeneration() },
                            onCopyOutput = { viewModel.copyOutputToClipboard() },
                            onExportMarkdown = { viewModel.exportMarkdownFile() },
                            onConvertToPdf = { viewModel.convertOutputToPdf() },
                            onNavigateToMarkdown = onNavigateToMarkdown,
                            onClearOutput = { viewModel.clearOutput() }
                        )
                    }
                }
            } else {
                var selectedPhoneTab by rememberSaveable { mutableIntStateOf(0) }
                Column(modifier = Modifier.fillMaxSize()) {
                    OmniPhoneTabSelector(
                        tabs = listOf("Study Tools & Input", "Output & Canvas"),
                        selectedTabIndex = selectedPhoneTab,
                        onTabSelected = { selectedPhoneTab = it },
                        accentColor = CyanAccent
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceCard)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        if (selectedPhoneTab == 0) {
                            GeminiStudyToolsPane(
                                selectedModel = selectedModel,
                                inputText = inputText,
                                contextDocument = contextDocument,
                                audioUri = audioUri,
                                imageUri = imageUri,
                                isGenerating = genState.isGenerating,
                                pipelines = viewModel.studyPipelines,
                                onSelectModel = { viewModel.selectModel(it) },
                                onClearContextDocument = { viewModel.clearContextDocument() },
                                onClearAudioAttachment = { viewModel.clearAudioAttachment() },
                                onClearImageAttachment = { viewModel.clearImageAttachment() },
                                onPickImage = { imagePicker.launch("image/*") },
                                onApplyPipeline = { viewModel.applyPreset(it) },
                                onUpdateInputText = { viewModel.updateInputText(it) },
                                onGenerate = { viewModel.generateCurrent() }
                            )
                        } else {
                            GeminiOutputAndStreamPane(
                                genState = genState,
                                showThinkingTrace = showThinkingTrace,
                                onToggleThinkingTrace = { showThinkingTrace = !showThinkingTrace },
                                onStopGeneration = { viewModel.stopGeneration() },
                                onCopyOutput = { viewModel.copyOutputToClipboard() },
                                onExportMarkdown = { viewModel.exportMarkdownFile() },
                                onConvertToPdf = { viewModel.convertOutputToPdf() },
                                onNavigateToMarkdown = onNavigateToMarkdown,
                                onClearOutput = { viewModel.clearOutput() }
                            )
                        }
                    }
                }
            }
        }

        // In-App API Key Configuration Dialog (Key Vault)
        if (showKeyDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.closeApiKeyDialog() },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Key, contentDescription = null, tint = CyanAccent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Gemini API Key Vault",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                text = {
                    Column {
                        Text(
                            text = "Configure your Google AI Studio Free-Tier API Key. Unlocks all 10 high-speed models with smart rate-limit fallback.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Slate400
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Masked preview pill
                        OmniCard(borderColor = BorderSubtle, backgroundColor = SurfaceElevated) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("CURRENT STORED KEY", style = MaterialTheme.typography.labelSmall, color = Slate400)
                                    Text(maskedApiKey, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = CyanAccent, fontSize = 12.sp)
                                }
                                OutlinedButton(
                                    onClick = { viewModel.testApiKey() },
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text("Test Key", fontSize = 11.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = keyInputValue,
                            onValueChange = { keyInputValue = it },
                            label = { Text("Update API Key") },
                            placeholder = { Text("AIzaSy...") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("api_key_dialog_input")
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Keys can also be configured in AI Studio Secrets as GEMINI_API_KEY.",
                            fontSize = 11.sp,
                            color = Slate400
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.saveApiKey(keyInputValue) },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black),
                        modifier = Modifier.testTag("save_api_key_dialog_button")
                    ) {
                        Text("Save & Connect")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.closeApiKeyDialog() }) {
                        Text("Cancel", color = Slate400)
                    }
                },
                containerColor = SurfaceCard,
                shape = RoundedCornerShape(14.dp)
            )
        }
    }
}

@Composable
private fun TopQuotaTelemetryBar(
    selectedModel: GeminiTargetModel,
    smartRouting: Boolean,
    quotaUsages: List<ModelQuotaUsage>,
    estimatedTokens: Int,
    onToggleSmartRouting: (Boolean) -> Unit,
    onSelectModel: (GeminiTargetModel) -> Unit
) {
    var modelMenuExpanded by remember { mutableStateOf(false) }

    OmniCard(borderColor = BorderSubtle, backgroundColor = SurfaceElevated) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Row 1: Model selector & Auto-Router Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Model Dropdown Button
                Box {
                    Surface(
                        onClick = { modelMenuExpanded = true },
                        shape = RoundedCornerShape(8.dp),
                        color = SurfaceCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = selectedModel.displayName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = CyanAccent,
                                maxLines = 1,
                                softWrap = false
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                        }
                    }

                    DropdownMenu(
                        expanded = modelMenuExpanded,
                        onDismissRequest = { modelMenuExpanded = false }
                    ) {
                        GeminiTargetModel.values().forEach { model ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(model.displayName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            OmniBadge(text = model.categoryBadge, color = CyanAccent)
                                        }
                                        Text(model.tpmQuotaLabel, fontSize = 10.sp, color = Slate400)
                                    }
                                },
                                onClick = {
                                    onSelectModel(model)
                                    modelMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // Smart Router Switch
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (smartRouting) "SMART ROUTER: ON" else "SMART ROUTER: OFF",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (smartRouting) EmeraldAccent else Slate400,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text("HTTP 429 Auto-Fallback", fontSize = 9.sp, color = Slate400)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = smartRouting,
                        onCheckedChange = onToggleSmartRouting,
                        colors = SwitchDefaults.colors(checkedThumbColor = EmeraldAccent, checkedTrackColor = Slate800)
                    )
                }
            }

            // Row 2: Live Horizontal Quota Meter Pills & Token Counter
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OmniStatPill(label = "Active Token Load", value = "$estimatedTokens / 250K TPM", valueColor = CyanAccent)

                quotaUsages.forEach { usage ->
                    val isCurrent = usage.model == selectedModel
                    val pillColor = if (usage.isThrottled) RoseAccent else if (isCurrent) CyanAccent else Slate400
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(SurfaceCard)
                            .border(1.dp, pillColor.copy(alpha = if (isCurrent) 0.6f else 0.2f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${usage.model.technicalModelId}: ${usage.usedRpd}/${usage.model.maxRpd} RPD",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            color = pillColor,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GeminiStudyToolsPane(
    selectedModel: GeminiTargetModel,
    inputText: String,
    contextDocument: String?,
    audioUri: Uri?,
    imageUri: Uri?,
    isGenerating: Boolean,
    pipelines: List<GeminiPromptPreset>,
    onSelectModel: (GeminiTargetModel) -> Unit,
    onClearContextDocument: () -> Unit,
    onClearAudioAttachment: () -> Unit,
    onClearImageAttachment: () -> Unit,
    onPickImage: () -> Unit,
    onApplyPipeline: (GeminiPromptPreset) -> Unit,
    onUpdateInputText: (String) -> Unit,
    onGenerate: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Section Header: 1-Tap Document & Study Pipelines
        Text(
            text = "1-TAP STUDY & DOCUMENT AI PIPELINES",
            style = MaterialTheme.typography.labelSmall,
            color = Slate400,
            maxLines = 1,
            softWrap = false
        )

        // 5 Study Pipelines
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            pipelines.forEach { pipeline ->
                val icon = when (pipeline.id) {
                    "ai_ocr_post_correction" -> Icons.Default.Description
                    "executive_summary_concepts" -> Icons.Default.AutoAwesome
                    "oral_exam_simulator" -> Icons.Default.QuestionAnswer
                    "flashcards_concept_map" -> Icons.Default.TableChart
                    "vision_inspector" -> Icons.Default.Image
                    else -> Icons.Default.Psychology
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceElevated)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                        .clickable { onApplyPipeline(pipeline) }
                        .padding(10.dp)
                        .testTag("pipeline_${pipeline.id}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Slate800),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = pipeline.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = pipeline.subtitle,
                                    fontSize = 10.sp,
                                    color = Slate400,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        // Context Deck (Markdown Context, Audio, or Image)
        if (contextDocument != null || audioUri != null || imageUri != null) {
            OmniCard(borderColor = CyanAccent.copy(alpha = 0.5f), backgroundColor = SurfaceElevated) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when {
                                imageUri != null -> "VISION ATTACHMENT LOADED"
                                audioUri != null -> "AUDIO ATTACHMENT LOADED"
                                else -> "EXTRACTED DOCUMENT CONTEXT"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanAccent,
                            maxLines = 1,
                            softWrap = false
                        )
                        IconButton(
                            onClick = {
                                onClearContextDocument()
                                onClearAudioAttachment()
                                onClearImageAttachment()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = Slate400, modifier = Modifier.size(14.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    if (imageUri != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Slate800)
                            ) {
                                AsyncImage(
                                    model = imageUri,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Image ready for Vision Inspector analysis", fontSize = 11.sp, color = Slate300)
                        }
                    } else if (audioUri != null) {
                        Text("Audio track ready for Gemini 3.8 Transcribe", fontSize = 11.sp, color = Slate300)
                    } else {
                        Text("Context loaded from Markdown Exporter (~${(contextDocument?.length ?: 0) / 4} tokens)", fontSize = 11.sp, color = Slate300)
                    }
                }
            }
        }

        // Multimodal Attachment Bar (Attach diagram/page)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("CUSTOM PROMPT & QUERY", style = MaterialTheme.typography.labelSmall, color = Slate400)

            OutlinedButton(
                onClick = onPickImage,
                modifier = Modifier.height(32.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Attach Image", fontSize = 10.sp, maxLines = 1, softWrap = false)
            }
        }

        OutlinedTextField(
            value = inputText,
            onValueChange = onUpdateInputText,
            placeholder = { Text("Ask study questions, request summaries, or give instructions...", color = Slate400, fontSize = 12.sp) },
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .testTag("gemini_prompt_input")
        )

        Button(
            onClick = onGenerate,
            enabled = !isGenerating && (inputText.isNotBlank() || contextDocument != null || imageUri != null),
            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("run_gemini_button")
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "EXECUTE WITH ${selectedModel.displayName.uppercase()}",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun GeminiOutputAndStreamPane(
    genState: GeminiGenerationState,
    showThinkingTrace: Boolean,
    onToggleThinkingTrace: () -> Unit,
    onStopGeneration: () -> Unit,
    onCopyOutput: () -> Unit,
    onExportMarkdown: () -> Unit,
    onConvertToPdf: () -> Unit,
    onNavigateToMarkdown: (String) -> Unit,
    onClearOutput: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Telemetry strip
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OmniBadge(text = genState.modelUsed.displayName, color = CyanAccent)
                    Spacer(modifier = Modifier.width(6.dp))
                    if (genState.isGenerating) {
                        OmniBadge(text = "STREAMING", color = AmberAccent)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OmniStatPill(label = "Tokens", value = "~${genState.estimatedTokens}")
                    if (genState.latencyMs > 0) {
                        OmniStatPill(label = "Latency", value = "${genState.latencyMs}ms", valueColor = Slate400)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Thinking process trace
            if (genState.thinkingTrace.isNotEmpty()) {
                OmniCard(
                    borderColor = AmberAccent.copy(alpha = 0.4f),
                    backgroundColor = SurfaceElevated,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleThinkingTrace() },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Psychology, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "EXTENDED COGNITIVE THINKING TRACE",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = AmberAccent,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                imageVector = if (showThinkingTrace) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = AmberAccent
                            )
                        }

                        AnimatedVisibility(
                            visible = showThinkingTrace,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                                    .padding(top = 8.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Slate800)
                                    .padding(8.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = genState.thinkingTrace,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = Slate300,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Markdown Output Display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceElevated)
                .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                .padding(14.dp)
        ) {
            if (genState.outputText.isEmpty() && !genState.isGenerating) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Slate400, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Ready to Analyze & Study",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Select one of the 5 1-Tap Pipelines above (OCR Post-Correction, Executive Summary, Oral Exam, Flashcards, or Vision Inspector).",
                        fontSize = 12.sp,
                        color = Slate400,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(0.85f)
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = genState.outputText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 22.sp
                    )
                    if (genState.isGenerating) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "▋",
                            color = CyanAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action Canvas Bar
        if (genState.isGenerating) {
            Button(
                onClick = onStopGeneration,
                colors = ButtonDefaults.buttonColors(containerColor = RoseAccent, contentColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("stop_generation_button")
            ) {
                Icon(Icons.Default.Stop, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("STOP GENERATION", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onCopyOutput,
                    enabled = genState.outputText.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .height(44.dp)
                        .testTag("copy_gemini_output_button")
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("COPY", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1, softWrap = false)
                }

                Button(
                    onClick = onExportMarkdown,
                    enabled = genState.outputText.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated, contentColor = CyanAccent),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .height(44.dp)
                        .testTag("export_md_button")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("EXPORT .MD", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1, softWrap = false)
                }

                Button(
                    onClick = onConvertToPdf,
                    enabled = genState.outputText.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated, contentColor = AmberAccent),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AmberAccent.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .height(44.dp)
                        .testTag("convert_study_guide_pdf_button")
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("CONVERT TO PDF", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1, softWrap = false)
                }

                Button(
                    onClick = {
                        if (genState.outputText.isNotEmpty()) {
                            onNavigateToMarkdown(genState.outputText)
                        }
                    },
                    enabled = genState.outputText.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated, contentColor = Slate300),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(44.dp)
                ) {
                    Text("TO MARKDOWN EXPORTER", fontSize = 11.sp, maxLines = 1, softWrap = false)
                }

                OutlinedButton(
                    onClick = onClearOutput,
                    enabled = genState.outputText.isNotEmpty(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate400),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(44.dp)
                ) {
                    Text("CLEAR", fontSize = 11.sp, maxLines = 1, softWrap = false)
                }
            }
        }
    }
}
