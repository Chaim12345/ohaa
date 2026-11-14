package com.example.data.api

import com.example.data.model.EditResponseDto
import com.example.data.model.EventResponse
import com.example.data.model.GitInstallationPageResponse
import com.example.data.model.GitOrganizationsDto
import com.example.data.model.GitProviderTokenRequest
import com.example.data.model.InstalledSkillDto
import com.example.data.model.MarketplaceCheckResponseDto
import com.example.data.model.MarketplaceRegistrationDto
import com.example.data.model.MarketplaceSkillsPreviewDto
import com.example.data.model.McpOAuthStartRequest
import com.example.data.model.McpOAuthStartResponseDto
import com.example.data.model.McpOAuthStatusResponseDto
import com.example.data.model.McpToolCallRequest
import com.example.data.model.SkillPageDto
import com.example.data.model.McpToolCallResultDto
import com.example.data.model.ProfileDetailDto
import com.example.data.model.ProfileMutationDto
import com.example.data.model.RenameProfileRequest
import com.example.data.model.RepositoryDetailDto
import com.example.data.model.SaveProfileRequest
import com.example.data.model.SecretNamesDto
import com.example.data.model.StartTaskPageResponse
import com.example.data.model.UpdateUserSettingsRequest
import com.example.data.model.UserSettingsDto
import com.example.data.model.WebClientConfigDto
import com.example.data.model.BranchPageResponse
import com.example.data.model.ConversationPageResponse
import com.example.data.model.ConversationResponse
import com.example.data.model.ConversationUpdateRequest
import com.example.data.model.CreateSecretRequest
import com.example.data.model.EventPageResponse
import com.example.data.model.GitChangeDto
import com.example.data.model.LlmModelPageResponse
import com.example.data.model.PendingMessageRequest
import com.example.data.model.ProfileListResponse
import com.example.data.model.ProviderPageResponse
import com.example.data.model.RepositoryPageResponse
import com.example.data.model.SandboxInfoDto
import com.example.data.model.SandboxPageResponse
import com.example.data.model.SandboxSpecPageResponse
import com.example.data.model.SandboxSpecInfoDto
import com.example.data.model.SecretDto
import com.example.data.model.SecretPageResponse
import com.example.data.model.SendMessagePayload
import com.example.data.model.SkillPageResponse
import com.example.data.model.StartConversationRequest
import com.example.data.model.StartTaskResponse
import com.example.data.model.SuccessResponse
import com.example.data.model.SuggestedTaskPageResponse
import com.example.data.model.SwitchAcpEffortRequest
import com.example.data.model.SwitchAcpModelRequest
import com.example.data.model.SwitchProfileRequest
import com.example.data.model.UpdateSecretRequest
import com.example.data.model.UserGitInfoDto
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

interface OpenHandsApiService {

    /** Cheap authenticated probe; 401 means the key is wrong. */
    @GET("api/v1/users/me")
    suspend fun getCurrentUser(): Response<ResponseBody>

    /** Starts a conversation asynchronously; returns a start task, not a conversation. */
    @POST("api/v1/app-conversations")
    suspend fun startConversation(
        @Body request: StartConversationRequest
    ): Response<StartTaskResponse>

    /** Poll the start task until `app_conversation_id` is populated. */
    @GET("api/v1/app-conversations/start-tasks")
    suspend fun getStartTask(
        @Query("ids") ids: String
    ): Response<List<StartTaskResponse?>>

    /** Paginated listing. `GET /api/v1/app-conversations` requires `ids`, so use search. */
    @GET("api/v1/app-conversations/search")
    suspend fun searchConversations(
        @Query("limit") limit: Int = 50,
        @Query("page_id") pageId: String? = null,
    ): Response<ConversationPageResponse>

    /** Batch fetch by id (the server requires at least one id). */
    @GET("api/v1/app-conversations")
    suspend fun getConversationsByIds(
        @Query("ids") ids: String
    ): Response<List<ConversationResponse?>>

    @POST("api/v1/app-conversations/{id}/send-message")
    suspend fun sendMessage(
        @Path("id") id: String,
        @Body request: SendMessagePayload
    ): Response<ResponseBody>

    /**
     * Paginated event history. `GET .../events` requires an `id` query param, so use search.
     *
     * Sorted newest-first so the most recent events survive the server's page
     * limit; the Room query re-orders them chronologically for display.
     */
    @GET("api/v1/conversation/{id}/events/search")
    suspend fun searchEvents(
        @Path("id") id: String,
        @Query("limit") limit: Int = 100,
        @Query("sort_order") sortOrder: String = "TIMESTAMP_DESC",
        @Query("exclude_full_state") excludeFullState: Boolean = true,
        @Query("page_id") pageId: String? = null,
    ): Response<EventPageResponse>

