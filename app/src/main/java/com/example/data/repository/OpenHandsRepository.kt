package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import com.example.data.api.ApiClient
import com.example.data.api.ApiException
import com.example.data.api.EventMapper
import com.example.data.api.OpenHandsApiService
import com.example.data.api.bodyOrNullOrThrow
import com.example.data.api.bodyOrThrow
import com.example.data.local.AgentEventEntity
import com.example.data.local.AppDatabase
import com.example.data.local.ConversationEntity
import com.example.data.local.SavedTemplateEntity
import com.example.data.model.AccountInfo
import com.example.data.model.AgentEvent
import com.example.data.model.AppConversationDto
import com.example.data.model.CreateSecretRequest
import com.example.data.model.CustomSecret
import com.example.data.model.LlmProfile
import com.example.data.model.Skill
import com.example.data.model.SwitchProfileRequest
import com.example.data.model.Conversation
import com.example.data.model.GitBranch
import com.example.data.model.GitChange
import com.example.data.model.GitDiff
import com.example.data.model.GitRepository
import com.example.data.model.IsoTime
import com.example.data.model.PromptTemplate
import com.example.data.model.Sandbox
import com.example.data.model.SandboxDto
import com.example.data.model.SandboxStatus
import com.example.data.model.SendMessageRequest
import com.example.data.model.StartConversationRequest
import com.example.data.model.StartTaskProgress
import com.example.data.model.StartTaskStatus
import com.example.data.model.SuggestedTask
import com.example.data.model.UpdateConversationRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

/** Parameters for starting a conversation; mirrors the fields the New Conversation screen exposes. */
data class StartConversationParams(
    val prompt: String,
    val repository: String? = null,
    val branch: String? = null,
    val gitProvider: String? = null,
    val title: String? = null,
    val agentType: String = "default",
    val llmModel: String? = null,
    val observabilitySpanName: String? = null,
    val observabilityTags: List<String>? = null,
    val observabilityMetadata: Map<String, String>? = null
)

data class SendMessageOutcome(val sandboxStatus: SandboxStatus, val resumedSandbox: Boolean, val message: String?)

/**
 * Single source of truth for cloud + cache access. All network calls run on IO and
 * return [Result] so the ViewModel can present failures instead of hiding them.
 */
