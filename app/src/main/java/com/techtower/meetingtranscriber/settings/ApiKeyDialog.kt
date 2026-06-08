package com.techtower.meetingtranscriber.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun ApiKeyDialog(
    initialValue: String,
    validation: ApiKeyValidationSnapshot,
    onDismiss: () -> Unit,
    onSaveAndTest: (String) -> Unit,
    onTestSavedKey: () -> Unit,
) {
    var apiKey by remember(initialValue) { mutableStateOf(initialValue) }
    val isChecking = validation.status == ApiKeyValidationStatus.CHECKING
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("OpenRouter API key") },
        text = {
            Column {
                Text("Paste your key to transcribe audio directly from this phone.")
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    singleLine = true,
                    label = { Text("API key") },
                    visualTransformation = PasswordVisualTransformation(),
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (isChecking) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp))
                }
                validation.message?.let { message ->
                    Text(
                        text = message,
                        color = when (validation.status) {
                            ApiKeyValidationStatus.VALID -> MaterialTheme.colorScheme.primary
                            ApiKeyValidationStatus.INVALID -> MaterialTheme.colorScheme.error
                            ApiKeyValidationStatus.CHECKING -> MaterialTheme.colorScheme.onSurfaceVariant
                            ApiKeyValidationStatus.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
                validation.label?.let { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = apiKey.isNotBlank() && !isChecking,
                onClick = { onSaveAndTest(apiKey) },
            ) {
                Text("Save & test")
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    enabled = !isChecking,
                    onClick = {
                        if (apiKey == initialValue) {
                            onTestSavedKey()
                        } else {
                            onSaveAndTest(apiKey)
                        }
                    },
                ) {
                    Text("Test")
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(
                    enabled = !isChecking,
                    onClick = onDismiss,
                ) {
                    Text("Cancel")
                }
            }
        },
    )
}
