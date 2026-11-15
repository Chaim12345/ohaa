package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.local.AppDatabase
import com.example.data.repository.AgentServerRepository
import com.example.data.repository.OpenHandsRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Live smoke test against the real OpenHands Cloud API. Runs on the emulator in
 * CI where `OPENHANDS_API_KEY` and `OPENHANDS_TEST_CONVERSATION_ID` are injected
 * as BuildConfig fields (via the secrets gradle plugin reading `.env`). Skipped
 * entirely when either is absent, so local runs without secrets are unaffected.
 *
 * Owner rule: tests must reuse ONE pinned conversation, never create a new one
 * per run. The pinned conversation id comes from the `OPENHANDS_TEST_CONVERSATION_ID`
 * repo variable; the test never starts or deletes conversations. Set up the
 * conversation once by hand in the app, copy its id into the repo variable, and
 * every CI run exercises the same sandbox.
 *
 * The flow exercises the call chain the UI uses against that conversation:
 * health check -> list -> refresh -> send message -> runtime ops (server info,
 * bash, git changes, condense) -> events.
 */
@RunWith(AndroidJUnit4::class)
class LiveCloudApiTest {

    private lateinit var context: Context
    private lateinit var repo: OpenHandsRepository
    private lateinit var runtime: AgentServerRepository
    private var conversationId: String? = null

    private fun buildConfigField(name: String): String = try {
        com.example.BuildConfig::class.java.getField(name).get(null) as String
    } catch (_: NoSuchFieldException) {
        ""
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // BuildConfig fields only exist when the secrets plugin actually emitted
        // them (non-blank .env values). Reflectively probe so the app still
        // compiles when the fields are absent (blank key / placeholder).
        val key = buildConfigField("OPENHANDS_API_KEY")
        assumeTrue(
            "OPENHANDS_API_KEY not configured — skipping live test",
            key.isNotBlank() && key != "OPENHANDS_API_KEY_PLACEHOLDER"
        )
        val pinned = buildConfigField("OPENHANDS_TEST_CONVERSATION_ID")
        assumeTrue(
            "OPENHANDS_TEST_CONVERSATION_ID not configured — live tests reuse one " +
                "pinned conversation and never create new ones. Create a conversation " +
                "once in the app and put its id in the repo variable.",
            pinned.isNotBlank() && pinned != "OPENHANDS_TEST_CONVERSATION_ID_PLACEHOLDER"
        )
        conversationId = pinned
        val db = AppDatabase.getInstance(context)
        repo = OpenHandsRepository(context, db)
        repo.updateConfig(key, OpenHandsRepository.DEFAULT_BASE_URL)
        runtime = AgentServerRepository(db.conversationDao())
    }

    @Test
    fun liveCloudRoundTrip() = runBlocking {
        // 1. Health / auth
        val health = repo.checkHealth()
        assertTrue("health failed: $health", health.isSuccess)

        // 2. List conversations (auth works, endpoint reachable)
        val list = repo.refreshConversations()
        assertTrue("list failed: $list", list.isSuccess)

        // 3. Reuse the pinned conversation - never start a new one per run
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
