package com.example

import com.example.data.api.ApiClient
import com.example.data.model.AgentProfileInfoDto
import com.example.data.model.ApiKeyCreateResponseDto
import com.example.data.model.AppConversationDto
import com.example.data.model.ConversationInfoDto
import com.example.data.model.CreditsDto
import com.example.data.model.DeviceAuthorizationResponseDto
import com.example.data.model.EventDto
import com.example.data.model.LlmProfileListDto
import com.example.data.model.MeResponseDto
import com.example.data.model.MessagePayloads
import com.example.data.model.MessageToolCallDto
import com.example.data.model.OrgPageDto
import com.example.data.model.PageDto
import com.example.data.model.QuotaStatusResponseDto
import com.example.data.model.RepositoryDto
import com.example.data.model.SendMessageRequest
import com.example.data.model.StartTaskDto
import com.example.data.model.SwitchAcpModelRequest
import com.example.data.model.SwitchProfileRequest
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
                "metrics": {"accumulated_cost": 0.08}
              },
              {
                "id": "660e8400-0002",
                "title": "Rate limiting",
                "sandbox_status": "STARTING"
              }
            ]
        """.trimIndent()
        val listType = com.squareup.moshi.Types.newParameterizedType(
            List::class.java, com.example.data.model.AppConversationDto::class.java
        )
        val dtos = moshi.adapter<List<com.example.data.model.AppConversationDto>>(listType).fromJson(json)!!
        assertEquals(2, dtos.size)
        assertEquals("Fix README", dtos[0].title)
        assertEquals("finished", dtos[0].executionStatus)
        assertEquals("660e8400-0001", dtos[0].resolvedId)
        assertEquals(0.08, dtos[0].cost, 0.001)
        assertNull(dtos[1].executionStatus)
    }

    @Test
    fun `parses events page with items array and spec event shapes`() {
        val json = """
            {
              "items": [
                {
                  "id": "e1",
                  "kind": "MessageEvent",
                  "source": "user",
                  "timestamp": "2025-01-15T10:31:00Z",
                  "llm_message": {
                    "role": "user",
                    "content": [{"type": "text", "text": "Fix the failing test"}]
                  }
                },
                {
                  "id": "e2",
                  "kind": "ObservationEvent",
                  "source": "agent",
                  "tool_name": "terminal",
                  "observation": {
                    "content": [{"type": "text", "text": "2 passed in 0.35s"}],
                    "exit_code": 0
                  }
                }
              ],
              "next_page_id": null
            }
        """.trimIndent()
        val eventType = com.squareup.moshi.Types.newParameterizedType(
            PageDto::class.java, EventDto::class.java
        )
        @Suppress("UNCHECKED_CAST")
        val page = moshi.adapter<PageDto<EventDto>>(eventType).fromJson(json)!!
        assertEquals(2, page.items.size)
        assertNull(page.nextPageId)
        val e1 = page.items[0]
        assertEquals("Fix the failing test", e1.llmMessage?.contentText())
        assertTrue(e1.kind?.endsWith("MessageEvent") == true)
        assertEquals("terminal", page.items[1].toolName)
    }

    @Test
    fun `action event falls back to tool_call arguments command`() {
        // Live ActionEvent shape: tool_call.arguments is a JSON string.
        val dto = EventDto(
            id = "e3",
            source = "agent",
            kind = "ActionEvent",
            toolName = "terminal",
            toolCall = MessageToolCallDto(
                name = "terminal",
                arguments = "{\"command\": \"pytest -q\", \"summary\": \"run tests\"}"
            )
        )
        assertEquals("pytest -q", MessagePayloads.argument(dto.toolCall?.arguments, "command"))
    }

    @Test
    fun `start task without app_conversation_id still parses`() {
        val json = """{"id": "t-1", "status": "WAITING_FOR_SANDBOX"}"""
        val dto = moshi.adapter(StartTaskDto::class.java).fromJson(json)
        assertEquals("WAITING_FOR_SANDBOX", dto?.status)
        assertNull(dto?.appConversationId)
    }

    @Test
    fun `switch profile request serializes profile_name per spec`() {
        val json = moshi.adapter(SwitchProfileRequest::class.java)
            .toJson(SwitchProfileRequest(profileName = "claude-sonnet"))
        assertTrue("profile_name" in json)
        assertTrue("claude-sonnet" in json)
    }

    @Test
    fun `switch acp model request serializes model per spec`() {
        val json = moshi.adapter(SwitchAcpModelRequest::class.java)
            .toJson(SwitchAcpModelRequest(model = "claude-sonnet-4-5"))
        assertTrue("\"model\"" in json)
        assertTrue("claude-sonnet-4-5" in json)
    }

    @Test
    fun `send message request includes role content and run`() {
        val json = moshi.adapter(SendMessageRequest::class.java)
            .toJson(SendMessageRequest.text("hello"))
        assertTrue("\"role\":\"user\"" in json.replace(" ", ""))
        assertTrue("\"run\":true" in json.replace(" ", ""))
        assertTrue("\"type\":\"text\"" in json.replace(" ", ""))
    }

    @Test
    fun `credits response parses numeric string per spec`() {
        val dto = moshi.adapter(CreditsDto::class.java)
            .fromJson("""{"credits": "12.50"}""")
        assertEquals("12.50", dto?.credits)
    }

    @Test
    fun `profiles list parses profiles and active profile`() {
        val json = """
            {"profiles": [
               {"name": "sonnet", "model": "anthropic/claude-sonnet-4-5", "api_key_set": true},
               {"name": "gpt5", "model": "openai/gpt-5"}
             ],
             "active_profile": "sonnet"}
        """.trimIndent()
        val dto = moshi.adapter(LlmProfileListDto::class.java).fromJson(json)!!
        assertEquals(2, dto.profiles.size)
        assertEquals("sonnet", dto.activeProfile)
        assertEquals("anthropic/claude-sonnet-4-5", dto.profiles[0].model)
        assertEquals("gpt5", dto.profiles[1].name)
    }

    @Test
    fun `repository parses spec fields including main_branch`() {
        val json = """
            {"id": "1317", "full_name": "you/your-repo", "git_provider": "github",
             "is_public": false, "stargazers_count": 3, "main_branch": "master"}
        """.trimIndent()
        val dto = moshi.adapter(RepositoryDto::class.java).fromJson(json)!!
        assertEquals("you/your-repo", dto.displayName)
        assertEquals("master", dto.branch)
        assertEquals(false, dto.isPublic)
        assertEquals(3, dto.stargazersCount)
    }

    @Test
    fun `launched_agent_profile parses as object not string`() {
        // Regression: the live wire shape is an object; String typing crashed with
        // "Expected a string but was BEGIN_OBJECT at $.items[0].launched_agent_profile".
        val json = """
            {
              "id": "660e8400-0001",
              "title": "Fix README",
              "launched_agent_profile": {
                "agent_profile_id": "0199b7b0-6b2f-7d21-9b2c-3b4b5c6d7e8f",
                "revision": 1
              }
            }
        """.trimIndent()
        val dto = moshi.adapter(AppConversationDto::class.java).fromJson(json)!!
        assertEquals("0199b7b0-6b2f-7d21-9b2c-3b4b5c6d7e8f", dto.launchedAgentProfile?.agentProfileId)
        assertEquals(1, dto.launchedAgentProfile?.revision)
    }

    @Test
    fun `quota status parses spec fields`() {
        val json = """
            {
              "daily_limit": 50,
              "used_today": 12,
              "remaining": 38,
              "reset_at": "2026-09-11T00:00:00Z",
              "work_email": "dev@corp.example",
              "work_email_verified": true,
              "latest_request_status": "approved",
              "latest_request_requested_limit": 100
            }
        """.trimIndent()
        val dto = moshi.adapter(QuotaStatusResponseDto::class.java).fromJson(json)!!
        assertEquals(50, dto.dailyLimit)
        assertEquals(12, dto.usedToday)
        assertEquals(38, dto.remaining)
        assertEquals("approved", dto.latestRequestStatus)
        assertEquals(100, dto.latestRequestRequestedLimit)
    }

    @Test
    fun `org page parses items and next page cursor`() {
        val json = """
            {
              "items": [
                {"id": "org-1", "name": "Acme", "is_personal": false},
                {"id": "org-2", "name": "Personal", "is_personal": true}
              ],
              "next_page_id": "cursor-9",
              "current_org_id": "org-1"
            }
        """.trimIndent()
        val dto = moshi.adapter(OrgPageDto::class.java).fromJson(json)!!
        assertEquals(2, dto.items?.size)
        assertEquals("Acme", dto.items?.first()?.name)
        assertEquals("cursor-9", dto.nextPageId)
        assertEquals("org-1", dto.currentOrgId)
    }

    @Test
    fun `device authorization response parses required oauth fields`() {
        val json = """
            {
              "device_code": "dc-123",
              "user_code": "WDJB-MJHT",
              "verification_uri": "https://app.all-hands.dev/oauth/device",
              "verification_uri_complete": "https://app.all-hands.dev/oauth/device?user_code=WDJB-MJHT",
              "expires_in": 1800,
              "interval": 5
            }
        """.trimIndent()
        val dto = moshi.adapter(DeviceAuthorizationResponseDto::class.java).fromJson(json)!!
        assertEquals("dc-123", dto.deviceCode)
        assertEquals("WDJB-MJHT", dto.userCode)
        assertEquals("https://app.all-hands.dev/oauth/device", dto.verificationUri)
        assertEquals("https://app.all-hands.dev/oauth/device?user_code=WDJB-MJHT", dto.verificationUriComplete)
        assertEquals(1800, dto.expiresIn)
        assertEquals(5, dto.interval)
    }

    @Test
    fun `api key create response parses key material`() {
        val json = """
            {
              "id": 42,
              "name": "ci-key",
              "key": "oh_xxxxxxxxxxxx"
            }
        """.trimIndent()
        val dto = moshi.adapter(ApiKeyCreateResponseDto::class.java).fromJson(json)!!
        assertEquals(42, dto.id)
        assertEquals("ci-key", dto.name)
        assertEquals("oh_xxxxxxxxxxxx", dto.key)
    }

    @Test
    fun `agent profile info parses revision and mcp refs`() {
        val json = """
            {
              "id": "0199b7b0-6b2f-7d21-9b2c-3b4b5c6d7e8f",
              "name": "default",
              "agent_kind": "openhands",
              "revision": 3,
              "llm_profile_ref": "anthropic-claude",
              "mcp_server_refs": ["gh-mcp"]
            }
        """.trimIndent()
        val dto = moshi.adapter(AgentProfileInfoDto::class.java).fromJson(json)!!
        assertEquals("default", dto.name)
        assertEquals("openhands", dto.agentKind)
        assertEquals(3, dto.revision)
        assertEquals("anthropic-claude", dto.llmProfileRef)
        assertEquals(listOf("gh-mcp"), dto.mcpServerRefs)
    }

    @Test
    fun `me response parses role permissions and api keys`() {
        val json = """
            {
              "org_id": "org-1",
              "user_id": "u-1",
              "email": "me@x.io",
              "role": "ADMIN",
              "permissions": ["conversation:read", "settings:write"],
              "llm_api_key": "managed",
              "llm_api_key_for_byor": null,
              "agent_settings_diff": {},
              "conversation_settings_diff": {}
            }
        """.trimIndent()
        val dto = moshi.adapter(MeResponseDto::class.java).fromJson(json)!!
        assertEquals("ADMIN", dto.role)
        assertEquals(listOf("conversation:read", "settings:write"), dto.permissions)
        assertEquals("managed", dto.llmApiKey)
        assertNull(dto.llmApiKeyForByor)
    }

    @Test
    fun `webhook conversation payload parses flat fields`() {
        val json = """
            {
              "id": "conv-webhook",
              "max_iterations": 100,
              "execution_status": "RUNNING",
              "title": "CI run",
              "created_at": "2026-09-10T00:00:00Z",
              "updated_at": "2026-09-10T00:05:00Z",
              "sub_conversation_ids": [],
              "supports_runtime_model_switch": false,
              "launched_agent_profile": {"agent_profile_id": "p-1", "revision": 2},
              "client_tools": []
            }
        """.trimIndent()
        val dto = moshi.adapter(ConversationInfoDto::class.java).fromJson(json)!!
        assertEquals("conv-webhook", dto.id)
        assertEquals(100, dto.maxIterations)
        assertEquals("RUNNING", dto.executionStatus)
        assertEquals("CI run", dto.title)
        assertEquals(2, dto.launchedAgentProfile?.revision)
    }
}
