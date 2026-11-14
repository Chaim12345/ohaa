package com.example.data.api

import com.example.data.model.ActivateAgentProfileDto
import com.example.data.model.ActivateMetaProfileDto
import com.example.data.model.ActivateProfileResponseDto
import com.example.data.model.AddWorkspaceParentsRequest
import com.example.data.model.AddWorkspacesRequest
import com.example.data.model.AgentConversationPageDto
import com.example.data.model.AgentProfileDetailDto
import com.example.data.model.AgentProfileDiagnosticsDto
import com.example.data.model.AgentProfileMutationDto
import com.example.data.model.AcpModelDiscoveryDto
import com.example.data.model.AppBackendSessionDto
import com.example.data.model.DeleteResponseDto
import com.example.data.model.GitChangeDto
import com.example.data.model.GitPullRequestStatusDto
import com.example.data.model.GitRepositoryPageDto
import com.example.data.model.HealthStatusDto
import com.example.data.model.HooksRequestDto
import com.example.data.model.HooksResponseDto
import com.example.data.model.InitRequestDto
import com.example.data.model.InitStatusDto
import com.example.data.model.InstalledPluginDetailDto
import com.example.data.model.InstalledSkillDetailDto
import com.example.data.model.McpOAuthCallbackRequest
import com.example.data.model.MetaProfileConfigDto
import com.example.data.model.MetaProfileDetailDto
import com.example.data.model.MetaProfileListDto
import com.example.data.model.OpenAiModelListDto
import com.example.data.model.ProfileMutationResponseDto
import com.example.data.model.ProviderConnectionCreateRequest
import com.example.data.model.ProviderConnectionDto
import com.example.data.model.RenameAgentProfileRequest
import com.example.data.model.RenameProfileRequest
import com.example.data.model.RuntimeProfileDetailDto
import com.example.data.model.RuntimeProfileListDto
import com.example.data.model.RuntimeSecretItemDto
import com.example.data.model.RuntimeSecretsListDto
import com.example.data.model.SaveRuntimeProfileRequest
import com.example.data.model.SecretCreateRequest
import com.example.data.model.SendMessageRequestDto
import com.example.data.model.SettingsSchemaDto
import com.example.data.model.StartConversationRequest
import com.example.data.model.SubscriptionDevicePollRequest
import com.example.data.model.SubscriptionDeviceStartDto
import com.example.data.model.SubscriptionModelsDto
import com.example.data.model.SubscriptionStatusDto
import com.example.data.model.UpdatePluginDto
import com.example.data.model.UpdatePluginStateDto
import com.example.data.model.UpdatePluginStateRequest
import com.example.data.model.UpdateSkillDto
import com.example.data.model.UpdateSkillStateDto
import com.example.data.model.UpdateSkillStateRequest
import com.example.data.model.ValidateProfileResponseDto
import com.example.data.model.VerifiedModelsDto
import com.example.data.model.WorkspacesListDto
import com.example.data.model.AgentFinalResponseDto
import com.example.data.model.BashEventDto
import com.example.data.model.BashEventPageDto
import com.example.data.model.ConfirmationPolicyRequest
import com.example.data.model.ConfirmationResponseRequest
import com.example.data.model.ConversationRuntimeDto
import com.example.data.model.FileArchiveDto
import com.example.data.model.ForkConversationRequest
import com.example.data.model.HomeResponseDto
import com.example.data.model.InstallPluginRequest
import com.example.data.model.InstallSkillRequest
import com.example.data.model.InstalledPluginsResponseDto
import com.example.data.model.InstalledSkillsResponseDto
import com.example.data.model.LlmProvidersResponseDto
import com.example.data.model.FetchPluginRequestDto
import com.example.data.model.FetchPluginResponseDto
import com.example.data.model.LoadPluginRequest
import com.example.data.model.McpOAuthStartRequest
import com.example.data.model.McpOAuthStartResponseDto
import com.example.data.model.McpOAuthStatusResponseDto
import com.example.data.model.McpServerInputDto
import com.example.data.model.NavigateConversationRequest
import com.example.data.model.StartChildConversationRequest
import com.example.data.model.McpTestRequest
import com.example.data.model.PluginsRequestDto
import com.example.data.model.SkillsRequestDto
import com.example.data.model.SkillsResponseDto
import com.example.data.model.SubAgentsRequestDto
import com.example.data.model.SubAgentsResponseDto
import com.example.data.model.RuntimeModelsResponseDto
import com.example.data.model.RuntimeSettingsDto
import com.example.data.model.SetSecurityAnalyzerRequest
import com.example.data.model.SkillCatalogDto
import com.example.data.model.StartGoalRequest
import com.example.data.model.ToolCatalogResponseDto
import com.example.data.model.UpdateRuntimeSettingsRequest
import com.example.data.model.UpdateSecretsRequest
import com.example.data.model.VSCodeUrlDto

