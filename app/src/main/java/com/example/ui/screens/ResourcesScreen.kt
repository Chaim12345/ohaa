package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProfileInfoDto
import com.example.data.model.SandboxInfoDto
import com.example.data.model.SecretDto
import com.example.data.model.SkillInfoDto
import com.example.data.model.UpdateUserSettingsRequest
import com.example.data.model.UserSettingsDto
import com.example.data.model.WebClientConfigDto
import com.example.ui.theme.OpenHandsPrimaryDark

/**
 * Cloud resources: secrets, skills, sandboxes, git, and settings. Each section
 * has its own list so it can be refreshed independently.
 */
@Composable
fun ResourcesScreen(
    secrets: List<SecretDto>,
    skills: List<SkillInfoDto>,
    sandboxes: List<SandboxInfoDto>,
    gitLogin: String?,
    gitOrganizations: List<String> = emptyList(),
    gitInstallations: List<String> = emptyList(),
    userSettings: UserSettingsDto? = null,
    webClientConfig: WebClientConfigDto? = null,
    onLoadSecrets: () -> Unit,
    onLoadSkills: () -> Unit,
    onLoadSandboxes: () -> Unit,
    onLoadGitAccount: () -> Unit,
    onLoadGitOrganizations: () -> Unit,
    onLoadGitInstallations: () -> Unit,
    onLoadSettings: () -> Unit,
    onLoadWebClientConfig: () -> Unit,
    onSaveSettings: (UpdateUserSettingsRequest) -> Unit,
    onStoreGitToken: (String, String) -> Unit,
    onUnlinkGitProvider: (String) -> Unit,
    onCreateSecret: (String, String, String?) -> Unit,
    onDeleteSecret: (String) -> Unit,
    onDeleteSandbox: (String) -> Unit,
    onCreateSandbox: (String) -> Unit = {},
    onDeleteProfile: (String) -> Unit = {},
    onActivateProfile: (String) -> Unit = {},
    onSaveProfile: (String, String?, String?) -> Unit = { _, _, _ -> },
    profiles: List<ProfileInfoDto> = emptyList(),
    onLoadProfiles: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var section by remember { mutableStateOf("Secrets") }
    var showAddSecret by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Cloud Resources", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            IconButton(
                onClick = {
                    when (section) {
                        "Secrets" -> onLoadSecrets()
                        "Skills" -> onLoadSkills()
                        "Sandboxes" -> onLoadSandboxes()
                        "Git" -> { onLoadGitAccount(); onLoadGitOrganizations(); onLoadGitInstallations() }
                        "Profiles" -> onLoadProfiles()
                        "Settings" -> { onLoadSettings(); onLoadWebClientConfig() }
                    }
                },
                modifier = Modifier.testTag("resources_refresh")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = OpenHandsPrimaryDark)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("Secrets", "Skills", "Sandboxes", "Git", "Profiles", "Settings").forEach { s ->
                FilterChip(
                    selected = section == s,
                    onClick = { section = s },
                    label = { Text(s, fontSize = 12.sp) },
                    modifier = Modifier.testTag("resources_tab_$s")
                )
            }
        }

        if (gitLogin != null) {
            Text(
                text = "Git account: $gitLogin",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        when (section) {
            "Secrets" -> SecretsSection(
                secrets = secrets,
                onAdd = { showAddSecret = true },
                onDelete = onDeleteSecret
            )
            "Skills" -> SkillsSection(skills = skills)
            "Sandboxes" -> SandboxesSection(
                sandboxes = sandboxes,
                onDelete = onDeleteSandbox,
                onCreate = onCreateSandbox
            )
            "Git" -> GitSection(
                organizations = gitOrganizations,
                installations = gitInstallations,
                onStoreToken = onStoreGitToken,
                onUnlink = onUnlinkGitProvider
            )
            "Profiles" -> ProfilesSection(
                profiles = profiles,
                onDelete = onDeleteProfile,
                onActivate = onActivateProfile,
                onSave = onSaveProfile
            )
            "Settings" -> SettingsSection(settings = userSettings, webClient = webClientConfig, onSave = onSaveSettings)
        }
    }

    if (showAddSecret) {
        AddSecretDialog(
            onDismiss = { showAddSecret = false },
            onSave = { name, value, desc ->
                onCreateSecret(name, value, desc)
                showAddSecret = false
            }
        )
    }
}

