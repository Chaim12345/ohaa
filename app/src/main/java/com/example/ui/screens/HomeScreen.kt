package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AccountInfo
import com.example.data.model.Conversation
import com.example.data.model.ConversationStatus
import com.example.data.model.PromptTemplate
import com.example.ui.components.ErrorBanner
import com.example.ui.components.InfoCard
import com.example.ui.components.SectionTitle
import com.example.ui.components.StatMetricCard
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatCost
import com.example.ui.components.formatRelativeTime
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusWaiting
import com.example.ui.theme.StatusWorking

@Composable
fun HomeScreen(
    conversations: List<Conversation>,
    templates: List<PromptTemplate>,
    account: AccountInfo?,
    accountError: String?,
    needsApiKey: Boolean,
    isRefreshing: Boolean,
    listError: String?,
    onNewConversation: () -> Unit,
    onOpenConversation: (String) -> Unit,
    onOpenAllConversations: () -> Unit,
    onSelectTemplate: (PromptTemplate) -> Unit,
    onOpenSandboxes: () -> Unit,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val running = conversations.count { it.status == ConversationStatus.RUNNING || it.status == ConversationStatus.STARTING }
    val needsAttention = conversations.count {
        it.status == ConversationStatus.WAITING_FOR_CONFIRMATION || it.status == ConversationStatus.STUCK || it.status == ConversationStatus.ERROR
    }
    val totalCost = conversations.sumOf { it.cost }
    val recent = conversations.sortedByDescending { it.updatedAt }.take(6)

    LazyColumn(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("OpenHands Cloud", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Text(
                        account?.email ?: account?.orgName ?: "Cloud coding agents on your phone",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row {
                    IconButton(onClick = onRefresh, enabled = !isRefreshing, modifier = Modifier.testTag("home_refresh")) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onOpenSettings, modifier = Modifier.testTag("home_settings")) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        if (needsApiKey) {
            item {
                InfoCard(tint = StatusError) {
                    Column {
                        Text("Connect your OpenHands Cloud account", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            accountError ?: "Paste an API key from app.all-hands.dev → Settings → API Keys to load your conversations.",
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp
                        )
                        Spacer(Modifier.height(10.dp))
                        Button(onClick = onOpenSettings, shape = RoundedCornerShape(10.dp), modifier = Modifier.testTag("home_connect")) { Text("Open settings") }
                    }
                }
            }
        } else if (listError != null) {
            item { ErrorBanner(listError, actionLabel = "Retry", onAction = onRefresh) }
        }

        // Hero
        item {
            Card(shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), modifier = Modifier.fillMaxWidth().testTag("hero_banner_card")) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_openhands_banner),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(140.dp)
                    )
                    Box(modifier = Modifier.fillMaxWidth().height(140.dp).background(Brush.verticalGradient(listOf(Color.Transparent, MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)))))
                    Column(modifier = Modifier.align(Alignment.BottomStart).padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (account != null) Color(0xFF3FB950) else Color(0xFFF0883E)))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                when {
                                    account != null -> listOfNotNull(account.orgName, account.role).joinToString(" · ").ifBlank { "Connected" }
                                    needsApiKey -> "Not connected"
                                    else -> "Connecting…"
                                },
                                fontSize = 11.sp, color = Color.White.copy(alpha = 0.9f), fontFamily = FontFamily.Monospace
                            )
                        }
                        Text("Your agents, anywhere", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        // Stats
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                StatMetricCard("Running", running.toString(), "active sandboxes", Icons.Default.PlayArrow, StatusWorking, Modifier.weight(1f))
                StatMetricCard("Needs you", needsAttention.toString(), "waiting / stuck / error", Icons.Default.Warning, StatusWaiting, Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                StatMetricCard("Loaded", conversations.size.toString(), "conversations", Icons.AutoMirrored.Filled.List, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                StatMetricCard(
                    "Spend", formatCost(totalCost),
                    account?.credits?.let { "credits left: $it" } ?: "across loaded conversations",
                    Icons.Default.AttachMoney, MaterialTheme.colorScheme.tertiary, Modifier.weight(1f)
                )
            }
        }

        // Quick actions
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onNewConversation, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f).height(46.dp).testTag("home_new")) {
                    Icon(Icons.Default.Add, contentDescription = null); Spacer(Modifier.width(6.dp)); Text("New conversation")
                }
                OutlinedButton(onClick = onOpenSandboxes, shape = RoundedCornerShape(12.dp), modifier = Modifier.height(46.dp).testTag("home_sandboxes")) {
                    Icon(Icons.Default.Dns, contentDescription = null); Spacer(Modifier.width(6.dp)); Text("Sandboxes")
                }
            }
        }

        // Recent
        item {
            SectionTitle("Recent conversations") {
                Text("See all", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable(onClick = onOpenAllConversations))
            }
        }
        if (recent.isEmpty()) {
            item {
                Text(
                    if (needsApiKey) "Conversations appear here once you connect." else "No conversations yet. Start one above.",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        items(recent, key = { it.id }) { conv ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth().clickable { onOpenConversation(conv.id) }.testTag("recent_${conv.id}")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(conv.displayTitle, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(3.dp))
                        Text(
                            listOfNotNull(conv.repository, formatRelativeTime(conv.updatedAt).ifBlank { null }).joinToString(" · "),
                            fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    StatusBadge(conv.status)
                }
            }
        }

        // Templates
        if (templates.isNotEmpty()) {
            item {
                SectionTitle("Templates")
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(templates, key = { it.id }) { t ->
                        Surface(
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)),
                            modifier = Modifier.clickable { onSelectTemplate(t) }.testTag("home_template_${t.id}")
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp).width(160.dp)) {
                                Text(t.title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(t.prompt, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}
