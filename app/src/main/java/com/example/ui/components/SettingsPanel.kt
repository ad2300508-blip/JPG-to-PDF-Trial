package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.DownscaleResolution
import com.example.model.PageMargin
import com.example.model.PdfPageSize
import com.example.model.PdfSettings

@Composable
fun SettingsPanel(
  settings: PdfSettings,
  onSettingsChanged: ((PdfSettings) -> PdfSettings) -> Unit,
  imageCount: Int,
  rawSizeBytes: Long,
  estimatedPdfBytes: Long,
  onConvertClick: () -> Unit,
  isConverting: Boolean,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // 1. Compression Controls Card
    Card(
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface
      ),
      elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp)
      ) {
        SectionHeader(
          icon = Icons.Default.Compress,
          title = "Image Compression",
          badge = if (settings.enableCompression) "Active" else "Off",
          badgeColor = if (settings.enableCompression) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Enable Compression Toggle
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(horizontal = 14.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Enable Compression",
              style = MaterialTheme.typography.bodyLarge,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "Reduces PDF file size significantly while retaining sharp detail",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          Spacer(modifier = Modifier.width(8.dp))

          Switch(
            checked = settings.enableCompression,
            onCheckedChange = { isEnabled ->
              onSettingsChanged { it.copy(enableCompression = isEnabled) }
            },
            colors = SwitchDefaults.colors(
              checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
              checkedTrackColor = MaterialTheme.colorScheme.primary,
            ),
            modifier = Modifier.testTag("enable_compression_switch")
          )
        }

        AnimatedVisibility(
          visible = settings.enableCompression,
          enter = fadeIn() + expandVertically(),
          exit = fadeOut() + shrinkVertically()
        ) {
          Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.height(16.dp))

            // JPEG Quality Slider (10% - 100%)
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "JPEG Quality",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
              )
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
              ) {
                Text(
                  text = "${settings.jpegQuality}%",
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
              }
            }

            Text(
              text = settings.qualityDescription,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
            )

            Slider(
              value = settings.jpegQuality.toFloat(),
              onValueChange = { newValue ->
                onSettingsChanged { it.copy(jpegQuality = newValue.toInt()) }
              },
              valueRange = 10f..100f,
              steps = 17, // 5% increments
              colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
              ),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("jpeg_quality_slider")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Downscaling Selector
            Text(
              text = "Resolution Downscaling",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Medium,
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(
              modifier = Modifier.fillMaxWidth(),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              DownscaleResolution.values().forEach { res ->
                val isSelected = settings.downscaleResolution == res
                ResolutionOptionRow(
                  option = res,
                  isSelected = isSelected,
                  onClick = {
                    onSettingsChanged { it.copy(downscaleResolution = res) }
                  }
                )
              }
            }
          }
        }
      }
    }

    // 2. PDF Page Options Card
    Card(
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface
      ),
      elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp)
      ) {
        SectionHeader(
          icon = Icons.Default.Description,
          title = "Page Configuration",
          badge = settings.pageSize.label,
          badgeColor = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Page Size
        Text(
          text = "Page Size",
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          PdfPageSize.values().forEach { sizeOption ->
            val isSelected = settings.pageSize == sizeOption
            FilterChip(
              selected = isSelected,
              onClick = {
                onSettingsChanged { it.copy(pageSize = sizeOption) }
              },
              label = {
                Text(sizeOption.label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
              },
              modifier = Modifier
                .weight(1f)
                .testTag("page_size_${sizeOption.name.lowercase()}"),
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
              )
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Page Margins
        Text(
          text = "Page Margins",
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          PageMargin.values().forEach { marginOption ->
            val isSelected = settings.pageMargin == marginOption
            FilterChip(
              selected = isSelected,
              onClick = {
                onSettingsChanged { it.copy(pageMargin = marginOption) }
              },
              label = {
                Text(marginOption.label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
              },
              modifier = Modifier
                .weight(1f)
                .testTag("margin_${marginOption.name.lowercase()}"),
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
              )
            )
          }
        }
      }
    }

    // 3. Custom Output File Name Card
    Card(
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface
      ),
      elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp)
      ) {
        SectionHeader(
          icon = Icons.Default.Edit,
          title = "Output File Name",
          badge = ".pdf",
          badgeColor = MaterialTheme.colorScheme.tertiary
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = settings.outputFileName,
          onValueChange = { newName ->
            onSettingsChanged { it.copy(outputFileName = newName) }
          },
          label = { Text("File Name") },
          singleLine = true,
          trailingIcon = {
            Text(
              text = ".pdf",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(end = 12.dp)
            )
          },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("output_file_name_input"),
          shape = RoundedCornerShape(12.dp),
        )
      }
    }

    // 4. Live Size Estimator
    LiveSizeEstimatorCard(
      imageCount = imageCount,
      rawSizeBytes = rawSizeBytes,
      estimatedPdfBytes = estimatedPdfBytes,
      settings = settings
    )

    // 5. Main Convert Button
    Button(
      onClick = onConvertClick,
      enabled = imageCount > 0 && !isConverting,
      shape = RoundedCornerShape(16.dp),
      colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary
      ),
      modifier = Modifier
        .fillMaxWidth()
        .height(56.dp)
        .testTag("convert_to_pdf_primary_button")
    ) {
      Icon(
        imageVector = Icons.Default.PictureAsPdf,
        contentDescription = null,
        modifier = Modifier.size(24.dp)
      )
      Spacer(modifier = Modifier.width(10.dp))
      Text(
        text = if (imageCount > 0) "Convert $imageCount Images to PDF" else "Add Images to Convert",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
      )
    }
  }
}

@Composable
private fun SectionHeader(
  icon: ImageVector,
  title: String,
  badge: String,
  badgeColor: Color,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier = Modifier
          .size(32.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(18.dp)
        )
      }
      Spacer(modifier = Modifier.width(10.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )
    }

    Surface(
      shape = RoundedCornerShape(12.dp),
      color = badgeColor.copy(alpha = 0.12f),
      contentColor = badgeColor
    ) {
      Text(
        text = badge,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
      )
    }
  }
}

@Composable
private fun ResolutionOptionRow(
  option: DownscaleResolution,
  isSelected: Boolean,
  onClick: () -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(
      width = if (isSelected) 1.5.dp else 1.dp,
      color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    ),
    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface,
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .clickable(onClick = onClick)
      .testTag("downscale_option_${option.name.lowercase()}")
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Column {
        Text(
          text = option.label,
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
          color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = option.description,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      Box(
        modifier = Modifier
          .size(20.dp)
          .clip(CircleShape)
          .background(
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
          ),
        contentAlignment = Alignment.Center
      ) {
        if (isSelected) {
          Box(
            modifier = Modifier
              .size(8.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.onPrimary)
          )
        }
      }
    }
  }
}
