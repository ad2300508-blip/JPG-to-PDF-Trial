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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import com.example.ui.components.OmniPhoneTabSelector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ArchiveConfig
import com.example.model.ArchiveItem
import com.example.ui.components.OmniBadge
import com.example.ui.components.OmniCard
import com.example.ui.components.OmniEmptyState
import com.example.ui.components.OmniErrorBanner
import com.example.ui.components.OmniHeader
import com.example.ui.components.OmniLinearProgress
import com.example.ui.components.OmniStatPill
import com.example.ui.components.OmniSuccessBanner
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.RoseAccent
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceElevated
import com.example.viewmodel.ArchiveViewModel

@Composable
fun ArchiveScreen(
    viewModel: ArchiveViewModel,
    modifier: Modifier = Modifier
) {
    val items by viewModel.items.collectAsState()
    val config by viewModel.config.collectAsState()
    val isArchiving by viewModel.isArchiving.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val lastGeneratedUri by viewModel.lastGeneratedUri.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addFiles(uris)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("archive_screen")
    ) {
        OmniHeader(
            workspaceTitle = "Archive ZIP",
            categoryBadge = "Lossless Deflate Archiver",
            badgeColor = AmberAccent,
            itemCount = items.size,
            isProcessing = isArchiving,
            actions = {
                OutlinedButton(
                    onClick = { pickerLauncher.launch("*/*") },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberAccent),
                    modifier = Modifier.testTag("archive_add_button")
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Files")
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

        if (lastGeneratedUri != null) {
            OmniSuccessBanner(message = "ZIP Archive created and saved to Downloads/OmniConvert/Archives", onDismiss = { viewModel.clearAll() })
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (isArchiving) {
            val p = if (progress.second > 0) progress.first.toFloat() / progress.second.toFloat() else 0f
            OmniLinearProgress(progress = p, statusText = "Compressing file ${progress.first} of ${progress.second}...")
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Responsive Layout: Tablet (>= 600.dp) side-by-side, Phone (< 600.dp) 2-Tab Switcher
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val isTablet = maxWidth >= 600.dp
            if (isTablet) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(0.42f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceCard)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        ArchiveFilesPane(
                            items = items,
                            onPickFiles = { pickerLauncher.launch("*/*") },
                            onRemoveItem = { viewModel.removeItem(it) }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(0.58f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceCard)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                            .padding(18.dp)
                    ) {
                        ArchiveSettingsPane(
                            items = items,
                            config = config,
                            isArchiving = isArchiving,
                            onUpdateConfig = { viewModel.updateConfig(it) },
                            onCreateArchive = { viewModel.createArchive() }
                        )
                    }
                }
            } else {
                var selectedPhoneTab by rememberSaveable { mutableIntStateOf(0) }
                Column(modifier = Modifier.fillMaxSize()) {
                    OmniPhoneTabSelector(
                        tabs = listOf("Files (${items.size})", "ZIP Settings"),
                        selectedIndex = selectedPhoneTab,
                        onTabSelected = { selectedPhoneTab = it },
                        activeColor = AmberAccent
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceCard)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        if (selectedPhoneTab == 0) {
                            ArchiveFilesPane(
                                items = items,
                                onPickFiles = { pickerLauncher.launch("*/*") },
                                onRemoveItem = { viewModel.removeItem(it) }
                            )
                        } else {
                            ArchiveSettingsPane(
                                items = items,
                                config = config,
                                isArchiving = isArchiving,
                                onUpdateConfig = { viewModel.updateConfig(it) },
                                onCreateArchive = { viewModel.createArchive() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArchiveFilesPane(
    items: List<ArchiveItem>,
    onPickFiles: () -> Unit,
    onRemoveItem: (String) -> Unit
) {
    if (items.isEmpty()) {
        OmniEmptyState(
            icon = Icons.Default.FolderZip,
            title = "No Files to Archive",
            description = "Add any documents, images, audio, or datasets to bundle into a standard lossless Deflate ZIP."
        ) {
            Button(
                onClick = onPickFiles,
                colors = ButtonDefaults.buttonColors(containerColor = AmberAccent, contentColor = Color.Black)
            ) {
                Text("Select Files")
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(items, key = { it.id }) { item ->
                ArchiveItemCard(item = item, onRemove = { onRemoveItem(item.id) })
            }
        }
    }
}

@Composable
private fun ArchiveSettingsPane(
    items: List<ArchiveItem>,
    config: ArchiveConfig,
    isArchiving: Boolean,
    onUpdateConfig: (ArchiveConfig) -> Unit,
    onCreateArchive: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "ARCHIVE ENGINE CONFIGURATION",
                style = MaterialTheme.typography.labelSmall,
                color = Slate400
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = config.zipFileName,
                onValueChange = { onUpdateConfig(config.copy(zipFileName = it)) },
                label = { Text("Archive Name") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("zip_name_input"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Deflate Compression Level: ${config.compressionLevel} / 9",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (config.compressionLevel == 0) "Store (No compression, fastest)" else if (config.compressionLevel <= 6) "Standard Deflate (Balanced)" else "Maximum Deflate (Smallest file size)",
                style = MaterialTheme.typography.bodyMedium,
                color = Slate400
            )
            Slider(
                value = config.compressionLevel.toFloat(),
                onValueChange = { onUpdateConfig(config.copy(compressionLevel = it.toInt())) },
                valueRange = 0f..9f,
                steps = 8,
                colors = SliderDefaults.colors(thumbColor = AmberAccent, activeTrackColor = AmberAccent)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Command Action Bar
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OmniStatPill(label = "Total Files", value = "${items.size}")
                val totalKb = items.sumOf { it.sizeBytes } / 1024
                OmniStatPill(label = "Uncompressed", value = "$totalKb KB", valueColor = AmberAccent)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onCreateArchive,
                enabled = items.isNotEmpty() && !isArchiving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("create_zip_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AmberAccent,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Archive, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isArchiving) "BUILDING ARCHIVE..." else "BUNDLE & SAVE ZIP",
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

@Composable
private fun ArchiveItemCard(
    item: ArchiveItem,
    onRemove: () -> Unit
) {
    OmniCard(borderColor = BorderSubtle, backgroundColor = SurfaceElevated) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Slate800),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.FolderZip, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(24.dp))
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
                OmniBadge(text = "${item.sizeBytes / 1024} KB", color = Slate400)
            }

            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = RoseAccent, modifier = Modifier.size(18.dp))
            }
        }
    }
}
