package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pages
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.PdfDocumentItem

@Composable
fun PdfDocumentCard(
  item: PdfDocumentItem,
  index: Int,
  totalCount: Int,
  onMoveUp: () -> Unit,
  onMoveDown: () -> Unit,
  onRemove: () -> Unit,
  onUpdatePageRange: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  var showPageRangeDialog by remember { mutableStateOf(false) }
  var tempRange by remember(item.pageRange) { mutableStateOf(item.pageRange) }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("pdf_item_card_${item.id}"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // Thumbnail Preview or Document Icon
      Box(
        modifier = Modifier
          .width(68.dp)
          .height(90.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(MaterialTheme.colorScheme.surface)
          .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
      ) {
        if (item.thumbnailBitmap != null) {
          Image(
            bitmap = item.thumbnailBitmap.asImageBitmap(),
            contentDescription = "Preview of ${item.fileName}",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
          )
        } else {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
          ) {
            Icon(
              imageVector = if (item.isEncrypted) Icons.Default.Lock else Icons.Default.Description,
              contentDescription = null,
              tint = if (item.isEncrypted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "PDF",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        // Sequential index pill badge
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary,
          modifier = Modifier
            .align(Alignment.TopStart)
            .padding(4.dp)
        ) {
          Text(
            text = "${index + 1}",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
          )
        }
      }

      // Metadata & Details
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Text(
          text = item.fileName,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
          ) {
            Text(
              text = item.displayPageCount,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
              fontWeight = FontWeight.Medium,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }

          Text(
            text = item.formattedSize,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        // Custom Page Range Chip
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable { showPageRangeDialog = true }
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Pages,
            contentDescription = null,
            modifier = Modifier.size(13.dp),
            tint = MaterialTheme.colorScheme.primary
          )
          Text(
            text = if (item.pageRange.isBlank() || item.pageRange.lowercase() == "all") "All Pages" else "Pages: ${item.pageRange}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
          )
          Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = "Edit page range",
            modifier = Modifier.size(11.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        if (item.errorMessage != null) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Warning,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(14.dp)
            )
            Text(
              text = item.errorMessage,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.error
            )
          }
        }
      }

      // Reordering & Actions
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
      ) {
        IconButton(
          onClick = onMoveUp,
          enabled = index > 0,
          modifier = Modifier
            .size(36.dp)
            .testTag("move_up_$index")
        ) {
          Icon(
            imageVector = Icons.Default.ArrowUpward,
            contentDescription = "Move Up",
            modifier = Modifier.size(18.dp)
          )
        }

        IconButton(
          onClick = onMoveDown,
          enabled = index < totalCount - 1,
          modifier = Modifier
            .size(36.dp)
            .testTag("move_down_$index")
        ) {
          Icon(
            imageVector = Icons.Default.ArrowDownward,
            contentDescription = "Move Down",
            modifier = Modifier.size(18.dp)
          )
        }

        IconButton(
          onClick = onRemove,
          modifier = Modifier
            .size(36.dp)
            .testTag("remove_pdf_$index")
        ) {
          Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "Remove Document",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(18.dp)
          )
        }
      }
    }
  }

  // Page Range Filter Dialog
  if (showPageRangeDialog) {
    AlertDialog(
      onDismissRequest = { showPageRangeDialog = false },
      title = { Text("Select Pages to Merge") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "Enter specific pages or page ranges from \"${item.fileName}\" (Total: ${item.pageCount} pages).\nLeave blank or write \"all\" to merge all pages.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          OutlinedTextField(
            value = tempRange,
            onValueChange = { tempRange = it },
            label = { Text("Page Range (e.g., 1-5, 8, 10-12)") },
            placeholder = { Text("all") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        TextButton(
          onClick = {
            onUpdatePageRange(tempRange.trim())
            showPageRangeDialog = false
          }
        ) {
          Text("Apply")
        }
      },
      dismissButton = {
        TextButton(onClick = { showPageRangeDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }
}
