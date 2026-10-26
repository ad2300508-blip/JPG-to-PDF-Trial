package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PdfDocumentItem
import com.example.ui.theme.BorderCrisp
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CardSurface
import com.example.ui.theme.PdfGreen
import com.example.ui.theme.PdfOrange
import com.example.ui.theme.PdfRed
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate800
import com.example.ui.theme.SurfaceElevated

@Composable
fun PdfDocumentCard(
    item: PdfDocumentItem,
    index: Int,
    totalCount: Int,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    onPageRangeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OmniCard(
        borderColor = if (item.isCorrupted) PdfRed.copy(alpha = 0.5f) else BorderCrisp,
        backgroundColor = CardSurface,
        modifier = modifier.testTag("pdf_card_${item.id}")
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thumbnail or Fallback Icon
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Slate800),
                    contentAlignment = Alignment.Center
                ) {
                    if (item.thumbnailBitmap != null) {
                        Image(
                            bitmap = item.thumbnailBitmap.asImageBitmap(),
                            contentDescription = item.name,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = if (item.isCorrupted) Icons.Default.ErrorOutline else Icons.Default.Description,
                            contentDescription = null,
                            tint = if (item.isCorrupted) PdfRed else PdfGreen,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name.ifBlank { item.fileName },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OmniBadge(text = item.displayPageCount, color = PdfGreen)
                        OmniBadge(text = item.formattedSize, color = Slate400)
                        if (item.isEncrypted) {
                            OmniBadge(text = "PROTETTO", color = PdfOrange)
                        }
                    }
                }

                // Reorder and Delete controls
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onMoveUp,
                        enabled = index > 0,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Sposta su", tint = if (index > 0) Slate400 else Slate800)
                    }
                    IconButton(
                        onClick = onMoveDown,
                        enabled = index < totalCount - 1,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Sposta giù", tint = if (index < totalCount - 1) Slate400 else Slate800)
                    }
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Rimuovi", tint = PdfRed)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Page range input with strictly explicit lambda parameter
            OutlinedTextField(
                value = item.pageRange,
                onValueChange = { newValue: String ->
                    onPageRangeChange(newValue)
                },
                label = { Text("Intervallo pagine (es. 1-5, 8, 11-15)") },
                placeholder = { Text("Tutte le pagine") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("page_range_${item.id}")
            )
        }
    }
}
