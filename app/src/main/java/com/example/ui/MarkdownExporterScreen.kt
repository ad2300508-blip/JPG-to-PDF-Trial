package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.CompiledMarkdownResult
import com.example.model.LlmPromptWrapper
import com.example.model.MarkdownExportSettings
import com.example.model.MarkdownSourceItem
import com.example.model.SourceFileType
import com.example.ui.theme.BadgeAud
import com.example.ui.theme.BadgeAudBg
import com.example.ui.theme.BadgeImg
import com.example.ui.theme.BadgeImgBg
import com.example.ui.theme.BadgeMd
import com.example.ui.theme.BadgeMdBg
import com.example.ui.theme.BadgePdf
import com.example.ui.theme.BadgePdfBg
import com.example.ui.theme.BorderCrisp
import com.example.ui.theme.CardSurface
import com.example.ui.theme.CodeTextStyle
import com.example.ui.theme.PanelElevated
import com.example.ui.theme.SuccessGreen
import com.example.viewmodel.MarkdownExporterViewModel

@Composable
fun MarkdownExporterScreen(
  viewModel: MarkdownExporterViewModel,
  modifier: Modifier = Modifier
) {
  val items by viewModel.items.collectAsStateWithLifecycle()
  val settings by viewModel.settings.collectAsStateWithLifecycle()
  val isExtracting by viewModel.isExtracting.collectAsStateWithLifecycle()
  val compiledResult by viewModel.compiledResult.collectAsStateWithLifecycle()
  val clipboardMessage by viewModel.clipboardMessage.collectAsStateWithLifecycle()
  val exportMessage by viewModel.exportMessage.collectAsStateWithLifecycle()

  val pickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenMultipleDocuments()
  ) { uris ->
    if (uris.isNotEmpty()) {
      viewModel.addUris(uris)
    }
  }

  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(clipboardMessage) {
    clipboardMessage?.let {
      snackbarHostState.showSnackbar(it)
      viewModel.clearClipboardMessage()
    }
  }

  LaunchedEffect(exportMessage) {
    exportMessage?.let {
      snackbarHostState.showSnackbar(it)
      viewModel.clearExportMessage()
    }
  }

  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val isTabletLandscape = maxWidth >= 840.dp

    if (isTabletLandscape) {
      // Bespoke Asymmetric Dual-Pane Layout: 38% Left Source List, 62% Right AI Prompt Engine
      Row(modifier = Modifier.fillMaxSize()) {
        Box(
          modifier = Modifier
            .weight(0.38f)
            .fillMaxHeight()
            .padding(start = 16.dp, top = 8.dp, bottom = 16.dp, end = 8.dp)
        ) {
          MarkdownSourceListPane(
            items = items,
            isExtracting = isExtracting,
            onPickSources = { pickerLauncher.launch(arrayOf("*/*")) },
            onRemoveItem = { viewModel.removeItem(it) },
            onClearAll = { viewModel.clearAll() }
          )
        }

        Box(
          modifier = Modifier
            .weight(0.62f)
            .fillMaxHeight()
            .padding(start = 8.dp, top = 8.dp, bottom = 16.dp, end = 16.dp)
        ) {
          MarkdownCompiledPane(
            items = items,
            settings = settings,
            compiledResult = compiledResult,
            isExtracting = isExtracting,
            onSettingsUpdate = { viewModel.updateSettings(it) },
            onCopyToClipboard = { viewModel.copyToClipboard() },
            onExportMarkdown = { viewModel.exportToDownloads() }
          )
        }
      }
    } else {
      // Mobile Single-Pane Column with Vertical Scroll
      Column(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        MarkdownCompiledPane(
          items = items,
          settings = settings,
          compiledResult = compiledResult,
          isExtracting = isExtracting,
          onSettingsUpdate = { viewModel.updateSettings(it) },
          onCopyToClipboard = { viewModel.copyToClipboard() },
          onExportMarkdown = { viewModel.exportToDownloads() }
        )

        MarkdownSourceListPane(
          items = items,
          isExtracting = isExtracting,
          onPickSources = { pickerLauncher.launch(arrayOf("*/*")) },
          onRemoveItem = { viewModel.removeItem(it) },
          onClearAll = { viewModel.clearAll() }
        )
      }
    }

    Box(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(bottom = 16.dp)
    ) {
      SnackbarHost(hostState = snackbarHostState)
    }
  }
}

@Composable
private fun MarkdownSourceListPane(
  items: List<MarkdownSourceItem>,
  isExtracting: Boolean,
  onPickSources: () -> Unit,
  onRemoveItem: (String) -> Unit,
  onClearAll: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    color = CardSurface,
    shape = RoundedCornerShape(20.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, BorderCrisp),
    modifier = modifier.fillMaxSize()
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)
    ) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "SOURCE CONTEXT",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "Ingested Files (${items.size})",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
          )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          if (items.isNotEmpty()) {
            OutlinedButton(
              onClick = onClearAll,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.testTag("md_clear_all")
            ) {
              Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Clear", style = MaterialTheme.typography.labelMedium)
            }
          }
          Button(
            onClick = onPickSources,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.testTag("md_add_sources")
          ) {
            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (items.isEmpty()) "Import Files" else "Add More", style = MaterialTheme.typography.labelMedium)
          }
        }
      }

      if (isExtracting) {
        Spacer(modifier = Modifier.height(12.dp))
        LinearProgressIndicator(
          modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      if (items.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          contentAlignment = Alignment.Center
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
          ) {
            Box(
              modifier = Modifier
                .size(72.dp)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Psychology,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
              )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
              text = "Multi-Source AI Context",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
              text = "Import PDFs, screenshots (OCR), code files, and notes. The engine structures them into high-density Markdown ready for LLM prompts.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
              onClick = onPickSources,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.testTag("md_empty_import_button")
            ) {
              Icon(Icons.Default.FolderOpen, contentDescription = null)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Select Documents & Media")
            }
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          items(items, key = { it.id }) { item ->
            MarkdownSourceItemCard(
              item = item,
              onRemove = { onRemoveItem(item.id) }
            )
          }
        }
      }
    }
  }
}

