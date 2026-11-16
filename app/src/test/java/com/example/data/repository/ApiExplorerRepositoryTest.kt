package com.example.data.repository

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Exercises [ApiExplorerRepository] against a real HTTP server. The explorer is
 * what gives every catalog endpoint a reachable UI home, so these tests pin the
 * URL assembly and the per-surface auth header.
 */
class ApiExplorerRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: ApiExplorerRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        repository = ApiExplorerRepository()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `cloud call sends the bearer token and returns the body`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"ok":true}"""))

        val result = repository.call(
            surface = "cloud",
            method = "GET",
            baseUrl = server.url("/").toString(),
            credential = "cloud-key",
            path = "/api/v1/users/me",
            body = null,
        )

        assertTrue(result.ok)
        assertEquals(200, result.code)
        assertEquals("""{"ok":true}""", result.body)
        val recorded = server.takeRequest()
        assertEquals("/api/v1/users/me", recorded.path)
        assertEquals("Bearer cloud-key", recorded.getHeader("Authorization"))
        assertEquals(null, recorded.getHeader("X-Session-API-Key"))
    }

    @Test
    fun `agent call sends the session key instead of the bearer token`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""[]"""))

        val result = repository.call(
            surface = "agent",
            method = "GET",
            baseUrl = server.url("/").toString(),
            credential = "session-secret",
            path = "/api/git/changes?path=%2F",
            body = null,
        )

        assertTrue(result.ok)
        val recorded = server.takeRequest()
        assertEquals("/api/git/changes?path=%2F", recorded.path)
        assertEquals("session-secret", recorded.getHeader("X-Session-API-Key"))
        assertEquals(null, recorded.getHeader("Authorization"))
    }

    @Test
    fun `a POST forwards the JSON body`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"success":true}"""))

        val result = repository.call(
            surface = "cloud",
            method = "POST",
            baseUrl = server.url("/").toString(),
            credential = "k",
            path = "/api/v1/secrets",
            body = """{"name":"A","value":"B"}""",
        )

        assertTrue(result.ok)
        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("""{"name":"A","value":"B"}""", recorded.body.readUtf8())
    }

    @Test
    fun `an error status is reported without throwing`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(422).setBody("""{"detail":"bad"}"""))

        val result = repository.call(
            surface = "cloud",
            method = "GET",
            baseUrl = server.url("/").toString(),
            credential = "k",
            path = "/api/v1/nope",
            body = null,
        )

        assertFalse(result.ok)
        assertEquals(422, result.code)
    }

    @Test
    fun `the catalog exposes every path on every surface`() {
        assertEquals(67, repository.cloudPaths.size)
        assertEquals(190, repository.cloudAdminPaths.size)
        assertEquals(151, repository.agentPaths.size)
    }
}
