package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.TextFields
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
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.model.ImageItem
import com.example.ui.theme.PdfGreen
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
  onViewExtractedText: () -> Unit,
  onToggleEnhance: () -> Unit,
  onCompareBeforeAfter: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val borderColor = if (isSelected) {
    MaterialTheme.colorScheme.primary
  } else if (item.isAiEnhanced) {
    PdfGreen.copy(alpha = 0.6f)
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
        // Thumbnail with Live Rotation and activeUri (supports enhanced cropped bitmap)
        SubcomposeAsyncImage(
          model = ImageRequest.Builder(LocalContext.current)
            .data(item.activeUri)
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

        // Top-Left: Page Index Badge + AI Cleaned Badge + Multi-select Indicator
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

          if (item.detectedPageBadge != null) {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = MaterialTheme.colorScheme.tertiaryContainer,
              contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
              modifier = Modifier.testTag("page_number_badge_$index")
            ) {
              Text(
                text = item.detectedPageBadge,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
              )
            }
          }

          if (item.isAiEnhanced) {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = PdfGreen,
              contentColor = Color.White,
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onCompareBeforeAfter)
                .testTag("ai_cleaned_badge_$index")
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.AutoAwesome,
                  contentDescription = null,
                  modifier = Modifier.size(11.dp)
                )
                Text(
                  text = "AI Cleaned",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold
                )
              }
            }
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

        // Top-Right: Sharp Action Overlay (Magic Wand AI Enhance, Rotate CW/CCW, Delete)
        Row(
          modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(6.dp),
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          // Magic Wand Auto-Crop & Enhance Button
          FilledIconButton(
            onClick = onToggleEnhance,
            colors = IconButtonDefaults.filledIconButtonColors(
              containerColor = if (item.isAiEnhanced) PdfGreen else MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
              contentColor = if (item.isAiEnhanced) Color.White else MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
              .size(28.dp)
              .testTag("magic_wand_enhance_$index")
          ) {
            if (item.isEnhancing) {
              CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
              )
            } else {
              Icon(
                imageVector = Icons.Default.AutoFixHigh,
                contentDescription = "Magic Wand (Auto-Crop & Enhance)",
                modifier = Modifier.size(15.dp)
              )
            }
          }

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
            .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
          Icon(
            imageVector = Icons.Default.DragHandle,
            contentDescription = "Drag to reorder",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(14.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // File Details: Title + Move Buttons
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = item.fileName,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            color = MaterialTheme.colorScheme.onSurface
          )

          Spacer(modifier = Modifier.height(3.dp))

          // Subtle Pill Chips (File Size, Dimensions, Rotation, Compare Action)
          Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            SubtlePillChip(text = item.formattedSize)
            SubtlePillChip(text = item.dimensionsText)
            if (item.rotationDegrees != 0) {
              SubtlePillChip(text = "${item.rotationDegrees}°", isRotation = true)
            }
          }
        }

        // Reorder Up / Down & Compare Buttons
        Row(
          horizontalArrangement = Arrangement.spacedBy(2.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Compare Before/After Button
          FilledIconButton(
            onClick = onCompareBeforeAfter,
            colors = IconButtonDefaults.filledIconButtonColors(
              containerColor = if (item.isAiEnhanced) PdfGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
              contentColor = if (item.isAiEnhanced) PdfGreen else MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier
              .size(28.dp)
              .testTag("compare_before_after_$index")
          ) {
            Icon(
              imageVector = Icons.Default.CompareArrows,
              contentDescription = "Compare original vs enhanced",
              modifier = Modifier.size(15.dp)
            )
          }

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

      Spacer(modifier = Modifier.height(6.dp))

      // OCR Text Status & Action Chip Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        if (item.isOcrScanning) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
            modifier = Modifier.testTag("ocr_scanning_$index")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              CircularProgressIndicator(
                modifier = Modifier.size(11.dp),
                strokeWidth = 1.8.dp,
                color = MaterialTheme.colorScheme.onSecondaryContainer
              )
              Spacer(modifier = Modifier.width(5.dp))
              Text(
                text = "Scanning OCR...",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
              )
            }
          }
        } else if (item.hasOcrResult) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .clickable(onClick = onViewExtractedText)
              .testTag("view_ocr_button_$index")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.TextFields,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(13.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Extracted Text (${item.ocrWordCount} words)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
              )
            }
          }
        } else {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .clickable(onClick = onViewExtractedText)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.TextFields,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(13.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "No text found (Tap to check)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
              )
            }
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
