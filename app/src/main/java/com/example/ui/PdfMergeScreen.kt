package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PdfDocumentItem
import com.example.ui.components.ContinueAction
import com.example.ui.components.ConversionResultView
import com.example.ui.components.Dropzone
import com.example.ui.components.FileCard
import com.example.ui.components.FileListHeader
import com.example.ui.components.MorphTextField
import com.example.ui.components.OmniActionBar
import com.example.ui.components.OmniProgressBanner
import com.example.ui.components.OmniToast
import com.example.ui.components.OptionRow
import com.example.ui.components.RevealVertically
import com.example.ui.components.ToolLayout
import com.example.ui.theme.Haptic
import com.example.ui.theme.OmniAccent
import com.example.ui.theme.OmniDanger
import com.example.ui.theme.OmniOnAccent
import com.example.ui.theme.OmniSurface
import com.example.ui.theme.OmniText
import com.example.ui.theme.OmniTextMuted
import com.example.ui.theme.RadiusLg
import com.example.ui.theme.RadiusMd
import com.example.ui.theme.TintMerge
import com.example.ui.theme.TintPdf
import com.example.ui.theme.enterAnimation
import com.example.ui.theme.rememberHaptics
import com.example.util.OutputFolder
import com.example.util.OutputStorage
import com.example.util.PageRange
import com.example.viewmodel.PdfMergeViewModel

@Composable
fun PdfMergeScreen(
    viewModel: PdfMergeViewModel,
    modifier: Modifier = Modifier,
    onContinueWithMarkdown: (Uri) -> Unit = {},
    onContinueWithCompress: (Uri) -> Unit = {},
    onContinueWithOrganize: (Uri) -> Unit = {}
) {
    val documents by viewModel.documents.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val isMerging by viewModel.isMerging.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val lastMergedUri by viewModel.lastMergedUri.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val haptics = rememberHaptics()

    AnimatedContent(
        targetState = lastMergedUri,
        transitionSpec = { stageTransition(if (initialState == null) 0 else 1, if (targetState == null) 0 else 1) },
        modifier = modifier,
        label = "merge_stage"
    ) { mergedUri ->
        if (mergedUri != null) {
            ConversionResultView(
                title = "Il tuo PDF è pronto",
                subtitle = "${documents.size} documenti uniti in un unico file.",
                outputUri = mergedUri,
                mimeType = "application/pdf",
                previewIcon = Icons.AutoMirrored.Filled.MergeType,
                previewTint = TintMerge,
                originalBytes = viewModel.getTotalFileSizeBytes(),
                detailPrefix = viewModel.selectedPageCount(documents).takeIf { it > 0 }?.let { "$it pagine" },
                onClose = { viewModel.dismissSuccess() },
                onNewConversion = { viewModel.clearAll() },
                savedIn = "Salvato in ${OutputStorage.displayPath(OutputFolder.PDFS)}",
                continueActions = listOf(
                    ContinueAction("Comprimi il PDF", Icons.Default.Compress, TintPdf) { onContinueWithCompress(mergedUri) },
                    ContinueAction("Organizza le pagine", Icons.Default.ViewModule, TintMerge) { onContinueWithOrganize(mergedUri) },
                    ContinueAction("Estrai il testo (Markdown)", Icons.Default.TextSnippet, TintPdf) { onContinueWithMarkdown(mergedUri) }
                )
            )
        } else {
            MergeEditor(
                viewModel = viewModel,
                documents = documents,
                isMerging = isMerging,
                isLoading = isLoading,
                progress = progress,
                errorMessage = errorMessage,
                onHaptic = haptics
            )
        }
    }
}

