package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.PdfDetails
import com.example.service.PdfMetadata
import com.example.ui.components.ContinueAction
import com.example.ui.components.ConversionResultView
import com.example.ui.components.MorphTextField
import com.example.ui.components.OmniActionBar
import com.example.ui.components.formatBytes
import com.example.ui.theme.LocalReduceMotion
import com.example.ui.theme.OmniAccent
import com.example.ui.theme.OmniAccentSoft
import com.example.ui.theme.OmniDanger
import com.example.ui.theme.OmniSurface2
import com.example.ui.theme.OmniText
import com.example.ui.theme.OmniTextMuted
import com.example.ui.theme.RadiusMd
import com.example.ui.theme.TintPdf
import com.example.ui.theme.enterAnimation
import com.example.ui.theme.motionSpec
import com.example.util.DocStats
import com.example.util.OutputFolder
import com.example.util.OutputStorage
import com.example.viewmodel.LoadedPdf
import com.example.viewmodel.PdfInfoViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun PdfInfoScreen(
    viewModel: PdfInfoViewModel,
    modifier: Modifier = Modifier,
    onOpenInReader: (Uri) -> Unit = {}
) {
    val source by viewModel.source.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isWorking by viewModel.isWorking.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val resultUri by viewModel.resultUri.collectAsState()
    val details by viewModel.details.collectAsState()
    val analyzing by viewModel.analyzing.collectAsState()
    val metadata by viewModel.metadata.collectAsState()
    val strippedAll by viewModel.strippedAll.collectAsState()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.loadPdf(uri)
    }

    AnimatedContent(
        targetState = resultUri != null,
        transitionSpec = { stageTransition(if (initialState) 1 else 0, if (targetState) 1 else 0) },
        modifier = modifier,
        label = "info_stage"
    ) { showResult ->
        val uri = resultUri
        if (showResult && uri != null) {
            ConversionResultView(
                title = if (strippedAll) "Metadati rimossi" else "Metadati aggiornati",
                subtitle = if (strippedAll) "Nella copia non restano autore, titolo, date né programmi usati." else "La copia del PDF contiene i nuovi dati.",
                outputUri = uri,
                mimeType = "application/pdf",
                previewIcon = Icons.Default.Info,
                previewTint = TintPdf,
                savedIn = "Salvato in ${OutputStorage.displayPath(OutputFolder.PDFS)}",
                onClose = { viewModel.dismissResult() },
                onNewConversion = { viewModel.clear() },
                continueActions = listOf(
                    ContinueAction("Leggi il PDF", Icons.AutoMirrored.Filled.MenuBook, TintPdf) { onOpenInReader(uri) }
                )
            )
        } else {
            SinglePdfScaffold(
                title = "Info PDF",
                description = "Proprietà, statistiche e metadati",
                source = source,
                isLoading = isLoading,
                isWorking = isWorking,
                progress = progress,
                progressLabel = "Salvo la copia…",
                errorMessage = errorMessage,
                onDismissError = { viewModel.dismissError() },
                onCancel = { viewModel.cancel() },
                onPick = { picker.launch("application/pdf") },
                onClear = { viewModel.clear() },
                dropIcon = Icons.Default.Info,
                dropHeadline = "Scegli il PDF da analizzare",
                dropHelp = "Scopri formato, parole e tempo di lettura, e modifica o togli titolo e autore",
                tint = TintPdf
            ) { pdf ->
                val current = details
                AnimatedContent(
                    targetState = current,
                    contentKey = { it != null },
                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) },
                    label = "info_details"
                ) { d ->
                    if (d == null) {
                        AnalyzingPlaceholder(analyzing = analyzing)
                    } else {
                        DetailsContent(
                            details = d,
                            pdf = pdf,
                            metadata = metadata,
                            hasChanges = viewModel.hasChanges(),
                            isWorking = isWorking,
                            onEdit = viewModel::updateMetadata,
                            onSave = { viewModel.save(removeAll = false) },
                            onRemoveAll = { viewModel.save(removeAll = true) }
                        )
                    }
                }
            }
        }
    }
}

/** Soft pulsing placeholders while the PDF is read. */
@Composable
private fun AnalyzingPlaceholder(analyzing: Boolean) {
    val reduce = LocalReduceMotion.current
    val transition = rememberInfiniteTransition(label = "info_placeholder")
    val pulse by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = if (reduce) 0.45f else 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "info_pulse"
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(74.dp)
                        .graphicsLayer { alpha = pulse }
                        .clip(RoundedCornerShape(RadiusMd))
                        .background(OmniSurface2)
                )
            }
        }
        repeat(4) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp)
                    .graphicsLayer { alpha = pulse }
                    .clip(RoundedCornerShape(8.dp))
                    .background(OmniSurface2)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (analyzing) {
                CircularProgressIndicator(color = OmniAccent, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(10.dp))
            }
            Text(if (analyzing) "Analizzo il PDF…" else "Informazioni non disponibili", fontSize = 14.sp, color = OmniTextMuted)
        }
    }
}