class OpenHandsRepository(
    context: Context,
    private val database: AppDatabase,
    private val serviceFactory: (baseUrl: String, apiKeyProvider: () -> String) -> OpenHandsApiService =
        { url, keyProvider -> ApiClient.create(url, keyProvider, enableBodyLogging = BuildConfig.DEBUG) }
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val conversationDao = database.conversationDao()
    private val eventDao = database.eventDao()
    private val templateDao = database.templateDao()

    // ------------------------------------------------------------- configuration

    /** Key typed in Settings; falls back to the build-time key from `.env` / `.env.example`. */
    var apiKey: String
        get() {
            val stored = prefs.getString(KEY_API_KEY, null)?.trim()
            if (!stored.isNullOrEmpty()) return stored
            val buildKey = BuildConfig.OPENHANDS_API_KEY.trim()
            return if (buildKey == "OPENHANDS_API_KEY_PLACEHOLDER") "" else buildKey
        }
        set(value) = prefs.edit().putString(KEY_API_KEY, value.trim()).apply()

    val isUsingBuildTimeKey: Boolean
        get() = prefs.getString(KEY_API_KEY, null).isNullOrBlank() && apiKey.isNotBlank()

    var baseUrl: String
        get() = prefs.getString(KEY_BASE_URL, ApiClient.DEFAULT_BASE_URL) ?: ApiClient.DEFAULT_BASE_URL
        set(value) = prefs.edit().putString(KEY_BASE_URL, ApiClient.normalizeBaseUrl(value).removeSuffix("/")).apply()

    var showNoiseEvents: Boolean
        get() = prefs.getBoolean(KEY_SHOW_NOISE, false)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_NOISE, value).apply()

    @Volatile
    private var api: OpenHandsApiService = serviceFactory(baseUrl) { apiKey }

    fun updateConfig(newKey: String, newUrl: String) {
        apiKey = newKey
        baseUrl = newUrl
        api = serviceFactory(baseUrl) { apiKey }
    }

    // ---------------------------------------------------------------- observers

    val conversationsFlow: Flow<List<Conversation>> =
        conversationDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    fun conversationFlow(id: String): Flow<Conversation?> =
        conversationDao.observeById(id).map { it?.toDomain() }

    fun eventsFlow(conversationId: String): Flow<List<AgentEvent>> =
        eventDao.observeForConversation(conversationId).map { rows -> rows.map { it.toDomain() } }

    val templatesFlow: Flow<List<PromptTemplate>> =
        templateDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    // ------------------------------------------------------------------ account

    /** Validates the key and returns who we are. Used by "Test connection". */
    suspend fun fetchAccount(): Result<AccountInfo> = io {
        val me = api.me().bodyOrThrow()
        // Secondary calls are best-effort: they live outside the v1 surface and may be disabled.
        val key = runCatching { api.currentApiKey().bodyOrNullOrThrow() }.getOrNull()
        val credits = runCatching { api.credits().bodyOrNullOrThrow() }.getOrNull()
        AccountInfo(
            email = me.email,
            userId = me.id,
            orgName = me.orgName,
            orgId = me.orgId,
            role = me.role,
            gitUserName = me.gitUserName,
            apiKeyName = key?.name,
            apiKeyAuthType = key?.authType,
            credits = credits?.credits,
            maxBudgetPerTask = me.maxBudgetPerTask,
            v1Enabled = me.v1Enabled
        )
    }

    // ------------------------------------------------------------ conversations

    private var nextConversationPage: String? = null
    private var lastSearchQuery: String? = null
    /** Start time of the current full (unfiltered) sync; rows older than this are pruned once the last page lands. */
    private var fullSyncStartedAt: Long? = null

    /**
     * Loads the first page from the server and replaces the local cache with it
     * (so conversations deleted elsewhere disappear). Returns whether more pages exist.
     */
    suspend fun refreshConversations(titleQuery: String? = null): Result<Boolean> = io {
        val query = titleQuery?.trim()?.ifEmpty { null }
        val page = api.searchConversations(titleContains = query, limit = PAGE_SIZE).bodyOrThrow()
        val now = System.currentTimeMillis()
        val rows = page.items.map { it.toEntity(now) }
        conversationDao.upsertAll(rows)
        if (query == null) {
            // Full refresh: anything not re-fetched by the time the last page lands is stale.
            fullSyncStartedAt = now
            if (page.nextPageId == null) {
                conversationDao.deleteSyncedBefore(now)
                fullSyncStartedAt = null
            }
        } else {
            fullSyncStartedAt = null
        }
        nextConversationPage = page.nextPageId
        lastSearchQuery = query
        page.nextPageId != null
    }

    suspend fun loadMoreConversations(): Result<Boolean> = io {
        val pageId = nextConversationPage ?: return@io false
        val page = api.searchConversations(
            titleContains = lastSearchQuery,
            pageId = pageId,
            limit = PAGE_SIZE
        ).bodyOrThrow()
        conversationDao.upsertAll(page.items.map { it.toEntity(System.currentTimeMillis()) })
        nextConversationPage = page.nextPageId
        if (page.nextPageId == null) {
            fullSyncStartedAt?.let { conversationDao.deleteSyncedBefore(it) }
            fullSyncStartedAt = null
        }
        page.nextPageId != null
    }

    suspend fun refreshConversation(id: String): Result<Conversation?> = io {
        val dto = api.getConversations(listOf(id)).bodyOrThrow().firstOrNull()
        if (dto == null) {
            conversationDao.delete(id)
            eventDao.deleteForConversation(id)
            null
        } else {
            val entity = dto.toEntity(System.currentTimeMillis())
            conversationDao.upsert(entity)
            entity.toDomain()
        }
    }

    suspend fun countConversations(): Result<Int> = io { api.countConversations().bodyOrThrow() }

    /**
     * Starts a conversation and streams start-task progress until the server reports READY or ERROR.
     * The final emission is [StartTaskProgress.Ready] or [StartTaskProgress.Failed].
     */
    fun startConversation(params: StartConversationParams): Flow<StartTaskProgress> = flow {
        emit(StartTaskProgress.Submitting())
        val request = StartConversationRequest(
            initialMessage = SendMessageRequest.text(params.prompt.trim()),
            selectedRepository = params.repository?.trim()?.ifEmpty { null },
            selectedBranch = params.branch?.trim()?.ifEmpty { null },
            gitProvider = params.gitProvider?.trim()?.ifEmpty { null },
            title = params.title?.trim()?.ifEmpty { null },
            agentType = params.agentType,
            llmModel = params.llmModel?.trim()?.ifEmpty { null },
            observabilitySpanName = params.observabilitySpanName?.trim()?.ifEmpty { null },
            observabilityTags = params.observabilityTags?.map { it.trim() }?.filter { it.isNotEmpty() }?.ifEmpty { null },
            observabilityMetadata = params.observabilityMetadata?.filterValues { it.isNotBlank() }?.ifEmpty { null }
        )

        var task = try {
            api.startConversation(request).bodyOrThrow()
        } catch (e: Exception) {
            emit(StartTaskProgress.Failed(null, e.friendlyMessage()))
            return@flow
        }

        val deadline = System.currentTimeMillis() + START_TIMEOUT_MS
        var consecutiveErrors = 0
        while (true) {
            val status = StartTaskStatus.from(task.status)
            when {
                status == StartTaskStatus.READY && !task.appConversationId.isNullOrBlank() -> {
                    val convId = task.appConversationId
                    // Pull the conversation row so the detail screen has a title/status immediately.
                    runCatching { refreshConversation(convId) }
                    emit(StartTaskProgress.Ready(task.id, convId, task.sandboxId))
                    return@flow
                }
                status == StartTaskStatus.ERROR -> {
                    emit(StartTaskProgress.Failed(task.id, task.detail ?: "Conversation failed to start"))
                    return@flow
                }
                else -> emit(StartTaskProgress.InProgress(task.id, status, task.detail))
            }
            if (System.currentTimeMillis() > deadline) {
                emit(StartTaskProgress.Failed(task.id, "Timed out waiting for the sandbox to start."))
                return@flow
            }
            delay(START_POLL_MS)
            try {
                val refreshed = api.getStartTasks(listOf(task.id)).bodyOrThrow().firstOrNull()
                if (refreshed != null) task = refreshed
                consecutiveErrors = 0
            } catch (e: Exception) {
                if (++consecutiveErrors >= 5) {
                    emit(StartTaskProgress.Failed(task.id, e.friendlyMessage()))
                    return@flow
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun renameConversation(id: String, title: String): Result<Conversation> = io {
        val dto = api.updateConversation(id, UpdateConversationRequest(title = title.trim())).bodyOrThrow()
        val entity = dto.toEntity(System.currentTimeMillis())
        conversationDao.upsert(entity)
        entity.toDomain()
    }

    suspend fun setConversationPublic(id: String, public: Boolean): Result<Conversation> = io {
        val dto = api.updateConversation(id, UpdateConversationRequest(public = public)).bodyOrThrow()
        val entity = dto.toEntity(System.currentTimeMillis())
        conversationDao.upsert(entity)
        entity.toDomain()
    }

    suspend fun deleteConversation(id: String): Result<Unit> = io {
        try {
            api.deleteConversation(id).bodyOrThrow()
        } catch (e: ApiException) {
            if (e.code != 404) throw e // already gone server-side: fine
        }
        conversationDao.delete(id)
        eventDao.deleteForConversation(id)
    }

    // ------------------------------------------------------------------ messages

    /**
     * Sends a follow-up. If the sandbox is paused it is resumed first (the API requires RUNNING).
     * The local user message is written optimistically so the UI shows it right away.
     */
    suspend fun sendMessage(conversationId: String, text: String): Result<SendMessageOutcome> = io {
        val trimmed = text.trim()
        require(trimmed.isNotEmpty()) { "Message is empty" }

        var resumed = false
        val conv = conversationDao.getById(conversationId)?.toDomain()

        // A sandbox that is still booting cannot accept a message; the server queues it for us instead.
        if (conv?.sandboxStatus == SandboxStatus.STARTING) {
            val queued = api.queuePendingMessage(conversationId, SendMessageRequest.text(trimmed)).bodyOrThrow()
            eventDao.upsert(userMessageRow(conversationId, trimmed, "You (queued)"))
            return@io SendMessageOutcome(
                SandboxStatus.STARTING,
                resumedSandbox = false,
                message = "Queued while the sandbox starts (position ${queued.position})."
            )
        }

        if (conv?.sandboxStatus == SandboxStatus.PAUSED || conv?.sandboxStatus == SandboxStatus.MISSING) {
            conv.sandboxId?.let { sid ->
                api.resumeSandbox(sid).bodyOrThrow()
                resumed = true
                waitForSandboxRunning(sid)
            }
        }

        val optimisticRow = userMessageRow(conversationId, trimmed, "You")
        val optimisticId = optimisticRow.id
        eventDao.upsert(optimisticRow)

        val response = try {
            api.sendMessage(conversationId, SendMessageRequest.text(trimmed)).bodyOrThrow()
        } catch (e: Exception) {
            eventDao.upsert(optimisticRow.copy(title = "You (not delivered)", isError = true))
            throw e
        }
        val sandboxStatus = SandboxStatus.from(response.sandboxStatus)
        conversationDao.updateSandboxStatus(conversationId, sandboxStatus.name)
        if (!response.success) {
            throw ApiException(200, response.message ?: "The agent did not accept the message.")
        }
        // Replace the optimistic row with the server's MessageEvent on the next sync.
        runCatching { syncEvents(conversationId) }
        SendMessageOutcome(sandboxStatus, resumed, response.message)
    }

    /** Locally inserted user message, shown immediately and replaced by the server's MessageEvent on the next sync. */
    private fun userMessageRow(conversationId: String, text: String, title: String) = AgentEventEntity(
        id = "local-" + UUID.randomUUID(),
        conversationId = conversationId,
        timestamp = System.currentTimeMillis(),
        timestampIso = null,
        kind = "MESSAGE",
        category = "USER_MESSAGE",
        source = "user",
        title = title,
        body = text,
        code = null,
        codeLabel = null,
        toolName = null,
        actionId = null,
        toolCallId = null,
        filePath = null,
        exitCode = null,
        securityRisk = null
    )

    private suspend fun waitForSandboxRunning(sandboxId: String) {
        val deadline = System.currentTimeMillis() + RESUME_TIMEOUT_MS
        while (System.currentTimeMillis() < deadline) {
            val sb = api.getSandboxes(listOf(sandboxId)).bodyOrThrow().firstOrNull()
            val status = SandboxStatus.from(sb?.status)
            conversationDao.updateSandboxStatusBySandbox(sandboxId, status.name)
            when (status) {
                SandboxStatus.RUNNING -> return
                SandboxStatus.ERROR, SandboxStatus.MISSING ->
                    throw ApiException(409, "Sandbox is ${status.label.lowercase()}: ${sb?.statusDetail ?: "cannot resume"}")
                else -> delay(RESUME_POLL_MS)
            }
        }
        throw ApiException(408, "Timed out waiting for the sandbox to resume.")
    }

    // -------------------------------------------------------------------- events

    /**
     * Incrementally syncs events: fetches everything at or after the newest cached event
     * (timestamps are inclusive, so overlaps are de-duplicated by primary key) and follows pagination.
     * Returns the number of new events stored.
     */
    suspend fun syncEvents(conversationId: String, full: Boolean = false): Result<Int> = io {
        val since = if (full) null else eventDao.latestForConversation(conversationId)?.timestampIso
        var pageId: String? = null
        var stored = 0
        val fallbackTs = System.currentTimeMillis()
        do {
            val page = api.searchEvents(
                conversationId = conversationId,
                timestampGte = since,
                sortOrder = "TIMESTAMP",
                pageId = pageId,
                limit = 100
            ).bodyOrThrow()
            val entities = page.items.map { dto ->
                AgentEventEntity.fromDomain(EventMapper.map(dto, conversationId, fallbackTs))
            }
            if (entities.isNotEmpty()) {
                eventDao.upsertAll(entities)
                stored += entities.size
            }
            pageId = page.nextPageId
        } while (pageId != null)
        if (stored > 0) dropDeliveredOptimisticMessages(conversationId)
        stored
    }

    /** Once the server echoes a user MessageEvent, the locally inserted placeholder is redundant. */
    private suspend fun dropDeliveredOptimisticMessages(conversationId: String) {
        val rows = eventDao.snapshotForConversation(conversationId)
        val serverUserBodies = rows
            .filter { it.source == "user" && !it.id.startsWith("local-") }
            .mapNotNull { it.body }
            .toSet()
        rows.filter { it.id.startsWith("local-") && !it.isError && it.body in serverUserBodies }
            .forEach { eventDao.deleteById(it.id) }
    }

    suspend fun countEvents(conversationId: String): Result<Int> = io { api.countEvents(conversationId).bodyOrThrow() }

    // ----------------------------------------------------------------- sandboxes

    suspend fun listSandboxes(): Result<List<Sandbox>> = io {
        val out = mutableListOf<Sandbox>()
        var pageId: String? = null
        do {
            val page = api.searchSandboxes(pageId = pageId, limit = 50).bodyOrThrow()
            out += page.items.map { it.toDomain() }
            pageId = page.nextPageId
        } while (pageId != null && out.size < 500)
        out
    }

    suspend fun pauseSandbox(sandboxId: String): Result<Unit> = io {
        api.pauseSandbox(sandboxId).bodyOrThrow()
        conversationDao.updateSandboxStatusBySandbox(sandboxId, SandboxStatus.PAUSED.name)
    }

    suspend fun resumeSandbox(sandboxId: String): Result<Unit> = io {
        api.resumeSandbox(sandboxId).bodyOrThrow()
        conversationDao.updateSandboxStatusBySandbox(sandboxId, SandboxStatus.STARTING.name)
    }

    suspend fun deleteSandbox(sandboxId: String): Result<Unit> = io {
        api.deleteSandbox(sandboxId, sandboxId).bodyOrThrow()
        conversationDao.updateSandboxStatusBySandbox(sandboxId, SandboxStatus.MISSING.name)
    }

    // ------------------------------------------------------------- files & git

    suspend fun listFiles(conversationId: String, path: String?): Result<List<String>> = io {
        api.listFiles(conversationId, path).bodyOrThrow()
    }

    suspend fun readFile(conversationId: String, filePath: String): Result<String> = io {
        val body = api.readFile(conversationId, filePath).bodyOrThrow().string()
        // The endpoint returns a JSON string literal; unquote it if so.
        if (body.length >= 2 && body.startsWith("\"") && body.endsWith("\"")) {
            ApiClient.moshi.adapter(String::class.java).fromJson(body) ?: body
        } else body
    }

    suspend fun gitChanges(conversationId: String, repoPath: String): Result<List<GitChange>> = io {
        api.gitChanges(conversationId, repoPath).bodyOrThrow().map { GitChange(it.path, it.status) }
    }

    suspend fun gitDiff(conversationId: String, filePath: String): Result<GitDiff> = io {
        val d = api.gitDiff(conversationId, filePath).bodyOrThrow()
        GitDiff(filePath, d.original ?: "", d.modified ?: "")
    }

    /** `provider` is required by the server (422 otherwise): github, gitlab, bitbucket, ... */
    suspend fun searchRepositories(query: String?, provider: String = "github"): Result<List<GitRepository>> = io {
        api.searchRepositories(query = query?.trim()?.ifEmpty { null }, provider = provider, limit = 30).bodyOrThrow().items.map {
            GitRepository(
                fullName = it.fullName,
                provider = it.gitProvider,
                isPublic = it.isPublic,
                mainBranch = it.mainBranch,
                stars = it.stargazersCount,
                pushedAt = IsoTime.toEpochMillis(it.pushedAt)
            )
        }
    }

    suspend fun searchBranches(repository: String, provider: String?, query: String?): Result<List<GitBranch>> = io {
        api.searchBranches(
            repository = repository,
            provider = provider?.ifBlank { null } ?: "github",
            query = query?.trim().orEmpty(),
            limit = 30
        ).bodyOrThrow().items.map { GitBranch(it.name, it.commitSha, it.protected) }
    }

    // ----------------------------------------------------- secrets / profiles / skills

    suspend fun listSecrets(): Result<List<CustomSecret>> = io {
        val out = mutableListOf<CustomSecret>()
        var pageId: String? = null
        do {
            val page = api.searchSecrets(pageId = pageId, limit = 50).bodyOrThrow()
            out += page.items.map { CustomSecret(it.name, it.description) }
            pageId = page.nextPageId
        } while (pageId != null && out.size < 500)
        out
    }

    suspend fun createSecret(name: String, value: String, description: String?): Result<Unit> = io {
        api.createSecret(CreateSecretRequest(name.trim(), value, description?.trim()?.ifEmpty { null })).bodyOrNullOrThrow()
        Unit
    }

    suspend fun deleteSecret(name: String): Result<Unit> = io {
        api.deleteSecret(name).bodyOrNullOrThrow()
        Unit
    }

    suspend fun listLlmProfiles(): Result<List<LlmProfile>> = io {
        val dto = api.llmProfiles().bodyOrThrow()
        dto.profiles.map { LlmProfile(it.name, it.model, it.baseUrl, it.apiKeySet == true, it.name == dto.activeProfile) }
    }

    suspend fun activateLlmProfile(name: String): Result<Unit> = io {
        api.activateLlmProfile(name).bodyOrNullOrThrow()
        Unit
    }

    /** Switches the LLM profile of a running conversation. */
    suspend fun switchConversationProfile(conversationId: String, profileName: String): Result<Unit> = io {
        api.switchProfile(conversationId, SwitchProfileRequest(profileName)).bodyOrThrow()
        Unit
    }

    suspend fun conversationSkills(conversationId: String): Result<List<Skill>> = io {
        api.conversationSkills(conversationId).bodyOrThrow().skills.map {
            Skill(it.name, it.type, it.source, it.triggers ?: emptyList(), it.content)
        }
    }

    suspend fun suggestedTasks(): Result<List<SuggestedTask>> = io {
        api.searchSuggestedTasks(limit = 30).bodyOrThrow().items.map {
            SuggestedTask(it.repo, it.issueNumber, it.title, it.taskType, it.gitProvider)
        }
    }

    // ----------------------------------------------------------------- templates

    suspend fun saveTemplate(template: PromptTemplate) = withContext(Dispatchers.IO) {
        templateDao.upsert(SavedTemplateEntity.fromDomain(template))
    }

    suspend fun deleteTemplate(id: String) = withContext(Dispatchers.IO) { templateDao.delete(id) }

    suspend fun seedTemplatesIfEmpty() = withContext(Dispatchers.IO) {
        if (templateDao.count() > 0) return@withContext
        templateDao.upsertAll(
            listOf(
                SavedTemplateEntity("t1", "Fix a failing test", "Debugging",
                    "Find the cause of the failing test or error in this repository, reproduce it, apply a minimal fix, and make sure the test suite passes.", null),
                SavedTemplateEntity("t2", "Add unit tests", "Testing",
                    "Write unit tests for the recently added code paths. Aim for meaningful coverage of edge cases, not just happy paths.", null),
                SavedTemplateEntity("t3", "Refactor for clarity", "Refactoring",
                    "Review this codebase for duplicated logic and unclear abstractions. Refactor into clean, well-named units without changing public behavior.", null),
                SavedTemplateEntity("t4", "Dependency & security audit", "Security",
                    "Audit dependencies for known vulnerabilities, check for insecure input handling or leaked secrets, and upgrade or patch what you find.", null),
                SavedTemplateEntity("t5", "Set up CI", "DevOps",
                    "Add a GitHub Actions workflow that builds, lints, and runs tests on every push and pull request.", null),
                SavedTemplateEntity("t6", "Write API docs", "Documentation",
                    "Document every exposed endpoint, its request and response payloads, and add curl examples in Markdown.", null)
            )
        )
    }

    suspend fun clearCache() = withContext(Dispatchers.IO) {
        conversationDao.clearAll()
        eventDao.clearAll()
        nextConversationPage = null
    }

    // ------------------------------------------------------------------ helpers

    private suspend fun <T> io(block: suspend () -> T): Result<T> = withContext(Dispatchers.IO) {
        try {
            Result.success(block())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun AppConversationDto.toEntity(now: Long): ConversationEntity = ConversationEntity(
        id = id,
        title = title?.trim().orEmpty(),
        selectedRepository = selectedRepository,
        selectedBranch = selectedBranch,
        gitProvider = gitProvider,
        sandboxId = sandboxId,
        sandboxStatus = SandboxStatus.from(sandboxStatus).name,
        executionStatus = executionStatus,
        createdAt = IsoTime.toEpochMillis(createdAt) ?: now,
        updatedAt = IsoTime.toEpochMillis(updatedAt) ?: IsoTime.toEpochMillis(createdAt) ?: now,
        cost = metrics?.accumulatedCost ?: 0.0,
        promptTokens = metrics?.accumulatedTokenUsage?.promptTokens ?: 0,
        completionTokens = metrics?.accumulatedTokenUsage?.completionTokens ?: 0,
        llmModel = llmModel ?: metrics?.modelName?.takeIf { it != "default" },
        conversationUrl = conversationUrl,
        isPublic = public ?: false,
        parentConversationId = parentConversationId,
        prNumbers = prNumber?.joinToString(",") ?: "",
        trigger = trigger,
        workspacePath = tags?.get("archiveworkspacepath"),
        syncedAt = now
    )

    private fun SandboxDto.toDomain() = Sandbox(
        id = id,
        specId = sandboxSpecId,
        status = SandboxStatus.from(status),
        statusDetail = statusDetail,
        exposedUrls = exposedUrls?.map { it.name to it.url } ?: emptyList(),
        createdAt = IsoTime.toEpochMillis(createdAt) ?: 0L
    )

    companion object {
        const val PREFS_NAME = "openhands_prefs"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_SHOW_NOISE = "show_noise_events"
        private const val PAGE_SIZE = 50
        private const val START_POLL_MS = 2_000L
        private const val START_TIMEOUT_MS = 10 * 60_000L
        private const val RESUME_POLL_MS = 2_000L
        private const val RESUME_TIMEOUT_MS = 3 * 60_000L
    }
}

/** Turns exceptions into text the user can act on. */
fun Throwable.friendlyMessage(): String = when (this) {
    is ApiException -> message ?: "Request failed ($code)"
    is java.net.UnknownHostException -> "Cannot reach the server. Check your connection and the server URL."
    is java.net.SocketTimeoutException -> "The server took too long to respond."
    is javax.net.ssl.SSLException -> "Secure connection failed: ${message ?: "TLS error"}"
    is com.squareup.moshi.JsonDataException -> "Unexpected response format: ${message ?: "parse error"}"
    is IllegalArgumentException -> message ?: "Invalid input"
    else -> message ?: this::class.java.simpleName
}
