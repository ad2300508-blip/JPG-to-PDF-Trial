package com.example.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.text.format.DateUtils
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import com.example.ui.theme.Haptic
import com.example.ui.theme.LocalReduceMotion
import com.example.ui.theme.Motion
import com.example.ui.theme.enterAnimation
import com.example.ui.theme.motionSpec
import com.example.ui.theme.pressScale
import com.example.ui.theme.rememberHaptics
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
    HistoryKind.OTHER -> "*/*"
}

/** Prefers the real type of the file over a generic wildcard type so viewers pick the right app. */
private fun resolveMime(context: Context, uri: Uri, fallback: String?): String {
    val actual = runCatching { context.contentResolver.getType(uri) }.getOrNull()
    return when {
        actual != null && actual != "application/octet-stream" -> actual
        fallback != null -> fallback
        else -> "*/*"
    }
}

/** Shares several files at once (e.g. all the pages exported as images). */
fun shareFiles(context: Context, uris: List<Uri>, mimeType: String) {
    if (uris.isEmpty()) return
    if (uris.size == 1) return shareFile(context, uris.first(), mimeType)
    val send = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
        type = mimeType
        putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(Intent.createChooser(send, "Condividi file"))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "Nessuna app disponibile per condividere", Toast.LENGTH_SHORT).show()
    }
}

fun shareFile(context: Context, uri: Uri, mimeType: String?) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = resolveMime(context, uri, mimeType)
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
        setDataAndType(uri, resolveMime(context, uri, mimeType))
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
    HistoryKind.OTHER -> Icons.Default.InsertDriveFile
}