@Composable
private fun MarkdownSourceItemCard(
  item: MarkdownSourceItem,
  onRemove: () -> Unit
) {
  val (badgeBg, badgeColor) = when (item.fileType) {
    SourceFileType.PDF -> BadgePdfBg to BadgePdf
    SourceFileType.IMAGE_OCR -> BadgeImgBg to BadgeImg
    SourceFileType.AUDIO_NOTE -> BadgeAudBg to BadgeAud
    SourceFileType.CODE_FILE -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.primary
    SourceFileType.TEXT_DOCUMENT -> BadgeMdBg to BadgeMd
  }

  Surface(
    color = PanelElevated,
    shape = RoundedCornerShape(12.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, BorderCrisp),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("md_source_item_${item.id}")
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Type Badge
      Surface(
        color = badgeBg,
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f))
      ) {
        Text(
          text = item.fileType.badgeText,
          style = MaterialTheme.typography.labelSmall,
          color = badgeColor,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = item.fileName,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = item.displaySize,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = " • ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = "~${item.tokenEstimate} TOKENS",
            style = MaterialTheme.typography.labelSmall,
            color = SuccessGreen,
            fontWeight = FontWeight.Bold
          )
        }
      }

      IconButton(
        onClick = onRemove,
        modifier = Modifier.size(32.dp).testTag("md_remove_source_${item.id}")
      ) {
        Icon(
          imageVector = Icons.Default.Close,
          contentDescription = "Remove",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(16.dp)
        )
      }
    }
  }
}

@Composable
private fun MarkdownCompiledPane(
  items: List<MarkdownSourceItem>,
  settings: MarkdownExportSettings,
  compiledResult: CompiledMarkdownResult?,
  isExtracting: Boolean,
  onSettingsUpdate: ((MarkdownExportSettings) -> MarkdownExportSettings) -> Unit,
  onCopyToClipboard: () -> Unit,
  onExportMarkdown: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    color = CardSurface,
    shape = RoundedCornerShape(20.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, BorderCrisp),
    modifier = modifier.fillMaxSize()
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(20.dp)
    ) {
      // Top Controls: Bundle Title & Stats
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "LLM EXPORTER ENGINE",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "Compiled AI Markdown",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
          )
        }

        if (compiledResult != null) {
          Surface(
            color = BadgeMdBg,
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BadgeMd.copy(alpha = 0.5f))
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = BadgeMd,
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "~${compiledResult.estimatedTokens} TOKENS • ${compiledResult.totalWordCount} WORDS",
                style = MaterialTheme.typography.labelSmall,
                color = BadgeMd,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // LLM Prompt Wrapper Tabs
      Text(
        text = "PROMPT FORMATTING WRAPPER",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Spacer(modifier = Modifier.height(6.dp))

      TabRow(
        selectedTabIndex = settings.llmWrapper.ordinal,
        containerColor = PanelElevated,
        modifier = Modifier.clip(RoundedCornerShape(12.dp))
      ) {
        LlmPromptWrapper.values().forEach { wrapper ->
          Tab(
            selected = settings.llmWrapper == wrapper,
            onClick = { onSettingsUpdate { it.copy(llmWrapper = wrapper) } },
            text = {
              Text(
                text = wrapper.displayName,
                fontSize = 11.sp,
                fontWeight = if (settings.llmWrapper == wrapper) FontWeight.Bold else FontWeight.Medium
              )
            }
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Markdown Preview Canvas (Monospace with JetBrains Mono)
      Surface(
        color = Color(0xFF070B12),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderCrisp),
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
      ) {
        if (compiledResult == null || compiledResult.compiledMarkdown.isBlank()) {
          Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = if (isExtracting) "Extracting & structuring document intelligence..." else "Add documents on the left to generate clean, prompt-ready Markdown",
              style = CodeTextStyle,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        } else {
          SelectionContainer(modifier = Modifier.fillMaxSize()) {
            Box(
              modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .horizontalScroll(rememberScrollState())
                .padding(16.dp)
            ) {
              Text(
                text = compiledResult.compiledMarkdown,
                style = CodeTextStyle,
                color = Color(0xFFE2E8F0)
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Action Bar: Copy to Clipboard & Save to .md
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Button(
          onClick = onCopyToClipboard,
          enabled = compiledResult != null && compiledResult.compiledMarkdown.isNotBlank(),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .weight(1.3f)
            .height(50.dp)
            .testTag("md_copy_clipboard_button")
        ) {
          Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Copy to Clipboard (LLM Prompt)",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
          )
        }

        OutlinedButton(
          onClick = onExportMarkdown,
          enabled = compiledResult != null && compiledResult.compiledMarkdown.isNotBlank(),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .weight(1f)
            .height(50.dp)
            .testTag("md_export_file_button")
        ) {
          Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Save as .md",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
          )
        }
      }
    }
  }
}
