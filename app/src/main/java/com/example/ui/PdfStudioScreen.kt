package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.PdfDocumentItem
import com.example.model.PdfExportConfig
import com.example.ui.components.OmniBadge
import com.example.ui.components.OmniCard
import com.example.ui.components.OmniEmptyState
import com.example.ui.components.OmniErrorBanner
import com.example.ui.components.OmniHeader
import com.example.ui.components.OmniLinearProgress
import com.example.ui.components.OmniStatPill
import com.example.ui.components.OmniSuccessBanner
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.RoseAccent
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceElevated
import com.example.viewmodel.PdfStudioViewModel

@Composable
fun PdfStudioScreen(
    viewModel: PdfStudioViewModel,
    modifier: Modifier = Modifier
) {
    val items by viewModel.items.collectAsState()
    val config by viewModel.config.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val progress by viewModel.generationProgress.collectAsState()
    val lastGeneratedUri by viewModel.lastGeneratedUri.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addImages(uris)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("pdf_studio_screen")
    ) {
        OmniHeader(
            workspaceTitle = "PDF Studio",
            categoryBadge = "Book Cleaner & PDF",
            badgeColor = CyanAccent,
            itemCount = items.size,
            isProcessing = isGenerating,
            actions = {
                OutlinedButton(
                    onClick = { pickerLauncher.launch("image/*") },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent),
                    modifier = Modifier.testTag("pdf_add_images_button")
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Images")
                }
                if (items.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { viewModel.clearAll() },
                        modifier = Modifier.testTag("pdf_clear_all_button")
                    ) {
                        Icon(Icons.Default.ClearAll, contentDescription = "Clear All", tint = Slate400)
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (errorMessage != null) {
            OmniErrorBanner(message = errorMessage!!, onDismiss = { viewModel.dismissError() })
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (lastGeneratedUri != null) {
            OmniSuccessBanner(message = "PDF successfully generated and saved to Downloads/OmniConvert/PDFs", onDismiss = { viewModel.clearAll() })
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (isGenerating) {
            val p = if (progress.second > 0) progress.first.toFloat() / progress.second.toFloat() else 0f
            OmniLinearProgress(progress = p, statusText = "Generating page ${progress.first} of ${progress.second}...")
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Dual Pane Tablet Layout: Left 42% Images, Right 58% Command Center
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Left Pane: Document Image Pages
            Box(
                modifier = Modifier
                    .weight(0.42f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceCard)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                if (items.isEmpty()) {
                    OmniEmptyState(
                        icon = Icons.Default.PictureAsPdf,
                        title = "No Pages Loaded",
                        description = "Import book screenshots or scans to clean contrast, split two-page spreads, and compile into a PDF."
                    ) {
                        Button(
                            onClick = { pickerLauncher.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black)
                        ) {
                            Text("Import Images")
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(items, key = { it.id }) { item ->
                            PdfItemCard(
                                item = item,
                                onToggleSplit = { viewModel.toggleSpreadSplit(item.id) },
                                onToggleBinarize = { viewModel.toggleBinarization(item.id) },
                                onRotate = { viewModel.rotateItem(item.id) },
                                onRemove = { viewModel.removeItem(item.id) }
                            )
                        }
                    }
                }
            }

            // Right Pane: PDF Engine Configuration & Compile Command
            Box(
                modifier = Modifier
                    .weight(0.58f)
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
                    Column {
                        Text(
                            text = "PDF ENGINE CONFIGURATION",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate400
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = config.title,
                            onValueChange = { viewModel.updateConfig(config.copy(title = it)) },
                            label = { Text("Document Title") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pdf_title_input"),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Page Format & Margins",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf("A4", "Letter", "Original").forEach { size ->
                                val selected = config.pageSize == size
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) CyanAccent.copy(alpha = 0.2f) else SurfaceElevated)
                                        .border(1.dp, if (selected) CyanAccent else BorderSubtle, RoundedCornerShape(8.dp))
                                        .clickable { viewModel.updateConfig(config.copy(pageSize = size)) }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                        .testTag("page_size_$size")
                                ) {
                                    Text(
                                        text = size,
                                        color = if (selected) CyanAccent else Slate400,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Book Screenshot Cleaner toggles
                        OmniCard(borderColor = BorderSubtle, backgroundColor = SurfaceElevated) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Auto Contrast & Background Clean",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Removes page shadows, yellow tint, and enhances text darkness.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Slate400
                                        )
                                    }
                                    Switch(
                                        checked = config.autoContrastClean,
                                        onCheckedChange = { viewModel.updateConfig(config.copy(autoContrastClean = it)) },
                                        colors = SwitchDefaults.colors(checkedThumbColor = CyanAccent, checkedTrackColor = Slate800),
                                        modifier = Modifier.testTag("auto_contrast_switch")
                                    )
                                }
                            }
                        }
                    }

                    // Command Action Bar
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OmniStatPill(label = "Total Pages", value = "${items.size}")
                            OmniStatPill(label = "Format", value = config.pageSize)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { viewModel.generatePdf() },
                            enabled = items.isNotEmpty() && !isGenerating,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("generate_pdf_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanAccent,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isGenerating) "GENERATING PDF..." else "BUILD & EXPORT PDF",
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PdfItemCard(
    item: PdfDocumentItem,
    onToggleSplit: () -> Unit,
    onToggleBinarize: () -> Unit,
    onRotate: () -> Unit,
    onRemove: () -> Unit
) {
    OmniCard(
        borderColor = if (item.isSplitSpread) IndigoAccent.copy(alpha = 0.5f) else BorderSubtle,
        backgroundColor = SurfaceElevated
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Slate800)
            ) {
                AsyncImage(
                    model = item.uri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
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
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (item.isSplitSpread) {
                        OmniBadge(text = if (item.splitPart == 1) "SPREAD L" else "SPREAD R", color = IndigoAccent)
                    }
                    if (item.isBinarized) {
                        OmniBadge(text = "CLEANED", color = EmeraldAccent)
                    }
                    if (item.rotationDegrees > 0) {
                        OmniBadge(text = "${item.rotationDegrees}°", color = Slate400)
                    }
                }
            }

            Row {
                IconButton(onClick = onToggleSplit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Book, contentDescription = "Split Spread", tint = if (item.isSplitSpread) IndigoAccent else Slate400, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onToggleBinarize, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.BrightnessMedium, contentDescription = "Clean Contrast", tint = if (item.isBinarized) EmeraldAccent else Slate400, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onRotate, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = "Rotate", tint = Slate400, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = RoseAccent, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
