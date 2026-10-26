package com.example.ui.components

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Canvas
import com.example.ui.theme.OmniAccentOnInk
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.CropRect
import com.example.ui.theme.OmniAccent
import com.example.ui.theme.OmniOnAccent
import com.example.ui.theme.RadiusMd
import com.example.ui.theme.RadiusPill
import com.example.util.BitmapLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.hypot

private enum class CropHandle { NONE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, MOVE }

private data class AspectPreset(val label: String, val ratio: Float?)

private val ASPECTS = listOf(
    AspectPreset("Libero", null),
    AspectPreset("A4", 1f / 1.4142f),
    AspectPreset("4:3", 3f / 4f),
    AspectPreset("1:1", 1f)
)

/**
 * Full-screen crop editor: drag the corners (or the whole frame) over the page image.
 * The crop is expressed in fractions of the image *after* the page rotation.
 */
@Composable
fun CropDialog(
    uri: Uri,
    rotationDegrees: Int,
    initial: CropRect?,
    onDismiss: () -> Unit,
    onConfirm: (CropRect?) -> Unit
) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null, uri, rotationDegrees) {
        value = withContext(Dispatchers.IO) {
            BitmapLoader.decode(context, uri, 1600)?.let { BitmapLoader.rotate(it, rotationDegrees) }
        }
    }
    var crop by remember { mutableStateOf(initial ?: CropRect()) }
    var aspect by remember { mutableStateOf<Float?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(color = Color(0xFF0B0D12), modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Annulla", tint = Color.White)
                    }
                    Text(
                        "Ritaglia pagina",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = {
                        crop = CropRect()
                        aspect = null
                    }) {
                        Text("Ripristina", color = Color.White.copy(alpha = 0.8f))
                    }
                }

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val bmp = bitmap
                    if (bmp == null) {
                        CircularProgressIndicator(color = Color.White)
                    } else {
                        val boxW = constraints.maxWidth.toFloat()
                        val boxH = constraints.maxHeight.toFloat()
                        val scale = minOf(boxW / bmp.width, boxH / bmp.height)
                        val imgW = bmp.width * scale
                        val imgH = bmp.height * scale
                        val left = (boxW - imgW) / 2f
                        val top = (boxH - imgH) / 2f
                        val imageAspect = bmp.width.toFloat() / bmp.height

                        Image(
                            bitmap = remember(bmp) { bmp.asImageBitmap() },
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )

                        val handleColor = OmniAccentOnInk
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(imgW, imgH) {
                                    var handle = CropHandle.NONE
                                    val threshold = 40.dp.toPx()
                                    val min = 0.08f
                                    detectDragGestures(
                                        onDragStart = { pos ->
                                            val l = left + crop.left * imgW
                                            val t = top + crop.top * imgH
                                            val r = left + crop.right * imgW
                                            val b = top + crop.bottom * imgH
                                            fun near(x: Float, y: Float) = hypot(pos.x - x, pos.y - y) < threshold
                                            handle = when {
                                                near(l, t) -> CropHandle.TOP_LEFT
                                                near(r, t) -> CropHandle.TOP_RIGHT
                                                near(l, b) -> CropHandle.BOTTOM_LEFT
                                                near(r, b) -> CropHandle.BOTTOM_RIGHT
                                                pos.x in l..r && pos.y in t..b -> CropHandle.MOVE
                                                else -> CropHandle.NONE
                                            }
                                        },
                                        onDragEnd = { handle = CropHandle.NONE },
                                        onDrag = { change, drag ->
                                            change.consume()
                                            val dx = drag.x / imgW
                                            val dy = drag.y / imgH
                                            val c = crop
                                            var next = when (handle) {
                                                CropHandle.TOP_LEFT -> c.copy(
                                                    left = (c.left + dx).coerceIn(0f, c.right - min),
                                                    top = (c.top + dy).coerceIn(0f, c.bottom - min)
                                                )
                                                CropHandle.TOP_RIGHT -> c.copy(
                                                    right = (c.right + dx).coerceIn(c.left + min, 1f),
                                                    top = (c.top + dy).coerceIn(0f, c.bottom - min)
                                                )
                                                CropHandle.BOTTOM_LEFT -> c.copy(
                                                    left = (c.left + dx).coerceIn(0f, c.right - min),
                                                    bottom = (c.bottom + dy).coerceIn(c.top + min, 1f)
                                                )
                                                CropHandle.BOTTOM_RIGHT -> c.copy(
                                                    right = (c.right + dx).coerceIn(c.left + min, 1f),
                                                    bottom = (c.bottom + dy).coerceIn(c.top + min, 1f)
                                                )
                                                CropHandle.MOVE -> {
                                                    val w = c.right - c.left
                                                    val h = c.bottom - c.top
                                                    val nl = (c.left + dx).coerceIn(0f, 1f - w)
                                                    val nt = (c.top + dy).coerceIn(0f, 1f - h)
                                                    CropRect(nl, nt, nl + w, nt + h)
                                                }
                                                CropHandle.NONE -> c
                                            }
                                            val ratio = aspect
                                            if (ratio != null && handle != CropHandle.MOVE && handle != CropHandle.NONE) {
                                                next = constrainAspect(next, ratio, imageAspect, handle)
                                            }
                                            crop = next
                                        }
                                    )
                                }
                        ) {
                            val l = left + crop.left * imgW
                            val t = top + crop.top * imgH
                            val r = left + crop.right * imgW
                            val b = top + crop.bottom * imgH
                            val dim = Color.Black.copy(alpha = 0.55f)
                            // Dim everything outside the frame.
                            drawRect(dim, Offset(left, top), Size(imgW, t - top))
                            drawRect(dim, Offset(left, b), Size(imgW, top + imgH - b))
                            drawRect(dim, Offset(left, t), Size(l - left, b - t))
                            drawRect(dim, Offset(r, t), Size(left + imgW - r, b - t))
                            // Frame and rule-of-thirds grid.
                            drawRect(Color.White, Offset(l, t), Size(r - l, b - t), style = Stroke(2.dp.toPx()))
                            val grid = Color.White.copy(alpha = 0.35f)
                            for (i in 1..2) {
                                val gx = l + (r - l) * i / 3f
                                val gy = t + (b - t) * i / 3f
                                drawLine(grid, Offset(gx, t), Offset(gx, b), 1.dp.toPx())
                                drawLine(grid, Offset(l, gy), Offset(r, gy), 1.dp.toPx())
                            }
                            val handleSize = 22.dp.toPx()
                            val stroke = 4.dp.toPx()
                            listOf(Offset(l, t), Offset(r, t), Offset(l, b), Offset(r, b)).forEachIndexed { index, corner ->
                                val sx = if (index % 2 == 0) 1f else -1f
                                val sy = if (index < 2) 1f else -1f
                                drawLine(handleColor, corner, Offset(corner.x + handleSize * sx, corner.y), stroke)
                                drawLine(handleColor, corner, Offset(corner.x, corner.y + handleSize * sy), stroke)
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    ASPECTS.forEach { preset ->
                        val selected = aspect == preset.ratio
                        Surface(
                            onClick = {
                                aspect = preset.ratio
                                val ratio = preset.ratio
                                val bmp = bitmap
                                if (ratio != null && bmp != null) {
                                    crop = constrainAspect(crop, ratio, bmp.width.toFloat() / bmp.height, CropHandle.BOTTOM_RIGHT)
                                }
                            },
                            shape = RoundedCornerShape(RadiusPill),
                            color = if (selected) Color.White else Color.White.copy(alpha = 0.12f)
                        ) {
                            Text(
                                preset.label,
                                color = if (selected) Color.Black else Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = { onConfirm(crop.takeIf { !it.isFullFrame }) },
                    colors = ButtonDefaults.buttonColors(containerColor = OmniAccent, contentColor = OmniOnAccent),
                    shape = RoundedCornerShape(RadiusMd),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(54.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Applica ritaglio", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}


/**
 * Forces the crop frame to [ratio] (width/height in real pixels), keeping the corner
 * opposite to the dragged [handle] fixed and staying inside the image.
 */
private fun constrainAspect(rect: CropRect, ratio: Float, imageAspect: Float, handle: CropHandle): CropRect {
    // In normalized units: wNorm * imageAspect / hNorm = ratio
    val width = rect.right - rect.left
    var height = width * imageAspect / ratio
    var w = width
    if (height > 1f) {
        height = 1f
        w = height * ratio / imageAspect
    }
    return when (handle) {
        CropHandle.TOP_LEFT -> {
            val r = rect.right
            val b = rect.bottom
            val l = (r - w).coerceAtLeast(0f)
            val t = (b - height).coerceAtLeast(0f)
            CropRect(l, t, l + w, t + height).clampInside()
        }
        CropHandle.TOP_RIGHT -> {
            val l = rect.left
            val b = rect.bottom
            val t = (b - height).coerceAtLeast(0f)
            CropRect(l, t, l + w, t + height).clampInside()
        }
        CropHandle.BOTTOM_LEFT -> {
            val r = rect.right
            val t = rect.top
            val l = (r - w).coerceAtLeast(0f)
            CropRect(l, t, l + w, t + height).clampInside()
        }
        else -> CropRect(rect.left, rect.top, rect.left + w, rect.top + height).clampInside()
    }
}

private fun CropRect.clampInside(): CropRect {
    val w = (right - left).coerceAtMost(1f)
    val h = (bottom - top).coerceAtMost(1f)
    val l = left.coerceIn(0f, 1f - w)
    val t = top.coerceIn(0f, 1f - h)
    return CropRect(l, t, l + w, t + h)
}
