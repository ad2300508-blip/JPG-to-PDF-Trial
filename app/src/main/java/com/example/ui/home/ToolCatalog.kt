package com.example.ui.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.MergeType
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.AutoAwesomeMosaic
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Gif
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material.icons.filled.FlipToBack
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
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
import com.example.util.SearchText

/** Home sections, in the order they are shown; [chip] is the short label of the filter chip. */
enum class ToolGroup(val label: String, val chip: String) {
    CREATE("Crea PDF", "Crea"),
    EDIT("Modifica PDF", "Modifica"),
    SIGN("Firma e proteggi", "Firma"),
    READ("Leggi ed estrai", "Estrai"),
    PHOTOS("Foto", "Foto"),
    MEDIA("Audio e archivi", "Audio e ZIP")
}

/** Screen that actually implements a tool (several catalog entries can share one). */
enum class ToolScreen { IMAGES_TO_PDF, MERGE_PDF, MARKDOWN, IMAGE_LAB, AUDIO_LAB, ARCHIVE, COMPRESS_PDF, ORGANIZE_PDF, PDF_TO_IMAGES, EXTRACT_ZIP, STAMP_PDF, SECURE_PDF, SPLIT_PDF, SIGN_PDF, COLLAGE, GIF_MAKER, PHOTO_FILTERS, PDF_READER, PDF_INFO, TEXT_TO_PDF, BLANK_PAGES, NUP_PDF, NOTES_PDF, DUPLEX_PDF, CROP_PDF }

