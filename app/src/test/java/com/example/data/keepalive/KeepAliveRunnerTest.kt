package com.example.data.keepalive

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.ConversationEntity
import com.example.data.repository.AgentServerRepository
import com.example.data.repository.OpenHandsRepository
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Exercises [KeepAliveRunner] against a real repository: a real in-memory Room
 * database for the target list and a real HTTP server for the runtime calls.
 */
@RunWith(RobolectricTestRunner::class)
class KeepAliveRunnerTest {

    private lateinit var server: MockWebServer
    private lateinit var database: AppDatabase
    private lateinit var repository: OpenHandsRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = OpenHandsRepository(context, database)
        repository.updateConfig("test-key", server.url("/").toString())
    }

    @After
    fun tearDown() {
        database.close()
        server.shutdown()
    }

    private fun insert(id: String, sandboxId: String, status: String, sandboxStatus: String) =
        runBlocking {
            database.conversationDao().insertConversation(
                ConversationEntity(
                    id = id,
                    title = "t",
                    status = status,
                    selectedRepository = "org/repo",
                    createdAt = 1L,
                    updatedAt = 1L,
                    sandboxId = sandboxId,
                    sandboxStatus = sandboxStatus,
                )
            )
        }

    @Test
    fun `running sandbox is touched via the runtime endpoint`() = runBlocking {
        insert("conv-1", "sbx-1", "RUNNING", "RUNNING")
        server.enqueue(MockResponse().setResponseCode(200).setBody("""[]"""))

        val result = KeepAliveRunner(repository).tick()

        assertEquals(1, result.touched)
        assertEquals(0, result.resumed)
        assertEquals("/api/v1/app-conversations/conv-1/git/changes", server.takeRequest().path)
    }

    @Test
    fun `paused sandbox is resumed instead of touched`() = runBlocking {
        insert("conv-2", "sbx-2", "PAUSED", "PAUSED")
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"success":true}"""))

        val result = KeepAliveRunner(repository).tick()

        assertEquals(0, result.touched)
        assertEquals(1, result.resumed)
        assertEquals("/api/v1/sandboxes/sbx-2/resume", server.takeRequest().path)
    }

    @Test
    fun `terminal conversations are skipped`() = runBlocking {
        insert("conv-3", "sbx-3", "FINISHED", "RUNNING")
        insert("conv-4", "sbx-4", "ERROR", "ERROR")

        val result = KeepAliveRunner(repository).tick()

        assertEquals(0, result.total)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `conversation without a sandbox id is skipped`() = runBlocking {
        insert("conv-5", "", "RUNNING", "RUNNING")

        val result = KeepAliveRunner(repository).tick()

        assertEquals(0, result.total)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `a missing api key leaves the sandboxes untouched`() = runBlocking {
        insert("conv-6", "sbx-6", "RUNNING", "RUNNING")
        repository.updateConfig("", server.url("/").toString())

        val result = KeepAliveRunner(repository).tick()

        assertEquals(0, result.total)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `a failed ping does not abort the pass`() = runBlocking {
        insert("conv-7", "sbx-7", "RUNNING", "RUNNING")
        insert("conv-8", "sbx-8", "RUNNING", "RUNNING")
        server.enqueue(MockResponse().setResponseCode(500).setBody("""{"detail":"boom"}"""))
        server.enqueue(MockResponse().setResponseCode(200).setBody("""[]"""))

        val result = KeepAliveRunner(repository).tick()

        assertEquals(1, result.touched)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `a conversation with runtime access is touched on the sandbox runtime`() = runBlocking {
        runBlocking {
            database.conversationDao().insertConversation(
                ConversationEntity(
                    id = "conv-rt",
                    title = "t",
                    status = "RUNNING",
                    selectedRepository = "org/repo",
                    createdAt = 1L,
                    updatedAt = 1L,
                    sandboxId = "sbx-rt",
                    sandboxStatus = "RUNNING",
                    conversationUrl = server.url("/").toString().trimEnd('/'),
                    sessionApiKey = "session-secret",
                )
            )
        }
        server.enqueue(MockResponse().setResponseCode(200).setBody("""[]"""))

        val runner = KeepAliveRunner(repository, AgentServerRepository(database.conversationDao()))
        val result = runner.tick()

        assertEquals(1, result.touched)
        val recorded = server.takeRequest()
        assertEquals("/api/git/changes", recorded.path)
        assertEquals("session-secret", recorded.getHeader("X-Session-API-Key"))
    }
}