    /** Files changed in the sandbox workspace (git porcelain status + path). */
    @GET("api/v1/app-conversations/{id}/git/changes")
    suspend fun getGitChanges(
        @Path("id") id: String
    ): Response<List<GitChangeDto?>>

    @DELETE("api/v1/app-conversations/{id}")
    suspend fun deleteConversation(
        @Path("id") id: String
    ): Response<ResponseBody>

    /**
     * Resumes a PAUSED sandbox. Idempotent: a sandbox that is already STARTING
     * or RUNNING returns 200 without side effects.
     */
    @POST("api/v1/sandboxes/{id}/resume")
    suspend fun resumeSandbox(
        @Path("id") sandboxId: String
    ): Response<SuccessResponse>

    /** Pauses a running sandbox, releasing CPU and memory while keeping storage. */
    @POST("api/v1/sandboxes/{id}/pause")
    suspend fun pauseSandbox(
        @Path("id") sandboxId: String
    ): Response<SuccessResponse>

    // ---- conversation metadata / control ----------------------------------

    /** Rename, re-tag, or re-target a conversation. */
    @PATCH("api/v1/app-conversations/{id}")
    suspend fun updateConversation(
        @Path("id") id: String,
        @Body request: ConversationUpdateRequest
    ): Response<ConversationResponse>

    /** Queues a message that runs when the agent is free. */
    @POST("api/v1/conversations/{id}/pending-messages")
    suspend fun queuePendingMessage(
        @Path("id") id: String,
        @Body request: PendingMessageRequest
    ): Response<SuccessResponse>

    @GET("api/v1/app-conversations/count")
    suspend fun countConversations(): Response<ResponseBody>

    @GET("api/v1/app-conversations/start-tasks/count")
    suspend fun countStartTasks(): Response<ResponseBody>

    @GET("api/v1/conversation/{id}/events/count")
    suspend fun countEvents(@Path("id") id: String): Response<ResponseBody>

    // ---- model / effort switching (cloud) ---------------------------------

    @POST("api/v1/app-conversations/{id}/switch_profile")
    suspend fun switchProfile(
        @Path("id") id: String,
        @Body request: SwitchProfileRequest
    ): Response<SuccessResponse>

    @POST("api/v1/app-conversations/{id}/switch_acp_model")
    suspend fun switchAcpModel(
        @Path("id") id: String,
        @Body request: SwitchAcpModelRequest
    ): Response<SuccessResponse>

    @POST("api/v1/app-conversations/{id}/switch_acp_effort")
    suspend fun switchAcpEffort(
        @Path("id") id: String,
        @Body request: SwitchAcpEffortRequest
    ): Response<SuccessResponse>

    @GET("api/v1/settings/profiles")
    suspend fun listProfiles(): Response<ProfileListResponse>

    @GET("api/v1/config/models/search")
    suspend fun searchModels(
        @Query("limit") limit: Int = 100,
        @Query("page_id") pageId: String? = null,
        @Query("query") query: String? = null,
        @Query("verified__eq") verified: Boolean? = null,
        @Query("provider__eq") provider: String? = null,
    ): Response<LlmModelPageResponse>

    @GET("api/v1/config/providers/search")
    suspend fun searchProviders(
        @Query("limit") limit: Int = 100,
        @Query("page_id") pageId: String? = null,
    ): Response<ProviderPageResponse>

    @GET("api/v1/app-conversations/{id}/skills")
    suspend fun getConversationSkills(@Path("id") id: String): Response<ResponseBody>

    // ---- git ---------------------------------------------------------------

    @GET("api/v1/git/repositories/search")
    suspend fun searchRepositories(
        @Query("provider") provider: String,
        @Query("query") query: String? = null,
        @Query("limit") limit: Int = 100,
        @Query("page_id") pageId: String? = null,
    ): Response<RepositoryPageResponse>

    @GET("api/v1/git/branches/search")
    suspend fun searchBranches(
        @Query("provider") provider: String,
        @Query("repository") repository: String,
        @Query("query") query: String,
        @Query("limit") limit: Int = 30,
    ): Response<BranchPageResponse>

    @GET("api/v1/git/suggested-tasks/search")
    suspend fun searchSuggestedTasks(
        @Query("limit") limit: Int = 30,
    ): Response<SuggestedTaskPageResponse>

    // ---- sandboxes ---------------------------------------------------------

    @GET("api/v1/sandboxes/search")
    suspend fun searchSandboxes(
        @Query("limit") limit: Int = 100,
        @Query("page_id") pageId: String? = null,
    ): Response<SandboxPageResponse>

