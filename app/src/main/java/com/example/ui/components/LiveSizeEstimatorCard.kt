package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.ImageItem
import com.example.model.PdfSettings
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber

@Composable
fun LiveSizeEstimatorCard(
  imageCount: Int,
  rawSizeBytes: Long,
  estimatedPdfBytes: Long,
  settings: PdfSettings,
  modifier: Modifier = Modifier,
) {
  val hasSavings = settings.enableCompression && rawSizeBytes > 0 && estimatedPdfBytes < rawSizeBytes
  val savingsBytes = if (hasSavings) rawSizeBytes - estimatedPdfBytes else 0L
  val savingsPercent = if (hasSavings && rawSizeBytes > 0) {
    (((rawSizeBytes - estimatedPdfBytes).toDouble() / rawSizeBytes.toDouble()) * 100).toInt()
  } else {
    0
  }

  val gaugeProgress = if (rawSizeBytes > 0) {
    (estimatedPdfBytes.toFloat() / rawSizeBytes.toFloat()).coerceIn(0.04f, 1f)
  } else {
    1f
  }

  val animatedProgress by animateFloatAsState(targetValue = gaugeProgress, label = "gauge_progress")

  val badgeColor = when {
    savingsPercent >= 60 -> SuccessGreen
    savingsPercent >= 30 -> AccentCyan
    savingsPercent > 0 -> WarningAmber
    else -> MaterialTheme.colorScheme.outline
  }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("live_size_estimator_card"),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ),
    border = androidx.compose.foundation.BorderStroke(
      width = 1.dp,
      color = if (hasSavings) badgeColor.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(18.dp)
    ) {
      // Header: Gauge Title & Savings Badge
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .background(badgeColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Speed,
              contentDescription = null,
              tint = badgeColor,
              modifier = Modifier.size(20.dp)
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "Compression Savings Gauge",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = if (settings.enableCompression) "Live estimation based on current quality" else "Compression disabled (Raw bytes)",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        if (hasSavings && savingsPercent > 0) {
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = badgeColor.copy(alpha = 0.15f),
            border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.35f))
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.TrendingDown,
                contentDescription = null,
                tint = badgeColor,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "-$savingsPercent%",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = badgeColor
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Gauge Numbers Breakdown
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(14.dp))
          .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
          .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "ORIGINAL RAW",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = ImageItem.formatBytes(rawSizeBytes),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
          )
        }

        Text(
          text = "➔",
          style = MaterialTheme.typography.headlineSmall,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Bold
        )

        Column(horizontalAlignment = Alignment.End) {
          Text(
            text = "ESTIMATED PDF",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
          Text(
            text = if (imageCount > 0) ImageItem.formatBytes(estimatedPdfBytes) else "--",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Visual Gauge Progress Bar
      LinearProgressIndicator(
        progress = { animatedProgress },
        modifier = Modifier
          .fillMaxWidth()
          .height(10.dp)
          .clip(RoundedCornerShape(5.dp)),
        color = badgeColor,
        trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
      )

      Spacer(modifier = Modifier.height(8.dp))

      // Gauge Labels
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = if (hasSavings) "Saved: ${ImageItem.formatBytes(savingsBytes)}" else "Full fidelity output",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Medium,
          color = if (hasSavings) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
          text = "${settings.jpegQuality}% Quality • ${settings.downscaleResolution.label}",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}
