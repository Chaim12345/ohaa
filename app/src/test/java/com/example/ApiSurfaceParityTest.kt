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
 * contract (v1.59.1, https://app.all-hands.dev/openapi.json — 211 operations).
 * Every declared endpoint must be covered by exactly one Retrofit method with
 * the right HTTP verb.
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
    fun `all 211 spec operations are present with correct verbs`() {
        val have = surface()
        // Complete surface of the live OpenAPI spec, every operation.
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
            "GET api/v1/config/models/search" to "searchModels",
            "GET alive" to "aliveAliveGet",
            "POST api/accept_tos" to "acceptTos",
            "GET api/admin/feature-flags" to "listFlags",
            "POST api/admin/feature-flags" to "createFlag",
            "GET api/admin/feature-flags/{key}" to "getFlag",
            "PATCH api/admin/feature-flags/{key}" to "updateFlag",
            "DELETE api/admin/feature-flags/{key}" to "deleteFlag",
            "POST api/admin/feature-flags/{key}/evaluate" to "evaluateFlag",
            "POST api/admin/feature-flags/{key}/rules" to "createRule",
            "DELETE api/admin/feature-flags/{key}/rules/{rule_id}" to "deleteRule",
            "PUT api/admin/quota/orgs/{org_id}/quota" to "setOrgQuota",
            "GET api/admin/quota/requests" to "listPendingQuotaRequests",
            "POST api/admin/quota/requests/{request_id}/approve" to "approveQuotaRequest",
            "POST api/admin/quota/requests/{request_id}/reject" to "rejectQuotaRequest",
            "GET api/admin/super-admins" to "listSuperAdmins",
            "POST api/admin/super-admins" to "grantSuperAdmin",
            "DELETE api/admin/super-admins/{user_id}" to "revokeSuperAdmin",
            "DELETE api/admin/users/{user_id}" to "deleteUser",
            "POST api/admin/users/{user_id}/disable" to "disableUser",
            "POST api/admin/users/{user_id}/enable" to "enableUser",
            "GET api/admin/verified-models" to "searchVerifiedModels",
            "POST api/admin/verified-models" to "createVerifiedModel",
            "PUT api/admin/verified-models/{provider}/{model_name}" to "updateVerifiedModel",
            "DELETE api/admin/verified-models/{provider}/{model_name}" to "deleteVerifiedModel",
            "GET api/agent-profiles" to "listAgentProfiles",
            "GET api/agent-profiles/{name}" to "getAgentProfile",
            "POST api/agent-profiles/{name}" to "saveAgentProfile",
            "DELETE api/agent-profiles/{name}" to "deleteAgentProfile",
            "POST api/agent-profiles/{name}/materialize" to "materializeAgentProfile",
            "POST api/agent-profiles/{name}/rename" to "renameAgentProfile",
            "POST api/agent-profiles/{profile_id}/activate" to "activateAgentProfile",
            "POST api/analytics/events" to "trackFrontendEvent",
            "POST api/authenticate" to "authenticate",
            "GET api/billing/cancel" to "cancelCallback",
            "POST api/billing/create-checkout-session" to "createCheckoutSession",
            "POST api/billing/create-customer-setup-session" to "createCustomerSetupSession",
            "POST api/billing/has-payment-method" to "hasPaymentMethod",
            "GET api/billing/subscription-access" to "getSubscriptionAccess",
            "GET api/billing/success" to "successCallback",
            "POST api/complete_onboarding" to "completeOnboarding",
            "POST api/email" to "updateEmail",
            "PUT api/email/resend" to "resendEmailVerification",
            "GET api/email/verified" to "verifiedEmail",
            "GET api/keys" to "list",
            "POST api/keys" to "create",
            "GET api/keys/llm/byor" to "getLlm",
            "GET api/keys/llm/byor/permitted" to "checkByorPermitted",
            "POST api/keys/llm/byor/refresh" to "refreshLlm",
            "POST api/keys/llm/managed/refresh" to "refreshManagedLlm",
            "DELETE api/keys/{key_id}" to "delete",
            "POST api/logout" to "logout",
            "GET api/onboarding_status" to "onboardingStatus",
            "GET api/organizations" to "listUserOrgs",
            "POST api/organizations" to "createOrg",
            "GET api/organizations/app" to "getOrgAppSettings",
            "POST api/organizations/app" to "updateOrgAppSettings",
            "GET api/organizations/llm" to "getLegacyOrgDefaultsSettings",
            "POST api/organizations/llm" to "updateLegacyOrgDefaultsSettings",
            "GET api/organizations/members/invite/accept" to "acceptInvitationRedirect",
            "POST api/organizations/members/invite/accept" to "acceptInvitation",
            "GET api/organizations/{org_id}" to "getOrg",
            "DELETE api/organizations/{org_id}" to "deleteOrg",
            "PATCH api/organizations/{org_id}" to "updateOrg",
            "GET api/organizations/{org_id}/budgets" to "getOrgBudgetSettings",
            "PATCH api/organizations/{org_id}/budgets" to "updateOrgBudgetSettings",
            "PUT api/organizations/{org_id}/budgets/overrides/{user_id}" to "upsertOrgBudgetOverride",
            "DELETE api/organizations/{org_id}/budgets/overrides/{user_id}" to "deleteOrgBudgetOverride",
            "GET api/organizations/{org_id}/conversations" to "listOrgConversations",
            "GET api/organizations/{org_id}/conversations/export" to "exportOrgConversationsCsv",
            "GET api/organizations/{org_id}/conversations/stats" to "getOrgConversationStats",
            "GET api/organizations/{org_id}/conversations/usage-stats" to "getOrgConversationUsageStats",
            "GET api/organizations/{org_id}/conversations/user-usage" to "getOrgConversationUserUsageStats",
            "GET api/organizations/{org_id}/conversations/{conversation_id}" to "getOrgConversation",
            "POST api/organizations/{org_id}/conversations/{conversation_id}/stop" to "stopOrgConversation",
            "GET api/organizations/{org_id}/git-claims" to "getGitClaims",
            "POST api/organizations/{org_id}/git-claims" to "claimGitOrganization",
            "DELETE api/organizations/{org_id}/git-claims/{claim_id}" to "disconnectGitOrganization",
            "GET api/organizations/{org_id}/me" to "getMe",
            "GET api/organizations/{org_id}/members" to "getOrgMembers",
            "GET api/organizations/{org_id}/members/count" to "getOrgMembersCount",
            "GET api/organizations/{org_id}/members/financial" to "getOrgMembersFinancial",
            "POST api/organizations/{org_id}/members/invite" to "createInvitation",
            "GET api/organizations/{org_id}/members/invite" to "listPendingInvitations",
            "DELETE api/organizations/{org_id}/members/invite/{invitation_id}" to "revokeInvitation",
            "GET api/organizations/{org_id}/members/{user_id}" to "getOrgMember",
            "DELETE api/organizations/{org_id}/members/{user_id}" to "removeOrgMember",
            "PATCH api/organizations/{org_id}/members/{user_id}" to "updateOrgMember",
            "GET api/organizations/{org_id}/profiles" to "orgListProfiles",
            "GET api/organizations/{org_id}/profiles/{name}" to "orgGetProfile",
            "POST api/organizations/{org_id}/profiles/{name}" to "orgSaveProfile",
            "DELETE api/organizations/{org_id}/profiles/{name}" to "orgDeleteProfile",
            "POST api/organizations/{org_id}/profiles/{name}/activate" to "orgActivateProfile",
            "POST api/organizations/{org_id}/profiles/{name}/rename" to "orgRenameProfile",
            "GET api/organizations/{org_id}/provider-connections" to "listProviderConnections",
            "POST api/organizations/{org_id}/provider-connections" to "createProviderConnection",
            "PATCH api/organizations/{org_id}/provider-connections/{connection_id}" to "updateProviderConnection",
            "DELETE api/organizations/{org_id}/provider-connections/{connection_id}" to "deleteProviderConnection",
            "GET api/organizations/{org_id}/settings" to "getOrgDefaultsSettings",
            "PATCH api/organizations/{org_id}/settings" to "updateOrgDefaultsSettings",
            "POST api/organizations/{org_id}/switch" to "switchOrg",
            "POST api/quota/increase-request" to "createQuotaIncreaseRequest",
            "GET api/quota/status" to "getQuotaStatus",
            "GET api/quota/verify" to "verifyQuotaIncrease",
            "GET api/refresh-tokens" to "refreshTokens",
            "POST api/service/users/{user_id}/orgs/{org_id}/api-keys" to "getOrCreate",
            "DELETE api/service/users/{user_id}/orgs/{org_id}/api-keys/{key_name}" to "deleteUser2",
            "GET api/shared-conversations" to "batchGetSharedConversations",
            "GET api/shared-events" to "batchGetSharedEvents",
            "GET api/shared-events/count" to "countSharedEvents",
            "GET api/shared-events/search" to "searchSharedEvents",
            "GET api/shared-events/{conversation_id}/{event_id}" to "getSharedEvent",
            "GET api/users/app" to "getUserAppSettings",
            "POST api/users/app" to "updateUserAppSettings",
            "GET api/v1/sandboxes/{sandbox_id}/settings/secrets/{secret_name}" to "getSecretValue",
            "POST api/v1/webhooks/conversations" to "onConversationUpdate",
            "POST api/v1/webhooks/events/{conversation_id}" to "onEvent",
            "GET api/v1/webhooks/secrets" to "getSecret",
            "GET health" to "healthHealthGet",
            "POST integration/bitbucket/events" to "bitbucketEvents",
            "POST integration/github/events" to "githubEvents",
            "POST integration/gitlab/events" to "gitlabEvents",
            "POST integration/gitlab/reinstall-webhook" to "reinstallGitlabWebhook",
            "GET integration/gitlab/resources" to "getGitlabResources",
            "GET integration/jira/callback" to "jiraCallback",
            "POST integration/jira/events" to "jiraEvents",
            "POST integration/jira/workspaces" to "createJiraWorkspace",
            "GET integration/jira/workspaces/link" to "getCurrentWorkspaceLink",
            "POST integration/jira/workspaces/link" to "createWorkspaceLink",
            "GET integration/jira/workspaces/status" to "getJiraInstanceStatus",
            "POST integration/jira/workspaces/unlink" to "unlinkWorkspace",
            "GET integration/jira/workspaces/validate/{workspace_name}" to "validateWorkspace",
            "POST oauth/device/authorize" to "deviceAuthorization",
            "POST oauth/device/token" to "deviceToken",
            "POST oauth/device/verify-authenticated" to "deviceVerificationAuthenticated",
            "GET oauth/github/callback" to "githubDummyCallback",
            "GET oauth/keycloak/callback" to "keycloakCallback",
            "GET oauth/keycloak/offline/callback" to "keycloakOfflineCallback",
            "GET ready" to "isReadyReadyGet",
            "GET saas" to "isSaasSaasGet",
            "GET server_info" to "getServerInfoServerInfoGet",
            "GET slack/install" to "install",
            "GET slack/install-callback" to "installCallback",
            "GET slack/keycloak-callback" to "keycloakCallback2",
            "POST slack/on-event" to "onEvent2",
            "POST slack/on-form-interaction" to "onFormInteraction",
            "POST slack/on-options-load" to "onOptionsLoad",
        )
        assertEquals("surface size mismatch", 211, expected.size)
        assertEquals("service declares extra or missing endpoints", expected.size, have.size)
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

    @Test
    fun `oauth device endpoints are form encoded`() {
        val method = OpenHandsApiService::class.java.declaredMethods
            .first { it.name == "deviceToken" }
        assertTrue(method.getAnnotation(retrofit2.http.FormUrlEncoded::class.java) != null)
    }
}
