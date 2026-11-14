package com.example.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.ConversationEntity
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Exercises [AgentServerRepository] against a real HTTP server. The repository
 * resolves the sandbox host and session key from Room, so the test seeds a
 * conversation row and then asserts the exact runtime URL and headers used.
 */
@RunWith(RobolectricTestRunner::class)
class AgentServerRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var database: AppDatabase
    private lateinit var repository: AgentServerRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = AgentServerRepository(database.conversationDao())
    }

    @After
    fun tearDown() {
        database.close()
        server.shutdown()
    }

    private fun seed(conversationId: String = "conv-1") = runBlocking {
        database.conversationDao().insertConversation(
            ConversationEntity(
                id = conversationId,
                title = "t",
                status = "RUNNING",
                selectedRepository = "org/repo",
                createdAt = 1L,
                updatedAt = 1L,
                sandboxId = "sbx-1",
                sandboxStatus = "RUNNING",
                conversationUrl = server.url("/").toString().trimEnd('/'),
                sessionApiKey = "session-secret",
            )
        )
    }

    @Test
    fun `execute bash posts to the runtime and returns stdout`() = runBlocking {
        seed()
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("""{"command_id":"c1","stdout":"hello\n","stderr":"","exit_code":0}""")
        )

        val result = repository.executeBash("conv-1", "echo hello")

        assertTrue(result.isSuccess)
        assertEquals("hello\n", result.getOrNull()?.stdout)
        val recorded = server.takeRequest()
        assertEquals("/api/bash/execute_bash_command", recorded.path)
        assertEquals("session-secret", recorded.getHeader("X-Session-API-Key"))
        assertTrue(recorded.body.readUtf8().contains("\"command\":\"echo hello\""))
    }

    @Test
    fun `condense hits the condense endpoint`() = runBlocking {
        seed()
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"success":true}"""))

        val result = repository.condense("conv-1")

        assertTrue(result.isSuccess)
        assertEquals("/api/conversations/conv-1/condense", server.takeRequest().path)
    }

    @Test
    fun `switch acp model posts the model id`() = runBlocking {
        seed()
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"success":true}"""))

        val result = repository.switchAcpModel("conv-1", "claude-sonnet-4-5")

        assertTrue(result.isSuccess)
        val recorded = server.takeRequest()
        assertEquals("/api/conversations/conv-1/switch_acp_model", recorded.path)
        assertTrue(recorded.body.readUtf8().contains("claude-sonnet-4-5"))
    }

    @Test
    fun `missing runtime url fails fast without a request`() = runBlocking {
        database.conversationDao().insertConversation(
            ConversationEntity(
                id = "conv-2",
                title = "t",
                status = "RUNNING",
                selectedRepository = null,
                createdAt = 1L,
                updatedAt = 1L,
                sandboxId = "sbx-2",
                sandboxStatus = "PAUSED",
            )
        )

        val result = repository.executeBash("conv-2", "ls")

        assertTrue(result.isFailure)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `git changes maps status and path`() = runBlocking {
        seed()
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("""[{"status":"MODIFIED","path":"src/Main.kt"}]""")
        )

        val result = repository.gitChanges("conv-1")

        assertTrue(result.isSuccess)
        val changes = result.getOrDefault(emptyList())
        assertEquals(1, changes.size)
        assertEquals("src/Main.kt", changes.first().path)
        assertEquals("MODIFIED", changes.first().status)
        assertEquals("/api/git/changes", server.takeRequest().path)
    }

    @Test
    fun `goal start posts the goal text`() = runBlocking {
        seed()
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"success":true}"""))

        val result = repository.startGoal("conv-1", "ship the feature")

        assertTrue(result.isSuccess)
        val recorded = server.takeRequest()
        assertEquals("/api/conversations/conv-1/goal", recorded.path)
        assertTrue(recorded.body.readUtf8().contains("ship the feature"))
    }

    @Test
    fun `goal stop hits the stop endpoint`() = runBlocking {
        seed()
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"success":true}"""))

        repository.stopGoal("conv-1")

        assertEquals("/api/conversations/conv-1/goal/stop", server.takeRequest().path)
    }

    @Test
    fun `confirmation response posts the accept flag`() = runBlocking {
        seed()
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"success":true}"""))

        repository.respondToConfirmation("conv-1", accept = false, reason = "unsafe")

        val recorded = server.takeRequest()
        assertEquals("/api/conversations/conv-1/events/respond_to_confirmation", recorded.path)
        assertTrue(recorded.body.readUtf8().contains("\"accept\":false"))
    }

    @Test
    fun `fork posts to the fork endpoint`() = runBlocking {
        seed()
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"id":"conv-2"}"""))

        val result = repository.forkConversation("conv-1")

        assertTrue(result.isSuccess)
        assertEquals("/api/conversations/conv-1/fork", server.takeRequest().path)
    }

    @Test
    fun `agent final response decodes the text`() = runBlocking {
        seed()
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"response":"done","status":"finished"}"""))

        val result = repository.agentFinalResponse("conv-1")

        assertEquals("done", result.getOrNull()?.response)
        assertEquals("/api/conversations/conv-1/agent_final_response", server.takeRequest().path)
    }

    @Test
    fun `runtime info decodes the runtime dto`() = runBlocking {
        seed()
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"id":"conv-1","status":"running"}"""))

        val result = repository.runtimeInfo("conv-1")

        assertEquals("running", result.getOrNull()?.status)
        assertEquals("/api/conversations/conv-1/runtime", server.takeRequest().path)
    }

    @Test
    fun `vscode url decodes the url`() = runBlocking {
        seed()
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"url":"https://vs.code/x"}"""))

        val result = repository.vscodeUrl("conv-1")

        assertEquals("https://vs.code/x", result.getOrNull()?.url)
        assertEquals("/api/vscode/url", server.takeRequest().path)
    }

    @Test
    fun `installed skills unwraps the list`() = runBlocking {
        seed()
        server.enqueue(
            MockResponse().setResponseCode(200).setBody("""{"skills":[{"name":"pdf","enabled":true}]}""")
        )

        val result = repository.installedSkills("conv-1")

        assertEquals("pdf", result.getOrNull()?.single()?.name)
        assertEquals("/api/skills/installed", server.takeRequest().path)
    }

    @Test
    fun `install skill posts the name`() = runBlocking {
        seed()
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"success":true}"""))

        repository.installSkill("conv-1", "pdf")

        val recorded = server.takeRequest()
        assertEquals("/api/skills/install", recorded.path)
        assertTrue(recorded.body.readUtf8().contains("pdf"))
    }

    @Test
    fun `tool catalog unwraps the tools`() = runBlocking {
        seed()
        server.enqueue(
            MockResponse().setResponseCode(200).setBody("""{"tools":[{"name":"bash","description":"run"}]}""")
        )

        val result = repository.toolCatalog("conv-1")

        assertEquals("bash", result.getOrNull()?.single()?.name)
        assertEquals("/api/tools/catalog", server.takeRequest().path)
    }

    @Test
    fun `bash events unwraps the page`() = runBlocking {
        seed()
        server.enqueue(
            MockResponse().setResponseCode(200).setBody("""{"items":[{"id":"e1","kind":"BashOutput","stdout":"hi"}]}""")
        )

        val result = repository.bashEvents("conv-1")

        assertEquals("e1", result.getOrNull()?.items?.single()?.id)
        assertEquals("/api/bash/bash_events/search?limit=100", server.takeRequest().path)
    }

    @Test
    fun `create directory sends the path query`() = runBlocking {
        seed()
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"success":true}"""))

        repository.createDirectory("conv-1", "src/new")

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/api/file/create_directory?path=src%2Fnew", recorded.path)
    }

    @Test
    fun `runtime providers unwraps the list`() = runBlocking {
        seed()
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"providers":[{"name":"openai"}]}"""))

        val result = repository.runtimeProviders("conv-1")

        assertEquals("openai", result.getOrNull()?.single()?.name)
        assertEquals("/api/llm/providers", server.takeRequest().path)
    }

    @Test
    fun `start bash posts the command and decodes the command id`() = runBlocking {
        seed()
        server.enqueue(
            MockResponse().setResponseCode(200).setBody("""{"id":"cmd-1","command":"ls","kind":"BashCommand"}""")
        )

        val result = repository.startBash("conv-1", "ls")

        assertEquals("cmd-1", result.getOrNull()?.id)
        val recorded = server.takeRequest()
        assertEquals("/api/bash/start_bash_command", recorded.path)
        assertTrue(recorded.body.readUtf8().contains("\"command\":\"ls\""))
    }

    @Test
    fun `clear bash events issues a delete`() = runBlocking {
        seed()
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"success":true}"""))

        val result = repository.clearBashEvents("conv-1")

        assertTrue(result.isSuccess)
        val recorded = server.takeRequest()
        assertEquals("DELETE", recorded.method)
        assertEquals("/api/bash/bash_events", recorded.path)
    }

    @Test
    fun `sub agents unwraps the list`() = runBlocking {
        seed()
        server.enqueue(
            MockResponse().setResponseCode(200).setBody("""{"agents":[{"name":"reviewer","description":"reviews"}]}""")
        )

        val result = repository.listSubAgents("conv-1")

        assertEquals("reviewer", result.getOrNull()?.single()?.name)
        assertEquals("/api/sub-agents", server.takeRequest().path)
    }

    @Test
    fun `list plugins posts a plugins request`() = runBlocking {
        seed()
        server.enqueue(
            MockResponse().setResponseCode(200).setBody("""{"plugins":[{"name":"p1","enabled":true}]}""")
        )

        val result = repository.listPlugins("conv-1")

        assertEquals("p1", result.getOrNull()?.single()?.name)
        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/api/plugins", recorded.path)
    }

    @Test
    fun `fetch plugin posts the source`() = runBlocking {
        seed()
        server.enqueue(
            MockResponse().setResponseCode(200).setBody("""{"path":"/x/p","resolved_ref":"abc"}""")
        )

        val result = repository.fetchPlugin("conv-1", "https://github.com/x/y")

        assertEquals("abc", result.getOrNull()?.resolvedRef)
        val recorded = server.takeRequest()
        assertEquals("/api/plugins/fetch", recorded.path)
        assertTrue(recorded.body.readUtf8().contains("github.com/x/y"))
    }

    @Test
    fun `mcp oauth start posts the server name`() = runBlocking {
        seed()
        server.enqueue(
            MockResponse().setResponseCode(200).setBody("""{"ok":true,"job_id":"j9","authorization_url":"https://a"}""")
        )

        val result = repository.startMcpOAuth("conv-1", "weather")

        assertEquals("j9", result.getOrNull()?.jobId)
        val recorded = server.takeRequest()
        assertEquals("/api/mcp/oauth/start", recorded.path)
        assertTrue(recorded.body.readUtf8().contains("weather"))
    }

    @Test
    fun `load plugin posts to the conversation load_plugin endpoint`() = runBlocking {
        seed()
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"success":true}"""))

        repository.loadConversationPlugin("conv-1", "https://github.com/x/y")

        assertEquals("/api/conversations/conv-1/load_plugin", server.takeRequest().path)
    }

    @Test
    fun `start child conversation posts the task`() = runBlocking {
        seed()
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"id":"child-1"}"""))

        val result = repository.startChildConversation("conv-1", "do the thing")

        assertEquals("child-1", result.getOrNull()?.id)
        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/api/conversations/conv-1/children", recorded.path)
        assertTrue(recorded.body.readUtf8().contains("do the thing"))
    }

    @Test
    fun `children returns the list`() = runBlocking {
        seed()
        server.enqueue(MockResponse().setResponseCode(200).setBody("""[{"id":"child-1"},{"id":"child-2"}]"""))

        val result = repository.children("conv-1")

        assertEquals(listOf("child-1", "child-2"), result.getOrNull()?.map { it.id })
        assertEquals("/api/conversations/conv-1/children", server.takeRequest().path)
    }

    @Test
    fun `stop bash command hits the command stop endpoint`() = runBlocking {
        seed()
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"success":true}"""))

        repository.stopBashCommand("conv-1", "cmd-9")

        assertEquals("/api/bash/bash_commands/cmd-9/stop", server.takeRequest().path)
    }

    @Test
    fun `navigate posts the event id`() = runBlocking {
        seed()
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"id":"conv-1"}"""))

        repository.navigateConversation("conv-1", "evt-3")

        val recorded = server.takeRequest()
        assertEquals("/api/conversations/conv-1/navigate", recorded.path)
        assertTrue(recorded.body.readUtf8().contains("evt-3"))
    }

    // ---- newly added surfaces ---------------------------------------------

    @Test
    fun `list canvas extensions hits the installed endpoint`() = runBlocking {
        seed()
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"canvas_extensions":[]}"""))

        val result = repository.canvasExtensions("conv-1")

        assertTrue(result.isSuccess)
        assertEquals("/api/canvas-extensions/installed", server.takeRequest().path)
    }

    @Test
    fun `install canvas extension posts the source`() = runBlocking {
        seed()
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"name":"demo","source":"gh/x"}"""))

        repository.installCanvasExtension("conv-1", source = "gh/x", ref = "main", force = true)

        val recorded = server.takeRequest()
        assertEquals("/api/canvas-extensions/install", recorded.path)
        val body = recorded.body.readUtf8()
        assertTrue(body.contains("\"source\":\"gh/x\""))
        assertTrue(body.contains("\"ref\":\"main\""))
        assertTrue(body.contains("\"force\":true"))
    }

    @Test
    fun `start canvas backend posts to the extension backend start route`() = runBlocking {
        seed()
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"name":"demo","state":"running"}"""))

        repository.startCanvasBackend("conv-1", "demo", revision = "r1")

        val recorded = server.takeRequest()
        assertEquals("/api/canvas-extensions/installed/demo/backend/start", recorded.path)
        assertTrue(recorded.body.readUtf8().contains("\"revision\":\"r1\""))
    }

    @Test
    fun `conversation scoped git changes hits the conversation route with path`() = runBlocking {
        seed()
        server.enqueue(MockResponse().setResponseCode(200).setBody("""[{"status":"UPDATED","path":"a.txt"}]"""))

        val result = repository.conversationGitChanges("conv-1", path = "/workspace/project")

        assertEquals(listOf("a.txt"), result.getOrNull()?.map { it.path })
        val recorded = server.takeRequest()
        assertTrue(recorded.path!!.startsWith("/api/conversations/conv-1/git/changes"))
        assertTrue(recorded.path!!.contains("path="))
    }

    @Test
    fun `conversation scoped git diff hits the conversation route`() = runBlocking {
        seed()
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"original":"a","modified":"b"}"""))

        repository.conversationGitDiff("conv-1", path = "/workspace")

        assertEquals("/api/conversations/conv-1/git/diff?path=%2Fworkspace", server.takeRequest().path)
    }

    @Test
    fun `download conversation trajectory hits the trajectory route`() = runBlocking {
        seed()
        server.enqueue(MockResponse().setResponseCode(200).setBody("traj-bytes"))

        val result = repository.downloadConversationTrajectory("conv-1")

        assertTrue(result.isSuccess)
        assertEquals(
            "/api/conversations/conv-1/file/download-trajectory/conv-1",
            server.takeRequest().path,
        )
    }
}
