package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import com.example.data.api.ApiClient
import com.example.data.api.OpenHandsApiService
import com.example.data.local.AgentEventEntity
import com.example.data.local.AppDatabase
import com.example.data.local.ConversationEntity
import com.example.data.local.SavedTemplateEntity
import com.example.data.model.AcpModelInfoDto
import com.example.data.model.AcpAgentProfileDto
import com.example.data.model.AgentProfileInfoDto
import com.example.data.model.AgentProfileListDto
import com.example.data.model.GitOrganizationsDto
import com.example.data.model.GitProviderTokenRequest
import com.example.data.model.InstalledSkillDto
import com.example.data.model.MarketplaceCheckResponseDto
import com.example.data.model.MarketplaceRegistrationDto
import com.example.data.model.MarketplaceSkillsPreviewDto
import com.example.data.model.McpOAuthStartRequest
import com.example.data.model.McpOAuthStartResponseDto
import com.example.data.model.McpOAuthStatusResponseDto
import com.example.data.model.McpTestRequest
import com.example.data.model.McpToolCallRequest
import com.example.data.model.ProfileConfigDto
import com.example.data.model.ProfileDetailDto
import com.example.data.model.ProfileMutationDto
import com.example.data.model.RenameProfileRequest
import com.example.data.model.RenameAgentProfileRequest
import com.example.data.model.SaveAgentProfileRequest
import com.example.data.model.RepositoryDetailDto
import com.example.data.model.SaveProfileRequest
import com.example.data.model.SecretNameItemDto
import com.example.data.model.StartTaskResponse
import com.example.data.model.UpdateUserSettingsRequest
import com.example.data.model.UserSettingsDto
import com.example.data.model.WebClientConfigDto

import com.example.data.model.AgentEvent
import com.example.data.model.ActionDto
import com.example.data.model.BranchDto
import com.example.data.model.ConversationUpdateRequest
import com.example.data.model.CreateSecretRequest
import com.example.data.model.LlmModelDto
import com.example.data.model.PendingMessageRequest
import com.example.data.model.ProfileInfoDto
import com.example.data.model.ProviderDto
import com.example.data.model.RepositoryDto
import com.example.data.model.SandboxInfoDto
import com.example.data.model.SandboxSpecInfoDto
import com.example.data.model.SecretDto
import com.example.data.model.SkillInfoDto
import com.example.data.model.SuggestedTaskDto
import com.example.data.model.UpdateSecretRequest
import com.example.data.model.UserGitInfoDto
import com.example.data.model.ConversationResponse
import com.example.data.model.ConversationStatus
import com.example.data.model.EventResponse
import com.example.data.model.GitChange
import com.example.data.model.OpenHandsConversation
import com.example.data.model.PromptTemplate
import com.example.data.model.SandboxTarget
import com.example.data.model.SendMessagePayload
import com.example.data.model.StartConversationRequest
import com.example.data.model.TextContentDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

/**
 * Talks to the OpenHands Cloud V1 app server.
 *
 * Endpoint contract (verified against OpenHands 1.92.0):
 *  - listing          `GET  /api/v1/app-conversations/search`
 *  - start            `POST /api/v1/app-conversations`  -> AppConversationStartTask
 *  - start poll       `GET  /api/v1/app-conversations/start-tasks?ids=`
 *  - batch fetch      `GET  /api/v1/app-conversations?ids=`
 *  - events           `GET  /api/v1/conversation/{id}/events/search`
 *  - send message     `POST /api/v1/app-conversations/{id}/send-message`
 */