@Composable
private fun GitSection(
    organizations: List<String>,
    installations: List<String>,
    onStoreToken: (String, String) -> Unit,
    onUnlink: (String) -> Unit,
) {
    var provider by remember { mutableStateOf("github") }
    var token by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = provider,
            onValueChange = { provider = it },
            label = { Text("Provider (github/gitlab/bitbucket)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("git_provider_input")
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = token,
            onValueChange = { token = it },
            label = { Text("Personal access token") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("git_token_input")
        )
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(
                onClick = { if (token.isNotBlank()) onStoreToken(provider, token) },
                enabled = token.isNotBlank(),
                modifier = Modifier.testTag("git_store_token")
            ) { Text("Store token") }
            OutlinedButton(
                onClick = { onUnlink(provider) },
                modifier = Modifier.testTag("git_unlink")
            ) { Text("Unlink") }
        }
        Spacer(Modifier.height(10.dp))
        if (organizations.isNotEmpty()) {
            Text("Organizations", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            organizations.forEach { Text("• $it", fontSize = 12.sp) }
            Spacer(Modifier.height(8.dp))
        }
        if (installations.isNotEmpty()) {
            Text("Installations", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            installations.forEach { Text("• $it", fontSize = 12.sp) }
        }
        if (organizations.isEmpty() && installations.isEmpty()) {
            EmptyHint("No git organizations or installations. Tap refresh.")
        }
    }
}

@Composable
private fun ProfilesSection(
    profiles: List<ProfileInfoDto>,
    onDelete: (String) -> Unit,
    onActivate: (String) -> Unit,
    onSave: (String, String?, String?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var baseUrl by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Profile name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("profile_name_input")
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = model,
            onValueChange = { model = it },
            label = { Text("Model") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("profile_model_input")
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = baseUrl,
            onValueChange = { baseUrl = it },
            label = { Text("Base URL (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("profile_base_url_input")
        )
        Spacer(Modifier.height(6.dp))
        OutlinedButton(
            onClick = { if (name.isNotBlank()) onSave(name, model.ifBlank { null }, baseUrl.ifBlank { null }) },
            enabled = name.isNotBlank(),
            modifier = Modifier.fillMaxWidth().testTag("profile_save")
        ) { Text("Save profile") }
        Spacer(Modifier.height(8.dp))
        if (profiles.isEmpty()) {
            EmptyHint("No saved profiles. Tap refresh.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(profiles) { p ->
                    ResourceRow(
                        title = p.name ?: "(unnamed)",
                        subtitle = p.model,
                        onClick = p.name?.let { n -> { onActivate(n) } },
                        onDelete = p.name?.let { n -> { onDelete(n) } }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    settings: UserSettingsDto?,
    webClient: WebClientConfigDto?,
    onSave: (UpdateUserSettingsRequest) -> Unit,
) {
    var gitUserName by remember { mutableStateOf("") }
    var gitUserEmail by remember { mutableStateOf("") }
    var budget by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        if (settings != null) {
            Text("Current settings", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("email: ${settings.email ?: "—"}", fontSize = 12.sp)
            Text("language: ${settings.language ?: "—"}", fontSize = 12.sp)
            Text("git user: ${settings.gitUserName ?: "—"}", fontSize = 12.sp)
            Text("llm api key set: ${settings.llmApiKeySet}", fontSize = 12.sp)
            Text("max budget/task: ${settings.maxBudgetPerTask ?: "—"}", fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
        } else {
            EmptyHint("No settings loaded. Tap refresh.")
        }
        OutlinedTextField(
            value = gitUserName,
            onValueChange = { gitUserName = it },
            label = { Text("Git user name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("settings_git_name")
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = gitUserEmail,
            onValueChange = { gitUserEmail = it },
            label = { Text("Git user email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("settings_git_email")
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = budget,
            onValueChange = { budget = it },
            label = { Text("Max budget per task") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("settings_budget")
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                onSave(
                    UpdateUserSettingsRequest(
                        gitUserName = gitUserName.ifBlank { null },
                        gitUserEmail = gitUserEmail.ifBlank { null },
                        maxBudgetPerTask = budget.toDoubleOrNull()
                    )
                )
            },
            colors = ButtonDefaults.buttonColors(containerColor = OpenHandsPrimaryDark),
            modifier = Modifier.fillMaxWidth().testTag("settings_save")
        ) { Text("Save settings", color = Color.White) }

        if (webClient != null) {
            Spacer(Modifier.height(12.dp))
            Text("Web client", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("app mode: ${webClient.appMode ?: "—"}", fontSize = 12.sp)
            Text("auth url: ${webClient.authUrl ?: "—"}", fontSize = 12.sp)
            Text("acp providers: ${webClient.acpProviders?.joinToString { it.key ?: "?" } ?: "—"}", fontSize = 12.sp)
        }
    }
}

@Composable
private fun SecretsSection(
    secrets: List<SecretDto>,
    onAdd: () -> Unit,
    onDelete: (String) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        OutlinedButton(
            onClick = onAdd,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("add_secret_button")
        ) { Text("Add secret") }

        Spacer(Modifier.height(8.dp))

        if (secrets.isEmpty()) {
            EmptyHint("No secrets. Tap Add secret, or refresh.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(secrets) { s ->
                    ResourceRow(
                        title = s.name ?: "(unnamed)",
                        subtitle = s.description ?: s.scope,
                        onDelete = s.id?.let { id -> { onDelete(id) } }
                    )
                }
            }
        }
    }
}

@Composable
private fun SkillsSection(skills: List<SkillInfoDto>) {
    if (skills.isEmpty()) {
        EmptyHint("No skills found. Tap refresh.")
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(skills) { s ->
                ResourceRow(title = s.name ?: "(unnamed)", subtitle = s.description ?: s.type)
            }
        }
    }
}

@Composable
private fun SandboxesSection(
    sandboxes: List<SandboxInfoDto>,
    onDelete: (String) -> Unit,
    onCreate: (String) -> Unit = {},
) {
    var specId by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = specId,
            onValueChange = { specId = it },
            label = { Text("Sandbox spec id") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("sandbox_spec_input")
        )
        Spacer(Modifier.height(6.dp))
        OutlinedButton(
            onClick = { if (specId.isNotBlank()) onCreate(specId) },
            enabled = specId.isNotBlank(),
            modifier = Modifier.fillMaxWidth().testTag("sandbox_create")
        ) { Text("Create sandbox") }
        Spacer(Modifier.height(8.dp))
        if (sandboxes.isEmpty()) {
            EmptyHint("No sandboxes. Tap refresh.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(sandboxes) { s ->
                    ResourceRow(
                        title = "Sandbox ${s.id?.take(8) ?: "?"}",
                        subtitle = "${s.status ?: "?"} · spec ${s.sandboxSpecId?.take(8) ?: "?"}",
                        onDelete = s.id?.let { id -> { onDelete(id) } }
                    )
                }
            }
        }
    }
}

@Composable
private fun ResourceRow(
    title: String,
    subtitle: String?,
    onDelete: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        subtitle,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AddSecretDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add secret", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("secret_name_input")
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("Value") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("secret_value_input")
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank() && value.isNotBlank()) onSave(name, value, description.ifBlank { null }) },
                enabled = name.isNotBlank() && value.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = OpenHandsPrimaryDark),
                modifier = Modifier.testTag("secret_save_button")
            ) { Text("Save", color = Color.White) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        shape = RoundedCornerShape(18.dp)
    )
}
