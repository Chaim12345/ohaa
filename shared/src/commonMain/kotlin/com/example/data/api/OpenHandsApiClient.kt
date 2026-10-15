package com.example.data.api

import com.example.data.model.AgentEvent
import com.example.data.model.AppConversationDto
import com.example.data.model.BranchDto
import com.example.data.model.CloudCredits
import com.example.data.model.CreateConversationRequestDto
import com.example.data.model.EventDto
import com.example.data.model.GitChangeDto
import com.example.data.model.GitDiffDto
import com.example.data.model.MessagePayloads
import com.example.data.model.LlmProfileDto
import com.example.data.model.OpenHandsConversation
import com.example.data.model.PageDto
import com.example.data.model.PromptTemplate
import com.example.data.model.RepositoryDto
import com.example.data.model.SecretDto
import com.example.data.model.SendMessageRequest
import com.example.data.model.SkillDto
import com.example.data.model.SkillListDto
import com.example.data.model.StartTaskDto
import com.example.data.model.StatusMapper
import com.example.data.model.SuggestedTaskDto
import com.example.data.model.UserInfoDto
import com.example.util.toEpochMillis
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * The API surface shared by the Android app and the web client.
 *
 * Everything here is a real call against the OpenHands Cloud REST API; there is
 * no demo or stubbed path. The targets differ only in [apiBaseUrl] and the header
 * carrying the key ([authHeaderName]):
 *  - Android talks to `https://app.all-hands.dev` directly with `X-Access-Token`,
 *    because the key is stored on the device and Android has no same-origin
 *    restriction.
 *  - The browser talks to its own origin with `X-OpenHands-Key`, which the Ktor
 *    backend forwards upstream as `Authorization: Bearer …`. That keeps the API
 *    key off the page's network tab and sidesteps CORS entirely.
 */
