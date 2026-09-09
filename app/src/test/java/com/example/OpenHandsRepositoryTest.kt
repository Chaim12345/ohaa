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
            MockResponse().setResponseCode(200).setBody("[]").addHeader("Content-Type", "application/json")
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
                [
                  {"id":"c1","title":"Fix README","sandbox_status":"RUNNING","execution_status":"finished","cost":0.08},
                  {"id":"c2","title":"Rate limit","sandbox_status":"STARTING"}
                ]
                """.trimIndent()
            ).addHeader("Content-Type", "application/json")
        )

        val result = repo.refreshConversations()
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrThrow().size)
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
    fun `demo seed only when unconfigured`() = runTest {
        settings.apiKey = ""
        repo.seedLocalData()
        val demo = repo.conversationsFlow.first()
        assertEquals(3, demo.size)
    }

    @Test
    fun `events refresh parses wrapped page and stores rows`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {"events":[{"id":"e1","source":"user","message":"hello"}],"next_page_id":null}
                """.trimIndent()
            ).addHeader("Content-Type", "application/json")
        )

        val result = repo.refreshEvents("conv-x")
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow().size)
        val stored = repo.getEventsFlow("conv-x").first()
        assertEquals("hello", stored[0].content)
    }
}
