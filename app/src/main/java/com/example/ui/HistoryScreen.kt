@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.ui

import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HistoryEntry
import com.example.data.HistoryKind
import com.example.data.HistoryRepository
import com.example.ui.components.EmptyState
import com.example.ui.components.HistoryRow
import com.example.ui.components.MorphChipRow
import com.example.ui.components.OmniToast
import com.example.ui.components.RevealVertically
import com.example.ui.components.SectionLabel
import com.example.ui.components.formatBytes
import com.example.ui.components.mimeForKind
import com.example.ui.components.openFile
import com.example.ui.components.shareFile
import com.example.ui.theme.Haptic
import com.example.ui.theme.OmniAccent
import com.example.ui.theme.OmniBorder
import com.example.ui.theme.OmniDanger
import com.example.ui.theme.OmniSurface
import com.example.ui.theme.OmniText
import com.example.ui.theme.OmniTextMuted
import com.example.ui.theme.RadiusCard
import com.example.ui.theme.RadiusMd
import com.example.ui.theme.RadiusPill
import com.example.ui.theme.enterAnimation
import com.example.ui.theme.rememberHaptics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

private const val ALL = "Tutti"

@Composable
fun HistoryScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val entries by HistoryRepository.entries.collectAsState()
    var filter by rememberSaveable { mutableStateOf(ALL) }
    var query by rememberSaveable { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<HistoryEntry?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    var undo by remember { mutableStateOf<Pair<HistoryEntry, Int>?>(null) }
    val scope = rememberCoroutineScope()
    val haptics = rememberHaptics()

    val usedKinds = entries.map { it.kind }.toSet()
    val filters = listOf(ALL) + HistoryKind.entries.filter { it in usedKinds }.map { it.label }
    val normalized = query.trim().lowercase()
    val visible = entries.filter { entry ->
        (filter == ALL || entry.kind.label == filter) &&
            (normalized.isEmpty() || entry.name.lowercase().contains(normalized) || entry.toolLabel.lowercase().contains(normalized))
    }
    val grouped = visible.groupBy { dayBucket(it.timestamp) }
    val totalBytes = entries.sumOf { it.sizeBytes }

    fun removeWithUndo(entry: HistoryEntry) {
        val index = entries.indexOfFirst { it.id == entry.id }
        HistoryRepository.remove(entry.id)
        undo = entry to index
        haptics(Haptic.REJECT)
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize()
                .testTag("history_screen"),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "header") {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Cronologia", style = MaterialTheme.typography.displayLarge, color = OmniText, modifier = Modifier.enterAnimation(0))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                if (entries.isEmpty()) "I file che hai convertito di recente." else "${entries.size} file · ${formatBytes(totalBytes)}",
                                fontSize = 16.sp,
                                color = OmniTextMuted,
                                modifier = Modifier.enterAnimation(1)
                            )
                        }
                        if (entries.isNotEmpty()) {
                            IconButton(onClick = {
                                scope.launch {
                                    val removed = withContext(Dispatchers.IO) { HistoryRepository.pruneMissing(context) }
                                    Toast.makeText(
                                        context,
                                        if (removed == 0) "Tutti i file sono ancora presenti" else "Rimossi $removed file non più presenti",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }) {
                                Icon(Icons.Default.CleaningServices, contentDescription = "Rimuovi i file eliminati", tint = OmniTextMuted)
                            }
                        }
                    }
                    if (entries.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        HistorySearchField(query = query, onQueryChange = { query = it }, modifier = Modifier.enterAnimation(2))
                        Spacer(modifier = Modifier.height(12.dp))
                        MorphChipRow(options = filters, selected = filter, onSelected = { filter = it }, modifier = Modifier.enterAnimation(3))
                    }
                }
            }

            item(key = "undo") {
                RevealVertically(visible = undo != null) {
                    OmniToast(
                        message = "\"${undo?.first?.name.orEmpty()}\" rimosso dalla cronologia",
                        actionLabel = "Annulla",
                        onAction = {
                            undo?.let { (entry, index) -> HistoryRepository.restore(entry, index) }
                            undo = null
                        },
                        onDismiss = { undo = null },
                        autoDismissMillis = 5000
                    )
                }
            }

            if (visible.isEmpty()) {
                item(key = "empty") {
                    if (entries.isEmpty()) {
                        EmptyState(
                            icon = Icons.Default.History,
                            title = "Ancora nessun file",
                            message = "Le conversioni completate compariranno qui."
                        )
                    } else {
                        EmptyState(
                            icon = Icons.Default.Search,
                            title = "Nessun risultato",
                            message = "Prova a cambiare filtro o ricerca."
                        )
                    }
                }
            }

            DayBucket.entries.forEach { bucket ->
                val bucketEntries = grouped[bucket].orEmpty()
                if (bucketEntries.isNotEmpty()) {
                    item(key = "header_${bucket.name}") {
                        SectionLabel(bucket.label, modifier = Modifier.animateItem().padding(top = 12.dp, bottom = 2.dp))
                    }
                    items(bucketEntries, key = { it.id }) { entry ->
                        SwipeableHistoryRow(
                            entry = entry,
                            onOpen = { openFile(context, Uri.parse(entry.uri), mimeForKind(entry.kind)) },
                            onShare = { shareFile(context, Uri.parse(entry.uri), mimeForKind(entry.kind)) },
                            onDelete = { pendingDelete = entry },
                            onSwiped = { removeWithUndo(entry) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }

            if (entries.isNotEmpty()) {
                item(key = "footer") {
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Scorri un file verso sinistra per toglierlo dalla cronologia", fontSize = 12.sp, color = OmniTextMuted)
                        TextButton(onClick = { confirmClear = true }) {
                            Text("Svuota cronologia", color = OmniDanger, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor = OmniSurface,
            shape = RoundedCornerShape(RadiusCard),
            icon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = OmniDanger) },
            title = { Text("Eliminare \"${entry.name}\"?", color = OmniText) },
            text = {
                Text(
                    "Puoi toglierlo solo dalla cronologia oppure eliminare anche il file salvato in Download.",
                    color = OmniTextMuted
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val deleted = HistoryRepository.deleteWithFile(context, entry.id)
                    Toast.makeText(
                        context,
                        if (deleted) "File eliminato" else "Rimosso dalla cronologia (file non trovato)",
                        Toast.LENGTH_SHORT
                    ).show()
                    pendingDelete = null
                }) {
                    Text("Elimina file", color = OmniDanger)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    removeWithUndo(entry)
                    pendingDelete = null
                }) {
                    Text("Solo dalla cronologia", color = OmniAccent)
                }
            }
        )
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            containerColor = OmniSurface,
            shape = RoundedCornerShape(RadiusCard),
            title = { Text("Svuotare la cronologia?", color = OmniText) },
            text = { Text("I file restano in Download: viene cancellato solo l'elenco.", color = OmniTextMuted) },
            confirmButton = {
                TextButton(onClick = {
                    HistoryRepository.clear()
                    confirmClear = false
                }) { Text("Svuota", color = OmniDanger) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Annulla", color = OmniTextMuted) }
            }
        )
    }
}

@Composable
private fun SwipeableHistoryRow(
    entry: HistoryEntry,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onSwiped: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onSwiped()
                true
            } else false
        },
        positionalThreshold = { distance -> distance * 0.4f }
    )
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        modifier = modifier,
        backgroundContent = {
            val active = state.targetValue == SwipeToDismissBoxValue.EndToStart
            val color by animateColorAsState(if (active) OmniDanger else OmniDanger.copy(alpha = 0.25f), label = "swipe_bg")
            val iconScale by animateFloatAsState(if (active) 1.2f else 0.9f, label = "swipe_icon")
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(RadiusMd + 2.dp))
                    .background(color)
                    .padding(horizontal = 22.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    }
                )
            }
        }
    ) {
        HistoryRow(entry = entry, onOpen = onOpen, onShare = onShare, onDelete = onDelete)
    }
}

@Composable
private fun HistorySearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(RadiusPill + 4.dp),
        color = OmniSurface,
        border = BorderStroke(1.dp, OmniBorder),
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
    ) {
        Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Search, contentDescription = null, tint = OmniTextMuted, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (query.isEmpty()) Text("Cerca nella cronologia", color = OmniTextMuted, fontSize = 15.sp)
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(color = OmniText, fontSize = 15.sp),
                    cursorBrush = SolidColor(OmniAccent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("history_search_field")
                )
            }
            AnimatedVisibility(visible = query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Cancella ricerca", tint = OmniTextMuted, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

private enum class DayBucket(val label: String) {
    TODAY("Oggi"),
    YESTERDAY("Ieri"),
    THIS_WEEK("Questa settimana"),
    OLDER("Meno recenti")
}

private fun dayBucket(timestamp: Long): DayBucket {
    val startOfToday = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val day = 24L * 60 * 60 * 1000
    return when {
        timestamp >= startOfToday -> DayBucket.TODAY
        timestamp >= startOfToday - day -> DayBucket.YESTERDAY
        timestamp >= startOfToday - 6 * day -> DayBucket.THIS_WEEK
        else -> DayBucket.OLDER
    }
}
