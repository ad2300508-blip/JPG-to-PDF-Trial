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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.example.model.PdfDocumentItem
import com.example.ui.components.OmniPhoneTabSelector

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

    var phoneTabIndex by remember { mutableIntStateOf(0) }

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
                    Text("Add PDFs", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
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

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val isCompact = maxWidth < 600.dp

            if (isCompact) {
                // Phone Layout: 2 Tabs (Documents vs Merge Settings)
                Column(modifier = Modifier.fillMaxSize()) {
                    OmniPhoneTabSelector(
                        selectedTabIndex = phoneTabIndex,
                        tabs = listOf("PDFs (${documents.size})", "Merge Settings"),
                        accentColor = PdfPurple,
                        onTabSelected = { phoneTabIndex = it },
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    if (phoneTabIndex == 0) {
                        PdfMergeDocsPane(
                            documents = documents,
                            onPickPdfs = { filePicker.launch("application/pdf") },
                            viewModel = viewModel,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            onNavigateToSettings = if (documents.size >= 2) { { phoneTabIndex = 1 } } else null
                        )
                    } else {
                        PdfMergeSettingsPane(
                            settings = settings,
                            documentsCount = documents.size,
                            totalPageCount = viewModel.getTotalPageCount(),
                            totalFileSizeBytes = viewModel.getTotalFileSizeBytes(),
                            isMerging = isMerging,
                            onSettingsChange = { viewModel.updateSettings(it) },
                            onMerge = { viewModel.mergeDocuments() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                    }
                }
            } else {
                // Dual Pane Tablet Layout: Left 42% Document Order, Right 58% Configuration & Merge
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    PdfMergeDocsPane(
                        documents = documents,
                        onPickPdfs = { filePicker.launch("application/pdf") },
                        viewModel = viewModel,
                        modifier = Modifier
                            .weight(0.42f)
                            .fillMaxHeight(),
                        onNavigateToSettings = null
                    )

                    PdfMergeSettingsPane(
                        settings = settings,
                        documentsCount = documents.size,
                        totalPageCount = viewModel.getTotalPageCount(),
                        totalFileSizeBytes = viewModel.getTotalFileSizeBytes(),
                        isMerging = isMerging,
                        onSettingsChange = { viewModel.updateSettings(it) },
                        onMerge = { viewModel.mergeDocuments() },
                        modifier = Modifier
                            .weight(0.58f)
                            .fillMaxHeight()
                    )
                }
            }
        }
    }
}

@Composable
private fun PdfMergeDocsPane(
    documents: List<PdfDocumentItem>,
    onPickPdfs: () -> Unit,
    viewModel: PdfMergeViewModel,
    modifier: Modifier = Modifier,
    onNavigateToSettings: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
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
                    onClick = onPickPdfs,
                    colors = ButtonDefaults.buttonColors(containerColor = PdfPurple, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select PDF Files", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
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

                if (onNavigateToSettings != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onNavigateToSettings,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CardSurface,
                            contentColor = PdfPurple
                        ),
                        border = BorderStroke(1.dp, PdfPurple.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Configure & Merge PDF →", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun PdfMergeSettingsPane(
    settings: PdfMergeSettings,
    documentsCount: Int,
    totalPageCount: Int,
    totalFileSizeBytes: Long,
    isMerging: Boolean,
    onSettingsChange: (PdfMergeSettings) -> Unit,
    onMerge: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
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
                    onSettingsChange = onSettingsChange
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
                    OmniStatPill(label = "Total Files", value = "$documentsCount")
                    OmniStatPill(label = "Total Pages", value = "$totalPageCount", valueColor = PdfGreen)
                    val totalMb = String.format("%.1f MB", totalFileSizeBytes.toDouble() / (1024 * 1024))
                    OmniStatPill(label = "Input Size", value = totalMb, valueColor = Slate400)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onMerge,
                    enabled = documentsCount >= 2 && !isMerging && settings.hasValidPassword,
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
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
