package com.example.data.repository

import com.example.data.api.ApiClient
import com.example.data.api.AgentServerApiService
import com.example.data.local.ConversationDao
import com.example.data.model.InstallCanvasExtensionRequest
import com.example.data.model.InstalledCanvasExtensionDto
import com.example.data.model.InstalledCanvasExtensionsListDto
import com.example.data.model.UpdateCanvasExtensionStateRequest
import com.example.data.model.UpdateCanvasExtensionStateDto
import com.example.data.model.UninstallCanvasExtensionDto
import com.example.data.model.CanvasBackendStatusDto
import com.example.data.model.CanvasBackendLogsDto
import com.example.data.model.BackendRevisionRequest
import com.example.data.model.BackendSecretGrantRequest
import com.example.data.model.ActivateAgentProfileDto
import com.example.data.model.ActivateMetaProfileDto
import com.example.data.model.ActivateProfileResponseDto
import com.example.data.model.AddWorkspaceParentsRequest
import com.example.data.model.AddWorkspacesRequest
import com.example.data.model.AgentProfileDetailDto
import com.example.data.model.AgentProfileDiagnosticsDto
import com.example.data.model.AgentProfileMutationDto
import com.example.data.model.AppBackendSessionDto
import com.example.data.model.DeleteResponseDto
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
import com.example.data.model.AcpModelDiscoveryDto
import com.example.data.model.AgentFinalResponseDto
import com.example.data.model.BashCommandDto
import com.example.data.model.BashEventDto
import com.example.data.model.BashEventPageDto
import com.example.data.model.ConfirmationPolicyRequest
import com.example.data.model.ConfirmationResponseRequest
import com.example.data.model.ConversationRuntimeDto
import com.example.data.model.FileArchiveDto
import com.example.data.model.FetchPluginRequestDto
import com.example.data.model.FetchPluginResponseDto
import com.example.data.model.ForkConversationRequest
import com.example.data.model.LoadPluginRequest
import com.example.data.model.McpOAuthStartRequest
import com.example.data.model.McpOAuthStartResponseDto
import com.example.data.model.McpOAuthStatusResponseDto
import com.example.data.model.McpServerInputDto
import com.example.data.model.NavigateConversationRequest
import com.example.data.model.StartChildConversationRequest
import com.example.data.model.PluginsRequestDto
import com.example.data.model.SkillsRequestDto
import com.example.data.model.SubAgentDto
import com.example.data.model.SubAgentsRequestDto
import com.example.data.model.InstallPluginRequest
import com.example.data.model.InstallSkillRequest
import com.example.data.model.InstalledPluginDto
import com.example.data.model.InstalledSkillDto
import com.example.data.model.LlmProviderDto
import com.example.data.model.McpTestRequest
import com.example.data.model.RuntimeLlmModelDto
import com.example.data.model.RuntimeSettingsDto
import com.example.data.model.SetSecurityAnalyzerRequest
import com.example.data.model.StartGoalRequest
import com.example.data.model.ToolCatalogEntryDto
import com.example.data.model.UpdateRuntimeSettingsRequest
import com.example.data.model.UpdateSecretsRequest
import com.example.data.model.VSCodeUrlDto

import com.example.data.model.AskAgentResponseDto
import com.example.data.model.BashOutputDto
import com.example.data.model.ConversationInfoDto
import com.example.data.model.ExecuteBashRequest
import com.example.data.model.FileEntryDto
import com.example.data.model.GitChange
import com.example.data.model.GitCommitsPageDto
import com.example.data.model.GitChangeDto
import com.example.data.model.GitDiffDto
import com.example.data.model.ServerInfoDto
import com.example.data.model.SwitchAcpEffortRequest
import com.example.data.model.SwitchAcpModelRequest
import com.example.data.model.SwitchLlmRequest
import com.example.data.model.SwitchProfileRequest
import com.example.data.model.StartConversationRequest
import com.example.data.model.SuccessResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response

/**
 * Runtime (agent-server) operations for a single conversation.
 *
 * Every call resolves the conversation's `conversation_url` and
 * `session_api_key` from Room, then hits the sandbox's own API. This keeps the
 * per-sandbox credential out of the ViewModel and in one place.
 */
