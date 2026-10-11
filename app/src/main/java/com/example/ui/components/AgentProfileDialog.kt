package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.example.data.settings.AgentProfile
import com.example.data.settings.DEFAULT_OPENHANDS_MODEL
import com.example.ui.theme.CanvasPrimary

/**
 * LLM switcher. The OpenHands agent is the only backend; the model list comes
 * from the live cloud catalogue (free, verified `openhands`-provider entries),
 * falling back to the built-in default when the catalogue has not loaded.
 */
@Composable
fun AgentProfileDialog(
    profile: AgentProfile,
    onDismiss: () -> Unit,
    onModelChanged: (String) -> Unit,
    title: String = "Agent & LLM",
    availableModels: List<String> = emptyList()
) {
    var modelDraft by remember(profile) { mutableStateOf(profile.model) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            profileDialogBody(profile, modelDraft, availableModels, { modelDraft = it }, onModelChanged)
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (modelDraft != profile.model) onModelChanged(modelDraft)
                    onDismiss()
                },
                modifier = Modifier.testTag("btn_profile_done")
            ) { Text("Done") }
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
    availableModels: List<String>,
    onModelDraftChanged: (String) -> Unit,
    onModelChanged: (String) -> Unit
) {
    Column(
        Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Icon(
                Icons.Default.SmartToy,
                null,
                Modifier.size(16.dp),
                tint = CanvasPrimary
            )
            Text(
                "  OpenHands agent",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Text(
            "Model (free, verified OpenHands Cloud models)",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        // Read-only: the free-model set is decided by the cloud, so the user
        // picks from the catalogue rather than typing a model that may not run.
        OutlinedTextField(
            value = modelDraft,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("field_model")
        )

        // The catalogue is provider-scoped upstream; when it has not loaded yet
        // offer at least the known-good default so the picker is never empty.
        val suggestions = availableModels.ifEmpty { listOf(DEFAULT_OPENHANDS_MODEL) }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(suggestions, key = { it }) { suggestion ->
                FilterChip(
                    selected = modelDraft == suggestion,
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
        if (availableModels.isEmpty()) {
            Text(
                "Loading the model catalogue…",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
