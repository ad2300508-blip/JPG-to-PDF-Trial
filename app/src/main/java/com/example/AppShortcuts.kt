package com.example

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat

/** Long-press shortcuts on the launcher icon: open a tool directly. */
object AppShortcuts {
    const val ACTION_OPEN_TOOL = "com.example.action.OPEN_TOOL"
    const val EXTRA_TOOL = "tool"

    /** (OmniTool name, short label, long label). */
    private val entries = listOf(
        Triple("IMAGES_TO_PDF", "Foto in PDF", "Immagini in PDF"),
        Triple("PDF_READER", "Leggi PDF", "Leggi un PDF"),
        Triple("RECORDER", "Registra", "Registra audio"),
        Triple("MERGE_PDF", "Unisci PDF", "Unisci più PDF")
    )

    fun publish(context: Context) {
        runCatching {
            val shortcuts = entries.mapIndexed { rank, (tool, short, long) ->
                val intent = Intent(context, MainActivity::class.java)
                    .setAction(ACTION_OPEN_TOOL)
                    .putExtra(EXTRA_TOOL, tool)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                ShortcutInfoCompat.Builder(context, "tool_$tool")
                    .setShortLabel(short)
                    .setLongLabel(long)
                    .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
                    .setIntent(intent)
                    .setRank(rank)
                    .build()
            }
            ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
        }
    }
}
