package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.settings.AgentKind
import com.example.data.settings.AgentProfile
import com.example.data.settings.SUGGESTED_MODELS
import com.example.ui.theme.CanvasPrimary

/**
 * Dialog for viewing and switching the active agent + LLM. Mirrors the
 * OpenHands SDK agent settings: the default OpenHands agent is driven by a
 * LiteLLM-convention model (`provider/model`), while the opt-in ACP agent
 * spawns an ACP server that manages its own LLM (model field not applicable).
 *
 * Inside a conversation the edits are a mid-session switch; in Settings they
 * update the default for new tasks. ACP is only ever active if the user
 * explicitly picks it.
 */
@Composable
fun AgentProfileDialog(
    profile: AgentProfile,
    onDismiss: () -> Unit,
    onAgentKindChanged: (AgentKind) -> Unit,
    onModelChanged: (String) -> Unit,
    onAcpCommandChanged: (String) -> Unit,
    title: String = "Agent & LLM",
    /**
     * Optional "Reset to default" action. Only conversations carry a per-
     * conversation override, so callers outside a conversation leave this null.
     */
    onReset: (() -> Unit)? = null
) {
    var modelDraft by remember(profile) { mutableStateOf(profile.model) }
    var acpDraft by remember(profile) { mutableStateOf(profile.acpCommand) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { profileDialogBody(profile, modelDraft, acpDraft, {
            modelDraft = it
        }, { acpDraft = it }, onAgentKindChanged, onModelChanged, onAcpCommandChanged) },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                onReset?.let { reset ->
                    TextButton(
                        onClick = {
                            reset()
                            onDismiss()
                        },
                        modifier = Modifier.testTag("btn_profile_reset")
                    ) { Text("Reset to default") }
                }
                TextButton(
                    onClick = {
                        if (profile.kind == AgentKind.OPENHANDS && modelDraft != profile.model) {
                            onModelChanged(modelDraft)
                        }
                        if (profile.kind == AgentKind.ACP && acpDraft != profile.acpCommand) {
                            onAcpCommandChanged(acpDraft)
                        }
                        onDismiss()
                    },
                    modifier = Modifier.testTag("btn_profile_done")
                ) { Text("Done") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
/** Body of the dialog — kept in its own function to keep file size manageable. */
@Composable
private fun profileDialogBody(
    profile: AgentProfile,
    modelDraft: String,
    acpDraft: String,
    onModelDraftChanged: (String) -> Unit,
    onAcpDraftChanged: (String) -> Unit,
    onAgentKindChanged: (AgentKind) -> Unit,
    onModelChanged: (String) -> Unit,
    onAcpCommandChanged: (String) -> Unit
) {
    Column(
        Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Agent backend",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        AgentKind.entries.forEach { kind ->
            FilterChip(
                selected = profile.kind == kind,
                onClick = { onAgentKindChanged(kind) },
                label = {
                    Column {
                        Text(kind.label, fontWeight = FontWeight.SemiBold)
                        Text(
                            kind.blurb,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.SmartToy,
                        null,
                        Modifier.size(16.dp),
                        tint = if (profile.kind == kind) CanvasPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("agent_kind_${kind.name}")
            )
        }

        when (profile.kind) {
            AgentKind.OPENHANDS -> {
                Text(
                    "LLM model (LiteLLM convention: provider/model)",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = modelDraft,
                    onValueChange = onModelDraftChanged,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("field_model")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SUGGESTED_MODELS.take(3).forEach { suggestion ->
                        FilterChip(
                            selected = profile.model == suggestion,
                            onClick = {
                                onModelDraftChanged(suggestion)
                                onModelChanged(suggestion)
                            },
                            label = {
                                Text(
                                    suggestion.substringAfterLast('/'),
                                    fontSize = 10.sp,
                                    maxLines = 1
                                )
                            }
                        )
                    }
                }
            }
            AgentKind.ACP -> {
                Text(
                    "ACP server command (the server manages its own LLM)",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = acpDraft,
                    onValueChange = onAcpDraftChanged,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("field_acp_command")
                )
            }
        }
    }
}