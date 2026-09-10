package com.example

import com.example.data.api.OpenHandsApiService
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Streaming
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType

/**
 * Guards the typed API surface against the live OpenHands Cloud OpenAPI
 * contract. Every mobile-relevant path from
 * https://app.all-hands.dev/openapi.json must be covered by exactly one
 * Retrofit method with the right HTTP verb.
 */
class ApiSurfaceParityTest {

    /** method name → "VERB path" resolved from the Retrofit annotations. */
    private fun surface(): Map<String, String> {
        val out = LinkedHashMap<String, String>()
        for (m in OpenHandsApiService::class.java.declaredMethods) {
            val annotations = listOfNotNull(
                m.getAnnotation(GET::class.java)?.let { "GET" to it.value },
                m.getAnnotation(POST::class.java)?.let { "POST" to it.value },
                m.getAnnotation(PUT::class.java)?.let { "PUT" to it.value },
                m.getAnnotation(PATCH::class.java)?.let { "PATCH" to it.value },
                m.getAnnotation(DELETE::class.java)?.let { "DELETE" to it.value }
            )
            if (annotations.isEmpty()) continue
            val (verb, path) = annotations.first()
            out[m.name] = "$verb $path"
        }
        return out
    }

    @Test
    fun `all cloud-v1 mobile surface endpoints are present`() {
        val have = surface()
        // The complete mobile-relevant surface of the cloud API (66 operations).
        val expected = listOf(
            "GET api/service/health" to "health",
            "POST api/v1/app-conversations" to "startConversation",
            "POST api/v1/app-conversations/stream-start" to "streamStartConversation",
            "GET api/v1/app-conversations/start-tasks" to "getStartTasks",
            "GET api/v1/app-conversations/start-tasks/search" to "searchStartTasks",
            "GET api/v1/app-conversations/start-tasks/count" to "countStartTasks",
            "GET api/v1/app-conversations" to "getConversations",
            "GET api/v1/app-conversations/search" to "searchConversations",
            "GET api/v1/app-conversations/count" to "countConversations",
            "PATCH api/v1/app-conversations/{conversation_id}" to "updateConversation",
            "DELETE api/v1/app-conversations/{conversation_id}" to "deleteConversation",
            "POST api/v1/app-conversations/{conversation_id}/send-message" to "sendMessage",
            "POST api/v1/conversations/{conversation_id}/pending-messages" to "queuePendingMessage",
            "GET api/v1/app-conversations/{conversation_id}/files" to "listFiles",
            "GET api/v1/app-conversations/{conversation_id}/file" to "readFile",
            "GET api/v1/app-conversations/{conversation_id}/git/changes" to "gitChanges",
            "GET api/v1/app-conversations/{conversation_id}/git/diff" to "gitDiff",
            "GET api/v1/app-conversations/{conversation_id}/download" to "downloadTrajectory",
            "GET api/v1/app-conversations/{conversation_id}/skills" to "conversationSkills",
            "GET api/v1/app-conversations/{conversation_id}/hooks" to "conversationHooks",
            "POST api/v1/app-conversations/{conversation_id}/switch_profile" to "switchProfile",
            "POST api/v1/app-conversations/{conversation_id}/switch_acp_model" to "switchAcpModel",
            "GET api/v1/conversation/{conversation_id}/events/search" to "searchEvents",
            "GET api/v1/conversation/{conversation_id}/events/count" to "countEvents",
            "GET api/v1/conversation/{conversation_id}/events" to "getEvents",
            "GET api/v1/sandboxes" to "getSandboxes",
            "GET api/v1/sandboxes/search" to "searchSandboxes",
            "POST api/v1/sandboxes" to "createSandbox",
            "POST api/v1/sandboxes/{sandbox_id}/pause" to "pauseSandbox",
            "POST api/v1/sandboxes/{sandbox_id}/resume" to "resumeSandbox",
            "DELETE api/v1/sandboxes/{id}" to "deleteSandbox",
            "GET api/v1/sandbox-specs/search" to "searchSandboxSpecs",
            "GET api/v1/sandbox-specs" to "getSandboxSpecs",
            "GET api/v1/sandboxes/{sandbox_id}/settings/secrets" to "sandboxSecretNames",
            "GET api/v1/git/repositories/search" to "searchRepositories",
            "GET api/v1/git/branches/search" to "searchBranches",
            "GET api/v1/git/suggested-tasks/search" to "searchSuggestedTasks",
            "GET api/v1/git/installations/search" to "searchInstallations",
            "GET api/v1/users/git-organizations" to "gitOrganizations",
            "DELETE api/v1/users/git-providers/{provider}" to "disconnectGitProvider",
            "GET api/v1/secrets/search" to "searchSecrets",
            "POST api/v1/secrets" to "createSecret",
            "PUT api/v1/secrets/{secret_id}" to "updateSecret",
            "DELETE api/v1/secrets/{secret_id}" to "deleteSecret",
            "POST api/v1/secrets/git-providers" to "setGitProviderTokens",
            "DELETE api/v1/secrets/git-providers" to "clearGitProviderTokens",
            "GET api/v1/settings" to "settings",
            "POST api/v1/settings" to "storeSettings",
            "GET api/v1/settings/agent-schema" to "agentSettingsSchema",
            "GET api/v1/settings/conversation-schema" to "conversationSettingsSchema",
            "GET api/v1/settings/profiles" to "listProfiles",
            "GET api/v1/settings/profiles/{name}" to "getProfile",
            "POST api/v1/settings/profiles/{name}" to "saveProfile",
            "DELETE api/v1/settings/profiles/{name}" to "deleteProfile",
            "POST api/v1/settings/profiles/{name}/activate" to "activateProfile",
            "POST api/v1/settings/profiles/{name}/rename" to "renameProfile",
            "GET api/v1/skills/search" to "searchSkills",
            "POST api/v1/skills/marketplace-skills" to "marketplaceSkills",
            "GET api/v1/config/providers/search" to "searchProviders",
            "GET api/v1/web-client/config" to "webClientConfig",
            "GET api/v1/users/me" to "me",
            "GET api/v1/users/git-info" to "gitUser",
            "GET api/keys/current" to "currentApiKey",
            "GET api/billing/credits" to "credits",
            "GET api/v1/config/models/search" to "searchModels"
        )
        assertEquals(expected.size, have.size)
        expected.forEach { (verbPath, method) ->
            assertEquals("surface mismatch for $method", verbPath, have[method])
        }
    }

