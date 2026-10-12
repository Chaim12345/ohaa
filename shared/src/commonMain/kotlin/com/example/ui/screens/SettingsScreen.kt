package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LlmProfileDto
import com.example.data.model.SecretDto
import com.example.data.model.UserInfoDto
import com.example.data.settings.AgentProfile
import com.example.ui.components.AgentProfileDialog
import com.example.ui.components.CanvasCard
import com.example.ui.components.CanvasDivider
import com.example.ui.components.InlineNotice
import com.example.ui.components.KeyValueRow
import com.example.ui.components.MetaChip
import com.example.ui.components.SectionHeader
import com.example.ui.theme.CanvasBorder
import com.example.ui.theme.CanvasColors
import com.example.ui.theme.CanvasPrimary
import com.example.ui.theme.CanvasPrimaryDeep
import com.example.ui.theme.Shapes
import com.example.ui.theme.Sizes
import com.example.ui.theme.Spacing
import com.example.util.formatDecimals

/**
 * Settings.
 *
 * Rebuilt as a single scrolling column of clearly-separated cards instead of
 * the previous ad-hoc mix of section types. Order reflects what a user does:
 * connect first, then manage what the connection gives them (model, profiles,
 * secrets), then local data last.
 *
 * All existing test tags are preserved so the regression suite still covers the
 * same behaviours.
 */
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
    var showClearCacheConfirm by remember { mutableStateOf(false) }
    var showModelDialog by remember { mutableStateOf(false) }
    var renamingProfile by remember { mutableStateOf<String?>(null) }
    var deletingProfile by remember { mutableStateOf<String?>(null) }
    var deletingSecret by remember { mutableStateOf<String?>(null) }
    var showAddSecret by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("settings_screen"),
        contentPadding = PaddingValues(Spacing.screen),
        verticalArrangement = Arrangement.spacedBy(Spacing.cardGap)
    ) {
        item { Text("Settings", style = MaterialTheme.typography.titleLarge) }

        // ---------------------------------------------------------- connection
        item {
            CanvasCard {
                SectionHeader(
                    title = "Connection",
                    subtitle = if (isDemoMode) "Not connected" else "OpenHands Cloud",
                    trailing = {
                        if (isLoading) {
                            CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                        }
                    }
                )
                Spacer(Modifier.height(Spacing.md))
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = onApiKeyChanged,
                    label = { Text("API key") },
                    singleLine = true,
                    visualTransformation = if (apiKeyVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                            Icon(
                                if (apiKeyVisible) Icons.Default.VisibilityOff
                                else Icons.Default.Visibility,
                                contentDescription = if (apiKeyVisible) "Hide key" else "Show key"
                            )
                        }
                    },
                    shape = Shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CanvasPrimary,
                        unfocusedBorderColor = CanvasBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("api_key_input")
                )
                Spacer(Modifier.height(Spacing.sm))
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = onBaseUrlChanged,
                    label = { Text("Base URL") },
                    singleLine = true,
                    shape = Shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CanvasPrimary,
                        unfocusedBorderColor = CanvasBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("base_url_input")
                )
                Spacer(Modifier.height(Spacing.md))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Button(
                        onClick = onSaveConfig,
                        shape = Shapes.medium,
                        colors = ButtonDefaults.buttonColors(containerColor = CanvasPrimaryDeep),
                        modifier = Modifier
                            .weight(1f)
                            .height(Sizes.touchTarget)
                            .testTag("btn_save_config")
                    ) { Text("Save") }
                    OutlinedButton(
                        onClick = onTestConnection,
                        enabled = !isLoading,
                        shape = Shapes.medium,
                        modifier = Modifier
                            .weight(1f)
                            .height(Sizes.touchTarget)
                            .testTag("btn_test_connection")
                    ) { Text("Test") }
                }

                healthMessage?.let { msg ->
                    Spacer(Modifier.height(Spacing.md))
                    InlineNotice(
                        message = msg,
                        isError = healthOk == false,
                        actionLabel = null
                    )
                }

                credits?.let {
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        "Credits: $${it.formatDecimals(2)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!isDemoMode) {
                    Spacer(Modifier.height(Spacing.sm))
                    TextButton(
                        onClick = onClearApiKey,
                        modifier = Modifier.testTag("btn_clear_api_key")
                    ) { Text("Clear API key", color = CanvasColors.danger) }
                }
            }
        }

        // ------------------------------------------------------------- account
        if (!isDemoMode) {
            account?.let { info ->
                item {
                    CanvasCard(modifier = Modifier.testTag("account_section")) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = CanvasColors.success,
                                modifier = Modifier.size(Sizes.iconSmall)
                            )
                            Spacer(Modifier.width(Spacing.xs))
                            Text(
                                info.email ?: "Signed in",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        info.orgName?.let {
                            Spacer(Modifier.height(Spacing.xs))
                            Text(
                                "Organization: $it${info.role?.let { r -> " · $r" } ?: ""}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ------------------------------------------------------------ model
            item {
                CanvasCard(onClick = { showModelDialog = true }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Model", style = MaterialTheme.typography.titleMedium)
                            Text(
                                activeProfile.shortLabel(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text("Change", style = MaterialTheme.typography.labelMedium, color = CanvasPrimary)
                    }
                }
            }

            // --------------------------------------------------------- profiles
            item {
                CanvasCard {
                    SectionHeader(
                        title = "Cloud profiles",
                        subtitle = "Server-side model configurations",
                        trailing = {
                            IconButton(
                                onClick = onRefreshCloudSettings,
                                modifier = Modifier.testTag("btn_refresh_cloud")
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Refresh profiles",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    if (cloudProfiles.isEmpty()) {
                        Text(
                            "No cloud profiles yet — create one in the web app.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("cloud_profiles_section")
                        )
                    } else {
                        cloudProfiles.forEach { profile ->
                            ProfileRow(
                                profile = profile,
                                isActive = profile.name == activeCloudProfile,
                                onActivate = { onActivateCloudProfile(profile.name) },
                                onRename = { renamingProfile = profile.name },
                                onDelete = { deletingProfile = profile.name }
                            )
                        }
                    }
                }
            }

            // ---------------------------------------------------------- secrets
            item {
                CanvasCard {
                    SectionHeader(
                        title = "Secrets",
                        subtitle = "Values the agent can use without seeing them",
                        trailing = {
                            IconButton(
                                onClick = { showAddSecret = true },
                                modifier = Modifier.testTag("btn_add_secret")
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Add secret",
                                    tint = CanvasPrimary
                                )
                            }
                        }
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    if (secrets.isEmpty()) {
                        Text(
                            "No secrets yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("secrets_section")
                        )
                    } else {
                        secrets.forEach { secret ->
                            SecretRow(secret) { deletingSecret = secret.name }
                        }
                    }
                }
            }
        }

        // --------------------------------------------------------- local data
        item {
            CanvasCard {
                SectionHeader(title = "Local data")
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    if (isDemoMode) "Cached conversations are shown from this device."
                    else "Cached conversations and transcripts live on this device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(Spacing.md))
                OutlinedButton(
                    onClick = { showClearCacheConfirm = true },
                    shape = Shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Sizes.touchTarget)
                        .testTag("btn_clear_cache")
                ) { Text("Clear local data") }
            }
        }

        item {
            Text(
                "OpenHands Canvas · Android",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.sm)
            )
        }
    }

    // ------------------------------------------------------------- dialogs

    if (showModelDialog) {
        AgentProfileDialog(
            profile = activeProfile,
            onDismiss = { showModelDialog = false },
            onModelChanged = onModelChanged,
            title = "Model",
            availableModels = availableModels
        )
    }

    if (showClearCacheConfirm) {
        AlertDialog(
            onDismissRequest = { showClearCacheConfirm = false },
            title = { Text("Clear local data?") },
            text = { Text("Cached conversations and transcripts will be removed from this device. Cloud conversations remain on the server.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearCacheConfirm = false
                        onClearCache()
                    },
                    modifier = Modifier.testTag("btn_confirm_clear_cache")
                ) { Text("Clear", color = CanvasColors.danger) }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheConfirm = false }) { Text("Cancel") }
            }
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
                    onClick = { deletingProfile = null; onDeleteCloudProfile(name) },
                    modifier = Modifier.testTag("btn_confirm_delete_profile")
                ) { Text("Delete", color = CanvasColors.danger) }
            },
            dismissButton = { TextButton(onClick = { deletingProfile = null }) { Text("Cancel") } }
        )
    }

    deletingSecret?.let { name ->
        AlertDialog(
            onDismissRequest = { deletingSecret = null },
            title = { Text("Delete secret?") },
            text = { Text("'$name' will be removed from your account. This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = { deletingSecret = null; onDeleteSecret(name) },
                    modifier = Modifier.testTag("btn_confirm_delete_secret")
                ) { Text("Delete", color = CanvasColors.danger) }
            },
            dismissButton = { TextButton(onClick = { deletingSecret = null }) { Text("Cancel") } }
        )
    }

    if (showAddSecret) {
        AddSecretDialog(
            onDismiss = { showAddSecret = false },
            onCreate = { name, value, description ->
                showAddSecret = false
                onAddSecret(name, value, description)
            }
        )
    }
}

