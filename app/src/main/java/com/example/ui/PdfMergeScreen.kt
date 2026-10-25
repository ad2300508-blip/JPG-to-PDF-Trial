package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MergeType
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.UploadFile
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PdfDocumentItem
import com.example.model.PdfMergeSettings
import com.example.ui.components.Dropzone
import com.example.ui.components.OmniActionBar
import com.example.ui.components.OmniProgressBanner
import com.example.ui.components.OmniToast
import com.example.ui.components.OptionRow
import com.example.ui.components.ToolLayout
import com.example.ui.theme.OmniAccent
import com.example.ui.theme.OmniBorder
import com.example.ui.theme.OmniDanger
import com.example.ui.theme.OmniSurface
import com.example.ui.theme.OmniSurface2
import com.example.ui.theme.OmniText
import com.example.ui.theme.OmniTextMuted
import com.example.ui.theme.RadiusMd
import com.example.ui.theme.RadiusSm
import com.example.viewmodel.PdfMergeViewModel

@Composable
fun PdfMergeScreen(
    viewModel: PdfMergeViewModel,
    modifier: Modifier = Modifier
) {
    val documents by viewModel.documents.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val isMerging by viewModel.isMerging.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val lastMergedUri by viewModel.lastMergedUri.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addPdfDocuments(uris)
        }
    }

    val totalPages = documents.sumOf { it.pageCount }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("pdf_merge_screen")
    ) {
        if (errorMessage != null) {
            OmniToast(
                message = errorMessage!!,
                isError = true,
                actionLabel = "Dismiss",
                onAction = { viewModel.dismissError() },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }

        if (lastMergedUri != null) {
            OmniToast(
                message = "PDF documents merged and saved to Downloads/OmniConvert/PDFs",
                isError = false,
                actionLabel = "Clear",
                onAction = { viewModel.dismissSuccess() },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }

        if (isMerging) {
            val p = if (progress.second > 0) progress.first.toFloat() / progress.second.toFloat() else 0f
            OmniProgressBanner(
                progress = p,
                statusText = "Merging document ${progress.first} of ${progress.second}...",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }

        ToolLayout(
            title = "Merge PDF",
            description = "Combine multiple PDF files into one ordered, secure document.",
            isEmpty = documents.isEmpty(),
            phoneTabTitles = listOf("Files (${documents.size})", "Settings"),
            emptyDropzone = {
                Dropzone(
                    icon = Icons.AutoMirrored.Filled.MergeType,
                    headline = "No PDFs loaded yet",
                    helpText = "Select multiple PDF files to combine, reorder, or password-protect",
                    supportedFormats = "PDF documents",
                    actionLabel = "Add PDFs",
                    tintColor = com.example.ui.theme.TintMerge,
                    onAction = { filePicker.launch("application/pdf") },
                    testTag = "pdf_add_docs_button"
                )
            },
            fileList = {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${documents.size} files · $totalPages total pages",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OmniText
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { filePicker.launch("application/pdf") },
                                modifier = Modifier
                                    .height(36.dp)
                                    .testTag("merge_add_more_button"),
                                border = BorderStroke(1.dp, OmniBorder),
                                shape = RoundedCornerShape(RadiusSm),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = OmniAccent)
                            ) {
                                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
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
                        itemsIndexed(documents, key = { _, doc -> doc.id }) { index, doc ->
                            PdfMergeFileCard(
                                item = doc,
                                index = index,
                                onMoveUp = if (index > 0) { { viewModel.moveDocumentUp(index) } } else null,
                                onMoveDown = if (index < documents.size - 1) { { viewModel.moveDocumentDown(index) } } else null,
                                onRemove = { viewModel.removeDocument(doc.id) }
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
                        text = "Merge Options",
                        style = MaterialTheme.typography.titleMedium,
                        color = OmniText
                    )

                    // Output file name
                    Column {
                        Text(
                            text = "Output file name",
                            fontSize = 14.sp,
                            color = OmniTextMuted,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = settings.outputFileName,
                            onValueChange = { viewModel.updateSettings(settings.copy(outputFileName = it)) },
                            placeholder = { Text("Merged_Document", color = OmniTextMuted, fontSize = 14.sp) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("merge_filename_input"),
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

                    // Password protection switch
                    OptionRow(
                        label = "Password protection",
                        description = "Encrypt with 128-bit security password",
                        checked = settings.enableEncryption,
                        onCheckedChange = { viewModel.updateSettings(settings.copy(enableEncryption = it)) }
                    )

                    // Password input field revealed when enabled
                    if (settings.enableEncryption) {
                        Column {
                            Text(
                                text = "Document password",
                                fontSize = 14.sp,
                                color = OmniTextMuted,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            OutlinedTextField(
                                value = settings.userPassword,
                                onValueChange = { viewModel.updateSettings(settings.copy(userPassword = it)) },
                                placeholder = { Text("Enter secret password", color = OmniTextMuted, fontSize = 14.sp) },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("user_password_input"),
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
                    }
                }
            },
            actionBar = {
                OmniActionBar(
                    summaryText = "${documents.size} files · $totalPages pages",
                    primaryButtonLabel = if (isMerging) "Merging PDFs..." else "Merge PDFs",
                    isEnabled = documents.size >= 2 && !isMerging,
                    disabledHelperText = if (documents.size < 2) "Add at least two PDF documents" else null,
                    onPrimaryAction = { viewModel.mergeDocuments() },
                    testTag = "merge_action_button"
                )
            }
        )
    }
}

@Composable
private fun PdfMergeFileCard(
    item: PdfDocumentItem,
    index: Int,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
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
            // Thumbnail or PDF Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(RadiusSm))
                    .background(OmniSurface2),
                contentAlignment = Alignment.Center
            ) {
                if (item.thumbnailBitmap != null) {
                    Image(
                        bitmap = item.thumbnailBitmap!!.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = OmniAccent,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${index + 1}. ${item.name}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OmniText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${item.pageCount} pages · ${item.formattedSize}",
                    fontSize = 13.sp,
                    color = OmniTextMuted
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
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
                        contentDescription = "Remove file",
                        tint = OmniTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
