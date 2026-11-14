package com.example.data.keepalive

import com.example.data.repository.AgentServerRepository
import com.example.data.repository.OpenHandsRepository

/**
 * One keep-alive pass over the active conversations.
 *
 * Separated from the [KeepAliveService] so the decision logic (touch vs.
 * resume) is unit-testable against a real repository and a mock HTTP server.
 *
 * Touching prefers a call that lands on the sandbox's own runtime (the
 * agent-server `git/changes` endpoint) because only runtime activity resets the
 * idle timer; the cloud conversation record is DB-backed and does not. When the
 * conversation has no runtime URL/key yet, it falls back to the cloud proxy.
 */
class KeepAliveRunner(
    private val repository: OpenHandsRepository,
    private val runtimeRepository: AgentServerRepository? = null,
) {

    data class TickResult(val touched: Int, val resumed: Int) {
        val total: Int get() = touched + resumed
    }

    suspend fun tick(): TickResult {
        if (repository.apiKey.isBlank()) return TickResult(0, 0)
        var touched = 0
        var resumed = 0
        for (target in repository.activeSandboxTargets()) {
            val sandboxId = target.sandboxId ?: continue
            if (target.sandboxStatus.equals("PAUSED", ignoreCase = true)) {
                if (repository.resumeSandbox(sandboxId).isSuccess) resumed++
            } else if (touch(target.conversationId, target.hasRuntimeAccess)) {
                touched++
            }
        }
        return TickResult(touched, resumed)
    }

    private suspend fun touch(conversationId: String, hasRuntimeAccess: Boolean): Boolean {
        if (hasRuntimeAccess && runtimeRepository != null) {
            val runtime = runtimeRepository.touchRuntime(conversationId)
            if (runtime.isSuccess) return true
            // Fall through to the cloud proxy only if the runtime is unreachable.
        }
        return repository.keepAlive(conversationId).isSuccess
    }
}