import com.example.data.model.AgentProfileListDto
import com.example.data.model.AskAgentRequest
import com.example.data.model.AskAgentResponseDto
import com.example.data.model.BashCommandDto
import com.example.data.model.BashOutputDto
import com.example.data.model.ConversationInfoDto
import com.example.data.model.ExecuteBashRequest
import com.example.data.model.FileUploadResultDto
import com.example.data.model.GitCommitsPageDto
import com.example.data.model.GitDiffDto
import com.example.data.model.ServerInfoDto
import com.example.data.model.SubdirectoryPageDto
import com.example.data.model.SuccessResponse
import com.example.data.model.SwitchAcpEffortRequest
import com.example.data.model.SwitchAcpModelRequest
import com.example.data.model.SwitchLlmRequest
import com.example.data.model.SwitchProfileRequest
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PATCH
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Query
import retrofit2.http.Streaming
import retrofit2.http.Url

/**
 * Agent-server (per-sandbox runtime) API.
 *
 * Unlike [OpenHandsApiService] this surface lives at a per-conversation
 * `conversation_url` and authenticates with `X-Session-API-Key`, so every call
 * takes a full `@Url` rather than a relative path. The base URL is still set on
 * the Retrofit instance for the rare relative call.
 */
interface AgentServerApiService {

    // ---- runtime lifecycle -------------------------------------------------

    @GET
    suspend fun getServerInfo(@Url url: String): Response<ServerInfoDto>

    @GET
    suspend fun getConversationInfo(@Url url: String): Response<ConversationInfoDto>

    @POST
    suspend fun runConversation(@Url url: String): Response<SuccessResponse>

    @POST
    suspend fun pauseConversation(@Url url: String): Response<SuccessResponse>

    @POST
    suspend fun interruptConversation(@Url url: String): Response<SuccessResponse>

    @POST
    suspend fun condenseConversation(@Url url: String): Response<SuccessResponse>

    @POST
    suspend fun askAgent(
        @Url url: String,
        @Body body: AskAgentRequest,
    ): Response<AskAgentResponseDto>

    // ---- model / effort switching -----------------------------------------

    @POST
    suspend fun switchLlm(
        @Url url: String,
        @Body body: SwitchLlmRequest,
    ): Response<SuccessResponse>

    @POST
    suspend fun switchAcpModel(
        @Url url: String,
        @Body body: SwitchAcpModelRequest,
    ): Response<SuccessResponse>

    @POST
    suspend fun switchAcpEffort(
        @Url url: String,
        @Body body: SwitchAcpEffortRequest,
    ): Response<SuccessResponse>

    @POST
    suspend fun switchProfile(
        @Url url: String,
        @Body body: SwitchProfileRequest,
    ): Response<SuccessResponse>

    @GET
    suspend fun discoverAcpModels(@Url url: String): Response<AcpModelDiscoveryDto>

    @GET
    suspend fun listAgentProfiles(@Url url: String): Response<AgentProfileListDto>

    @POST
    suspend fun activateAgentProfile(@Url url: String): Response<SuccessResponse>

    @POST
    suspend fun activateAgentProfileDetailed(@Url url: String): Response<ActivateAgentProfileDto>

    // ---- bash --------------------------------------------------------------

    @POST
    suspend fun executeBash(
        @Url url: String,
        @Body body: ExecuteBashRequest,
    ): Response<BashOutputDto>

