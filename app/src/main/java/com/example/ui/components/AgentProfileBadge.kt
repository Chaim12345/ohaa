package com.example.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.settings.AgentProfile

/**
 * Compact "who is driving this conversation" badge shown in the chat header.
 * Tapping it opens the [AgentProfileDialog] where the user can switch the
 * agent backend and LLM — mid-session for the open conversation.
 */
@Composable
fun AgentProfileBadge(
    profile: AgentProfile,
    onOpen: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(top = 2.dp)
            .testTag("agent_profile_badge")
    ) {
        IconButton(
            onClick = onOpen,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                Icons.Default.SmartToy,
                contentDescription = "Switch agent / LLM",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(15.dp)
            )
        }
        Spacer(Modifier.width(2.dp))
        Text(
            profile.shortLabel(),
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.testTag("agent_profile_label")
        )
    }
}