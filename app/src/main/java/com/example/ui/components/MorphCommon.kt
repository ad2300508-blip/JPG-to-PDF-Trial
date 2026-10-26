package com.example.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.text.format.DateUtils
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HistoryEntry
import com.example.data.HistoryKind
import com.example.ui.theme.OmniAccent
import com.example.ui.theme.OmniBorder
import com.example.ui.theme.OmniOnAccent
import com.example.ui.theme.OmniSuccess
import com.example.ui.theme.OmniSuccessSoft
import com.example.ui.theme.OmniSurface
import com.example.ui.theme.OmniSurface2
import com.example.ui.theme.OmniText
import com.example.ui.theme.OmniTextMuted
import com.example.ui.theme.RadiusCard
import com.example.ui.theme.RadiusMd
import com.example.ui.theme.RadiusPill
import com.example.ui.theme.TintAudio
import com.example.ui.theme.TintImage
import com.example.ui.theme.TintMerge
import com.example.ui.theme.TintPdf
import com.example.ui.theme.TintZip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "—"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return when {
        mb >= 1.0 -> String.format(Locale.ITALY, "%.1f MB", mb)
        kb >= 1.0 -> String.format(Locale.ITALY, "%.0f KB", kb)
        else -> "$bytes B"
    }
}

fun formatRelativeTime(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    return DateUtils.getRelativeTimeSpanString(
        timestamp,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE
    ).toString()
}

data class OutputFileInfo(val name: String, val sizeBytes: Long, val mimeType: String?)

suspend fun queryOutputFile(context: Context, uri: Uri): OutputFileInfo = withContext(Dispatchers.IO) {
    var name = uri.lastPathSegment ?: "File"
    var size = 0L
    runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIdx >= 0) cursor.getString(nameIdx)?.let { name = it }
                if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) size = cursor.getLong(sizeIdx)
            }
        }
    }
    OutputFileInfo(name, size, runCatching { context.contentResolver.getType(uri) }.getOrNull())
}

fun mimeForKind(kind: HistoryKind): String = when (kind) {
    HistoryKind.PDF -> "application/pdf"
    HistoryKind.MARKDOWN -> "text/markdown"
    HistoryKind.ZIP -> "application/zip"
    HistoryKind.IMAGE -> "image/*"
    HistoryKind.AUDIO -> "audio/*"
}

fun shareFile(context: Context, uri: Uri, mimeType: String?) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = mimeType ?: context.contentResolver.getType(uri) ?: "*/*"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(Intent.createChooser(send, "Condividi file"))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "Nessuna app disponibile per condividere", Toast.LENGTH_SHORT).show()
    }
}

fun openFile(context: Context, uri: Uri, mimeType: String?) {
    val view = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mimeType ?: context.contentResolver.getType(uri) ?: "*/*")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(view)
    } catch (e: Exception) {
        Toast.makeText(context, "Nessuna app trovata per aprire questo file", Toast.LENGTH_SHORT).show()
    }
}

fun HistoryKind.icon(): ImageVector = when (this) {
    HistoryKind.PDF -> Icons.Default.Description
    HistoryKind.MARKDOWN -> Icons.Default.TextSnippet
    HistoryKind.ZIP -> Icons.Default.Archive
    HistoryKind.IMAGE -> Icons.Default.Image
    HistoryKind.AUDIO -> Icons.Default.Audiotrack
}

fun HistoryKind.tint(): Color = when (this) {
    HistoryKind.PDF -> TintImage
    HistoryKind.MARKDOWN -> TintPdf
    HistoryKind.ZIP -> TintZip
    HistoryKind.IMAGE -> TintMerge
    HistoryKind.AUDIO -> TintAudio
}

// ---------------------------------------------------------------------------
// Building blocks
// ---------------------------------------------------------------------------

/** Rounded square with a soft tint background and a tinted icon (Morph tool tile). */
@Composable
fun IconTile(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    filled: Boolean = false
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.3f))
            .background(if (filled) tint else tint.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (filled) Color.White else tint,
            modifier = Modifier.size(size * 0.48f)
        )
    }
}

/** Horizontal row of Morph filter chips (selected = solid dark pill). */
@Composable
fun MorphChipRow(
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Surface(
                onClick = { onSelected(option) },
                shape = RoundedCornerShape(RadiusPill),
                color = if (isSelected) OmniText else OmniSurface,
                border = if (isSelected) null else BorderStroke(1.dp, OmniBorder),
                modifier = Modifier.height(44.dp)
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) OmniSurface else OmniText,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/** Section label in Morph's small caps style (e.g. "OGGI"). */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(Locale.ITALY),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        color = OmniTextMuted,
        modifier = modifier
    )
}