    @POST
    suspend fun startBash(
        @Url url: String,
        @Body body: ExecuteBashRequest,
    ): Response<BashCommandDto>

    @POST
    suspend fun stopBash(@Url url: String): Response<SuccessResponse>

    // ---- files -------------------------------------------------------------

    @GET
    suspend fun getHome(@Url url: String): Response<ResponseBody>

    @GET
    suspend fun searchSubdirs(
        @Url url: String,
        @Query("path") path: String? = null,
        @Query("limit") limit: Int = 100,
        @Query("page_id") pageId: String? = null,
        @Query("include_hidden") includeHidden: Boolean = false,
    ): Response<SubdirectoryPageDto>

    @Streaming
    @GET
    suspend fun downloadFile(
        @Url url: String,
        @Query("path") path: String,
    ): Response<ResponseBody>

    @Multipart
    @POST
    suspend fun uploadFile(
        @Url url: String,
        @Query("path") path: String,
        @Part file: MultipartBody.Part,
    ): Response<FileUploadResultDto>

    @POST
    suspend fun createDirectory(
        @Url url: String,
        @Query("path") path: String,
    ): Response<SuccessResponse>

    // ---- git ---------------------------------------------------------------

    @GET
    suspend fun getGitChanges(
        @Url url: String,
        @Query("path") path: String? = null,
    ): Response<List<GitChangeDto?>>

    @GET
    suspend fun getGitDiff(
        @Url url: String,
        @Query("path") path: String? = null,
        @Query("commit") commit: String? = null,
    ): Response<GitDiffDto>

    @GET
    suspend fun getGitCommits(
        @Url url: String,
        @Query("path") path: String? = null,
        @Query("limit") limit: Int = 30,
    ): Response<GitCommitsPageDto>

    @GET
    suspend fun getCommitChanges(
        @Url url: String,
        @Query("path") path: String? = null,
    ): Response<List<GitChangeDto?>>

    // ---- goal / confirmation / fork ---------------------------------------

    @POST
    suspend fun startGoal(
        @Url url: String,
        @Body body: StartGoalRequest,
    ): Response<SuccessResponse>

    @POST
    suspend fun resumeGoal(@Url url: String): Response<SuccessResponse>

    @POST
    suspend fun stopGoal(@Url url: String): Response<SuccessResponse>

    @POST
    suspend fun setConfirmationPolicy(
        @Url url: String,
        @Body body: ConfirmationPolicyRequest,
    ): Response<SuccessResponse>

    @POST
    suspend fun respondToConfirmation(
        @Url url: String,
        @Body body: ConfirmationResponseRequest,
    ): Response<SuccessResponse>

    @POST
    suspend fun forkConversation(
        @Url url: String,
        @Body body: ForkConversationRequest,
    ): Response<ConversationInfoDto>

    @GET
    suspend fun getAgentFinalResponse(@Url url: String): Response<AgentFinalResponseDto>

    @GET
    suspend fun getRuntime(@Url url: String): Response<ConversationRuntimeDto>

    @POST
    suspend fun reprovisionRuntime(@Url url: String): Response<ConversationRuntimeDto>

    @POST
    suspend fun setSecurityAnalyzer(
        @Url url: String,
        @Body body: SetSecurityAnalyzerRequest,
    ): Response<SuccessResponse>

    @POST
    suspend fun updateConversationSecrets(
        @Url url: String,
        @Body body: UpdateSecretsRequest,
    ): Response<SuccessResponse>

    // ---- bash events -------------------------------------------------------

    @GET
    suspend fun searchBashEvents(
        @Url url: String,
        @Query("limit") limit: Int = 100,
        @Query("page_id") pageId: String? = null,
    ): Response<BashEventPageDto>

    @GET
    suspend fun getBashEvent(@Url url: String): Response<BashEventDto>

    @DELETE
    suspend fun clearBashEvents(@Url url: String): Response<SuccessResponse>

    // ---- mcp oauth ---------------------------------------------------------

