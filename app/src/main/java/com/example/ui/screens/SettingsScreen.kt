package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CanvasBorder
import com.example.ui.theme.CanvasPrimary
import com.example.ui.theme.CanvasPrimaryDeep
import com.example.ui.theme.CanvasSuccess

@Composable
fun SettingsScreen(
    apiKey: String,
    baseUrl: String,
    isLoading: Boolean,
    healthMessage: String?,
    healthOk: Boolean?,
    credits: Double?,
    isDemoMode: Boolean,
    onApiKeyChanged: (String) -> Unit,
    onBaseUrlChanged: (String) -> Unit,
    onSaveConfig: () -> Unit,
    onTestConnection: () -> Unit,
    onClearApiKey: () -> Unit,
    onClearCache: () -> Unit
) {
    var apiKeyVisible by remember { mutableStateOf(false) }
    var showClearCacheConfirmation by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("settings_screen")
    ) {
        Text("Cloud API", style = MaterialTheme.typography.titleLarge)
        Text(
            "Connect this app to your OpenHands Cloud account. Keys are stored on-device only.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
        )

        OutlinedTextField(
            value = apiKey,
            onValueChange = onApiKeyChanged,
            label = { Text("API key") },
            placeholder = { Text("Paste your OpenHands Cloud key") },
            singleLine = true,
            visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                    Icon(
                        imageVector = if (apiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (apiKeyVisible) "Hide API key" else "Show API key"
                    )
                }
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CanvasPrimary,
                unfocusedBorderColor = CanvasBorder
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("api_key_input")
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = baseUrl,
            onValueChange = onBaseUrlChanged,
            label = { Text("Base URL") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CanvasPrimary,
                unfocusedBorderColor = CanvasBorder
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("base_url_input")
        )

        Spacer(Modifier.height(12.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onSaveConfig,
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = CanvasPrimaryDeep),
                modifier = Modifier.testTag("btn_save_config")
            ) {
                Text("Save")
            }
            OutlinedButton(
                onClick = onTestConnection,
                enabled = !isLoading,
                modifier = Modifier.testTag("btn_test_connection")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(6.dp))
                }
                Text("Test connection")
            }
        }

        if (!isDemoMode) {
            TextButton(
                onClick = onClearApiKey,
                modifier = Modifier
                    .align(Alignment.End)
                    .testTag("btn_clear_api_key")
            ) {
                Text("Clear API key", color = MaterialTheme.colorScheme.error)
            }
        }

        // Health status card
        healthMessage?.let { message ->
            Spacer(Modifier.height(12.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = when (healthOk) {
                        true -> CanvasSuccess.copy(alpha = 0.1f)
                        false -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                        null -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(12.dp)
                ) {
                    Icon(
                        imageVector = when (healthOk) {
                            true -> Icons.Default.CheckCircle
                            false -> Icons.Default.ErrorOutline
                            null -> Icons.Default.Cloud
                        },
                        contentDescription = null,
                        tint = when (healthOk) {
                            true -> CanvasSuccess
                            false -> MaterialTheme.colorScheme.error
                            null -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(message, fontSize = 13.sp)
                }
            }
        }

        credits?.let { creditsValue ->
            Spacer(Modifier.height(8.dp))
            Text(
                "Cloud credits: $${"%.2f".format(creditsValue)}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(20.dp))
        HorizontalDivider(color = CanvasBorder.copy(alpha = 0.5f))
        Spacer(Modifier.height(16.dp))

        Text("Local data", style = MaterialTheme.typography.titleMedium)
        Text(
            if (isDemoMode) "Demo mode is active — sample conversations are shown."
            else "Cached conversations and events live on this device.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = { showClearCacheConfirmation = true },
            modifier = Modifier.testTag("btn_clear_cache")
        ) {
            Text("Clear local data")
        }

        Spacer(Modifier.height(24.dp))
        Text(
            "OpenHands Canvas for Android · v2.0",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    if (showClearCacheConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearCacheConfirmation = false },
            title = { Text("Clear local data?") },
            text = { Text("Cached conversations and event transcripts will be removed from this device. Demo data can be restored afterward.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearCacheConfirmation = false
                        onClearCache()
                    },
                    modifier = Modifier.testTag("btn_confirm_clear_cache")
                ) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
