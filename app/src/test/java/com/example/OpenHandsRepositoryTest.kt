package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.api.ApiClient
import com.example.data.api.ApiSettings
import com.example.data.local.AppDatabase
import com.example.data.repository.CloudError
import com.example.data.repository.OpenHandsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OpenHandsRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var db: AppDatabase
    private lateinit var settings: ApiSettings
    private lateinit var repo: OpenHandsRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        settings = ApiSettings(context)
        settings.apiKey = "test-key-123"
        val api = ApiClient.create(
            baseUrlProvider = { server.url("/").toString() },
            apiKeyProvider = { settings.apiKey },
            enableLogging = false
        )
        repo = OpenHandsRepository(
            settings = settings,
            database = db,
            apiProvider = { api },
            now = { 1_000_000L }
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
        db.close()
    }

    @Test
    fun `createConversation persists optimistic row and returns id`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {"id":"task-1","status":"WORKING","app_conversation_id":"conv-1"}
                """.trimIndent()
            ).addHeader("Content-Type", "application/json")
        )

        val result = repo.createConversation("Fix the login bug", "owner/repo")
        assertTrue(result.isSuccess)
        assertEquals("conv-1", result.getOrThrow())

        val stored = repo.conversationsFlow.first()
        assertEquals(1, stored.size)
        assertEquals("Fix the login bug", stored[0].title)
        assertEquals("owner/repo", stored[0].repository)
    }

    @Test
    fun `unauthorized 401 maps to CloudError Unauthorized`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401).setBody("{}"))

        val result = repo.refreshConversations()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is CloudError.Unauthorized)
    }

    @Test
    fun `network failure maps to CloudError Network and cache survives`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody("""{"items": [], "next_page_id": null}""")
                .addHeader("Content-Type", "application/json")
        )
        repo.refreshConversations()
        server.shutdown()

        val result = repo.refreshConversations()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is CloudError.Network)

        // Local cache still readable (offline-first)
        val cached = repo.conversationsFlow.first()
        assertEquals(0, cached.size)
    }

    @Test
    fun `refreshConversations stores cloud rows with raw statuses`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {"items": [
                  {"id":"c1","title":"Fix README","sandbox_status":"RUNNING","execution_status":"finished","metrics":{"accumulated_cost":0.08}},
                  {"id":"c2","title":"Rate limit","sandbox_status":"STARTING"}
                ], "next_page_id": null}
                """.trimIndent()
            ).addHeader("Content-Type", "application/json")
        )

        val result = repo.refreshConversations()
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrThrow().size)
    }

    @Test
    fun `refreshConversations requests the search surface not batch get`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody("""{"items": [], "next_page_id": null}""")
                .addHeader("Content-Type", "application/json")
        )
        repo.refreshConversations()
        val request = server.takeRequest()
        assertTrue(request.path!!.startsWith("/api/v1/app-conversations/search"))
    }

    @Test
    fun `pollStartTask returns the matching task`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """[{"id":"task-9","status":"READY","app_conversation_id":"conv-9"}]"""
            ).addHeader("Content-Type", "application/json")
        )

        val result = repo.pollStartTask("task-9")
        assertTrue(result.isSuccess)
        assertEquals("READY", result.getOrThrow().status)
    }

    @Test
    fun `seeding never fabricates cloud conversations`() = runTest {
        settings.apiKey = ""
        repo.seedLocalData()
        val stored = repo.conversationsFlow.first()
        assertTrue("Expected no fabricated conversations, got ${stored.size}", stored.isEmpty())
    }

    @Test
    fun `templates are seeded as a local feature`() = runTest {
        settings.apiKey = ""
        repo.seedLocalData()
        assertTrue(repo.templatesFlow.first().isNotEmpty())
    }

    @Test
    fun `sandbox pause hits the documented endpoint`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody("{}")
                .addHeader("Content-Type", "application/json")
        )
        val result = repo.pauseSandbox("sbx-1")
        assertTrue(result.isSuccess)
        assertEquals("/api/v1/sandboxes/sbx-1/pause", server.takeRequest().path)
    }

    @Test
    fun `workspace file listing calls the conversation files route`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody("""["a.py","b.py"]""")
                .addHeader("Content-Type", "application/json")
        )
        val result = repo.listFiles("conv-7")
        assertTrue(result.isSuccess)
        assertEquals(listOf("a.py", "b.py"), result.getOrThrow())
        assertTrue(server.takeRequest().path!!.startsWith("/api/v1/app-conversations/conv-7/files"))
    }

    @Test
    fun `conversation rename sends a PATCH with the new title`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"id":"conv-3","title":"Renamed","created_at":"2025-01-01T00:00:00Z"}"""
            ).addHeader("Content-Type", "application/json")
        )
        val result = repo.renameConversation("conv-3", "Renamed")
        assertTrue(result.isSuccess)
        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertTrue(request.body.readUtf8().contains("Renamed"))
    }

    @Test
    fun `model catalogue is fetched from the config models route`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"items":[{"name":"openhands/gpt-5","provider":"openai","verified":true},
                            {"name":"x/draft","provider":"x","verified":false}]}"""
            ).addHeader("Content-Type", "application/json")
        )
        val result = repo.searchModels(limit = 10)
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrThrow().size)
        assertTrue(server.takeRequest().path!!.startsWith("/api/v1/config/models/search"))
    }

    @Test
    fun `suggested tasks are fetched from the git route`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"items":[{"git_provider":"github","task_type":"FAILING_CHECKS","repo":"o/r","issue_number":12,"title":"Fix CI"}]}"""
            ).addHeader("Content-Type", "application/json")
        )
        val result = repo.suggestedTasks()
        assertTrue(result.isSuccess)
        assertEquals("Fix CI", result.getOrThrow().first().title)
        assertTrue(server.takeRequest().path!!.startsWith("/api/v1/git/suggested-tasks/search"))
    }

    @Test
    fun `events refresh parses spec page and stores rows`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {"items":[{"id":"e1","source":"user","kind":"MessageEvent",
                  "llm_message":{"role":"user","content":[{"type":"text","text":"hello"}]}}],
                 "next_page_id":null}
                """.trimIndent()
            ).addHeader("Content-Type", "application/json")
        )

        val result = repo.refreshEvents("conv-x")
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow().size)
        val stored = repo.getEventsFlow("conv-x").first()
        assertEquals("hello", stored[0].content)
    }

    @Test
    fun `sendMessage posts spec body with role content and run`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"success":true,"sandbox_status":"RUNNING","message":null}"""
            ).addHeader("Content-Type", "application/json")
        )
        val result = repo.sendMessage("conv-1", "do the thing")
        assertTrue(result.isSuccess)
        val request = server.takeRequest()
        assertEquals("/api/v1/app-conversations/conv-1/send-message", request.path)
        val body = request.body.readUtf8()
        assertTrue("\"role\":\"user\"" in body.replace(" ", ""))
        assertTrue("\"run\":true" in body.replace(" ", ""))
    }

    @Test
    fun `switchProfile posts profile_name per spec`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody("""{"success":true}""")
                .addHeader("Content-Type", "application/json")
        )
        val result = repo.switchProfile("conv-1", "sonnet")
        assertTrue(result.isSuccess)
        val request = server.takeRequest()
        assertEquals("/api/v1/app-conversations/conv-1/switch_profile", request.path)
        val body = request.body.readUtf8()
        assertTrue("\"profile_name\":\"sonnet\"" in body.replace(" ", ""))
    }

    @Test
    fun `gitChanges hits required path query and maps list response`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """[{"path":"docs/API_SPEC.md","status":"modified"}]"""
            ).addHeader("Content-Type", "application/json")
        )
        val result = repo.gitChanges("conv-1")
        assertTrue(result.isSuccess)
        val requestPath = server.takeRequest().path ?: ""
        assertTrue(requestPath.contains("/git/changes"))
        assertTrue("path=%2Fworkspace%2Fproject" in requestPath)
        assertEquals(1, result.getOrThrow().files?.size)
        assertEquals("docs/API_SPEC.md", result.getOrThrow().files?.get(0)?.displayName)
    }

    @Test
    fun `auth header uses the documented X-Access-Token scheme`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody("""{"items": [], "next_page_id": null}""")
                .addHeader("Content-Type", "application/json")
        )
        repo.refreshConversations()
        val request = server.takeRequest()
        assertEquals("test-key-123", request.getHeader("X-Access-Token"))
    }

    @Test
    fun `listProfiles parses profiles array from wrapper`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"profiles":[{"name":"sonnet","model":"anthropic/claude-sonnet-4-5","api_key_set":true}],"active_profile":"sonnet"}"""
            ).addHeader("Content-Type", "application/json")
        )
        val result = repo.listProfiles()
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow().size)
        assertEquals("sonnet", result.getOrThrow()[0].name)
    }
}