    @POST
    suspend fun startMcpOAuth(
        @Url url: String,
        @Body body: McpOAuthStartRequest,
    ): Response<McpOAuthStartResponseDto>

    @GET
    suspend fun getMcpOAuthStatus(@Url url: String): Response<McpOAuthStatusResponseDto>

    // ---- plugins / skills / sub-agents -------------------------------------

    @POST
    suspend fun listPlugins(
        @Url url: String,
        @Body body: PluginsRequestDto,
    ): Response<InstalledPluginsResponseDto>

    @POST
    suspend fun fetchPlugin(
        @Url url: String,
        @Body body: FetchPluginRequestDto,
    ): Response<FetchPluginResponseDto>

    @POST
    suspend fun listSkills(
        @Url url: String,
        @Body body: SkillsRequestDto,
    ): Response<SkillsResponseDto>

    @GET
    suspend fun getSkillsMarketplace(@Url url: String): Response<ResponseBody>

    @POST
    suspend fun listSubAgents(
        @Url url: String,
        @Body body: SubAgentsRequestDto,
    ): Response<SubAgentsResponseDto>

    @POST
    suspend fun loadConversationPlugin(
        @Url url: String,
        @Body body: LoadPluginRequest,
    ): Response<SuccessResponse>

    @POST
    suspend fun setMcpServer(
        @Url url: String,
        @Body body: McpServerInputDto,
    ): Response<RuntimeSettingsDto>

    // ---- sub-conversations / workspace / navigate --------------------------

    /** Spawns a child (sub-)conversation of this one. */
    @POST
    suspend fun startChildConversation(
        @Url url: String,
        @Body body: StartChildConversationRequest,
    ): Response<ConversationInfoDto>

    @GET
    suspend fun getChildren(@Url url: String): Response<List<ConversationInfoDto?>>

    /** Stops a backgrounded bash command by id. */
    @POST
    suspend fun stopBashCommand(@Url url: String): Response<SuccessResponse>

    @GET
    suspend fun getWorkspace(@Url url: String): Response<ResponseBody>

    /** Re-roots the active branch on a given event (no new conversation). */
    @POST
    suspend fun navigateConversation(
        @Url url: String,
        @Body body: NavigateConversationRequest,
    ): Response<ConversationInfoDto>

    // ---- vscode ------------------------------------------------------------

    @GET
    suspend fun getVSCodeUrl(@Url url: String): Response<VSCodeUrlDto>

    @GET
    suspend fun getVSCodeStatus(@Url url: String): Response<ResponseBody>

    // ---- skills / plugins / mcp / settings ---------------------------------

    @GET
    suspend fun listInstalledSkills(@Url url: String): Response<InstalledSkillsResponseDto>

    @GET
    suspend fun getSkillCatalog(@Url url: String): Response<SkillCatalogDto>

    @POST
    suspend fun installSkill(
        @Url url: String,
        @Body body: InstallSkillRequest,
    ): Response<ResponseBody>

    @POST
    suspend fun syncSkills(@Url url: String): Response<ResponseBody>

    @GET
    suspend fun listInstalledPlugins(@Url url: String): Response<InstalledPluginsResponseDto>

    @POST
    suspend fun installPlugin(
        @Url url: String,
        @Body body: InstallPluginRequest,
    ): Response<ResponseBody>

    @POST
    suspend fun testMcp(
        @Url url: String,
        @Body body: McpTestRequest,
    ): Response<ResponseBody>

    @GET
    suspend fun getRuntimeSettings(@Url url: String): Response<RuntimeSettingsDto>

    @PATCH
    suspend fun updateRuntimeSettings(
        @Url url: String,
        @Body body: UpdateRuntimeSettingsRequest,
    ): Response<RuntimeSettingsDto>

    @GET
    suspend fun getRuntimeProviders(@Url url: String): Response<LlmProvidersResponseDto>

    @GET
    suspend fun getRuntimeModels(@Url url: String): Response<RuntimeModelsResponseDto>

    @GET
    suspend fun getToolCatalog(@Url url: String): Response<ToolCatalogResponseDto>

    // ---- files (extended) --------------------------------------------------

