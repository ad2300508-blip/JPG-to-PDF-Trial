package com.example.ui.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MergeType
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.AutoAwesomeMosaic
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Gif
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.theme.TintAudio
import com.example.ui.theme.TintImage
import com.example.ui.theme.TintMerge
import com.example.ui.theme.TintOcr
import com.example.ui.theme.TintPdf
import com.example.ui.theme.TintSecure
import com.example.ui.theme.TintStamp
import com.example.ui.theme.TintZip

enum class ToolCategory(val label: String) {
    DOCUMENTS("Documenti"),
    IMAGES("Immagini"),
    COMPRESSION("Compressione"),
    AUDIO("Audio"),
    ARCHIVES("Archivi")
}

/** Screen that actually implements a tool (several catalog entries can share one). */
enum class ToolScreen { IMAGES_TO_PDF, MERGE_PDF, MARKDOWN, IMAGE_LAB, AUDIO_LAB, ARCHIVE, COMPRESS_PDF, ORGANIZE_PDF, PDF_TO_IMAGES, EXTRACT_ZIP, STAMP_PDF, SECURE_PDF, SPLIT_PDF, SIGN_PDF, COLLAGE, GIF_MAKER }

enum class OmniTool(
    val title: String,
    val subtitle: String,
    val description: String,
    val icon: ImageVector,
    val tint: Color,
    val categories: Set<ToolCategory>,
    val screen: ToolScreen,
    val keywords: String,
    /** MIME prefixes this tool accepts, used to suggest tools for files shared from other apps. */
    val accepts: List<String> = emptyList(),
    val isNew: Boolean = false
) {
    IMAGES_TO_PDF(
        title = "Immagini in PDF",
        subtitle = "JPG, PNG, HEIC",
        description = "Unisci foto e scansioni in un unico PDF impaginato.",
        icon = Icons.Default.PhotoLibrary,
        tint = TintPdf,
        categories = setOf(ToolCategory.DOCUMENTS, ToolCategory.IMAGES),
        screen = ToolScreen.IMAGES_TO_PDF,
        keywords = "jpg png heic foto scansione pdf immagini",
        accepts = listOf("image/")
    ),
    PDF_TO_MARKDOWN(
        title = "PDF in Markdown",
        subtitle = "PDF → MD",
        description = "Estrai testo pulito da PDF e appunti, pronto per l'AI.",
        icon = Icons.Default.TextSnippet,
        tint = TintPdf,
        categories = setOf(ToolCategory.DOCUMENTS),
        screen = ToolScreen.MARKDOWN,
        keywords = "pdf markdown md testo ai prompt docx word txt",
        accepts = listOf("application/pdf", "text/", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
    ),
    MERGE_PDF(
        title = "Unisci PDF",
        subtitle = "PDF + PDF",
        description = "Combina più PDF in un documento ordinato, anche protetto.",
        icon = Icons.AutoMirrored.Filled.MergeType,
        tint = TintMerge,
        categories = setOf(ToolCategory.DOCUMENTS),
        screen = ToolScreen.MERGE_PDF,
        keywords = "unisci merge pdf combina password pagine",
        accepts = listOf("application/pdf")
    ),
    COMPRESS_PDF(
        title = "Comprimi PDF",
        subtitle = "PDF più leggero",
        description = "Riduci il peso di PDF scansionati per email e WhatsApp.",
        icon = Icons.Default.Compress,
        tint = TintPdf,
        categories = setOf(ToolCategory.DOCUMENTS, ToolCategory.COMPRESSION),
        screen = ToolScreen.COMPRESS_PDF,
        keywords = "comprimi riduci peso pdf leggero email whatsapp",
        accepts = listOf("application/pdf"),
        isNew = true
    ),
    ORGANIZE_PDF(
        title = "Organizza pagine",
        subtitle = "Riordina, ruota, estrai",
        description = "Riordina, ruota, elimina o estrai pagine da un PDF.",
        icon = Icons.Default.ViewModule,
        tint = TintMerge,
        categories = setOf(ToolCategory.DOCUMENTS),
        screen = ToolScreen.ORGANIZE_PDF,
        keywords = "organizza pagine ruota elimina estrai dividi split riordina pdf",
        accepts = listOf("application/pdf"),
        isNew = true
    ),
    SPLIT_PDF(
        title = "Dividi PDF",
        subtitle = "Un PDF → più file",
        description = "Spezza un PDF ogni N pagine, pagina per pagina o per intervalli.",
        icon = Icons.Default.ContentCut,
        tint = TintStamp,
        categories = setOf(ToolCategory.DOCUMENTS),
        screen = ToolScreen.SPLIT_PDF,
        keywords = "dividi split separa spezza capitoli pagine pdf",
        accepts = listOf("application/pdf"),
        isNew = true
    ),
    PDF_TO_IMAGES(
        title = "PDF in immagini",
        subtitle = "PDF → JPG, PNG",
        description = "Salva le pagine di un PDF come immagini.",
        icon = Icons.Default.Collections,
        tint = TintImage,
        categories = setOf(ToolCategory.DOCUMENTS, ToolCategory.IMAGES),
        screen = ToolScreen.PDF_TO_IMAGES,
        keywords = "pdf immagini jpg png pagine esporta",
        accepts = listOf("application/pdf"),
        isNew = true
    ),
    STAMP_PDF(
        title = "Filigrana e numeri",
        subtitle = "Timbra le pagine",
        description = "Aggiungi una filigrana o i numeri di pagina a un PDF.",
        icon = Icons.Default.WaterDrop,
        tint = TintStamp,
        categories = setOf(ToolCategory.DOCUMENTS),
        screen = ToolScreen.STAMP_PDF,
        keywords = "filigrana watermark timbro bozza numeri pagina numera pdf",
        accepts = listOf("application/pdf"),
        isNew = true
    ),
    SIGN_PDF(
        title = "Firma PDF",
        subtitle = "Firma col dito",
        description = "Disegna la tua firma e mettila su un modulo o una giustificazione.",
        icon = Icons.Default.Draw,
        tint = TintStamp,
        categories = setOf(ToolCategory.DOCUMENTS),
        screen = ToolScreen.SIGN_PDF,
        keywords = "firma firmare sign autografo modulo giustificazione autorizzazione gita pdf",
        accepts = listOf("application/pdf"),
        isNew = true
    ),
    SECURE_PDF(
        title = "Proteggi PDF",
        subtitle = "Password on/off",
        description = "Metti o togli la password a un PDF.",
        icon = Icons.Default.Lock,
        tint = TintSecure,
        categories = setOf(ToolCategory.DOCUMENTS),
        screen = ToolScreen.SECURE_PDF,
        keywords = "password proteggi sblocca cifra crittografa sicurezza lucchetto pdf",
        accepts = listOf("application/pdf"),
        isNew = true
    ),
    IMAGE_CONVERT(
        title = "Converti immagini",
        subtitle = "JPG, PNG, WebP",
        description = "Cambia formato, comprimi e ridimensiona le immagini.",
        icon = Icons.Default.Image,
        tint = TintImage,
        categories = setOf(ToolCategory.IMAGES, ToolCategory.COMPRESSION),
        screen = ToolScreen.IMAGE_LAB,
        keywords = "immagini converti comprimi webp jpg png exif ridimensiona",
        accepts = listOf("image/")
    ),
    COLLAGE(
        title = "Collage foto",
        subtitle = "Foto → 1 immagine",
        description = "Unisci più foto in colonna, in riga o a griglia.",
        icon = Icons.Default.AutoAwesomeMosaic,
        tint = TintImage,
        categories = setOf(ToolCategory.IMAGES),
        screen = ToolScreen.COLLAGE,
        keywords = "collage unisci foto immagini griglia mosaico affianca combina",
        accepts = listOf("image/"),
        isNew = true
    ),
    GIF_MAKER(
        title = "GIF animata",
        subtitle = "Foto → GIF",
        description = "Trasforma una serie di foto in una GIF animata.",
        icon = Icons.Default.Gif,
        tint = TintImage,
        categories = setOf(ToolCategory.IMAGES),
        screen = ToolScreen.GIF_MAKER,
        keywords = "gif animazione animata foto fotogrammi boomerang sticker meme",
        accepts = listOf("image/"),
        isNew = true
    ),
    IMAGE_TO_TEXT(
        title = "Immagine in testo",
        subtitle = "OCR · JPG, PNG, PDF",
        description = "Riconosci il testo da foto e scansioni.",
        icon = Icons.Default.DocumentScanner,
        tint = TintOcr,
        categories = setOf(ToolCategory.DOCUMENTS, ToolCategory.IMAGES),
        screen = ToolScreen.MARKDOWN,
        keywords = "ocr testo immagine riconoscimento scansione",
        accepts = listOf("image/")
    ),
    AUDIO_CONVERT(
        title = "Converti audio",
        subtitle = "M4A, WAV · taglia",
        description = "Converti, taglia e alza il volume di lezioni, o trascrivile con Gemini.",
        icon = Icons.Default.Audiotrack,
        tint = TintAudio,
        categories = setOf(ToolCategory.AUDIO),
        screen = ToolScreen.AUDIO_LAB,
        keywords = "audio m4a wav mp3 voce lezione trascrivi taglia normalizza",
        accepts = listOf("audio/", "video/")
    ),
    ZIP_ARCHIVE(
        title = "Crea archivio ZIP",
        subtitle = "File → ZIP",
        description = "Comprimi file e documenti in un archivio ZIP.",
        icon = Icons.Default.Archive,
        tint = TintZip,
        categories = setOf(ToolCategory.ARCHIVES),
        screen = ToolScreen.ARCHIVE,
        keywords = "zip archivio comprimi file",
        accepts = listOf("")
    ),
    EXTRACT_ZIP(
        title = "Estrai ZIP",
        subtitle = "ZIP → file",
        description = "Apri un archivio ZIP e salva i file che contiene.",
        icon = Icons.Default.Unarchive,
        tint = TintZip,
        categories = setOf(ToolCategory.ARCHIVES),
        screen = ToolScreen.EXTRACT_ZIP,
        keywords = "estrai decomprimi unzip zip archivio",
        accepts = listOf("application/zip", "application/x-zip"),
        isNew = true
    )
}

/** Tools that can work with a file of [mimeType] (for files shared from other apps). */
fun toolsFor(mimeType: String): List<OmniTool> = OmniTool.entries.filter { tool ->
    tool.accepts.any { prefix -> prefix.isEmpty() || mimeType.startsWith(prefix) }
}.sortedBy { if (it.accepts.contains("")) 1 else 0 }
