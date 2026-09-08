package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Conversation
import com.example.data.model.Sandbox
import com.example.data.model.SandboxStatus
import com.example.ui.components.EmptyState
import com.example.ui.components.LinkRow
import com.example.ui.components.SandboxBadge
import com.example.ui.components.formatRelativeTime
import com.example.ui.theme.OpenHandsPrimaryDark

@Composable
fun SandboxesScreen(
    sandboxes: List<Sandbox>,
    conversations: List<Conversation>,
    isLoading: Boolean,
    isMutating: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onPause: (String) -> Unit,
    onResume: (String) -> Unit,
    onDelete: (String) -> Unit,
    onOpenConversation: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var deleteTarget by remember { mutableStateOf<Sandbox?>(null) }
    val context = LocalContext.current

    Column(modifier = modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface) }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Sandboxes", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("${sandboxes.size} total · ${sandboxes.count { it.status == SandboxStatus.RUNNING }} running", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onRefresh, enabled = !isLoading) { Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.primary) }
            }
        }
        if (isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp), color = OpenHandsPrimaryDark)

        if (sandboxes.isEmpty() && !isLoading) {
            EmptyState("No sandboxes", "Sandboxes are created when you start a conversation and are removed when it is deleted.")
        }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Spacer(Modifier.height(8.dp)) }
            items(sandboxes, key = { it.id }) { sb ->
                val linked = conversations.filter { it.sandboxId == sb.id }
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth().testTag("sandbox_${sb.id}")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text(sb.id.take(12), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                            SandboxBadge(sb.status)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            listOfNotNull(sb.specId?.let { "spec $it" }, if (sb.createdAt > 0) "created ${formatRelativeTime(sb.createdAt)}" else null).joinToString(" · "),
                            fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        sb.statusDetail?.let { Text(it, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        linked.forEach { conv ->
                            TextButton(onClick = { onOpenConversation(conv.id) }, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                                Text("↳ ${conv.displayTitle}", fontSize = 12.sp)
                            }
                        }
                        sb.exposedUrls.forEach { (name, url) ->
                            LinkRow("$name: $url") { context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))) }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            when (sb.status) {
                                SandboxStatus.RUNNING, SandboxStatus.STARTING ->
                                    OutlinedButton(onClick = { onPause(sb.id) }, enabled = !isMutating, shape = RoundedCornerShape(10.dp)) { Text("Pause") }
                                SandboxStatus.PAUSED ->
                                    OutlinedButton(onClick = { onResume(sb.id) }, enabled = !isMutating, shape = RoundedCornerShape(10.dp)) { Text("Resume") }
                                else -> Unit
                            }
                            Spacer(Modifier.width(2.dp))
                            TextButton(onClick = { deleteTarget = sb }, enabled = !isMutating) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    deleteTarget?.let { sb ->
        ConfirmDialog(
            title = "Delete sandbox?",
            text = "Sandbox ${sb.id.take(12)} and any uncommitted work inside it will be destroyed.",
            confirmLabel = "Delete",
            onDismiss = { deleteTarget = null },
            onConfirm = { onDelete(sb.id); deleteTarget = null }
        )
    }
}
