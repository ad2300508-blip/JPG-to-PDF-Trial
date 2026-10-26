package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.theme.Haptic
import com.example.ui.theme.Motion
import com.example.ui.theme.OmniAccent
import com.example.ui.theme.OmniAccentSoft
import com.example.ui.theme.OmniBg
import com.example.ui.theme.OmniBorder
import com.example.ui.theme.OmniOnAccent
import com.example.ui.theme.OmniSurface
import com.example.ui.theme.OmniSurface2
import com.example.ui.theme.OmniText
import com.example.ui.theme.OmniTextMuted
import com.example.ui.theme.RadiusMd
import com.example.ui.theme.motionSpec
import com.example.ui.theme.rememberHaptics
import com.example.util.GalleryImage
import com.example.util.MediaGallery

private const val ALL_ALBUMS = "Tutte"

/**
 * Returns a launcher for the in-app photo picker: the user taps photos, sees 1, 2, 3… on them and
 * gets the URIs back exactly in that order (system pickers do not guarantee it).
 * When gallery access is denied it falls back to [fallback] (usually the system photo picker).
 */
@Composable
fun rememberOrderedImagePicker(onPicked: (List<Uri>) -> Unit, fallback: () -> Unit): () -> Unit {
    val context = LocalContext.current
    var showPicker by rememberSaveable { mutableStateOf(false) }
    var galleryVersion by remember { mutableIntStateOf(0) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        galleryVersion++
        if (result.values.any { it } || MediaGallery.hasAccess(context)) showPicker = true else fallback()
    }
    if (showPicker) {
        OrderedImagePickerDialog(
            version = galleryVersion,
            onDismiss = { showPicker = false },
            onConfirm = { uris ->
                showPicker = false
                if (uris.isNotEmpty()) onPicked(uris)
            },
            onRequestMore = { permissionLauncher.launch(MediaGallery.permissions()) },
            onUseSystemPicker = {
                showPicker = false
                fallback()
            }
        )
    }
    return {
        if (MediaGallery.hasAccess(context)) showPicker = true else permissionLauncher.launch(MediaGallery.permissions())
    }
}

@Composable
private fun OrderedImagePickerDialog(
    version: Int,
    onDismiss: () -> Unit,
    onConfirm: (List<Uri>) -> Unit,
    onRequestMore: () -> Unit,
    onUseSystemPicker: () -> Unit
) {
    val context = LocalContext.current
    val haptics = rememberHaptics()
    var images by remember { mutableStateOf<List<GalleryImage>?>(null) }
    val selected = remember { mutableStateListOf<Uri>() }
    var album by rememberSaveable { mutableStateOf(ALL_ALBUMS) }
    val partial = remember(version) { MediaGallery.isPartialAccess(context) }

    LaunchedEffect(version) { images = MediaGallery.load(context) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = OmniBg) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 16.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Chiudi", tint = OmniText) }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Scegli le pagine", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = OmniText)
                        AnimatedText(
                            text = if (selected.isEmpty()) "Tocca le foto nell'ordine delle pagine: 1, 2, 3…" else "${selected.size} selezionate · tocca di nuovo per togliere",
                            fontSize = 13.sp,
                            color = OmniTextMuted
                        )
                    }
                    AnimatedVisibility(visible = selected.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
                        TextButton(onClick = { selected.clear() }) { Text("Azzera", color = OmniAccent) }
                    }
                }

                val list = images
                val albums = remember(list) {
                    listOf(ALL_ALBUMS) + list.orEmpty().groupingBy { it.album }.eachCount().entries
                        .sortedByDescending { it.value }.take(12).map { it.key }
                }
                if (albums.size > 2) {
                    MorphChipRow(
                        options = albums,
                        selected = album,
                        onSelected = { album = it },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
                if (partial) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(RadiusMd))
                            .background(OmniAccentSoft)
                            .padding(start = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Vedi solo le foto che hai consentito", fontSize = 13.sp, color = OmniText, modifier = Modifier.weight(1f))
                        TextButton(onClick = onRequestMore) { Text("Aggiungi", color = OmniAccent, fontWeight = FontWeight.SemiBold) }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    when {
                        list == null -> CircularProgressIndicator(color = OmniAccent, modifier = Modifier.align(Alignment.Center))
                        list.isEmpty() -> EmptyState(
                            icon = Icons.Default.PhotoLibrary,
                            title = "Nessuna foto trovata",
                            message = "Prova con il selettore di Android o scatta una foto.",
                            modifier = Modifier.align(Alignment.Center)
                        )
                        else -> {
                            val visible = if (album == ALL_ALBUMS) list else list.filter { it.album == album }
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 104.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(visible, key = { it.uri.toString() }) { image ->
                                    val order = selected.indexOf(image.uri)
                                    GalleryCell(
                                        image = image,
                                        order = if (order >= 0) order + 1 else null,
                                        onClick = {
                                            if (order >= 0) {
                                                selected.removeAt(order)
                                                haptics(Haptic.TICK)
                                            } else {
                                                selected.add(image.uri)
                                                haptics(Haptic.TAP)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Surface(color = OmniSurface, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onUseSystemPicker) {
                            Text("Selettore di Android", color = OmniTextMuted, fontSize = 13.sp, textAlign = TextAlign.Start)
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Button(
                            onClick = {
                                haptics(Haptic.CONFIRM)
                                onConfirm(selected.toList())
                            },
                            enabled = selected.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = OmniAccent,
                                contentColor = OmniOnAccent,
                                disabledContainerColor = OmniSurface2,
                                disabledContentColor = OmniTextMuted
                            ),
                            modifier = Modifier.height(48.dp)
                        ) {
                            AnimatedText(
                                text = when (selected.size) {
                                    0 -> "Aggiungi"
                                    1 -> "Aggiungi 1 pagina"
                                    else -> "Aggiungi ${selected.size} pagine"
                                },
                                fontWeight = FontWeight.SemiBold,
                                color = if (selected.isEmpty()) OmniTextMuted else OmniOnAccent
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GalleryCell(image: GalleryImage, order: Int?, onClick: () -> Unit) {
    val context = LocalContext.current
    val isSelected = order != null
    val inset by animateDpAsState(if (isSelected) 6.dp else 0.dp, motionSpec(Motion.snappy()), label = "cell_inset")
    val border by animateColorAsState(if (isSelected) OmniAccent else OmniBorder.copy(alpha = 0f), label = "cell_border")
    val dim by animateFloatAsState(if (isSelected) 0.9f else 1f, motionSpec(Motion.snappy()), label = "cell_scale")
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(OmniSurface2)
            .border(3.dp, border, RoundedCornerShape(10.dp))
            .clickable(onClickLabel = if (isSelected) "Togli la pagina $order" else "Aggiungi come pagina", onClick = onClick)
    ) {
        AsyncImage(
            model = remember(image.uri) {
                ImageRequest.Builder(context).data(image.uri).size(320).crossfade(true).build()
            },
            contentDescription = image.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .padding(inset)
                .clip(RoundedCornerShape(8.dp))
                .scale(dim)
        )
        AnimatedVisibility(
            visible = isSelected,
            enter = scaleIn(motionSpec(Motion.bouncy())) + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(OmniAccent)
                    .border(2.dp, OmniOnAccent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("${order ?: ""}", color = OmniOnAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
