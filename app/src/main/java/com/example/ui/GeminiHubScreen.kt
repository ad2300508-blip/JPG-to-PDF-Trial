package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GeminiPromptPreset
import com.example.model.GeminiTargetModel
import com.example.ui.components.OmniBadge
import com.example.ui.components.OmniCard
import com.example.ui.components.OmniErrorBanner
import com.example.ui.components.OmniHeader
import com.example.ui.components.OmniLinearProgress
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
    val inputText by viewModel.inputText.collectAsState()
    val contextDocument by viewModel.contextDocument.collectAsState()
    val audioUri by viewModel.audioAttachmentUri.collectAsState()
    val genState by viewModel.generationState.collectAsState()
    val apiKeyStatus by viewModel.apiKeyStatus.collectAsState()
    val latency by viewModel.connectionLatency.collectAsState()
    val showKeyDialog by viewModel.showApiKeyDialog.collectAsState()
    val keyInput by viewModel.apiKeyInput.collectAsState()
    val copyNotice by viewModel.copiedNotice.collectAsState()

    var showThinkingTrace by remember { mutableStateOf(true) }
    var keyInputValue by remember(keyInput) { mutableStateOf(keyInput) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("gemini_hub_screen")
    ) {
        OmniHeader(
            workspaceTitle = "Gemini AI Hub",
            categoryBadge = "Google AI Studio Free Tier",
            badgeColor = CyanAccent,
            itemCount = GeminiTargetModel.values().size,
            isProcessing = genState.isGenerating,
            actions = {
                // API Key Status Pill
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
                            text = if (apiKeyStatus == "CONNECTED") "CONNECTED (${latency ?: 0}ms)" else if (apiKeyStatus == "TESTING") "TESTING..." else "KEY NEEDED",
                            color = if (apiKeyStatus == "CONNECTED") EmeraldAccent else AmberAccent,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(onClick = { viewModel.testApiKey() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh Status", tint = Slate400, modifier = Modifier.size(20.dp))
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (genState.error != null) {
            OmniErrorBanner(message = genState.error!!, onDismiss = { viewModel.clearOutput() })
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (copyNotice != null) {
            OmniSuccessBanner(message = copyNotice!!, onDismiss = { viewModel.dismissCopiedNotice() })
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Asymmetric Tablet Layout: Left 38% Model Selector & Presets, Right 62% Output & Streaming
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Left Pane (38%): Model Matrix & Action Presets
            Box(
                modifier = Modifier
                    .weight(0.38f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceCard)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "FREE QUOTA MODELS (RATE LIMITS)",
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate400
                    )

                    // 6 Target Models Grid
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        GeminiTargetModel.values().forEach { model ->
                            val isSelected = selectedModel == model
                            val accent = when (model) {
                                GeminiTargetModel.GEMINI_3_FLASH -> CyanAccent
                                GeminiTargetModel.GEMINI_3_8_THINKING -> AmberAccent
                                GeminiTargetModel.GEMINI_3_5_TRANSLATE -> EmeraldAccent
                                GeminiTargetModel.GEMINI_3_5_TRANSCRIBE -> IndigoAccent
                                GeminiTargetModel.GEMINI_2_5_FLASH_AUDIO -> CyanAccent
                                GeminiTargetModel.GEMINI_3_8_LIVE -> Color(0xFFE2E8F0)
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) accent.copy(alpha = 0.12f) else SurfaceElevated)
                                    .border(1.dp, if (isSelected) accent else BorderSubtle, RoundedCornerShape(10.dp))
                                    .clickable { viewModel.selectModel(model) }
                                    .padding(10.dp)
                                    .testTag("model_card_${model.name.lowercase()}")
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = model.displayName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (isSelected) accent else MaterialTheme.colorScheme.onSurface
                                        )
                                        OmniBadge(text = model.categoryBadge, color = accent)
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = model.tpmQuota,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = Slate400
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = model.description,
                                        fontSize = 11.sp,
                                        color = Slate400,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }

                    // Context Ingestion Deck
                    if (contextDocument != null || audioUri != null) {
                        OmniCard(borderColor = CyanAccent.copy(alpha = 0.4f), backgroundColor = SurfaceElevated) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (audioUri != null) "LOADED AUDIO ATTACHMENT" else "INGESTED CONTEXT LOADED",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CyanAccent
                                    )
                                    IconButton(
                                        onClick = {
                                            viewModel.clearContextDocument()
                                            viewModel.clearAudioAttachment()
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = Slate400, modifier = Modifier.size(14.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (audioUri != null) "Audio track ready for Gemini 3.5 Transcribe or Gemini 2.5 Audio" else "Context loaded from Markdown Exporter (~${(contextDocument?.length ?: 0) / 4} tokens)",
                                    fontSize = 11.sp,
                                    color = Slate300
                                )
                            }
                        }
                    }

                    // Task Presets
                    Text(
                        text = "INTELLIGENT TASK PRESETS",
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate400
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        viewModel.presets.forEach { preset ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SurfaceElevated)
                                    .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                                    .clickable { viewModel.applyPreset(preset) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .testTag("preset_${preset.id}")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = preset.title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                        Text(text = preset.subtitle, fontSize = 10.sp, color = Slate400)
                                    }
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    // Custom Prompt Input
                    Text(
                        text = "CUSTOM PROMPT QUERY",
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate400
                    )

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { viewModel.updateInputText(it) },
                        placeholder = { Text("Ask anything or give instructions...", color = Slate400, fontSize = 12.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .testTag("gemini_prompt_input")
                    )

                    Button(
                        onClick = { viewModel.generateCurrent() },
                        enabled = !genState.isGenerating && (inputText.isNotBlank() || contextDocument != null),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("run_gemini_button")
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("EXECUTE WITH ${selectedModel.displayName.uppercase()}", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }

            // Right Pane (62%): Live Stream & Thinking Trace Output
            Box(
                modifier = Modifier
                    .weight(0.62f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceCard)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                    .padding(18.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top Telemetry Header
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OmniBadge(text = genState.modelUsed.displayName, color = CyanAccent)
                                Spacer(modifier = Modifier.width(8.dp))
                                if (genState.isGenerating) {
                                    OmniBadge(text = "STREAMING", color = AmberAccent)
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OmniStatPill(label = "Tokens", value = "~${genState.estimatedTokens}")
                                if (genState.latencyMs > 0) {
                                    OmniStatPill(label = "Latency", value = "${genState.latencyMs}ms", valueColor = Slate400)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Extended Thinking Trace (Expandable)
                        if (genState.thinkingTrace.isNotEmpty()) {
                            OmniCard(
                                borderColor = AmberAccent.copy(alpha = 0.4f),
                                backgroundColor = SurfaceElevated,
                                modifier = Modifier.padding(bottom = 10.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { showThinkingTrace = !showThinkingTrace },
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Psychology, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "EXTENDED THINKING PROCESS TRACE",
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = AmberAccent
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

                    // Main Output Scrollable Viewer
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
                                    text = "Ready to Generate",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Select a preset or enter a prompt using Gemini 3 Flash, 3.8 Thinking, 3.5 Translate, or 2.5 Audio.",
                                    fontSize = 12.sp,
                                    color = Slate400,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(0.8f)
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

                    Spacer(modifier = Modifier.height(14.dp))

                    // Bottom Action Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (genState.isGenerating) {
                            Button(
                                onClick = { viewModel.stopGeneration() },
                                colors = ButtonDefaults.buttonColors(containerColor = RoseAccent, contentColor = Color.White),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("stop_generation_button")
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("STOP GENERATION", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = { viewModel.copyOutputToClipboard() },
                                enabled = genState.outputText.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("copy_gemini_output_button")
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("COPY OUTPUT", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    if (genState.outputText.isNotEmpty()) {
                                        onNavigateToMarkdown(genState.outputText)
                                    }
                                },
                                enabled = genState.outputText.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated, contentColor = CyanAccent),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(0.7f)
                                    .height(48.dp)
                                    .testTag("send_to_markdown_button")
                            ) {
                                Text("TO AI MARKDOWN", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = { viewModel.clearOutput() },
                                enabled = genState.outputText.isNotEmpty(),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate400),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text("CLEAR", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // In-App API Key Configuration Dialog
    if (showKeyDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.closeApiKeyDialog() },
            title = {
                Text(
                    text = "Google AI Studio API Key",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter your Google AI Studio API key to unlock the 6 free-tier models (Gemini 2.5 Flash, 3 Flash, 3.5 Translate, 3.5 Transcribe, 3.8 Thinking).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate400
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = keyInputValue,
                        onValueChange = { keyInputValue = it },
                        label = { Text("AI Studio API Key") },
                        placeholder = { Text("AIzaSy...") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("api_key_dialog_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Keys can also be configured via Secrets panel in AI Studio as GEMINI_API_KEY.",
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
