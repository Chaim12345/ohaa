package com.example.data.settings

import kotlinx.serialization.Serializable

/** LiteLLM-convention default used by OpenHands Cloud (free model). */
const val DEFAULT_OPENHANDS_MODEL: String = "openhands/deepseek-v4.1-flash"

/** The single provider whose models this client offers. */
const val OPENHANDS_PROVIDER: String = "openhands"

/**
 * The LLM the user has selected for a conversation. Always the plain OpenHands
 * agent; the provider is fixed to [OPENHANDS_PROVIDER] and the catalogue is
 * filtered to that provider's free, verified models.
 */
@Serializable
data class AgentProfile(
    val model: String = DEFAULT_OPENHANDS_MODEL
) {
    /** Short model name for badges, e.g. "deepseek-v4.1-flash". */
    fun shortLabel(): String = model.substringAfterLast('/').ifBlank { model }

    fun isValid(): Boolean = model.isNotBlank()
}
