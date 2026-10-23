package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.TabletMac
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.OmniWorkspace
import com.example.ui.theme.BadgeMd
import com.example.ui.theme.BadgeMdBg
import com.example.ui.theme.BorderCrisp
import com.example.ui.theme.CardSurface

@Composable
fun OmniCommandHeader(
  currentWorkspace: OmniWorkspace,
  statusText: String = "Engine Ready • 100% On-Device",
  modifier: Modifier = Modifier
) {
  val (workspaceName, workspaceTag) = when (currentWorkspace) {
    OmniWorkspace.PDF_STUDIO -> "PDF STUDIO" to "BOOK SCREENSHOT & CLEANER"
    OmniWorkspace.IMAGE_LAB -> "IMAGE LAB" to "BATCH LOSSY & LOSSLESS"
    OmniWorkspace.AUDIO_LAB -> "AUDIO LAB" to "NATIVE CODEC TRANSCODER"
    OmniWorkspace.ARCHIVE_ZIP -> "ARCHIVE ZIP" to "LOSSLESS DEFLATE ARCHIVER"
    OmniWorkspace.AI_MARKDOWN -> "AI MARKDOWN" to "LLM CONTEXT & PROMPT EXPORTER"
  }

  Surface(
    color = CardSurface,
    shape = RoundedCornerShape(16.dp),
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 8.dp)
      .border(1.dp, BorderCrisp, RoundedCornerShape(16.dp))
      .testTag("omni_command_header")
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Breadcrumb / Workspace Identification
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(8.dp)
            .background(Color(0xFF38BDF8), CircleShape)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "OMNICONVERT",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "  /  ",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Text(
              text = workspaceName,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.Bold
            )
          }
          Text(
            text = workspaceTag,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
        }
      }

      // Tablet Device Profile & Live Status Chips
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // High-End Tablet Indicator Chip
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
          border = androidx.compose.foundation.BorderStroke(1.dp, BorderCrisp)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.TabletMac,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "TAB S10 ULTRA • PRO SUITE",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurface,
              fontWeight = FontWeight.Bold
            )
          }
        }

        // Engine Status Chip
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = BadgeMdBg,
          border = androidx.compose.foundation.BorderStroke(1.dp, BadgeMd.copy(alpha = 0.4f))
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(6.dp)
                .background(BadgeMd, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "OFFLINE • ZERO COST",
              style = MaterialTheme.typography.labelSmall,
              color = BadgeMd,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }
  }
}
