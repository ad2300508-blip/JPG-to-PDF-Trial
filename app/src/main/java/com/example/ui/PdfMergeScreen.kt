package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MergeType
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.UploadFile
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PdfMergeSettings
import com.example.ui.components.OmniEmptyState
import com.example.ui.components.OmniErrorBanner
import com.example.ui.components.OmniHeader
import com.example.ui.components.OmniLinearProgress
import com.example.ui.components.OmniStatPill
import com.example.ui.components.OmniSuccessBanner
import com.example.ui.components.PdfDocumentCard
import com.example.ui.components.PdfMergeSettingsPanel
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.BorderCrisp
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CardSurface
import com.example.ui.theme.PdfGreen
import com.example.ui.theme.PdfPurple
import com.example.ui.theme.Slate400
import com.example.ui.theme.SurfaceCard
import com.example.viewmodel.PdfMergeViewModel

@Composable
fun PdfMergeScreen(
    viewModel: PdfMergeViewModel,
    modifier: Modifier = Modifier
) {
    val documents by viewModel.documents.collectAsState()
    val settings: PdfMergeSettings by viewModel.settings.collectAsState()
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("pdf_merge_screen")
    ) {
        OmniHeader(
            workspaceTitle = "PDF Merge Studio",
            categoryBadge = "Document Combiner & Security",
            badgeColor = PdfPurple,
            itemCount = documents.size,
            isProcessing = isMerging,
            actions = {
                OutlinedButton(
                    onClick = { filePicker.launch("application/pdf") },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PdfPurple),
                    modifier = Modifier.testTag("pdf_add_docs_button")
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add PDFs")
                }
                if (documents.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = { viewModel.clearAll() }) {
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

        if (lastMergedUri != null) {
            OmniSuccessBanner(
                message = "PDF documents successfully merged and saved to Downloads/OmniConvert/PDFs",
                onDismiss = { viewModel.dismissSuccess() }
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (isMerging) {
            val p = if (progress.second > 0) progress.first.toFloat() / progress.second.toFloat() else 0f
            OmniLinearProgress(progress = p, statusText = "Merging file ${progress.first} of ${progress.second}...")
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Dual Pane Tablet Layout: Left 42% Document Order & Cards, Right 58% Configuration & Merge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Left Pane (42%): Reorderable PDF Documents List
            Box(
                modifier = Modifier
                    .weight(0.42f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceCard)
                    .border(1.dp, BorderCrisp, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                if (documents.isEmpty()) {
                    OmniEmptyState(
                        icon = Icons.Default.PictureAsPdf,
                        title = "No PDFs Selected",
                        description = "Add two or more PDF files to combine them in custom sequence with optional page ranges and encryption."
                    ) {
                        Button(
                            onClick = { filePicker.launch("application/pdf") },
                            colors = ButtonDefaults.buttonColors(containerColor = PdfPurple, contentColor = Color.White)
                        ) {
                            Text("Select PDF Files")
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(documents, key = { _, item -> item.id }) { index, item ->
                            PdfDocumentCard(
                                item = item,
                                index = index,
                                totalCount = documents.size,
                                onMoveUp = { viewModel.moveDocumentUp(index) },
                                onMoveDown = { viewModel.moveDocumentDown(index) },
                                onRemove = { viewModel.removeDocument(item.id) },
                                onPageRangeChange = { range -> viewModel.updatePageRange(item.id, range) }
                            )
                        }
                    }
                }
            }

            // Right Pane (58%): Merge Settings & Command Center
            Box(
                modifier = Modifier
                    .weight(0.58f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceCard)
                    .border(1.dp, BorderCrisp, RoundedCornerShape(14.dp))
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
                        PdfMergeSettingsPanel(
                            settings = settings,
                            onSettingsChange = { newSettings -> viewModel.updateSettings(newSettings) }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action Bar
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OmniStatPill(label = "Total Files", value = "${documents.size}")
                            OmniStatPill(label = "Total Pages", value = "${viewModel.getTotalPageCount()}", valueColor = PdfGreen)
                            val totalMb = String.format("%.1f MB", viewModel.getTotalFileSizeBytes().toDouble() / (1024 * 1024))
                            OmniStatPill(label = "Input Size", value = totalMb, valueColor = Slate400)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { viewModel.mergeDocuments() },
                            enabled = documents.size >= 2 && !isMerging && settings.hasValidPassword,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("merge_pdfs_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PdfPurple,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.MergeType, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isMerging) "MERGING PDFS..." else "MERGE & SAVE PDF",
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
