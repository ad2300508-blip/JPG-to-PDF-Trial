package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.model.ImageItem

@Composable
fun ImageGridItem(
  item: ImageItem,
  index: Int,
  totalCount: Int,
  onMoveUp: () -> Unit,
  onMoveDown: () -> Unit,
  onRemove: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("image_item_$index"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(10.dp)
    ) {
      // Image Thumbnail with Page Badge
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .aspectRatio(1.2f)
          .clip(RoundedCornerShape(12.dp))
          .background(MaterialTheme.colorScheme.surface)
      ) {
        SubcomposeAsyncImage(
          model = ImageRequest.Builder(LocalContext.current)
            .data(item.uri)
            .crossfade(true)
            .build(),
          contentDescription = item.fileName,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
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

        // Page Number Badge
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary,
          modifier = Modifier
            .padding(8.dp)
            .align(Alignment.TopStart)
        ) {
          Text(
            text = "#${index + 1}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }

        // Delete Quick Action
        FilledIconButton(
          onClick = onRemove,
          colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            contentColor = MaterialTheme.colorScheme.error
          ),
          modifier = Modifier
            .size(34.dp)
            .padding(4.dp)
            .align(Alignment.TopEnd)
            .testTag("delete_image_$index")
        ) {
          Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "Remove image",
            modifier = Modifier.size(16.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // File Name
      Text(
        text = item.fileName,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        color = MaterialTheme.colorScheme.onSurface
      )

      // Meta: dimensions & size
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = item.dimensionsText,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
          text = item.formattedSize,
          style = MaterialTheme.typography.bodySmall,
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colorScheme.primary
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Reordering Controls Bar
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
      ) {
        FilledIconButton(
          onClick = onMoveUp,
          enabled = index > 0,
          colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
          ),
          modifier = Modifier
            .size(36.dp)
            .testTag("move_up_$index")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Move left / up",
            modifier = Modifier.size(18.dp)
          )
        }

        Spacer(modifier = Modifier.width(6.dp))

        FilledIconButton(
          onClick = onMoveDown,
          enabled = index < totalCount - 1,
          colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
          ),
          modifier = Modifier
            .size(36.dp)
            .testTag("move_down_$index")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "Move right / down",
            modifier = Modifier.size(18.dp)
          )
        }
      }
    }
  }
}