class OpenHandsApiClient(
    private val apiBaseUrl: String,
    private val apiKey: () -> String,
    /**
     * Wire header carrying the key. Android talks to app.all-hands.dev directly
     * with `X-Access-Token`; the browser build passes `X-OpenHands-Key` so the
     * local proxy can forward it upstream as `Authorization: Bearer …`.
     */
    private val authHeaderName: String = "X-Access-Token",
    private val http: HttpClient = defaultHttpClient()
) {

    private fun io.ktor.client.request.HttpRequestBuilder.auth() {
        val key = apiKey().trim()
        if (key.isNotEmpty()) header(authHeaderName, key)
    }

    private suspend inline fun <reified T> call(block: () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: ResponseException) {
            // Non-2xx: never parse an error body as success, and never surface
            // Ktor's URL-bearing message — status plus a short body excerpt.
            Result.failure(IllegalStateException(httpError(e)))
        } catch (e: Throwable) {
            Result.failure(e)
        }

    /** `HTTP 401: <body>` — first ~200 chars only; no URL, no stack trace. */
    private suspend fun httpError(e: ResponseException): String {
        val body = runCatching { e.response.bodyAsText() }.getOrDefault("").ifEmpty {
            e.message?.substringAfter(". Text: \"", "")?.removeSuffix("\"").orEmpty()
        }
        return "HTTP ${e.response.status.value}: ${body.take(200)}"
    }

    /** GET /api/service/health — used by Settings to test the connection. */
    suspend fun checkHealth(): Result<String> = call {
        http.get("$apiBaseUrl/api/service/health") { auth() }.body()
    }

    /** GET /api/v1/users/me — account identity shown in Settings. */
    suspend fun getUserInfo(): Result<UserInfoDto> = call {
        http.get("$apiBaseUrl/api/v1/users/me") { auth() }.body()
    }

    suspend fun getCredits(): Result<CloudCredits> = call {
        http.get("$apiBaseUrl/api/billing/credits") { auth() }.body()
    }

    /**
     * GET /api/v1/app-conversations/search — the primary listing surface.
     *
     * The bare `/app-conversations` endpoint is a batch fetch that requires an
     * `ids` query parameter, so it cannot list anything on its own.
     */
    suspend fun listConversations(limit: Int = 50): Result<List<OpenHandsConversation>> = call {
        val page: PageDto<AppConversationDto> = http
            .get("$apiBaseUrl/api/v1/app-conversations/search") {
                auth()
                parameter("limit", limit)
            }.body()
        page.items.mapNotNull { it.toDomain() }
    }

    suspend fun searchConversations(query: String, limit: Int = 50): Result<List<OpenHandsConversation>> = call {
        val page: PageDto<AppConversationDto> = http
            .get("$apiBaseUrl/api/v1/app-conversations/search") {
                auth()
                parameter("title__contains", query)
                parameter("limit", limit)
            }.body()
        page.items.mapNotNull { it.toDomain() }
    }

    /** Batch fetch by id; `ids` is repeated. */
    suspend fun getConversation(id: String): Result<AppConversationDto> = call {
        http.get("$apiBaseUrl/api/v1/app-conversations") {
            auth()
            parameter("ids", id)
        }.body<List<AppConversationDto?>>().filterNotNull().first()
    }

    /**
     * GET .../events/search — the transcript for a conversation.
     *
     * The server paginates from the *oldest* event by default, so a long-running
     * conversation would return only its first 100 events and the UI would never
     * show recent activity. Sorting descending and reversing yields the newest
     * [limit] events in chronological order.
     */
    suspend fun searchEvents(
        conversationId: String,
        limit: Int = 100,
        sortOrder: String = "TIMESTAMP_DESC"
    ): Result<List<EventDto>> = call {
        http.get("$apiBaseUrl/api/v1/conversation/$conversationId/events/search") {
            auth()
            parameter("limit", limit)
            parameter("sort_order", sortOrder)
        }.body<PageDto<EventDto>>().items.reversed()
    }

    /** POST /api/v1/app-conversations — starts a task, returns the start-task id. */
    suspend fun startConversation(
        prompt: String,
        repository: String?,
        branch: String? = null,
        llmModel: String? = null
    ): Result<StartTaskDto> = call {
        http.post("$apiBaseUrl/api/v1/app-conversations") {
            auth()
            contentType(ContentType.Application.Json)
            setBody(
                CreateConversationRequestDto(
                    initialMessage = SendMessageRequest.text(prompt),
                    selectedRepository = repository?.trim()?.ifEmpty { null },
                    selectedBranch = branch?.trim()?.ifEmpty { null },
                    llmModel = llmModel?.trim()?.ifEmpty { null }
                )
            )
        }.body()
    }

    /** GET start-tasks — polled after [startConversation] until the sandbox is up. */
    suspend fun getStartTask(id: String): Result<StartTaskDto> = call {
        http.get("$apiBaseUrl/api/v1/app-conversations/start-tasks") {
            auth()
            parameter("ids", id)
        }.body<List<StartTaskDto?>>().filterNotNull()
            .firstOrNull { it.id == id }
            ?: error("Start task $id not found")
    }

    suspend fun sendMessage(conversationId: String, text: String): Result<Unit> = call {
        http.post("$apiBaseUrl/api/v1/app-conversations/$conversationId/send-message") {
            auth()
            contentType(ContentType.Application.Json)
            setBody(SendMessageRequest.text(text))
        }.body<Unit>()
    }

    suspend fun renameConversation(conversationId: String, title: String): Result<Unit> = call {
        http.patch("$apiBaseUrl/api/v1/app-conversations/$conversationId") {
            auth()
            contentType(ContentType.Application.Json)
            setBody(mapOf("title" to title))
        }.body<Unit>()
    }

    suspend fun deleteConversation(conversationId: String): Result<Unit> = call {
        http.delete("$apiBaseUrl/api/v1/app-conversations/$conversationId") { auth() }.body<Unit>()
    }

    /** Workspace files in the conversation's sandbox; null path lists the repo root. */
    suspend fun listFiles(conversationId: String, path: String? = null): Result<List<String>> = call {
        http.get("$apiBaseUrl/api/v1/app-conversations/$conversationId/files") {
            auth()
            path?.let { parameter("path", it) }
        }.body()
    }

    /** Raw contents of one sandbox file. */
    suspend fun readFile(conversationId: String, filePath: String): Result<String> = call {
        http.get("$apiBaseUrl/api/v1/app-conversations/$conversationId/file") {
            auth()
            parameter("file_path", filePath)
        }.body<String>()
    }

    /** Working-tree changes for the repo root inside the sandbox. */
    suspend fun gitChanges(
        conversationId: String,
        repoPath: String = WORKSPACE_DIR,
        ref: String? = null
    ): Result<GitDiffDto> = call {
        val changes = http.get("$apiBaseUrl/api/v1/app-conversations/$conversationId/git/changes") {
            auth()
            parameter("path", repoPath)
            ref?.let { parameter("ref", it) }
        }.body<List<GitChangeDto>>()
        GitDiffDto(files = changes)
    }

    /** Unified diff for a single file. */
    suspend fun gitDiff(
        conversationId: String,
        filePath: String,
        ref: String? = null
    ): Result<GitDiffDto> = call {
        http.get("$apiBaseUrl/api/v1/app-conversations/$conversationId/git/diff") {
            auth()
            parameter("path", filePath)
            ref?.let { parameter("ref", it) }
        }.body()
    }

    /** Skills active in the conversation's sandbox. */
    suspend fun conversationSkills(conversationId: String): Result<List<SkillDto>> = call {
        http.get("$apiBaseUrl/api/v1/app-conversations/$conversationId/skills") { auth() }
            .body<SkillListDto>().skills
    }

    suspend fun pauseSandbox(sandboxId: String): Result<Unit> = call {
        http.post("$apiBaseUrl/api/v1/sandboxes/$sandboxId/pause") { auth() }.body<Unit>()
    }

    suspend fun resumeSandbox(sandboxId: String): Result<Unit> = call {
        http.post("$apiBaseUrl/api/v1/sandboxes/$sandboxId/resume") { auth() }.body<Unit>()
    }

    suspend fun deleteSandbox(sandboxId: String): Result<Unit> = call {
        http.delete("$apiBaseUrl/api/v1/sandboxes/$sandboxId") {
            auth()
            parameter("sandbox_id", sandboxId)
        }.body<Unit>()
    }

    /** `provider` is required by upstream; `query` filters repositories by name. */
    suspend fun searchRepositories(query: String, limit: Int = 20): Result<List<RepositoryDto>> = call {
        http.get("$apiBaseUrl/api/v1/git/repositories/search") {
            auth()
            // The only git provider this client supports (upstream requires the field).
            parameter("provider", "github")
            parameter("query", query)
            parameter("limit", limit)
        }.body<PageDto<RepositoryDto>>().items
    }

    /** `provider` and `query` are required by upstream (`query` may be empty but must be sent). */
    suspend fun searchBranches(repository: String, query: String? = null): Result<List<BranchDto>> = call {
        http.get("$apiBaseUrl/api/v1/git/branches/search") {
            auth()
            parameter("provider", "github")
            parameter("repository", repository)
            parameter("query", query ?: "")
        }.body<PageDto<BranchDto>>().items
    }

    suspend fun suggestedTasks(limit: Int = 30): Result<List<SuggestedTaskDto>> = call {
        http.get("$apiBaseUrl/api/v1/git/suggested-tasks/search") {
            auth()
            parameter("limit", limit)
        }.body<PageDto<SuggestedTaskDto>>().items
    }

    suspend fun listProfiles(): Result<List<LlmProfileDto>> = call {
        http.get("$apiBaseUrl/api/v1/settings/profiles") { auth() }
            .body<PageDto<LlmProfileDto>>().items
    }

    suspend fun listSecrets(): Result<List<SecretDto>> = call {
        http.get("$apiBaseUrl/api/v1/secrets/search") { auth() }
            .body<PageDto<SecretDto>>().items
    }

    /**
     * Templates the composer can seed from.
     *
     * The Cloud API has no template endpoint; the Android app stores user-saved
     * templates in Room. The web client has no local store, so it derives them
     * from the real suggested-task feed instead of shipping canned prompts.
     */
    suspend fun templates(): Result<List<PromptTemplate>> = call {
        suggestedTasks().getOrThrow().map { task ->
            val prompt = buildString {
                append(task.title.ifBlank { "Investigate this task" })
                if (task.issueNumber > 0) {
                    append("\n\nIssue #")
                    append(task.issueNumber)
                    if (task.repo.isNotBlank()) {
                        append(" in ")
                        append(task.repo)
                    }
                }
            }
            PromptTemplate(
                id = "${task.repo}#${task.issueNumber}",
                title = task.title.ifBlank { "Issue #${task.issueNumber}" },
                category = task.repo.ifBlank { "Suggested" },
                prompt = prompt,
                defaultRepo = task.repo.ifBlank { null }
            )
        }
    }

    companion object {
        /** Default repo root inside a conversation's sandbox. */
        const val WORKSPACE_DIR = "/workspace/project"

        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            explicitNulls = false
            encodeDefaults = true
            coerceInputValues = true
        }

        fun defaultHttpClient(): HttpClient = HttpClient {
            // Reject non-2xx before deserialization, so an error body can never
            // be decoded as a success payload.
            expectSuccess = true
            install(ContentNegotiation) { json(json) }
        }
    }
}

