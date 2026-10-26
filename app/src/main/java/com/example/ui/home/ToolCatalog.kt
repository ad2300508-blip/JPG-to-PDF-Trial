package com.example.ui.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MergeType
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.theme.TintAudio
import com.example.ui.theme.TintImage
import com.example.ui.theme.TintMerge
import com.example.ui.theme.TintOcr
import com.example.ui.theme.TintPdf
import com.example.ui.theme.TintZip

enum class ToolCategory(val label: String) {
    DOCUMENTS("Documenti"),
    IMAGES("Immagini"),
    AUDIO("Audio"),
    ARCHIVES("Archivi")
}

/** Screen that actually implements a tool (several catalog entries can share one). */
enum class ToolScreen { IMAGES_TO_PDF, MERGE_PDF, MARKDOWN, IMAGE_LAB, AUDIO_LAB, ARCHIVE }

enum class OmniTool(
    val title: String,
    val subtitle: String,
    val description: String,
    val icon: ImageVector,
    val tint: Color,
    val categories: Set<ToolCategory>,
    val screen: ToolScreen,
    val keywords: String
) {
    IMAGES_TO_PDF(
        title = "Immagini in PDF",
        subtitle = "JPG, PNG, HEIC",
        description = "Unisci foto e scansioni in un unico PDF impaginato.",
        icon = Icons.Default.PhotoLibrary,
        tint = TintPdf,
        categories = setOf(ToolCategory.DOCUMENTS, ToolCategory.IMAGES),
        screen = ToolScreen.IMAGES_TO_PDF,
        keywords = "jpg png heic foto scansione pdf immagini"
    ),
    PDF_TO_MARKDOWN(
        title = "PDF in Markdown",
        subtitle = "PDF → MD",
        description = "Estrai testo pulito da PDF e appunti, pronto per l'AI.",
        icon = Icons.Default.TextSnippet,
        tint = TintPdf,
        categories = setOf(ToolCategory.DOCUMENTS),
        screen = ToolScreen.MARKDOWN,
        keywords = "pdf markdown md testo ai prompt docx txt"
    ),
    MERGE_PDF(
        title = "Unisci PDF",
        subtitle = "PDF + PDF",
        description = "Combina più PDF in un documento ordinato, anche protetto.",
        icon = Icons.AutoMirrored.Filled.MergeType,
        tint = TintMerge,
        categories = setOf(ToolCategory.DOCUMENTS),
        screen = ToolScreen.MERGE_PDF,
        keywords = "unisci merge pdf combina password"
    ),
    IMAGE_CONVERT(
        title = "Converti immagini",
        subtitle = "JPG, PNG, WebP",
        description = "Cambia formato, comprimi e ridimensiona le immagini.",
        icon = Icons.Default.Image,
        tint = TintImage,
        categories = setOf(ToolCategory.IMAGES),
        screen = ToolScreen.IMAGE_LAB,
        keywords = "immagini converti comprimi webp jpg png exif ridimensiona"
    ),
    IMAGE_TO_TEXT(
        title = "Immagine in testo",
        subtitle = "OCR · JPG, PNG, PDF",
        description = "Riconosci il testo da foto e scansioni.",
        icon = Icons.Default.DocumentScanner,
        tint = TintOcr,
        categories = setOf(ToolCategory.DOCUMENTS, ToolCategory.IMAGES),
        screen = ToolScreen.MARKDOWN,
        keywords = "ocr testo immagine riconoscimento scansione"
    ),
    AUDIO_CONVERT(
        title = "Converti audio",
        subtitle = "M4A, WAV",
        description = "Converti registrazioni e lezioni, o trascrivile con Gemini.",
        icon = Icons.Default.Audiotrack,
        tint = TintAudio,
        categories = setOf(ToolCategory.AUDIO),
        screen = ToolScreen.AUDIO_LAB,
        keywords = "audio m4a wav mp3 voce lezione trascrivi"
    ),
    ZIP_ARCHIVE(
        title = "Crea archivio ZIP",
        subtitle = "File → ZIP",
        description = "Comprimi file e documenti in un archivio ZIP.",
        icon = Icons.Default.Archive,
        tint = TintZip,
        categories = setOf(ToolCategory.ARCHIVES),
        screen = ToolScreen.ARCHIVE,
        keywords = "zip archivio comprimi file"
    )
}
