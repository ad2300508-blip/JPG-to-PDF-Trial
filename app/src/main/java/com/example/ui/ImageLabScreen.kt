package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.ImageConvertItem
import com.example.model.ImageFormat
import com.example.ui.components.Dropzone
import com.example.ui.components.OmniActionBar
import com.example.ui.components.OmniProgressBanner
import com.example.ui.components.OmniSlider
import com.example.ui.components.OmniToast
import com.example.ui.components.OptionRow
import com.example.ui.components.RadioCard
import com.example.ui.components.SegmentedControl
import com.example.ui.components.ToolLayout
import com.example.ui.theme.OmniAccent
import com.example.ui.theme.OmniBorder
import com.example.ui.theme.OmniDanger
import com.example.ui.theme.OmniSurface
import com.example.ui.theme.OmniSurface2
import com.example.ui.theme.OmniText
import com.example.ui.theme.OmniTextMuted
import com.example.ui.theme.RadiusCard
import com.example.ui.theme.RadiusMd
import com.example.ui.theme.RadiusSm
import com.example.ui.theme.TintImage
import com.example.viewmodel.ImageLabViewModel

@Composable
fun ImageLabScreen(
    viewModel: ImageLabViewModel,
    modifier: Modifier = Modifier
) {
    val items by viewModel.items.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addImages(uris)
        }
    }

    val scaleOptions = listOf("100%", "75%", "50%")
    val scaleValues = listOf(100, 75, 50)
    val selectedScaleIndex = scaleValues.indexOf(settings.resolutionScalePercent).coerceAtLeast(0)

    val convertedCount = items.count { it.convertedSizeBytes != null }
    val isAllConverted = items.isNotEmpty() && convertedCount == items.size

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("image_lab_screen")
    ) {
        if (errorMessage != null) {
            OmniToast(
                message = errorMessage!!,
                isError = true,
                actionLabel = "Dismiss",
                onAction = { viewModel.dismissError() },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }

        if (isAllConverted) {
            OmniToast(
                message = "All ${items.size} images converted and saved to Pictures/OmniConvert",
                isError = false,
                actionLabel = "Clear",
                onAction = { viewModel.clearAll() },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }

        if (isProcessing) {
            val progressVal = if (items.isNotEmpty()) convertedCount.toFloat() / items.size.toFloat() else 0f
            OmniProgressBanner(
                progress = progressVal,
                statusText = "Converting $convertedCount of ${items.size} images...",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }

        ToolLayout(
            title = "Images",
            description = "Transcode, compress, and scale images to modern web formats.",
            isEmpty = items.isEmpty(),
            phoneTabTitles = listOf("Images (${items.size})", "Options"),
            emptyDropzone = {
                Dropzone(
                    icon = Icons.Default.Image,
                    headline = "No images loaded yet",
                    helpText = "Select images to transcode into JPEG, WebP, or PNG with custom quality and scale",
                    supportedFormats = "JPG · PNG · WEBP · HEIC · BMP",
                    actionLabel = "Add Images",
                    tintColor = TintImage,
                    onAction = { pickerLauncher.launch("image/*") },
                    testTag = "image_add_button"
                )
            },
            fileList = {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${items.size} images loaded",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OmniText
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { pickerLauncher.launch("image/*") },
                                modifier = Modifier
                                    .height(36.dp)
                                    .testTag("image_add_more_button"),
                                border = BorderStroke(1.dp, OmniBorder),
                                shape = RoundedCornerShape(RadiusSm),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = OmniAccent)
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add More", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                            TextButton(
                                onClick = { viewModel.clearAll() },
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("Clear", color = OmniTextMuted, fontSize = 13.sp)
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(items, key = { it.id }) { item ->
                            ImageFileRow(
                                item = item,
                                onRemove = { viewModel.removeItem(item.id) }
                            )
                        }
                    }
                }
            },
            optionsPanel = {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Format & Quality",
                        style = MaterialTheme.typography.titleMedium,
                        color = OmniText
                    )

                    // 4 Radio Cards for formats
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        RadioCard(
                            title = "JPEG",
                            description = "Standard lossy format for universal compatibility",
                            isSelected = settings.targetFormat == ImageFormat.JPEG,
                            onClick = { viewModel.updateSettings(settings.copy(targetFormat = ImageFormat.JPEG)) },
                            badgeText = "Lossy"
                        )
                        RadioCard(
                            title = "WebP (Lossy)",
                            description = "25-35% smaller than JPEG with comparable quality",
                            isSelected = settings.targetFormat == ImageFormat.WEBP_LOSSY,
                            onClick = { viewModel.updateSettings(settings.copy(targetFormat = ImageFormat.WEBP_LOSSY)) },
                            badgeText = "Recommended"
                        )
                        RadioCard(
                            title = "PNG",
                            description = "Lossless compression with transparency support",
                            isSelected = settings.targetFormat == ImageFormat.PNG,
                            onClick = { viewModel.updateSettings(settings.copy(targetFormat = ImageFormat.PNG)) },
                            badgeText = "Lossless"
                        )
                        RadioCard(
                            title = "WebP (Lossless)",
                            description = "Lossless quality with superior compression to PNG",
                            isSelected = settings.targetFormat == ImageFormat.WEBP_LOSSLESS,
                            onClick = { viewModel.updateSettings(settings.copy(targetFormat = ImageFormat.WEBP_LOSSLESS)) },
                            badgeText = "Lossless"
                        )
                    }

                    // Quality slider (only visible if format is lossy)
                    if (!settings.targetFormat.isLossless) {
                        OmniSlider(
                            label = "Quality",
                            value = settings.qualityPercent.toFloat(),
                            onValueChange = { viewModel.updateSettings(settings.copy(qualityPercent = it.toInt())) },
                            valueRange = 10f..100f,
                            valueFormatter = { "${it.toInt()}%" }
                        )
                    }

                    // Resolution Scale Segmented Control
                    Column {
                        Text(
                            text = "Resolution Scale",
                            fontSize = 14.sp,
                            color = OmniTextMuted,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        SegmentedControl(
                            options = scaleOptions,
                            selectedIndex = selectedScaleIndex,
                            onOptionSelected = { idx ->
                                viewModel.updateSettings(settings.copy(resolutionScalePercent = scaleValues[idx]))
                            }
                        )
                    }

                    // Strip EXIF
                    OptionRow(
                        label = "Strip EXIF metadata",
                        description = "Removes GPS location and camera data for privacy",
                        checked = settings.stripExif,
                        onCheckedChange = { viewModel.updateSettings(settings.copy(stripExif = it)) }
                    )
                }
            },
            actionBar = {
                val formatName = when (settings.targetFormat) {
                    ImageFormat.JPEG -> "JPEG"
                    ImageFormat.WEBP_LOSSY -> "WebP"
                    ImageFormat.PNG -> "PNG"
                    ImageFormat.WEBP_LOSSLESS -> "WebP LL"
                }
                OmniActionBar(
                    summaryText = "${items.size} images · $formatName ${if (!settings.targetFormat.isLossless) "${settings.qualityPercent}%" else ""} · ${settings.resolutionScalePercent}%",
                    primaryButtonLabel = if (isProcessing) "Converting..." else "Convert Images",
                    isEnabled = items.isNotEmpty() && !isProcessing,
                    disabledHelperText = if (items.isEmpty()) "Add at least one image" else null,
                    onPrimaryAction = { viewModel.convertAll() },
                    testTag = "convert_all_images_button"
                )
            }
        )
    }
}

@Composable
private fun ImageFileRow(
    item: ImageConvertItem,
    onRemove: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RadiusMd)),
        color = OmniSurface,
        border = BorderStroke(1.dp, OmniBorder),
        shape = RoundedCornerShape(RadiusMd)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(RadiusSm))
                    .background(OmniSurface2),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = item.uri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OmniText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val sizeKb = item.originalSizeBytes / 1024
                    val sizeLabel = if (sizeKb > 1024) "${"%.1f".format(sizeKb / 1024f)} MB" else "$sizeKb KB"
                    val dimLabel = if (item.originalWidth > 0) " · ${item.originalWidth}x${item.originalHeight}" else ""
                    Text(
                        text = "$sizeLabel$dimLabel",
                        fontSize = 13.sp,
                        color = OmniTextMuted
                    )

                    if (item.convertedSizeBytes != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        val convKb = item.convertedSizeBytes / 1024
                        val convLabel = if (convKb > 1024) "${"%.1f".format(convKb / 1024f)} MB" else "$convKb KB"
                        Text(
                            text = "→ $convLabel",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = OmniAccent
                        )
                    }
                }
            }

            if (item.isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = OmniAccent,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove image",
                    tint = OmniTextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
