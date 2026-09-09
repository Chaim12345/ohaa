package com.example.data.settings

import android.content.Context
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/**
 * Which agent backend drives a conversation.
 *
 * [OPENHANDS] is the default: the standard OpenHands agent that plans and acts
 * by calling an LLM directly (LiteLLM `provider/model` naming).
 *
 * [ACP] is strictly opt-in: the OpenHands SDK `ACPAgent` delegates to an
 * Agent Client Protocol server (Claude Code, Gemini CLI, …) which manages its
 * own LLM and tools — no OpenHands model choice applies in that mode.
 */
enum class AgentKind(val label: String, val blurb: String) {
    OPENHANDS(
        "OpenHands",
        "Default agent — plans, edits, and runs commands with the selected LLM."
    ),
    ACP(
        "ACP agent",
        "Delegates to an ACP server (Claude Code, Gemini CLI…) that manages its own LLM."
    )
}

/** LiteLLM-convention default used by OpenHands Cloud. */
const val DEFAULT_OPENHANDS_MODEL: String = "openhands/claude-sonnet-4-5-20250929"

/** Command spawned by the SDK to bring up an ACP-compatible server. */
const val DEFAULT_ACP_COMMAND: String = "npx -y @agentclientprotocol/claude-agent-acp"

/** Models commonly offered on OpenHands Cloud — suggestions, not a hard limit. */
val SUGGESTED_MODELS: List<String> = listOf(
    "openhands/claude-sonnet-4-5-20250929",
    "openhands/gpt-5",
    "anthropic/claude-opus-4-1-20250805",
    "openai/gpt-4o",
    "o3"
)

/**
 * The agent + LLM combination the user has selected for a conversation.
 * Always defaults to the plain OpenHands agent; ACP is used only when
 * explicitly chosen by the user.
 */
@JsonClass(generateAdapter = true)
data class AgentProfile(
    val kind: AgentKind = AgentKind.OPENHANDS,
    val model: String = DEFAULT_OPENHANDS_MODEL,
    val acpCommand: String = DEFAULT_ACP_COMMAND
) {
    /** Compact label for badges, e.g. "OpenHands · claude-sonnet-4-5" or "ACP · claude-agent-acp". */
    fun shortLabel(): String {
        val tail = when (kind) {
            AgentKind.OPENHANDS -> model.substringAfterLast('/').ifBlank { model }
            AgentKind.ACP -> acpCommand.substringAfterLast('/')
                .removeSuffix("\"").ifBlank { "ACP server" }
        }
        return "${kind.label} · $tail"
    }

    fun isValid(): Boolean = when (kind) {
        AgentKind.OPENHANDS -> model.isNotBlank() && model.contains('/')
        AgentKind.ACP -> acpCommand.isNotBlank()
    }
}

/**
 * Persisted agent/LLM selection:
 * - one global default (applies to new tasks),
 * - optional per-conversation overrides (mid-session switching in the chat).
 */
class AgentProfileStore(context: Context) {

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val profileAdapter = moshi.adapter(AgentProfile::class.java)

    private val prefs =
        context.getSharedPreferences("agent_profile_prefs", Context.MODE_PRIVATE)

    // ---- Global default (used for new conversations) ------------------------

    fun defaultProfile(): AgentProfile =
        prefs.getString(KEY_DEFAULT, null)
            ?.let(profileAdapter::fromJson)
            ?: AgentProfile() // OpenHands agent + default model

    fun saveDefaultProfile(profile: AgentProfile) {
        prefs.edit().putString(KEY_DEFAULT, profileAdapter.toJson(profile)).apply()
    }

    // ---- Per-conversation overrides (mid-session switching) -----------------

    fun profileFor(conversationId: String): AgentProfile =
        prefs.getString(keyFor(conversationId), null)
            ?.let(profileAdapter::fromJson)
            ?: defaultProfile()

    fun setProfileFor(conversationId: String, profile: AgentProfile) {
        prefs.edit().putString(keyFor(conversationId), profileAdapter.toJson(profile)).apply()
    }

    /** Drops the override so the conversation falls back to the default profile. */
    fun clearProfileFor(conversationId: String) {
        prefs.edit().remove(keyFor(conversationId)).apply()
    }

    private fun keyFor(conversationId: String) = "conv_$conversationId"

    private companion object {
        const val KEY_DEFAULT = "default_profile"
    }
}
