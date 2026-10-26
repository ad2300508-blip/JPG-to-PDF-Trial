package com.example.ui

import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HistoryEntry
import com.example.data.HistoryKind
import com.example.data.HistoryRepository
import com.example.ui.components.HistoryRow
import com.example.ui.components.IconTile
import com.example.ui.components.MorphChipRow
import com.example.ui.components.SectionLabel
import com.example.ui.components.mimeForKind
import com.example.ui.components.openFile
import com.example.ui.components.shareFile
import com.example.ui.theme.OmniAccent
import com.example.ui.theme.OmniDanger
import com.example.ui.theme.OmniSurface
import com.example.ui.theme.OmniText
import com.example.ui.theme.OmniTextMuted
import com.example.ui.theme.RadiusCard
import java.util.Calendar

private const val ALL = "Tutti"

@Composable
fun HistoryScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val entries by HistoryRepository.entries.collectAsState()
    var filter by rememberSaveable { mutableStateOf(ALL) }
    var pendingDelete by remember { mutableStateOf<HistoryEntry?>(null) }

    val filters = listOf(ALL) + HistoryKind.entries.map { it.label }
    val visible = entries.filter { filter == ALL || it.kind.label == filter }
    val grouped = visible.groupBy { dayBucket(it.timestamp) }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize()
                .testTag("history_screen"),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Column {
                    Text("Cronologia", style = MaterialTheme.typography.displayLarge, color = OmniText)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("I file che hai convertito di recente.", fontSize = 16.sp, color = OmniTextMuted)
                    Spacer(modifier = Modifier.height(18.dp))
                    MorphChipRow(options = filters, selected = filter, onSelected = { filter = it })
                }
            }

            if (visible.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 64.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        IconTile(icon = Icons.Default.History, tint = OmniAccent, size = 64.dp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Ancora nessun file", fontSize = 18.sp, color = OmniText, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Le conversioni completate compariranno qui.",
                            fontSize = 14.sp,
                            color = OmniTextMuted
                        )
                    }
                }
            }

            DayBucket.entries.forEach { bucket ->
                val bucketEntries = grouped[bucket].orEmpty()
                if (bucketEntries.isNotEmpty()) {
                    item(key = "header_${bucket.name}") {
                        SectionLabel(bucket.label, modifier = Modifier.padding(top = 12.dp, bottom = 2.dp))
                    }
                    items(bucketEntries, key = { it.id }) { entry ->
                        HistoryRow(
                            entry = entry,
                            onOpen = { openFile(context, Uri.parse(entry.uri), mimeForKind(entry.kind)) },
                            onShare = { shareFile(context, Uri.parse(entry.uri), mimeForKind(entry.kind)) },
                            onDelete = { pendingDelete = entry }
                        )
                    }
                }
            }

            if (entries.isNotEmpty()) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        TextButton(onClick = { HistoryRepository.clear() }) {
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
                    HistoryRepository.remove(entry.id)
                    pendingDelete = null
                }) {
                    Text("Solo dalla cronologia", color = OmniAccent)
                }
            }
        )
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
