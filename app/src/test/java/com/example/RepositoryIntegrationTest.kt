package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.api.ApiClient
import com.example.data.api.ApiException
import com.example.data.local.AppDatabase
import com.example.data.local.ConversationEntity
import com.example.data.model.SandboxStatus
import com.example.data.model.StartTaskProgress
import com.example.data.repository.OpenHandsRepository
import com.example.data.repository.StartConversationParams
import kotlinx.coroutines.flow.toList
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
import org.robolectric.annotation.Config

/** Drives the repository against a MockWebServer that answers with real V1 payload shapes. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RepositoryIntegrationTest {

    private lateinit var server: MockWebServer
    private lateinit var db: AppDatabase
    private lateinit var repo: OpenHandsRepository

    @Before
    fun setUp() {
        server = MockWebServer().also { it.start() }
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences(OpenHandsRepository.PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        repo = OpenHandsRepository(context, db) { _, keyProvider ->
            ApiClient.create(server.url("/").toString(), keyProvider, enableBodyLogging = false)
        }
        repo.updateConfig("sk-oh-test", server.url("/").toString())
    }

    @After
    fun tearDown() {
        server.shutdown()
        db.close()
    }

    private fun json(body: String, code: Int = 200) =
        MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body)

    @Test
    fun startConversation_pollsStartTaskUntilReady() = runBlocking {
        server.enqueue(json("""{"id":"task1","status":"WORKING","detail":null,"app_conversation_id":null,"sandbox_id":null,"created_at":"2026-09-08T07:00:00Z","updated_at":"2026-09-08T07:00:00Z"}"""))
        server.enqueue(json("""[{"id":"task1","status":"WAITING_FOR_SANDBOX","detail":"Provisioning","app_conversation_id":null,"sandbox_id":"sb1","created_at":"2026-09-08T07:00:00Z","updated_at":"2026-09-08T07:00:01Z"}]"""))
        server.enqueue(json("""[{"id":"task1","status":"READY","detail":null,"app_conversation_id":"conv1","sandbox_id":"sb1","agent_server_url":"https://x","created_at":"2026-09-08T07:00:00Z","updated_at":"2026-09-08T07:00:05Z"}]"""))
        // refreshConversation(conv1)
        server.enqueue(json("""[{"id":"conv1","sandbox_id":"sb1","title":"Fix it","sandbox_status":"RUNNING","execution_status":"running","created_at":"2026-09-08T07:00:00Z","updated_at":"2026-09-08T07:00:05Z","metrics":{"accumulated_cost":0.01}}]"""))

        val progress = repo.startConversation(StartConversationParams(prompt = "do it", repository = "o/r", agentType = "plan")).toList()

        val first = server.takeRequest()
        assertEquals("POST", first.method)
        assertEquals("/api/v1/app-conversations", first.path)
        assertEquals("sk-oh-test", first.getHeader("X-Access-Token"))
        assertEquals("Bearer sk-oh-test", first.getHeader("Authorization"))
        val body = first.body.readUtf8()
        assertTrue(body, body.contains("\"selected_repository\":\"o/r\"") && body.contains("\"agent_type\":\"plan\""))
        assertEquals("/api/v1/app-conversations/start-tasks?ids=task1", server.takeRequest().path)
        assertEquals("/api/v1/app-conversations/start-tasks?ids=task1", server.takeRequest().path)
        assertEquals("/api/v1/app-conversations?ids=conv1", server.takeRequest().path)

        assertTrue(progress.first() is StartTaskProgress.Submitting)
        assertTrue(progress.any { it is StartTaskProgress.InProgress && it.status.name == "WAITING_FOR_SANDBOX" })
        val ready = progress.last() as StartTaskProgress.Ready
        assertEquals("conv1", ready.conversationId)
        assertEquals("sb1", ready.sandboxId)
        assertEquals("Fix it", db.conversationDao().getById("conv1")!!.title)
    }

    @Test
    fun startConversation_reportsServerError() = runBlocking {
        server.enqueue(json("""{"id":"task2","status":"WORKING","created_at":"2026-09-08T07:00:00Z","updated_at":"2026-09-08T07:00:00Z"}"""))
        server.enqueue(json("""[{"id":"task2","status":"ERROR","detail":"Git provider authentication issue","created_at":"2026-09-08T07:00:00Z","updated_at":"2026-09-08T07:00:01Z"}]"""))
        val last = repo.startConversation(StartConversationParams(prompt = "x")).toList().last()
        assertTrue(last is StartTaskProgress.Failed)
        assertEquals("Git provider authentication issue", (last as StartTaskProgress.Failed).message)
    }

    @Test
    fun sendMessage_resumesPausedSandboxFirst() = runBlocking {
        db.conversationDao().upsert(
            ConversationEntity(
                id = "conv1", title = "t", selectedRepository = null, selectedBranch = null, gitProvider = null,
                sandboxId = "sb1", sandboxStatus = "PAUSED", executionStatus = "idle", createdAt = 1, updatedAt = 1
            )
        )
        server.enqueue(json("""{"success":true}"""))                                   // resume
        server.enqueue(json("""[{"id":"sb1","sandbox_spec_id":"spec","status":"RUNNING","created_at":"2026-09-08T07:00:00Z"}]""")) // sandboxes?id
        server.enqueue(json("""{"success":true,"sandbox_status":"RUNNING","message":null}"""))    // send-message
        server.enqueue(json("""{"items":[],"next_page_id":null}"""))                    // events sync

        val outcome = repo.sendMessage("conv1", "hello").getOrThrow()

        assertEquals("/api/v1/sandboxes/sb1/resume", server.takeRequest().path)
        assertEquals("/api/v1/sandboxes?id=sb1", server.takeRequest().path)
        val send = server.takeRequest()
        assertEquals("/api/v1/app-conversations/conv1/send-message", send.path)
        val sent = send.body.readUtf8()
        assertTrue(sent, sent.contains("\"role\":\"user\"") && sent.contains("\"text\":\"hello\"") && sent.contains("\"run\":true"))
        assertTrue(outcome.resumedSandbox)
        assertEquals(SandboxStatus.RUNNING, outcome.sandboxStatus)
        assertEquals("RUNNING", db.conversationDao().getById("conv1")!!.sandboxStatus)
    }

    @Test
    fun syncEvents_followsPaginationAndStoresRows() = runBlocking {
        server.enqueue(json("""{"items":[{"id":"e1","kind":"MessageEvent","timestamp":"2026-09-08T07:05:25.465929","source":"user","llm_message":{"role":"user","content":[{"type":"text","text":"hi"}]}}],"next_page_id":"p2"}"""))
        server.enqueue(json("""{"items":[{"id":"e2","kind":"ACPToolCallEvent","timestamp":"2026-09-08T07:05:40.5","source":"agent","tool_call_id":"c1","title":"ls","status":"completed","tool_kind":"execute","raw_input":{"command":"ls"},"raw_output":"a\nb","is_error":false}],"next_page_id":null}"""))

        val stored = repo.syncEvents("conv1", full = true).getOrThrow()

        assertEquals(2, stored)
        val r1 = server.takeRequest().path!!
        assertTrue(r1, r1.startsWith("/api/v1/conversation/conv1/events/search?") && r1.contains("sort_order=TIMESTAMP") && !r1.contains("page_id"))
        assertTrue(server.takeRequest().path!!.contains("page_id=p2"))
        val rows = db.eventDao().snapshotForConversation("conv1")
        assertEquals(listOf("e1", "e2"), rows.map { it.id })
        assertEquals("$ ls\na\nb", rows[1].code)

        // Incremental sync uses the newest cached timestamp as the lower bound.
        server.enqueue(json("""{"items":[],"next_page_id":null}"""))
        repo.syncEvents("conv1").getOrThrow()
        assertTrue(server.takeRequest().path!!.contains("timestamp__gte=2026-09-08T07%3A05%3A40.5"))
    }

    @Test
    fun refreshConversations_surfaces401AsApiException() = runBlocking {
        server.enqueue(json("""{"error":"NoCredentialsError"}""", 401))
        val result = repo.refreshConversations()
        val e = result.exceptionOrNull() as ApiException
        assertEquals(401, e.code)
        assertTrue(e.isAuthError)
        assertTrue(e.message!!.contains("NoCredentialsError"))
    }

    @Test
    fun deleteSandbox_sendsIdAsQueryParam() = runBlocking {
        server.enqueue(json("""{"success":true}"""))
        repo.deleteSandbox("sb9").getOrThrow()
        val req = server.takeRequest()
        assertEquals("DELETE", req.method)
        assertEquals("/api/v1/sandboxes/sb9?sandbox_id=sb9", req.path)
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ConversationPruningTest {
    @Test
    fun staleRowsPrunedOnlyAfterLastPage() = runBlocking {
        val server = MockWebServer().also { it.start() }
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        val repo = OpenHandsRepository(context, db) { _, kp -> ApiClient.create(server.url("/").toString(), kp, false) }
        repo.updateConfig("k", server.url("/").toString())
        db.conversationDao().upsert(ConversationEntity("stale", "old", null, null, null, null, "RUNNING", "idle", 1, 1, syncedAt = 1))
        fun conv(id: String) = """{"id":"$id","sandbox_status":"RUNNING","execution_status":"idle","created_at":"2026-09-08T07:00:00Z","updated_at":"2026-09-08T07:00:00Z"}"""
        server.enqueue(MockResponse().setBody("""{"items":[${conv("a")}],"next_page_id":"2"}"""))
        server.enqueue(MockResponse().setBody("""{"items":[${conv("b")}],"next_page_id":null}"""))

        assertTrue(repo.refreshConversations().getOrThrow())
        assertEquals(setOf("stale", "a"), db.conversationDao().getAll().map { it.id }.toSet())
        assertTrue(!repo.loadMoreConversations().getOrThrow())
        assertEquals(setOf("a", "b"), db.conversationDao().getAll().map { it.id }.toSet())
        server.shutdown(); db.close()
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class StartingSandboxAndRepoRootTest {
    @Test
    fun startingSandbox_queuesMessageInsteadOfSending() = runBlocking {
        val server = MockWebServer().also { it.start() }
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        val repo = OpenHandsRepository(context, db) { _, kp -> ApiClient.create(server.url("/").toString(), kp, false) }
        repo.updateConfig("k", server.url("/").toString())
        db.conversationDao().upsert(
            ConversationEntity("c1", "t", "own/repo", null, null, "sb1", "STARTING", "idle", 1, 1,
                workspacePath = "/workspace/project/c1")
        )
        server.enqueue(MockResponse().setBody("""{"id":"m1","queued":true,"position":1}"""))

        val outcome = repo.sendMessage("c1", "hello").getOrThrow()

        assertEquals("/api/v1/conversations/c1/pending-messages", server.takeRequest().path)
        assertEquals(SandboxStatus.STARTING, outcome.sandboxStatus)
        assertTrue(outcome.message!!.contains("position 1"))
        assertTrue(db.eventDao().snapshotForConversation("c1").single().title.contains("queued"))
        server.shutdown(); db.close()
    }

    @Test
    fun defaultRepoRoot_usesWorkspaceTagAndRepoLeaf() {
        val c = ConversationEntity("c1", "t", "Chaim12345/ohaa", null, null, "sb", "RUNNING", "idle", 1, 1,
            workspacePath = "/workspace/project/c1").toDomain()
        assertEquals("/workspace/project/c1/ohaa", c.defaultRepoRoot)

        val noRepo = ConversationEntity("c2", "t", null, null, null, "sb", "RUNNING", "idle", 1, 1,
            workspacePath = "/workspace/project/c2").toDomain()
        assertEquals("/workspace/project/c2", noRepo.defaultRepoRoot)

        val noTag = ConversationEntity("c3", "t", "o/r", null, null, "sb", "RUNNING", "idle", 1, 1).toDomain()
        assertEquals("/workspace/project/c3/r", noTag.defaultRepoRoot)
    }
}
