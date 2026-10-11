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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
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
import com.example.data.model.LlmProfileDto
import com.example.data.model.SecretDto
import com.example.data.model.UserInfoDto
import com.example.data.settings.AgentProfile
import com.example.ui.components.AgentProfileDialog
import com.example.ui.components.ProfileSummaryCard
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
    activeProfile: AgentProfile,
    account: UserInfoDto? = null,
    cloudProfiles: List<LlmProfileDto> = emptyList(),
    activeCloudProfile: String? = null,
    secrets: List<SecretDto> = emptyList(),
    isCloudSettingsLoading: Boolean = false,
    availableModels: List<String> = emptyList(),
    onApiKeyChanged: (String) -> Unit,
    onBaseUrlChanged: (String) -> Unit,
    onSaveConfig: () -> Unit,
    onTestConnection: () -> Unit,
    onClearApiKey: () -> Unit,
    onClearCache: () -> Unit,
    onModelChanged: (String) -> Unit,
    onRefreshCloudSettings: () -> Unit = {},
    onActivateCloudProfile: (String) -> Unit = {},
    onDeleteCloudProfile: (String) -> Unit = {},
    onRenameCloudProfile: (String, String) -> Unit = { _, _ -> },
    onAddSecret: (String, String, String?) -> Unit = { _, _, _ -> },
    onDeleteSecret: (String) -> Unit = {}
) {
    var apiKeyVisible by remember { mutableStateOf(false) }
    var showClearCacheConfirmation by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var renamingProfile by remember { mutableStateOf<String?>(null) }
    var deletingProfile by remember { mutableStateOf<String?>(null) }
    var deletingSecret by remember { mutableStateOf<String?>(null) }
    var showAddSecretDialog by remember { mutableStateOf(false) }

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

        if (!isDemoMode) {
            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = CanvasBorder.copy(alpha = 0.5f))
            Spacer(Modifier.height(16.dp))

            // ---- Account ------------------------------------------------------
            account?.let { info ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("account_section")
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = CanvasSuccess,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Signed in as ${info.email ?: "unknown"}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        info.orgName?.let { org ->
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Organization: $org${info.role?.let { " · $it" } ?: ""}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // ---- Cloud profiles --------------------------------------------------
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cloud profiles", style = MaterialTheme.typography.titleMedium)
                Row {
                    if (isCloudSettingsLoading) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    IconButton(
                        onClick = onRefreshCloudSettings,
                        modifier = Modifier.testTag("btn_refresh_cloud")
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh cloud settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Text(
                "Server-side LLM profiles. The active one is used for new cloud conversations.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
            )
            if (cloudProfiles.isEmpty()) {
                Text(
                    "No cloud profiles yet — create one in the OpenHands web app.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("cloud_profiles_section")
                )
            } else {
                cloudProfiles.forEach { profile ->
                    val isActive = profile.name == activeCloudProfile
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isActive) CanvasSuccess.copy(alpha = 0.08f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .testTag("profile_row_${profile.name}")
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    profile.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isActive) {
                                    Text(
                                        "ACTIVE",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CanvasSuccess,
                                        modifier = Modifier.testTag("active_profile_badge")
                                    )
                                }
                            }
                            profile.model?.let { model ->
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    model,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            profile.baseUrl?.takeIf { it.isNotBlank() }?.let { url ->
                                Text(
                                    url,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (!isActive) {
                                    TextButton(
                                        onClick = { onActivateCloudProfile(profile.name) },
                                        modifier = Modifier.testTag("btn_activate_profile_${profile.name}")
                                    ) {
                                        Text("Activate", color = CanvasPrimaryDeep)
                                    }
                                }
                                IconButton(
                                    onClick = { renamingProfile = profile.name },
                                    modifier = Modifier.testTag("btn_rename_profile_${profile.name}")
                                ) {
                                    Icon(
                                        Icons.Default.DriveFileRenameOutline,
                                        contentDescription = "Rename profile",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { deletingProfile = profile.name },
                                    modifier = Modifier.testTag("btn_delete_profile_${profile.name}")
                                ) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = "Delete profile",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ---- Secrets ---------------------------------------------------------
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Secrets", style = MaterialTheme.typography.titleMedium)
                OutlinedButton(
                    onClick = { showAddSecretDialog = true },
                    modifier = Modifier.testTag("btn_add_secret")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add secret")
                }
            }
            Text(
                "Reusable values the agent can reference without seeing them in chat.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
            )
            if (secrets.isEmpty()) {
                Text(
                    "No secrets yet.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("secrets_section")
                )
            } else {
                secrets.forEach { secret ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .testTag("secret_row_${secret.name}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(secret.name, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                secret.description?.let { desc ->
                                    Text(
                                        desc,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(
                                onClick = { deletingSecret = secret.name },
                                modifier = Modifier.testTag("btn_delete_secret_${secret.name}")
                            ) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "Delete secret",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        HorizontalDivider(color = CanvasBorder.copy(alpha = 0.5f))
        Spacer(Modifier.height(16.dp))

        Text("Agent & LLM", style = MaterialTheme.typography.titleMedium)
        Text(
            "Default agent backend and model for new conversations. You can switch at launch time or mid-session per conversation.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
        Spacer(Modifier.height(10.dp))
        ProfileSummaryCard(
            profile = activeProfile,
            onEdit = { showProfileDialog = true }
        )
        Spacer(Modifier.height(12.dp))

        Text("Local data", style = MaterialTheme.typography.titleMedium)
        Text(
            if (isDemoMode) "Not connected — cached conversations are shown from this device."
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
            text = { Text("Cached conversations and event transcripts will be removed from this device. Cloud conversations remain on the server.") },
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

    if (showProfileDialog) {
        AgentProfileDialog(
            profile = activeProfile,
            onDismiss = { showProfileDialog = false },
            onModelChanged = onModelChanged,
            title = "Agent & LLM",
            availableModels = availableModels
        )
    }

    renamingProfile?.let { name ->
        RenameProfileDialog(
            currentName = name,
            onDismiss = { renamingProfile = null },
            onConfirm = { newName ->
                renamingProfile = null
                onRenameCloudProfile(name, newName)
            }
        )
    }

    deletingProfile?.let { name ->
        AlertDialog(
            onDismissRequest = { deletingProfile = null },
            title = { Text("Delete cloud profile?") },
            text = { Text("'$name' will be removed from your account. This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        deletingProfile = null
                        onDeleteCloudProfile(name)
                    },
                    modifier = Modifier.testTag("btn_confirm_delete_profile")
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingProfile = null }) { Text("Cancel") }
            }
        )
    }

    deletingSecret?.let { name ->
        AlertDialog(
            onDismissRequest = { deletingSecret = null },
            title = { Text("Delete secret?") },
            text = { Text("'$name' will be removed from your account. This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        deletingSecret = null
                        onDeleteSecret(name)
                    },
                    modifier = Modifier.testTag("btn_confirm_delete_secret")
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingSecret = null }) { Text("Cancel") }
            }
        )
    }

    if (showAddSecretDialog) {
        AddSecretDialog(
            onDismiss = { showAddSecretDialog = false },
            onCreate = { name, value, description ->
                showAddSecretDialog = false
                onAddSecret(name, value, description)
            }
        )
    }
}

@Composable
private fun RenameProfileDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newName by remember(currentName) { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename profile") },
        text = {
            Column {
                Text(
                    "'$currentName' — letters, digits, dots, dashes and underscores.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("New name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CanvasPrimary,
                        unfocusedBorderColor = CanvasBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("field_rename_profile")
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(newName) },
                enabled = newName.isNotBlank(),
                modifier = Modifier.testTag("btn_confirm_rename")
            ) {
                Text("Rename", color = CanvasPrimaryDeep)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun AddSecretDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var valueVisible by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add secret") },
        text = {
            Column {
                Text(
                    "The value is stored server-side; the agent only sees the name.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CanvasPrimary,
                        unfocusedBorderColor = CanvasBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("field_secret_name")
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("Value") },
                    singleLine = true,
                    visualTransformation = if (valueVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { valueVisible = !valueVisible }) {
                            Icon(
                                imageVector = if (valueVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (valueVisible) "Hide secret value" else "Show secret value"
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
                        .testTag("field_secret_value")
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CanvasPrimary,
                        unfocusedBorderColor = CanvasBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("field_secret_description")
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(name, value, description) },
                enabled = name.isNotBlank() && value.isNotBlank(),
                modifier = Modifier.testTag("btn_confirm_add_secret")
            ) {
                Text("Add", color = CanvasPrimaryDeep)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
