package com.example.data.repository

import com.example.data.api.ApiSettings
import com.example.data.api.OpenHandsApiService
import com.example.data.local.AppDatabase
import com.example.data.local.AgentEventEntity
import com.example.data.local.ConversationEntity
import com.example.data.local.SavedTemplateEntity
import com.example.data.model.AgentEvent
import com.example.data.model.AppConversationDto
import com.example.data.model.BranchDto
import com.example.data.model.CloudCredits
import com.example.data.model.CreateSecretRequest
import com.example.data.model.CreateConversationRequestDto
import com.example.data.model.EventDto
import com.example.data.model.GitChangeDto
import com.example.data.model.GitDiffDto
import com.example.data.model.GitUserDto
import com.example.data.model.LlmProfileDto
import com.example.data.model.LlmProfileListDto
import com.example.data.model.MessagePayloads
import com.example.data.model.MessageResponseDto
import com.example.data.model.OpenHandsConversation
import com.example.data.model.PageDto
import com.example.data.model.PendingMessageResponseDto
import com.example.data.model.PromptTemplate
import com.example.data.model.RepositoryDto
import com.example.data.model.SandboxDto
import com.example.data.model.SandboxSpecDto
import com.example.data.model.SendMessageRequest
import com.example.data.model.SkillDto
import com.example.data.model.StartTaskDto
import com.example.data.model.SuggestedTaskDto
import com.example.data.model.SwitchProfileRequest
import com.example.data.model.UserInfoDto
import com.example.data.model.WebClientConfigDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** Failure kinds the UI can branch on without string-matching. */
sealed class CloudError(message: String) : Exception(message) {
    class Unauthorized : CloudError("Invalid or missing API key (401). Update it in Settings.")
    class Api(val code: Int, detail: String) : CloudError("Cloud API error $code: $detail")
    class Network(cause: Throwable) : CloudError("Network error: ${cause.message ?: "connection failed"}")
}

