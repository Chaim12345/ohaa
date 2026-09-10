package com.example.data.api

import com.example.data.model.AppConversationDto
import com.example.data.model.BranchDto
import com.example.data.model.CreateSecretRequest
import com.example.data.model.CreditsDto
import com.example.data.model.CurrentApiKeyDto
import com.example.data.model.EventDto
import com.example.data.model.GitChangeDto
import com.example.data.model.GitDiffDto
import com.example.data.model.GitOrganizationsDto
import com.example.data.model.GitUserDto
import com.example.data.model.HookListDto
import com.example.data.model.LlmModelDto
import com.example.data.model.LlmProfileListDto
import com.example.data.model.LlmProviderDto
import com.example.data.model.MessageResponseDto
import com.example.data.model.PageDto
import com.example.data.model.PendingMessageResponseDto
import com.example.data.model.RenameProfileRequest
import com.example.data.model.RepositoryDto
import com.example.data.model.SandboxDto
import com.example.data.model.SandboxSpecDto
import com.example.data.model.SecretDto
import com.example.data.model.SecretNamesDto
import com.example.data.model.SendMessageRequest
import com.example.data.model.SendMessageResponseDto
import com.example.data.model.SetProviderTokensRequest
import com.example.data.model.SkillDto
import com.example.data.model.SkillListDto
import com.example.data.model.StartConversationRequest
import com.example.data.model.StartTaskDto
import com.example.data.model.SuccessDto
import com.example.data.model.SuggestedTaskDto
import com.example.data.model.SwitchAcpModelRequest
import com.example.data.model.SwitchProfileRequest
import com.example.data.model.UpdateConversationRequest
import com.example.data.model.UserInfoDto
import com.example.data.model.WebClientConfigDto
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

/**
 * OpenHands Cloud V1 REST API.
 *
 * Source of truth: the live OpenAPI contract at
 * https://app.all-hands.dev/openapi.json (verified against v1.59.0) plus live
 * response probing. Auth is `X-Access-Token: <api key>` (the documented
 * APIKeyHeader scheme; `Authorization: Bearer` is also accepted server-side).
 */
interface OpenHandsApiService {

    // ------------------------------------------------------------------ health

    /** Unversioned liveness probe → {status, …}. */
    @GET("api/service/health")
    suspend fun health(): Response<ResponseBody>

    // ----------------------------------------------------------- conversations

    /** Start a conversation. Returns a start task that must be polled until READY/ERROR. */
    @POST("api/v1/app-conversations")
    suspend fun startConversation(@Body request: StartConversationRequest): Response<StartTaskDto>

    /**
     * Same as [startConversation], but the server streams the start task as a
     * JSON array of snapshots until READY/ERROR; the last element is final.
     */
    @Streaming
    @POST("api/v1/app-conversations/stream-start")
    suspend fun streamStartConversation(@Body request: StartConversationRequest): Response<ResponseBody>

    /** Batch get start tasks. `ids` is repeated: ?ids=a&ids=b (required). */
    @GET("api/v1/app-conversations/start-tasks")
    suspend fun getStartTasks(@Query("ids") ids: List<String>): Response<List<StartTaskDto?>>

    @GET("api/v1/app-conversations/start-tasks/search")
    suspend fun searchStartTasks(
        @Query("conversation_id__eq") conversationId: String? = null,
        @Query("sort_order") sortOrder: String = "CREATED_AT_DESC",
        @Query("page_id") pageId: String? = null,
        @Query("limit") limit: Int = 20
    ): Response<PageDto<StartTaskDto>>

    @GET("api/v1/app-conversations/start-tasks/count")
    suspend fun countStartTasks(
        @Query("conversation_id__eq") conversationId: String? = null
    ): Response<Int>

    /** Batch get conversations (there is no single-item GET). `ids` is repeated and required. */
    @GET("api/v1/app-conversations")
    suspend fun getConversations(@Query("ids") ids: List<String>): Response<List<AppConversationDto?>>

    /** The primary listing surface — filters + pagination via next_page_id. */
    @GET("api/v1/app-conversations/search")
    suspend fun searchConversations(
        @Query("title__contains") titleContains: String? = null,
        @Query("created_at__gte") createdAtGte: String? = null,
        @Query("created_at__lt") createdAtLt: String? = null,
        @Query("updated_at__gte") updatedAtGte: String? = null,
        @Query("updated_at__lt") updatedAtLt: String? = null,
        @Query("sandbox_id__eq") sandboxId: String? = null,
        @Query("include_sub_conversations") includeSubConversations: Boolean = false,
        @Query("page_id") pageId: String? = null,
        @Query("limit") limit: Int = 50
    ): Response<PageDto<AppConversationDto>>

