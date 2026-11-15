package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.local.AppDatabase
import com.example.data.repository.AgentServerRepository
import com.example.data.repository.OpenHandsRepository
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Live smoke test against the real OpenHands Cloud API. Runs on the emulator in
 * CI where `OPENHANDS_API_KEY` is injected as a BuildConfig field (via the secrets
 * gradle plugin reading `.env`). Skipped entirely when no key is present, so local
 * runs without a key are unaffected.
 *
 * The flow exercises the exact call chain the UI uses: health check -> list ->
 * start -> await sandbox -> send message -> runtime ops (server info, bash,
 * git changes, condense) -> delete. The conversation is always deleted in
 * [tearDown] so repeated runs don't leak sandboxes.
 */
@RunWith(AndroidJUnit4::class)
class LiveCloudApiTest {

    private lateinit var context: Context
    private lateinit var repo: OpenHandsRepository
    private lateinit var runtime: AgentServerRepository
    private var conversationId: String? = null

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // BuildConfig.OPENHANDS_API_KEY only exists when the secrets plugin
        // actually emitted it (non-blank .env value). Reflectively probe so the
        // app still compiles when the field is absent (blank key / placeholder).
        val key = try {
            com.example.BuildConfig::class.java.getField("OPENHANDS_API_KEY").get(null) as String
        } catch (_: NoSuchFieldException) {
            ""
        }
        assumeTrue(
            "OPENHANDS_API_KEY not configured — skipping live test",
            key.isNotBlank() && key != "OPENHANDS_API_KEY_PLACEHOLDER"
        )
        val db = AppDatabase.getInstance(context)
        repo = OpenHandsRepository(context, db)
        repo.updateConfig(key, OpenHandsRepository.DEFAULT_BASE_URL)
        runtime = AgentServerRepository(db.conversationDao())
    }

    @After
    fun tearDown() {
        val id = conversationId ?: return
        runBlocking { repo.deleteConversation(id) }
    }

    @Test
    fun liveCloudRoundTrip() = runBlocking {
        // 1. Health / auth
        val health = repo.checkHealth()
        assertTrue("health failed: $health", health.isSuccess)

        // 2. List conversations (auth works, endpoint reachable)
        val list = repo.refreshConversations()
        assertTrue("list failed: $list", list.isSuccess)

        // 3. Start a real conversation on a tiny repo with a trivial prompt
        val started = repo.startConversation(
            prompt = "Reply with exactly: LIVE_OK",
            repository = "https://github.com/Chaim12345/ohaa.git",
            branch = "main",
        )
        assertTrue("start failed: $started", started.isSuccess)
        val taskId = started.getOrThrow().taskId

        // 4. Wait for the sandbox to come up (start-task poll loop)
        val conv = withTimeout(90_000) { repo.awaitConversationId(taskId) }
        assertTrue("await failed: $conv", conv.isSuccess)
        conversationId = conv.getOrThrow()
        val id = conversationId!!

        // 5. The conversation is cached locally with a runtime URL + session key
        val cached = repo.refreshConversation(id)
        assertTrue("refresh failed: $cached", cached.isSuccess)
        val model = cached.getOrThrow()
        assertTrue("no runtime url", model?.hasRuntimeAccess == true)

        // 6. Send a message through the cloud endpoint
        val sent = repo.sendMessage(id, "Reply with exactly: LIVE_OK")
        assertTrue("send failed: $sent", sent.isSuccess)

        // 7. Runtime (agent-server) operations against the live sandbox
        val info = runtime.serverInfo(id)
        assertTrue("serverInfo failed: $info", info.isSuccess)

        val bash = runtime.executeBash(id, "echo LIVE_OK")
        assertTrue("executeBash failed: $bash", bash.isSuccess)

        val changes = runtime.gitChanges(id)
        assertTrue("gitChanges failed: $changes", changes.isSuccess)

        // 8. Context compaction endpoint (no cloud equivalent)
        val condensed = runtime.condense(id)
        assertTrue("condense failed: $condensed", condensed.isSuccess)

        // 9. Events actually streamed back
        val events = repo.refreshEvents(id)
        assertTrue("events failed: $events", events.isSuccess)
        assertTrue("expected at least one event", events.getOrThrow().isNotEmpty())
    }
}