/** Wire DTO to the UI-facing domain model. */
fun AppConversationDto.toDomain(): OpenHandsConversation? {
    val conversationId = id ?: return null
    return OpenHandsConversation(
        id = conversationId,
        title = title ?: "Untitled task",
        uiStatus = StatusMapper.map(sandboxStatus, executionStatus, null),
        sandboxStatus = sandboxStatus,
        executionStatus = executionStatus,
        repository = selectedRepository,
        createdAt = createdAt.toEpochMillis(),
        updatedAt = updatedAt.toEpochMillis(),
        cost = metrics?.accumulatedCost ?: 0.0,
        lastMessage = null,
        sandboxId = sandboxId,
        conversationUrl = conversationUrl,
        sessionApiKey = sessionApiKey
    )
}

/**
 * Flattens the heterogeneous event payloads into the single-line transcript
 * model the UI renders. Mirrors the Android repository's mapping so both
 * targets show identical content for the same conversation.
 */
fun EventDto.toAgentEvent(conversationId: String): AgentEvent? {
    val eventId = id ?: return null
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
    val observationRaw = (observation as? JsonObject)
        ?.get("content")
        ?.let { MessagePayloads.contentToText(it) }
    return AgentEvent(
        id = eventId,
        conversationId = conversationId,
        timestamp = timestamp.toEpochMillis(),
        source = source ?: "agent",
        actionType = action,
        content = content,
        detail = observationRaw?.takeIf { it != content },
        isError = isError == true || error?.isNotBlank() == true ||
            (observation as? JsonObject)?.get("is_error")?.jsonPrimitive?.contentOrNull == "true" ||
            !code.isNullOrBlank() || kind == "ConversationErrorEvent"
    )
}
