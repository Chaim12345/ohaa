package com.example.data.repository

import com.example.data.model.ConversationPageResponse
import com.example.data.model.ConversationStatus
import com.example.data.model.EventPageResponse
import com.example.data.model.StartTaskResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Parses real OpenHands Cloud payloads (as captured from the V1 API) through the
 * production DTOs and mapping functions, then asserts the domain result.
 */
class OpenHandsMappingTest {

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    @Test
    fun `conversation page maps sandbox and execution status`() {
        val json = """
            {
              "items": [
                {
                  "id": "abc12345-6789",
                  "title": "Fix failing test",
                  "selected_repository": "All-Hands-AI/OpenHands",
                  "selected_branch": "main",
                  "sandbox_status": "RUNNING",
                  "execution_status": "running",
                  "created_at": "2026-11-14T09:00:00.123456Z",
                  "updated_at": "2026-11-14T09:30:00Z",
                  "conversation_ui_url": "https://app.all-hands.dev/conversations/abc12345-6789"
                }
              ],
              "next_page_id": null
            }
        """.trimIndent()

        val page = moshi.adapter(ConversationPageResponse::class.java).fromJson(json)!!
        val entity = page.items!!.single().toEntity()

        assertEquals("abc12345-6789", entity.id)
        assertEquals("Fix failing test", entity.title)
        assertEquals("All-Hands-AI/OpenHands", entity.selectedRepository)
        assertEquals("main", entity.selectedBranch)
        assertEquals(ConversationStatus.RUNNING.name, entity.status)
        assertTrue(entity.createdAt > 0)
        assertTrue(entity.updatedAt > entity.createdAt)

        val domain = entity.toDomain()
        assertEquals(ConversationStatus.RUNNING, domain.status)
    }

    @Test
    fun `conversation without execution status falls back to sandbox status`() {
        val json = """
            {"items":[{"id":"x","sandbox_status":"STARTING","execution_status":null}]}
        """.trimIndent()

        val entity = moshi.adapter(ConversationPageResponse::class.java)
            .fromJson(json)!!.items!!.single().toEntity()

        assertEquals(ConversationStatus.WAITING_FOR_SANDBOX.name, entity.status)
        // No title on the wire -> synthesized from the id.
        assertTrue(entity.title.isNotBlank())
    }

    @Test
    fun `action event with terminal command becomes a bash step`() {
        val json = """
            {
              "items": [
                {
                  "id": "evt-1",
                  "timestamp": "2026-11-14T09:05:00Z",
                  "source": "agent",
                  "kind": "ActionEvent",
                  "tool_name": "terminal",
                  "tool_call_id": "call_1",
                  "summary": "Running the test suite",
                  "thought": [{"type": "text", "text": "I will run pytest."}],
                  "action": {"kind": "TerminalAction", "command": "pytest -q"}
                }
              ]
            }
        """.trimIndent()

        val page = moshi.adapter(EventPageResponse::class.java).fromJson(json)!!
        val event = page.items!!.single().toEntity("conv-1")!!

        assertEquals("conv-1", event.conversationId)
        assertEquals("bash", event.actionType)
        assertEquals("agent", event.source)
        // The thought is preferred as the narrative line.
        assertEquals("I will run pytest.", event.content)
        assertTrue(event.timestamp > 0)
    }

    @Test
    fun `action event without thought falls back to the command`() {
        val json = """
            {"items":[{"id":"e","kind":"ActionEvent","tool_name":"terminal",
              "thought":[],"action":{"kind":"TerminalAction","command":"ls -la"}}]}
        """.trimIndent()

        val event = moshi.adapter(EventPageResponse::class.java)
            .fromJson(json)!!.items!!.single().toEntity("c")!!

        assertEquals("$ ls -la", event.content)
    }

    @Test
    fun `observation event surfaces stdout and error flag`() {
        val json = """
            {
              "items": [
                {
                  "id": "evt-2",
                  "timestamp": "2026-11-14T09:06:00Z",
                  "kind": "ObservationEvent",
                  "tool_name": "terminal",
                  "tool_call_id": "call_1",
                  "observation": {
                    "kind": "TerminalObservation",
                    "content": [{"type": "text", "text": "2 passed in 0.4s"}],
                    "is_error": false,
                    "command": "pytest -q",
                    "exit_code": 0,
                    "stdout": "2 passed in 0.4s",
                    "stderr": ""
                  }
                }
              ]
            }
        """.trimIndent()

        val event = moshi.adapter(EventPageResponse::class.java)
            .fromJson(json)!!.items!!.single().toEntity("c")!!

        assertEquals("observation", event.actionType)
        assertEquals("environment", event.source)
        assertEquals("2 passed in 0.4s", event.content)
        assertTrue(event.detail!!.contains("exit code: 0"))
        assertEquals(false, event.isError)
    }