    @GET("api/v1/app-conversations/count")
    suspend fun countConversations(
        @Query("title__contains") titleContains: String? = null,
        @Query("sandbox_id__eq") sandboxId: String? = null
    ): Response<Int>

    /** PATCH metadata (title / visibility / selected repo). */
    @PATCH("api/v1/app-conversations/{conversation_id}")
    suspend fun updateConversation(
        @Path("conversation_id") conversationId: String,
        @Body request: UpdateConversationRequest
    ): Response<AppConversationDto>

    @DELETE("api/v1/app-conversations/{conversation_id}")
    suspend fun deleteConversation(@Path("conversation_id") conversationId: String): Response<Unit>

    /** Sandbox must be RUNNING; resume it first if PAUSED. */
    @POST("api/v1/app-conversations/{conversation_id}/send-message")
    suspend fun sendMessage(
        @Path("conversation_id") conversationId: String,
        @Body request: SendMessageRequest
    ): Response<SendMessageResponseDto>

    /** Queue a message for a conversation that is not READY yet (id may be the start-task id). */
    @POST("api/v1/conversations/{conversation_id}/pending-messages")
    suspend fun queuePendingMessage(
        @Path("conversation_id") conversationId: String,
        @Body request: SendMessageRequest
    ): Response<PendingMessageResponseDto>

    @GET("api/v1/app-conversations/{conversation_id}/files")
    suspend fun listFiles(
        @Path("conversation_id") conversationId: String,
        @Query("path") path: String? = null
    ): Response<List<String>>

    /** Reads a file from the sandbox workspace; body is the raw file content. */
    @GET("api/v1/app-conversations/{conversation_id}/file")
    suspend fun readFile(
        @Path("conversation_id") conversationId: String,
        @Query("file_path") filePath: String
    ): Response<ResponseBody>

    /** Working-tree changes for a repo root inside the sandbox (path is required). */
    @GET("api/v1/app-conversations/{conversation_id}/git/changes")
    suspend fun gitChanges(
        @Path("conversation_id") conversationId: String,
        @Query("path") repoPath: String,
        @Query("ref") ref: String? = null
    ): Response<List<GitChangeDto>>

    /** Unified diff for a single file (path is required). */
    @GET("api/v1/app-conversations/{conversation_id}/git/diff")
    suspend fun gitDiff(
        @Path("conversation_id") conversationId: String,
        @Query("path") filePath: String,
        @Query("ref") ref: String? = null
    ): Response<GitDiffDto>

    /** Downloads the conversation trajectory (JSON export). */
    @Streaming
    @GET("api/v1/app-conversations/{conversation_id}/download")
    suspend fun downloadTrajectory(@Path("conversation_id") conversationId: String): Response<ResponseBody>

    /** Skills active in the conversation's sandbox (repo + global). */
    @GET("api/v1/app-conversations/{conversation_id}/skills")
    suspend fun conversationSkills(@Path("conversation_id") conversationId: String): Response<SkillListDto>

    @GET("api/v1/app-conversations/{conversation_id}/hooks")
    suspend fun conversationHooks(@Path("conversation_id") conversationId: String): Response<HookListDto>

    /** Switch the LLM profile used by a running conversation (profile from /settings/profiles). */
    @POST("api/v1/app-conversations/{conversation_id}/switch_profile")
    suspend fun switchProfile(
        @Path("conversation_id") conversationId: String,
        @Body request: SwitchProfileRequest
    ): Response<SuccessDto>

    /** For ACP-backed conversations: switch the model exposed by the ACP server. */
    @POST("api/v1/app-conversations/{conversation_id}/switch_acp_model")
    suspend fun switchAcpModel(
        @Path("conversation_id") conversationId: String,
        @Body request: SwitchAcpModelRequest
    ): Response<SuccessDto>

    // ------------------------------------------------------------------ events

    @GET("api/v1/conversation/{conversation_id}/events/search")
    suspend fun searchEvents(
        @Path("conversation_id") conversationId: String,
        @Query("kind__eq") kind: String? = null,
        @Query("timestamp__gte") timestampGte: String? = null,
        @Query("timestamp__lt") timestampLt: String? = null,
        @Query("sort_order") sortOrder: String = "TIMESTAMP",
        @Query("page_id") pageId: String? = null,
        @Query("limit") limit: Int = 100
    ): Response<PageDto<EventDto>>