    @GET("api/v1/sandboxes")
    suspend fun getSandbox(@Query("id") sandboxId: String): Response<SandboxInfoDto>

    @DELETE("api/v1/sandboxes/{id}")
    suspend fun deleteSandbox(@Path("id") sandboxId: String): Response<ResponseBody>

    @GET("api/v1/sandbox-specs/search")
    suspend fun searchSandboxSpecs(
        @Query("limit") limit: Int = 100,
    ): Response<SandboxSpecPageResponse>

    // ---- skills ------------------------------------------------------------

    @GET("api/v1/skills/search")
    suspend fun searchSkills(
        @Query("limit") limit: Int = 100,
        @Query("page_id") pageId: String? = null,
    ): Response<SkillPageResponse>

    @POST("api/v1/skills/marketplace-check")
    suspend fun checkMarketplaceSkills(@Body request: RequestBody): Response<ResponseBody>

    @POST("api/v1/skills/marketplace-skills")
    suspend fun installMarketplaceSkills(@Body request: RequestBody): Response<ResponseBody>

    // ---- secrets -----------------------------------------------------------

    @GET("api/v1/secrets/search")
    suspend fun searchSecrets(
        @Query("limit") limit: Int = 100,
        @Query("page_id") pageId: String? = null,
        @Query("name__contains") nameContains: String? = null,
    ): Response<SecretPageResponse>

    @POST("api/v1/secrets")
    suspend fun createSecret(@Body request: CreateSecretRequest): Response<SecretDto>

    @PUT("api/v1/secrets/{secret_id}")
    suspend fun updateSecret(
        @Path("secret_id") secretId: String,
        @Body request: UpdateSecretRequest
    ): Response<SecretDto>

    @DELETE("api/v1/secrets/{secret_id}")
    suspend fun deleteSecret(@Path("secret_id") secretId: String): Response<ResponseBody>

    // ---- users -------------------------------------------------------------

    @GET("api/v1/users/git-info")
    suspend fun getGitInfo(): Response<UserGitInfoDto>

    // ---- mcp ---------------------------------------------------------------

    @POST("api/v1/mcp/test")
    suspend fun testMcpServer(@Body request: RequestBody): Response<ResponseBody>

    @POST("api/v1/mcp/servers/{server_name}/call-tool")
    suspend fun callMcpTool(
        @Path("server_name") serverName: String,
        @Body request: McpToolCallRequest
    ): Response<McpToolCallResultDto>

    @POST("api/v1/mcp/oauth/start")
    suspend fun startMcpOAuth(@Body request: McpOAuthStartRequest): Response<McpOAuthStartResponseDto>

    @GET("api/v1/mcp/oauth/status/{job_id}")
    suspend fun getMcpOAuthStatus(@Path("job_id") jobId: String): Response<McpOAuthStatusResponseDto>

    // ---- skills marketplace -----------------------------------------------

    @GET("api/v1/skills/search")
    suspend fun searchSkillsCatalog(
        @Query("limit") limit: Int = 100,
        @Query("page_id") pageId: String? = null,
    ): Response<SkillPageDto>

    @POST("api/v1/skills/marketplace-check")
    suspend fun checkMarketplace(
        @Body request: MarketplaceRegistrationDto
    ): Response<MarketplaceCheckResponseDto>

    @POST("api/v1/skills/marketplace-skills")
    suspend fun previewMarketplaceSkills(
        @Body request: List<MarketplaceRegistrationDto>
    ): Response<MarketplaceSkillsPreviewDto>

    // ---- start tasks (search) ---------------------------------------------

    /** Paginated start-task listing. The by-ids form is used for polling. */
    @GET("api/v1/app-conversations/start-tasks/search")
    suspend fun searchStartTasks(
        @Query("limit") limit: Int = 50,
        @Query("page_id") pageId: String? = null,
    ): Response<StartTaskPageResponse>

    /** Streaming variant of `POST /app-conversations`; returns the created start tasks. */
    @POST("api/v1/app-conversations/stream-start")
    suspend fun streamStartConversation(
        @Body request: StartConversationRequest
    ): Response<List<StartTaskResponse?>>

    // ---- conversation files / diff / hooks / download ----------------------

    /** File listing within a conversation's repository (returns paths). */
    @GET("api/v1/app-conversations/{id}/files")
    suspend fun getConversationFiles(
        @Path("id") id: String,
        @Query("path") path: String? = null,
        @Query("repository") repository: String? = null,
    ): Response<List<String?>>

    /** Raw file content within a conversation's repository. */
    @GET("api/v1/app-conversations/{id}/file")
    suspend fun getConversationFile(
        @Path("id") id: String,
        @Query("file_path") filePath: String,
        @Query("repository") repository: String? = null,
    ): Response<ResponseBody>