class AgentServerRepository(
    private val conversationDao: ConversationDao,
) {
    private class RuntimeTarget(val baseUrl: String, val service: AgentServerApiService)

    private suspend fun target(conversationId: String): Result<RuntimeTarget> {
        val entity = conversationDao.getConversationDirect(conversationId)
            ?: return Result.failure(Exception("Conversation not found in local cache"))
        val url = entity.conversationUrl?.takeIf { it.isNotBlank() }
            ?: return Result.failure(Exception("This conversation has no runtime URL yet"))
        val key = entity.sessionApiKey?.takeIf { it.isNotBlank() }
            ?: return Result.failure(Exception("Missing sandbox session key — refresh the conversation"))
        return Result.success(RuntimeTarget(url, ApiClient.agentServerFor(url, key)))
    }

    private fun endpoint(baseUrl: String, path: String): String {
        val trimmed = baseUrl.trimEnd('/')
        return "$trimmed$path"
    }

    private fun <T> fail(response: Response<T>): Result<T> {
        val code = response.code()
        val message = when (code) {
            401, 403 -> "Sandbox rejected the session key (HTTP $code). Refresh the conversation."
            404 -> "Not found in the sandbox (HTTP 404). It may have paused or restarted."
            409 -> "The sandbox is not in a state that allows this (HTTP 409). Resume it and retry."
            502, 504 -> "Sandbox gateway timed out (HTTP $code). Try again."
            else -> ApiClient.errorMessage(response)
        }
        return Result.failure(Exception(message))
    }

    private inline fun <T> call(
        block: () -> Response<T>
    ): Result<T> = try {
        val response = block()
        if (response.isSuccessful) {
            @Suppress("UNCHECKED_CAST")
            Result.success((response.body() ?: Unit) as T)
        } else {
            fail(response)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ---- lifecycle ---------------------------------------------------------

    suspend fun serverInfo(conversationId: String): Result<ServerInfoDto> = withContext(Dispatchers.IO) {
        val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
        call { t.service.getServerInfo(endpoint(t.baseUrl, "/server_info")) }
    }

    suspend fun conversationInfo(conversationId: String): Result<ConversationInfoDto> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call {
                t.service.getConversationInfo(
                    endpoint(t.baseUrl, "/api/conversations/$conversationId")
                )
            }
        }

    suspend fun run(conversationId: String): Result<SuccessResponse> = withContext(Dispatchers.IO) {
        val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
        call { t.service.runConversation(endpoint(t.baseUrl, "/api/conversations/$conversationId/run")) }
    }

    suspend fun pause(conversationId: String): Result<SuccessResponse> = withContext(Dispatchers.IO) {
        val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
        call { t.service.pauseConversation(endpoint(t.baseUrl, "/api/conversations/$conversationId/pause")) }
    }

    suspend fun interrupt(conversationId: String): Result<SuccessResponse> = withContext(Dispatchers.IO) {
        val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
        call { t.service.interruptConversation(endpoint(t.baseUrl, "/api/conversations/$conversationId/interrupt")) }
    }

    suspend fun condense(conversationId: String): Result<SuccessResponse> = withContext(Dispatchers.IO) {
        val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
        call { t.service.condenseConversation(endpoint(t.baseUrl, "/api/conversations/$conversationId/condense")) }
    }

    suspend fun askAgent(conversationId: String, question: String): Result<AskAgentResponseDto> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call {
                t.service.askAgent(
                    endpoint(t.baseUrl, "/api/conversations/$conversationId/ask_agent"),
                    com.example.data.model.AskAgentRequest(question)
                )
            }
        }

    // ---- model / effort ----------------------------------------------------

    suspend fun switchLlm(conversationId: String, model: String): Result<SuccessResponse> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call {
                t.service.switchLlm(
                    endpoint(t.baseUrl, "/api/conversations/$conversationId/switch_llm"),
                    SwitchLlmRequest(model)
                )
            }
        }

    suspend fun switchAcpModel(conversationId: String, model: String): Result<SuccessResponse> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call {
                t.service.switchAcpModel(
                    endpoint(t.baseUrl, "/api/conversations/$conversationId/switch_acp_model"),
                    SwitchAcpModelRequest(model)
                )
            }
        }

    suspend fun switchAcpEffort(conversationId: String, effort: String): Result<SuccessResponse> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call {
                t.service.switchAcpEffort(
                    endpoint(t.baseUrl, "/api/conversations/$conversationId/switch_acp_effort"),
                    SwitchAcpEffortRequest(effort)
                )
            }
        }

    suspend fun switchProfile(conversationId: String, profileName: String): Result<SuccessResponse> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call {
                t.service.switchProfile(
                    endpoint(t.baseUrl, "/api/conversations/$conversationId/switch_profile"),
                    SwitchProfileRequest(profileName)
                )
            }
        }

    suspend fun discoverAcpModels(conversationId: String): Result<AcpModelDiscoveryDto> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.discoverAcpModels(endpoint(t.baseUrl, "/api/acp/models")) }
        }

    suspend fun agentProfiles(conversationId: String): Result<List<com.example.data.model.AgentProfileInfoDto>> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.listAgentProfiles(endpoint(t.baseUrl, "/api/agent-profiles")) }
                .map { it.profiles.orEmpty() }
        }

    suspend fun activateAgentProfile(conversationId: String, profileId: String): Result<SuccessResponse> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.activateAgentProfile(endpoint(t.baseUrl, "/api/agent-profiles/$profileId/activate")) }
        }

    // ---- bash --------------------------------------------------------------

    suspend fun executeBash(
        conversationId: String,
        command: String,
        cwd: String? = null,
    ): Result<BashOutputDto> = withContext(Dispatchers.IO) {
        val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
        call {
            t.service.executeBash(
                endpoint(t.baseUrl, "/api/bash/execute_bash_command"),
                ExecuteBashRequest(command = command, cwd = cwd)
            )
        }
    }

    // ---- files -------------------------------------------------------------

    suspend fun listFiles(conversationId: String, path: String?): Result<List<FileEntryDto>> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call {
                t.service.searchSubdirs(endpoint(t.baseUrl, "/api/file/search_subdirs"), path = path)
            }.map { it.items.orEmpty() }
        }

    suspend fun uploadFile(
        conversationId: String,
        path: String,
        bytes: ByteArray,
        fileName: String,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
        val body = bytes.toRequestBody("application/octet-stream".toMediaType())
        val part = MultipartBody.Part.createFormData("file", fileName, body)
        call { t.service.uploadFile(endpoint(t.baseUrl, "/api/file/upload"), path, part) }.map { _ -> Unit }
    }

    // ---- git ---------------------------------------------------------------

    suspend fun gitChanges(conversationId: String): Result<List<GitChange>> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.getGitChanges(endpoint(t.baseUrl, "/api/git/changes")) }.map { list ->
                list.mapNotNull { dto ->
                    val path = dto?.path?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    GitChange(status = dto.status ?: "MODIFIED", path = path)
                }
            }
        }

    suspend fun gitDiff(conversationId: String, path: String?): Result<GitDiffDto> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.getGitDiff(endpoint(t.baseUrl, "/api/git/diff"), path = path) }
        }

    suspend fun gitCommits(conversationId: String, path: String? = null): Result<GitCommitsPageDto> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.getGitCommits(endpoint(t.baseUrl, "/api/git/commits"), path = path) }
        }

    // ---- goal / confirmation / fork ---------------------------------------

    suspend fun startGoal(conversationId: String, goal: String): Result<SuccessResponse> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call {
                t.service.startGoal(
                    endpoint(t.baseUrl, "/api/conversations/$conversationId/goal"),
                    StartGoalRequest(goal = goal)
                )
            }
        }

    suspend fun resumeGoal(conversationId: String): Result<SuccessResponse> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.resumeGoal(endpoint(t.baseUrl, "/api/conversations/$conversationId/goal/resume")) }
        }

    suspend fun stopGoal(conversationId: String): Result<SuccessResponse> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.stopGoal(endpoint(t.baseUrl, "/api/conversations/$conversationId/goal/stop")) }
        }

    suspend fun setConfirmationPolicy(conversationId: String, policy: String): Result<SuccessResponse> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call {
                t.service.setConfirmationPolicy(
                    endpoint(t.baseUrl, "/api/conversations/$conversationId/confirmation_policy"),
                    ConfirmationPolicyRequest(policy)
                )
            }
        }

    suspend fun respondToConfirmation(conversationId: String, accept: Boolean, reason: String? = null): Result<SuccessResponse> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call {
                t.service.respondToConfirmation(
                    endpoint(t.baseUrl, "/api/conversations/$conversationId/events/respond_to_confirmation"),
                    ConfirmationResponseRequest(accept = accept, reason = reason)
                )
            }
        }

    suspend fun forkConversation(conversationId: String, includeSkills: Boolean = true): Result<ConversationInfoDto> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call {
                t.service.forkConversation(
                    endpoint(t.baseUrl, "/api/conversations/$conversationId/fork"),
                    ForkConversationRequest(includeSkills)
                )
            }
        }

    suspend fun agentFinalResponse(conversationId: String): Result<AgentFinalResponseDto> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.getAgentFinalResponse(endpoint(t.baseUrl, "/api/conversations/$conversationId/agent_final_response")) }
        }

    suspend fun runtimeInfo(conversationId: String): Result<ConversationRuntimeDto> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.getRuntime(endpoint(t.baseUrl, "/api/conversations/$conversationId/runtime")) }
        }

    suspend fun reprovisionRuntime(conversationId: String): Result<ConversationRuntimeDto> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.reprovisionRuntime(endpoint(t.baseUrl, "/api/conversations/$conversationId/runtime/reprovision")) }
        }

    suspend fun setSecurityAnalyzer(conversationId: String, enabled: Boolean): Result<SuccessResponse> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call {
                t.service.setSecurityAnalyzer(
                    endpoint(t.baseUrl, "/api/conversations/$conversationId/security_analyzer"),
                    SetSecurityAnalyzerRequest(enabled)
                )
            }
        }

    suspend fun updateConversationSecrets(conversationId: String, secrets: Map<String, String>): Result<SuccessResponse> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call {
                t.service.updateConversationSecrets(
                    endpoint(t.baseUrl, "/api/conversations/$conversationId/secrets"),
                    UpdateSecretsRequest(secrets)
                )
            }
        }

    // ---- bash events -------------------------------------------------------

    suspend fun bashEvents(conversationId: String, limit: Int = 100): Result<BashEventPageDto> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.searchBashEvents(endpoint(t.baseUrl, "/api/bash/bash_events/search"), limit = limit) }
        }

    suspend fun bashEvent(conversationId: String, eventId: String): Result<BashEventDto> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.getBashEvent(endpoint(t.baseUrl, "/api/bash/bash_events/$eventId")) }
        }

    suspend fun clearBashEvents(conversationId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.clearBashEvents(endpoint(t.baseUrl, "/api/bash/bash_events")) }.map { Unit }
        }

    suspend fun startBash(conversationId: String, command: String): Result<BashCommandDto> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call {
                t.service.startBash(
                    endpoint(t.baseUrl, "/api/bash/start_bash_command"),
                    ExecuteBashRequest(command = command)
                )
            }
        }

    // ---- mcp oauth ---------------------------------------------------------

    suspend fun startMcpOAuth(conversationId: String, name: String): Result<McpOAuthStartResponseDto> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call {
                t.service.startMcpOAuth(
                    endpoint(t.baseUrl, "/api/mcp/oauth/start"),
                    McpOAuthStartRequest(name = name)
                )
            }
        }

    suspend fun mcpOAuthStatus(conversationId: String, jobId: String): Result<McpOAuthStatusResponseDto> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.getMcpOAuthStatus(endpoint(t.baseUrl, "/api/mcp/oauth/status/$jobId")) }
        }

    // ---- plugins / skills / sub-agents -------------------------------------

    suspend fun listPlugins(conversationId: String): Result<List<InstalledPluginDto>> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.listPlugins(endpoint(t.baseUrl, "/api/plugins"), PluginsRequestDto()) }
                .map { it.plugins.orEmpty() }
        }

    suspend fun fetchPlugin(conversationId: String, source: String): Result<FetchPluginResponseDto> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.fetchPlugin(endpoint(t.baseUrl, "/api/plugins/fetch"), FetchPluginRequestDto(source = source)) }
        }

    suspend fun listSkills(conversationId: String): Result<List<InstalledSkillDto>> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.listSkills(endpoint(t.baseUrl, "/api/skills"), SkillsRequestDto()) }
                .map { it.skills.orEmpty() }
        }

    suspend fun skillsMarketplace(conversationId: String): Result<String> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.getSkillsMarketplace(endpoint(t.baseUrl, "/api/skills/marketplace")) }
                .map { it.string() }
        }

    suspend fun listSubAgents(conversationId: String): Result<List<SubAgentDto>> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.listSubAgents(endpoint(t.baseUrl, "/api/sub-agents"), SubAgentsRequestDto()) }
                .map { it.agents.orEmpty() }
        }

    suspend fun loadConversationPlugin(conversationId: String, source: String): Result<SuccessResponse> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call {
                t.service.loadConversationPlugin(
                    endpoint(t.baseUrl, "/api/conversations/$conversationId/load_plugin"),
                    LoadPluginRequest(source = source)
                )
            }
        }

    suspend fun setMcpServer(conversationId: String, key: String, server: McpServerInputDto): Result<RuntimeSettingsDto> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.setMcpServer(endpoint(t.baseUrl, "/api/settings/mcp/$key"), server) }
        }

    // ---- sub-conversations / workspace -------------------------------------

    suspend fun startChildConversation(conversationId: String, task: String, title: String? = null): Result<ConversationInfoDto> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call {
                t.service.startChildConversation(
                    endpoint(t.baseUrl, "/api/conversations/$conversationId/children"),
                    StartChildConversationRequest(task = task, title = title)
                )
            }
        }

    suspend fun children(conversationId: String): Result<List<ConversationInfoDto>> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.getChildren(endpoint(t.baseUrl, "/api/conversations/$conversationId/children")) }
                .map { it.filterNotNull() }
        }

    suspend fun stopBashCommand(conversationId: String, commandId: String): Result<SuccessResponse> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.stopBashCommand(endpoint(t.baseUrl, "/api/bash/bash_commands/$commandId/stop")) }
        }

    suspend fun workspace(conversationId: String): Result<String> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.getWorkspace(endpoint(t.baseUrl, "/api/conversations/$conversationId/workspace")) }
                .map { it.string() }
        }

    suspend fun navigateConversation(conversationId: String, eventId: String?): Result<ConversationInfoDto> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call {
                t.service.navigateConversation(
                    endpoint(t.baseUrl, "/api/conversations/$conversationId/navigate"),
                    NavigateConversationRequest(eventId)
                )
            }
        }

    /** Streams a sandbox file (or archive) to [sink]. Returns the byte count. */
    suspend fun downloadFile(conversationId: String, path: String, sink: java.io.OutputStream): Result<Long> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            try {
                val response = t.service.downloadFile(endpoint(t.baseUrl, "/api/file/download"), path)
                if (response.isSuccessful) {
                    val body = response.body() ?: return@withContext Result.failure(Exception("Empty download"))
                    body.byteStream().use { input -> input.copyTo(sink) }
                    Result.success(body.contentLength())
                } else {
                    Result.failure(Exception("Download failed (HTTP ${response.code()})"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ---- vscode ------------------------------------------------------------

    suspend fun vscodeUrl(conversationId: String): Result<VSCodeUrlDto> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.getVSCodeUrl(endpoint(t.baseUrl, "/api/vscode/url")) }
        }

    // ---- skills / plugins / mcp / settings ---------------------------------

    suspend fun installedSkills(conversationId: String): Result<List<InstalledSkillDto>> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.listInstalledSkills(endpoint(t.baseUrl, "/api/skills/installed")) }
                .map { it.skills.orEmpty() }
        }

    suspend fun skillCatalog(conversationId: String): Result<List<InstalledSkillDto>> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.getSkillCatalog(endpoint(t.baseUrl, "/api/skills/catalog")) }
                .map { it.skills.orEmpty() }
        }

    suspend fun installSkill(conversationId: String, name: String, source: String? = null): Result<Unit> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.installSkill(endpoint(t.baseUrl, "/api/skills/install"), InstallSkillRequest(name, source)) }
                .map { Unit }
        }

    suspend fun syncSkills(conversationId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.syncSkills(endpoint(t.baseUrl, "/api/skills/sync")) }.map { Unit }
        }

    suspend fun installedPlugins(conversationId: String): Result<List<InstalledPluginDto>> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.listInstalledPlugins(endpoint(t.baseUrl, "/api/plugins/installed")) }
                .map { it.plugins.orEmpty() }
        }

    suspend fun installPlugin(conversationId: String, name: String, source: String? = null): Result<Unit> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.installPlugin(endpoint(t.baseUrl, "/api/plugins/install"), InstallPluginRequest(name, source)) }
                .map { Unit }
        }

    suspend fun testMcp(conversationId: String, server: Map<String, Any>, name: String? = null): Result<Unit> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.testMcp(endpoint(t.baseUrl, "/api/mcp/test"), McpTestRequest(name = name, server = server)) }
                .map { Unit }
        }

    suspend fun runtimeSettings(conversationId: String): Result<RuntimeSettingsDto> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.getRuntimeSettings(endpoint(t.baseUrl, "/api/settings")) }
        }

    suspend fun updateRuntimeSettings(
        conversationId: String,
        agentSettings: Map<String, Any>?,
        conversationSettings: Map<String, Any>?,
    ): Result<RuntimeSettingsDto> = withContext(Dispatchers.IO) {
        val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
        call {
            t.service.updateRuntimeSettings(
                endpoint(t.baseUrl, "/api/settings"),
                UpdateRuntimeSettingsRequest(agentSettings, conversationSettings)
            )
        }
    }

    suspend fun runtimeProviders(conversationId: String): Result<List<LlmProviderDto>> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.getRuntimeProviders(endpoint(t.baseUrl, "/api/llm/providers")) }
                .map { it.providers.orEmpty() }
        }

    suspend fun runtimeModels(conversationId: String): Result<List<RuntimeLlmModelDto>> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.getRuntimeModels(endpoint(t.baseUrl, "/api/llm/models")) }
                .map { it.models.orEmpty() }
        }

    suspend fun toolCatalog(conversationId: String): Result<List<ToolCatalogEntryDto>> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.getToolCatalog(endpoint(t.baseUrl, "/api/tools/catalog")) }
                .map { it.tools.orEmpty() }
        }

    // ---- files (extended) --------------------------------------------------

    suspend fun homeDir(conversationId: String): Result<String> = withContext(Dispatchers.IO) {
        val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
        call { t.service.getHomeDir(endpoint(t.baseUrl, "/api/file/home")) }.map { it.home ?: "" }
    }

    suspend fun archivePath(conversationId: String, path: String): Result<FileArchiveDto> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.getFileArchive(endpoint(t.baseUrl, "/api/file/archive"), path = path) }
        }

    suspend fun createDirectory(conversationId: String, path: String): Result<SuccessResponse> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.createDirectory(endpoint(t.baseUrl, "/api/file/create_directory"), path) }
        }

    // ---- health / init / hooks --------------------------------------------

    suspend fun health(conversationId: String): Result<HealthStatusDto> = withContext(Dispatchers.IO) {
        val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
        call { t.service.getHealth(endpoint(t.baseUrl, "/health")) }
    }

    suspend fun ready(conversationId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
        call { t.service.getReady(endpoint(t.baseUrl, "/ready")) }.map { Unit }
    }

    suspend fun rootServerInfo(conversationId: String): Result<ServerInfoDto> = withContext(Dispatchers.IO) {
        val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
        call { t.service.getServerInfo(endpoint(t.baseUrl, "/")) }
    }

    suspend fun alive(conversationId: String): Result<HealthStatusDto> = withContext(Dispatchers.IO) {
        val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
        call { t.service.getHealth(endpoint(t.baseUrl, "/alive")) }
    }

    suspend fun initStatus(conversationId: String): Result<InitStatusDto> = withContext(Dispatchers.IO) {
        val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
        call { t.service.getInitStatus(endpoint(t.baseUrl, "/api/init")) }
    }

    suspend fun init(conversationId: String, request: InitRequestDto): Result<InitStatusDto> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.init(endpoint(t.baseUrl, "/api/init"), request) }
        }

    suspend fun hooks(conversationId: String): Result<HooksResponseDto> = withContext(Dispatchers.IO) {
        val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
        call { t.service.getHooks(endpoint(t.baseUrl, "/api/hooks"), HooksRequestDto()) }
    }

    // ---- conversations (bare) ---------------------------------------------

    suspend fun agentConversations(conversationId: String): Result<List<ConversationInfoDto>> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.listConversations(endpoint(t.baseUrl, "/api/conversations")) }
        }

    suspend fun agentConversationCount(conversationId: String): Result<Int> = withContext(Dispatchers.IO) {
        val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
        call { t.service.countConversations(endpoint(t.baseUrl, "/api/conversations/count")) }
    }

    suspend fun searchAgentConversations(conversationId: String): Result<List<ConversationInfoDto>> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.searchConversations(endpoint(t.baseUrl, "/api/conversations/search")) }
                .map { it.items.orEmpty() }
        }

    suspend fun startAgentConversation(conversationId: String, request: StartConversationRequest): Result<ConversationInfoDto> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.startConversation(endpoint(t.baseUrl, "/api/conversations"), request) }
        }

    suspend fun sendConversationMessage(conversationId: String, request: SendMessageRequestDto): Result<SuccessResponse> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call {
                t.service.sendMessage(
                    endpoint(t.baseUrl, "/api/conversations/$conversationId/events"),
                    request
                )
            }
        }

    // ---- events ------------------------------------------------------------

    suspend fun conversationEvents(conversationId: String): Result<List<Map<String, Any>>> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.getConversationEvents(endpoint(t.baseUrl, "/api/conversations/$conversationId/events")) }
        }

    suspend fun conversationEventCount(conversationId: String): Result<Int> = withContext(Dispatchers.IO) {
        val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
        call { t.service.countConversationEvents(endpoint(t.baseUrl, "/api/conversations/$conversationId/events/count")) }
    }

    suspend fun searchConversationEvents(conversationId: String): Result<String> = withContext(Dispatchers.IO) {
        val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
        call {
            t.service.searchConversationEvents(
                endpoint(t.baseUrl, "/api/conversations/$conversationId/events/search")
            )
        }.map { it.string() }
    }

    suspend fun conversationEvent(conversationId: String, eventId: String): Result<Map<String, Any>> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.getConversationEvent(endpoint(t.baseUrl, "/api/conversations/$conversationId/events/$eventId")) }
        }

    // ---- bash (conversation-prefixed aliases) ------------------------------

    suspend fun executeBashInConversation(conversationId: String, command: String): Result<BashOutputDto> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call {
                t.service.executeBashConversation(
                    endpoint(t.baseUrl, "/api/conversations/$conversationId/bash/execute_bash_command"),
                    ExecuteBashRequest(command = command)
                )
            }
        }

    suspend fun startBashInConversation(conversationId: String, command: String): Result<BashCommandDto> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call {
                t.service.startBashConversation(
                    endpoint(t.baseUrl, "/api/conversations/$conversationId/bash/start_bash_command"),
                    ExecuteBashRequest(command = command)
                )
            }
        }

    suspend fun clearConversationBashEvents(conversationId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
        call { t.service.clearConversationBashEvents(endpoint(t.baseUrl, "/api/conversations/$conversationId/bash/bash_events")) }
            .map { Unit }
    }

    suspend fun listConversationBashEvents(conversationId: String): Result<List<Map<String, Any>>> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call {
                t.service.listConversationBashEvents(
                    endpoint(t.baseUrl, "/api/conversations/$conversationId/bash/bash_events/")
                )
            }
        }

    suspend fun searchConversationBashEvents(conversationId: String): Result<BashEventPageDto> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call {
                t.service.searchConversationBashEvents(
                    endpoint(t.baseUrl, "/api/conversations/$conversationId/bash/bash_events/search")
                )
            }
        }

    suspend fun conversationBashEvent(conversationId: String, eventId: String): Result<BashEventDto> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call {
                t.service.getConversationBashEvent(
                    endpoint(t.baseUrl, "/api/conversations/$conversationId/bash/bash_events/$eventId")
                )
            }
        }

    suspend fun stopConversationBashCommand(conversationId: String, commandId: String): Result<SuccessResponse> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call {
                t.service.stopBashCommand(
                    endpoint(t.baseUrl, "/api/conversations/$conversationId/bash/bash_commands/$commandId/stop")
                )
            }
        }

    // ---- git (bare + commits) ---------------------------------------------

    suspend fun bareGitChanges(conversationId: String, path: String? = null): Result<List<GitChangeDto>> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.getBareGitChanges(endpoint(t.baseUrl, "/api/git/changes"), path = path) }
        }

    /**
     * Resets the sandbox idle timer with the cheapest real runtime call.
     *
     * Verified against a live sandbox: `GET /api/git/changes?path=/` returns 200
     * and resets `idle_time` to 0, whereas an invalid request (422) or a
     * DB-backed cloud call does not. `path=/` is accepted regardless of the
     * conversation's working directory.
     */
    suspend fun touchRuntime(conversationId: String): Result<Unit> =
        bareGitChanges(conversationId, path = "/").map { Unit }

    suspend fun bareGitCommits(conversationId: String): Result<GitCommitsPageDto> = withContext(Dispatchers.IO) {
        val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
        call { t.service.getBareGitCommits(endpoint(t.baseUrl, "/api/git/commits")) }
    }

    suspend fun bareGitDiff(conversationId: String): Result<GitDiffDto> = withContext(Dispatchers.IO) {
        val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
        call { t.service.getBareGitDiff(endpoint(t.baseUrl, "/api/git/diff")) }
    }

    suspend fun gitCommitChanges(conversationId: String, sha: String): Result<List<GitChangeDto>> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.getGitCommitChanges(endpoint(t.baseUrl, "/api/git/commits/$sha/changes")) }
        }

    suspend fun conversationGitCommitChanges(conversationId: String, sha: String): Result<List<GitChangeDto>> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call {
                t.service.getGitCommitChanges(
                    endpoint(t.baseUrl, "/api/conversations/$conversationId/git/commits/$sha/changes")
                )
            }
        }

    suspend fun searchGitRepositories(conversationId: String): Result<GitRepositoryPageDto> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.searchGitRepositories(endpoint(t.baseUrl, "/api/git/repositories/search")) }
        }

    suspend fun pullRequestStatus(conversationId: String, owner: String, repo: String, number: Int): Result<GitPullRequestStatusDto> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call {
                t.service.getPullRequest(
                    endpoint(t.baseUrl, "/api/git/repositories/$owner/$repo/pull-requests/$number")
                )
            }
        }

    // ---- vscode (bare) -----------------------------------------------------

    suspend fun bareVSCodeUrl(conversationId: String): Result<VSCodeUrlDto> = withContext(Dispatchers.IO) {
        val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
        call { t.service.getBareVSCodeUrl(endpoint(t.baseUrl, "/api/vscode/url")) }
    }

    suspend fun bareVSCodeStatus(conversationId: String): Result<Map<String, Any>> = withContext(Dispatchers.IO) {
        val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
        call { t.service.getBareVSCodeStatus(endpoint(t.baseUrl, "/api/vscode/status")) }
    }

    suspend fun conversationVSCodeUrl(conversationId: String): Result<VSCodeUrlDto> = withContext(Dispatchers.IO) {
        val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
        call { t.service.getBareVSCodeUrl(endpoint(t.baseUrl, "/api/conversations/$conversationId/vscode/url")) }
    }

    suspend fun conversationVSCodeStatus(conversationId: String): Result<Map<String, Any>> = withContext(Dispatchers.IO) {
        val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
        call { t.service.getBareVSCodeStatus(endpoint(t.baseUrl, "/api/conversations/$conversationId/vscode/status")) }
    }

    suspend fun conversationWorkspaceFile(conversationId: String, filePath: String): Result<String> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call {
                t.service.getWorkspace(endpoint(t.baseUrl, "/api/conversations/$conversationId/workspace/$filePath"))
            }.map { it.string() }
        }

    // ---- workspaces --------------------------------------------------------

    suspend fun workspaces(conversationId: String): Result<WorkspacesListDto> = withContext(Dispatchers.IO) {
        val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
        call { t.service.listWorkspaces(endpoint(t.baseUrl, "/api/workspaces")) }
    }

    suspend fun addWorkspaces(conversationId: String, paths: List<String>): Result<WorkspacesListDto> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.addWorkspaces(endpoint(t.baseUrl, "/api/workspaces"), AddWorkspacesRequest(paths)) }
        }

    suspend fun deleteWorkspaces(conversationId: String): Result<DeleteResponseDto> = withContext(Dispatchers.IO) {
        val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
        call { t.service.deleteWorkspaces(endpoint(t.baseUrl, "/api/workspaces")) }
    }

    suspend fun addWorkspaceParents(conversationId: String, parents: List<String>): Result<WorkspacesListDto> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call {
                t.service.addWorkspaceParents(
                    endpoint(t.baseUrl, "/api/workspaces/parents"),
                    AddWorkspaceParentsRequest(parents)
                )
            }
        }

    suspend fun deleteWorkspaceParents(conversationId: String): Result<DeleteResponseDto> = withContext(Dispatchers.IO) {
        val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
        call { t.service.deleteWorkspaceParents(endpoint(t.baseUrl, "/api/workspaces/parents")) }
    }

    // ---- profiles (LLM) ----------------------------------------------------

    suspend fun runtimeProfiles(conversationId: String): Result<RuntimeProfileListDto> = withContext(Dispatchers.IO) {
        val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
        call { t.service.listRuntimeProfiles(endpoint(t.baseUrl, "/api/profiles")) }
    }

    suspend fun runtimeProfile(conversationId: String, name: String): Result<RuntimeProfileDetailDto> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.getRuntimeProfile(endpoint(t.baseUrl, "/api/profiles/$name")) }
        }

    suspend fun saveRuntimeProfile(conversationId: String, name: String, llm: Map<String, Any>): Result<Unit> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call {
                t.service.saveRuntimeProfile(
                    endpoint(t.baseUrl, "/api/profiles/$name"),
                    SaveRuntimeProfileRequest(llm)
                )
            }.map { Unit }
        }

    suspend fun deleteRuntimeProfile(conversationId: String, name: String): Result<ProfileMutationResponseDto> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.deleteRuntimeProfile(endpoint(t.baseUrl, "/api/profiles/$name")) }
        }

    suspend fun activateRuntimeProfile(conversationId: String, name: String): Result<ActivateProfileResponseDto> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.activateRuntimeProfile(endpoint(t.baseUrl, "/api/profiles/$name/activate")) }
        }

    suspend fun renameRuntimeProfile(conversationId: String, name: String, newName: String): Result<ProfileMutationResponseDto> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call {
                t.service.renameRuntimeProfile(
                    endpoint(t.baseUrl, "/api/profiles/$name/rename"),
                    RenameProfileRequest(newName)
                )
            }
        }

    suspend fun validateRuntimeProfile(conversationId: String, name: String, llm: Map<String, Any>): Result<ValidateProfileResponseDto> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call {
                t.service.validateRuntimeProfile(
                    endpoint(t.baseUrl, "/api/profiles/$name/validate"),
                    SaveRuntimeProfileRequest(llm)
                )
            }
        }

    // ---- agent profiles ----------------------------------------------------

    suspend fun agentProfilesDetailed(conversationId: String): Result<String> = withContext(Dispatchers.IO) {
        val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
        call { t.service.listAgentProfilesDetailed(endpoint(t.baseUrl, "/api/agent-profiles")) }.map { it.string() }
    }

    suspend fun agentProfile(conversationId: String, name: String): Result<AgentProfileDetailDto> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.getAgentProfile(endpoint(t.baseUrl, "/api/agent-profiles/$name")) }
        }

    suspend fun saveAgentProfile(conversationId: String, name: String, profile: Map<String, Any>): Result<AgentProfileMutationDto> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.saveAgentProfile(endpoint(t.baseUrl, "/api/agent-profiles/$name"), profile) }
        }

    suspend fun deleteAgentProfile(conversationId: String, name: String): Result<AgentProfileMutationDto> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.deleteAgentProfile(endpoint(t.baseUrl, "/api/agent-profiles/$name")) }
        }

    suspend fun materializeAgentProfile(conversationId: String, name: String): Result<AgentProfileDiagnosticsDto> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.materializeAgentProfile(endpoint(t.baseUrl, "/api/agent-profiles/$name/materialize")) }
        }

    suspend fun renameAgentProfile(conversationId: String, name: String, newName: String): Result<AgentProfileMutationDto> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call {
                t.service.renameAgentProfile(
                    endpoint(t.baseUrl, "/api/agent-profiles/$name/rename"),
                    RenameAgentProfileRequest(newName)
                )
            }
        }

    suspend fun activateAgentProfileDetailed(conversationId: String, profileId: String): Result<ActivateAgentProfileDto> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.activateAgentProfileDetailed(endpoint(t.baseUrl, "/api/agent-profiles/$profileId/activate")) }
        }

    // ---- meta profiles -----------------------------------------------------

    suspend fun metaProfiles(conversationId: String): Result<MetaProfileListDto> = withContext(Dispatchers.IO) {
        val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
        call { t.service.listMetaProfiles(endpoint(t.baseUrl, "/api/meta-profiles")) }
    }

    suspend fun metaProfile(conversationId: String, name: String): Result<MetaProfileDetailDto> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.getMetaProfile(endpoint(t.baseUrl, "/api/meta-profiles/$name")) }
        }

    suspend fun saveMetaProfile(conversationId: String, name: String, config: MetaProfileConfigDto): Result<Unit> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.saveMetaProfile(endpoint(t.baseUrl, "/api/meta-profiles/$name"), config) }.map { Unit }
        }

    suspend fun deleteMetaProfile(conversationId: String, name: String): Result<ProfileMutationResponseDto> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.deleteMetaProfile(endpoint(t.baseUrl, "/api/meta-profiles/$name")) }
        }

    suspend fun activateMetaProfile(conversationId: String, name: String): Result<ActivateMetaProfileDto> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.activateMetaProfile(endpoint(t.baseUrl, "/api/meta-profiles/$name/activate")) }
        }

    // ---- plugins / skills (installed-item operations) ----------------------

    suspend fun installedPlugin(conversationId: String, name: String): Result<InstalledPluginDetailDto> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.getInstalledPlugin(endpoint(t.baseUrl, "/api/plugins/installed/$name")) }
        }

    suspend fun setPluginEnabled(conversationId: String, name: String, enabled: Boolean): Result<UpdatePluginStateDto> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call {
                t.service.updatePluginState(
                    endpoint(t.baseUrl, "/api/plugins/installed/$name"),
                    UpdatePluginStateRequest(enabled)
                )
            }
        }

    suspend fun uninstallPlugin(conversationId: String, name: String): Result<Unit> = withContext(Dispatchers.IO) {
        val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
        call { t.service.uninstallPlugin(endpoint(t.baseUrl, "/api/plugins/installed/$name")) }.map { Unit }
    }

    suspend fun refreshPlugin(conversationId: String, name: String): Result<UpdatePluginDto> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.refreshPlugin(endpoint(t.baseUrl, "/api/plugins/installed/$name/refresh")) }
        }

    suspend fun pluginMarketplace(conversationId: String): Result<String> = withContext(Dispatchers.IO) {
        val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
        call { t.service.getPluginMarketplace(endpoint(t.baseUrl, "/api/plugins/marketplace")) }.map { it.string() }
    }

    suspend fun installedSkill(conversationId: String, name: String): Result<InstalledSkillDetailDto> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.getInstalledSkill(endpoint(t.baseUrl, "/api/skills/installed/$name")) }
        }

    suspend fun setSkillEnabled(conversationId: String, name: String, enabled: Boolean): Result<UpdateSkillStateDto> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call {
                t.service.updateSkillState(
                    endpoint(t.baseUrl, "/api/skills/installed/$name"),
                    UpdateSkillStateRequest(enabled)
                )
            }
        }

    suspend fun uninstallSkill(conversationId: String, name: String): Result<Unit> = withContext(Dispatchers.IO) {
        val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
        call { t.service.uninstallSkill(endpoint(t.baseUrl, "/api/skills/installed/$name")) }.map { Unit }
    }

    suspend fun refreshSkill(conversationId: String, name: String): Result<UpdateSkillDto> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.refreshSkill(endpoint(t.baseUrl, "/api/skills/installed/$name/refresh")) }
        }

    // ---- settings schemas + secrets ----------------------------------------

    suspend fun agentSettingsSchema(conversationId: String): Result<SettingsSchemaDto> = withContext(Dispatchers.IO) {
        val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
        call { t.service.getAgentSettingsSchema(endpoint(t.baseUrl, "/api/settings/agent-schema")) }
    }

    suspend fun conversationSettingsSchema(conversationId: String): Result<SettingsSchemaDto> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.getConversationSettingsSchema(endpoint(t.baseUrl, "/api/settings/conversation-schema")) }
        }

    suspend fun runtimeSecrets(conversationId: String): Result<RuntimeSecretsListDto> = withContext(Dispatchers.IO) {
        val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
        call { t.service.listRuntimeSecrets(endpoint(t.baseUrl, "/api/settings/secrets")) }
    }

    suspend fun createRuntimeSecret(conversationId: String, name: String, value: String, description: String? = null): Result<RuntimeSecretItemDto> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call {
                t.service.createRuntimeSecret(
                    endpoint(t.baseUrl, "/api/settings/secrets"),
                    SecretCreateRequest(name, value, description)
                )
            }
        }

    suspend fun runtimeSecret(conversationId: String, name: String): Result<String> = withContext(Dispatchers.IO) {
        val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
        call { t.service.getRuntimeSecret(endpoint(t.baseUrl, "/api/settings/secrets/$name")) }.map { it.string() }
    }

    suspend fun deleteRuntimeSecret(conversationId: String, name: String): Result<Unit> = withContext(Dispatchers.IO) {
        val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
        call { t.service.deleteRuntimeSecret(endpoint(t.baseUrl, "/api/settings/secrets/$name")) }.map { Unit }
    }

    // ---- tools (bare) ------------------------------------------------------

    suspend fun bareToolCatalog(conversationId: String): Result<List<String>> = withContext(Dispatchers.IO) {
        val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
        call { t.service.getBareToolCatalog(endpoint(t.baseUrl, "/api/tools/")) }
    }

    // ---- llm provider connections ------------------------------------------

    suspend fun providerConnections(conversationId: String): Result<List<ProviderConnectionDto>> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.listProviderConnections(endpoint(t.baseUrl, "/api/llm/provider-connections")) }
        }

    suspend fun createProviderConnection(conversationId: String, request: ProviderConnectionCreateRequest): Result<ProviderConnectionDto> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.createProviderConnection(endpoint(t.baseUrl, "/api/llm/provider-connections"), request) }
        }

    suspend fun updateProviderConnection(conversationId: String, id: String, request: ProviderConnectionCreateRequest): Result<ProviderConnectionDto> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call {
                t.service.updateProviderConnection(
                    endpoint(t.baseUrl, "/api/llm/provider-connections/$id"),
                    request
                )
            }
        }

    suspend fun deleteProviderConnection(conversationId: String, id: String): Result<ProviderConnectionDto> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.deleteProviderConnection(endpoint(t.baseUrl, "/api/llm/provider-connections/$id")) }
        }

    suspend fun verifiedModels(conversationId: String): Result<VerifiedModelsDto> = withContext(Dispatchers.IO) {
        val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
        call { t.service.getVerifiedModels(endpoint(t.baseUrl, "/api/llm/models/verified")) }
    }

    // ---- OpenAI subscription -----------------------------------------------

    suspend fun subscriptionStatus(conversationId: String): Result<SubscriptionStatusDto> = withContext(Dispatchers.IO) {
        val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
        call { t.service.getSubscriptionStatus(endpoint(t.baseUrl, "/api/llm/subscription/openai/status")) }
    }

    suspend fun startSubscriptionDevice(conversationId: String): Result<SubscriptionDeviceStartDto> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.startSubscriptionDevice(endpoint(t.baseUrl, "/api/llm/subscription/openai/device/start")) }
        }

    suspend fun pollSubscriptionDevice(conversationId: String, deviceCode: String): Result<SubscriptionStatusDto> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call {
                t.service.pollSubscriptionDevice(
                    endpoint(t.baseUrl, "/api/llm/subscription/openai/device/poll"),
                    SubscriptionDevicePollRequest(deviceCode)
                )
            }
        }

    suspend fun logoutSubscription(conversationId: String): Result<SubscriptionStatusDto> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.logoutSubscription(endpoint(t.baseUrl, "/api/llm/subscription/openai/logout")) }
        }

    suspend fun subscriptionModels(conversationId: String): Result<SubscriptionModelsDto> = withContext(Dispatchers.IO) {
        val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
        call { t.service.getSubscriptionModels(endpoint(t.baseUrl, "/api/llm/subscription/openai/models")) }
    }

    // ---- mcp oauth callback ------------------------------------------------

    suspend fun mcpOAuthCallback(conversationId: String, callbackUrl: String): Result<McpOAuthStatusResponseDto> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call {
                t.service.mcpOAuthCallback(
                    endpoint(t.baseUrl, "/api/mcp/oauth/callback/$callbackUrl"),
                    McpOAuthCallbackRequest(callbackUrl)
                )
            }
        }

    // ---- canvas extension app backends -------------------------------------

    suspend fun createAppBackendSession(conversationId: String, extensionName: String): Result<AppBackendSessionDto> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.createAppBackendSession(endpoint(t.baseUrl, "/app-backends/$extensionName/session")) }
        }

    suspend fun deleteAppBackendSession(conversationId: String, extensionName: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.deleteAppBackendSession(endpoint(t.baseUrl, "/app-backends/$extensionName/session")) }
                .map { Unit }
        }

    // ---- auth workspace session --------------------------------------------

    suspend fun createWorkspaceSession(conversationId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
        call { t.service.createWorkspaceSession(endpoint(t.baseUrl, "/api/auth/workspace-session")) }.map { Unit }
    }

    suspend fun deleteWorkspaceSession(conversationId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
        call { t.service.deleteWorkspaceSession(endpoint(t.baseUrl, "/api/auth/workspace-session")) }.map { Unit }
    }

    // ---- OpenAI-compatible surface -----------------------------------------

    suspend fun openAiModels(conversationId: String): Result<OpenAiModelListDto> = withContext(Dispatchers.IO) {
        val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
        call { t.service.listOpenAiModels(endpoint(t.baseUrl, "/v1/models")) }
    }

    suspend fun openAiChatCompletions(conversationId: String, body: Map<String, Any>): Result<String> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.openAiChatCompletions(endpoint(t.baseUrl, "/v1/chat/completions"), body) }
                .map { it.string() }
        }

    suspend fun openAiResponses(conversationId: String, body: Map<String, Any>): Result<String> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.openAiResponses(endpoint(t.baseUrl, "/v1/responses"), body) }.map { it.string() }
        }

    // ---- canvas extensions ------------------------------------------------

    suspend fun installCanvasExtension(
        conversationId: String,
        source: String,
        ref: String? = null,
        repoPath: String? = null,
        force: Boolean = false,
    ): Result<InstalledCanvasExtensionDto> = withContext(Dispatchers.IO) {
        val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
        call {
            t.service.installCanvasExtension(
                endpoint(t.baseUrl, "/api/canvas-extensions/install"),
                InstallCanvasExtensionRequest(source = source, ref = ref, repoPath = repoPath, force = force),
            )
        }
    }

    suspend fun canvasExtensions(conversationId: String): Result<List<InstalledCanvasExtensionDto>> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call { t.service.listCanvasExtensions(endpoint(t.baseUrl, "/api/canvas-extensions/installed")) }
                .map { it.canvasExtensions.orEmpty() }
        }

    suspend fun canvasExtension(conversationId: String, name: String): Result<InstalledCanvasExtensionDto> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call {
                t.service.getCanvasExtension(endpoint(t.baseUrl, "/api/canvas-extensions/installed/$name"))
            }
        }

    suspend fun updateCanvasExtensionState(
        conversationId: String,
        name: String,
        enabled: Boolean,
        revision: String? = null,
        secretRefs: List<String>? = null,
    ): Result<UpdateCanvasExtensionStateDto> = withContext(Dispatchers.IO) {
        val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
        call {
            t.service.updateCanvasExtensionState(
                endpoint(t.baseUrl, "/api/canvas-extensions/installed/$name"),
                UpdateCanvasExtensionStateRequest(enabled = enabled, revision = revision, secretRefs = secretRefs),
            )
        }
    }

    suspend fun uninstallCanvasExtension(conversationId: String, name: String): Result<UninstallCanvasExtensionDto> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call {
                t.service.uninstallCanvasExtension(endpoint(t.baseUrl, "/api/canvas-extensions/installed/$name"))
            }
        }

    suspend fun canvasBackendStatus(conversationId: String, name: String): Result<CanvasBackendStatusDto> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call {
                t.service.getCanvasBackendStatus(
                    endpoint(t.baseUrl, "/api/canvas-extensions/installed/$name/backend")
                )
            }
        }

    suspend fun deleteCanvasBackendData(conversationId: String, name: String): Result<CanvasBackendStatusDto> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call {
                t.service.deleteCanvasBackendData(
                    endpoint(t.baseUrl, "/api/canvas-extensions/installed/$name/backend/data")
                )
            }
        }

    suspend fun canvasBackendLogs(
        conversationId: String,
        name: String,
        limitBytes: Int? = null,
    ): Result<CanvasBackendLogsDto> = withContext(Dispatchers.IO) {
        val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
        call {
            t.service.getCanvasBackendLogs(
                endpoint(t.baseUrl, "/api/canvas-extensions/installed/$name/backend/logs"),
                limitBytes = limitBytes,
            )
        }
    }

    suspend fun prepareCanvasBackend(
        conversationId: String,
        name: String,
        revision: String,
    ): Result<CanvasBackendStatusDto> = withContext(Dispatchers.IO) {
        val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
        call {
            t.service.prepareCanvasBackend(
                endpoint(t.baseUrl, "/api/canvas-extensions/installed/$name/backend/prepare"),
                BackendRevisionRequest(revision = revision),
            )
        }
    }

    suspend fun grantCanvasBackendSecret(
        conversationId: String,
        name: String,
        revision: String,
        secretRefs: List<String>,
    ): Result<CanvasBackendStatusDto> = withContext(Dispatchers.IO) {
        val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
        call {
            t.service.grantCanvasBackendSecret(
                endpoint(t.baseUrl, "/api/canvas-extensions/installed/$name/backend/secret-grant"),
                BackendSecretGrantRequest(revision = revision, secretRefs = secretRefs),
            )
        }
    }

    suspend fun revokeCanvasBackendSecret(conversationId: String, name: String): Result<CanvasBackendStatusDto> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call {
                t.service.revokeCanvasBackendSecret(
                    endpoint(t.baseUrl, "/api/canvas-extensions/installed/$name/backend/secret-grant")
                )
            }
        }

    suspend fun startCanvasBackend(
        conversationId: String,
        name: String,
        revision: String,
    ): Result<CanvasBackendStatusDto> = withContext(Dispatchers.IO) {
        val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
        call {
            t.service.startCanvasBackend(
                endpoint(t.baseUrl, "/api/canvas-extensions/installed/$name/backend/start"),
                BackendRevisionRequest(revision = revision),
            )
        }
    }

    suspend fun stopCanvasBackend(conversationId: String, name: String): Result<CanvasBackendStatusDto> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call {
                t.service.stopCanvasBackend(
                    endpoint(t.baseUrl, "/api/canvas-extensions/installed/$name/backend/stop")
                )
            }
        }

    suspend fun canvasExtensionBundle(conversationId: String, name: String): Result<String> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call {
                t.service.getCanvasExtensionBundle(
                    endpoint(t.baseUrl, "/api/canvas-extensions/installed/$name/bundle")
                )
            }.map { it.string() }
        }

    suspend fun canvasExtensionIcon(conversationId: String, name: String): Result<ByteArray> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call {
                t.service.getCanvasExtensionIcon(
                    endpoint(t.baseUrl, "/api/canvas-extensions/installed/$name/icon")
                )
            }.map { it.bytes() }
        }

    // ---- conversation-scoped files ----------------------------------------

    suspend fun downloadConversationFile(
        conversationId: String,
        path: String,
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
        call {
            t.service.downloadConversationFile(
                endpoint(t.baseUrl, "/api/conversations/$conversationId/file/download"), path
            )
        }.map { it.bytes() }
    }

    suspend fun uploadConversationFile(
        conversationId: String,
        path: String,
        bytes: ByteArray,
        fileName: String,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
        val body = bytes.toRequestBody("application/octet-stream".toMediaType())
        val part = MultipartBody.Part.createFormData("file", fileName, body)
        call {
            t.service.uploadConversationFile(
                endpoint(t.baseUrl, "/api/conversations/$conversationId/file/upload"), path, part
            )
        }.map { _ -> Unit }
    }

    suspend fun createConversationDirectory(
        conversationId: String,
        path: String,
    ): Result<SuccessResponse> = withContext(Dispatchers.IO) {
        val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
        call {
            t.service.createConversationDirectory(
                endpoint(t.baseUrl, "/api/conversations/$conversationId/file/create_directory"), path
            )
        }
    }

    suspend fun archiveConversationFiles(
        conversationId: String,
        path: String,
        format: String? = null,
        baseRef: String? = null,
        useDefaultExcludes: Boolean? = null,
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
        call {
            t.service.archiveConversationFiles(
                endpoint(t.baseUrl, "/api/conversations/$conversationId/file/archive"),
                path = path, format = format, baseRef = baseRef, useDefaultExcludes = useDefaultExcludes,
            )
        }.map { it.bytes() }
    }

    suspend fun downloadConversationTrajectory(conversationId: String): Result<ByteArray> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call {
                t.service.downloadConversationTrajectory(
                    endpoint(
                        t.baseUrl,
                        "/api/conversations/$conversationId/file/download-trajectory/$conversationId"
                    )
                )
            }.map { it.bytes() }
        }

    suspend fun downloadTrajectory(conversationId: String): Result<ByteArray> =
        withContext(Dispatchers.IO) {
            val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
            call {
                t.service.downloadTrajectory(
                    endpoint(t.baseUrl, "/api/file/download-trajectory/$conversationId")
                )
            }.map { it.bytes() }
        }

    // ---- conversation-scoped git ------------------------------------------

    suspend fun conversationGitChanges(
        conversationId: String,
        path: String? = null,
        ref: String? = null,
    ): Result<List<GitChange>> = withContext(Dispatchers.IO) {
        val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
        call {
            t.service.getConversationGitChanges(
                endpoint(t.baseUrl, "/api/conversations/$conversationId/git/changes"),
                path = path, ref = ref,
            )
        }.map { list ->
            list.mapNotNull { dto ->
                val p = dto?.path?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                GitChange(status = dto.status ?: "MODIFIED", path = p)
            }
        }
    }

    suspend fun conversationGitCommits(
        conversationId: String,
        path: String? = null,
        limit: Int = 30,
    ): Result<GitCommitsPageDto> = withContext(Dispatchers.IO) {
        val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
        call {
            t.service.getConversationGitCommits(
                endpoint(t.baseUrl, "/api/conversations/$conversationId/git/commits"),
                path = path, limit = limit,
            )
        }
    }

    suspend fun conversationGitDiff(
        conversationId: String,
        path: String? = null,
        ref: String? = null,
        commit: String? = null,
    ): Result<GitDiffDto> = withContext(Dispatchers.IO) {
        val t = target(conversationId).getOrElse { return@withContext Result.failure(it) }
        call {
            t.service.getConversationGitDiff(
                endpoint(t.baseUrl, "/api/conversations/$conversationId/git/diff"),
                path = path, ref = ref, commit = commit,
            )
        }
    }
}
