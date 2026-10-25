package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.PdfDocumentItem
import com.example.model.PdfExportConfig
import com.example.ui.components.Dropzone
import com.example.ui.components.OmniActionBar
import com.example.ui.components.OmniProgressBanner
import com.example.ui.components.OmniToast
import com.example.ui.components.OptionRow
import com.example.ui.components.SegmentedControl
import com.example.ui.components.ToolLayout
import com.example.ui.theme.OmniAccent
import com.example.ui.theme.OmniAccentSoft
import com.example.ui.theme.OmniBorder
import com.example.ui.theme.OmniDanger
import com.example.ui.theme.OmniOnAccent
import com.example.ui.theme.OmniSurface
import com.example.ui.theme.OmniSurface2
import com.example.ui.theme.OmniText
import com.example.ui.theme.OmniTextMuted
import com.example.ui.theme.RadiusLg
import com.example.ui.theme.RadiusMd
import com.example.ui.theme.RadiusSm
import com.example.viewmodel.PdfStudioViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    var showMetadataModal by rememberSaveable { mutableStateOf(false) }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addImages(uris)
        }
    }

    val pageSizes = listOf("A4", "Letter", "Original")
    val selectedPageSizeIndex = pageSizes.indexOf(config.pageSize).coerceAtLeast(0)

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("pdf_studio_screen")
    ) {
        // Feedback banners
        if (errorMessage != null) {
            OmniToast(
                message = errorMessage!!,
                isError = true,
                actionLabel = "Dismiss",
                onAction = { viewModel.dismissError() },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }

        if (lastGeneratedUri != null) {
            OmniToast(
                message = "PDF created successfully and saved to Downloads/OmniConvert/PDFs",
                isError = false,
                actionLabel = "Clear",
                onAction = { viewModel.clearAll() },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }

        if (isGenerating) {
            val p = if (progress.second > 0) progress.first.toFloat() / progress.second.toFloat() else 0f
            OmniProgressBanner(
                progress = p,
                statusText = "Rendering page ${progress.first} of ${progress.second}...",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }

        ToolLayout(
            title = "Images to PDF",
            description = "Convert scans, photos, and book spreads into clean, paginated PDF documents.",
            isEmpty = items.isEmpty(),
            phoneTabTitles = listOf("Pages (${items.size})", "Options"),
            emptyDropzone = {
                Dropzone(
                    icon = Icons.Default.AddPhotoAlternate,
                    headline = "No images loaded yet",
                    helpText = "Select scans, photos, or documents to compile into a high-quality PDF",
                    supportedFormats = "JPG · PNG · WEBP · HEIC",
                    actionLabel = "Add Images",
                    tintColor = com.example.ui.theme.TintPdf,
                    onAction = { pickerLauncher.launch("image/*") },
                    testTag = "pdf_add_images_button"
                )
            },
            fileList = {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header row with count and quick actions
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${items.size} pages loaded",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OmniText
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { pickerLauncher.launch("image/*") },
                                modifier = Modifier
                                    .height(36.dp)
                                    .testTag("pdf_add_more_button"),
                                border = BorderStroke(1.dp, OmniBorder),
                                shape = RoundedCornerShape(RadiusSm),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = OmniAccent)
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add More", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                            TextButton(
                                onClick = { viewModel.clearAll() },
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("Clear", color = OmniTextMuted, fontSize = 13.sp)
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                            PdfPageCard(
                                item = item,
                                pageNumber = index + 1,
                                onMoveUp = if (index > 0) { { viewModel.moveItem(index, index - 1) } } else null,
                                onMoveDown = if (index < items.size - 1) { { viewModel.moveItem(index, index + 1) } } else null,
                                onToggleSplit = { viewModel.toggleSpreadSplit(item.id) },
                                onToggleBinarize = { viewModel.toggleBinarization(item.id) },
                                onRotate = { viewModel.rotateItem(item.id) },
                                onRemove = { viewModel.removeItem(item.id) }
                            )
                        }
                    }
                }
            },
            optionsPanel = {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Document Settings",
                        style = MaterialTheme.typography.titleMedium,
                        color = OmniText
                    )

                    // Document Title
                    Column {
                        Text(
                            text = "Title",
                            fontSize = 14.sp,
                            color = OmniTextMuted,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = config.title,
                            onValueChange = { viewModel.updateConfig(config.copy(title = it)) },
                            placeholder = { Text("Untitled Document", color = OmniTextMuted, fontSize = 14.sp) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("pdf_title_input"),
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

                    // Page Format
                    Column {
                        Text(
                            text = "Page Format",
                            fontSize = 14.sp,
                            color = OmniTextMuted,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        SegmentedControl(
                            options = pageSizes,
                            selectedIndex = selectedPageSizeIndex,
                            onOptionSelected = { idx ->
                                viewModel.updateConfig(config.copy(pageSize = pageSizes[idx]))
                            }
                        )
                    }

                    // Auto Contrast Switch
                    OptionRow(
                        label = "Auto contrast & clean",
                        description = "Cleans background yellowing and enhances text clarity",
                        checked = config.autoContrastClean,
                        onCheckedChange = { viewModel.updateConfig(config.copy(autoContrastClean = it)) }
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Metadata Modal Trigger
                    Surface(
                        onClick = { showMetadataModal = true },
                        shape = RoundedCornerShape(RadiusSm),
                        color = OmniSurface2,
                        border = BorderStroke(1.dp, OmniBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("open_metadata_modal_button")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = OmniAccent, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Edit Document Metadata", fontSize = 14.sp, color = OmniText, fontWeight = FontWeight.Medium)
                            }
                            if (config.author.isNotBlank() || config.keywords.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(OmniAccentSoft)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("Set", fontSize = 11.sp, color = OmniAccent, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            },
            actionBar = {
                OmniActionBar(
                    summaryText = "${items.size} pages · ${config.pageSize}",
                    primaryButtonLabel = if (isGenerating) "Creating PDF..." else "Create PDF",
                    isEnabled = items.isNotEmpty() && !isGenerating,
                    disabledHelperText = if (items.isEmpty()) "Add at least one image" else null,
                    onPrimaryAction = { viewModel.generatePdf() },
                    testTag = "generate_pdf_button"
                )
            }
        )
    }

    if (showMetadataModal) {
        PdfMetadataDialog(
            currentConfig = config,
            onDismiss = { showMetadataModal = false },
            onSave = { updated ->
                viewModel.updateConfig(updated)
                showMetadataModal = false
            }
        )
    }
}

@Composable
private fun PdfPageCard(
    item: PdfDocumentItem,
    pageNumber: Int,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    onToggleSplit: () -> Unit,
    onToggleBinarize: () -> Unit,
    onRotate: () -> Unit,
    onRemove: () -> Unit
) {
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
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(RadiusSm))
                    .background(OmniSurface2),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = item.uri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Page info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Page $pageNumber · ${item.name}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OmniText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (item.isSplitSpread) {
                        Text(
                            text = if (item.splitPart == 1) "Spread L" else "Spread R",
                            fontSize = 12.sp,
                            color = OmniAccent,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    if (item.isBinarized) {
                        Text(
                            text = "Cleaned",
                            fontSize = 12.sp,
                            color = OmniAccent,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    if (item.rotationDegrees > 0) {
                        Text(
                            text = "${item.rotationDegrees}°",
                            fontSize = 12.sp,
                            color = OmniTextMuted
                        )
                    }
                }
            }

            // Quick Tools
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onToggleSplit, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Book,
                        contentDescription = "Split spread",
                        tint = if (item.isSplitSpread) OmniAccent else OmniTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onToggleBinarize, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.BrightnessMedium,
                        contentDescription = "Enhance contrast",
                        tint = if (item.isBinarized) OmniAccent else OmniTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onRotate, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.AutoMirrored.Filled.RotateRight,
                        contentDescription = "Rotate 90 degrees",
                        tint = OmniTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (onMoveUp != null || onMoveDown != null) {
                    if (onMoveUp != null) {
                        IconButton(onClick = onMoveUp, modifier = Modifier.size(36.dp)) {
                            Text("▲", color = OmniTextMuted, fontSize = 13.sp)
                        }
                    }
                    if (onMoveDown != null) {
                        IconButton(onClick = onMoveDown, modifier = Modifier.size(36.dp)) {
                            Text("▼", color = OmniTextMuted, fontSize = 13.sp)
                        }
                    }
                }

                IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove page",
                        tint = OmniTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PdfMetadataDialog(
    currentConfig: PdfExportConfig,
    onDismiss: () -> Unit,
    onSave: (PdfExportConfig) -> Unit
) {
    var title by rememberSaveable { mutableStateOf(currentConfig.title) }
    var author by rememberSaveable { mutableStateOf(currentConfig.author) }
    var subject by rememberSaveable { mutableStateOf(currentConfig.subject) }
    var keywords by rememberSaveable { mutableStateOf(currentConfig.keywords) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = OmniSurface,
        shape = RoundedCornerShape(RadiusLg),
        title = {
            Text(
                text = "Document Metadata",
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
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title", fontSize = 14.sp) },
                    placeholder = { Text("e.g. History Notes Chapter 1", fontSize = 14.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("metadata_title_input"),
                    shape = RoundedCornerShape(RadiusSm)
                )

                OutlinedTextField(
                    value = author,
                    onValueChange = { author = it },
                    label = { Text("Author", fontSize = 14.sp) },
                    placeholder = { Text("e.g. Maria Rossi", fontSize = 14.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("metadata_author_input"),
                    shape = RoundedCornerShape(RadiusSm)
                )

                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject", fontSize = 14.sp) },
                    placeholder = { Text("e.g. Renaissance Art History", fontSize = 14.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("metadata_subject_input"),
                    shape = RoundedCornerShape(RadiusSm)
                )

                OutlinedTextField(
                    value = keywords,
                    onValueChange = { keywords = it },
                    label = { Text("Keywords", fontSize = 14.sp) },
                    placeholder = { Text("e.g. study, history, art", fontSize = 14.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("metadata_keywords_input"),
                    shape = RoundedCornerShape(RadiusSm)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        currentConfig.copy(
                            title = title,
                            author = author,
                            subject = subject,
                            keywords = keywords
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = OmniAccent,
                    contentColor = OmniOnAccent
                ),
                shape = RoundedCornerShape(RadiusSm),
                modifier = Modifier.height(44.dp)
            ) {
                Text("Save Metadata", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.height(44.dp)) {
                Text("Cancel", color = OmniTextMuted, fontSize = 14.sp)
            }
        }
    )
}
