package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PromptTemplate
import com.example.ui.theme.CanvasPrimary
import com.example.ui.theme.CanvasPrimaryDeep
import com.example.ui.viewmodel.LaunchStage

@Composable
fun NewTaskScreen(
    promptText: String,
    repoText: String,
    templates: List<PromptTemplate>,
    isLaunching: Boolean,
    launchStage: LaunchStage,
    launchStatusText: String?,
    isDemoMode: Boolean,
    onPromptChanged: (String) -> Unit,
    onRepoChanged: (String) -> Unit,
    onSelectTemplate: (PromptTemplate) -> Unit,
    onLaunchTask: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp)
            .testTag("new_task_screen")
    ) {
        Text("New agent task", style = MaterialTheme.typography.titleLarge)
        Text(
            "OpenHands will work in its own sandbox and report back with a summary, diffs, and test results.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
        )

        OutlinedTextField(
            value = promptText,
            onValueChange = onPromptChanged,
            placeholder = { Text("Describe the task — e.g. \"Fix the failing checkout test and open a PR\"") },
            minLines = 5,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CanvasPrimary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("prompt_input")
        )

        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = repoText,
            onValueChange = onRepoChanged,
            placeholder = { Text("owner/repository (optional)") },
            leadingIcon = { Icon(Icons.Default.Folder, null, Modifier.size(18.dp)) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CanvasPrimary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("repo_input")
        )

        Spacer(Modifier.height(14.dp))
        Text("Quick fill", style = MaterialTheme.typography.titleMedium)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            items(templates, key = { it.id }) { template ->
                AssistChip(
                    onClick = { onSelectTemplate(template) },
                    label = { Text(template.title, fontSize = 11.sp) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.AutoAwesome,
                            null,
                            Modifier.size(14.dp),
                            tint = CanvasPrimary
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }

        if (isLaunching) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = CanvasPrimary.copy(alpha = 0.08f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag("launch_progress")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(12.dp)
                ) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        launchStatusText ?: LaunchStage.WAITING_SANDBOX.label,
                        fontSize = 13.sp,
                        color = CanvasPrimary
                    )
                }
            }
        }

        if (isDemoMode) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    "Demo mode is read-only. Add an API key in Settings to launch a live cloud agent.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onLaunchTask,
            enabled = !isLaunching && promptText.isNotBlank(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CanvasPrimaryDeep),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("btn_launch")
        ) {
            Icon(Icons.Default.RocketLaunch, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                when {
                    isLaunching -> "Launching…"
                    isDemoMode -> "Connect to launch"
                    else -> "Launch task"
                },
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}
