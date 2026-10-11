package com.example.data.settings

import android.content.Context
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/** LiteLLM-convention default used by OpenHands Cloud (free model). */
const val DEFAULT_OPENHANDS_MODEL: String = "openhands/deepseek-v4.1-flash"

/** The single provider whose models this client offers. */
const val OPENHANDS_PROVIDER: String = "openhands"

/**
 * The LLM the user has selected for a conversation. Always the plain OpenHands
 * agent; the provider is fixed to [OPENHANDS_PROVIDER] and the catalogue is
 * filtered to that provider's free, verified models.
 */
@JsonClass(generateAdapter = true)
data class AgentProfile(
    val model: String = DEFAULT_OPENHANDS_MODEL
) {
    /** Short model name for badges, e.g. "deepseek-v4.1-flash". */
    fun shortLabel(): String = model.substringAfterLast('/').ifBlank { model }

    fun isValid(): Boolean = model.isNotBlank()
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