@Composable
private fun ProfileRow(
    profile: LlmProfileDto,
    isActive: Boolean,
    onActivate: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs)
            .testTag("profile_row_${profile.name}")
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    profile.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (isActive) {
                    Spacer(Modifier.width(Spacing.xs))
                    MetaChip(
                        "ACTIVE",
                        color = CanvasColors.success,
                        modifier = Modifier.testTag("active_profile_badge")
                    )
                }
            }
            profile.model?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (!isActive) {
            TextButton(
                onClick = onActivate,
                modifier = Modifier.testTag("btn_activate_profile_${profile.name}")
            ) { Text("Use", color = CanvasPrimary) }
        }
        IconButton(
            onClick = onRename,
            modifier = Modifier.testTag("btn_rename_profile_${profile.name}")
        ) {
            Icon(
                Icons.Default.DriveFileRenameOutline,
                contentDescription = "Rename profile",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(Sizes.iconSmall)
            )
        }
        IconButton(
            onClick = onDelete,
            modifier = Modifier.testTag("btn_delete_profile_${profile.name}")
        ) {
            Icon(
                Icons.Default.DeleteOutline,
                contentDescription = "Delete profile",
                tint = CanvasColors.danger,
                modifier = Modifier.size(Sizes.iconSmall)
            )
        }
    }
}

