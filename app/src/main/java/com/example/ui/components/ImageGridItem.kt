package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.model.ImageItem
import com.example.ui.theme.PdfRed

@Composable
fun ImageGridItem(
  item: ImageItem,
  index: Int,
  totalCount: Int,
  isSelected: Boolean,
  onToggleSelect: () -> Unit,
  onRotateClockwise: () -> Unit,
  onRotateCounterClockwise: () -> Unit,
  onMoveUp: () -> Unit,
  onMoveDown: () -> Unit,
  onRemove: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val borderColor = if (isSelected) {
    MaterialTheme.colorScheme.primary
  } else {
    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
  }

  val containerColor = if (isSelected) {
    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
  } else {
    MaterialTheme.colorScheme.surface
  }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(18.dp))
      .clickable(onClick = onToggleSelect)
      .testTag("image_item_$index"),
    shape = RoundedCornerShape(18.dp),
    border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
    colors = CardDefaults.cardColors(containerColor = containerColor),
    elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(10.dp)
    ) {
      // Thumbnail Box with Overlays
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .aspectRatio(1.25f)
          .clip(RoundedCornerShape(14.dp))
          .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
      ) {
        // Thumbnail with Live Rotation
        SubcomposeAsyncImage(
          model = ImageRequest.Builder(LocalContext.current)
            .data(item.uri)
            .crossfade(true)
            .build(),
          contentDescription = item.fileName,
          contentScale = ContentScale.Fit,
          modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
              rotationZ = item.rotationDegrees.toFloat()
            },
          loading = {
            Box(
              modifier = Modifier.fillMaxSize(),
              contentAlignment = Alignment.Center
            ) {
              CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
              )
            }
          }
        )

        // Top-Left: Page Index Badge + Multi-select Indicator
        Row(
          modifier = Modifier
            .align(Alignment.TopStart)
            .padding(6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
          ) {
            Text(
              text = "#${index + 1}",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
            )
          }

          if (isSelected) {
            Surface(
              shape = CircleShape,
              color = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.size(20.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.Check,
                  contentDescription = "Selected",
                  modifier = Modifier.size(14.dp)
                )
              }
            }
          }
        }

        // Top-Right: Sharp Action Overlay (Rotate CW/CCW, Delete)
        Row(
          modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(6.dp),
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          FilledIconButton(
            onClick = onRotateCounterClockwise,
            colors = IconButtonDefaults.filledIconButtonColors(
              containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
              contentColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier
              .size(28.dp)
              .testTag("rotate_ccw_$index")
          ) {
            Icon(
              imageVector = Icons.Default.RotateLeft,
              contentDescription = "Rotate CCW",
              modifier = Modifier.size(15.dp)
            )
          }

          FilledIconButton(
            onClick = onRotateClockwise,
            colors = IconButtonDefaults.filledIconButtonColors(
              containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
              contentColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier
              .size(28.dp)
              .testTag("rotate_cw_$index")
          ) {
            Icon(
              imageVector = Icons.Default.RotateRight,
              contentDescription = "Rotate CW",
              modifier = Modifier.size(15.dp)
            )
          }

          FilledIconButton(
            onClick = onRemove,
            colors = IconButtonDefaults.filledIconButtonColors(
              containerColor = PdfRed.copy(alpha = 0.15f),
              contentColor = PdfRed
            ),
            modifier = Modifier
              .size(28.dp)
              .testTag("delete_image_$index")
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Remove page",
              modifier = Modifier.size(15.dp)
            )
          }
        }

        // Center-Bottom subtle drag handle indicator
        Box(
          modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 4.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
          Icon(
            imageVector = Icons.Default.DragHandle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // File Name Title
      Text(
        text = item.fileName,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        color = MaterialTheme.colorScheme.onSurface
      )

      Spacer(modifier = Modifier.height(6.dp))

      // Crisp Pill Chips: Dimensions & Size & Rotation
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        SubtlePillChip(text = item.dimensionsText)
        SubtlePillChip(text = item.formattedSize, isAccent = true)

        if (item.rotationDegrees != 0) {
          SubtlePillChip(text = "${item.rotationDegrees}°", isRotation = true)
        }

        Spacer(modifier = Modifier.weight(1f))

        // Reordering Arrows
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          FilledIconButton(
            onClick = onMoveUp,
            enabled = index > 0,
            colors = IconButtonDefaults.filledIconButtonColors(
              containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
              contentColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier
              .size(28.dp)
              .testTag("move_up_$index")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Move earlier",
              modifier = Modifier.size(14.dp)
            )
          }

          FilledIconButton(
            onClick = onMoveDown,
            enabled = index < totalCount - 1,
            colors = IconButtonDefaults.filledIconButtonColors(
              containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
              contentColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier
              .size(28.dp)
              .testTag("move_down_$index")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowForward,
              contentDescription = "Move later",
              modifier = Modifier.size(14.dp)
            )
          }
        }
      }
    }
  }
}

@Composable
private fun SubtlePillChip(
  text: String,
  isAccent: Boolean = false,
  isRotation: Boolean = false,
) {
  val bg = when {
    isRotation -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
    isAccent -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
  }

  val textColor = when {
    isRotation -> MaterialTheme.colorScheme.onTertiaryContainer
    isAccent -> MaterialTheme.colorScheme.primary
    else -> MaterialTheme.colorScheme.onSurfaceVariant
  }

  Surface(
    shape = RoundedCornerShape(8.dp),
    color = bg,
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.Medium,
      color = textColor,
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
    )
  }
}
