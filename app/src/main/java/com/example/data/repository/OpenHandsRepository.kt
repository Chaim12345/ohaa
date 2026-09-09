package com.example.data.repository

import com.example.data.api.ApiSettings
import com.example.data.api.OpenHandsApiService
import com.example.data.local.AppDatabase
import com.example.data.local.AgentEventEntity
import com.example.data.local.ConversationEntity
import com.example.data.local.SavedTemplateEntity
import com.example.data.model.AgentEvent
import com.example.data.model.CloudCredits
import com.example.data.model.ConversationDto
import com.example.data.model.CreateConversationRequestDto
import com.example.data.model.EventDto
import com.example.data.model.InitialMessageDto
import com.example.data.model.OpenHandsConversation
import com.example.data.model.PromptTemplate
import com.example.data.model.SendMessageRequestDto
import com.example.data.model.StartTaskDto
import com.example.data.model.TextContentDto
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
        call("health") { api.checkHealth() }.map { body ->
            "Connected — service status: ${body?.status ?: "ok"}"
        }
    }

    suspend fun getCredits(): Result<CloudCredits> = withContext(Dispatchers.IO) {
        call("credits") { api.getCredits() }.map { body -> CloudCredits(body?.credits) }
    }

    // ---- Conversations ---------------------------------------------------------

    suspend fun refreshConversations(@Suppress("UNUSED_PARAMETER") showFeedback: Boolean = true): Result<List<OpenHandsConversation>> =
        withContext(Dispatchers.IO) {
            call("list conversations") { api.listConversations() }.map { dtoList ->
                val entities = dtoList.map { it.toEntity(now()) }
                if (entities.isNotEmpty()) conversationDao.insertConversations(entities)
                entities.map { it.toDomain() }
            }
        }

    suspend fun searchConversationsRemote(query: String): Result<List<OpenHandsConversation>> =
        withContext(Dispatchers.IO) {
            call("search conversations") { api.searchConversations(query) }.map { page ->
                val dtoList = page?.conversations ?: page?.items ?: page?.results ?: emptyList()
                dtoList.map { it.toEntity(now()) }
            }
        }

    /**
     * Launches a task. Returns the conversation id resolved from the start task.
     * Persists an optimistic local row so the detail screen can open immediately.
     */
    suspend fun createConversation(prompt: String, repository: String?): Result<String> =
        withContext(Dispatchers.IO) {
            if (prompt.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Task prompt is empty"))
            }
            val cleanRepo = repository?.trim()?.ifEmpty { null }
            val request = CreateConversationRequestDto(
                initialMessage = InitialMessageDto(
                    content = listOf(TextContentDto(text = prompt))
                ),
                selectedRepository = cleanRepo
            )
            call("create conversation") { api.createConversation(request) }.mapCatching { start ->
                persistStartTask(start, prompt, cleanRepo)
            }
        }

    /** Polls the start task; returns the conversation id once the server reports READY. */
    suspend fun pollStartTask(startTaskId: String): Result<StartTaskDto> =
        withContext(Dispatchers.IO) {
            call("start task") { api.getStartTasks(startTaskId) }.mapCatching { list ->
                list.firstOrNull { it.id == startTaskId || list.size == 1 }
                    ?: throw IllegalStateException("Start task $startTaskId not found")
            }
        }

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
                api.sendMessage(conversationId, SendMessageRequestDto(content = listOf(TextContentDto(text = text))))
            }.map { }
        }

    suspend fun refreshEvents(conversationId: String): Result<List<AgentEvent>> =
        withContext(Dispatchers.IO) {
            call("events") { api.searchEvents(conversationId) }.map { page ->
                val dtos = page?.events ?: page?.items ?: emptyList()
                val entities = dtos.map { it.toEntity(conversationId) }
                if (entities.isNotEmpty()) eventDao.insertEvents(entities)
                entities.map { it.toDomain() }
            }
        }

    suspend fun deleteConversation(conversationId: String): Result<Unit> = withContext(Dispatchers.IO) {
        call("delete conversation") { api.deleteConversation(conversationId) }.map {
            conversationDao.deleteConversation(conversationId)
            eventDao.deleteEventsForConversation(conversationId)
        }
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

    private fun ConversationDto.toEntity(fallbackTime: Long): ConversationEntity = ConversationEntity(
        id = resolvedId.ifBlank { UUID.randomUUID().toString() },
        title = title ?: "Conversation",
        sandboxStatus = sandboxStatus,
        executionStatus = executionStatus,
        rawStatus = status,
        repository = selectedRepository,
        createdAt = parseTimestamp(createdAt) ?: fallbackTime,
        updatedAt = parseTimestamp(updatedAt) ?: parseTimestamp(createdAt) ?: fallbackTime,
        cost = cost ?: 0.0,
        lastMessage = lastMessage
    )

    private fun EventDto.toEntity(conversationId: String): AgentEventEntity {
        val action = action ?: kind?.substringAfterLast('.')
        val content = when {
            !message.isNullOrBlank() -> message!!
            !thought.isNullOrBlank() -> thought!!
            !content.isNullOrBlank() -> content!!
            !observation.isNullOrBlank() -> observation!!
            args?.get("command") is String -> args["command"] as String
            else -> "(no content)"
        }
        return AgentEventEntity(
            id = id ?: UUID.randomUUID().toString(),
            conversationId = conversationId,
            timestamp = parseTimestamp(timestamp) ?: now(),
            source = source ?: "agent",
            actionType = action,
            content = content,
            detail = observation?.takeIf { it != content },
            isError = observation?.contains("error", ignoreCase = true) == true
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

    private inline fun <T : Any> call(label: String, block: () -> retrofit2.Response<T>): Result<T> {
        return try {
            val response = block()
            val body = response.body()
            when {
                response.isSuccessful && body != null -> Result.success(body)
                response.isSuccessful -> Result.failure(
                    CloudError.Api(response.code(), "$label returned an empty body")
                )
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