    /** Unified diff for a conversation's workspace. */
    @GET("api/v1/app-conversations/{id}/git/diff")
    suspend fun getConversationGitDiff(
        @Path("id") id: String,
        @Query("path") path: String? = null,
        @Query("ref") ref: String? = null,
        @Query("repository") repository: String? = null,
    ): Response<ResponseBody>

    /** Git hooks configured for the conversation's repository. */
    @GET("api/v1/app-conversations/{id}/hooks")
    suspend fun getConversationHooks(
        @Path("id") id: String,
        @Query("repository") repository: String? = null,
    ): Response<ResponseBody>

    /** Download a conversation trajectory (zip). */
    @Streaming
    @GET("api/v1/app-conversations/{id}/download")
    suspend fun downloadConversation(@Path("id") id: String): Response<ResponseBody>

    /** Non-paginated event fetch; requires `id` as a query param. */
    @GET("api/v1/conversation/{id}/events")
    suspend fun getEventsById(
        @Path("id") id: String,
        @Query("id") eventIds: String,
    ): Response<List<EventResponse?>>

    // ---- git ---------------------------------------------------------------

    @GET("api/v1/git/installations/search")
    suspend fun searchGitInstallations(
        @Query("provider") provider: String,
        @Query("limit") limit: Int = 100,
        @Query("page_id") pageId: String? = null,
    ): Response<GitInstallationPageResponse>

    @GET("api/v1/git/repositories/details")
    suspend fun getRepositoryDetails(
        @Query("provider") provider: String,
        @Query("repository") repository: String,
    ): Response<RepositoryDetailDto>

    // ---- sandboxes ---------------------------------------------------------

    /** Creates a standalone sandbox from a spec. */
    @POST("api/v1/sandboxes")
    suspend fun createSandbox(
        @Query("sandbox_spec_id") sandboxSpecId: String
    ): Response<SandboxInfoDto>

    @GET("api/v1/sandbox-specs")
    suspend fun getSandboxSpecs(
        @Query("ids") ids: String,
    ): Response<List<SandboxSpecInfoDto?>>

    @GET("api/v1/sandboxes/{id}/settings/secrets")
    suspend fun getSandboxSecrets(@Path("id") sandboxId: String): Response<SecretNamesDto>

    @GET("api/v1/sandboxes/{id}/settings/secrets/{name}")
    suspend fun getSandboxSecretValue(
        @Path("id") sandboxId: String,
        @Path("name") name: String,
    ): Response<ResponseBody>

    // ---- git provider tokens ----------------------------------------------

    @POST("api/v1/secrets/git-providers")
    suspend fun storeGitProviderTokens(@Body request: GitProviderTokenRequest): Response<EditResponseDto>

    @DELETE("api/v1/secrets/git-providers")
    suspend fun deleteGitProviderTokens(): Response<EditResponseDto>

    @DELETE("api/v1/users/git-providers/{provider}")
    suspend fun unlinkGitProvider(@Path("provider") provider: String): Response<ResponseBody>

    @GET("api/v1/users/git-organizations")
    suspend fun getGitOrganizations(): Response<GitOrganizationsDto>

    // ---- settings / profiles ----------------------------------------------

    @GET("api/v1/settings")
    suspend fun getSettings(): Response<UserSettingsDto>

    @POST("api/v1/settings")
    suspend fun updateSettings(@Body request: UpdateUserSettingsRequest): Response<UserSettingsDto>

    @GET("api/v1/settings/agent-schema")
    suspend fun getAgentSchema(): Response<ResponseBody>

    @GET("api/v1/settings/conversation-schema")
    suspend fun getConversationSchema(): Response<ResponseBody>

    @GET("api/v1/settings/profiles/{name}")
    suspend fun getProfile(@Path("name") name: String): Response<ProfileDetailDto>

    @POST("api/v1/settings/profiles/{name}")
    suspend fun saveProfile(
        @Path("name") name: String,
        @Body request: SaveProfileRequest
    ): Response<ProfileMutationDto>

    @DELETE("api/v1/settings/profiles/{name}")
    suspend fun deleteProfile(@Path("name") name: String): Response<ProfileMutationDto>

    @POST("api/v1/settings/profiles/{name}/activate")
    suspend fun activateProfile(@Path("name") name: String): Response<ProfileMutationDto>

    @POST("api/v1/settings/profiles/{name}/rename")
    suspend fun renameProfile(
        @Path("name") name: String,
        @Body request: RenameProfileRequest
    ): Response<ProfileMutationDto>

    // ---- web client --------------------------------------------------------

    @GET("api/v1/web-client/config")
    suspend fun getWebClientConfig(): Response<WebClientConfigDto>
}
