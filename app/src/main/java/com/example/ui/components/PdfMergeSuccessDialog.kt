package com.example.ui.components

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Pages
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.ui.theme.PdfGreen
import com.example.util.DownloadUtils
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfMergeSuccessDialog(
  mergedFile: File,
  totalPages: Int,
  fileSizeBytes: Long,
  onDismiss: () -> Unit,
  onShowSnackbar: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  val contentUri: Uri = try {
    val authority = "${context.packageName}.fileprovider"
    FileProvider.getUriForFile(context, authority, mergedFile)
  } catch (_: Exception) {
    Uri.fromFile(mergedFile)
  }

  val formattedSize = remember(fileSizeBytes) {
    val kb = fileSizeBytes / 1024.0
    val mb = kb / 1024.0
    when {
      mb >= 1.0 -> String.format(Locale.US, "%.2f MB", mb)
      kb >= 1.0 -> String.format(Locale.US, "%.0f KB", kb)
      else -> "$fileSizeBytes B"
    }
  }

  BasicAlertDialog(
    onDismissRequest = onDismiss,
    modifier = modifier.testTag("pdf_merge_success_dialog")
  ) {
    Card(
      shape = RoundedCornerShape(28.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface
      ),
      elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        // Success Icon
        Box(
          modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(PdfGreen.copy(alpha = 0.15f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = PdfGreen,
            modifier = Modifier.size(38.dp)
          )
        }

        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Text(
            text = "PDFs Merged Successfully!",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "All selected documents have been compiled into a single file",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        // Summary Card
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Text(
              text = mergedFile.name,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Pages,
                  contentDescription = null,
                  modifier = Modifier.size(15.dp),
                  tint = MaterialTheme.colorScheme.primary
                )
                Text(
                  text = "$totalPages Pages",
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = FontWeight.Medium
                )
              }

              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.SdCard,
                  contentDescription = null,
                  modifier = Modifier.size(15.dp),
                  tint = MaterialTheme.colorScheme.primary
                )
                Text(
                  text = formattedSize,
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = FontWeight.Medium
                )
              }
            }
          }
        }

        // Action Buttons
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          // Open PDF Button
          Button(
            onClick = {
              val opened = DownloadUtils.openPdf(context, contentUri)
              if (!opened) {
                onShowSnackbar("No installed app found to open PDF.")
              }
            },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("open_merged_pdf_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.OpenInNew,
              contentDescription = null,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Open Merged PDF", fontWeight = FontWeight.Bold)
          }

          // Share PDF Button
          FilledTonalButton(
            onClick = {
              DownloadUtils.sharePdf(context, contentUri, "Share Merged PDF")
            },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("share_merged_pdf_button")
          ) {
            Icon(
              imageVector = Icons.Default.Share,
              contentDescription = null,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Share PDF", fontWeight = FontWeight.SemiBold)
          }

          // Save to Downloads Button
          OutlinedButton(
            onClick = {
              scope.launch {
                val res = DownloadUtils.savePdfToDownloads(context, mergedFile, mergedFile.name)
                res.onSuccess { dl ->
                  onShowSnackbar("Saved to ${dl.displayPath}")
                }.onFailure { err ->
                  onShowSnackbar("Failed to save: ${err.message}")
                }
              }
            },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("download_merged_pdf_button")
          ) {
            Icon(
              imageVector = Icons.Default.Download,
              contentDescription = null,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save to Downloads Folder", fontWeight = FontWeight.SemiBold)
          }

          TextButton(
            onClick = onDismiss,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("dismiss_merged_dialog_button")
          ) {
            Text("Done")
          }
        }
      }
    }
  }
}
