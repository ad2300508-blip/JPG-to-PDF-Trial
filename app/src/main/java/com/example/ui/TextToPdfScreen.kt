package com.example.ui

import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AnimatedText
import com.example.ui.components.ContinueAction
import com.example.ui.components.ConversionResultView
import com.example.ui.components.MorphTextField
import com.example.ui.components.OmniActionBar
import com.example.ui.components.OmniToast
import com.example.ui.components.RevealVertically
import com.example.ui.components.ToolHeader
import com.example.ui.components.morphTextFieldColors
import com.example.ui.theme.OmniAccent
import com.example.ui.theme.OmniText
import com.example.ui.theme.OmniTextMuted
import com.example.ui.theme.RadiusSm
import com.example.ui.theme.TintPdf
import com.example.util.DocStats
import com.example.util.OutputFolder
import com.example.util.OutputStorage
import com.example.viewmodel.TextToPdfViewModel

@Composable
fun TextToPdfScreen(
    viewModel: TextToPdfViewModel,
    modifier: Modifier = Modifier,
    onOpenInReader: (Uri) -> Unit = {},
    onCompress: (Uri) -> Unit = {}
) {
    val title by viewModel.title.collectAsState()
    val text by viewModel.text.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val resultUri by viewModel.resultUri.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val clipboard = LocalClipboardManager.current

    AnimatedContent(
        targetState = resultUri != null,
        transitionSpec = { stageTransition(if (initialState) 1 else 0, if (targetState) 1 else 0) },
        modifier = modifier,
        label = "text_pdf_stage"
    ) { showResult ->
        val uri = resultUri
        if (showResult && uri != null) {
            ConversionResultView(
                title = "PDF creato",
                subtitle = "Il testo è impaginato su fogli A4 con i numeri di pagina.",
                outputUri = uri,
                mimeType = "application/pdf",
                previewIcon = Icons.Default.EditNote,
                previewTint = TintPdf,
                savedIn = "Salvato in ${OutputStorage.displayPath(OutputFolder.PDFS)}",
                onClose = { viewModel.dismissResult() },
                onNewConversion = { viewModel.startOver() },
                continueActions = listOf(
                    ContinueAction("Leggi il PDF", Icons.AutoMirrored.Filled.MenuBook, TintPdf) { onOpenInReader(uri) },
                    ContinueAction("Comprimi", Icons.Default.Compress, TintPdf) { onCompress(uri) }
                )
            )
        } else {
            val words = remember(text) { DocStats.words(text) }
            val characters = remember(text) { DocStats.characters(text) }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                ToolHeader(title = "Testo in PDF", description = "Scrivi o incolla un testo e salvalo in PDF")
                RevealVertically(visible = errorMessage != null) {
                    OmniToast(
                        message = errorMessage.orEmpty(),
                        isError = true,
                        actionLabel = "Chiudi",
                        onAction = { viewModel.dismissError() },
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                }
                MorphTextField(
                    value = title,
                    onValueChange = viewModel::setTitle,
                    label = "Titolo",
                    placeholder = "Es. Appunti di storia",
                    testTag = "text_pdf_title"
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Testo", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = OmniText, modifier = Modifier.weight(1f))
                    TextButton(onClick = { clipboard.getText()?.text?.takeIf { it.isNotBlank() }?.let(viewModel::appendText) }) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, tint = OmniAccent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Incolla", color = OmniAccent, fontWeight = FontWeight.SemiBold)
                    }
                    AnimatedVisibility(visible = text.isNotEmpty(), enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
                        TextButton(onClick = { viewModel.clearText() }) {
                            Text("Svuota", color = OmniTextMuted)
                        }
                    }
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = viewModel::setText,
                    placeholder = {
                        Text(
                            "Scrivi qui. Usa # per un titolo, ## per un sottotitolo e - per gli elenchi.",
                            color = OmniTextMuted,
                            fontSize = 14.sp
                        )
                    },
                    minLines = 6,
                    shape = RoundedCornerShape(RadiusSm),
                    colors = morphTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("text_pdf_body")
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    AnimatedText(text = if (words == 1) "1 parola" else "$words parole", fontSize = 13.sp, color = OmniTextMuted)
                    AnimatedText(text = "$characters caratteri", fontSize = 13.sp, color = OmniTextMuted)
                    AnimatedText(text = "≈ ${DocStats.readingMinutes(words)} min di lettura", fontSize = 13.sp, color = OmniTextMuted)
                }
                val pages = DocStats.estimatedPages(words)
                OmniActionBar(
                    summaryText = if (pages == 1) "Circa 1 pagina A4" else "Circa $pages pagine A4",
                    detailText = title.ifBlank { "Senza titolo: lo chiamo «Appunti»" },
                    primaryButtonLabel = if (isSaving) "Creo…" else "Crea PDF",
                    isEnabled = text.isNotBlank() && !isSaving,
                    disabledHelperText = if (text.isBlank()) "Scrivi o incolla un testo" else null,
                    onPrimaryAction = { viewModel.save() },
                    testTag = "text_pdf_create"
                )
            }
        }
    }
}