    @GET
    suspend fun getHomeDir(
        @Url url: String,
        @Query("include_hidden") includeHidden: Boolean = false,
    ): Response<HomeResponseDto>

    @GET
    suspend fun getFileArchive(
        @Url url: String,
        @Query("path") path: String,
    ): Response<FileArchiveDto>

    // ---- health / init / hooks --------------------------------------------

    @GET
    suspend fun getHealth(@Url url: String): Response<HealthStatusDto>

    @GET
    suspend fun getReady(@Url url: String): Response<ResponseBody>

    @GET
    suspend fun getInitStatus(@Url url: String): Response<InitStatusDto>

    @POST
    suspend fun init(
        @Url url: String,
        @Body body: InitRequestDto,
    ): Response<InitStatusDto>

    @POST
    suspend fun getHooks(
        @Url url: String,
        @Body body: HooksRequestDto,
    ): Response<HooksResponseDto>

    // ---- conversations (bare agent-server routes) --------------------------

    @GET
    suspend fun listConversations(@Url url: String): Response<List<ConversationInfoDto>>

    @GET
    suspend fun countConversations(@Url url: String): Response<Int>

    @GET
    suspend fun searchConversations(
        @Url url: String,
        @Query("limit") limit: Int = 100,
        @Query("page_id") pageId: String? = null,
    ): Response<AgentConversationPageDto>

    @POST
    suspend fun startConversation(
        @Url url: String,
        @Body body: StartConversationRequest,
    ): Response<ConversationInfoDto>

    @POST
    suspend fun sendMessage(
        @Url url: String,
        @Body body: SendMessageRequestDto,
    ): Response<SuccessResponse>

    // ---- events ------------------------------------------------------------

    @GET
    suspend fun getConversationEvents(
        @Url url: String,
        @Query("start_id") startId: Int? = null,
    ): Response<List<Map<String, Any>>>

    @GET
    suspend fun countConversationEvents(@Url url: String): Response<Int>

    @GET
    suspend fun searchConversationEvents(
        @Url url: String,
        @Query("limit") limit: Int = 100,
        @Query("page_id") pageId: String? = null,
    ): Response<ResponseBody>

    @GET
    suspend fun getConversationEvent(@Url url: String): Response<Map<String, Any>>

    // ---- bash (conversation-prefixed aliases) ------------------------------

    @POST
    suspend fun executeBashConversation(
        @Url url: String,
        @Body body: ExecuteBashRequest,
    ): Response<BashOutputDto>

    @POST
    suspend fun startBashConversation(
        @Url url: String,
        @Body body: ExecuteBashRequest,
    ): Response<BashCommandDto>

    @DELETE
    suspend fun clearConversationBashEvents(@Url url: String): Response<ResponseBody>

    @GET
    suspend fun listConversationBashEvents(@Url url: String): Response<List<Map<String, Any>>>

    @GET
    suspend fun searchConversationBashEvents(
        @Url url: String,
        @Query("limit") limit: Int = 100,
        @Query("page_id") pageId: String? = null,
    ): Response<BashEventPageDto>

    @GET
    suspend fun getConversationBashEvent(@Url url: String): Response<BashEventDto>

    // ---- git (bare + conversation-prefixed) --------------------------------

    @GET
    suspend fun getBareGitChanges(
        @Url url: String,
        @Query("path") path: String? = null,
    ): Response<List<GitChangeDto>>

    @GET
    suspend fun getBareGitCommits(@Url url: String): Response<GitCommitsPageDto>

    @GET
    suspend fun getBareGitDiff(@Url url: String): Response<GitDiffDto>

    @GET
    suspend fun getGitCommitChanges(@Url url: String): Response<List<GitChangeDto>>

    @GET
    suspend fun searchGitRepositories(
        @Url url: String,
        @Query("limit") limit: Int = 100,
        @Query("page_id") pageId: String? = null,
        @Query("query") query: String? = null,
    ): Response<GitRepositoryPageDto>

    @GET
    suspend fun getPullRequest(@Url url: String): Response<GitPullRequestStatusDto>

    // ---- vscode (bare) -----------------------------------------------------

