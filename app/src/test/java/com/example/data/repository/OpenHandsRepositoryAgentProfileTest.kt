package com.example.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.SaveAgentProfileRequest
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
 * Exercises the cloud agent-profile surface (used by the New Task ACP picker)
 * against a real HTTP server, so the exact method, path and body are pinned.
 */
@RunWith(RobolectricTestRunner::class)
class OpenHandsRepositoryAgentProfileTest {

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
        repository.updateConfig("cloud-key", server.url("/").toString())
    }

    @After
    fun tearDown() {
        database.close()
        server.shutdown()
    }

    @Test
    fun `list agent profiles hits the cloud agent-profiles endpoint with bearer auth`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"profiles":[{"id":"p1","name":"claude","agent_kind":"acp","acp_server":"claude-code"}],""" +
                    """"active_agent_profile_id":"p1"}"""
            )
        )

        val result = repository.listAgentProfiles()

        assertTrue("call failed: ${result.exceptionOrNull()}", result.isSuccess)
        val profiles = result.getOrNull().orEmpty()
        assertEquals(1, profiles.size)
        assertEquals("claude-code", profiles[0].acpServer)
        val recorded = server.takeRequest()
        assertEquals("/api/agent-profiles", recorded.path)
        assertEquals("Bearer cloud-key", recorded.getHeader("Authorization"))
    }

    @Test
    fun `save agent profile posts the acp body`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"name":"claude"}"""))

        repository.saveAgentProfile(
            SaveAgentProfileRequest(
                name = "claude",
                agentKind = "acp",
                acpServer = "claude-code",
                acpModel = "sonnet",
                acpEffort = "high",
            )
        )

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/api/agent-profiles/claude", recorded.path)
        val body = recorded.body.readUtf8()
        assertTrue(body.contains("\"agent_kind\":\"acp\""))
        assertTrue(body.contains("\"acp_server\":\"claude-code\""))
        assertTrue(body.contains("\"acp_model\":\"sonnet\""))
        assertTrue(body.contains("\"acp_effort\":\"high\""))
    }

    @Test
    fun `get agent profile reads the typed acp body`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"name":"claude","scope":"personal","profile":""" +
                    """{"agent_kind":"acp","acp_server":"claude-code","acp_model":"opus"}}"""
            )
        )

        val result = repository.getAgentProfile("claude")

        assertEquals("claude-code", result.getOrNull()?.acpServer)
        assertEquals("opus", result.getOrNull()?.acpModel)
        assertEquals("/api/agent-profiles/claude", server.takeRequest().path)
    }

    @Test
    fun `activate and deactivate hit the profile id routes`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"id":"p1"}"""))
        repository.activateAgentProfile("p1")
        assertEquals("/api/agent-profiles/p1/activate", server.takeRequest().path)

        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"id":"p1"}"""))
        repository.deactivateAgentProfile("p1")
        assertEquals("/api/agent-profiles/p1/deactivate", server.takeRequest().path)
    }

    @Test
    fun `rename posts the new name`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"name":"claude2"}"""))

        repository.renameAgentProfile("claude", "claude2")

        val recorded = server.takeRequest()
        assertEquals("/api/agent-profiles/claude/rename", recorded.path)
        assertTrue(recorded.body.readUtf8().contains("claude2"))
    }
}