    @GET("api/v1/conversation/{conversation_id}/events/count")
    suspend fun countEvents(
        @Path("conversation_id") conversationId: String,
        @Query("kind__eq") kind: String? = null,
        @Query("timestamp__gte") timestampGte: String? = null
    ): Response<Int>

    /** Batch get events by id. `id` is repeated. */
    @GET("api/v1/conversation/{conversation_id}/events")
    suspend fun getEvents(
        @Path("conversation_id") conversationId: String,
        @Query("id") ids: List<String>
    ): Response<List<EventDto?>>

    // ------------------------------------------------------------------ sandboxes

    @GET("api/v1/sandboxes")
    suspend fun getSandboxes(@Query("id") ids: List<String>): Response<List<SandboxDto?>>

    @GET("api/v1/sandboxes/search")
    suspend fun searchSandboxes(
        @Query("page_id") pageId: String? = null,
        @Query("limit") limit: Int = 50
    ): Response<PageDto<SandboxDto>>

    @POST("api/v1/sandboxes")
    suspend fun createSandbox(@Query("sandbox_spec_id") specId: String? = null): Response<SandboxDto>

    @POST("api/v1/sandboxes/{sandbox_id}/pause")
    suspend fun pauseSandbox(@Path("sandbox_id") sandboxId: String): Response<SuccessDto>

    @POST("api/v1/sandboxes/{sandbox_id}/resume")
    suspend fun resumeSandbox(@Path("sandbox_id") sandboxId: String): Response<SuccessDto>

    /** The id is passed as a required query parameter alongside the path segment. */
    @DELETE("api/v1/sandboxes/{id}")
    suspend fun deleteSandbox(
        @Path("id") pathId: String,
        @Query("sandbox_id") sandboxId: String
    ): Response<SuccessDto>

    @GET("api/v1/sandbox-specs/search")
    suspend fun searchSandboxSpecs(
        @Query("page_id") pageId: String? = null,
        @Query("limit") limit: Int = 50
    ): Response<PageDto<SandboxSpecDto>>

    @GET("api/v1/sandbox-specs")
    suspend fun getSandboxSpecs(@Query("id") ids: List<String>): Response<List<SandboxSpecDto?>>

    /** Secret names visible to a sandbox (requires the sandbox session key). */
    @GET("api/v1/sandboxes/{sandbox_id}/settings/secrets")
    suspend fun sandboxSecretNames(@Path("sandbox_id") sandboxId: String): Response<SecretNamesDto>

    // ----------------------------------------------------------------------- git

    /** `provider` is required; `query` is an optional name filter. */
    @GET("api/v1/git/repositories/search")
    suspend fun searchRepositories(
        @Query("provider") provider: String,
        @Query("query") query: String? = null,
        @Query("installation_id") installationId: String? = null,
        @Query("sort_order") sortOrder: String? = null,
        @Query("page_id") pageId: String? = null,
        @Query("limit") limit: Int = 30
    ): Response<PageDto<RepositoryDto>>

    /** `provider`, `repository` and `query` are all required (`query` may be empty). */
    @GET("api/v1/git/branches/search")
    suspend fun searchBranches(
        @Query("provider") provider: String,
        @Query("repository") repository: String,
        @Query("query") query: String = "",
        @Query("page_id") pageId: String? = null,
        @Query("limit") limit: Int = 30
    ): Response<PageDto<BranchDto>>

    @GET("api/v1/git/suggested-tasks/search")
    suspend fun searchSuggestedTasks(
        @Query("page_id") pageId: String? = null,
        @Query("limit") limit: Int = 30
    ): Response<PageDto<SuggestedTaskDto>>

    /** Installation ids of the OpenHands app for a provider (GitHub App installations). */
    @GET("api/v1/git/installations/search")
    suspend fun searchInstallations(
        @Query("provider") provider: String,
        @Query("page_id") pageId: String? = null,
        @Query("limit") limit: Int = 50
    ): Response<PageDto<String>>

    @GET("api/v1/users/git-organizations")
    suspend fun gitOrganizations(): Response<GitOrganizationsDto>

    /** Removes the stored credentials for a provider (e.g. to re-connect). */
    @DELETE("api/v1/users/git-providers/{provider}")
    suspend fun disconnectGitProvider(@Path("provider") provider: String): Response<Unit>

