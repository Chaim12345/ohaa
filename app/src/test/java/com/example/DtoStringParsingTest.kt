package com.example

import com.example.data.api.ApiClient
import com.example.data.model.EventDto
import com.example.data.model.EventsPageDto
import com.example.data.model.StartTaskDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DtoStringParsingTest {

    private val moshi = ApiClient.moshi

    @Test
    fun `parses start task response from cloud docs example`() {
        val json = """
            {
              "id": "550e8400-e29b-41d4-a716-446655440000",
              "status": "WORKING",
              "app_conversation_id": "660e8400-e29b-41d4-a716-446655440001",
              "sandbox_id": "sandbox-abc123",
              "created_at": "2025-01-15T10:30:00Z"
            }
        """.trimIndent()
        val dto = moshi.adapter(StartTaskDto::class.java).fromJson(json)
        assertEquals("550e8400-e29b-41d4-a716-446655440000", dto?.id)
        assertEquals("WORKING", dto?.status)
        assertEquals("660e8400-e29b-41d4-a716-446655440001", dto?.appConversationId)
    }

    @Test
    fun `parses conversation list with sandbox and execution statuses`() {
        val json = """
            [
              {
                "id": "660e8400-0001",
                "title": "Fix README",
                "sandbox_status": "RUNNING",
                "execution_status": "finished",
                "selected_repository": "you/your-repo",
                "cost": 0.08
              },
              {
                "id": "660e8400-0002",
                "title": "Rate limiting",
                "sandbox_status": "STARTING"
              }
            ]
        """.trimIndent()
        val listType = com.squareup.moshi.Types.newParameterizedType(
            List::class.java, com.example.data.model.ConversationDto::class.java
        )
        val dtos = moshi.adapter<List<com.example.data.model.ConversationDto>>(listType).fromJson(json)!!
        assertEquals(2, dtos.size)
        assertEquals("Fix README", dtos[0].title)
        assertEquals("finished", dtos[0].executionStatus)
        assertEquals("660e8400-0001", dtos[0].resolvedId)
        assertNull(dtos[1].executionStatus)
    }

    @Test
    fun `parses events page with wrapped events array`() {
        val json = """
            {
              "events": [
                {
                  "id": "e1",
                  "kind": "MessageEvent",
                  "source": "user",
                  "timestamp": "2025-01-15T10:31:00Z",
                  "message": "Fix the failing test"
                },
                {
                  "id": "e2",
                  "kind": "ObservationEvent",
                  "source": "agent",
                  "action": "bash",
                  "observation": "2 passed in 0.35s"
                }
              ],
              "next_page_id": null
            }
        """.trimIndent()
        val page = moshi.adapter(EventsPageDto::class.java).fromJson(json)!!
        assertEquals(2, page.events?.size)
        assertNull(page.nextPageId)
        val e1 = page.events!![0]
        assertEquals("Fix the failing test", e1.message)
        assertTrue(e1.kind?.endsWith("MessageEvent") == true)
    }

    @Test
    fun `event falls back to args command when message fields absent`() {
        val dto = EventDto(
            id = "e3",
            source = "agent",
            action = "execute",
            args = mapOf("command" to "pytest -q")
        )
        assertEquals("pytest -q", dto.args?.get("command"))
    }

    @Test
    fun `start task without app_conversation_id still parses`() {
        val json = """{"id": "t-1", "status": "WAITING_FOR_SANDBOX"}"""
        val dto = moshi.adapter(StartTaskDto::class.java).fromJson(json)
        assertEquals("WAITING_FOR_SANDBOX", dto?.status)
        assertNull(dto?.appConversationId)
    }
}