    @GET
    suspend fun getBareVSCodeUrl(@Url url: String): Response<VSCodeUrlDto>

    @GET
    suspend fun getBareVSCodeStatus(@Url url: String): Response<Map<String, Any>>

    // ---- workspaces --------------------------------------------------------

    @GET
    suspend fun listWorkspaces(@Url url: String): Response<WorkspacesListDto>

    @POST
    suspend fun addWorkspaces(
        @Url url: String,
        @Body body: AddWorkspacesRequest,
    ): Response<WorkspacesListDto>

    @DELETE
    suspend fun deleteWorkspaces(@Url url: String): Response<DeleteResponseDto>

    @POST
    suspend fun addWorkspaceParents(
        @Url url: String,
        @Body body: AddWorkspaceParentsRequest,
    ): Response<WorkspacesListDto>

    @DELETE
    suspend fun deleteWorkspaceParents(@Url url: String): Response<DeleteResponseDto>

    // ---- profiles (LLM) ----------------------------------------------------

    @GET
    suspend fun listRuntimeProfiles(@Url url: String): Response<RuntimeProfileListDto>

    @GET
    suspend fun getRuntimeProfile(@Url url: String): Response<RuntimeProfileDetailDto>

    @POST
    suspend fun saveRuntimeProfile(
        @Url url: String,
        @Body body: SaveRuntimeProfileRequest,
    ): Response<ResponseBody>

    @DELETE
    suspend fun deleteRuntimeProfile(@Url url: String): Response<ProfileMutationResponseDto>

    @POST
    suspend fun activateRuntimeProfile(@Url url: String): Response<ActivateProfileResponseDto>

    @POST
    suspend fun renameRuntimeProfile(
        @Url url: String,
        @Body body: RenameProfileRequest,
    ): Response<ProfileMutationResponseDto>

    @POST
    suspend fun validateRuntimeProfile(
        @Url url: String,
        @Body body: SaveRuntimeProfileRequest,
    ): Response<ValidateProfileResponseDto>

    // ---- agent profiles ----------------------------------------------------

    @GET
    suspend fun listAgentProfilesDetailed(@Url url: String): Response<ResponseBody>

    @GET
    suspend fun getAgentProfile(@Url url: String): Response<AgentProfileDetailDto>

    @POST
    suspend fun saveAgentProfile(
        @Url url: String,
        @Body body: Map<String, Any>,
    ): Response<AgentProfileMutationDto>

    @DELETE
    suspend fun deleteAgentProfile(@Url url: String): Response<AgentProfileMutationDto>

    @POST
    suspend fun materializeAgentProfile(@Url url: String): Response<AgentProfileDiagnosticsDto>

    @POST
    suspend fun renameAgentProfile(
        @Url url: String,
        @Body body: RenameAgentProfileRequest,
    ): Response<AgentProfileMutationDto>

    // ---- meta profiles -----------------------------------------------------

    @GET
    suspend fun listMetaProfiles(@Url url: String): Response<MetaProfileListDto>

    @GET
    suspend fun getMetaProfile(@Url url: String): Response<MetaProfileDetailDto>

    @POST
    suspend fun saveMetaProfile(
        @Url url: String,
        @Body body: MetaProfileConfigDto,
    ): Response<ResponseBody>

    @DELETE
    suspend fun deleteMetaProfile(@Url url: String): Response<ProfileMutationResponseDto>

    @POST
    suspend fun activateMetaProfile(@Url url: String): Response<ActivateMetaProfileDto>

    // ---- plugins / skills (installed-item operations) ----------------------

    @GET
    suspend fun getInstalledPlugin(@Url url: String): Response<InstalledPluginDetailDto>

    @PATCH
    suspend fun updatePluginState(
        @Url url: String,
        @Body body: UpdatePluginStateRequest,
    ): Response<UpdatePluginStateDto>

    @DELETE
    suspend fun uninstallPlugin(@Url url: String): Response<ResponseBody>

    @POST
    suspend fun refreshPlugin(@Url url: String): Response<UpdatePluginDto>

    @GET
    suspend fun getPluginMarketplace(@Url url: String): Response<ResponseBody>