@Composable
private fun DetailsContent(
    details: PdfDetails,
    pdf: LoadedPdf,
    metadata: PdfMetadata,
    hasChanges: Boolean,
    isWorking: Boolean,
    onEdit: ((PdfMetadata) -> PdfMetadata) -> Unit,
    onSave: () -> Unit,
    onRemoveAll: () -> Unit
) {
    val minutes = DocStats.readingMinutes(details.words)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.enterAnimation(0)) {
            CountingStat(value = details.pageCount, label = if (details.pageCount == 1) "pagina" else "pagine", modifier = Modifier.weight(1f))
            CountingStat(value = details.words, label = "parole", modifier = Modifier.weight(1f))
            CountingStat(value = minutes, label = if (minutes == 1) "minuto di lettura" else "minuti di lettura", modifier = Modifier.weight(1f))
        }
        when {
            details.words == 0 -> Text(
                "Non ho trovato testo: probabilmente è una scansione. Usa «Immagine in testo» per riconoscerlo.",
                fontSize = 13.sp,
                color = OmniTextMuted
            )
            details.countedPages < details.pageCount -> Text(
                "Parole contate sulle prime ${details.countedPages} pagine.",
                fontSize = 13.sp,
                color = OmniTextMuted
            )
        }

        Text("Documento", style = MaterialTheme.typography.titleMedium, color = OmniText)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(RadiusMd))
                .background(OmniSurface2)
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            infoRows(details, pdf).forEachIndexed { index, (label, value) ->
                InfoRow(label = label, value = value, modifier = Modifier.enterAnimation(index + 1, key = "info_$label"))
            }
        }

        Text("Metadati", style = MaterialTheme.typography.titleMedium, color = OmniText)
        Text("Li vedono i lettori PDF e chi riceve il file. Le modifiche vanno in una copia.", fontSize = 13.sp, color = OmniTextMuted)
        MorphTextField(value = metadata.title, onValueChange = { v -> onEdit { it.copy(title = v) } }, label = "Titolo", placeholder = "Es. Relazione di fisica")
        MorphTextField(value = metadata.author, onValueChange = { v -> onEdit { it.copy(author = v) } }, label = "Autore", placeholder = "Il tuo nome")
        MorphTextField(value = metadata.subject, onValueChange = { v -> onEdit { it.copy(subject = v) } }, label = "Oggetto", placeholder = "Di cosa parla")
        MorphTextField(value = metadata.keywords, onValueChange = { v -> onEdit { it.copy(keywords = v) } }, label = "Parole chiave", placeholder = "Separate da virgola")

        OmniActionBar(
            summaryText = if (hasChanges) "Metadati modificati" else "Nessuna modifica",
            detailText = formatBytes(pdf.sizeBytes),
            primaryButtonLabel = if (isWorking) "Salvo…" else "Salva copia",
            isEnabled = hasChanges && !isWorking,
            disabledHelperText = if (!hasChanges && !isWorking) "Cambia un campo per salvare" else null,
            onPrimaryAction = onSave,
            testTag = "info_save_button",
            secondaryAction = {
                TextButton(onClick = onRemoveAll, enabled = !isWorking) {
                    Text("Togli tutti", color = OmniDanger, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }
}

/** Big number that counts up from zero the first time it appears. */
@Composable
private fun CountingStat(value: Int, label: String, modifier: Modifier = Modifier) {
    var started by rememberSaveable(value) { mutableStateOf(false) }
    LaunchedEffect(value) { started = true }
    val shown by animateIntAsState(if (started) value else 0, motionSpec(tween(900)), label = "count_$label")
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(RadiusMd))
            .background(OmniAccentSoft)
            .padding(horizontal = 12.dp, vertical = 14.dp)
    ) {
        Text(
            text = NumberFormat.getIntegerInstance(Locale.ITALY).format(shown),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = OmniAccent,
            maxLines = 1
        )
        Text(label, fontSize = 12.sp, color = OmniTextMuted, maxLines = 2)
    }
}

@Composable
private fun InfoRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(label, fontSize = 14.sp, color = OmniTextMuted, modifier = Modifier.width(120.dp))
        Text(
            value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = OmniText,
            textAlign = TextAlign.End,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun infoRows(d: PdfDetails, pdf: LoadedPdf): List<Pair<String, String>> {
    val dateFormat = SimpleDateFormat("d MMM yyyy, HH:mm", Locale.ITALY)
    val wMm = DocStats.ptToMm(d.pageWidthPt).roundToInt()
    val hMm = DocStats.ptToMm(d.pageHeightPt).roundToInt()
    val paper = DocStats.paperName(d.pageWidthPt, d.pageHeightPt)
    val orientation = if (d.pageWidthPt > d.pageHeightPt) "orizzontale" else "verticale"
    return buildList {
        add("Formato" to listOfNotNull(paper, "$wMm × $hMm mm", orientation).joinToString(" · ") + if (d.mixedPageSizes) " (pagine di misure diverse)" else "")
        add("Dimensione" to formatBytes(pdf.sizeBytes))
        add("Caratteri" to NumberFormat.getIntegerInstance(Locale.ITALY).format(d.characters))
        add("Immagini" to "${d.imageCount}")
        add("Font" to "${d.fontCount}")
        add("Versione PDF" to String.format(Locale.ITALY, "%.1f", d.version))
        d.createdAt?.let { add("Creato" to dateFormat.format(Date(it))) }
        d.modifiedAt?.let { add("Modificato" to dateFormat.format(Date(it))) }
        if (d.creator.isNotBlank()) add("Creato con" to d.creator)
        if (d.producer.isNotBlank()) add("Prodotto da" to d.producer)
    }
}