fun HistoryKind.tint(): Color = when (this) {
    HistoryKind.PDF -> TintImage
    HistoryKind.MARKDOWN -> TintPdf
    HistoryKind.ZIP -> TintZip
    HistoryKind.IMAGE -> TintMerge
    HistoryKind.AUDIO -> TintAudio
    HistoryKind.OTHER -> TintZip
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

/** Horizontal row of Morph filter chips (selected = solid dark pill, colour animated). */
@Composable
fun MorphChipRow(
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    /** Optional number shown after each label (e.g. how many tools a filter contains). */
    counts: Map<String, Int> = emptyMap()
) {
    val haptics = rememberHaptics()
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            val container by animateColorAsState(if (isSelected) OmniText else OmniSurface, label = "chip_bg")
            val content by animateColorAsState(if (isSelected) OmniSurface else OmniText, label = "chip_fg")
            val interaction = remember { MutableInteractionSource() }
            Surface(
                onClick = {
                    if (!isSelected) haptics(Haptic.TICK)
                    onSelected(option)
                },
                interactionSource = interaction,
                shape = RoundedCornerShape(RadiusPill),
                color = container,
                border = if (isSelected) null else BorderStroke(1.dp, OmniBorder),
                modifier = Modifier
                    .height(44.dp)
                    .pressScale(interaction, 0.94f)
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = option,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = content,
                            maxLines = 1
                        )
                        val count = counts[option]
                        if (count != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            AnimatedContent(
                                targetState = count,
                                transitionSpec = {
                                    (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
                                },
                                label = "chip_count"
                            ) { value ->
                                Text(
                                    text = "$value",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = content.copy(alpha = 0.6f),
                                    maxLines = 1
                                )
                            }
                        }
                    }
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
    val interaction = remember { MutableInteractionSource() }
    Surface(
        onClick = onOpen,
        interactionSource = interaction,
        shape = RoundedCornerShape(RadiusMd + 2.dp),
        color = if (framed) OmniSurface else Color.Transparent,
        border = if (framed) BorderStroke(1.dp, OmniBorder) else null,
        modifier = modifier
            .fillMaxWidth()
            .pressScale(interaction, 0.98f)
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
 * Morph "Il tuo file è pronto" screen: animated success badge, output card with an animated size
 * comparison, share/open buttons, follow-up tools and a "new conversion" link.
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
    outputBytesOverride: Long? = null,
    detailPrefix: String? = null,
    continueActions: List<ContinueAction> = emptyList(),
    savedIn: String = "Salvato in Download/Morph",
    shareUris: List<Uri> = emptyList()
) {
    val context = LocalContext.current
    val haptics = rememberHaptics()
    val info by produceState<OutputFileInfo?>(initialValue = null, outputUri) {
        value = queryOutputFile(context, outputUri)
    }
    val outputBytes = outputBytesOverride ?: info?.sizeBytes ?: 0L
    LaunchedEffect(outputUri) { haptics(Haptic.CONFIRM) }

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

            SuccessBadge()

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = title,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.8).sp,
                color = OmniText,
                textAlign = TextAlign.Center,
                modifier = Modifier.enterAnimation(2, key = outputUri)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                fontSize = 15.sp,
                color = OmniTextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.enterAnimation(3, key = outputUri)
            )
            Spacer(modifier = Modifier.height(22.dp))

            // Output card
            Surface(
                shape = RoundedCornerShape(RadiusCard),
                color = OmniSurface,
                border = BorderStroke(1.dp, OmniBorder),
                shadowElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .enterAnimation(4, key = outputUri)
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
                                text = listOfNotNull(detailPrefix, formatBytes(outputBytes).takeIf { outputBytes > 0 }).joinToString(" · "),
                                fontSize = 13.sp,
                                color = OmniTextMuted
                            )
                            if (originalBytes > 0 && outputBytes > 0) {
                                val delta = ((outputBytes - originalBytes) * 100.0 / originalBytes).toInt()
                                Spacer(modifier = Modifier.height(6.dp))
                                MorphBadge(
                                    text = if (delta <= 0) "−${-delta}% di peso" else "+$delta% di peso",
                                    color = if (delta <= 0) OmniSuccess else OmniTextMuted,
                                    container = if (delta <= 0) OmniSuccessSoft else OmniSurface2
                                )
                            }
                        }
                    }
                    if (originalBytes > 0 && outputBytes > 0) {
                        Spacer(modifier = Modifier.height(14.dp))
                        val maxBytes = maxOf(originalBytes, outputBytes).toFloat()
                        SizeBar("Originali", originalBytes, originalBytes / maxBytes, OmniTextMuted, delayMillis = 350)
                        Spacer(modifier = Modifier.height(6.dp))
                        SizeBar("Risultato", outputBytes, outputBytes / maxBytes, OmniSuccess, delayMillis = 550)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val shareInteraction = remember { MutableInteractionSource() }
            Button(
                onClick = {
                    haptics(Haptic.TAP)
                    if (shareUris.size > 1) shareFiles(context, shareUris, mimeType) else shareFile(context, outputUri, mimeType)
                },
                interactionSource = shareInteraction,
                colors = ButtonDefaults.buttonColors(containerColor = OmniAccent, contentColor = OmniOnAccent),
                shape = RoundedCornerShape(RadiusMd),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .enterAnimation(5, key = outputUri)
                    .pressScale(shareInteraction)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(if (shareUris.size > 1) "Condividi tutti (${shareUris.size})" else "Condividi", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(10.dp))
            val openInteraction = remember { MutableInteractionSource() }
            OutlinedButton(
                onClick = { openFile(context, outputUri, mimeType) },
                interactionSource = openInteraction,
                shape = RoundedCornerShape(RadiusMd),
                border = BorderStroke(1.dp, OmniBorder),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = OmniSurface, contentColor = OmniText),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .enterAnimation(6, key = outputUri)
                    .pressScale(openInteraction)
            ) {
                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Apri", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                text = savedIn,
                fontSize = 12.sp,
                color = OmniTextMuted,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .enterAnimation(6, key = outputUri)
            )

            if (continueActions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Continua con questo file",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = OmniText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .enterAnimation(7, key = outputUri)
                )
                Spacer(modifier = Modifier.height(10.dp))
                continueActions.forEachIndexed { index, action ->
                    val interaction = remember { MutableInteractionSource() }
                    Surface(
                        onClick = action.onClick,
                        interactionSource = interaction,
                        shape = RoundedCornerShape(RadiusMd + 2.dp),
                        color = OmniSurface,
                        border = BorderStroke(1.dp, OmniBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                            .enterAnimation(8 + index, key = outputUri)
                            .pressScale(interaction, 0.98f)
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

/**
 * Animated "done" badge: the halo pops in with a bounce, a ring ripples outward and the
 * check mark draws itself.
 */
@Composable
fun SuccessBadge(modifier: Modifier = Modifier, size: Dp = 104.dp) {
    val reduce = LocalReduceMotion.current
    val pop = remember { Animatable(if (reduce) 1f else 0f) }
    val inner = remember { Animatable(if (reduce) 1f else 0f) }
    val check = remember { Animatable(if (reduce) 1f else 0f) }
    val ripple = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (reduce) return@LaunchedEffect
        launch { pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 300f)) }
        delay(120)
        launch { inner.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 350f)) }
        launch { ripple.animateTo(1f, tween(900, easing = Motion.EmphasizedDecelerate)) }
        delay(220)
        check.animateTo(1f, tween(380, easing = Motion.EmphasizedDecelerate))
    }
    val successSoft = OmniSuccessSoft
    val success = OmniSuccess
    Box(
        modifier = modifier.size(size * 1.5f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val r = ripple.value
            if (r in 0.01f..0.99f) {
                drawCircle(
                    color = success.copy(alpha = (1f - r) * 0.35f),
                    radius = (size.toPx() / 2f) * (1f + 0.45f * r),
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }
        Box(
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    scaleX = pop.value
                    scaleY = pop.value
                }
                .clip(CircleShape)
                .background(successSoft),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(size * 0.65f)
                    .graphicsLayer {
                        scaleX = inner.value
                        scaleY = inner.value
                    }
                    .clip(CircleShape)
                    .background(success),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(size * 0.34f)) {
                    val path = Path().apply {
                        moveTo(this@Canvas.size.width * 0.08f, this@Canvas.size.height * 0.52f)
                        lineTo(this@Canvas.size.width * 0.40f, this@Canvas.size.height * 0.82f)
                        lineTo(this@Canvas.size.width * 0.94f, this@Canvas.size.height * 0.22f)
                    }
                    val measure = PathMeasure().apply { setPath(path, false) }
                    val partial = Path()
                    measure.getSegment(0f, measure.length * check.value, partial, true)
                    drawPath(
                        path = partial,
                        color = Color.White,
                        style = Stroke(width = 4.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }
        }
    }
}

@Composable
private fun SizeBar(label: String, bytes: Long, fraction: Float, color: Color, delayMillis: Int) {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val animatedFraction by animateFloatAsState(
        targetValue = if (started) fraction.coerceIn(0.02f, 1f) else 0.02f,
        animationSpec = motionSpec(tween(700, delayMillis = delayMillis, easing = Motion.EmphasizedDecelerate)),
        label = "size_bar"
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 12.sp, color = OmniTextMuted, modifier = Modifier.width(72.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(CircleShape)
                .background(OmniSurface2)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedFraction)
                    .clip(CircleShape)
                    .background(color)
            )
        }
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

/** Big empty-state block: icon tile, title and hint. */
@Composable
fun EmptyState(icon: ImageVector, title: String, message: String, tint: Color = OmniAccent, modifier: Modifier = Modifier) {
    val reduce = LocalReduceMotion.current
    val transition = rememberInfiniteTransition(label = "empty_state")
    val bob by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (reduce) 0f else 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = Motion.Standard), RepeatMode.Reverse),
        label = "empty_bob"
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconTile(
            icon = icon,
            tint = tint,
            size = 72.dp,
            modifier = Modifier
                .enterAnimation(0)
                .graphicsLayer { translationY = -5.dp.toPx() * bob }
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = OmniText, modifier = Modifier.enterAnimation(1))
        Spacer(modifier = Modifier.height(4.dp))
        Text(message, fontSize = 14.sp, color = OmniTextMuted, textAlign = TextAlign.Center, modifier = Modifier.enterAnimation(2))
    }
}
