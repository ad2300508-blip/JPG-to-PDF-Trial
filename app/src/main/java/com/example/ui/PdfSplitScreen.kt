package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ContinueAction
import com.example.ui.components.ConversionResultView
import com.example.ui.components.MorphTextField
import com.example.ui.components.OmniActionBar
import com.example.ui.components.OmniSlider
import com.example.ui.components.OptionRow
import com.example.ui.components.OptionSection
import com.example.ui.components.SegmentedControl
import com.example.ui.components.formatBytes
import com.example.ui.theme.OmniSurface2
import com.example.ui.theme.OmniText
import com.example.ui.theme.OmniTextMuted
import com.example.ui.theme.RadiusMd
import com.example.ui.theme.TintMerge
import com.example.ui.theme.TintStamp
import com.example.ui.theme.enterAnimation
import com.example.util.OutputFolder
import com.example.util.OutputStorage
import com.example.util.PageRange
import com.example.viewmodel.PdfSplitViewModel
import com.example.viewmodel.SplitMode

@Composable
fun PdfSplitScreen(
    viewModel: PdfSplitViewModel,
    modifier: Modifier = Modifier,
    onContinueWithOrganize: (Uri) -> Unit = {}
) {
    val source by viewModel.source.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isWorking by viewModel.isWorking.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val resultUri by viewModel.resultUri.collectAsState()
    val result by viewModel.result.collectAsState()
    val mode by viewModel.mode.collectAsState()
    val everyN by viewModel.everyN.collectAsState()
    val customRanges by viewModel.customRanges.collectAsState()
    val asZip by viewModel.asZip.collectAsState()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.loadPdf(uri)
    }

    AnimatedContent(
        targetState = resultUri != null,
        transitionSpec = { stageTransition(if (initialState) 1 else 0, if (targetState) 1 else 0) },
        modifier = modifier,
        label = "split_stage"
    ) { showResult ->
        val uri = resultUri
        val res = result
        if (showResult && uri != null) {
            val zipped = res?.zipUri != null
            val parts = res?.parts ?: 0
            ConversionResultView(
                title = if (parts == 1) "1 PDF creato" else "$parts PDF creati",
                subtitle = if (zipped) "Tutte le parti sono raccolte in un unico ZIP." else "Ogni parte è un PDF separato.",
                outputUri = uri,
                mimeType = if (zipped) "application/zip" else "application/pdf",
                previewIcon = Icons.Default.ContentCut,
                previewTint = TintStamp,
                outputBytesOverride = res?.totalBytes?.takeIf { it > 0 },
                detailPrefix = "$parts parti",
                savedIn = "Salvato in ${OutputStorage.displayPath(if (zipped) OutputFolder.ARCHIVES else OutputFolder.PDFS)}",
                shareUris = res?.uris.orEmpty(),
                onClose = { viewModel.dismissResult() },
                onNewConversion = { viewModel.clear() },
                continueActions = if (zipped) emptyList() else listOf(
                    ContinueAction("Organizza la prima parte", Icons.Default.ViewModule, TintMerge) { onContinueWithOrganize(uri) }
                )
            )
        } else {
            SinglePdfScaffold(
                title = "Dividi PDF",
                description = "Spezza un PDF in più file più piccoli",
                source = source,
                isLoading = isLoading,
                isWorking = isWorking,
                progress = progress,
                progressLabel = "Creo la parte ${progress.first} di ${progress.second}…",
                errorMessage = errorMessage,
                onDismissError = { viewModel.dismissError() },
                onCancel = { viewModel.cancel() },
                onPick = { picker.launch("application/pdf") },
                onClear = { viewModel.clear() },
                dropIcon = Icons.Default.ContentCut,
                dropHeadline = "Scegli il PDF da dividere",
                dropHelp = "Separa i capitoli di un libro o le verifiche scansionate tutte insieme",
                tint = TintStamp
            ) { pdf ->
                val groups = remember(pdf, mode, everyN, customRanges) { viewModel.groups(pdf.pageCount) }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SegmentedControl(
                        options = SplitMode.entries.map { it.label },
                        selectedIndex = mode.ordinal,
                        onOptionSelected = { viewModel.setMode(SplitMode.entries[it]) },
                        modifier = Modifier.enterAnimation(1)
                    )
                    AnimatedContent(
                        targetState = mode,
                        transitionSpec = { stageTransition(initialState.ordinal, targetState.ordinal) },
                        label = "split_mode"
                    ) { current ->
                        when (current) {
                            SplitMode.EVERY_N -> OmniSlider(
                                label = "Pagine per file",
                                value = everyN.toFloat(),
                                onValueChange = { viewModel.setEveryN(it.toInt()) },
                                valueRange = 1f..pdf.pageCount.coerceAtLeast(2).toFloat(),
                                steps = (pdf.pageCount - 2).coerceIn(0, 98),
                                valueFormatter = { "${it.toInt()}" }
                            )
                            SplitMode.SINGLE -> Text(
                                text = "Ogni pagina diventa un PDF a sé: utile per verifiche o moduli scansionati insieme.",
                                fontSize = 14.sp,
                                color = OmniTextMuted
                            )
                            SplitMode.CUSTOM -> OptionSection(title = "Intervalli", hint = "Una parte per ogni intervallo, separati da virgola. Es. 1-3, 4-10, 11-") {
                                MorphTextField(
                                    value = customRanges,
                                    onValueChange = viewModel::setCustomRanges,
                                    placeholder = "1-3, 4-10, 11-",
                                    isError = customRanges.isNotBlank() && groups.isEmpty(),
                                    supportingText = if (customRanges.isNotBlank() && groups.isEmpty()) "Nessun intervallo valido (il PDF ha ${pdf.pageCount} pagine)" else null,
                                    testTag = "split_ranges_field"
                                )
                            }
                        }
                    }
                    PartsPreview(groups)
                    OptionRow(
                        label = "Raccogli in uno ZIP",
                        description = if (groups.size > 30) "Più di 30 parti: le raccolgo sempre in uno ZIP" else "Un solo file da condividere invece di tanti PDF",
                        checked = asZip || groups.size > 30,
                        onCheckedChange = viewModel::setAsZip,
                        enabled = groups.size <= 30
                    )
                    OmniActionBar(
                        summaryText = "${pdf.pageCount} pagine · ${formatBytes(pdf.sizeBytes)}",
                        detailText = if (groups.size == 1) "1 parte" else "${groups.size} parti",
                        primaryButtonLabel = if (isWorking) "Divido…" else "Dividi",
                        isEnabled = !isWorking && groups.isNotEmpty() && !(groups.size == 1 && groups[0].size == pdf.pageCount),
                        disabledHelperText = when {
                            groups.isEmpty() -> "Scrivi almeno un intervallo valido"
                            groups.size == 1 && groups[0].size == pdf.pageCount -> "Così otterresti lo stesso PDF: scegli meno pagine per file"
                            else -> null
                        },
                        onPrimaryAction = { viewModel.split() },
                        testTag = "split_pdf_button"
                    )
                }
            }
        }
    }
}

/** The first parts as rows ("Parte 1 · pagine 1-3"), with a counter for the rest. */
@Composable
private fun PartsPreview(groups: List<List<Int>>) {
    val shown = groups.take(6)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AnimatedContent(targetState = groups.size, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "parts_count") { count ->
            Text(
                text = if (count == 1) "Verrà creata 1 parte" else "Verranno create $count parti",
                style = MaterialTheme.typography.titleMedium,
                color = OmniText
            )
        }
        shown.forEachIndexed { index, pages ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(RadiusMd))
                    .background(OmniSurface2)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .enterAnimation(index, key = pages),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Description, contentDescription = null, tint = TintStamp, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Parte ${index + 1}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = OmniText, modifier = Modifier.weight(1f))
                Text(
                    text = (if (pages.size == 1) "pagina " else "pagine ") + PageRange.describe(pages),
                    fontSize = 13.sp,
                    color = OmniTextMuted
                )
            }
        }
        if (groups.size > shown.size) {
            Text("…e altre ${groups.size - shown.size}", fontSize = 13.sp, color = OmniTextMuted, modifier = Modifier.padding(start = 4.dp))
        }
    }
}
