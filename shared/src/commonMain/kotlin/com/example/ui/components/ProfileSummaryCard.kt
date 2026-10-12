package com.example.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.settings.AgentProfile

/**
 * Read-only row for the New Task screen: shows which agent + LLM currently
 * drives new launches, with a button to open the switcher dialog.
 */
@Composable
fun ProfileSummaryCard(
    profile: AgentProfile,
    onEdit: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("profile_summary_card")
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
        ) {
            androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                Text(
                    "Agent & LLM",
                    fontSize = 12.sp,
                    style = androidx.compose.material3.MaterialTheme.typography.labelLarge
                )
                Text(
                    profile.shortLabel(),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
            Spacer(Modifier.width(2.dp))
            Button(
                onClick = onEdit,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("btn_edit_profile")
            ) { Text("Change") }
        }
    }
}