    @Test
    fun `message event from the user keeps the user role`() {
        val json = """
            {
              "items": [
                {
                  "id": "evt-3",
                  "timestamp": "2026-11-14T09:01:00Z",
                  "source": "user",
                  "kind": "MessageEvent",
                  "llm_message": {
                    "role": "user",
                    "content": [{"type": "text", "text": "Fix the login bug"}]
                  }
                }
              ]
            }
        """.trimIndent()

        val event = moshi.adapter(EventPageResponse::class.java)
            .fromJson(json)!!.items!!.single().toEntity("c")!!

        assertEquals("user", event.source)
        assertEquals("message", event.actionType)
        assertEquals("Fix the login bug", event.content)
    }

    @Test
    fun `noise events are dropped`() {
        val json = """
            {"items":[
              {"id":"s1","kind":"ConversationStateUpdateEvent"},
              {"id":"s2","kind":"SystemPromptEvent","system_prompt":{"type":"text","text":"..."}}
            ]}
        """.trimIndent()

        val events = moshi.adapter(EventPageResponse::class.java)
            .fromJson(json)!!.items!!.map { it.toEntity("c") }

        assertNull(events[0])
        assertNull(events[1])
    }

    @Test
    fun `start task exposes conversation id once ready`() {
        val json = """
            {"id":"task-1","status":"READY","app_conversation_id":"conv-9",
             "sandbox_id":"sbx-1","agent_server_url":"https://agent.example"}
        """.trimIndent()

        val task = moshi.adapter(StartTaskResponse::class.java).fromJson(json)!!

        assertEquals("task-1", task.id)
        assertEquals("conv-9", task.appConversationId)
        assertEquals(ConversationStatus.READY, ConversationStatus.fromStartTask(task.status))
    }

    @Test
    fun `start task status maps to a UI status while provisioning`() {
        assertEquals(
            ConversationStatus.WAITING_FOR_SANDBOX,
            ConversationStatus.fromStartTask("WAITING_FOR_SANDBOX")
        )
        assertEquals(
            ConversationStatus.PREPARING_REPOSITORY,
            ConversationStatus.fromStartTask("PREPARING_REPOSITORY")
        )
        assertEquals(
            ConversationStatus.SETTING_UP_SKILLS,
            ConversationStatus.fromStartTask("SETTING_UP_SKILLS")
        )
        assertEquals(ConversationStatus.ERROR, ConversationStatus.fromStartTask("ERROR"))
    }

    @Test
    fun `timestamps tolerate fractional seconds and offsets`() {
        val zulu = OpenHandsRepository.parseIsoTimestamp("2026-11-14T09:00:00Z")
        val fractional = OpenHandsRepository.parseIsoTimestamp("2026-11-14T09:00:00.123456Z")
        val naive = OpenHandsRepository.parseIsoTimestamp("2026-11-14T09:00:00")

        assertEquals(zulu, fractional)
        assertEquals(zulu, naive)
        assertEquals(0L, OpenHandsRepository.parseIsoTimestamp(null))
        assertEquals(0L, OpenHandsRepository.parseIsoTimestamp("not-a-date"))
    }

    @Test
    fun `real cloud conversation payload maps without loss`() {
        val entity = moshi.adapter(ConversationPageResponse::class.java)
            .fromJson(readFixture("live/conversations.json"))!!
            .items!!.map { it.toEntity() }

        assertTrue(entity.isNotEmpty())
        entity.forEach {
            assertTrue(it.id.isNotBlank())
            assertTrue(it.title.isNotBlank())
            assertTrue(it.createdAt > 0)
            assertTrue(it.updatedAt >= it.createdAt)
        }
        assertEquals("Chaim12345/ohaa", entity.first().selectedRepository)
    }

    @Test
    fun `real cloud event payload renders every timeline entry`() {
        val events = moshi.adapter(EventPageResponse::class.java)
            .fromJson(readFixture("live/events.json"))!!
            .items!!.mapNotNull { it.toEntity("conv") }

        // The live page mixes Action/Observation/Message plus noise kinds.
        assertTrue(events.any { it.actionType == "bash" })
        assertTrue(events.any { it.actionType == "observation" })
        assertTrue(events.any { it.actionType == "message" })
        assertTrue(events.all { it.content.isNotBlank() })
        assertTrue(events.all { it.conversationId == "conv" })
    }

    @Test
    fun `sub-second timestamps keep their wire order`() {
        val json = """
            {"items":[
              {"id":"e2","kind":"ActionEvent","timestamp":"2026-11-14T09:00:00.500000","tool_name":"terminal",
               "action":{"kind":"TerminalAction","command":"second"}},
              {"id":"e1","kind":"ActionEvent","timestamp":"2026-11-14T09:00:00.100000","tool_name":"terminal",
               "action":{"kind":"TerminalAction","command":"first"}}
            ]}
        """.trimIndent()

        val entities = moshi.adapter(EventPageResponse::class.java)
            .fromJson(json)!!.items!!.mapNotNull { it.toEntity("c") }

        // Same millisecond, so the raw timestamp must decide the order.
        assertEquals(entities[0].timestamp, entities[1].timestamp)
        val ordered = entities.sortedBy { it.sortKey }.map { it.content }
        assertEquals(listOf("$ first", "$ second"), ordered)
    }

    private fun readFixture(path: String): String =
        checkNotNull(javaClass.classLoader?.getResourceAsStream(path)) {
            "missing fixture $path"
        }.use { it.readBytes().decodeToString() }
}