class OpenHandsRepository(
    private val context: Context,
    private val database: AppDatabase
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("openhands_prefs", Context.MODE_PRIVATE)

    private val conversationDao = database.conversationDao()
    private val eventDao = database.eventDao()
    private val templateDao = database.templateDao()

    var apiKey: String
        get() {
            val pref = prefs.getString("api_key", null)
            val fallback = try {
                BuildConfig::class.java.getField("OPENHANDS_API_KEY").get(null) as String
            } catch (_: NoSuchFieldException) {
                ""
            }
            val key = pref ?: fallback
            return if (key == "OPENHANDS_API_KEY_PLACEHOLDER") "" else key
        }
        set(value) = prefs.edit().putString("api_key", value.trim()).apply()

    var baseUrl: String
        get() = prefs.getString("base_url", DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
        set(value) = prefs.edit().putString("base_url", value.trim()).apply()

    /** Whether the background keep-alive service should run. Off by default. */
    var keepAliveEnabled: Boolean
        get() = prefs.getBoolean("keep_alive_enabled", false)
        set(value) = prefs.edit().putBoolean("keep_alive_enabled", value).apply()

    /** Persisted appearance selection; defaults to following the system theme. */
    var themeMode: String
        get() = prefs.getString("theme_mode", "SYSTEM") ?: "SYSTEM"
        set(value) = prefs.edit().putString("theme_mode", value).apply()

    private var apiService: OpenHandsApiService = createApiService()

    /** Typed client for the non-`/api/v1` Cloud surface (admin, keys, billing, …). */
    var cloudOps: CloudOpsRepository = createCloudOps()

    private fun createApiService(): OpenHandsApiService {
        return ApiClient.create(
            baseUrlProvider = { baseUrl },
            apiKeyProvider = { apiKey }
        )
    }

    private fun createCloudOps(): CloudOpsRepository {
        return CloudOpsRepository(
            ApiClient.createCloudAdmin(
                baseUrlProvider = { baseUrl },
                apiKeyProvider = { apiKey }
            )
        )
    }

    fun updateConfig(newKey: String, newUrl: String) {
        apiKey = newKey
        baseUrl = if (newUrl.isNotBlank()) newUrl else DEFAULT_BASE_URL
        apiService = createApiService()
        cloudOps = createCloudOps()
    }

    // Reactive streams from Room
    val conversationsFlow: Flow<List<OpenHandsConversation>> =
        conversationDao.getAllConversations().map { list -> list.map { it.toDomain() } }

    val templatesFlow: Flow<List<PromptTemplate>> =
        templateDao.getAllTemplates().map { list -> list.map { it.toDomain() } }

    fun getConversationFlow(id: String): Flow<OpenHandsConversation?> =
        conversationDao.getConversationById(id).map { it?.toDomain() }

    fun getEventsFlow(conversationId: String): Flow<List<AgentEvent>> =
        eventDao.getEventsForConversation(conversationId).map { list -> list.map { it.toDomain() } }

    suspend fun checkHealth(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getCurrentUser()
            when {
                response.isSuccessful ->
                    Result.success("Connected to OpenHands Cloud (HTTP ${response.code()})")
                response.code() == 401 ->
                    Result.failure(Exception("Invalid API key (401 Unauthorized). Update it in Settings."))
                else ->
                    Result.failure(Exception(ApiClient.errorMessage(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Total conversation count for the account (`GET /api/v1/app-conversations/count`). */
    suspend fun countConversations(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.countConversations()
            if (response.isSuccessful) Result.success(response.body()?.string()?.trim()?.toIntOrNull() ?: 0)
            else Result.failure(Exception(authAwareMessage(response)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Number of events recorded for a conversation. */
    suspend fun countEvents(conversationId: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.countEvents(conversationId)
            if (response.isSuccessful) Result.success(response.body()?.string()?.trim()?.toIntOrNull() ?: 0)
            else Result.failure(Exception(authAwareMessage(response)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshConversations(): Result<List<OpenHandsConversation>> =
        withContext(Dispatchers.IO) {
            try {
                val entities = mutableListOf<ConversationEntity>()
                var pageId: String? = null
                var pages = 0
                do {
                    val response = apiService.searchConversations(limit = PAGE_LIMIT, pageId = pageId)
                    if (!response.isSuccessful) {
                        if (entities.isEmpty()) {
                            return@withContext Result.failure(Exception(authAwareMessage(response)))
                        }
                        break
                    }
                    val page = response.body()
                    entities += page?.items.orEmpty()
                        .filter { it.id != null }
                        .map { it.toEntity() }
                    pageId = page?.nextPageId
                    pages++
                } while (!pageId.isNullOrBlank() && pages < MAX_CONVERSATION_PAGES)

                if (entities.isNotEmpty()) {
                    conversationDao.insertConversations(entities)
                }
                Result.success(entities.map { it.toDomain() })
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Starts a conversation. `POST /api/v1/app-conversations` is asynchronous:
     * it returns a start task whose `id` is NOT a conversation id. The real
     * `app_conversation_id` only appears once the task reaches READY, so the
     * caller awaits it with [awaitConversationId].
     */
    suspend fun startConversation(
        prompt: String,
        repository: String?,
        branch: String? = null,
        llmModel: String? = null,
        llmProfile: String? = null,
        agentType: String? = null,
        agentProfileId: String? = null,
        gitProvider: String? = null,
        title: String? = null,
    ): Result<StartTaskHandle> = withContext(Dispatchers.IO) {
        val cleanRepo = repository?.trim()?.ifEmpty { null }
        val cleanBranch = branch?.trim()?.ifEmpty { null }

        try {
            val request = StartConversationRequest(
                initialMessage = SendMessagePayload(
                    content = listOf(TextContentDto(text = prompt))
                ),
                selectedRepository = cleanRepo,
                selectedBranch = cleanBranch,
                llmModel = llmModel?.trim()?.ifEmpty { null },
                llmProfile = llmProfile?.trim()?.ifEmpty { null },
                agentType = agentType?.trim()?.ifEmpty { null },
                agentProfileId = agentProfileId?.trim()?.ifEmpty { null },
                gitProvider = gitProvider?.trim()?.ifEmpty { null },
                title = title?.trim()?.ifEmpty { null },
            )
            val response = apiService.startConversation(request)
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception(authAwareMessage(response)))
            }
            val task = response.body()
                ?: return@withContext Result.failure(Exception("Empty response from OpenHands Cloud"))
            val taskId = task.id
                ?: return@withContext Result.failure(Exception("Start task response is missing an id"))

            task.appConversationId?.takeIf { it.isNotBlank() }?.let { fetchConversation(it) }
            Result.success(StartTaskHandle(taskId, task.appConversationId?.takeIf { it.isNotBlank() }))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Polls a start task until the sandbox is ready and a conversation id exists. */
    suspend fun awaitConversationId(taskId: String): Result<String> = withContext(Dispatchers.IO) {
        var attempts = 0
        while (attempts < START_TASK_MAX_POLLS) {
            delay(START_TASK_POLL_INTERVAL_MS)
            attempts++
            val poll = try {
                apiService.getStartTask(taskId)
            } catch (e: Exception) {
                return@withContext Result.failure(e)
            }
            if (poll.code() == 401) {
                return@withContext Result.failure(Exception(authAwareMessage(poll)))
            }
            if (!poll.isSuccessful) continue
            val task = poll.body()?.firstOrNull { it != null } ?: continue
            if (task.status.equals("ERROR", ignoreCase = true)) {
                return@withContext Result.failure(
                    Exception(task.detail ?: "OpenHands Cloud failed to start the conversation")
                )
            }
            val conversationId = task.appConversationId?.takeIf { it.isNotBlank() }
            if (conversationId != null) {
                fetchConversation(conversationId)
                return@withContext Result.success(conversationId)
            }
        }
        Result.failure(Exception("Timed out waiting for the sandbox to start. It may still be provisioning."))
    }

    suspend fun refreshEvents(
        conversationId: String,
        deep: Boolean = false
    ): Result<List<AgentEvent>> =
        withContext(Dispatchers.IO) {
            try {
                val entities = mutableListOf<AgentEventEntity>()
                var pageId: String? = null
                var pages = 0
                val maxPages = if (deep) MAX_EVENT_PAGES else 1
                do {
                    val response = apiService.searchEvents(
                        id = conversationId,
                        limit = EVENT_PAGE_LIMIT,
                        excludeFullState = true,
                        pageId = pageId
                    )
                    if (!response.isSuccessful) {
                        if (entities.isEmpty()) {
                            return@withContext Result.failure(Exception(authAwareMessage(response)))
                        }
                        break
                    }
                    val page = response.body()
                    entities += page?.items.orEmpty()
                        .mapNotNull { it.toEntity(conversationId) }
                    pageId = page?.nextPageId
                    pages++
                } while (!pageId.isNullOrBlank() && pages < maxPages)

                if (entities.isNotEmpty()) {
                    eventDao.insertEvents(entities)
                }
                Result.success(entities.map { it.toDomain() })
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun sendMessage(conversationId: String, text: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val request = SendMessagePayload(
                    content = listOf(TextContentDto(text = text))
                )
                val response = apiService.sendMessage(conversationId, request)
                if (response.isSuccessful) {
                    refreshEvents(conversationId)
                    Result.success(Unit)
                } else {
                    Result.failure(Exception(authAwareMessage(response)))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /** Lists files the agent changed in the sandbox (git status + path). */
    suspend fun getGitChanges(conversationId: String): Result<List<GitChange>> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.getGitChanges(conversationId)
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception(authAwareMessage(response)))
                }
                val changes = response.body().orEmpty()
                    .mapNotNull { dto ->
                        val path = dto?.path?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                        GitChange(status = dto.status ?: "MODIFIED", path = path)
                    }
                Result.success(changes)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun deleteConversation(conversationId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.deleteConversation(conversationId)
                if (response.isSuccessful) {
                    eventDao.deleteEventsForConversation(conversationId)
                    conversationDao.deleteConversation(conversationId)
                    Result.success(Unit)
                } else {
                    Result.failure(Exception(authAwareMessage(response)))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Keeps a running sandbox alive by issuing a real runtime call.
     *
     * The agent server pauses a sandbox after `runtime_idle_timeout_seconds`
     * (1200s / 20 min) with no runtime activity. Only calls that reach the
     * runtime reset that timer, so this uses `git/changes` (a runtime proxy)
     * rather than a DB-backed endpoint such as the conversation record.
     */
    suspend fun keepAlive(conversationId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.getGitChanges(conversationId)
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception(authAwareMessage(response)))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Resumes a paused sandbox. Idempotent server-side, so it is safe to call
     * when the sandbox turns out to be already running.
     */
    suspend fun resumeSandbox(sandboxId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.resumeSandbox(sandboxId)
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception(authAwareMessage(response)))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Conversations worth keeping alive: those whose sandbox still exists and
     * whose agent has not reached a terminal state.
     */
    suspend fun activeSandboxTargets(): List<SandboxTarget> = withContext(Dispatchers.IO) {
        conversationDao.getActiveConversations().mapNotNull { entity ->
            val sandboxId = entity.sandboxId
            if (sandboxId.isNullOrBlank()) return@mapNotNull null
            val status = ConversationStatus.fromName(entity.status)
            if (status in TERMINAL_STATUSES) return@mapNotNull null
            SandboxTarget(
                conversationId = entity.id,
                sandboxId = sandboxId,
                sandboxStatus = entity.sandboxStatus,
                status = status,
                hasRuntimeAccess = !entity.conversationUrl.isNullOrBlank() &&
                    !entity.sessionApiKey.isNullOrBlank(),
            )
        }
    }

    private suspend fun fetchConversation(conversationId: String) {
        try {
            val response = apiService.getConversationsByIds(conversationId)
            val dto = response.body()?.firstOrNull { it != null }
            if (response.isSuccessful && dto != null) {
                conversationDao.insertConversation(dto.toEntity())
            }
        } catch (_: Exception) {
            // The conversation list refresh will pick it up later.
        }
    }

    /** Re-fetches one conversation so its live status replaces the cached row. */
    suspend fun refreshConversation(conversationId: String): Result<OpenHandsConversation?> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.getConversationsByIds(conversationId)
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception(authAwareMessage(response)))
                }
                val dto = response.body()?.firstOrNull { it != null }
                    ?: return@withContext Result.success(null)
                val entity = dto.toEntity()
                conversationDao.insertConversation(entity)
                Result.success(entity.toDomain())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ---- conversation metadata --------------------------------------------

    suspend fun updateConversation(
        conversationId: String,
        title: String? = null,
        branch: String? = null,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.updateConversation(
                conversationId,
                ConversationUpdateRequest(title = title, selectedBranch = branch)
            )
            if (response.isSuccessful) {
                response.body()?.let { conversationDao.insertConversation(it.toEntity()) }
                Result.success(Unit)
            } else {
                Result.failure(Exception(authAwareMessage(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Queues a message for when the agent becomes free. */
    suspend fun queueMessage(conversationId: String, text: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.queuePendingMessage(
                    conversationId,
                    PendingMessageRequest(content = listOf(TextContentDto(text = text)))
                )
                if (response.isSuccessful) Result.success(Unit)
                else Result.failure(Exception(authAwareMessage(response)))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ---- model / profile catalogs -----------------------------------------

    suspend fun listProfiles(): Result<List<ProfileInfoDto>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.listProfiles()
            if (response.isSuccessful) {
                Result.success(response.body()?.profiles.orEmpty())
            } else {
                Result.failure(Exception(authAwareMessage(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ---- agent profiles (cloud; used by the New Task ACP picker) -----------

    /**
     * Lists every agent profile. The New Task screen filters to the ACP ones
     * (`agent_kind == "acp"`) so a task can be started on Claude Code, Codex,
     * Gemini CLI, Kimi, OpenCode, or any custom ACP server.
     */
    suspend fun listAgentProfiles(): Result<List<AgentProfileInfoDto>> = withContext(Dispatchers.IO) {
        listAgentProfilesDetailed().map { it.profiles.orEmpty() }
    }

    /**
     * The full agent-profile listing, including `active_agent_profile_id`, which
     * the plain list form drops. The settings screen uses it to highlight the
     * account's active profile.
     */
    suspend fun listAgentProfilesDetailed(): Result<AgentProfileListDto> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.listAgentProfiles()
            if (response.isSuccessful) {
                Result.success(response.body() ?: AgentProfileListDto())
            } else {
                Result.failure(Exception(authAwareMessage(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Reads one agent profile body so the ACP server/model/effort are visible. */
    suspend fun getAgentProfile(name: String): Result<AcpAgentProfileDto> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getAgentProfile(name)
            if (response.isSuccessful) {
                Result.success(response.body()?.profile ?: AcpAgentProfileDto())
            } else {
                Result.failure(Exception(authAwareMessage(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Creates or replaces an agent profile. */
    suspend fun saveAgentProfile(request: SaveAgentProfileRequest): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.saveAgentProfile(request.name, request)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception(authAwareMessage(response)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Deletes an agent profile by name. */
    suspend fun deleteAgentProfile(name: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.deleteAgentProfile(name)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception(authAwareMessage(response)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Renames an agent profile. */
    suspend fun renameAgentProfile(name: String, newName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.renameAgentProfile(name, RenameAgentProfileRequest(newName))
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception(authAwareMessage(response)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Makes an agent profile the active one for the account. */
    suspend fun activateAgentProfile(profileId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.activateAgentProfile(profileId)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception(authAwareMessage(response)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Clears the active agent profile for the account. */
    suspend fun deactivateAgentProfile(profileId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.deactivateAgentProfile(profileId)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception(authAwareMessage(response)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Materializes a stored profile into a runnable definition. */
    suspend fun materializeAgentProfile(name: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.materializeAgentProfile(name)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception(authAwareMessage(response)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchModels(query: String? = null): Result<List<LlmModelDto>> =
        withContext(Dispatchers.IO) {
            try {
                val items = mutableListOf<LlmModelDto>()
                var pageId: String? = null
                var pages = 0
                do {
                    val response = apiService.searchModels(
                        limit = PAGE_LIMIT, pageId = pageId, query = query?.ifBlank { null }
                    )
                    if (!response.isSuccessful) {
                        if (items.isEmpty()) return@withContext Result.failure(Exception(authAwareMessage(response)))
                        break
                    }
                    val page = response.body()
                    items += page?.items.orEmpty()
                    pageId = page?.nextPageId
                    pages++
                } while (!pageId.isNullOrBlank() && pages < MAX_CONVERSATION_PAGES)
                Result.success(items)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun searchProviders(): Result<List<ProviderDto>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.searchProviders()
            if (response.isSuccessful) Result.success(response.body()?.items.orEmpty())
            else Result.failure(Exception(authAwareMessage(response)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ---- git catalogs ------------------------------------------------------

    suspend fun searchRepositories(
        provider: String,
        query: String? = null,
    ): Result<List<RepositoryDto>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.searchRepositories(provider = provider, query = query)
            if (response.isSuccessful) Result.success(response.body()?.items.orEmpty())
            else Result.failure(Exception(authAwareMessage(response)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchBranches(
        provider: String,
        repository: String,
        query: String,
    ): Result<List<BranchDto>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.searchBranches(provider, repository, query)
            if (response.isSuccessful) Result.success(response.body()?.items.orEmpty())
            else Result.failure(Exception(authAwareMessage(response)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchSuggestedTasks(): Result<List<SuggestedTaskDto>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.searchSuggestedTasks()
            if (response.isSuccessful) Result.success(response.body()?.items.orEmpty())
            else Result.failure(Exception(authAwareMessage(response)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getGitInfo(): Result<UserGitInfoDto> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getGitInfo()
            if (response.isSuccessful) {
                response.body()?.let { Result.success(it) }
                    ?: Result.failure(Exception("Empty git-info response"))
            } else {
                Result.failure(Exception(authAwareMessage(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ---- sandboxes ---------------------------------------------------------

    suspend fun searchSandboxes(): Result<List<SandboxInfoDto>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.searchSandboxes()
            if (response.isSuccessful) Result.success(response.body()?.items.orEmpty())
            else Result.failure(Exception(authAwareMessage(response)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSandbox(sandboxId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.deleteSandbox(sandboxId)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception(authAwareMessage(response)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Pauses a running sandbox, releasing compute while keeping its storage. */
    suspend fun pauseSandbox(sandboxId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.pauseSandbox(sandboxId)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception(authAwareMessage(response)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Reads a single sandbox record by id. */
    suspend fun getSandbox(sandboxId: String): Result<SandboxInfoDto> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getSandbox(sandboxId)
            if (response.isSuccessful) response.body()?.let { Result.success(it) }
                ?: Result.failure(Exception("Empty sandbox response"))
            else Result.failure(Exception(authAwareMessage(response)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Reads one secret's value from a sandbox's settings. */
    suspend fun getSandboxSecretValue(sandboxId: String, name: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.getSandboxSecretValue(sandboxId, name)
                if (response.isSuccessful) Result.success(response.body()?.string().orEmpty())
                else Result.failure(Exception(authAwareMessage(response)))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun searchSandboxSpecs(): Result<List<SandboxSpecInfoDto>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.searchSandboxSpecs()
            if (response.isSuccessful) Result.success(response.body()?.items.orEmpty())
            else Result.failure(Exception(authAwareMessage(response)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ---- skills / secrets --------------------------------------------------

    suspend fun searchSkills(): Result<List<SkillInfoDto>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.searchSkills()
            if (response.isSuccessful) Result.success(response.body()?.items.orEmpty())
            else Result.failure(Exception(authAwareMessage(response)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchSecrets(): Result<List<SecretDto>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.searchSecrets()
            if (response.isSuccessful) Result.success(response.body()?.items.orEmpty())
            else Result.failure(Exception(authAwareMessage(response)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createSecret(name: String, value: String, description: String?): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.createSecret(
                    CreateSecretRequest(name = name, value = value, description = description)
                )
                if (response.isSuccessful) Result.success(Unit)
                else Result.failure(Exception(authAwareMessage(response)))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun deleteSecret(secretId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.deleteSecret(secretId)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception(authAwareMessage(response)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateSecret(secretId: String, value: String?, description: String?): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.updateSecret(
                    secretId, UpdateSecretRequest(description = description, value = value)
                )
                if (response.isSuccessful) Result.success(Unit)
                else Result.failure(Exception(authAwareMessage(response)))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ---- settings / profiles ----------------------------------------------

    suspend fun getSettings(): Result<UserSettingsDto> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getSettings()
            if (response.isSuccessful) response.body()?.let { Result.success(it) }
                ?: Result.failure(Exception("Empty settings response"))
            else Result.failure(Exception(authAwareMessage(response)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateSettings(request: UpdateUserSettingsRequest): Result<UserSettingsDto> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.updateSettings(request)
                if (response.isSuccessful) response.body()?.let { Result.success(it) }
                    ?: Result.success(UserSettingsDto())
                else Result.failure(Exception(authAwareMessage(response)))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getProfile(name: String): Result<ProfileDetailDto> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getProfile(name)
            if (response.isSuccessful) response.body()?.let { Result.success(it) }
                ?: Result.failure(Exception("Empty profile response"))
            else Result.failure(Exception(authAwareMessage(response)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveProfile(name: String, config: ProfileConfigDto, includeSecrets: Boolean): Result<ProfileMutationDto> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.saveProfile(
                    name,
                    SaveProfileRequest(llm = config, includeSecrets = includeSecrets)
                )
                if (response.isSuccessful) Result.success(response.body() ?: ProfileMutationDto())
                else Result.failure(Exception(authAwareMessage(response)))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun deleteProfile(name: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.deleteProfile(name)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception(authAwareMessage(response)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun activateProfile(name: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.activateProfile(name)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception(authAwareMessage(response)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun renameProfile(name: String, newName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.renameProfile(name, RenameProfileRequest(newName))
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception(authAwareMessage(response)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ---- git extras / users ------------------------------------------------

    suspend fun searchGitInstallations(provider: String): Result<List<String>> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.searchGitInstallations(provider)
                if (response.isSuccessful) Result.success(response.body()?.items.orEmpty())
                else Result.failure(Exception(authAwareMessage(response)))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getRepositoryDetails(provider: String, repository: String): Result<RepositoryDetailDto> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.getRepositoryDetails(provider, repository)
                if (response.isSuccessful) response.body()?.let { Result.success(it) }
                    ?: Result.failure(Exception("Empty repository details"))
                else Result.failure(Exception(authAwareMessage(response)))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getGitOrganizations(): Result<GitOrganizationsDto> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getGitOrganizations()
            if (response.isSuccessful) response.body()?.let { Result.success(it) }
                ?: Result.failure(Exception("Empty git organizations response"))
            else Result.failure(Exception(authAwareMessage(response)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unlinkGitProvider(provider: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.unlinkGitProvider(provider)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception(authAwareMessage(response)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Removes every stored git provider token for the account. */
    suspend fun clearGitProviderTokens(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.deleteGitProviderTokens()
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception(authAwareMessage(response)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun storeGitProviderTokens(provider: String, token: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.storeGitProviderTokens(
                    GitProviderTokenRequest(providerTokens = mapOf(provider to token))
                )
                if (response.isSuccessful) Result.success(response.body()?.message ?: "Stored")
                else Result.failure(Exception(authAwareMessage(response)))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getWebClientConfig(): Result<WebClientConfigDto> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getWebClientConfig()
            if (response.isSuccessful) response.body()?.let { Result.success(it) }
                ?: Result.failure(Exception("Empty web-client config"))
            else Result.failure(Exception(authAwareMessage(response)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ---- start tasks / sandbox extras -------------------------------------

    suspend fun searchStartTasks(): Result<List<StartTaskResponse>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.searchStartTasks()
            if (response.isSuccessful) Result.success(response.body()?.items.orEmpty())
            else Result.failure(Exception(authAwareMessage(response)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Files in the conversation's repository (cloud path, distinct from the runtime browser). */
    suspend fun getConversationFiles(conversationId: String, path: String?): Result<List<String>> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.getConversationFiles(conversationId, path)
                if (response.isSuccessful) Result.success(response.body().orEmpty().filterNotNull())
                else Result.failure(Exception(authAwareMessage(response)))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getConversationFile(conversationId: String, filePath: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.getConversationFile(conversationId, filePath)
                if (response.isSuccessful) Result.success(response.body()?.string().orEmpty())
                else Result.failure(Exception(authAwareMessage(response)))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getConversationGitDiff(conversationId: String, path: String?): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.getConversationGitDiff(conversationId, path)
                if (response.isSuccessful) Result.success(response.body()?.string().orEmpty())
                else Result.failure(Exception(authAwareMessage(response)))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getConversationHooks(conversationId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getConversationHooks(conversationId)
            if (response.isSuccessful) Result.success(response.body()?.string().orEmpty())
            else Result.failure(Exception(authAwareMessage(response)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Streams the conversation trajectory zip to [sink]. Returns the byte count. */
    suspend fun downloadConversation(conversationId: String, sink: java.io.OutputStream): Result<Long> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.downloadConversation(conversationId)
                if (response.isSuccessful) {
                    val body = response.body() ?: return@withContext Result.failure(Exception("Empty download"))
                    body.byteStream().use { input -> input.copyTo(sink) }
                    Result.success(body.contentLength())
                } else Result.failure(Exception(authAwareMessage(response)))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getEventsById(conversationId: String, eventIds: String): Result<List<EventResponse>> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.getEventsById(conversationId, eventIds)
                if (response.isSuccessful) Result.success(response.body().orEmpty().filterNotNull())
                else Result.failure(Exception(authAwareMessage(response)))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getSandboxSpecs(ids: String): Result<List<SandboxSpecInfoDto>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getSandboxSpecs(ids)
            if (response.isSuccessful) Result.success(response.body().orEmpty().filterNotNull())
            else Result.failure(Exception(authAwareMessage(response)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAgentSchema(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getAgentSchema()
            if (response.isSuccessful) Result.success(response.body()?.string().orEmpty())
            else Result.failure(Exception(authAwareMessage(response)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getConversationSchema(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getConversationSchema()
            if (response.isSuccessful) Result.success(response.body()?.string().orEmpty())
            else Result.failure(Exception(authAwareMessage(response)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createSandbox(sandboxSpecId: String): Result<SandboxInfoDto> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.createSandbox(sandboxSpecId)
            if (response.isSuccessful) response.body()?.let { Result.success(it) }
                ?: Result.failure(Exception("Empty sandbox response"))
            else Result.failure(Exception(authAwareMessage(response)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSandboxSecrets(sandboxId: String): Result<List<SecretNameItemDto>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getSandboxSecrets(sandboxId)
            if (response.isSuccessful) Result.success(response.body()?.secrets.orEmpty())
            else Result.failure(Exception(authAwareMessage(response)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun callMcpTool(serverName: String, toolName: String, arguments: Map<String, Any>?): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.callMcpTool(
                    serverName, McpToolCallRequest(name = toolName, arguments = arguments)
                )
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.isError == true) {
                        Result.failure(Exception(body.text ?: "MCP tool returned an error"))
                    } else {
                        Result.success(body?.text ?: "")
                    }
                } else Result.failure(Exception(authAwareMessage(response)))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /** Validates an MCP server definition without persisting it. */
    suspend fun testMcpServer(name: String, server: Map<String, Any>): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val json = ApiClient.moshi.adapter(McpTestRequest::class.java)
                    .toJson(McpTestRequest(name = name, server = server))
                val body = json.toRequestBody("application/json".toMediaType())
                val response = apiService.testMcpServer(body)
                if (response.isSuccessful) Result.success(response.body()?.string().orEmpty())
                else Result.failure(Exception(authAwareMessage(response)))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /** Starts the MCP OAuth dance; the caller opens [McpOAuthStartResponseDto.authorizationUrl]. */
    suspend fun startMcpOAuth(name: String): Result<McpOAuthStartResponseDto> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.startMcpOAuth(McpOAuthStartRequest(name = name))
            if (response.isSuccessful) response.body()?.let { Result.success(it) }
                ?: Result.failure(Exception("Empty MCP OAuth response"))
            else Result.failure(Exception(authAwareMessage(response)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMcpOAuthStatus(jobId: String): Result<McpOAuthStatusResponseDto> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getMcpOAuthStatus(jobId)
            if (response.isSuccessful) response.body()?.let { Result.success(it) }
                ?: Result.failure(Exception("Empty MCP OAuth status"))
            else Result.failure(Exception(authAwareMessage(response)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchSkillsCatalog(): Result<List<InstalledSkillDto>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.searchSkillsCatalog()
            if (response.isSuccessful) Result.success(response.body()?.items.orEmpty())
            else Result.failure(Exception(authAwareMessage(response)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkMarketplace(name: String, source: String): Result<MarketplaceCheckResponseDto> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.checkMarketplace(
                    MarketplaceRegistrationDto(name = name, source = source)
                )
                if (response.isSuccessful) response.body()?.let { Result.success(it) }
                    ?: Result.failure(Exception("Empty marketplace check response"))
                else Result.failure(Exception(authAwareMessage(response)))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun previewMarketplaceSkills(name: String, source: String): Result<MarketplaceSkillsPreviewDto> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.previewMarketplaceSkills(
                    listOf(MarketplaceRegistrationDto(name = name, source = source))
                )
                if (response.isSuccessful) response.body()?.let { Result.success(it) }
                    ?: Result.failure(Exception("Empty marketplace preview response"))
                else Result.failure(Exception(authAwareMessage(response)))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getConversationSkills(conversationId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getConversationSkills(conversationId)
            if (response.isSuccessful) Result.success(response.body()?.string().orEmpty())
            else Result.failure(Exception(authAwareMessage(response)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun authAwareMessage(response: Response<*>): String {
        return if (response.code() == 401) {
            "Invalid or missing OpenHands API key (401 Unauthorized). Update it in Settings."
        } else {
            ApiClient.errorMessage(response)
        }
    }

    suspend fun seedInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        if (templateDao.count() == 0) {
            templateDao.insertTemplates(
                listOf(
                    SavedTemplateEntity(
                        id = "t1",
                        title = "Fix Bug or Failing Test",
                        category = "Debugging",
                        prompt = "Locate the cause of the failing test or error log in this repository, reproduce the issue, apply a minimal fix, and verify tests pass.",
                        defaultRepo = "All-Hands-AI/OpenHands"
                    ),
                    SavedTemplateEntity(
                        id = "t2",
                        title = "Add Comprehensive Unit Tests",
                        category = "Testing",
                        prompt = "Write comprehensive unit and integration tests for all newly added endpoints and service functions, achieving >85% test coverage.",
                        defaultRepo = null
                    ),
                    SavedTemplateEntity(
                        id = "t3",
                        title = "Code Review & Refactoring",
                        category = "Refactoring",
                        prompt = "Audit the codebase for performance bottlenecks, redundant abstractions, and code smells. Refactor into clean, modern patterns without breaking public APIs.",
                        defaultRepo = null
                    ),
                    SavedTemplateEntity(
                        id = "t4",
                        title = "Security & Dependency Audit",
                        category = "Security",
                        prompt = "Run vulnerability scan on dependencies, check for insecure input handling or secrets leakage, and upgrade vulnerable packages.",
                        defaultRepo = null
                    ),
                    SavedTemplateEntity(
                        id = "t5",
                        title = "Docker & CI/CD Pipeline Setup",
                        category = "DevOps",
                        prompt = "Create a multi-stage Dockerfile and GitHub Actions workflow to build, lint, and run tests on every push and pull request.",
                        defaultRepo = null
                    ),
                    SavedTemplateEntity(
                        id = "t6",
                        title = "Generate API Documentation",
                        category = "Documentation",
                        prompt = "Analyze all exposed endpoints, data models, and request/response payloads, and generate clean Markdown API documentation with curl examples.",
                        defaultRepo = null
                    )
                )
            )
        }
    }

    suspend fun clearCache() = withContext(Dispatchers.IO) {
        conversationDao.clearAll()
        eventDao.clearAll()
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://app.all-hands.dev"
        private const val PAGE_LIMIT = 50
        private const val EVENT_PAGE_LIMIT = 100
        private const val MAX_CONVERSATION_PAGES = 20
        private const val MAX_EVENT_PAGES = 10
        private const val START_TASK_MAX_POLLS = 40
        private const val START_TASK_POLL_INTERVAL_MS = 1_500L

        val TERMINAL_STATUSES = setOf(
            ConversationStatus.FINISHED,
            ConversationStatus.ERROR,
        )

        private val ISO_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        /** Parses an OpenHands ISO-8601 timestamp, tolerating fractional seconds. */
        fun parseIsoTimestamp(value: String?): Long {
            if (value.isNullOrBlank()) return 0L
            val normalized = value.substringBefore('.').substringBefore('+').trimEnd('Z')
            return try {
                synchronized(ISO_FORMAT) {
                    ISO_FORMAT.parse(normalized)?.time ?: 0L
                }
            } catch (e: Exception) {
                0L
            }
        }

        /**
         * Stable chronological sort key. Millisecond precision is not enough
         * (OpenHands emits several events per millisecond), so the sub-second
         * digits of the wire timestamp are preserved and the id breaks any
         * remaining tie.
         */
        fun sortKey(rawTimestamp: String?, eventId: String): String {
            val raw = rawTimestamp.orEmpty()
            val fraction = raw.substringAfter('.', "")
                .takeWhile { it.isDigit() }
                .padEnd(6, '0')
                .take(6)
            return "%013d%s|%s".format(parseIsoTimestamp(raw), fraction, eventId)
        }
    }
}

// ---------------------------------------------------------------------------
// Wire -> entity mapping
// ---------------------------------------------------------------------------

/**
 * Result of the asynchronous start call. [conversationId] is null while the
 * sandbox is still provisioning; poll [OpenHandsRepository.awaitConversationId].
 */
data class StartTaskHandle(
    val taskId: String,
    val conversationId: String?,
)

internal fun ConversationResponse.toEntity(): ConversationEntity {
    val created = OpenHandsRepository.parseIsoTimestamp(createdAt)
    val updated = OpenHandsRepository.parseIsoTimestamp(updatedAt)
    val id = resolvedId
    val acpJson = acpAvailableModels
        ?.takeIf { it.isNotEmpty() }
        ?.let { ApiClient.moshi.adapter<List<AcpModelInfoDto>>(
            com.squareup.moshi.Types.newParameterizedType(List::class.java, AcpModelInfoDto::class.java)
        ).toJson(it) }
    return ConversationEntity(
        id = id,
        title = title?.takeIf { it.isNotBlank() } ?: "Task #${id.take(8)}",
        status = ConversationStatus.resolve(sandboxStatus, executionStatus).name,
        selectedRepository = selectedRepository,
        selectedBranch = selectedBranch,
        createdAt = created,
        updatedAt = if (updated > 0L) updated else created,
        conversationUrl = conversationUrl ?: conversationUiUrl,
        sandboxId = sandboxId,
        sandboxStatus = sandboxStatus,
        sessionApiKey = sessionApiKey,
        agentKind = agentKind,
        llmModel = llmModel,
        acpServer = acpServer,
        acpSupportsRuntimeModelSwitch = acpSupportsRuntimeModelSwitch ?: false,
        acpAvailableModelsJson = acpJson,
        executionStatus = executionStatus,
    )
}

internal fun EventResponse.toEntity(conversationId: String): AgentEventEntity? {
    val eventId = id ?: UUID.randomUUID().toString()
    val time = OpenHandsRepository.parseIsoTimestamp(timestamp)
    val key = OpenHandsRepository.sortKey(timestamp, eventId)

    return when (kind) {
        "MessageEvent" -> {
            val role = llmMessage?.role ?: source ?: "user"
            val text = llmMessage?.content.plainText()
            if (text.isBlank()) null else AgentEventEntity(
                id = eventId,
                conversationId = conversationId,
                timestamp = time,
                source = if (role == "user") "user" else "agent",
                actionType = "message",
                content = text,
                detail = null,
                sortKey = key
            )
        }

        "ActionEvent" -> {
            val thought = thought.plainText()
            val (type, fallback, detail) = renderAction(toolName, action, summary)
            AgentEventEntity(
                id = eventId,
                conversationId = conversationId,
                timestamp = time,
                source = "agent",
                actionType = type,
                content = thought.ifBlank { fallback },
                detail = detail,
                sortKey = key
            )
        }

        "ObservationEvent" -> {
            val obs = observation
            val text = obs?.content.plainText()
            val detail = buildString {
                obs?.stdout?.takeIf { it.isNotBlank() }?.let { appendLine(it) }
                obs?.stderr?.takeIf { it.isNotBlank() }?.let { appendLine(it) }
                obs?.exitCode?.let { appendLine("exit code: $it") }
            }.trim().ifBlank { null }
            val content = text.ifBlank { obs?.message.orEmpty().ifBlank { "(no output)" } }
            AgentEventEntity(
                id = eventId,
                conversationId = conversationId,
                timestamp = time,
                source = "environment",
                actionType = "observation",
                content = content,
                detail = detail,
                isError = obs?.isError == true,
                sortKey = key
            )
        }

        // SystemPromptEvent / ConversationStateUpdateEvent / SecurityAnalysisEvent
        // and anything else are noise for the timeline.
        else -> null
    }
}

/** Concatenates the text parts of a content array, skipping images and blanks. */
private fun List<TextContentDto>?.plainText(): String =
    this.orEmpty().mapNotNull { it.text?.takeIf(String::isNotBlank) }.joinToString("\n").trim()

private data class RenderedAction(val type: String, val content: String, val detail: String?)

private fun renderAction(
    toolName: String?,
    action: ActionDto?,
    summary: String?
): RenderedAction {
    return when (toolName) {
        "terminal" -> RenderedAction(
            type = "bash",
            content = action?.command?.let { "$ $it" } ?: summary ?: "Running command",
            detail = null
        )
        "file_editor", "str_replace_editor", "planning_file_editor" -> {
            val detail = buildString {
                action?.oldStr?.takeIf { it.isNotBlank() }?.let { appendLine("- $it") }
                action?.newStr?.takeIf { it.isNotBlank() }?.let { appendLine("+ $it") }
            }.trim().ifBlank { null }
            RenderedAction(
                type = "edit",
                content = summary ?: action?.path?.let { "Editing $it" } ?: "Editing a file",
                detail = detail
            )
        }
        "browser", "browser_navigate", "browser_get_content" -> RenderedAction(
            type = "browse",
            content = summary ?: action?.url?.let { "Opening $it" } ?: "Browsing the web",
            detail = null
        )
        "grep", "glob", "list_directory", "read_file" -> RenderedAction(
            type = "view",
            content = summary ?: action?.path?.let { "Inspecting $it" } ?: "Inspecting the workspace",
            detail = null
        )
        null -> RenderedAction(
            type = "thought",
            content = summary ?: "Working",
            detail = null
        )
        else -> RenderedAction(
            type = toolName,
            content = summary ?: "Using $toolName",
            detail = null
        )
    }
}