@Composable
private fun SecretRow(secret: SecretDto, onDelete: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs)
            .testTag("secret_row_${secret.name}")
    ) {
        Column(Modifier.weight(1f)) {
            Text(secret.name, style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium)
            secret.description?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        IconButton(
            onClick = onDelete,
            modifier = Modifier.testTag("btn_delete_secret_${secret.name}")
        ) {
            Icon(
                Icons.Default.DeleteOutline,
                contentDescription = "Delete secret",
                tint = CanvasColors.danger,
                modifier = Modifier.size(Sizes.iconSmall)
            )
        }
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
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("New name") },
                singleLine = true,
                shape = Shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CanvasPrimary,
                    unfocusedBorderColor = CanvasBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("field_rename_profile")
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(newName) },
                enabled = newName.isNotBlank(),
                modifier = Modifier.testTag("btn_confirm_rename")
            ) { Text("Rename", color = CanvasPrimaryDeep) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
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
                    "Stored server-side; the agent only sees the name.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(Spacing.md))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    shape = Shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CanvasPrimary,
                        unfocusedBorderColor = CanvasBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("field_secret_name")
                )
                Spacer(Modifier.height(Spacing.sm))
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("Value") },
                    singleLine = true,
                    visualTransformation = if (valueVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { valueVisible = !valueVisible }) {
                            Icon(
                                if (valueVisible) Icons.Default.VisibilityOff
                                else Icons.Default.Visibility,
                                contentDescription = if (valueVisible) "Hide value" else "Show value"
                            )
                        }
                    },
                    shape = Shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CanvasPrimary,
                        unfocusedBorderColor = CanvasBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("field_secret_value")
                )
                Spacer(Modifier.height(Spacing.sm))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    singleLine = true,
                    shape = Shapes.medium,
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
            ) { Text("Add", color = CanvasPrimaryDeep) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