    @Test
    fun `batch query params are lists not single strings`() {
        // Suspend functions carry a trailing Continuation param, so look up by name.
        val method = OpenHandsApiService::class.java.declaredMethods
            .first { it.name == "getConversations" }
        val query = method.parameterAnnotations[0]
            .filterIsInstance<retrofit2.http.Query>()
            .first()
        assertEquals("ids", query.value)
        assertTrue(method.parameterTypes[0] == List::class.java)
    }

    @Test
    fun `streaming endpoints are annotated as streaming`() {
        val method = OpenHandsApiService::class.java.declaredMethods
            .first { it.name == "downloadTrajectory" }
        assertTrue(method.getAnnotation(Streaming::class.java) != null)
    }

    @Test
    fun `raw endpoints return ResponseBody`() {
        val method = OpenHandsApiService::class.java.declaredMethods
            .first { it.name == "readFile" }
        // Suspend funs compile to Object; the real return type rides on the
        // Continuation<? super Response<ResponseBody>> parameter, hidden
        // behind a wildcard bound.
        val lastParam = method.genericParameterTypes.last() as ParameterizedType
        val continuationArg = lastParam.actualTypeArguments[0]
        val responseType = when (continuationArg) {
            is java.lang.reflect.WildcardType ->
                continuationArg.lowerBounds.firstOrNull()
                    ?: continuationArg.upperBounds.first()
            else -> continuationArg
        } as ParameterizedType
        // The bound is Response<ResponseBody> — unwrap the Response envelope.
        val unwrapped = responseType.actualTypeArguments[0]
        assertEquals(ResponseBody::class.java, unwrapped)
    }
}
