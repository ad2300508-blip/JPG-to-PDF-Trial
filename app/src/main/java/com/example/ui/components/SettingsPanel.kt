package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.model.DownscaleResolution
import com.example.model.PageMargin
import com.example.model.PdfPageSize
import com.example.model.PdfSettings
import com.example.model.SmartCropMode
import com.example.ui.theme.PdfGreen
import com.example.ui.theme.PdfOrange
import com.example.ui.theme.PdfPurple

@Composable
fun SettingsPanel(
  settings: PdfSettings,
  onSettingsChanged: ((PdfSettings) -> PdfSettings) -> Unit,
  imageCount: Int,
  rawSizeBytes: Long,
  estimatedPdfBytes: Long,
  onConvertClick: () -> Unit,
  isConverting: Boolean,
  isBatchCleaning: Boolean = false,
  batchCleanProgress: Float = 0f,
  batchCleanStatusText: String = "",
  onBatchCleanAllPages: () -> Unit = {},
  onSaveGeminiApiKey: (String) -> Unit = {},
  modifier: Modifier = Modifier,
) {
  var showUserPassword by remember { mutableStateOf(false) }
  var showOwnerPassword by remember { mutableStateOf(false) }
  var showApiKey by remember { mutableStateOf(false) }
  var enteredApiKey by remember(settings.customGeminiApiKey) { mutableStateOf(settings.customGeminiApiKey) }

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // 1. Compression Engine Card
    Card(
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp)
      ) {
        SectionHeader(
          icon = Icons.Default.Tune,
          title = "Compression Engine",
          badge = if (settings.enableCompression) "Active" else "Off",
          badgeColor = if (settings.enableCompression) PdfGreen else MaterialTheme.colorScheme.outline
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Master Compression Toggle Row
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Smart Image Compression",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Reduces PDF file size up to 90% while preserving document sharpness",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            Spacer(modifier = Modifier.width(10.dp))

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
        }

        AnimatedVisibility(
          visible = settings.enableCompression,
          enter = fadeIn() + expandVertically(),
          exit = fadeOut() + shrinkVertically()
        ) {
          Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.height(18.dp))

            // JPEG Quality Slider with Live Value Indicator
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(
                  text = "JPEG Image Quality",
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = settings.qualityDescription,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.primary
                )
              }

              Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
              ) {
                Text(
                  text = "${settings.jpegQuality}%",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.ExtraBold,
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Slider(
              value = settings.jpegQuality.toFloat(),
              onValueChange = { newValue ->
                onSettingsChanged { it.copy(jpegQuality = newValue.toInt()) }
              },
              valueRange = 10f..100f,
              steps = 17,
              colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
              ),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("jpeg_quality_slider")
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text("10% Compact", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text("80% Balanced", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text("100% Max", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Resolution Downscaling Segmented Buttons
            Text(
              text = "Target Resolution Downscaling",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(
              modifier = Modifier.fillMaxWidth(),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              DownscaleResolution.values().forEach { res ->
                val isSelected = settings.downscaleResolution == res
                ResolutionSegmentRow(
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

    // 2. AI Book & Screenshot Studio Card
    Card(
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp)
      ) {
        SectionHeader(
          icon = Icons.Default.AutoFixHigh,
          title = "Universal Smart Screenshot Cleaner",
          badge = "Zero-Loss Trimmer",
          badgeColor = PdfPurple
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Explanatory callout
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = PdfPurple.copy(alpha = 0.08f),
          border = BorderStroke(1.dp, PdfPurple.copy(alpha = 0.2f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Icon(
              imageVector = Icons.Default.PhoneAndroid,
              contentDescription = null,
              tint = PdfPurple,
              modifier = Modifier.size(22.dp)
            )
            Text(
              text = "Universal engine for e-books, web pages, chats, documents & system UI. Strips status bars, navigation bars, pillarboxes, and dynamic floating toasts with content-aware 3% uniform frame padding.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Toggle 1: Auto-Crop Borders & Remove UI Chrome
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Auto-Crop Borders & Remove UI Chrome",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Strips top status bar, bottom nav bar, letterboxes & neutralizes intrusive floating toasts with uniform 3% frame",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Switch(
              checked = settings.autoRemoveBorders,
              onCheckedChange = { isEnabled ->
                onSettingsChanged { it.copy(autoRemoveBorders = isEnabled) }
              },
              colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = PdfPurple,
              ),
              modifier = Modifier.testTag("auto_remove_borders_switch")
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Toggle 2: Text & Diagram Super-Resolution Sharpening
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Text & Diagram Super-Resolution Sharpening",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Dynamic overlay eraser + luminance-only typography unsharp mask + background paper whitening (#FFFFFF)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Switch(
              checked = settings.superResolutionEnhancer,
              onCheckedChange = { isEnabled ->
                onSettingsChanged { it.copy(superResolutionEnhancer = isEnabled) }
              },
              colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = PdfPurple,
              ),
              modifier = Modifier.testTag("super_resolution_switch")
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Toggle 3: Split Dual-Page Spread (2-Up to 1-Up)
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Split Dual-Page Book Spreads",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Splits landscape spreads (e.g., Zanichelli/bSmart) into 2 sequential portrait pages (Left first, Right second) & erases spine shadow",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Switch(
              checked = settings.splitDualPageSpread,
              onCheckedChange = { isEnabled ->
                onSettingsChanged { it.copy(splitDualPageSpread = isEnabled) }
              },
              colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = PdfPurple,
              ),
              modifier = Modifier.testTag("split_dual_page_switch")
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Crop Mode Selector (Offline vs Gemini AI)
        Text(
          text = "Crop Detection Engine",
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          SmartCropMode.values().forEach { mode ->
            val isSelected = settings.cropMode == mode
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = if (isSelected) PdfPurple.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
              border = BorderStroke(
                if (isSelected) 2.dp else 1.dp,
                if (isSelected) PdfPurple else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
              ),
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                  onSettingsChanged { it.copy(cropMode = mode) }
                }
                .testTag("crop_mode_${mode.name}")
            ) {
              Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.Center
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Icon(
                    imageVector = if (mode == SmartCropMode.OFFLINE_OCR_EDGE) Icons.Default.Crop else Icons.Default.Cloud,
                    contentDescription = null,
                    tint = if (isSelected) PdfPurple else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                  )
                  Text(
                    text = mode.shortLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) PdfPurple else MaterialTheme.colorScheme.onSurface
                  )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = mode.description,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 2
                )
              }
            }
          }
        }

        // Collapsible Gemini API Key field
        AnimatedVisibility(
          visible = settings.cropMode == SmartCropMode.GEMINI_VISION,
          enter = fadeIn() + expandVertically(),
          exit = fadeOut() + shrinkVertically()
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 12.dp)
          ) {
            OutlinedTextField(
              value = enteredApiKey,
              onValueChange = { enteredApiKey = it },
              label = { Text("Gemini API Key (Optional)") },
              placeholder = { Text("Leaves blank to use AI Studio system key") },
              singleLine = true,
              visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
              trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  IconButton(onClick = { showApiKey = !showApiKey }) {
                    Icon(
                      imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                      contentDescription = if (showApiKey) "Hide key" else "Show key"
                    )
                  }
                  IconButton(
                    onClick = {
                      onSaveGeminiApiKey(enteredApiKey)
                    }
                  ) {
                    Icon(
                      imageVector = Icons.Default.Save,
                      contentDescription = "Save key",
                      tint = MaterialTheme.colorScheme.primary
                    )
                  }
                }
              },
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("gemini_api_key_input")
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Calls Google Gemini Flash Vision API for exact page boundary detection. Falls back automatically to local detector if offline.",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Batch Clean All Pages Now Button
        Button(
          onClick = onBatchCleanAllPages,
          enabled = imageCount > 0 && !isBatchCleaning,
          colors = ButtonDefaults.buttonColors(
            containerColor = PdfPurple,
            contentColor = Color.White,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
          ),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("batch_clean_all_pages_button")
        ) {
          if (isBatchCleaning) {
            CircularProgressIndicator(
              modifier = Modifier.size(18.dp),
              strokeWidth = 2.dp,
              color = Color.White
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "Cleaning Screenshots...",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold
            )
          } else {
            Icon(
              imageVector = Icons.Default.AutoFixHigh,
              contentDescription = null,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = if (imageCount > 0) "Batch Clean All $imageCount Screenshots Now" else "Add Images to Batch Clean",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold
            )
          }
        }

        // Live Batch Progress Indicator
        if (isBatchCleaning) {
          Spacer(modifier = Modifier.height(8.dp))
          LinearProgressIndicator(
            progress = { batchCleanProgress },
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(4.dp)),
            color = PdfPurple,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = batchCleanStatusText,
            style = MaterialTheme.typography.labelSmall,
            color = PdfPurple,
            fontWeight = FontWeight.Medium
          )
        }
      }
    }

    // 3. Page Layout & Margins Card
    Card(
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp)
      ) {
        SectionHeader(
          icon = Icons.Default.AspectRatio,
          title = "Page Format & Sizing",
          badge = settings.pageSize.label,
          badgeColor = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Page Size Segmented Buttons
        Text(
          text = "PDF Canvas Dimensions",
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          PdfPageSize.values().forEach { size ->
            val isSelected = settings.pageSize == size
            OptionChipButton(
              text = size.label,
              subtitle = size.description,
              isSelected = isSelected,
              onClick = {
                onSettingsChanged { it.copy(pageSize = size) }
              },
              modifier = Modifier.weight(1f)
            )
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Margins Selector
        SectionHeader(
          icon = Icons.Default.Straighten,
          title = "Document Margins",
          badge = settings.pageMargin.label,
          badgeColor = MaterialTheme.colorScheme.tertiary
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          PageMargin.values().forEach { margin ->
            val isSelected = settings.pageMargin == margin
            OptionChipButton(
              text = margin.label,
              subtitle = "${margin.points.toInt()} pt",
              isSelected = isSelected,
              onClick = {
                onSettingsChanged { it.copy(pageMargin = margin) }
              },
              modifier = Modifier.weight(1f)
            )
          }
        }
      }
    }

    // 4. Enterprise Password Encryption Card
    Card(
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp)
      ) {
        SectionHeader(
          icon = Icons.Default.Security,
          title = "Document Security & Encryption",
          badge = if (settings.enableEncryption) "Protected" else "Unencrypted",
          badgeColor = if (settings.enableEncryption) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Secure PDF with Password Switch
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Secure PDF with Password",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Requires password verification in Adobe Acrobat, Samsung Notes, and PDF readers",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Switch(
              checked = settings.enableEncryption,
              onCheckedChange = { isEnabled ->
                onSettingsChanged { it.copy(enableEncryption = isEnabled) }
              },
              colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onError,
                checkedTrackColor = MaterialTheme.colorScheme.error,
              ),
              modifier = Modifier.testTag("enable_encryption_switch")
            )
          }
        }

        AnimatedVisibility(
          visible = settings.enableEncryption,
          enter = fadeIn() + expandVertically(),
          exit = fadeOut() + shrinkVertically()
        ) {
          Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.height(16.dp))

            // User Password Field
            OutlinedTextField(
              value = settings.userPassword,
              onValueChange = { newPass ->
                onSettingsChanged { it.copy(userPassword = newPass) }
              },
              label = { Text("User Password (Required to Open)") },
              placeholder = { Text("e.g. Secret123") },
              singleLine = true,
              visualTransformation = if (showUserPassword) VisualTransformation.None else PasswordVisualTransformation(),
              trailingIcon = {
                IconButton(onClick = { showUserPassword = !showUserPassword }) {
                  Icon(
                    imageVector = if (showUserPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (showUserPassword) "Hide password" else "Show password"
                  )
                }
              },
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("user_password_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Owner / Admin Password Field
            OutlinedTextField(
              value = settings.ownerPassword,
              onValueChange = { newPass ->
                onSettingsChanged { it.copy(ownerPassword = newPass) }
              },
              label = { Text("Owner Password (Optional / Permission Admin)") },
              placeholder = { Text("Leaves blank to use User Password") },
              singleLine = true,
              visualTransformation = if (showOwnerPassword) VisualTransformation.None else PasswordVisualTransformation(),
              trailingIcon = {
                IconButton(onClick = { showOwnerPassword = !showOwnerPassword }) {
                  Icon(
                    imageVector = if (showOwnerPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (showOwnerPassword) "Hide password" else "Show password"
                  )
                }
              },
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("owner_password_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (!settings.hasValidPassword) {
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                  )
                  Text(
                    text = "Please enter a user password to encrypt the document.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                  )
                }
              }
            } else {
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                  )
                  Text(
                    text = "128-bit AES/Standard encryption will be embedded directly into the PDF.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                }
              }
            }
          }
        }
      }
    }

    // 5. Searchable OCR Text Layer Card
    Card(
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp)
      ) {
        SectionHeader(
          icon = Icons.Default.TextFields,
          title = "Searchable PDF (ML Kit OCR)",
          badge = if (settings.embedSearchableText) "Searchable" else "Image Only",
          badgeColor = if (settings.embedSearchableText) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline
        )

        Spacer(modifier = Modifier.height(14.dp))

        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Embed Searchable OCR Layer",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Bakes recognized text invisibly beneath images so you can highlight, search, and copy text in PDF viewers",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Switch(
              checked = settings.embedSearchableText,
              onCheckedChange = { isEnabled ->
                onSettingsChanged { it.copy(embedSearchableText = isEnabled) }
              },
              colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onTertiary,
                checkedTrackColor = MaterialTheme.colorScheme.tertiary,
              ),
              modifier = Modifier.testTag("embed_searchable_ocr_switch")
            )
          }
        }
      }
    }

    // 6. Document Name Card
    Card(
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp)
      ) {
        SectionHeader(
          icon = Icons.Default.Edit,
          title = "Document File Name",
          badge = ".pdf",
          badgeColor = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
          value = settings.outputFileName,
          onValueChange = { newName ->
            onSettingsChanged { it.copy(outputFileName = newName) }
          },
          label = { Text("Output PDF Name") },
          singleLine = true,
          trailingIcon = {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = MaterialTheme.colorScheme.primaryContainer,
              modifier = Modifier.padding(end = 8.dp)
            ) {
              Text(
                text = ".pdf",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("output_file_name_input"),
          shape = RoundedCornerShape(14.dp),
        )
      }
    }

    // 7. Live Compression Savings Gauge
    LiveSizeEstimatorCard(
      imageCount = imageCount,
      rawSizeBytes = rawSizeBytes,
      estimatedPdfBytes = estimatedPdfBytes,
      settings = settings
    )

    // 8. High-Impact Convert & Export Action
    Button(
      onClick = onConvertClick,
      enabled = imageCount > 0 && !isConverting && settings.hasValidPassword,
      shape = RoundedCornerShape(18.dp),
      colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary
      ),
      modifier = Modifier
        .fillMaxWidth()
        .height(58.dp)
        .testTag("convert_to_pdf_primary_button")
    ) {
      Icon(
        imageVector = if (settings.enableEncryption) Icons.Default.Lock else Icons.Default.PictureAsPdf,
        contentDescription = null,
        modifier = Modifier.size(24.dp)
      )
      Spacer(modifier = Modifier.width(10.dp))
      Text(
        text = if (imageCount > 0) {
          if (settings.enableEncryption) "Encrypt & Export $imageCount Pages" else "Convert & Export $imageCount Pages"
        } else {
          "Add Images to Convert"
        },
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
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Surface(
        shape = CircleShape,
        color = badgeColor.copy(alpha = 0.15f),
        contentColor = badgeColor,
        modifier = Modifier.size(32.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
          )
        }
      }

      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )
    }

    Surface(
      shape = RoundedCornerShape(8.dp),
      color = badgeColor.copy(alpha = 0.15f),
      contentColor = badgeColor
    ) {
      Text(
        text = badge,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
      )
    }
  }
}

@Composable
private fun ResolutionSegmentRow(
  option: DownscaleResolution,
  isSelected: Boolean,
  onClick: () -> Unit,
) {
  val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
  val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)

  Surface(
    shape = RoundedCornerShape(12.dp),
    color = containerColor,
    border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .clickable(onClick = onClick)
      .testTag("res_option_${option.name}")
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(
          text = option.label,
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = option.description,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
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
  }
}

@Composable
private fun OptionChipButton(
  text: String,
  subtitle: String,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
  val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)

  Surface(
    shape = RoundedCornerShape(12.dp),
    color = containerColor,
    border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
    modifier = modifier
      .clip(RoundedCornerShape(12.dp))
      .clickable(onClick = onClick)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(10.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = subtitle,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}