@Composable
private fun MergeEditor(
    viewModel: PdfMergeViewModel,
    documents: List<PdfDocumentItem>,
    isMerging: Boolean,
    isLoading: Boolean,
    progress: Pair<Int, Int>,
    errorMessage: String?,
    onHaptic: (Haptic) -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    var unlockTargetId by rememberSaveable { mutableStateOf<String?>(null) }
    var expandedId by rememberSaveable { mutableStateOf<String?>(null) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) viewModel.addPdfDocuments(uris)
    }

    val selectedPages = viewModel.selectedPageCount(documents)
    val blocking = viewModel.blockingReason(documents, settings)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("pdf_merge_screen")
    ) {
        RevealVertically(visible = errorMessage != null) {
            OmniToast(
                message = errorMessage.orEmpty(),
                isError = true,
                actionLabel = "Chiudi",
                onAction = { viewModel.dismissError() },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }

        RevealVertically(visible = isMerging) {
            val p = if (progress.second > 0) progress.first.toFloat() / progress.second.toFloat() else 0f
            OmniProgressBanner(
                progress = p,
                statusText = "Unisco il documento ${progress.first} di ${progress.second}…",
                onCancel = { viewModel.cancelMerge() },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }

        ToolLayout(
            title = "Unisci PDF",
            description = "Combina più PDF in un unico documento ordinato",
            isEmpty = documents.isEmpty() && !isLoading,
            phoneTabTitles = listOf("File · ${documents.size}", "Impostazioni"),
            emptyDropzone = {
                Dropzone(
                    icon = Icons.AutoMirrored.Filled.MergeType,
                    headline = "Aggiungi i tuoi PDF",
                    helpText = "Scegli più PDF da unire, riordinare, tagliare per pagine o proteggere con password",
                    supportedFormats = "PDF · anche protetti",
                    actionLabel = "Scegli file",
                    tintColor = TintMerge,
                    onAction = { filePicker.launch("application/pdf") },
                    testTag = "pdf_add_docs_button"
                )
            },
            fileList = {
                Column(modifier = Modifier.fillMaxSize()) {
                    FileListHeader(
                        title = "${documents.size} file · $selectedPages pagine",
                        onAdd = { filePicker.launch("application/pdf") },
                        onClear = { viewModel.clearAll() },
                        addIcon = Icons.Default.UploadFile,
                        extra = {
                            if (documents.size > 1) {
                                IconButton(onClick = { viewModel.sortByName() }) {
                                    Icon(Icons.Default.SortByAlpha, contentDescription = "Ordina per nome", tint = OmniTextMuted)
                                }
                            }
                        }
                    )

                    AnimatedVisibility(visible = isLoading) {
                        Row(
                            modifier = Modifier.padding(bottom = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = OmniAccent)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Leggo i PDF…", fontSize = 13.sp, color = OmniTextMuted)
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(documents, key = { _, doc -> doc.id }) { index, doc ->
                            MergeFileCard(
                                item = doc,
                                index = index,
                                expanded = expandedId == doc.id,
                                onToggleExpand = { expandedId = if (expandedId == doc.id) null else doc.id },
                                onMoveUp = if (index > 0) { { onHaptic(Haptic.TICK); viewModel.moveDocumentUp(index) } } else null,
                                onMoveDown = if (index < documents.size - 1) { { onHaptic(Haptic.TICK); viewModel.moveDocumentDown(index) } } else null,
                                onRemove = { onHaptic(Haptic.REJECT); viewModel.removeDocument(doc.id) },
                                onUnlock = { unlockTargetId = doc.id },
                                onRangeChange = { viewModel.updatePageRange(doc.id, it) },
                                modifier = Modifier
                                    .animateItem()
                                    .enterAnimation(index, key = doc.id)
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
                        text = "Opzioni di unione",
                        style = MaterialTheme.typography.titleMedium,
                        color = OmniText
                    )

                    MorphTextField(
                        value = settings.outputFileName,
                        onValueChange = { viewModel.updateSettings(settings.copy(outputFileName = it)) },
                        label = "Nome del file",
                        placeholder = "PDF_unito",
                        testTag = "merge_filename_input"
                    )

                    OptionRow(
                        label = "Protezione con password",
                        description = "Serve la password per aprire il PDF unito",
                        checked = settings.enableEncryption,
                        onCheckedChange = { viewModel.updateSettings(settings.copy(enableEncryption = it)) }
                    )

                    AnimatedVisibility(
                        visible = settings.enableEncryption,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        MorphTextField(
                            value = settings.userPassword,
                            onValueChange = { viewModel.updateSettings(settings.copy(userPassword = it)) },
                            label = "Password del documento",
                            placeholder = "Inserisci la password",
                            visualTransformation = PasswordVisualTransformation(),
                            isError = settings.userPassword.isBlank(),
                            supportingText = if (settings.userPassword.isBlank()) "Obbligatoria per proteggere il file" else null,
                            testTag = "user_password_input"
                        )
                    }

                    Text(
                        text = "Suggerimento: tocca un file per scegliere solo alcune pagine (es. 1-3, 5).",
                        fontSize = 13.sp,
                        color = OmniTextMuted
                    )
                }
            },
            actionBar = {
                OmniActionBar(
                    summaryText = "${documents.size} file · $selectedPages pagine",
                    primaryButtonLabel = if (isMerging) "Unisco i PDF…" else "Unisci PDF",
                    isEnabled = blocking == null && !isMerging,
                    disabledHelperText = blocking,
                    onPrimaryAction = { viewModel.mergeDocuments() },
                    testTag = "merge_action_button"
                )
            }
        )
    }

    val unlockTarget = documents.firstOrNull { it.id == unlockTargetId }
    if (unlockTarget != null) {
        UnlockDialog(
            name = unlockTarget.name,
            onDismiss = { unlockTargetId = null },
            onConfirm = { password ->
                viewModel.unlockDocument(unlockTarget.id, password)
                unlockTargetId = null
            }
        )
    }
}

@Composable
private fun MergeFileCard(
    item: PdfDocumentItem,
    index: Int,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    onRemove: () -> Unit,
    onUnlock: () -> Unit,
    onRangeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val locked = item.isEncrypted && item.password.isEmpty()
    val rangeValid = item.pageCount <= 0 || PageRange.isValid(item.pageRange, item.pageCount)
    val selectedPages = if (item.pageCount > 0) PageRange.parse(item.pageRange, item.pageCount).size else 0
    val details = when {
        item.isCorrupted -> item.errorMessage ?: "File non valido"
        locked -> "Protetto da password · tocca per sbloccare"
        item.pageRange.isNotBlank() && rangeValid -> "$selectedPages di ${item.pageCount} pagine · ${item.formattedSize}"
        else -> "${item.displayPageCount} · ${item.formattedSize}"
    }

    FileCard(
        name = "${index + 1}. ${item.name}",
        details = details,
        detailsColor = if (item.isCorrupted || !rangeValid) OmniDanger else OmniTextMuted,
        highlighted = expanded,
        onClick = if (locked) onUnlock else if (!item.isCorrupted) onToggleExpand else null,
        thumbnail = {
            val thumb = item.thumbnailBitmap
            when {
                thumb != null -> Image(
                    bitmap = remember(thumb) { thumb.asImageBitmap() },
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                locked -> Icon(Icons.Default.Lock, contentDescription = null, tint = OmniAccent, modifier = Modifier.size(22.dp))
                else -> Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = OmniAccent, modifier = Modifier.size(24.dp))
            }
        },
        onMoveUp = onMoveUp,
        onMoveDown = onMoveDown,
        onRemove = onRemove,
        modifier = modifier,
        bottomContent = {
            AnimatedVisibility(
                visible = expanded && !locked && !item.isCorrupted,
                enter = expandVertically(tween(260)) + fadeIn(),
                exit = shrinkVertically(tween(200)) + fadeOut()
            ) {
                Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
                    MorphTextField(
                        value = item.pageRange,
                        onValueChange = onRangeChange,
                        label = "Pagine da includere",
                        placeholder = "Tutte (es. 1-3, 5, 8-)",
                        isError = !rangeValid,
                        supportingText = if (!rangeValid) "Intervallo non valido: il documento ha ${item.pageCount} pagine" else "Lascia vuoto per includere tutte le ${item.pageCount} pagine"
                    )
                }
            }
        }
    )
}

@Composable
private fun UnlockDialog(name: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var password by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = OmniSurface,
        shape = RoundedCornerShape(RadiusLg),
        icon = { Icon(Icons.Default.LockOpen, contentDescription = null, tint = OmniAccent) },
        title = { Text("Sblocca \"$name\"", color = OmniText, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = {
            MorphTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = "Password del PDF",
                visualTransformation = PasswordVisualTransformation()
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(password) },
                enabled = password.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = OmniAccent, contentColor = OmniOnAccent),
                shape = RoundedCornerShape(RadiusMd)
            ) { Text("Sblocca", fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla", color = OmniTextMuted) }
        }
    )
}
