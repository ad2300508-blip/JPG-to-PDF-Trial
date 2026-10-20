package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.model.ImageItem
import com.example.ui.theme.PdfGreen
import com.example.ui.theme.PdfPurple

@Composable
fun BeforeAfterCompareDialog(
  item: ImageItem,
  pageIndex: Int,
  onDismiss: () -> Unit,
  onRevertToOriginal: () -> Unit,
  onApplyEnhance: () -> Unit,
) {
  // 0: Cleaned & Enhanced, 1: Original Screenshot, 2: Side-by-Side
  var selectedTab by remember { mutableIntStateOf(if (item.isAiEnhanced) 0 else 1) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Card(
      modifier = Modifier
        .fillMaxWidth(0.92f)
        .fillMaxHeight(0.90f)
        .widthIn(max = 840.dp)
        .testTag("before_after_compare_dialog"),
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(20.dp)
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Surface(
              shape = CircleShape,
              color = PdfPurple.copy(alpha = 0.15f),
              contentColor = PdfPurple,
              modifier = Modifier.size(40.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.AutoFixHigh,
                  contentDescription = null,
                  modifier = Modifier.size(22.dp)
                )
              }
            }

            Column {
              Text(
                text = "Page ${pageIndex + 1} Studio Inspector",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Compare Original Screenshot vs AI Cleaned Book Page",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          IconButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("close_compare_dialog")
          ) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // View Mode Selector Tabs
        TabRow(
          selectedTabIndex = selectedTab,
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
          containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
          contentColor = MaterialTheme.colorScheme.primary,
        ) {
          Tab(
            selected = selectedTab == 0,
            onClick = { selectedTab = 0 },
            text = {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.AutoAwesome,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                  tint = if (selectedTab == 0) PdfGreen else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text("Cleaned & Enhanced")
              }
            }
          )

          Tab(
            selected = selectedTab == 1,
            onClick = { selectedTab = 1 },
            text = {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.PhoneAndroid,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp)
                )
                Text("Original Screenshot")
              }
            }
          )

          Tab(
            selected = selectedTab == 2,
            onClick = { selectedTab = 2 },
            text = {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.ViewColumn,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp)
                )
                Text("Side-by-Side")
              }
            }
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Main Preview Area
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(12.dp),
          contentAlignment = Alignment.Center
        ) {
          AnimatedContent(
            targetState = selectedTab,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "tab_preview"
          ) { tabIndex ->
            when (tabIndex) {
              0 -> {
                // Cleaned preview
                SingleImagePreview(
                  uri = item.enhancedUri ?: item.uri,
                  title = "Cleaned Book Page",
                  subtitle = item.enhancementDetails ?: "Auto-crop borders • 2x Super-Resolution • Pure paper background",
                  width = item.enhancedWidth ?: item.width,
                  height = item.enhancedHeight ?: item.height,
                  sizeBytes = item.enhancedSizeBytes ?: item.sizeBytes,
                  isCleaned = true
                )
              }
              1 -> {
                // Original preview
                SingleImagePreview(
                  uri = item.originalUri,
                  title = "Raw Tablet Screenshot",
                  subtitle = "Contains Android status bar, bottom navigation, and margins",
                  width = item.width,
                  height = item.height,
                  sizeBytes = item.sizeBytes,
                  isCleaned = false
                )
              }
              else -> {
                // Side-by-side split
                Row(
                  modifier = Modifier.fillMaxSize(),
                  horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  Box(modifier = Modifier.weight(1f)) {
                    SingleImagePreview(
                      uri = item.originalUri,
                      title = "Original",
                      subtitle = "Raw screenshot",
                      width = item.width,
                      height = item.height,
                      sizeBytes = item.sizeBytes,
                      isCleaned = false
                    )
                  }
                  Box(modifier = Modifier.weight(1f)) {
                    SingleImagePreview(
                      uri = item.enhancedUri ?: item.uri,
                      title = "Cleaned",
                      subtitle = "Auto-cropped & enhanced",
                      width = item.enhancedWidth ?: item.width,
                      height = item.enhancedHeight ?: item.height,
                      sizeBytes = item.enhancedSizeBytes ?: item.sizeBytes,
                      isCleaned = true
                    )
                  }
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Feature checklist / info row
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          FeaturePill("Auto-Crop Status Bar")
          FeaturePill("Navigation Trim")
          FeaturePill("2x Super-Resolution")
          FeaturePill("Paper Whitening")
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Bottom Action Bar
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          if (item.isAiEnhanced) {
            OutlinedButton(
              onClick = {
                onRevertToOriginal()
                onDismiss()
              },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.testTag("revert_to_original_btn")
            ) {
              Icon(
                imageVector = Icons.Default.Undo,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("Revert to Original")
            }
          } else {
            Spacer(modifier = Modifier.width(1.dp))
          }

          Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
              onClick = {
                onApplyEnhance()
                onDismiss()
              },
              colors = ButtonDefaults.buttonColors(
                containerColor = PdfGreen,
                contentColor = Color.White
              ),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.testTag("apply_enhance_btn")
            ) {
              Icon(
                imageVector = Icons.Default.AutoFixHigh,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(if (item.isAiEnhanced) "Re-Clean Page" else "Apply AI Clean & Enhance")
            }

            Button(
              onClick = onDismiss,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.testTag("done_compare_btn")
            ) {
              Text("Done")
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SingleImagePreview(
  uri: android.net.Uri,
  title: String,
  subtitle: String,
  width: Int,
  height: Int,
  sizeBytes: Long,
  isCleaned: Boolean,
) {
  Column(
    modifier = Modifier.fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // Badges Row
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 6.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = if (isCleaned) PdfGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
          contentColor = if (isCleaned) PdfGreen else MaterialTheme.colorScheme.onSurfaceVariant
        ) {
          Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
          )
        }
        Text(
          text = "${width} × ${height} • ${ImageItem.formatBytes(sizeBytes)}",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }

    // Image Content
    Box(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .clip(RoundedCornerShape(10.dp))
        .background(Color.White),
      contentAlignment = Alignment.Center
    ) {
      SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
          .data(uri)
          .crossfade(true)
          .build(),
        contentDescription = title,
        contentScale = ContentScale.Fit,
        modifier = Modifier.fillMaxSize(),
        loading = {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
          }
        }
      )
    }

    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = subtitle,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(top = 2.dp)
    )
  }
}

@Composable
private fun FeaturePill(text: String) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    Icon(
      imageVector = Icons.Default.Check,
      contentDescription = null,
      modifier = Modifier.size(13.dp),
      tint = PdfGreen
    )
    Text(
      text = text,
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.Medium,
      color = MaterialTheme.colorScheme.onSurface
    )
  }
}