class OpenHandsRepository(
    private val settings: ApiSettings,
    private val database: AppDatabase,
    private val apiProvider: () -> OpenHandsApiService,
    private val now: () -> Long = System::currentTimeMillis
) {
    private val conversationDao = database.conversationDao()
    private val eventDao = database.eventDao()
    private val templateDao = database.templateDao()

    private val api: OpenHandsApiService get() = apiProvider()

    /** conversationId → start-task id, so polling uses the right id even when
     *  the create response already carried the conversation id (docs §2.2). */
    private val startTaskIds = ConcurrentHashMap<String, String>()

    fun startTaskIdFor(conversationId: String): String = startTaskIds[conversationId] ?: conversationId

    // ---- Config -------------------------------------------------------------

    fun updateConfig(newKey: String, newUrl: String) {
        settings.apiKey = newKey
        settings.baseUrl = newUrl
    }

    fun isConfigured(): Boolean = settings.hasApiKey()

    /** Returns the stored key so the UI can keep it when re-saving settings. */
    fun storedApiKey(): String = settings.apiKey

    // ---- Reactive local streams ----------------------------------------------

    val conversationsFlow: Flow<List<OpenHandsConversation>> =
        conversationDao.getAllConversations().map { list -> list.map { it.toDomain() } }

    val templatesFlow: Flow<List<PromptTemplate>> =
        templateDao.getAllTemplates().map { list -> list.map { it.toDomain() } }

    fun getEventsFlow(conversationId: String): Flow<List<AgentEvent>> =
        eventDao.getEventsForConversation(conversationId).map { list -> list.map { it.toDomain() } }

    // ---- Health / credits -----------------------------------------------------

    suspend fun checkHealth(): Result<String> = withContext(Dispatchers.IO) {
        call("health") { api.health() }.map { body ->
            "Connected — service status: ${body.string().take(200)}"
        }
    }

    suspend fun getCredits(): Result<CloudCredits> = withContext(Dispatchers.IO) {
        call("credits") { api.credits() }.map { body ->
            CloudCredits(body?.credits?.toDoubleOrNull())
        }
    }

    // ---- Conversations ---------------------------------------------------------

    /**
     * Refreshes the conversation list. The cloud has no "list all" batch route
     * (batch GET requires ids), so /search with default filters is the surface.
     */
    suspend fun refreshConversations(@Suppress("UNUSED_PARAMETER") showFeedback: Boolean = true): Result<List<OpenHandsConversation>> =
        withContext(Dispatchers.IO) {
            call("search conversations") { api.searchConversations() }.map { page ->
                val entities = page.items.map { it.toEntity(now()) }
                if (entities.isNotEmpty()) conversationDao.insertConversations(entities)
                entities.map { it.toDomain() }
            }
        }

    /** Title-filtered remote search backed by the `title__contains` parameter. */
    suspend fun searchConversationsRemote(query: String): Result<List<OpenHandsConversation>> =
        withContext(Dispatchers.IO) {
            if (query.isBlank()) return@withContext refreshConversations()
            call("search conversations") {
                api.searchConversations(titleContains = query.trim())
            }.map { page ->
                val entities = page.items.map { it.toEntity(now()) }
                if (entities.isNotEmpty()) conversationDao.insertConversations(entities)
                entities.map { it.toDomain() }
            }
        }

    /** Raw paged search for callers that want pagination tokens. */
    suspend fun searchConversationsPage(
        titleContains: String? = null,
        pageId: String? = null,
        limit: Int = 50
    ): Result<PageDto<AppConversationDto>> = withContext(Dispatchers.IO) {
        call("search conversations") {
            api.searchConversations(titleContains = titleContains, pageId = pageId, limit = limit)
        }
    }

    suspend fun countConversations(): Result<Int> = withContext(Dispatchers.IO) {
        call("count conversations") { api.countConversations() }
    }

    /** Fetches specific conversations by id (batch GET). */
    suspend fun getConversations(ids: List<String>): Result<List<AppConversationDto>> =
        withContext(Dispatchers.IO) {
            if (ids.isEmpty()) return@withContext Result.success(emptyList())
            call("get conversations") { api.getConversations(ids) }.map { list ->
                list.filterNotNull().also { dtos ->
                    if (dtos.isNotEmpty()) {
                        conversationDao.insertConversations(dtos.map { it.toEntity(now()) })
                    }
                }
            }
        }

    /** Renames a conversation (PATCH metadata). */
    suspend fun renameConversation(conversationId: String, title: String): Result<AppConversationDto> =
        withContext(Dispatchers.IO) {
            call("rename conversation") {
                api.updateConversation(
                    conversationId,
                    com.example.data.model.UpdateConversationRequest(title = title)
                )
            }.onSuccess { dto ->
                conversationDao.insertConversation(dto.toEntity(now()))
            }
        }

    /**
     * Launches a task. Returns the conversation id resolved from the start task.
     * Persists an optimistic local row so the detail screen can open immediately.
     */
    suspend fun createConversation(
        prompt: String,
        repository: String?,
        branch: String? = null,
        gitProvider: String? = null,
        llmModel: String? = null,
        agentType: String = "default",
        title: String? = null,
        agentProfileId: String? = null
    ): Result<String> =
        withContext(Dispatchers.IO) {
            if (prompt.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Task prompt is empty"))
            }
            val cleanRepo = repository?.trim()?.ifEmpty { null }
            val request = CreateConversationRequestDto(
                initialMessage = SendMessageRequest.text(prompt),
                selectedRepository = cleanRepo,
                selectedBranch = branch?.trim()?.ifEmpty { null },
                gitProvider = gitProvider?.trim()?.ifEmpty { null },
                llmModel = llmModel?.trim()?.ifEmpty { null },
                agentType = agentType.ifBlank { "default" },
                title = title?.trim()?.ifEmpty { null },
                agentProfileId = agentProfileId?.trim()?.ifEmpty { null }
            )
            call("create conversation") { api.startConversation(request) }.mapCatching { start ->
                persistStartTask(start, prompt, cleanRepo)
            }
        }

    /** Polls the start task; returns the task once the server reports it. */
    suspend fun pollStartTask(startTaskId: String): Result<StartTaskDto> =
        withContext(Dispatchers.IO) {
            call("start task") { api.getStartTasks(listOf(startTaskId)) }.mapCatching { list ->
                list.filterNotNull()
                    .firstOrNull { it.id == startTaskId || list.size == 1 }
                    ?: throw IllegalStateException("Start task $startTaskId not found")
            }
        }

    /** Start-task history, newest first (e.g. to re-attach to a pending launch). */
    suspend fun searchStartTasks(limit: Int = 20): Result<PageDto<StartTaskDto>> =
        withContext(Dispatchers.IO) {
            call("search start tasks") { api.searchStartTasks(limit = limit) }
        }

    /**
     * Sends a follow-up message. If the conversation is still starting (not
     * READY), queues it as a pending message instead so it is delivered once
     * the sandbox comes up.
     */
    suspend fun sendMessage(conversationId: String, text: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            if (text.isBlank()) return@withContext Result.success(Unit)
            insertLocalEvent(
                conversationId = conversationId,
                source = "user",
                actionType = "message",
                content = text
            )
            call("send message") {
                api.sendMessage(conversationId, SendMessageRequest.text(text))
            }.map { }
        }

    /** Explicitly queues a message for a not-yet-READY conversation. */
    suspend fun queuePendingMessage(conversationId: String, text: String): Result<PendingMessageResponseDto> =
        withContext(Dispatchers.IO) {
            if (text.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Message is empty"))
            }
            insertLocalEvent(
                conversationId = conversationId,
                source = "user",
                actionType = "queued message",
                content = text
            )
            call("queue pending message") {
                api.queuePendingMessage(conversationId, SendMessageRequest.text(text))
            }
        }

    suspend fun refreshEvents(conversationId: String): Result<List<AgentEvent>> =
        withContext(Dispatchers.IO) {
            call("events") { api.searchEvents(conversationId) }.map { page ->
                val entities = page.items.map { it.toEntity(conversationId) }
                if (entities.isNotEmpty()) eventDao.insertEvents(entities)
                entities.map { it.toDomain() }
            }
        }

    /** Event history page with kind/timestamp filters and pagination. */
    suspend fun searchEvents(
        conversationId: String,
        kind: String? = null,
        pageId: String? = null,
        limit: Int = 100
    ): Result<PageDto<EventDto>> = withContext(Dispatchers.IO) {
        call("events") { api.searchEvents(conversationId, kind = kind, pageId = pageId, limit = limit) }
    }

    suspend fun countEvents(conversationId: String): Result<Int> =
        withContext(Dispatchers.IO) {
            call("count events") { api.countEvents(conversationId) }
        }

    suspend fun deleteConversation(conversationId: String): Result<Unit> = withContext(Dispatchers.IO) {
        call("delete conversation") { api.deleteConversation(conversationId) }.map {
            conversationDao.deleteConversation(conversationId)
            eventDao.deleteEventsForConversation(conversationId)
        }
    }

    // ---- Conversation files / export -------------------------------------------

    /** Lists files under a path inside the sandbox workspace. */
    suspend fun listFiles(conversationId: String, path: String? = null): Result<List<String>> =
        withContext(Dispatchers.IO) {
            call("list files") { api.listFiles(conversationId, path) }
        }

    /** Reads a workspace file's raw content. */
    suspend fun readFile(conversationId: String, filePath: String): Result<String> =
        withContext(Dispatchers.IO) {
            call("read file") { api.readFile(conversationId, filePath) }.map { it.string() }
        }

    /** Downloads the conversation trajectory (JSON export). */
    suspend fun downloadTrajectory(conversationId: String): Result<String> =
        withContext(Dispatchers.IO) {
            call("download trajectory") { api.downloadTrajectory(conversationId) }.map { it.string() }
        }

    /** Skills active in the conversation's sandbox (repo + global). */
    suspend fun conversationSkills(conversationId: String): Result<List<SkillDto>> =
        withContext(Dispatchers.IO) {
            call("conversation skills") { api.conversationSkills(conversationId) }
                .map { it.skills }
        }

    // ---- Git integration + profiles (restored from cloud-v1 client) ----------

    /** Repositories visible to the configured upstream git provider. */
    suspend fun searchRepositories(
        query: String = "",
        provider: String = DEFAULT_GIT_PROVIDER
    ): Result<List<RepositoryDto>> =
        withContext(Dispatchers.IO) {
            call("list repositories") {
                api.searchRepositories(provider = provider, query = query.ifEmpty { null })
            }.map { page -> page.items }
        }

    suspend fun searchBranches(
        repository: String,
        provider: String = DEFAULT_GIT_PROVIDER,
        query: String = ""
    ): Result<List<BranchDto>> =
        withContext(Dispatchers.IO) {
            call("list branches") { api.searchBranches(provider, repository, query) }
                .map { page -> page.items }
        }

    /** Suggested tasks (failing checks, open issues, …) across connected repos. */
    suspend fun suggestedTasks(limit: Int = 30): Result<List<SuggestedTaskDto>> =
        withContext(Dispatchers.IO) {
            call("suggested tasks") { api.searchSuggestedTasks(limit = limit) }.map { it.items }
        }

    /** Working-tree changes for a running conversation's sandbox. */
    suspend fun gitChanges(
        conversationId: String,
        repoPath: String = WORKSPACE_DIR
    ): Result<GitDiffDto> =
        withContext(Dispatchers.IO) {
            call("git changes") { api.gitChanges(conversationId, repoPath) }
                .map { changes -> GitDiffDto(files = changes) }
        }

    /** Unified diff for one file in the conversation's sandbox. */
    suspend fun gitDiff(
        conversationId: String,
        filePath: String,
        ref: String? = null
    ): Result<GitDiffDto> =
        withContext(Dispatchers.IO) {
            call("git diff") { api.gitDiff(conversationId, filePath, ref) }
        }

    /** Saved LLM profiles switchable onto a running conversation. */
    suspend fun listProfiles(): Result<List<LlmProfileDto>> =
        withContext(Dispatchers.IO) {
            call("list profiles") { api.listProfiles() }.map { it.profiles }
        }

    /** Profile list with the active profile name, for profile-management UIs. */
    suspend fun profilesWithActive(): Result<LlmProfileListDto> =
        withContext(Dispatchers.IO) {
            call("list profiles") { api.listProfiles() }
        }

    /** Applies a saved profile to a running conversation (mid-session model switch). */
    suspend fun switchProfile(conversationId: String, profileName: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            call("switch profile") {
                api.switchProfile(conversationId, SwitchProfileRequest(profileName))
            }.map { }
        }

    /** For ACP-backed conversations: switch the model exposed by the ACP server. */
    suspend fun switchAcpModel(conversationId: String, model: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            call("switch ACP model") {
                api.switchAcpModel(conversationId, com.example.data.model.SwitchAcpModelRequest(model))
            }.map { }
        }

    // ---- Sandboxes ---------------------------------------------------------------

    suspend fun searchSandboxes(limit: Int = 50): Result<List<SandboxDto>> =
        withContext(Dispatchers.IO) {
            call("search sandboxes") { api.searchSandboxes(limit = limit) }.map { it.items }
        }

    suspend fun getSandboxes(ids: List<String>): Result<List<SandboxDto>> =
        withContext(Dispatchers.IO) {
            if (ids.isEmpty()) return@withContext Result.success(emptyList())
            call("get sandboxes") { api.getSandboxes(ids) }.map { list -> list.filterNotNull() }
        }

    suspend fun createSandbox(specId: String? = null): Result<SandboxDto> =
        withContext(Dispatchers.IO) {
            call("create sandbox") { api.createSandbox(specId) }
        }

    suspend fun pauseSandbox(sandboxId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            call("pause sandbox") { api.pauseSandbox(sandboxId) }.map { }
        }

    suspend fun resumeSandbox(sandboxId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            call("resume sandbox") { api.resumeSandbox(sandboxId) }.map { }
        }

    suspend fun deleteSandbox(sandboxId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            call("delete sandbox") { api.deleteSandbox(sandboxId, sandboxId) }.map { }
        }

    suspend fun searchSandboxSpecs(limit: Int = 50): Result<List<SandboxSpecDto>> =
        withContext(Dispatchers.IO) {
            call("search sandbox specs") { api.searchSandboxSpecs(limit = limit) }.map { it.items }
        }

    // ---- Secrets --------------------------------------------------------------------

    suspend fun searchSecrets(nameContains: String? = null): Result<List<com.example.data.model.SecretDto>> =
        withContext(Dispatchers.IO) {
            call("search secrets") { api.searchSecrets(nameContains) }.map { it.items }
        }

    suspend fun createSecret(name: String, value: String): Result<MessageResponseDto> =
        withContext(Dispatchers.IO) {
            call("create secret") { api.createSecret(CreateSecretRequest(name, value)) }
        }

    /** Renames a secret; values are write-only and cannot be updated. */
    suspend fun renameSecret(secretId: String, newName: String): Result<MessageResponseDto> =
        withContext(Dispatchers.IO) {
            call("rename secret") {
                api.updateSecret(secretId, com.example.data.model.SecretDto(newName))
            }
        }

    suspend fun deleteSecret(secretId: String): Result<MessageResponseDto> =
        withContext(Dispatchers.IO) {
            call("delete secret") { api.deleteSecret(secretId) }
        }

    // ---- Account / config ------------------------------------------------------------

    suspend fun currentUser(): Result<UserInfoDto> = withContext(Dispatchers.IO) {
        call("user info") { api.me() }
    }

    suspend fun gitUser(): Result<GitUserDto> = withContext(Dispatchers.IO) {
        call("git user") { api.gitUser() }
    }

    suspend fun currentApiKey(): Result<com.example.data.model.CurrentApiKeyDto> =
        withContext(Dispatchers.IO) {
            call("current api key") { api.currentApiKey() }
        }

    suspend fun webClientConfig(): Result<WebClientConfigDto> = withContext(Dispatchers.IO) {
        call("web client config") { api.webClientConfig() }
    }

    /** Verified LLM models available on the cloud (for model pickers). */
    suspend fun searchModels(query: String? = null, limit: Int = 50): Result<List<com.example.data.model.LlmModelDto>> =
        withContext(Dispatchers.IO) {
            call("search models") { api.searchModels(query = query, limit = limit) }.map { it.items }
        }

    suspend fun searchProviders(query: String? = null): Result<List<com.example.data.model.LlmProviderDto>> =
        withContext(Dispatchers.IO) {
            call("search providers") { api.searchProviders(query) }.map { it.items }
        }

    suspend fun searchSkills(limit: Int = 50): Result<List<SkillDto>> =
        withContext(Dispatchers.IO) {
            call("search skills") { api.searchSkills(limit = limit) }.map { it.items }
        }

    /** Raw settings JSON (GET /api/v1/settings). */
    suspend fun settings(): Result<String> = withContext(Dispatchers.IO) {
        call("settings") { api.settings() }.map { it.string() }
    }

    /** Stores a settings patch (arbitrary key/value object). */
    suspend fun storeSettings(patch: Map<String, @JvmSuppressWildcards Any?>): Result<Unit> =
        withContext(Dispatchers.IO) {
            call("store settings") { api.storeSettings(patch) }.map { }
        }

    // ---- Local helpers ---------------------------------------------------------

    fun localConversation(id: String): Flow<OpenHandsConversation?> =
        conversationDao.getConversationById(id).map { it?.toDomain() }

    private suspend fun persistStartTask(
        start: StartTaskDto,
        prompt: String,
        repo: String?
    ): String {
        val conversationId = start.appConversationId
        val entityId = conversationId ?: start.id ?: UUID.randomUUID().toString()
        start.id?.let { startTaskIds[entityId] = it }
        val title = if (prompt.length > 60) prompt.take(57) + "…" else prompt
        conversationDao.insertConversation(
            ConversationEntity(
                id = entityId,
                title = title,
                sandboxStatus = null,
                executionStatus = null,
                rawStatus = start.status ?: "WORKING",
                repository = repo,
                createdAt = now(),
                updatedAt = now(),
                cost = 0.0,
                lastMessage = prompt
            )
        )
        insertLocalEvent(
            conversationId = entityId,
            source = "user",
            actionType = "message",
            content = prompt
        )
        return entityId
    }

    private suspend fun insertLocalEvent(
        conversationId: String,
        source: String,
        actionType: String?,
        content: String,
        detail: String? = null
    ) {
        eventDao.insertEvents(
            listOf(
                AgentEventEntity(
                    id = UUID.randomUUID().toString(),
                    conversationId = conversationId,
                    timestamp = now(),
                    source = source,
                    actionType = actionType,
                    content = content,
                    detail = detail
                )
            )
        )
    }

    private fun AppConversationDto.toEntity(fallbackTime: Long): ConversationEntity = ConversationEntity(
        id = resolvedId.ifBlank { UUID.randomUUID().toString() },
        title = title ?: "Conversation",
        sandboxStatus = sandboxStatus,
        executionStatus = executionStatus,
        rawStatus = null,
        repository = selectedRepository,
        createdAt = parseTimestamp(createdAt) ?: fallbackTime,
        updatedAt = parseTimestamp(updatedAt) ?: parseTimestamp(createdAt) ?: fallbackTime,
        cost = cost,
        lastMessage = null
    )

    private fun EventDto.toEntity(conversationId: String): AgentEventEntity {
        val action = toolName ?: kind?.removeSuffix("Event")?.lowercase()
        val command = MessagePayloads.argument(toolCall?.arguments, "command")
        val messageText = message?.takeIf { it.isNotBlank() }
        val llmText = llmMessage?.contentText()?.takeIf { it.isNotBlank() }
        val thoughtText = this.thoughtText?.takeIf { it.isNotBlank() }
        val observationText = this.observationText?.takeIf { it.isNotBlank() }
        val content = when {
            messageText != null -> messageText
            llmText != null -> llmText
            thoughtText != null -> thoughtText
            observationText != null -> observationText
            !command.isNullOrBlank() -> command
            !error.isNullOrBlank() -> error!!
            !detail.isNullOrBlank() -> detail!!
            !title.isNullOrBlank() -> title!!
            else -> "(no content)"
        }
        val observationRaw = when (val obs = observation) {
            is Map<*, *> -> obs["content"]?.let { MessagePayloads.contentToText(it) }
            else -> null
        }
        return AgentEventEntity(
            id = id ?: UUID.randomUUID().toString(),
            conversationId = conversationId,
            timestamp = parseTimestamp(timestamp) ?: now(),
            source = source ?: "agent",
            actionType = action,
            content = content,
            detail = observationRaw?.takeIf { it != content },
            isError = isError == true || error?.isNotBlank() == true ||
                (observation is Map<*, *> && (observation["is_error"] as? Boolean) == true)
        )
    }

    /**
     * Seeds templates always; seeds clearly-labeled demo conversations only when
     * no API key is configured (demo mode) and the store is empty.
     */
    suspend fun seedLocalData() = withContext(Dispatchers.IO) {
        seedTemplatesIfEmpty()
        if (settings.hasApiKey()) return@withContext
        val existing = conversationDao.getAllConversations().first()
        if (existing.isEmpty()) {
            conversationDao.insertConversations(DEMO_CONVERSATIONS(now()))
            eventDao.insertEvents(DEMO_EVENTS(now()))
        }
    }

    suspend fun seedTemplatesIfEmpty() {
        if (templateDao.count() == 0) {
            templateDao.insertTemplates(DEFAULT_TEMPLATES)
        }
    }

    suspend fun clearCache() {
        conversationDao.clearAll()
        eventDao.clearAll()
    }

    private inline fun <reified T : Any> call(label: String, block: () -> retrofit2.Response<T>): Result<T> {
        return try {
            val response = block()
            val body = response.body()
            when {
                response.isSuccessful && body != null -> Result.success(body)
                // 200-with-empty-body is legal for Unit responses (delete, patch).
                response.isSuccessful && T::class == Unit::class ->
                    @Suppress("UNCHECKED_CAST")
                    Result.success(Unit as T)
                response.isSuccessful && body == null ->
                    Result.failure(CloudError.Api(response.code(), "$label returned an empty body"))
                response.code() == 401 -> Result.failure(CloudError.Unauthorized())
                else -> Result.failure(
                    CloudError.Api(response.code(), response.errorBody()?.string()?.take(300) ?: response.message())
                )
            }
        } catch (e: java.io.IOException) {
            Result.failure(CloudError.Network(e))
        } catch (e: Exception) {
            Result.failure(CloudError.Network(e))
        }
    }

    companion object {
        /** Canonical sandbox workspace root (the runtime file endpoints' base). */
        const val WORKSPACE_DIR = "/workspace/project"

        /** Provider used when the caller has none selected. */
        const val DEFAULT_GIT_PROVIDER = "github"

        fun parseTimestamp(raw: String?): Long? {
            if (raw.isNullOrBlank()) return null
            return try {
                parseIsoUtc(raw)
            } catch (e: Exception) {
                null
            }
        }

        /**
         * Minimal ISO-8601 parser ("2025-01-15T10:30:00.123Z"; numeric offsets accepted).
         * Pure Kotlin so the app stays minSdk 24 without core-library desugaring.
         */
        private fun parseIsoUtc(raw: String): Long {
            val text = raw.trim()
            val tIndex = text.indexOfFirst { it == 'T' || it == ' ' }
            val datePart = if (tIndex >= 0) text.substring(0, tIndex) else text
            val timePart = if (tIndex >= 0) text.substring(tIndex + 1) else ""

            val date = datePart.split('-')
            if (date.size != 3) throw IllegalArgumentException("Invalid date: $datePart")
            val year = date[0].toInt()
            val month = date[1].toInt()
            val day = date[2].toInt()
            if (month !in 1..12 || day !in 1..31) throw IllegalArgumentException("Invalid date: $datePart")
            val dayMillis = daysSinceEpoch(year, month, day) * 86_400_000L
            if (timePart.isEmpty()) return dayMillis

            var time = timePart
            if (time.endsWith("Z") || time.endsWith("z")) time = time.dropLast(1)

            var offsetMinutes = 0
            val signPos = time.indexOfAny(charArrayOf('+', '-'))
            if (signPos >= 0) {
                val isNegative = time[signPos] == '-'
                val offset = time.substring(signPos + 1).replace(":", "")
                time = time.substring(0, signPos)
                if (offset.isNotEmpty()) {
                    val hours = offset.take(2).toInt()
                    val minutes = if (offset.length >= 4) offset.substring(2, 4).toInt() else 0
                    offsetMinutes = hours * 60 + minutes
                    if (isNegative) offsetMinutes = -offsetMinutes
                }
            }

            var millis = 0L
            val fracPos = time.indexOf('.')
            if (fracPos >= 0) {
                millis = time.substring(fracPos + 1).padEnd(3, '0').take(3).toLong()
                time = time.substring(0, fracPos)
            }

            val parts = time.split(':')
            val hour = parts[0].toInt()
            val minute = if (parts.size > 1) parts[1].toInt() else 0
            val second = if (parts.size > 2) parts[2].toInt() else 0

            return dayMillis + hour * 3_600_000L + minute * 60_000L + second * 1_000L + millis -
                offsetMinutes * 60_000L
        }

        /** Days from 1970-01-01 for a civil date (Howard Hinnant's days_from_civil). */
        private fun daysSinceEpoch(year: Int, month: Int, day: Int): Long {
            val y = year.toLong() - if (month <= 2) 1 else 0
            val era = (if (y >= 0) y else y - 399) / 400
            val yoe = y - era * 400
            val doy = (153 * (if (month > 2) month - 3 else month + 9) + 2) / 5 + day - 1
            val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
            return era * 146_097 + doe - 719_468
        }

        internal fun DEMO_CONVERSATIONS(now: Long): List<ConversationEntity> = listOf(
            ConversationEntity(
                id = "demo-1",
                title = "Fix token validation in auth middleware",
                sandboxStatus = "RUNNING",
                executionStatus = "finished",
                rawStatus = null,
                repository = "All-Hands-AI/OpenHands",
                createdAt = now - 7_200_000,
                updatedAt = now - 3_600_000,
                cost = 0.08,
                lastMessage = "All 14 unit tests pass — ready for PR."
            ),
            ConversationEntity(
                id = "demo-2",
                title = "Implement rate limiting middleware for FastAPI",
                sandboxStatus = "RUNNING",
                executionStatus = "running",
                rawStatus = null,
                repository = "octocat/Hello-World",
                createdAt = now - 3_600_000,
                updatedAt = now - 120_000,
                cost = 0.04,
                lastMessage = "Writing sliding-window limiter tests…"
            ),
            ConversationEntity(
                id = "demo-3",
                title = "Upgrade dependencies + security audit",
                sandboxStatus = "STARTING",
                executionStatus = null,
                rawStatus = "PREPARING_REPOSITORY",
                repository = "example/api-service",
                createdAt = now - 600_000,
                updatedAt = now - 60_000,
                cost = 0.0,
                lastMessage = null
            )
        )

        internal fun DEMO_EVENTS(now: Long): List<AgentEventEntity> = listOf(
            AgentEventEntity(
                id = "demo-e1",
                conversationId = "demo-1",
                timestamp = now - 7_200_000,
                source = "user",
                actionType = "message",
                content = "Bearer token parsing crashes when the Authorization header has extra whitespace. Fix and cover with tests."
            ),
            AgentEventEntity(
                id = "demo-e2",
                conversationId = "demo-1",
                timestamp = now - 7_100_000,
                source = "agent",
                actionType = "thought",
                content = "Inspect how the auth middleware splits the Authorization header, then harden the parser."
            ),
            AgentEventEntity(
                id = "demo-e3",
                conversationId = "demo-1",
                timestamp = now - 6_900_000,
                source = "agent",
                actionType = "bash",
                content = "grep -rn \"Bearer\" src/",
                detail = "src/middleware/auth.py:42: token = auth_header.split('Bearer ')[1]"
            ),
            AgentEventEntity(
                id = "demo-e4",
                conversationId = "demo-1",
                timestamp = now - 6_500_000,
                source = "agent",
                actionType = "edit",
                content = "src/middleware/auth.py",
                detail = "@@ -40 +41,4 @@\n- token = auth_header.split('Bearer ')[1]\n+ parts = auth_header.strip().split()\n+ if len(parts) != 2 or parts[0].lower() != 'bearer':\n+     raise HTTPException(401, 'Invalid Bearer format')"
            ),
            AgentEventEntity(
                id = "demo-e5",
                conversationId = "demo-1",
                timestamp = now - 4_000_000,
                source = "agent",
                actionType = "message",
                content = "Fixed the parser and added whitespace tests — 14/14 passing. Want me to open a PR?"
            )
        )

        internal val DEFAULT_TEMPLATES: List<SavedTemplateEntity> = listOf(
            SavedTemplateEntity(
                id = "t1",
                title = "Fix bug or failing test",
                category = "Debugging",
                prompt = "Locate the cause of the failing test or error, reproduce it, apply a minimal fix, and verify the test suite passes.",
                defaultRepo = "All-Hands-AI/OpenHands"
            ),
            SavedTemplateEntity(
                id = "t2",
                title = "Add comprehensive unit tests",
                category = "Testing",
                prompt = "Write unit and integration tests for the recently changed modules and report achieved coverage.",
                defaultRepo = null
            ),
            SavedTemplateEntity(
                id = "t3",
                title = "Code review & refactor",
                category = "Refactoring",
                prompt = "Audit the codebase for code smells and redundant abstractions; refactor without breaking public APIs.",
                defaultRepo = null
            ),
            SavedTemplateEntity(
                id = "t4",
                title = "Security & dependency audit",
                category = "Security",
                prompt = "Scan dependencies for known vulnerabilities, check input handling and secrets hygiene, and upgrade vulnerable packages.",
                defaultRepo = null
            ),
            SavedTemplateEntity(
                id = "t5",
                title = "Docker & CI/CD setup",
                category = "DevOps",
                prompt = "Create a multi-stage Dockerfile and a GitHub Actions workflow that builds, lints, and tests on every push.",
                defaultRepo = null
            ),
            SavedTemplateEntity(
                id = "t6",
                title = "Generate API documentation",
                category = "Documentation",
                prompt = "Analyze exposed endpoints and models, then generate concise Markdown API documentation with examples.",
                defaultRepo = null
            )
        )
    }
}