    @GET
    suspend fun getInstalledSkill(@Url url: String): Response<InstalledSkillDetailDto>

    @PATCH
    suspend fun updateSkillState(
        @Url url: String,
        @Body body: UpdateSkillStateRequest,
    ): Response<UpdateSkillStateDto>

    @DELETE
    suspend fun uninstallSkill(@Url url: String): Response<ResponseBody>

    @POST
    suspend fun refreshSkill(@Url url: String): Response<UpdateSkillDto>

    // ---- settings schemas + secrets ----------------------------------------

    @GET
    suspend fun getAgentSettingsSchema(@Url url: String): Response<SettingsSchemaDto>

    @GET
    suspend fun getConversationSettingsSchema(@Url url: String): Response<SettingsSchemaDto>

    @GET
    suspend fun listRuntimeSecrets(@Url url: String): Response<RuntimeSecretsListDto>

    @PUT
    suspend fun createRuntimeSecret(
        @Url url: String,
        @Body body: SecretCreateRequest,
    ): Response<RuntimeSecretItemDto>

    @GET
    suspend fun getRuntimeSecret(@Url url: String): Response<ResponseBody>

    @DELETE
    suspend fun deleteRuntimeSecret(@Url url: String): Response<ResponseBody>

    // ---- tools (bare) ------------------------------------------------------

    @GET
    suspend fun getBareToolCatalog(@Url url: String): Response<List<String>>

    // ---- llm provider connections ------------------------------------------

    @GET
    suspend fun listProviderConnections(@Url url: String): Response<List<ProviderConnectionDto>>

    @POST
    suspend fun createProviderConnection(
        @Url url: String,
        @Body body: ProviderConnectionCreateRequest,
    ): Response<ProviderConnectionDto>

    @PATCH
    suspend fun updateProviderConnection(
        @Url url: String,
        @Body body: ProviderConnectionCreateRequest,
    ): Response<ProviderConnectionDto>

    @DELETE
    suspend fun deleteProviderConnection(@Url url: String): Response<ProviderConnectionDto>

    @GET
    suspend fun getVerifiedModels(@Url url: String): Response<VerifiedModelsDto>

    // ---- OpenAI subscription -----------------------------------------------

    @GET
    suspend fun getSubscriptionStatus(@Url url: String): Response<SubscriptionStatusDto>

    @POST
    suspend fun startSubscriptionDevice(@Url url: String): Response<SubscriptionDeviceStartDto>

    @POST
    suspend fun pollSubscriptionDevice(
        @Url url: String,
        @Body body: SubscriptionDevicePollRequest,
    ): Response<SubscriptionStatusDto>

    @POST
    suspend fun logoutSubscription(@Url url: String): Response<SubscriptionStatusDto>

    @GET
    suspend fun getSubscriptionModels(@Url url: String): Response<SubscriptionModelsDto>

    // ---- mcp oauth callback ------------------------------------------------

    @POST
    suspend fun mcpOAuthCallback(
        @Url url: String,
        @Body body: McpOAuthCallbackRequest,
    ): Response<McpOAuthStatusResponseDto>

    // ---- canvas extension app backends -------------------------------------

    @POST
    suspend fun createAppBackendSession(@Url url: String): Response<AppBackendSessionDto>

    @DELETE
    suspend fun deleteAppBackendSession(@Url url: String): Response<ResponseBody>

    // ---- auth workspace session --------------------------------------------

    @POST
    suspend fun createWorkspaceSession(@Url url: String): Response<ResponseBody>

    @DELETE
    suspend fun deleteWorkspaceSession(@Url url: String): Response<ResponseBody>

    // ---- OpenAI-compatible surface -----------------------------------------

    @GET
    suspend fun listOpenAiModels(@Url url: String): Response<OpenAiModelListDto>

    @POST
    suspend fun openAiChatCompletions(
        @Url url: String,
        @Body body: Map<String, Any>,
    ): Response<ResponseBody>

    @POST
    suspend fun openAiResponses(
        @Url url: String,
        @Body body: Map<String, Any>,
    ): Response<ResponseBody>
}
