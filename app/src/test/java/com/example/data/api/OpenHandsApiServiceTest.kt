package com.example.data.api

import com.example.data.model.EventPageResponse
import com.example.data.model.SendMessagePayload
import com.example.data.model.StartConversationRequest
import com.example.data.model.TextContentDto
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Drives the production Retrofit service against a real HTTP server, so the
 * request path, query parameters, auth header, and Moshi decoding are all
 * exercised end to end (no mocks of the code under test).
 */
class OpenHandsApiServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var service: OpenHandsApiService

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        service = ApiClient.create(
            baseUrlProvider = { server.url("/").toString() },
            apiKeyProvider = { "test-key-123" }
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `conversation search hits the search endpoint with limit`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"items":[{"id":"c1","title":"t","sandbox_status":"RUNNING","execution_status":"running"}],"next_page_id":null}"""
            )
        )

        val response = service.searchConversations(limit = 25)

        assertTrue(response.isSuccessful)
        assertEquals("c1", response.body()?.items?.single()?.id)

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/api/v1/app-conversations/search?limit=25", request.path)
        assertEquals("Bearer test-key-123", request.getHeader("Authorization"))
    }

    @Test
    fun `conversation search forwards the page id cursor`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"items":[{"id":"c2","sandbox_status":"RUNNING"}],"next_page_id":"4"}"""
            )
        )

        val response = service.searchConversations(limit = 50, pageId = "2")

        assertEquals("c2", response.body()?.items?.single()?.id)
        assertEquals("4", response.body()?.nextPageId)
        assertEquals(
            "/api/v1/app-conversations/search?limit=50&page_id=2",
            server.takeRequest().path
        )
    }

    @Test
    fun `event search sends the documented query parameters`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"items":[]}"""))

        service.searchEvents(id = "conv-1", limit = 100, excludeFullState = true)

        val request = server.takeRequest()
        assertEquals(
            "/api/v1/conversation/conv-1/events/search?limit=100&sort_order=TIMESTAMP_DESC&exclude_full_state=true",
            request.path
        )
    }

    @Test
    fun `start conversation posts the initial message body`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"id":"task-1","status":"WORKING","app_conversation_id":null}"""
            )
        )

        val body = StartConversationRequest(
            initialMessage = SendMessagePayload(content = listOf(TextContentDto(text = "Fix the bug"))),
            selectedRepository = "Chaim12345/ohaa",
            selectedBranch = "main"
        )
        val response = service.startConversation(body)

        assertEquals("task-1", response.body()?.id)

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/app-conversations", request.path)
        val json = request.body.readUtf8()
        assertTrue(json.contains("\"initial_message\""))
        assertTrue(json.contains("\"content\":[{\"type\":\"text\",\"text\":\"Fix the bug\"}]"))
        assertTrue(json.contains("\"selected_repository\":\"Chaim12345/ohaa\""))
        assertTrue(json.contains("\"selected_branch\":\"main\""))
    }

    @Test
    fun `start task poll returns a batch array`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """[{"id":"task-1","status":"READY","app_conversation_id":"conv-9"}]"""
            )
        )

        val response = service.getStartTask("task-1")

        val first = response.body()?.firstOrNull()
        assertNotNull(first)
        assertEquals("conv-9", first?.appConversationId)
        assertEquals("/api/v1/app-conversations/start-tasks?ids=task-1", server.takeRequest().path)
    }

    @Test
    fun `send message posts to the send-message endpoint`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

        service.sendMessage("conv-1", SendMessagePayload(content = listOf(TextContentDto(text = "continue"))))

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/app-conversations/conv-1/send-message", request.path)
        assertTrue(request.body.readUtf8().contains("\"run\":true"))
    }

    @Test
    fun `delete conversation issues a delete request`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

        service.deleteConversation("conv-1")

        val request = server.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/api/v1/app-conversations/conv-1", request.path)
    }

    @Test
    fun `batch fetch encodes the id list`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """[{"id":"c1","sandbox_status":"RUNNING","execution_status":null}]"""
            )
        )

        service.getConversationsByIds("c1")

        assertEquals("/api/v1/app-conversations?ids=c1", server.takeRequest().path)
    }

    @Test
    fun `401 is surfaced as unsuccessful without throwing`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"detail":"Unauthorized"}"""))

        val response = service.searchConversations()

        assertEquals(401, response.code())
        assertTrue(ApiClient.errorMessage(response).contains("Unauthorized"))
    }

    @Test
    fun `unknown json fields are ignored when decoding events`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"items":[{"id":"e1","kind":"ActionEvent","tool_name":"terminal",
                   "unexpected_field":{"deep":[1,2,3]},
                   "action":{"kind":"TerminalAction","command":"ls"}}]}"""
            )
        )

        val response = service.searchEvents("c1")

        val page: EventPageResponse? = response.body()
        assertEquals("e1", page?.items?.single()?.id)
    }

    @Test
    fun `git changes decode status and path entries`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """[{"status":"ADDED","path":"src/A.kt"},{"status":"UPDATED","path":"src/B.kt"}]"""
            )
        )

        val response = service.getGitChanges("conv-1")

        val changes = response.body().orEmpty()
        assertEquals(2, changes.size)
        assertEquals("ADDED", changes[0]?.status)
        assertEquals("src/B.kt", changes[1]?.path)
        assertEquals(
            "/api/v1/app-conversations/conv-1/git/changes",
            server.takeRequest().path
        )
    }

    @Test
    fun `users me is used as the authenticated probe`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"user_version":null}"""))

        val response = service.getCurrentUser()

        assertTrue(response.isSuccessful)
        assertEquals("/api/v1/users/me", server.takeRequest().path)
    }

    @Test
    fun `git changes request also serves as the keep-alive ping`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""[]"""))

        val response = service.getGitChanges("conv-1")

        assertTrue(response.isSuccessful)
        // The keep-alive path must be the runtime-proxied endpoint; DB-backed
        // endpoints do not reset the sandbox idle timer.
        assertEquals("/api/v1/app-conversations/conv-1/git/changes", server.takeRequest().path)
    }

    @Test
    fun `resume sandbox posts to the sandbox resume endpoint`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"success":true}"""))

        val response = service.resumeSandbox("sbx-1")

        assertTrue(response.isSuccessful)
        assertEquals(true, response.body()?.success)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/sandboxes/sbx-1/resume", request.path)
    }

    @Test
    fun `pause sandbox posts to the sandbox pause endpoint`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"success":true}"""))

        service.pauseSandbox("sbx-2")

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/sandboxes/sbx-2/pause", request.path)
    }

    @Test
    fun `conversation record exposes the sandbox id needed for resume`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """[{"id":"c1","sandbox_id":"sbx-9","sandbox_status":"PAUSED"}]"""
            )
        )

        val dto = service.getConversationsByIds("c1").body()?.firstOrNull()

        assertEquals("sbx-9", dto?.sandboxId)
        assertEquals("PAUSED", dto?.sandboxStatus)
    }

    // ---- settings / profiles ----------------------------------------------

    @Test
    fun `get settings decodes the user settings model`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"email":"a@b.c","language":"en","llm_api_key_set":true,"max_budget_per_task":5.0}"""
            )
        )

        val dto = service.getSettings().body()

        assertEquals("a@b.c", dto?.email)
        assertEquals(true, dto?.llmApiKeySet)
        assertEquals("/api/v1/settings", server.takeRequest().path)
    }

    @Test
    fun `save profile posts the llm config`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(201).setBody("""{"message":"ok","name":"fast"}"""))

        val response = service.saveProfile(
            "fast",
            com.example.data.model.SaveProfileRequest(
                llm = com.example.data.model.ProfileConfigDto(model = "gpt-5", baseUrl = "https://x")
            )
        )

        assertTrue(response.isSuccessful)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/settings/profiles/fast", request.path)
        assertTrue(request.body.readUtf8().contains("gpt-5"))
    }

    @Test
    fun `activate profile posts to the activate endpoint`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"message":"activated","name":"fast"}"""))

        service.activateProfile("fast")

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/settings/profiles/fast/activate", request.path)
    }

    @Test
    fun `rename profile sends the new name`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"message":"ok","name":"new"}"""))

        service.renameProfile("old", com.example.data.model.RenameProfileRequest("new"))

        val request = server.takeRequest()
        assertEquals("/api/v1/settings/profiles/old/rename", request.path)
        assertTrue(request.body.readUtf8().contains("new"))
    }

    @Test
    fun `get profile detail decodes the config`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"name":"fast","api_key_set":true,"config":{"model":"gpt-5","base_url":"https://x"}}"""
            )
        )

        val dto = service.getProfile("fast").body()

        assertEquals("gpt-5", dto?.config?.model)
        assertEquals("/api/v1/settings/profiles/fast", server.takeRequest().path)
    }

    // ---- git extras --------------------------------------------------------

    @Test
    fun `git installations search sends the provider`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody("""{"items":["102872169"]}""")
        )

        val dto = service.searchGitInstallations("github").body()

        assertEquals("102872169", dto?.items?.single())
        assertEquals("/api/v1/git/installations/search?provider=github&limit=100", server.takeRequest().path)
    }

    @Test
    fun `git organizations decodes the response`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"provider":"github","organizations":["acme"]}"""))

        val dto = service.getGitOrganizations().body()

        assertEquals(listOf("acme"), dto?.organizations)
        assertEquals("/api/v1/users/git-organizations", server.takeRequest().path)
    }

    @Test
    fun `repository details sends provider and repository`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"id":"1","full_name":"acme/repo"}"""))

        val dto = service.getRepositoryDetails("github", "acme/repo").body()

        assertEquals("acme/repo", dto?.fullName)
        assertEquals(
            "/api/v1/git/repositories/details?provider=github&repository=acme%2Frepo",
            server.takeRequest().path
        )
    }

    // ---- sandboxes / secrets extras ---------------------------------------

    @Test
    fun `create sandbox posts the spec id`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"id":"sbx-new","status":"STARTING"}"""))

        val dto = service.createSandbox("spec-1").body()

        assertEquals("sbx-new", dto?.id)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/sandboxes?sandbox_spec_id=spec-1", request.path)
    }

    @Test
    fun `sandbox secrets returns the names`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"secrets":[{"name":"A"},{"name":"B"}]}"""))

        val dto = service.getSandboxSecrets("sbx-1").body()

        assertEquals(listOf("A", "B"), dto?.secrets?.map { it.name })
        assertEquals("/api/v1/sandboxes/sbx-1/settings/secrets", server.takeRequest().path)
    }

    @Test
    fun `mcp call-tool posts the tool spec and decodes the result`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"is_error":false,"text":"42"}"""))

        val dto = service.callMcpTool(
            "weather",
            com.example.data.model.McpToolCallRequest(name = "get_temp", arguments = mapOf("city" to "NYC"))
        ).body()

        assertEquals("42", dto?.text)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/mcp/servers/weather/call-tool", request.path)
        assertTrue(request.body.readUtf8().contains("get_temp"))
    }

    // ---- start tasks / files / diff / web client --------------------------

    @Test
    fun `start tasks search decodes the page`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"items":[{"id":"t1","status":"READY","app_conversation_id":"c1"}],"next_page_id":null}"""
            )
        )

        val dto = service.searchStartTasks().body()

        assertEquals("c1", dto?.items?.single()?.appConversationId)
        assertEquals("/api/v1/app-conversations/start-tasks/search?limit=50", server.takeRequest().path)
    }

    @Test
    fun `conversation files returns paths`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""["a.txt","b/c.txt"]"""))

        val body = service.getConversationFiles("c1", path = "src").body()

        assertEquals(listOf("a.txt", "b/c.txt"), body)
        assertEquals("/api/v1/app-conversations/c1/files?path=src", server.takeRequest().path)
    }

    @Test
    fun `conversation git diff is fetched as raw text`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("diff --git a/x b/x"))

        val body = service.getConversationGitDiff("c1", path = "x").body()?.string()

        assertEquals("diff --git a/x b/x", body)
        assertEquals("/api/v1/app-conversations/c1/git/diff?path=x", server.takeRequest().path)
    }

    @Test
    fun `web client config decodes acp providers`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"app_mode":"saas","acp_providers":[{"key":"claude-code"},{"key":"codex"}]}"""
            )
        )

        val dto = service.getWebClientConfig().body()

        assertEquals(listOf("claude-code", "codex"), dto?.acpProviders?.map { it.key })
        assertEquals("/api/v1/web-client/config", server.takeRequest().path)
    }

    @Test
    fun `agent schema is fetched as raw json`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"type":"object"}"""))

        val body = service.getAgentSchema().body()?.string()

        assertEquals("""{"type":"object"}""", body)
        assertEquals("/api/v1/settings/agent-schema", server.takeRequest().path)
    }

    // ---- mcp oauth / marketplace ------------------------------------------

    @Test
    fun `mcp oauth start posts the server name and returns the authorization url`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"ok":true,"job_id":"j1","authorization_url":"https://auth/x"}"""
            )
        )

        val dto = service.startMcpOAuth(
            com.example.data.model.McpOAuthStartRequest(name = "weather")
        ).body()

        assertEquals("j1", dto?.jobId)
        assertEquals("https://auth/x", dto?.authorizationUrl)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/mcp/oauth/start", request.path)
        assertTrue(request.body.readUtf8().contains("weather"))
    }

    @Test
    fun `mcp oauth status polls the job id`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"ok":true,"status":"pending","job_id":"j1","callback_ready":false}"""
            )
        )

        val dto = service.getMcpOAuthStatus("j1").body()

        assertEquals("pending", dto?.status)
        assertEquals("/api/v1/mcp/oauth/status/j1", server.takeRequest().path)
    }

    @Test
    fun `skills search decodes the page`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"items":[{"name":"pdf","enabled":true}],"next_page_id":null}"""
            )
        )

        val dto = service.searchSkillsCatalog().body()

        assertEquals("pdf", dto?.items?.single()?.name)
        assertEquals("/api/v1/skills/search?limit=100", server.takeRequest().path)
    }

    @Test
    fun `marketplace check posts the registration`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"status":"ok","skill_count":3,"plugin_count":1}"""
            )
        )

        val dto = service.checkMarketplace(
            com.example.data.model.MarketplaceRegistrationDto(name = "ext", source = "https://github.com/x/y")
        ).body()

        assertEquals(3, dto?.skillCount)
        val request = server.takeRequest()
        assertEquals("/api/v1/skills/marketplace-check", request.path)
        assertTrue(request.body.readUtf8().contains("ext"))
    }

    @Test
    fun `marketplace skills preview posts a list`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody("""{"skills":["a","b"],"plugins":[]}""")
        )

        val dto = service.previewMarketplaceSkills(
            listOf(com.example.data.model.MarketplaceRegistrationDto(name = "ext", source = "s"))
        ).body()

        assertEquals(listOf("a", "b"), dto?.skills)
        assertEquals("/api/v1/skills/marketplace-skills", server.takeRequest().path)
    }
}
