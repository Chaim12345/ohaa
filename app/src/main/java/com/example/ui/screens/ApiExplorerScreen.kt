package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Generic caller for every endpoint in the API catalog. The bespoke screens
 * cover the everyday flows; this screen guarantees the remaining endpoints
 * (server-to-server callbacks, OpenAI-compatible surface, one-off admin routes)
 * are still reachable from the app.
 */
@Composable
fun ApiExplorerScreen(
    cloudPaths: List<String>,
    agentPaths: List<String>,
    response: String?,
    isLoading: Boolean,
    onCall: (surface: String, method: String, path: String, body: String) -> Unit,
) {
    var surface by remember { mutableStateOf("cloud") }
    var method by remember { mutableStateOf("GET") }
    var path by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var methodMenu by remember { mutableStateOf(false) }
    val paths = if (surface == "cloud") cloudPaths else agentPaths

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text("API Explorer", style = MaterialTheme.typography.titleLarge)
        Text(
            "Call any documented endpoint. Paths use {} for parameters — replace them with real values.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
            FilterChip(
                selected = surface == "cloud",
                onClick = { surface = "cloud" },
                label = { Text("Cloud V1") },
                modifier = Modifier.testTag("explorer_surface_cloud"),
            )
            FilterChip(
                selected = surface == "agent",
                onClick = { surface = "agent" },
                label = { Text("Sandbox runtime") },
                modifier = Modifier.testTag("explorer_surface_agent"),
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) {
            Column {
                Button(onClick = { methodMenu = true }, modifier = Modifier.testTag("explorer_method")) {
                    Text(method)
                }
                DropdownMenu(expanded = methodMenu, onDismissRequest = { methodMenu = false }) {
                    listOf("GET", "POST", "PUT", "PATCH", "DELETE").forEach {
                        DropdownMenuItem(text = { Text(it) }, onClick = { method = it; methodMenu = false })
                    }
                }
            }
            OutlinedTextField(
                value = path,
                onValueChange = { path = it },
                label = { Text("Path") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("explorer_path"),
            )
        }

        OutlinedTextField(
            value = body,
            onValueChange = { body = it },
            label = { Text("JSON body (POST/PUT/PATCH)") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag("explorer_body"),
        )

        Button(
            onClick = { onCall(surface, method, path, body) },
            enabled = !isLoading && path.isNotBlank(),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag("explorer_send"),
        ) { Text(if (isLoading) "Calling…" else "Call") }

        if (response != null) {
            Card(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Text(response, fontSize = 11.sp, modifier = Modifier.padding(12.dp).testTag("explorer_response"))
            }
        }

        Text("Available paths", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp))
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(paths) { p ->
                Text(
                    p,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .testTag("explorer_path_item"),
                )
            }
        }
    }
}
