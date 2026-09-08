package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Conversation
import com.example.data.model.GitChange
import com.example.data.model.GitDiff
import com.example.ui.components.CodeBlock
import com.example.ui.components.EmptyState
import com.example.ui.components.ErrorBanner
import com.example.ui.components.Pill
import com.example.ui.theme.OpenHandsPrimaryDark
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusReady
import com.example.ui.theme.StatusWaiting
import com.example.ui.viewmodel.WorkspaceState

@Composable
private fun SubScreenTopBar(title: String, subtitle: String?, onBack: () -> Unit, actions: @Composable () -> Unit = {}) {
    Surface(color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface) }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle != null) Text(subtitle, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            actions()
        }
    }
}

/** Browses the sandbox workspace via GET /files and GET /file. */
@Composable
fun FilesScreen(
    conversation: Conversation?,
    state: WorkspaceState,
    onBack: () -> Unit,
    onBrowse: (String?) -> Unit,
    onOpenFile: (String) -> Unit,
    onCloseFile: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        SubScreenTopBar(
            title = if (state.openFilePath != null) state.openFilePath.substringAfterLast('/') else "Workspace files",
            subtitle = state.openFilePath ?: (state.path ?: "/ (workspace root)"),
            onBack = { if (state.openFilePath != null) onCloseFile() else onBack() }
        ) {
            IconButton(onClick = { if (state.openFilePath != null) onOpenFile(state.openFilePath) else onBrowse(state.path) }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.primary)
            }
        }
        if (state.isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp), color = OpenHandsPrimaryDark)
        state.error?.let { ErrorBanner(it, modifier = Modifier.padding(12.dp), actionLabel = "Retry", onAction = { onBrowse(state.path) }) }

        if (state.openFilePath != null && state.openFileContent != null) {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
                CodeBlock(text = state.openFileContent, label = state.openFilePath, maxCollapsedLines = 400)
            }
            return
        }

        val parent = state.path?.trimEnd('/')?.substringBeforeLast('/', missingDelimiterValue = "")?.ifEmpty { null }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            if (state.path != null) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { onBrowse(parent) }.padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("..", fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
            if (state.entries.isEmpty() && !state.isLoading && state.error == null) {
                item {
                    EmptyState(
                        "Empty directory",
                        if (conversation?.sandboxStatus?.name == "RUNNING") null else "The sandbox must be running to list files. Resume it from the conversation menu."
                    )
                }
            }
            items(state.entries, key = { it }) { entry ->
                val isDir = entry.endsWith("/")
                val name = entry.trimEnd('/').substringAfterLast('/')
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                        .clickable { if (isDir) onBrowse(entry) else onOpenFile(entry) }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .testTag("file_$name")
                ) {
                    Icon(
                        if (isDir) Icons.Default.Folder else Icons.AutoMirrored.Filled.InsertDriveFile,
                        contentDescription = null,
                        tint = if (isDir) OpenHandsPrimaryDark else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(name, fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/** Shows GET /git/changes for the repo root and GET /git/diff for a selected file. */
@Composable
fun ChangesScreen(
    conversation: Conversation?,
    state: WorkspaceState,
    onBack: () -> Unit,
    onRepoRootChange: (String) -> Unit,
    onReload: () -> Unit,
    onOpenDiff: (GitChange) -> Unit,
    onCloseDiff: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        SubScreenTopBar(
            title = state.selectedDiff?.path?.substringAfterLast('/') ?: "Git changes",
            subtitle = state.selectedDiff?.path ?: conversation?.repository,
            onBack = { if (state.selectedDiff != null) onCloseDiff() else onBack() }
        ) {
            IconButton(onClick = onReload) { Icon(Icons.Default.Refresh, contentDescription = "Reload", tint = MaterialTheme.colorScheme.primary) }
        }
        if (state.isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp), color = OpenHandsPrimaryDark)

        val diff = state.selectedDiff
        if (diff != null) {
            DiffView(diff)
            return
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            OutlinedTextField(
                value = state.repoRoot,
                onValueChange = onRepoRootChange,
                label = { Text("Repository path inside the sandbox") },
                singleLine = true,
                trailingIcon = { TextButton(onClick = onReload) { Text("Load") } },
                modifier = Modifier.fillMaxWidth().testTag("repo_root_input")
            )
            Text(
                "Usually /workspace/<repo-name>. Cloned repositories live under /workspace.",
                fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp)
            )
        }
        state.error?.let { ErrorBanner(it, modifier = Modifier.padding(horizontal = 12.dp), actionLabel = "Retry", onAction = onReload) }

        if (state.changes.isEmpty() && !state.isLoading && state.error == null) {
            EmptyState("No uncommitted changes", "The working tree at ${state.repoRoot} is clean, or the path is not a git repository.")
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(state.changes, key = { it.path }) { change ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { onOpenDiff(change) }.padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Pill(
                        text = change.statusLabel,
                        color = when (change.status.uppercase()) { "A" -> StatusReady; "D" -> StatusError; else -> StatusWaiting },
                        icon = null
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(change.path, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun DiffView(diff: GitDiff) {
    val unified = remember(diff) { unifiedDiff(diff.original, diff.modified) }
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
        if (unified.isBlank()) {
            EmptyState("No textual difference", "The file content is identical, or the file is binary.")
        } else {
            CodeBlock(text = unified, label = "diff", maxCollapsedLines = 600, colorizeDiff = true)
        }
    }
}

@Composable
private fun <T> remember(key: T, calc: () -> String): String = androidx.compose.runtime.remember(key) { calc() }

/** Minimal LCS-based line diff producing +/- lines with a little context. */
internal fun unifiedDiff(original: String, modified: String, context: Int = 3): String {
    val a = original.lines()
    val b = modified.lines()
    if (a == b) return ""
    val n = a.size
    val m = b.size
    // LCS table (bounded to keep memory sane for big files)
    if (n.toLong() * m.toLong() > 4_000_000L) {
        return buildString {
            a.forEach { append("- ").append(it).append('\n') }
            b.forEach { append("+ ").append(it).append('\n') }
        }.trimEnd()
    }
    val dp = Array(n + 1) { IntArray(m + 1) }
    for (i in n - 1 downTo 0) for (j in m - 1 downTo 0) {
        dp[i][j] = if (a[i] == b[j]) dp[i + 1][j + 1] + 1 else maxOf(dp[i + 1][j], dp[i][j + 1])
    }
    data class Line(val tag: Char, val text: String)
    val out = mutableListOf<Line>()
    var i = 0
    var j = 0
    while (i < n && j < m) {
        when {
            a[i] == b[j] -> { out += Line(' ', a[i]); i++; j++ }
            dp[i + 1][j] >= dp[i][j + 1] -> { out += Line('-', a[i]); i++ }
            else -> { out += Line('+', b[j]); j++ }
        }
    }
    while (i < n) { out += Line('-', a[i]); i++ }
    while (j < m) { out += Line('+', b[j]); j++ }

    // Keep only changed lines plus surrounding context.
    val keep = BooleanArray(out.size)
    out.forEachIndexed { idx, l -> if (l.tag != ' ') for (k in maxOf(0, idx - context)..minOf(out.size - 1, idx + context)) keep[k] = true }
    val sb = StringBuilder()
    var lastKept = -2
    out.forEachIndexed { idx, l ->
        if (!keep[idx]) return@forEachIndexed
        if (idx != lastKept + 1) sb.append("@@ …\n")
        sb.append(l.tag).append(' ').append(l.text).append('\n')
        lastKept = idx
    }
    return sb.toString().trimEnd()
}
