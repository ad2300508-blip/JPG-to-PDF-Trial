package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalReduceMotion
import com.example.ui.theme.Motion
import com.example.ui.theme.OmniAccent
import com.example.ui.theme.OmniAccentSoft
import com.example.ui.theme.OmniOnAccent
import com.example.ui.theme.OmniSurface
import com.example.ui.theme.OmniText
import com.example.ui.theme.OmniTextMuted
import com.example.ui.theme.RadiusMd
import com.example.ui.theme.enterAnimation

private data class WhatsNewItem(val icon: ImageVector, val title: String, val text: String)

private val highlights = listOf(
    WhatsNewItem(Icons.Default.Speed, "Più fluida", "App ottimizzata, avvii più rapidi e animazioni che seguono le impostazioni del telefono."),
    WhatsNewItem(Icons.Default.GridView, "Strumenti in ordine", "Sei sezioni, filtri con contatori, preferiti in cima e scheda rapida con la pressione lunga."),
    WhatsNewItem(Icons.AutoMirrored.Filled.MenuBook, "Leggi PDF e Info PDF", "Lettore con zoom e modalità notte; parole, tempo di lettura e metadati."),
    WhatsNewItem(Icons.Default.Dashboard, "Nuovi strumenti per le pagine", "Pagine bianche, più pagine per foglio, spazio per appunti, fronte/retro e ritaglio."),
    WhatsNewItem(Icons.Default.AspectRatio, "Foto per i social", "Formato social con cornice, filigrana, ruota e specchia, palette colori."),
    WhatsNewItem(Icons.Default.Mic, "Registra audio", "Registra le lezioni e trascrivile con Gemini."),
    WhatsNewItem(Icons.Default.DarkMode, "Nero puro e colore dinamico", "Tema per schermi OLED e colore preso dallo sfondo del telefono.")
)

/** Shown once after an update: what changed, each line sliding in after the previous one. */
@Composable
fun WhatsNewDialog(versionName: String, onDismiss: () -> Unit) {
    val reduce = LocalReduceMotion.current
    val pop = remember { Animatable(if (reduce) 1f else 0.4f) }
    LaunchedEffect(Unit) { if (!reduce) pop.animateTo(1f, Motion.bouncy()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = OmniSurface,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .graphicsLayer {
                        scaleX = pop.value
                        scaleY = pop.value
                    }
                    .clip(CircleShape)
                    .background(OmniAccentSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Celebration, contentDescription = null, tint = OmniAccent, modifier = Modifier.size(30.dp))
            }
        },
        title = { Text("Novità di Morph $versionName", color = OmniText, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                highlights.forEachIndexed { index, item ->
                    Row(verticalAlignment = Alignment.Top, modifier = Modifier.enterAnimation(index + 1, key = "whats_new_$index")) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(OmniAccentSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(item.icon, contentDescription = null, tint = OmniAccent, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = OmniText)
                            Text(item.text, fontSize = 13.sp, color = OmniTextMuted)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = OmniAccent, contentColor = OmniOnAccent),
                shape = RoundedCornerShape(RadiusMd),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Iniziamo", fontWeight = FontWeight.Bold) }
        }
    )
}
