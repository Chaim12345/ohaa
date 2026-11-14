package com.example.data.api

import com.example.data.model.AcpModelDiscoveryDto
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
import com.example.data.model.GitChangeDto
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
}
