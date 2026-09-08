package com.example

import com.example.data.api.ApiClient
import com.example.data.model.AppConversationDto
import com.example.data.model.ConversationStatus
import com.example.data.model.ExecutionStatus
import com.example.data.model.IsoTime
import com.example.data.model.PageDto
import com.example.data.model.SandboxStatus
import com.example.data.model.SendMessageRequest
import com.example.data.model.StartConversationRequest
import com.example.data.model.StartTaskDto
import com.example.data.model.StartTaskStatus
import com.squareup.moshi.Types
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelsTest {

    @Test
    fun conversationStatus_derivesFromSandboxAndExecution() {
        assertEquals(ConversationStatus.STARTING, ConversationStatus.derive(SandboxStatus.STARTING, ExecutionStatus.RUNNING))
        assertEquals(ConversationStatus.PAUSED, ConversationStatus.derive(SandboxStatus.PAUSED, ExecutionStatus.RUNNING))
        assertEquals(ConversationStatus.RUNNING, ConversationStatus.derive(SandboxStatus.RUNNING, ExecutionStatus.RUNNING))
        assertEquals(ConversationStatus.WAITING_FOR_CONFIRMATION, ConversationStatus.derive(SandboxStatus.RUNNING, ExecutionStatus.WAITING_FOR_CONFIRMATION))
        assertEquals(ConversationStatus.FINISHED, ConversationStatus.derive(SandboxStatus.RUNNING, ExecutionStatus.FINISHED))
        assertEquals(ConversationStatus.FINISHED, ConversationStatus.derive(SandboxStatus.MISSING, ExecutionStatus.FINISHED))
        assertEquals(ConversationStatus.STUCK, ConversationStatus.derive(SandboxStatus.RUNNING, ExecutionStatus.STUCK))
        assertEquals(ConversationStatus.ERROR, ConversationStatus.derive(SandboxStatus.ERROR, ExecutionStatus.IDLE))
        assertEquals(ConversationStatus.IDLE, ConversationStatus.derive(SandboxStatus.RUNNING, ExecutionStatus.UNKNOWN))
    }

    @Test
    fun enums_parseServerStringsCaseInsensitively() {
        assertEquals(ExecutionStatus.WAITING_FOR_CONFIRMATION, ExecutionStatus.from("waiting_for_confirmation"))
        assertEquals(SandboxStatus.MISSING, SandboxStatus.from("MISSING"))
        assertEquals(StartTaskStatus.SETTING_UP_GIT_HOOKS, StartTaskStatus.from("SETTING_UP_GIT_HOOKS"))
        assertEquals(StartTaskStatus.UNKNOWN, StartTaskStatus.from("SOMETHING_NEW"))
        assertTrue(StartTaskStatus.READY.isTerminal)
        assertTrue(StartTaskStatus.ERROR.isTerminal)
        assertFalse(StartTaskStatus.WAITING_FOR_SANDBOX.isTerminal)
    }

    @Test
    fun isoTime_parsesServerFormats() {
        assertEquals(1_788_852_689_652L, IsoTime.toEpochMillis("2026-09-08T07:31:29.652635Z"))
        assertEquals(1_788_852_689_000L, IsoTime.toEpochMillis("2026-09-08T07:31:29+00:00"))
        assertEquals(1_788_852_689_000L, IsoTime.toEpochMillis("2026-09-08T07:31:29")) // naive → UTC
        assertNull(IsoTime.toEpochMillis("not a date"))
        assertNull(IsoTime.toEpochMillis(null))
    }

    @Test
    fun startRequest_serializesOnlyProvidedFields() {
        val json = ApiClient.moshi.adapter(StartConversationRequest::class.java).toJson(
            StartConversationRequest(
                initialMessage = SendMessageRequest.text("do it"),
                selectedRepository = "owner/repo",
                agentType = "plan"
            )
        )
        assertTrue(json.contains("\"initial_message\":{\"role\":\"user\",\"content\":[{\"type\":\"text\",\"text\":\"do it\"}],\"run\":true}"))
        assertTrue(json.contains("\"selected_repository\":\"owner/repo\""))
        assertTrue(json.contains("\"agent_type\":\"plan\""))
        assertFalse(json.contains("selected_branch"))
        assertFalse(json.contains("null"))
    }

    @Test
    fun conversationPage_parsesMetricsAndStatuses() {
        val adapter = ApiClient.moshi.adapter<PageDto<AppConversationDto>>(
            Types.newParameterizedType(PageDto::class.java, AppConversationDto::class.java)
        )
        val page = adapter.fromJson(
            """
            {"items":[{"id":"c1","sandbox_id":"sb1","selected_repository":"o/r","selected_branch":"main","git_provider":"github",
              "title":"Fix it","trigger":"openhands_api","pr_number":[12],"llm_model":"anthropic/claude","agent_kind":"openhands",
              "metrics":{"model_name":"anthropic/claude","accumulated_cost":0.4321,"accumulated_token_usage":{"prompt_tokens":1200,"completion_tokens":300}},
              "sub_conversation_ids":[],"public":false,"tags":{},"created_at":"2026-09-08T07:00:00Z","updated_at":"2026-09-08T07:30:00Z",
              "sandbox_status":"RUNNING","execution_status":"waiting_for_confirmation","conversation_url":"https://app.all-hands.dev/conversations/c1"}],
             "next_page_id":null}
            """
        )
        assertNotNull(page)
        val c = page!!.items.single()
        assertEquals("sb1", c.sandboxId)
        assertEquals(0.4321, c.metrics!!.accumulatedCost, 1e-9)
        assertEquals(1200L, c.metrics!!.accumulatedTokenUsage!!.promptTokens)
        assertEquals(listOf(12), c.prNumber)
        assertEquals("waiting_for_confirmation", c.executionStatus)
        assertNull(page.nextPageId)
    }

    @Test
    fun startTask_parsesNullableBatchResponse() {
        val adapter = ApiClient.moshi.adapter<List<StartTaskDto?>>(Types.newParameterizedType(List::class.java, StartTaskDto::class.java))
        val list = adapter.fromJson("""[{"id":"t1","status":"WAITING_FOR_SANDBOX","detail":"Provisioning","app_conversation_id":null,"created_at":"2026-09-08T07:00:00Z","updated_at":"2026-09-08T07:00:01Z"},null]""")
        assertEquals(2, list!!.size)
        assertEquals(StartTaskStatus.WAITING_FOR_SANDBOX, StartTaskStatus.from(list[0]!!.status))
        assertNull(list[1])
    }

    @Test
    fun describeError_readsFastApiAndAuthShapes() {
        assertTrue(ApiClient.describeError(401, """{"error":"NoCredentialsError"}""").contains("NoCredentialsError"))
        assertTrue(ApiClient.describeError(422, """{"detail":[{"loc":["body","initial_message"],"msg":"field required","type":"missing"}]}""").contains("initial_message: field required"))
        assertTrue(ApiClient.describeError(500, null).startsWith("OpenHands Cloud error (500)"))
    }
}
