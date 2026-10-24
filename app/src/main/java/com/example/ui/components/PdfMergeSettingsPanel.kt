package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.PdfMergeSettings
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.BorderCrisp
import com.example.ui.theme.CardSurface
import com.example.ui.theme.PdfGreen
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate800

@Composable
fun PdfMergeSettingsPanel(
    settings: PdfMergeSettings,
    onSettingsChange: (PdfMergeSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    OmniCard(
        borderColor = BorderCrisp,
        backgroundColor = CardSurface,
        modifier = modifier
    ) {
        Column {
            Text(
                text = "MERGE CONFIGURATION",
                style = MaterialTheme.typography.labelSmall,
                color = Slate400
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Output filename with explicit lambda type
            OutlinedTextField(
                value = settings.outputFileName,
                onValueChange = { newValue: String ->
                    onSettingsChange(settings.copy(outputFileName = newValue))
                },
                label = { Text("Output PDF Name") },
                placeholder = { Text("Merged_Document") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("merge_filename_input")
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Password Encryption toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "PDF Password Protection",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Encrypt the merged document with a 128-bit security password.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate400
                    )
                }
                Switch(
                    checked = settings.enableEncryption,
                    onCheckedChange = { isChecked: Boolean ->
                        onSettingsChange(settings.copy(enableEncryption = isChecked))
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = AccentCyan,
                        checkedTrackColor = Slate800
                    ),
                    modifier = Modifier.testTag("encryption_switch")
                )
            }

            if (settings.enableEncryption) {
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = settings.userPassword,
                    onValueChange = { newValue: String ->
                        onSettingsChange(settings.copy(userPassword = newValue))
                    },
                    label = { Text("User Open Password") },
                    placeholder = { Text("Enter secret password") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("user_password_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = settings.ownerPassword,
                    onValueChange = { newValue: String ->
                        onSettingsChange(settings.copy(ownerPassword = newValue))
                    },
                    label = { Text("Permissions Owner Password (Optional)") },
                    placeholder = { Text("Optional owner permissions key") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("owner_password_input")
                )
            }
        }
    }
}