/** One row of the history list / recents panel. */
@Composable
fun HistoryRow(
    entry: HistoryEntry,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDelete: (() -> Unit)?,
    modifier: Modifier = Modifier,
    framed: Boolean = true
) {
    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(RadiusMd + 2.dp),
        color = if (framed) OmniSurface else Color.Transparent,
        border = if (framed) BorderStroke(1.dp, OmniBorder) else null,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (framed) 12.dp else 0.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconTile(icon = entry.kind.icon(), tint = entry.kind.tint(), size = 44.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = OmniText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = listOf(entry.toolLabel, formatBytes(entry.sizeBytes), formatRelativeTime(entry.timestamp))
                        .filter { it.isNotBlank() }
                        .joinToString(" · "),
                    fontSize = 12.sp,
                    color = OmniTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onShare, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.Share, contentDescription = "Condividi", tint = OmniText, modifier = Modifier.size(20.dp))
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Elimina", tint = OmniText, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

data class ContinueAction(
    val label: String,
    val icon: ImageVector,
    val tint: Color,
    val onClick: () -> Unit
)

/**
 * Morph "Il tuo file è pronto" screen: success badge, output card with size comparison,
 * share/open buttons, follow-up tools and a "new conversion" link.
 */
@Composable
fun ConversionResultView(
    title: String,
    subtitle: String,
    outputUri: Uri,
    mimeType: String,
    previewIcon: ImageVector,
    previewTint: Color,
    onClose: () -> Unit,
    onNewConversion: () -> Unit,
    modifier: Modifier = Modifier,
    originalBytes: Long = 0L,
    detailPrefix: String? = null,
    continueActions: List<ContinueAction> = emptyList()
) {
    val context = LocalContext.current
    val info by produceState<OutputFileInfo?>(initialValue = null, outputUri) {
        value = queryOutputFile(context, outputUri)
    }
    val outputBytes = info?.sizeBytes ?: 0L

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Chiudi", tint = OmniText)
                }
            }

            Box(
                modifier = Modifier
                    .size(104.dp)
                    .clip(CircleShape)
                    .background(OmniSuccessSoft),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(OmniSuccess),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = title,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.8).sp,
                color = OmniText,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = subtitle, fontSize = 15.sp, color = OmniTextMuted, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(22.dp))

            // Output card
            Surface(
                shape = RoundedCornerShape(RadiusCard),
                color = OmniSurface,
                border = BorderStroke(1.dp, OmniBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconTile(icon = previewIcon, tint = previewTint, size = 64.dp)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = info?.name ?: "…",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = OmniText,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = listOfNotNull(detailPrefix, formatBytes(outputBytes)).joinToString(" · "),
                                fontSize = 13.sp,
                                color = OmniTextMuted
                            )
                            if (originalBytes > 0 && outputBytes > 0) {
                                val delta = ((outputBytes - originalBytes) * 100.0 / originalBytes).toInt()
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(RadiusPill))
                                        .background(if (delta <= 0) OmniSuccessSoft else OmniSurface2)
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (delta <= 0) "−${-delta}% di peso" else "+$delta% di peso",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (delta <= 0) OmniSuccess else OmniTextMuted
                                    )
                                }
                            }
                        }
                    }
                    if (originalBytes > 0 && outputBytes > 0) {
                        Spacer(modifier = Modifier.height(14.dp))
                        val maxBytes = maxOf(originalBytes, outputBytes).toFloat()
                        SizeBar("Originali", originalBytes, originalBytes / maxBytes, OmniTextMuted)
                        Spacer(modifier = Modifier.height(6.dp))
                        SizeBar("Risultato", outputBytes, outputBytes / maxBytes, OmniSuccess)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { shareFile(context, outputUri, mimeType) },
                colors = ButtonDefaults.buttonColors(containerColor = OmniAccent, contentColor = OmniOnAccent),
                shape = RoundedCornerShape(RadiusMd),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Condividi", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = { openFile(context, outputUri, mimeType) },
                shape = RoundedCornerShape(RadiusMd),
                border = BorderStroke(1.dp, OmniBorder),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = OmniSurface, contentColor = OmniText),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Apri", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                text = "Salvato in Download/OmniConvert",
                fontSize = 12.sp,
                color = OmniTextMuted,
                modifier = Modifier.padding(top = 8.dp)
            )

            if (continueActions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Continua con questo file",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = OmniText,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                continueActions.forEach { action ->
                    Surface(
                        onClick = action.onClick,
                        shape = RoundedCornerShape(RadiusMd + 2.dp),
                        color = OmniSurface,
                        border = BorderStroke(1.dp, OmniBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconTile(icon = action.icon, tint = action.tint, size = 44.dp)
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = action.label,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = OmniText,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = OmniTextMuted)
                        }
                    }
                }
            }

            TextButton(onClick = onNewConversion, modifier = Modifier.padding(vertical = 8.dp)) {
                Text("Nuova conversione", color = OmniAccent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SizeBar(label: String, bytes: Long, fraction: Float, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 12.sp, color = OmniTextMuted, modifier = Modifier.width(72.dp))
        LinearProgressIndicator(
            progress = { fraction.coerceIn(0.02f, 1f) },
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(CircleShape),
            color = color,
            trackColor = OmniSurface2,
            drawStopIndicator = {}
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = formatBytes(bytes),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = OmniText,
            modifier = Modifier.widthIn(min = 56.dp),
            textAlign = TextAlign.End
        )
    }
}