    // ------------------------------------------------------------------- secrets

    @GET("api/v1/secrets/search")
    suspend fun searchSecrets(
        @Query("name__contains") nameContains: String? = null,
        @Query("page_id") pageId: String? = null,
        @Query("limit") limit: Int = 50
    ): Response<PageDto<SecretDto>>

    @POST("api/v1/secrets")
    suspend fun createSecret(@Body request: CreateSecretRequest): Response<MessageResponseDto>

    /** Renames a secret; the value cannot be changed after creation (delete + create instead). */
    @PUT("api/v1/secrets/{secret_id}")
    suspend fun updateSecret(
        @Path("secret_id") secretId: String,
        @Body request: SecretDto
    ): Response<MessageResponseDto>

    @DELETE("api/v1/secrets/{secret_id}")
    suspend fun deleteSecret(@Path("secret_id") secretId: String): Response<MessageResponseDto>

    @POST("api/v1/secrets/git-providers")
    suspend fun setGitProviderTokens(@Body request: SetProviderTokensRequest): Response<MessageResponseDto>

    @DELETE("api/v1/secrets/git-providers")
    suspend fun clearGitProviderTokens(): Response<MessageResponseDto>

    // ---------------------------------------------------------- settings / skills

    /** Free-form settings object (GETSettingsModel / additionalProperties). */
    @GET("api/v1/settings")
    suspend fun settings(): Response<ResponseBody>

    /** Stores a settings patch; body is an arbitrary JSON object. */
    @POST("api/v1/settings")
    suspend fun storeSettings(@Body body: Map<String, @JvmSuppressWildcards Any?>): Response<ResponseBody>

    @GET("api/v1/settings/agent-schema")
    suspend fun agentSettingsSchema(): Response<ResponseBody>

    @GET("api/v1/settings/conversation-schema")
    suspend fun conversationSettingsSchema(): Response<ResponseBody>

    @GET("api/v1/settings/profiles")
    suspend fun listProfiles(): Response<LlmProfileListDto>

    @GET("api/v1/settings/profiles/{name}")
    suspend fun getProfile(@Path("name") name: String): Response<ResponseBody>

    @POST("api/v1/settings/profiles/{name}")
    suspend fun saveProfile(
        @Path("name") name: String,
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): Response<ResponseBody>

    @DELETE("api/v1/settings/profiles/{name}")
    suspend fun deleteProfile(@Path("name") name: String): Response<ResponseBody>

    @POST("api/v1/settings/profiles/{name}/activate")
    suspend fun activateProfile(@Path("name") name: String): Response<ResponseBody>

    @POST("api/v1/settings/profiles/{name}/rename")
    suspend fun renameProfile(
        @Path("name") name: String,
        @Body request: RenameProfileRequest
    ): Response<ResponseBody>

    @GET("api/v1/skills/search")
    suspend fun searchSkills(
        @Query("page_id") pageId: String? = null,
        @Query("limit") limit: Int = 50
    ): Response<PageDto<SkillDto>>

    /** Body is an array of marketplace registrations ({name, source, …}). */
    @POST("api/v1/skills/marketplace-skills")
    suspend fun marketplaceSkills(@Body body: List<@JvmSuppressWildcards Any?>): Response<ResponseBody>

    @GET("api/v1/config/providers/search")
    suspend fun searchProviders(
        @Query("query") query: String? = null,
        @Query("verified__eq") verified: Boolean? = null,
        @Query("page_id") pageId: String? = null,
        @Query("limit") limit: Int = 50
    ): Response<PageDto<LlmProviderDto>>

    @GET("api/v1/web-client/config")
    suspend fun webClientConfig(): Response<WebClientConfigDto>

    // ------------------------------------------------------------- user / config

    @GET("api/v1/users/me")
    suspend fun me(): Response<UserInfoDto>

    @GET("api/v1/users/git-info")
    suspend fun gitUser(): Response<GitUserDto>

    @GET("api/keys/current")
    suspend fun currentApiKey(): Response<CurrentApiKeyDto>

    @GET("api/billing/credits")
    suspend fun credits(): Response<CreditsDto>

    @GET("api/v1/config/models/search")
    suspend fun searchModels(
        @Query("query") query: String? = null,
        @Query("verified__eq") verified: Boolean? = null,
        @Query("provider__eq") provider: String? = null,
        @Query("page_id") pageId: String? = null,
        @Query("limit") limit: Int = 50
    ): Response<PageDto<LlmModelDto>>
}
