package com.example

import com.example.data.api.ApiClient
import com.example.data.model.ConversationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OpenHandsApiUnitTest {

    @Test
    fun parseConversationList_parsesV1JsonArrayAndObject() {
        val jsonArray = """
            [
              {
                "id": "conv-101",
                "app_conversation_id": "conv-101",
                "title": "Fix Auth Token Crash",
                "status": "READY",
                "sandbox_status": "RUNNING",
                "execution_status": "finished",
                "selected_repository": "OpenHands/OpenHands",
                "cost": 0.05
              }
            ]
        """.trimIndent()

        val list = ApiClient.parseConversationList(jsonArray)
        assertEquals(1, list.size)
        assertEquals("conv-101", list[0].resolvedId)
        assertEquals("OpenHands/OpenHands", list[0].selectedRepository)
        assertEquals("RUNNING", list[0].sandboxStatus)
        assertEquals("finished", list[0].executionStatus)
    }

    @Test
    fun parseStartTask_parsesTaskObjectAndArray() {
        val startTaskJson = """
            {
              "id": "task-550e8400",
              "status": "READY",
              "app_conversation_id": "conv-660e8400",
              "sandbox_id": "sandbox-abc123"
            }
        """.trimIndent()

        val task = ApiClient.parseStartTask(startTaskJson)
        assertNotNull(task)
        assertEquals("task-550e8400", task?.id)
        assertEquals("READY", task?.status)
        assertEquals("conv-660e8400", task?.appConversationId)
    }

    @Test
    fun parseEventList_parsesEventsWithTypedKinds() {
        val eventsJson = """
            {
              "items": [
                {
                  "id": "ev-1",
                  "kind": "ActionEvent",
                  "source": "agent",
                  "message": "Running bash command",
                  "code": "pytest tests/"
                },
                {
                  "id": "ev-2",
                  "kind": "ServerErrorEvent",
                  "source": "system",
                  "detail": "MCP connection failed"
                }
              ],
              "next_page_id": "page-2"
            }
        """.trimIndent()

        val events = ApiClient.parseEventList(eventsJson)
        assertEquals(2, events.size)
        assertEquals("ActionEvent", events[0].kind)
        assertEquals("ServerErrorEvent", events[1].kind)
    }

    @Test
    fun conversationStatus_mappingHandlesV1ExecutionStatus() {
        val finishedStatus = ConversationStatus.fromString(value = "WORKING", sandboxStatus = "RUNNING", executionStatus = "finished")
        assertEquals(ConversationStatus.FINISHED, finishedStatus)

        val waitingStatus = ConversationStatus.fromString(value = "WORKING", sandboxStatus = "RUNNING", executionStatus = "waiting_for_confirmation")
        assertEquals(ConversationStatus.WAITING_FOR_CONFIRMATION, waitingStatus)

        val stuckStatus = ConversationStatus.fromString(value = "WORKING", sandboxStatus = "RUNNING", executionStatus = "stuck")
        assertEquals(ConversationStatus.STUCK, stuckStatus)
    }
}
