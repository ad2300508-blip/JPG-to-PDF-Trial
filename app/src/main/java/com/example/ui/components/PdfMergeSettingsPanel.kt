package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMerge
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Pages
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.model.PdfMergeSettings

@Composable
fun PdfMergeSettingsPanel(
  settings: PdfMergeSettings,
  onSettingsChanged: ((PdfMergeSettings) -> PdfMergeSettings) -> Unit,
  documentCount: Int,
  totalCombinedPages: Int,
  formattedInputSize: String,
  onSortByName: (Boolean) -> Unit,
  onSortBySize: (Boolean) -> Unit,
  onSortByPageCount: (Boolean) -> Unit,
  onMergeClick: () -> Unit,
  isMerging: Boolean,
  modifier: Modifier = Modifier,
) {
  var showSortMenu by remember { mutableStateOf(false) }
  var showUserPassword by remember { mutableStateOf(false) }
  val scrollState = rememberScrollState()

  Column(
    modifier = modifier
      .fillMaxWidth()
      .verticalScroll(scrollState)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Header
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Merge Configuration",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )

      Box {
        FilledTonalButton(
          onClick = { showSortMenu = true },
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.testTag("sort_menu_button")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.Sort,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text("Sort")
        }

        DropdownMenu(
          expanded = showSortMenu,
          onDismissRequest = { showSortMenu = false }
        ) {
          DropdownMenuItem(
            text = { Text("Sort by Name (A → Z)") },
            onClick = {
              onSortByName(true)
              showSortMenu = false
            }
          )
          DropdownMenuItem(
            text = { Text("Sort by Name (Z → A)") },
            onClick = {
              onSortByName(false)
              showSortMenu = false
            }
          )
          DropdownMenuItem(
            text = { Text("Sort by Page Count (Largest first)") },
            onClick = {
              onSortByPageCount(true)
              showSortMenu = false
            }
          )
          DropdownMenuItem(
            text = { Text("Sort by File Size (Largest first)") },
            onClick = {
              onSortBySize(true)
              showSortMenu = false
            }
          )
        }
      }
    }

    // Statistics Summary Card
    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
      ),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = "$documentCount",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
          )
          Text(
            text = "PDFs",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Box(
          modifier = Modifier
            .width(1.dp)
            .height(36.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = "$totalCombinedPages",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
          )
          Text(
            text = "Total Pages",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Box(
          modifier = Modifier
            .width(1.dp)
            .height(36.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = formattedInputSize,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
          )
          Text(
            text = "Input Size",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }

    // Output File Name
    OutlinedTextField(
      value = settings.outputFileName,
      onValueChange = { name ->
        onSettingsChanged { it.copy(outputFileName = name) }
      },
      label = { Text("Output PDF Filename") },
      placeholder = { Text("Merged_Document.pdf") },
      leadingIcon = {
        Icon(imageVector = Icons.Default.Description, contentDescription = null)
      },
      singleLine = true,
      modifier = Modifier
        .fillMaxWidth()
        .testTag("output_filename_input")
    )

    // Enterprise Security Card
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
      ),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
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
              shape = RoundedCornerShape(8.dp),
              color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
              modifier = Modifier.size(32.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.Security,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(18.dp)
                )
              }
            }

            Column {
              Text(
                text = "Password Protection",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Encrypt merged PDF (128-bit AES)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          Switch(
            checked = settings.enableEncryption,
            onCheckedChange = { checked ->
              onSettingsChanged { it.copy(enableEncryption = checked) }
            },
            modifier = Modifier.testTag("encrypt_pdf_switch")
          )
        }

        AnimatedVisibility(visible = settings.enableEncryption) {
          Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(top = 6.dp)
          ) {
            OutlinedTextField(
              value = settings.userPassword,
              onValueChange = { pwd ->
                onSettingsChanged { it.copy(userPassword = pwd) }
              },
              label = { Text("Open Password (User)") },
              leadingIcon = {
                Icon(imageVector = Icons.Default.Lock, contentDescription = null)
              },
              trailingIcon = {
                IconButton(onClick = { showUserPassword = !showUserPassword }) {
                  Icon(
                    imageVector = if (showUserPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = "Toggle password visibility"
                  )
                }
              },
              visualTransformation = if (showUserPassword) VisualTransformation.None else PasswordVisualTransformation(),
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("user_password_input")
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Primary Merge Button
    Button(
      onClick = onMergeClick,
      enabled = documentCount >= 2 && !isMerging && settings.hasValidPassword,
      shape = RoundedCornerShape(16.dp),
      colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
      ),
      modifier = Modifier
        .fillMaxWidth()
        .height(56.dp)
        .testTag("start_merge_button")
    ) {
      if (isMerging) {
        CircularProgressIndicator(
          color = MaterialTheme.colorScheme.onPrimary,
          modifier = Modifier.size(24.dp),
          strokeWidth = 2.5.dp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
          text = "Merging Documents...",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )
      } else {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.CallMerge,
          contentDescription = null,
          modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = if (documentCount < 2) "Add at least 2 PDFs" else "Merge $documentCount PDFs into One",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )
      }
    }
  }
}
