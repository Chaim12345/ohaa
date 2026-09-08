package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AccountInfo
import com.example.data.model.LlmProfile
import com.example.ui.components.ErrorBanner
import com.example.ui.components.InfoCard
import com.example.ui.components.KeyValueRow
import com.example.ui.components.LinkRow
import com.example.ui.components.SectionTitle
import com.example.ui.theme.OpenHandsPrimaryDark
import com.example.ui.theme.StatusReady
import com.example.ui.theme.StatusWaiting

@Composable
fun SettingsScreen(
    apiKey: String,
    baseUrl: String,
    isUsingBuildTimeKey: Boolean,
    account: AccountInfo?,
    accountError: String?,
    isTesting: Boolean,
    showNoiseEvents: Boolean,
    onApiKeyChanged: (String) -> Unit,
    onBaseUrlChanged: (String) -> Unit,
    onSave: () -> Unit,
    onTestConnection: () -> Unit,
    onToggleNoise: () -> Unit,
    onClearCache: () -> Unit,
    onOpenSandboxes: () -> Unit,
    onOpenSecrets: () -> Unit = {},
    llmProfiles: List<LlmProfile> = emptyList(),
    onLoadProfiles: () -> Unit = {},
    onActivateProfile: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showKey by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val fieldColors = OutlinedTextFieldDefaults.colors(focusedBorderColor = OpenHandsPrimaryDark, unfocusedBorderColor = MaterialTheme.colorScheme.outline)

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp).verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(OpenHandsPrimaryDark.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.Cloud, contentDescription = null, tint = OpenHandsPrimaryDark, modifier = Modifier.size(22.dp)) }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Text("OpenHands Cloud account and server", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(18.dp))
        // Account status
        when {
            isTesting -> InfoCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Checking connection…", fontSize = 13.sp)
                }
            }
            account != null -> InfoCard(tint = StatusReady) {
                Column {
                    Text("Connected", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(6.dp))
                    account.email?.let { KeyValueRow("Email", it) }
                    account.orgName?.let { KeyValueRow("Organization", it) }
                    account.role?.let { KeyValueRow("Role", it) }
                    account.gitUserName?.let { KeyValueRow("Git user", it, mono = true) }
                    account.apiKeyName?.let { KeyValueRow("API key", it + (account.apiKeyAuthType?.let { t -> " ($t)" } ?: "")) }
                    account.credits?.let { KeyValueRow("Credits", it) }
                    account.maxBudgetPerTask?.let { KeyValueRow("Max budget / task", "$$it") }
                    account.userId?.let { KeyValueRow("User ID", it, mono = true) }
                }
            }
            accountError != null -> ErrorBanner(accountError, actionLabel = "Retry", onAction = onTestConnection)
        }
        if (isUsingBuildTimeKey) {
            Spacer(Modifier.height(10.dp))
            InfoCard(tint = StatusWaiting) {
                Text(
                    "Using the API key bundled with this build. Paste your own key below to override it on this device.",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, lineHeight = 16.sp
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        SectionTitle("API key")
        Spacer(Modifier.height(4.dp))
        Text("Create one at app.all-hands.dev → Settings → API Keys. Sent as X-Access-Token.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = apiKey,
            onValueChange = onApiKeyChanged,
            placeholder = { Text("sk-oh-…") },
            leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = OpenHandsPrimaryDark) },
            trailingIcon = {
                IconButton(onClick = { showKey = !showKey }) {
                    Icon(if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = if (showKey) "Hide" else "Show")
                }
            },
            visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = fieldColors,
            modifier = Modifier.fillMaxWidth().testTag("settings_api_key")
        )

        Spacer(Modifier.height(16.dp))
        SectionTitle("Server")
        Spacer(Modifier.height(4.dp))
        Text("Default is https://app.all-hands.dev. Point this at a self-hosted OpenHands app server if you run your own.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = baseUrl,
            onValueChange = onBaseUrlChanged,
            placeholder = { Text("https://app.all-hands.dev") },
            leadingIcon = { Icon(Icons.Default.Cloud, contentDescription = null, tint = OpenHandsPrimaryDark) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = fieldColors,
            modifier = Modifier.fillMaxWidth().testTag("settings_base_url")
        )

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onSave, shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OpenHandsPrimaryDark),
                modifier = Modifier.weight(1f).height(46.dp).testTag("settings_save")
            ) { Text("Save & connect", fontWeight = FontWeight.Bold) }
            OutlinedButton(onClick = onTestConnection, enabled = !isTesting, shape = RoundedCornerShape(12.dp), modifier = Modifier.height(46.dp).testTag("settings_test")) {
                Text("Test")
            }
        }

        Spacer(Modifier.height(22.dp))
        SectionTitle("Display")
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Show system events", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("Include system prompts, token usage, state updates and condensations in the timeline.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = showNoiseEvents, onCheckedChange = { onToggleNoise() }, modifier = Modifier.testTag("settings_noise_toggle"))
        }

        Spacer(Modifier.height(14.dp))
        SectionTitle("Cloud resources")
        LinkRow("Manage sandboxes", onClick = onOpenSandboxes)
        LinkRow("Manage secrets", onClick = onOpenSecrets)

        Spacer(Modifier.height(14.dp))
        SectionTitle("LLM profiles") {
            Text(if (llmProfiles.isEmpty()) "Load" else "Reload", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onLoadProfiles))
        }
        if (llmProfiles.isEmpty()) {
            Text("Profiles define which model new conversations use. Tap Load to fetch them.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 6.dp))
        }
        llmProfiles.forEach { p ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(p.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Text(listOfNotNull(p.model, if (p.apiKeySet) "key set" else "no key").joinToString(" · "), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (p.isActive) Text("Default", fontSize = 12.sp, color = StatusReady, fontWeight = FontWeight.SemiBold)
                else TextButton(onClick = { onActivateProfile(p.name) }) { Text("Make default") }
            }
        }

        Spacer(Modifier.height(14.dp))
        SectionTitle("Data")
        OutlinedButton(onClick = onClearCache, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().testTag("settings_clear_cache")) {
            Text("Clear local cache")
        }
        Text("Removes cached conversations and events on this device. Nothing is deleted from the cloud.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))

        Spacer(Modifier.height(14.dp))
        SectionTitle("Links")
        LinkRow("OpenHands Cloud web app") { context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://app.all-hands.dev"))) }
        LinkRow("Cloud API documentation") { context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://docs.openhands.dev/openhands/usage/cloud/cloud-api"))) }
        LinkRow("Manage API keys") { context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://app.all-hands.dev/settings/api-keys"))) }
        Spacer(Modifier.height(24.dp))
    }
}
