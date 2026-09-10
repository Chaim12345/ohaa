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
import com.example.data.model.*
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
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
    // ------------------------------------------------------------------
    // Full v1.59 surface — remaining 146 operations generated from the
    // live OpenAPI spec (orgs, members, budgets, admin, billing, quota,
    // keys, profiles, agent-profiles, integrations, shared data, OAuth)
    // ------------------------------------------------------------------

    @GET("alive")
    suspend fun aliveAliveGet(): Response<Unit>

    @POST("api/accept_tos")
    suspend fun acceptTos(): Response<Unit>

    @GET("api/admin/feature-flags")
    suspend fun listFlags(
        @Query("org_id") orgId: String? = null
    ): Response<List<FlagModelDto>>

    @POST("api/admin/feature-flags")
    suspend fun createFlag(
        @Body body: CreateFlagRequestDto,
        @Query("org_id") orgId: String? = null
    ): Response<FlagModelDto>

    @GET("api/admin/feature-flags/{key}")
    suspend fun getFlag(
        @Path("key") key: String,
        @Query("org_id") orgId: String? = null
    ): Response<FlagModelDto>

    @PATCH("api/admin/feature-flags/{key}")
    suspend fun updateFlag(
        @Path("key") key: String,
        @Body body: UpdateFlagRequestDto,
        @Query("org_id") orgId: String? = null
    ): Response<FlagModelDto>

    @DELETE("api/admin/feature-flags/{key}")
    suspend fun deleteFlag(
        @Path("key") key: String,
        @Query("org_id") orgId: String? = null
    ): Response<Unit>

    @POST("api/admin/feature-flags/{key}/evaluate")
    suspend fun evaluateFlag(
        @Path("key") key: String,
        @Body body: EvaluateRequestDto,
        @Query("org_id") orgId: String? = null
    ): Response<EvaluateResponseDto>

    @POST("api/admin/feature-flags/{key}/rules")
    suspend fun createRule(
        @Path("key") key: String,
        @Body body: CreateRuleRequestDto,
        @Query("org_id") orgId: String? = null
    ): Response<FlagModelDto>

    @DELETE("api/admin/feature-flags/{key}/rules/{rule_id}")
    suspend fun deleteRule(
        @Path("key") key: String,
        @Path("rule_id") ruleId: String,
        @Query("org_id") orgId: String? = null
    ): Response<FlagModelDto>

    @PUT("api/admin/quota/orgs/{org_id}/quota")
    suspend fun setOrgQuota(
        @Path("org_id") orgId: String,
        @Body body: OrgQuotaUpdateRequestDto
    ): Response<OrgQuotaResponseDto>

    @GET("api/admin/quota/requests")
    suspend fun listPendingQuotaRequests(
        @Query("org_id") orgId: String? = null
    ): Response<List<QuotaIncreaseRequestResponseDto>>

    @POST("api/admin/quota/requests/{request_id}/approve")
    suspend fun approveQuotaRequest(
        @Path("request_id") requestId: String,
        @Query("org_id") orgId: String? = null
    ): Response<QuotaIncreaseRequestResponseDto>

    @POST("api/admin/quota/requests/{request_id}/reject")
    suspend fun rejectQuotaRequest(
        @Path("request_id") requestId: String,
        @Query("org_id") orgId: String? = null
    ): Response<QuotaIncreaseRequestResponseDto>

    @GET("api/admin/super-admins")
    suspend fun listSuperAdmins(
        @Query("org_id") orgId: String? = null
    ): Response<SuperAdminListResponseDto>

    @POST("api/admin/super-admins")
    suspend fun grantSuperAdmin(
        @Body body: GrantSuperAdminRequestDto,
        @Query("org_id") orgId: String? = null
    ): Response<SuperAdminResponseDto>

    @DELETE("api/admin/super-admins/{user_id}")
    suspend fun revokeSuperAdmin(
        @Path("user_id") userId: String,
        @Query("org_id") orgId: String? = null
    ): Response<SuperAdminResponseDto>

    @DELETE("api/admin/users/{user_id}")
    suspend fun deleteUser(
        @Path("user_id") userId: String,
        @Query("org_id") orgId: String? = null
    ): Response<UserLifecycleResponseDto>

    @POST("api/admin/users/{user_id}/disable")
    suspend fun disableUser(
        @Path("user_id") userId: String,
        @Query("org_id") orgId: String? = null
    ): Response<UserLifecycleResponseDto>

    @POST("api/admin/users/{user_id}/enable")
    suspend fun enableUser(
        @Path("user_id") userId: String,
        @Query("org_id") orgId: String? = null
    ): Response<UserLifecycleResponseDto>

    @GET("api/admin/verified-models")
    suspend fun searchVerifiedModels(
        @Query("provider") provider: String? = null,
        @Query("page_id") pageId: String? = null,
        @Query("limit") limit: Int? = null
    ): Response<VerifiedModelPageDto>

    @POST("api/admin/verified-models")
    suspend fun createVerifiedModel(
        @Body body: VerifiedModelCreateDto
    ): Response<VerifiedModelDto>

    @PUT("api/admin/verified-models/{provider}/{model_name}")
    suspend fun updateVerifiedModel(
        @Path("provider") provider: String,
        @Path("model_name") modelName: String,
        @Body body: VerifiedModelUpdateDto
    ): Response<VerifiedModelDto>

    @DELETE("api/admin/verified-models/{provider}/{model_name}")
    suspend fun deleteVerifiedModel(
        @Path("provider") provider: String,
        @Path("model_name") modelName: String
    ): Response<Boolean>

    @GET("api/agent-profiles")
    suspend fun listAgentProfiles(
        @Query("org_id") orgId: String? = null
    ): Response<AgentProfileListResponseDto>

    @GET("api/agent-profiles/{name}")
    suspend fun getAgentProfile(
        @Path("name") `name`: String,
        @Query("org_id") orgId: String? = null
    ): Response<AgentProfileDetailResponseDto>

    @POST("api/agent-profiles/{name}")
    suspend fun saveAgentProfile(
        @Path("name") `name`: String,
        @Query("org_id") orgId: String? = null
    ): Response<AgentProfileMutationResponseDto>

    @DELETE("api/agent-profiles/{name}")
    suspend fun deleteAgentProfile(
        @Path("name") `name`: String,
        @Query("org_id") orgId: String? = null
    ): Response<AgentProfileMutationResponseDto>

    @POST("api/agent-profiles/{name}/materialize")
    suspend fun materializeAgentProfile(
        @Path("name") `name`: String,
        @Query("org_id") orgId: String? = null
    ): Response<AgentProfileDiagnosticsDto>

    @POST("api/agent-profiles/{name}/rename")
    suspend fun renameAgentProfile(
        @Path("name") `name`: String,
        @Body body: RenameAgentProfileRequestDto,
        @Query("org_id") orgId: String? = null
    ): Response<AgentProfileMutationResponseDto>

    @POST("api/agent-profiles/{profile_id}/activate")
    suspend fun activateAgentProfile(
        @Path("profile_id") profileId: String,
        @Query("org_id") orgId: String? = null
    ): Response<ActivateAgentProfileResponseDto>

    @POST("api/analytics/events")
    suspend fun trackFrontendEvent(
        @Body body: CreatePrButtonClickedEventDto
    ): Response<AnalyticsEventResponseDto>

    @POST("api/authenticate")
    suspend fun authenticate(): Response<Unit>

    @GET("api/billing/cancel")
    suspend fun cancelCallback(
        @Query("session_id") sessionId: String
    ): Response<Unit>

    @POST("api/billing/create-checkout-session")
    suspend fun createCheckoutSession(
        @Body body: CreateCheckoutSessionRequestDto
    ): Response<CreateBillingSessionResponseDto>

    @POST("api/billing/create-customer-setup-session")
    suspend fun createCustomerSetupSession(): Response<CreateBillingSessionResponseDto>

    @POST("api/billing/has-payment-method")
    suspend fun hasPaymentMethod(): Response<Boolean>

    @GET("api/billing/subscription-access")
    suspend fun getSubscriptionAccess(): Response<Unit>

    @GET("api/billing/success")
    suspend fun successCallback(
        @Query("session_id") sessionId: String
    ): Response<Unit>

    @POST("api/complete_onboarding")
    suspend fun completeOnboarding(): Response<Unit>

    @POST("api/email")
    suspend fun updateEmail(
        @Body body: EmailUpdateDto
    ): Response<Unit>

    @PUT("api/email/resend")
    suspend fun resendEmailVerification(): Response<Unit>

    @GET("api/email/verified")
    suspend fun verifiedEmail(): Response<Unit>

    @GET("api/keys")
    suspend fun list(): Response<List<ApiKeyResponseDto>>

    @POST("api/keys")
    suspend fun create(
        @Body body: ApiKeyCreateDto
    ): Response<ApiKeyCreateResponseDto>

    @GET("api/keys/llm/byor")
    suspend fun getLlm(): Response<LlmApiKeyResponseDto>

    @GET("api/keys/llm/byor/permitted")
    suspend fun checkByorPermitted(): Response<ByorPermittedResponseDto>

    @POST("api/keys/llm/byor/refresh")
    suspend fun refreshLlm(): Response<LlmApiKeyResponseDto>

    @POST("api/keys/llm/managed/refresh")
    suspend fun refreshManagedLlm(): Response<ManagedLlmApiKeyRefreshResponseDto>

    @DELETE("api/keys/{key_id}")
    suspend fun delete(
        @Path("key_id") keyId: String
    ): Response<MessageResponseDto>

    @POST("api/logout")
    suspend fun logout(): Response<Unit>

    @GET("api/onboarding_status")
    suspend fun onboardingStatus(): Response<Unit>

    @GET("api/organizations")
    suspend fun listUserOrgs(
        @Query("page_id") pageId: String? = null,
        @Query("limit") limit: Int? = null
    ): Response<OrgPageDto>

    @POST("api/organizations")
    suspend fun createOrg(
        @Body body: OrgCreateDto,
        @Query("org_id") orgId: String? = null
    ): Response<OrgResponseDto>

    @GET("api/organizations/app")
    suspend fun getOrgAppSettings(
        @Query("org_id") orgId: String? = null
    ): Response<OrgAppSettingsResponseDto>

    @POST("api/organizations/app")
    suspend fun updateOrgAppSettings(
        @Body body: OrgAppSettingsUpdateDto,
        @Query("org_id") orgId: String? = null
    ): Response<OrgAppSettingsResponseDto>

    @GET("api/organizations/llm")
    suspend fun getLegacyOrgDefaultsSettings(
        @Query("org_id") orgId: String? = null
    ): Response<OrgDefaultsSettingsResponseDto>

    @POST("api/organizations/llm")
    suspend fun updateLegacyOrgDefaultsSettings(
        @Body body: OrgUpdateDto,
        @Query("org_id") orgId: String? = null
    ): Response<OrgDefaultsSettingsResponseDto>

    @GET("api/organizations/members/invite/accept")
    suspend fun acceptInvitationRedirect(
        @Query("token") token: String
    ): Response<Unit>

    @POST("api/organizations/members/invite/accept")
    suspend fun acceptInvitation(
        @Body body: AcceptInvitationRequestDto
    ): Response<AcceptInvitationResponseDto>

    @GET("api/organizations/{org_id}")
    suspend fun getOrg(
        @Path("org_id") orgId: String
    ): Response<OrgResponseDto>

    @DELETE("api/organizations/{org_id}")
    suspend fun deleteOrg(
        @Path("org_id") orgId: String
    ): Response<Unit>

    @PATCH("api/organizations/{org_id}")
    suspend fun updateOrg(
        @Path("org_id") orgId: String,
        @Body body: OrgUpdateDto
    ): Response<OrgResponseDto>

    @GET("api/organizations/{org_id}/budgets")
    suspend fun getOrgBudgetSettings(
        @Path("org_id") orgId: String,
        @Query("users_page") usersPage: Int? = null,
        @Query("users_per_page") usersPerPage: Int? = null,
        @Query("users_search") usersSearch: String? = null,
        @Query("users_status") usersStatus: String? = null
    ): Response<OrgBudgetSettingsResponseDto>

    @PATCH("api/organizations/{org_id}/budgets")
    suspend fun updateOrgBudgetSettings(
        @Path("org_id") orgId: String,
        @Body body: OrgBudgetSettingsUpdateDto,
        @Query("users_page") usersPage: Int? = null,
        @Query("users_per_page") usersPerPage: Int? = null,
        @Query("users_search") usersSearch: String? = null,
        @Query("users_status") usersStatus: String? = null
    ): Response<OrgBudgetSettingsResponseDto>

    @PUT("api/organizations/{org_id}/budgets/overrides/{user_id}")
    suspend fun upsertOrgBudgetOverride(
        @Path("org_id") orgId: String,
        @Path("user_id") userId: String,
        @Body body: OrgBudgetUserOverrideUpdateDto
    ): Response<OrgBudgetUserResponseDto>

    @DELETE("api/organizations/{org_id}/budgets/overrides/{user_id}")
    suspend fun deleteOrgBudgetOverride(
        @Path("org_id") orgId: String,
        @Path("user_id") userId: String
    ): Response<Unit>

    @GET("api/organizations/{org_id}/conversations")
    suspend fun listOrgConversations(
        @Path("org_id") orgId: String,
        @Query("search") search: String? = null,
        @Query("sort_by") sortBy: String? = null,
        @Query("sort_order") sortOrder: String? = null,
        @Query("execution_status") executionStatus: String? = null,
        @Query("sandbox_status") sandboxStatus: String? = null,
        @Query("time_window") timeWindow: String? = null,
        @Query("page") page: Int? = null,
        @Query("per_page") perPage: Int? = null,
        @Query("include_sub_conversations") includeSubConversations: Boolean? = null
    ): Response<OrgConversationPageDto>

    @GET("api/organizations/{org_id}/conversations/export")
    suspend fun exportOrgConversationsCsv(
        @Path("org_id") orgId: String,
        @Query("search") search: String? = null,
        @Query("sort_by") sortBy: String? = null,
        @Query("sort_order") sortOrder: String? = null,
        @Query("execution_status") executionStatus: String? = null,
        @Query("sandbox_status") sandboxStatus: String? = null,
        @Query("time_window") timeWindow: String? = null,
        @Query("include_sub_conversations") includeSubConversations: Boolean? = null
    ): Response<Unit>

    @GET("api/organizations/{org_id}/conversations/stats")
    suspend fun getOrgConversationStats(
        @Path("org_id") orgId: String
    ): Response<OrgConversationStatsDto>

    @GET("api/organizations/{org_id}/conversations/usage-stats")
    suspend fun getOrgConversationUsageStats(
        @Path("org_id") orgId: String,
        @Query("time_window") timeWindow: String? = null,
        @Query("days") days: String? = null
    ): Response<OrgUsageStatsDto>

    @GET("api/organizations/{org_id}/conversations/user-usage")
    suspend fun getOrgConversationUserUsageStats(
        @Path("org_id") orgId: String,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null
    ): Response<OrgUserUsageStatsDto>

    @GET("api/organizations/{org_id}/conversations/{conversation_id}")
    suspend fun getOrgConversation(
        @Path("org_id") orgId: String,
        @Path("conversation_id") conversationId: String
    ): Response<OrgConversationResponseDto>

    @POST("api/organizations/{org_id}/conversations/{conversation_id}/stop")
    suspend fun stopOrgConversation(
        @Path("org_id") orgId: String,
        @Path("conversation_id") conversationId: String
    ): Response<Unit>

    @GET("api/organizations/{org_id}/git-claims")
    suspend fun getGitClaims(
        @Path("org_id") orgId: String
    ): Response<List<GitOrgClaimResponseDto>>

    @POST("api/organizations/{org_id}/git-claims")
    suspend fun claimGitOrganization(
        @Path("org_id") orgId: String,
        @Body body: GitOrgClaimRequestDto
    ): Response<GitOrgClaimResponseDto>

    @DELETE("api/organizations/{org_id}/git-claims/{claim_id}")
    suspend fun disconnectGitOrganization(
        @Path("org_id") orgId: String,
        @Path("claim_id") claimId: String
    ): Response<Unit>

    @GET("api/organizations/{org_id}/me")
    suspend fun getMe(
        @Path("org_id") orgId: String
    ): Response<MeResponseDto>

    @GET("api/organizations/{org_id}/members")
    suspend fun getOrgMembers(
        @Path("org_id") orgId: String,
        @Query("page_id") pageId: String? = null,
        @Query("limit") limit: Int? = null,
        @Query("email") email: String? = null
    ): Response<OrgMemberPageDto>

    @GET("api/organizations/{org_id}/members/count")
    suspend fun getOrgMembersCount(
        @Path("org_id") orgId: String,
        @Query("email") email: String? = null
    ): Response<Int>

    @GET("api/organizations/{org_id}/members/financial")
    suspend fun getOrgMembersFinancial(
        @Path("org_id") orgId: String,
        @Query("page_id") pageId: String? = null,
        @Query("limit") limit: Int? = null,
        @Query("email") email: String? = null
    ): Response<OrgMemberFinancialPageDto>

    @POST("api/organizations/{org_id}/members/invite")
    suspend fun createInvitation(
        @Path("org_id") orgId: String,
        @Body body: InvitationCreateDto
    ): Response<BatchInvitationResponseDto>

    @GET("api/organizations/{org_id}/members/invite")
    suspend fun listPendingInvitations(
        @Path("org_id") orgId: String
    ): Response<PendingInvitationsResponseDto>

    @DELETE("api/organizations/{org_id}/members/invite/{invitation_id}")
    suspend fun revokeInvitation(
        @Path("org_id") orgId: String,
        @Path("invitation_id") invitationId: String
    ): Response<Unit>

    @GET("api/organizations/{org_id}/members/{user_id}")
    suspend fun getOrgMember(
        @Path("org_id") orgId: String,
        @Path("user_id") userId: String
    ): Response<OrgMemberResponseDto>

    @DELETE("api/organizations/{org_id}/members/{user_id}")
    suspend fun removeOrgMember(
        @Path("org_id") orgId: String,
        @Path("user_id") userId: String
    ): Response<Unit>

    @PATCH("api/organizations/{org_id}/members/{user_id}")
    suspend fun updateOrgMember(
        @Path("org_id") orgId: String,
        @Path("user_id") userId: String,
        @Body body: OrgMemberUpdateDto
    ): Response<OrgMemberResponseDto>

    @GET("api/organizations/{org_id}/profiles")
    suspend fun orgListProfiles(
        @Path("org_id") orgId: String
    ): Response<ProfileListResponseDto>

    @GET("api/organizations/{org_id}/profiles/{name}")
    suspend fun orgGetProfile(
        @Path("org_id") orgId: String,
        @Path("name") `name`: String
    ): Response<ProfileDetailResponseDto>

    @POST("api/organizations/{org_id}/profiles/{name}")
    suspend fun orgSaveProfile(
        @Path("org_id") orgId: String,
        @Path("name") `name`: String,
        @Body body: SaveProfileRequestDto
    ): Response<ProfileMutationResponseDto>

    @DELETE("api/organizations/{org_id}/profiles/{name}")
    suspend fun orgDeleteProfile(
        @Path("org_id") orgId: String,
        @Path("name") `name`: String
    ): Response<ProfileMutationResponseDto>

    @POST("api/organizations/{org_id}/profiles/{name}/activate")
    suspend fun orgActivateProfile(
        @Path("org_id") orgId: String,
        @Path("name") `name`: String
    ): Response<ActivateProfileResponseDto>

    @POST("api/organizations/{org_id}/profiles/{name}/rename")
    suspend fun orgRenameProfile(
        @Path("org_id") orgId: String,
        @Path("name") `name`: String,
        @Body body: RenameProfileRequest
    ): Response<ProfileMutationResponseDto>

    @GET("api/organizations/{org_id}/provider-connections")
    suspend fun listProviderConnections(
        @Path("org_id") orgId: String
    ): Response<ProviderConnectionListResponseDto>

    @POST("api/organizations/{org_id}/provider-connections")
    suspend fun createProviderConnection(
        @Path("org_id") orgId: String,
        @Body body: ProviderConnectionCreateRequestDto
    ): Response<ProviderConnectionResponseDto>

    @PATCH("api/organizations/{org_id}/provider-connections/{connection_id}")
    suspend fun updateProviderConnection(
        @Path("org_id") orgId: String,
        @Path("connection_id") connectionId: String,
        @Body body: ProviderConnectionUpdateRequestDto
    ): Response<ProviderConnectionResponseDto>

    @DELETE("api/organizations/{org_id}/provider-connections/{connection_id}")
    suspend fun deleteProviderConnection(
        @Path("org_id") orgId: String,
        @Path("connection_id") connectionId: String
    ): Response<ProviderConnectionResponseDto>

    @GET("api/organizations/{org_id}/settings")
    suspend fun getOrgDefaultsSettings(
        @Path("org_id") orgId: String
    ): Response<OrgDefaultsSettingsResponseDto>

    @PATCH("api/organizations/{org_id}/settings")
    suspend fun updateOrgDefaultsSettings(
        @Path("org_id") orgId: String,
        @Body body: OrgUpdateDto
    ): Response<OrgDefaultsSettingsResponseDto>

    @POST("api/organizations/{org_id}/switch")
    suspend fun switchOrg(
        @Path("org_id") orgId: String
    ): Response<OrgResponseDto>

    @POST("api/quota/increase-request")
    suspend fun createQuotaIncreaseRequest(): Response<Unit>

    @GET("api/quota/status")
    suspend fun getQuotaStatus(): Response<QuotaStatusResponseDto>

    @GET("api/quota/verify")
    suspend fun verifyQuotaIncrease(): Response<Unit>

    @GET("api/refresh-tokens")
    suspend fun refreshTokens(
        @Query("provider") provider: String,
        @Query("sid") sid: String
    ): Response<TokenResponseDto>

    @POST("api/service/users/{user_id}/orgs/{org_id}/api-keys")
    suspend fun getOrCreate(
        @Path("user_id") userId: String,
        @Path("org_id") orgId: String,
        @Body body: CreateUserApiKeyRequestDto
    ): Response<CreateUserApiKeyResponseDto>

    @DELETE("api/service/users/{user_id}/orgs/{org_id}/api-keys/{key_name}")
    suspend fun deleteUser2(
        @Path("user_id") userId: String,
        @Path("org_id") orgId: String,
        @Path("key_name") keyName: String
    ): Response<Unit>

    @GET("api/shared-conversations")
    suspend fun batchGetSharedConversations(
        @Query("ids") ids: String
    ): Response<Unit>

    @GET("api/shared-events")
    suspend fun batchGetSharedEvents(
        @Query("conversation_id") conversationId: String,
        @Query("id") id: String
    ): Response<Unit>

    @GET("api/shared-events/count")
    suspend fun countSharedEvents(
        @Query("conversation_id") conversationId: String,
        @Query("kind__eq") kindEq: String? = null,
        @Query("timestamp__gte") timestampGte: String? = null,
        @Query("timestamp__lt") timestampLt: String? = null
    ): Response<Int>

    @GET("api/shared-events/search")
    suspend fun searchSharedEvents(
        @Query("conversation_id") conversationId: String,
        @Query("kind__eq") kindEq: String? = null,
        @Query("timestamp__gte") timestampGte: String? = null,
        @Query("timestamp__lt") timestampLt: String? = null,
        @Query("sort_order") sortOrder: String? = null,
        @Query("page_id") pageId: String? = null,
        @Query("limit") limit: Int? = null
    ): Response<EventPageDto>

    @GET("api/shared-events/{conversation_id}/{event_id}")
    suspend fun getSharedEvent(
        @Path("conversation_id") conversationId: String,
        @Path("event_id") eventId: String
    ): Response<Unit>

    @GET("api/users/app")
    suspend fun getUserAppSettings(): Response<UserAppSettingsResponseDto>

    @POST("api/users/app")
    suspend fun updateUserAppSettings(
        @Body body: UserAppSettingsUpdateDto
    ): Response<UserAppSettingsResponseDto>

    @GET("api/v1/sandboxes/{sandbox_id}/settings/secrets/{secret_name}")
    suspend fun getSecretValue(
        @Path("secret_name") secretName: String,
        @Path("sandbox_id") sandboxId: String
    ): Response<Unit>

    @POST("api/v1/webhooks/conversations")
    suspend fun onConversationUpdate(
        @Body body: ConversationInfoDto
    ): Response<SuccessDto>

    @POST("api/v1/webhooks/events/{conversation_id}")
    suspend fun onEvent(
        @Path("conversation_id") conversationId: String,
        @Body body: List<EventDto>
    ): Response<SuccessDto>

    @GET("api/v1/webhooks/secrets")
    suspend fun getSecret(): Response<Unit>

    @GET("health")
    suspend fun healthHealthGet(): Response<String>

    @POST("integration/bitbucket/events")
    suspend fun bitbucketEvents(): Response<Unit>

    @POST("integration/github/events")
    suspend fun githubEvents(): Response<Unit>

    @POST("integration/gitlab/events")
    suspend fun gitlabEvents(): Response<Unit>

    @POST("integration/gitlab/reinstall-webhook")
    suspend fun reinstallGitlabWebhook(
        @Body body: ReinstallWebhookRequestDto
    ): Response<ResourceInstallationResultDto>

    @GET("integration/gitlab/resources")
    suspend fun getGitlabResources(): Response<GitLabResourcesResponseDto>

    @GET("integration/jira/callback")
    suspend fun jiraCallback(
        @Query("code") code: String,
        @Query("state") state: String
    ): Response<Unit>

    @POST("integration/jira/events")
    suspend fun jiraEvents(): Response<Unit>

    @POST("integration/jira/workspaces")
    suspend fun createJiraWorkspace(
        @Body body: JiraWorkspaceCreateDto,
        @Query("org_id") orgId: String? = null
    ): Response<Unit>

    @GET("integration/jira/workspaces/link")
    suspend fun getCurrentWorkspaceLink(): Response<JiraUserResponseDto>

    @POST("integration/jira/workspaces/link")
    suspend fun createWorkspaceLink(
        @Body body: JiraLinkCreateDto
    ): Response<Unit>

    @GET("integration/jira/workspaces/status")
    suspend fun getJiraInstanceStatus(): Response<JiraInstanceStatusResponseDto>

    @POST("integration/jira/workspaces/unlink")
    suspend fun unlinkWorkspace(): Response<Unit>

    @GET("integration/jira/workspaces/validate/{workspace_name}")
    suspend fun validateWorkspace(
        @Path("workspace_name") workspaceName: String
    ): Response<JiraValidateWorkspaceResponseDto>

    @POST("oauth/device/authorize")
    suspend fun deviceAuthorization(): Response<DeviceAuthorizationResponseDto>

    @POST("oauth/device/token")
    @FormUrlEncoded
    suspend fun deviceToken(
        @Body formBody: Map<String, String>
    ): Response<Unit>

    @POST("oauth/device/verify-authenticated")
    @FormUrlEncoded
    suspend fun deviceVerificationAuthenticated(
        @Body formBody: Map<String, String>
    ): Response<Unit>

    @GET("oauth/github/callback")
    suspend fun githubDummyCallback(): Response<Unit>

    @GET("oauth/keycloak/callback")
    suspend fun keycloakCallback(
        @Query("code") code: String? = null,
        @Query("state") state: String? = null,
        @Query("error") error: String? = null,
        @Query("error_description") errorDescription: String? = null,
        @Query("kc_action_status") kcActionStatus: String? = null
    ): Response<Unit>

    @GET("oauth/keycloak/offline/callback")
    suspend fun keycloakOfflineCallback(
        @Query("code") code: String,
        @Query("state") state: String
    ): Response<Unit>

    @GET("ready")
    suspend fun isReadyReadyGet(): Response<Unit>

    @GET("saas")
    suspend fun isSaasSaasGet(): Response<Unit>

    @GET("server_info")
    suspend fun getServerInfoServerInfoGet(): Response<Unit>

    @GET("slack/install")
    suspend fun install(
        @Query("state") state: String? = null
    ): Response<Unit>

    @GET("slack/install-callback")
    suspend fun installCallback(
        @Query("code") code: String? = null,
        @Query("state") state: String? = null,
        @Query("error") error: String? = null
    ): Response<Unit>

    @GET("slack/keycloak-callback")
    suspend fun keycloakCallback2(
        @Query("code") code: String? = null,
        @Query("state") state: String? = null,
        @Query("error") error: String? = null
    ): Response<Unit>

    @POST("slack/on-event")
    suspend fun onEvent2(): Response<Unit>

    @POST("slack/on-form-interaction")
    suspend fun onFormInteraction(): Response<Unit>

    @POST("slack/on-options-load")
    suspend fun onOptionsLoad(): Response<Unit>

}
