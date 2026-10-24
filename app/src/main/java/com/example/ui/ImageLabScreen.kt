package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.ImageConversionSettings
import com.example.model.ImageConvertItem
import com.example.model.ImageFormat
import com.example.ui.components.OmniBadge
import com.example.ui.components.OmniCard
import com.example.ui.components.OmniEmptyState
import com.example.ui.components.OmniErrorBanner
import com.example.ui.components.OmniHeader
import com.example.ui.components.OmniLinearProgress
import com.example.ui.components.OmniStatPill
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.RoseAccent
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceElevated
import com.example.viewmodel.ImageLabViewModel

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextOverflow
import com.example.ui.components.OmniPhoneTabSelector

@Composable
fun ImageLabScreen(
    viewModel: ImageLabViewModel,
    modifier: Modifier = Modifier
) {
    val items by viewModel.items.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var phoneTabIndex by remember { mutableIntStateOf(0) }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addImages(uris)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("image_lab_screen")
    ) {
        OmniHeader(
            workspaceTitle = "Image Lab",
            categoryBadge = "Transcoder & Compressor",
            badgeColor = EmeraldAccent,
            itemCount = items.size,
            isProcessing = isProcessing,
            actions = {
                OutlinedButton(
                    onClick = { pickerLauncher.launch("image/*") },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldAccent),
                    modifier = Modifier.testTag("image_add_button")
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Images", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                }
                if (items.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = { viewModel.clearAll() }) {
                        Icon(Icons.Default.ClearAll, contentDescription = "Clear All", tint = Slate400)
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (errorMessage != null) {
            OmniErrorBanner(message = errorMessage!!, onDismiss = { viewModel.dismissError() })
            Spacer(modifier = Modifier.height(10.dp))
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val isCompact = maxWidth < 600.dp

            if (isCompact) {
                // Phone Layout: 2-Tab Switcher
                Column(modifier = Modifier.fillMaxSize()) {
                    OmniPhoneTabSelector(
                        selectedTabIndex = phoneTabIndex,
                        tabs = listOf("Images (${items.size})", "Transcode Settings"),
                        accentColor = EmeraldAccent,
                        onTabSelected = { phoneTabIndex = it },
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    if (phoneTabIndex == 0) {
                        ImageQueuePane(
                            items = items,
                            onPickImages = { pickerLauncher.launch("image/*") },
                            viewModel = viewModel,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            onNavigateToSettings = if (items.isNotEmpty()) { { phoneTabIndex = 1 } } else null
                        )
                    } else {
                        ImageSettingsPane(
                            settings = settings,
                            itemsCount = items.size,
                            isProcessing = isProcessing,
                            viewModel = viewModel,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                    }
                }
            } else {
                // Dual Pane Tablet Layout: Left 42% Image Queue, Right 58% Compression Command
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ImageQueuePane(
                        items = items,
                        onPickImages = { pickerLauncher.launch("image/*") },
                        viewModel = viewModel,
                        modifier = Modifier
                            .weight(0.42f)
                            .fillMaxHeight(),
                        onNavigateToSettings = null
                    )

                    ImageSettingsPane(
                        settings = settings,
                        itemsCount = items.size,
                        isProcessing = isProcessing,
                        viewModel = viewModel,
                        modifier = Modifier
                            .weight(0.58f)
                            .fillMaxHeight()
                    )
                }
            }
        }
    }
}

@Composable
private fun ImageQueuePane(
    items: List<ImageConvertItem>,
    onPickImages: () -> Unit,
    viewModel: ImageLabViewModel,
    modifier: Modifier = Modifier,
    onNavigateToSettings: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceCard)
            .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        if (items.isEmpty()) {
            OmniEmptyState(
                icon = Icons.Default.Image,
                title = "No Images Queued",
                description = "Import JPG, PNG, WEBP, or BMP images to convert format, downscale resolution, or compress."
            ) {
                Button(
                    onClick = onPickImages,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent, contentColor = Color.Black),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Import Images", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        ImageItemCard(
                            item = item,
                            onRemove = { viewModel.removeItem(item.id) }
                        )
                    }
                }

                if (onNavigateToSettings != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onNavigateToSettings,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SurfaceElevated,
                            contentColor = EmeraldAccent
                        ),
                        border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Configure Transcode & Export →", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun ImageSettingsPane(
    settings: ImageConversionSettings,
    itemsCount: Int,
    isProcessing: Boolean,
    viewModel: ImageLabViewModel,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceCard)
            .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
            .padding(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "TARGET FORMAT & ENCODING",
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate400,
                    maxLines = 1,
                    softWrap = false
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Target Formats Grid
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ImageFormat.values().forEach { fmt ->
                        val selected = settings.targetFormat == fmt
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) EmeraldAccent.copy(alpha = 0.2f) else SurfaceElevated)
                                .border(1.dp, if (selected) EmeraldAccent else BorderSubtle, RoundedCornerShape(10.dp))
                                .clickable { viewModel.updateSettings(settings.copy(targetFormat = fmt)) }
                                .padding(vertical = 10.dp, horizontal = 6.dp)
                                .testTag("target_format_${fmt.name}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = fmt.name.replace("_", " "),
                                    color = if (selected) EmeraldAccent else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (fmt.isLossless) "LOSSLESS" else "LOSSY",
                                    color = if (fmt.isLossless) CyanAccent else Slate400,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Quality Slider (For Lossy formats)
                if (!settings.targetFormat.isLossless) {
                    Text(
                        text = "Compression Quality: ${settings.qualityPercent}%",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        softWrap = false
                    )
                    Slider(
                        value = settings.qualityPercent.toFloat(),
                        onValueChange = { viewModel.updateSettings(settings.copy(qualityPercent = it.toInt())) },
                        valueRange = 1f..100f,
                        colors = SliderDefaults.colors(thumbColor = EmeraldAccent, activeTrackColor = EmeraldAccent)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Resolution Scale Presets
                Text(
                    text = "Resolution Scale",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    softWrap = false
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(100, 75, 50).forEach { scale ->
                        val selected = settings.resolutionScalePercent == scale
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) EmeraldAccent.copy(alpha = 0.2f) else SurfaceElevated)
                                .border(1.dp, if (selected) EmeraldAccent else BorderSubtle, RoundedCornerShape(8.dp))
                                .clickable { viewModel.updateSettings(settings.copy(resolutionScalePercent = scale)) }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .testTag("scale_$scale")
                        ) {
                            Text(
                                text = "$scale%",
                                color = if (selected) EmeraldAccent else Slate400,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Command Action Bar
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OmniStatPill(label = "Queued", value = "$itemsCount")
                    OmniStatPill(label = "Mode", value = if (settings.targetFormat.isLossless) "LOSSLESS" else "${settings.qualityPercent}% Q", valueColor = EmeraldAccent)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { viewModel.convertAll() },
                    enabled = itemsCount > 0 && !isProcessing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("convert_images_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldAccent,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Transform, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isProcessing) "BATCH CONVERTING..." else "CONVERT & SAVE IMAGES",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ImageItemCard(
    item: ImageConvertItem,
    onRemove: () -> Unit
) {
    OmniCard(borderColor = BorderSubtle, backgroundColor = SurfaceElevated) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Slate800)
            ) {
                AsyncImage(model = item.uri, contentDescription = null, modifier = Modifier.fillMaxSize())
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val origKb = item.originalSizeBytes / 1024
                    OmniBadge(text = "${origKb} KB", color = Slate400)

                    if (item.convertedSizeBytes != null) {
                        val convKb = item.convertedSizeBytes / 1024
                        val savings = if (origKb > 0) ((origKb - convKb).toFloat() / origKb.toFloat() * 100).toInt() else 0
                        OmniBadge(
                            text = "${convKb} KB (${if (savings >= 0) "-$savings%" else "+${-savings}%"})",
                            color = if (savings >= 0) EmeraldAccent else RoseAccent
                        )
                    }
                }
            }

            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = RoseAccent, modifier = Modifier.size(18.dp))
            }
        }
    }
}
