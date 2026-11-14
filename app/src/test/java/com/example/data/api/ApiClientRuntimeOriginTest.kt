package com.example.data.api

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pins [ApiClient.runtimeOrigin], which strips the path from a conversation's
 * reported `conversation_url`.
 *
 * OpenHands Cloud reports the URL as `<origin>/api/conversations/<id>`, but the
 * agent-server serves every route from the host root. Keeping the path makes the
 * runtime base `.../api/conversations/<id>/` so an appended `/server_info` 404s.
 */
class ApiClientRuntimeOriginTest {

    @Test
    fun `drops the conversation path segment`() {
        assertEquals(
            "https://rerrdbesjikfvera.prod-runtime.all-hands.dev/",
            ApiClient.runtimeOrigin(
                "https://rerrdbesjikfvera.prod-runtime.all-hands.dev/api/conversations/abc123"
            )
        )
    }

    @Test
    fun `keeps a non-default port`() {
        assertEquals(
            "http://localhost:8080/",
            ApiClient.runtimeOrigin("http://localhost:8080/api/conversations/abc123")
        )
    }

    @Test
    fun `a bare origin is unchanged`() {
        assertEquals(
            "https://app.all-hands.dev/",
            ApiClient.runtimeOrigin("https://app.all-hands.dev")
        )
    }

    @Test
    fun `adds a missing scheme`() {
        assertEquals(
            "https://example.com/",
            ApiClient.runtimeOrigin("example.com/api/conversations/x")
        )
    }
}