enum class OmniTool(
    val title: String,
    val subtitle: String,
    val description: String,
    val icon: ImageVector,
    val tint: Color,
    val group: ToolGroup,
    val screen: ToolScreen,
    val keywords: String,
    /** MIME prefixes this tool accepts, used to suggest tools for files shared from other apps. */
    val accepts: List<String> = emptyList()
) {
    IMAGES_TO_PDF(
        title = "Immagini in PDF",
        subtitle = "JPG, PNG, HEIC",
        description = "Unisci foto e scansioni in un unico PDF impaginato.",
        icon = Icons.Default.PhotoLibrary,
        tint = TintPdf,
        group = ToolGroup.CREATE,
        screen = ToolScreen.IMAGES_TO_PDF,
        keywords = "jpg png heic foto scansione pdf immagini",
        accepts = listOf("image/")
    ),
    TEXT_TO_PDF(
        title = "Testo in PDF",
        subtitle = "Scrivi → PDF",
        description = "Scrivi o incolla un testo e salvalo come PDF impaginato.",
        icon = Icons.Default.EditNote,
        tint = TintPdf,
        group = ToolGroup.CREATE,
        screen = ToolScreen.TEXT_TO_PDF,
        keywords = "testo scrivi incolla appunti note txt documento crea pdf"
    ),
    MERGE_PDF(
        title = "Unisci PDF",
        subtitle = "PDF + PDF",
        description = "Combina più PDF in un documento ordinato, anche protetto.",
        icon = Icons.AutoMirrored.Filled.MergeType,
        tint = TintMerge,
        group = ToolGroup.CREATE,
        screen = ToolScreen.MERGE_PDF,
        keywords = "unisci merge pdf combina password pagine",
        accepts = listOf("application/pdf")
    ),
    DUPLEX_PDF(
        title = "Unisci fronte/retro",
        subtitle = "Scansioni a due facciate",
        description = "Alterna fronti e retro scansionati separatamente nell'ordine giusto.",
        icon = Icons.Default.FlipToBack,
        tint = TintMerge,
        group = ToolGroup.CREATE,
        screen = ToolScreen.DUPLEX_PDF,
        keywords = "fronte retro duplex scansione due facciate alterna ordina pagine pdf",
        accepts = listOf("application/pdf")
    ),
    ORGANIZE_PDF(
        title = "Organizza pagine",
        subtitle = "Riordina, ruota, estrai",
        description = "Riordina, ruota, elimina o estrai pagine da un PDF.",
        icon = Icons.Default.ViewModule,
        tint = TintMerge,
        group = ToolGroup.EDIT,
        screen = ToolScreen.ORGANIZE_PDF,
        keywords = "organizza pagine ruota elimina estrai dividi split riordina pdf",
        accepts = listOf("application/pdf")
    ),
    SPLIT_PDF(
        title = "Dividi PDF",
        subtitle = "Un PDF → più file",
        description = "Spezza un PDF ogni N pagine, pagina per pagina o per intervalli.",
        icon = Icons.Default.ContentCut,
        tint = TintStamp,
        group = ToolGroup.EDIT,
        screen = ToolScreen.SPLIT_PDF,
        keywords = "dividi split separa spezza capitoli pagine pdf",
        accepts = listOf("application/pdf")
    ),
    COMPRESS_PDF(
        title = "Comprimi PDF",
        subtitle = "PDF più leggero",
        description = "Riduci il peso di PDF scansionati per email e WhatsApp.",
        icon = Icons.Default.Compress,
        tint = TintPdf,
        group = ToolGroup.EDIT,
        screen = ToolScreen.COMPRESS_PDF,
        keywords = "comprimi riduci peso pdf leggero email whatsapp",
        accepts = listOf("application/pdf")
    ),
    BLANK_PAGES(
        title = "Togli pagine bianche",
        subtitle = "Trova le pagine vuote",
        description = "Trova ed elimina da sole le pagine bianche di una scansione.",
        icon = Icons.Default.LayersClear,
        tint = TintMerge,
        group = ToolGroup.EDIT,
        screen = ToolScreen.BLANK_PAGES,
        keywords = "pagine bianche vuote elimina togli pulisci scansione pdf",
        accepts = listOf("application/pdf")
    ),
    CROP_PDF(
        title = "Ritaglia margini",
        subtitle = "Togli i bordi bianchi",
        description = "Ritaglia i margini di tutte le pagine, anche in automatico.",
        icon = Icons.Default.Crop,
        tint = TintStamp,
        group = ToolGroup.EDIT,
        screen = ToolScreen.CROP_PDF,
        keywords = "ritaglia margini bordi crop taglia ingrandisci leggibile pdf",
        accepts = listOf("application/pdf")
    ),
    NUP_PDF(
        title = "Più pagine per foglio",
        subtitle = "2 o 4 per A4",
        description = "Metti 2 o 4 pagine su ogni foglio per stampare risparmiando carta.",
        icon = Icons.Default.Dashboard,
        tint = TintMerge,
        group = ToolGroup.EDIT,
        screen = ToolScreen.NUP_PDF,
        keywords = "piu pagine per foglio stampa risparmia carta slide dispense nup pdf",
        accepts = listOf("application/pdf")
    ),
    NOTES_PDF(
        title = "Spazio per appunti",
        subtitle = "Righe accanto alle pagine",
        description = "Rimpicciolisci le pagine e aggiungi righe o quadretti per gli appunti.",
        icon = Icons.Default.NoteAlt,
        tint = TintStamp,
        group = ToolGroup.EDIT,
        screen = ToolScreen.NOTES_PDF,
        keywords = "appunti note righe quadretti margine stampa slide studio pdf",
        accepts = listOf("application/pdf")
    ),
    SIGN_PDF(
        title = "Firma PDF",
        subtitle = "Firma col dito",
        description = "Disegna la tua firma e mettila su un modulo o una giustificazione.",
        icon = Icons.Default.Draw,
        tint = TintStamp,
        group = ToolGroup.SIGN,
        screen = ToolScreen.SIGN_PDF,
        keywords = "firma firmare sign autografo modulo giustificazione autorizzazione gita pdf",
        accepts = listOf("application/pdf")
    ),
    STAMP_PDF(
        title = "Filigrana e numeri",
        subtitle = "Timbra le pagine",
        description = "Aggiungi una filigrana o i numeri di pagina a un PDF.",
        icon = Icons.Default.WaterDrop,
        tint = TintStamp,
        group = ToolGroup.SIGN,
        screen = ToolScreen.STAMP_PDF,
        keywords = "filigrana watermark timbro bozza numeri pagina numera pdf",
        accepts = listOf("application/pdf")
    ),
    SECURE_PDF(
        title = "Proteggi PDF",
        subtitle = "Password on/off",
        description = "Metti o togli la password a un PDF.",
        icon = Icons.Default.Lock,
        tint = TintSecure,
        group = ToolGroup.SIGN,
        screen = ToolScreen.SECURE_PDF,
        keywords = "password proteggi sblocca cifra crittografa sicurezza lucchetto pdf",
        accepts = listOf("application/pdf")
    ),
    PDF_READER(
        title = "Leggi PDF",
        subtitle = "Sfoglia e ingrandisci",
        description = "Leggi un PDF con zoom, miniature e modalità notte.",
        icon = Icons.AutoMirrored.Filled.MenuBook,
        tint = TintPdf,
        group = ToolGroup.READ,
        screen = ToolScreen.PDF_READER,
        keywords = "leggi visualizza apri sfoglia lettore viewer notte zoom libro pdf",
        accepts = listOf("application/pdf")
    ),
    PDF_INFO(
        title = "Info PDF",
        subtitle = "Pagine, parole, metadati",
        description = "Scopri formato, parole e tempo di lettura; modifica o togli i metadati.",
        icon = Icons.Default.Info,
        tint = TintPdf,
        group = ToolGroup.READ,
        screen = ToolScreen.PDF_INFO,
        keywords = "info proprieta metadati autore titolo parole statistiche tempo lettura formato pdf",
        accepts = listOf("application/pdf")
    ),
    PDF_TO_MARKDOWN(
        title = "PDF in Markdown",
        subtitle = "PDF → MD",
        description = "Estrai testo pulito da PDF e appunti, pronto per l'AI.",
        icon = Icons.Default.TextSnippet,
        tint = TintPdf,
        group = ToolGroup.READ,
        screen = ToolScreen.MARKDOWN,
        keywords = "pdf markdown md testo ai prompt docx word txt",
        accepts = listOf("application/pdf", "text/", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
    ),
    IMAGE_TO_TEXT(
        title = "Immagine in testo",
        subtitle = "OCR · JPG, PNG, PDF",
        description = "Riconosci il testo da foto e scansioni.",
        icon = Icons.Default.DocumentScanner,
        tint = TintOcr,
        group = ToolGroup.READ,
        screen = ToolScreen.MARKDOWN,
        keywords = "ocr testo immagine riconoscimento scansione",
        accepts = listOf("image/")
    ),
    PDF_TO_IMAGES(
        title = "PDF in immagini",
        subtitle = "PDF → JPG, PNG",
        description = "Salva le pagine di un PDF come immagini.",
        icon = Icons.Default.Collections,
        tint = TintImage,
        group = ToolGroup.READ,
        screen = ToolScreen.PDF_TO_IMAGES,
        keywords = "pdf immagini jpg png pagine esporta",
        accepts = listOf("application/pdf")
    ),
    IMAGE_CONVERT(
        title = "Converti immagini",
        subtitle = "JPG, PNG, WebP",
        description = "Cambia formato, comprimi e ridimensiona le immagini.",
        icon = Icons.Default.Image,
        tint = TintImage,
        group = ToolGroup.PHOTOS,
        screen = ToolScreen.IMAGE_LAB,
        keywords = "immagini converti comprimi webp jpg png exif ridimensiona",
        accepts = listOf("image/")
    ),
    PHOTO_FILTERS(
        title = "Filtri foto",
        subtitle = "Vivace, Seppia, B/N…",
        description = "Applica un filtro a più foto, con confronto prima/dopo.",
        icon = Icons.Default.AutoFixHigh,
        tint = TintImage,
        group = ToolGroup.PHOTOS,
        screen = ToolScreen.PHOTO_FILTERS,
        keywords = "filtri filtro foto seppia bianco nero vintage colori ritocca effetto",
        accepts = listOf("image/")
    ),
    COLLAGE(
        title = "Collage foto",
        subtitle = "Foto → 1 immagine",
        description = "Unisci più foto in colonna, in riga o a griglia.",
        icon = Icons.Default.AutoAwesomeMosaic,
        tint = TintImage,
        group = ToolGroup.PHOTOS,
        screen = ToolScreen.COLLAGE,
        keywords = "collage unisci foto immagini griglia mosaico affianca combina",
        accepts = listOf("image/")
    ),
    GIF_MAKER(
        title = "GIF animata",
        subtitle = "Foto → GIF",
        description = "Trasforma una serie di foto in una GIF animata.",
        icon = Icons.Default.Gif,
        tint = TintImage,
        group = ToolGroup.PHOTOS,
        screen = ToolScreen.GIF_MAKER,
        keywords = "gif animazione animata foto fotogrammi boomerang sticker meme",
        accepts = listOf("image/")
    ),
    AUDIO_CONVERT(
        title = "Converti audio",
        subtitle = "M4A, WAV · taglia",
        description = "Converti, taglia e alza il volume di lezioni, o trascrivile con Gemini.",
        icon = Icons.Default.Audiotrack,
        tint = TintAudio,
        group = ToolGroup.MEDIA,
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
        group = ToolGroup.MEDIA,
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
        group = ToolGroup.MEDIA,
        screen = ToolScreen.EXTRACT_ZIP,
        keywords = "estrai decomprimi unzip zip archivio",
        accepts = listOf("application/zip", "application/x-zip")
    );

    /** Accent-free, lower-case text matched by the home search (built once, not on every keystroke). */
    val searchIndex: String by lazy { SearchText.fold("$title $subtitle $description $keywords ${group.label}") }
}

/** The tool that produced a history entry (entries store the tool title or an older label). */
fun toolForHistoryLabel(label: String): OmniTool? = OmniTool.entries.firstOrNull { it.title == label } ?: when (label) {
    "Organizza PDF" -> OmniTool.ORGANIZE_PDF
    "Archivio ZIP" -> OmniTool.ZIP_ARCHIVE
    "Filigrana PDF", "Numera pagine" -> OmniTool.STAMP_PDF
    "Sblocca PDF" -> OmniTool.SECURE_PDF
    else -> null
}

/** Tools that can work with a file of [mimeType] (for files shared from other apps). */
fun toolsFor(mimeType: String): List<OmniTool> = OmniTool.entries.filter { tool ->
    tool.accepts.any { prefix -> prefix.isEmpty() || mimeType.startsWith(prefix) }
}.sortedBy {
    when {
        it.accepts.contains("") -> 2
        it == OmniTool.PDF_READER -> 0
        else -> 1
    }
}
