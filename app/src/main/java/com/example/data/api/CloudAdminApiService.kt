package com.example.data.api

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HEAD
import retrofit2.http.Header
import retrofit2.http.OPTIONS
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Generated, complete bindings for the non-`/api/v1` OpenHands Cloud surface:
 * org administration, service-to-service, keys, billing, analytics, quota,
 * license, SSO/IdP, git-integration and Slack webhooks, and public shares.
 *
 * One method per spec operation, derived from the bundled `cloud.json` spec, so
 * the whole cloud API has a real, typed call site. Bodies are raw
 * [RequestBody] because the spec's schemas are open-ended; responses are raw
 * [ResponseBody] for the same reason. This file is generated — regenerate from
 * the spec rather than editing by hand.
 */
interface CloudAdminApiService {

    @GET("alive")
    suspend fun alive_alive_get(
    ): Response<ResponseBody>

    @POST("api/accept_tos")
    suspend fun accept_tos_api_accept_tos_post(
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @GET("api/admin/directory/users")
    suspend fun list_admin_users_api_admin_directory_users_get(
        @Query("org_id") orgId: String? = null,
    ): Response<ResponseBody>

    @DELETE("api/admin/directory/users/{user_id}")
    suspend fun delete_admin_user_api_admin_directory_users_user_id_delete(
        @Path("user_id") userId: String,
        @Query("org_id") orgId: String? = null,
    ): Response<ResponseBody>

    @PATCH("api/admin/directory/users/{user_id}")
    suspend fun update_admin_user_status_api_admin_directory_users_user_id_patch(
        @Path("user_id") userId: String,
        @Query("org_id") orgId: String? = null,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @POST("api/admin/directory/users/{user_id}/groups")
    suspend fun update_admin_user_groups_api_admin_directory_users_user_id_groups_post(
        @Path("user_id") userId: String,
        @Query("org_id") orgId: String? = null,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @GET("api/admin/feature-flags")
    suspend fun list_flags_api_admin_feature_flags_get(
        @Query("org_id") orgId: String? = null,
    ): Response<ResponseBody>

    @POST("api/admin/feature-flags")
    suspend fun create_flag_api_admin_feature_flags_post(
        @Query("org_id") orgId: String? = null,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @DELETE("api/admin/feature-flags/{key}")
    suspend fun delete_flag_api_admin_feature_flags_key_delete(
        @Path("key") key: String,
        @Query("org_id") orgId: String? = null,
    ): Response<ResponseBody>

    @GET("api/admin/feature-flags/{key}")
    suspend fun get_flag_api_admin_feature_flags_key_get(
        @Path("key") key: String,
        @Query("org_id") orgId: String? = null,
    ): Response<ResponseBody>

    @PATCH("api/admin/feature-flags/{key}")
    suspend fun update_flag_api_admin_feature_flags_key_patch(
        @Path("key") key: String,
        @Query("org_id") orgId: String? = null,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @POST("api/admin/feature-flags/{key}/evaluate")
    suspend fun evaluate_flag_api_admin_feature_flags_key_evaluate_post(
        @Path("key") key: String,
        @Query("org_id") orgId: String? = null,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @POST("api/admin/feature-flags/{key}/rules")
    suspend fun create_rule_api_admin_feature_flags_key_rules_post(
        @Path("key") key: String,
        @Query("org_id") orgId: String? = null,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @DELETE("api/admin/feature-flags/{key}/rules/{rule_id}")
    suspend fun delete_rule_api_admin_feature_flags_key_rules_rule_id_delete(
        @Path("key") key: String,
        @Path("rule_id") ruleId: String,
        @Query("org_id") orgId: String? = null,
    ): Response<ResponseBody>

    @GET("api/admin/instance-settings")
    suspend fun get_instance_settings_api_admin_instance_settings_get(
    ): Response<ResponseBody>

    @PATCH("api/admin/instance-settings")
    suspend fun update_instance_settings_api_admin_instance_settings_patch(
        @Query("org_id") orgId: String? = null,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @GET("api/admin/oauth-providers")
    suspend fun list_oauth_providers_api_admin_oauth_providers_get(
        @Query("org_id") orgId: String? = null,
    ): Response<ResponseBody>

    @POST("api/admin/oauth-providers")
    suspend fun create_oauth_provider_api_admin_oauth_providers_post(
        @Query("org_id") orgId: String? = null,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @POST("api/admin/oauth-providers/saml-metadata-preview")
    suspend fun preview_saml_metadata_api_admin_oauth_providers_saml_metadata_preview_post(
        @Query("org_id") orgId: String? = null,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @DELETE("api/admin/oauth-providers/{provider_id}")
    suspend fun delete_oauth_provider_api_admin_oauth_providers_provider_id_delete(
        @Path("provider_id") providerId: String,
        @Query("org_id") orgId: String? = null,
    ): Response<ResponseBody>

    @GET("api/admin/oauth-providers/{provider_id}")
    suspend fun get_oauth_provider_api_admin_oauth_providers_provider_id_get(
        @Path("provider_id") providerId: String,
        @Query("org_id") orgId: String? = null,
    ): Response<ResponseBody>

    @PATCH("api/admin/oauth-providers/{provider_id}")
    suspend fun update_oauth_provider_api_admin_oauth_providers_provider_id_patch(
        @Path("provider_id") providerId: String,
        @Query("org_id") orgId: String? = null,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @GET("api/admin/oauth-providers/{provider_id}/users")
    suspend fun list_auth_provider_users_api_admin_oauth_providers_provider_id_users_get(
        @Path("provider_id") providerId: String,
        @Query("page_id") pageId: String? = null,
        @Query("limit") limit: String? = null,
        @Query("q") q: String? = null,
        @Query("org_id") orgId: String? = null,
    ): Response<ResponseBody>

    @PATCH("api/admin/oauth-providers/{provider_id}/users/{link_id}")
    suspend fun update_auth_provider_user_api_admin_oauth_providers_provider_id_users_link_id_patch(
        @Path("provider_id") providerId: String,
        @Path("link_id") linkId: String,
        @Query("org_id") orgId: String? = null,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @GET("api/admin/organizations")
    suspend fun list_admin_organizations_api_admin_organizations_get(
        @Query("org_id") orgId: String? = null,
    ): Response<ResponseBody>

    @DELETE("api/admin/organizations/{org_id}")
    suspend fun delete_admin_organization_api_admin_organizations_org_id_delete(
        @Path("org_id") orgId: String,
    ): Response<ResponseBody>

    @PATCH("api/admin/organizations/{org_id}")
    suspend fun update_admin_organization_status_api_admin_organizations_org_id_patch(
        @Path("org_id") orgId: String,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @PUT("api/admin/quota/orgs/{org_id}/quota")
    suspend fun set_org_quota_api_admin_quota_orgs_org_id_quota_put(
        @Path("org_id") orgId: String,
        @Header("X-Org-Id") xOrgId: String? = null,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @GET("api/admin/quota/requests")
    suspend fun list_pending_quota_requests_api_admin_quota_requests_get(
        @Query("org_id") orgId: String? = null,
        @Header("X-Org-Id") xOrgId: String? = null,
    ): Response<ResponseBody>

    @POST("api/admin/quota/requests/{request_id}/approve")
    suspend fun approve_quota_request_api_admin_quota_requests_request_id_approve_post(
        @Path("request_id") requestId: String,
        @Query("org_id") orgId: String? = null,
        @Header("X-Org-Id") xOrgId: String? = null,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @POST("api/admin/quota/requests/{request_id}/reject")
    suspend fun reject_quota_request_api_admin_quota_requests_request_id_reject_post(
        @Path("request_id") requestId: String,
        @Query("org_id") orgId: String? = null,
        @Header("X-Org-Id") xOrgId: String? = null,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @PUT("api/admin/quota/users/{user_id}/quota")
    suspend fun set_user_quota_api_admin_quota_users_user_id_quota_put(
        @Path("user_id") userId: String,
        @Query("org_id") orgId: String? = null,
        @Header("X-Org-Id") xOrgId: String? = null,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @GET("api/admin/setup-state")
    suspend fun get_setup_state_api_admin_setup_state_get(
    ): Response<ResponseBody>

    @PATCH("api/admin/setup-state")
    suspend fun update_setup_state_api_admin_setup_state_patch(
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @GET("api/admin/super-admins")
    suspend fun list_super_admins_api_admin_super_admins_get(
    ): Response<ResponseBody>

    @POST("api/admin/super-admins")
    suspend fun grant_super_admin_api_admin_super_admins_post(
        @Query("org_id") orgId: String? = null,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @GET("api/admin/super-admins/me")
    suspend fun get_my_super_admin_status_api_admin_super_admins_me_get(
    ): Response<ResponseBody>

    @DELETE("api/admin/super-admins/{user_id}")
    suspend fun revoke_super_admin_api_admin_super_admins_user_id_delete(
        @Path("user_id") userId: String,
        @Query("org_id") orgId: String? = null,
    ): Response<ResponseBody>

    @GET("api/admin/users")
    suspend fun list_all_users_api_admin_users_get(
        @Query("page_id") pageId: String? = null,
        @Query("limit") limit: String? = null,
        @Query("email") email: String? = null,
        @Query("org_id") orgId: String? = null,
    ): Response<ResponseBody>

    @GET("api/admin/users/count")
    suspend fun count_all_users_api_admin_users_count_get(
        @Query("email") email: String? = null,
        @Query("org_id") orgId: String? = null,
    ): Response<ResponseBody>

    @DELETE("api/admin/users/{user_id}")
    suspend fun delete_user_api_admin_users_user_id_delete(
        @Path("user_id") userId: String,
        @Query("org_id") orgId: String? = null,
    ): Response<ResponseBody>

    @POST("api/admin/users/{user_id}/disable")
    suspend fun disable_user_api_admin_users_user_id_disable_post(
        @Path("user_id") userId: String,
        @Query("org_id") orgId: String? = null,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @POST("api/admin/users/{user_id}/enable")
    suspend fun enable_user_api_admin_users_user_id_enable_post(
        @Path("user_id") userId: String,
        @Query("org_id") orgId: String? = null,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @GET("api/admin/verified-models")
    suspend fun search_verified_models_api_admin_verified_models_get(
        @Query("provider") provider: String? = null,
        @Query("page_id") pageId: String? = null,
        @Query("limit") limit: String? = null,
    ): Response<ResponseBody>

    @POST("api/admin/verified-models")
    suspend fun create_verified_model_api_admin_verified_models_post(
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @DELETE("api/admin/verified-models/{provider}/{model_name}")
    suspend fun delete_verified_model_api_admin_verified_models_provider_model_name_delete(
        @Path("provider") provider: String,
        @Path("model_name") modelName: String,
    ): Response<ResponseBody>

    @PUT("api/admin/verified-models/{provider}/{model_name}")
    suspend fun update_verified_model_api_admin_verified_models_provider_model_name_put(
        @Path("provider") provider: String,
        @Path("model_name") modelName: String,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @GET("api/agent-profiles")
    suspend fun list_agent_profiles_api_agent_profiles_get(
        @Query("org_id") orgId: String? = null,
    ): Response<ResponseBody>

    @DELETE("api/agent-profiles/{name}")
    suspend fun delete_agent_profile_api_agent_profiles_name_delete(
        @Path("name") name: String,
        @Query("scope") scope: String? = null,
    ): Response<ResponseBody>

    @GET("api/agent-profiles/{name}")
    suspend fun get_agent_profile_api_agent_profiles_name_get(
        @Path("name") name: String,
        @Query("scope") scope: String? = null,
        @Query("org_id") orgId: String? = null,
    ): Response<ResponseBody>

    @POST("api/agent-profiles/{name}")
    suspend fun save_agent_profile_api_agent_profiles_name_post(
        @Path("name") name: String,
        @Query("scope") scope: String? = null,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @POST("api/agent-profiles/{name}/materialize")
    suspend fun materialize_agent_profile_api_agent_profiles_name_materialize_post(
        @Path("name") name: String,
        @Query("scope") scope: String? = null,
        @Query("org_id") orgId: String? = null,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @POST("api/agent-profiles/{name}/rename")
    suspend fun rename_agent_profile_api_agent_profiles_name_rename_post(
        @Path("name") name: String,
        @Query("scope") scope: String? = null,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @POST("api/agent-profiles/{profile_id}/activate")
    suspend fun activate_agent_profile_api_agent_profiles_profile_id_activate_post(
        @Path("profile_id") profileId: String,
        @Query("org_id") orgId: String? = null,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @POST("api/agent-profiles/{profile_id}/deactivate")
    suspend fun deactivate_agent_profile_api_agent_profiles_profile_id_deactivate_post(
        @Path("profile_id") profileId: String,
        @Query("org_id") orgId: String? = null,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @POST("api/analytics/events")
    suspend fun track_frontend_event_api_analytics_events_post(
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @POST("api/authenticate")
    suspend fun authenticate_api_authenticate_post(
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @GET("api/billing/cancel")
    suspend fun cancel_callback_api_billing_cancel_get(
        @Query("session_id") sessionId: String? = null,
    ): Response<ResponseBody>

    @POST("api/billing/create-checkout-session")
    suspend fun create_checkout_session_api_billing_create_checkout_session_post(
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @POST("api/billing/create-customer-setup-session")
    suspend fun create_customer_setup_session_api_billing_create_customer_setup_session_post(
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @GET("api/billing/credits")
    suspend fun get_credits_api_billing_credits_get(
    ): Response<ResponseBody>

    @POST("api/billing/has-payment-method")
    suspend fun has_payment_method_api_billing_has_payment_method_post(
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @GET("api/billing/subscription-access")
    suspend fun get_subscription_access_api_billing_subscription_access_get(
    ): Response<ResponseBody>

    @GET("api/billing/success")
    suspend fun success_callback_api_billing_success_get(
        @Query("session_id") sessionId: String? = null,
    ): Response<ResponseBody>

    @POST("api/complete_onboarding")
    suspend fun complete_onboarding_api_complete_onboarding_post(
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @POST("api/email")
    suspend fun update_email_api_email_post(
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @PUT("api/email/resend")
    suspend fun resend_email_verification_api_email_resend_put(
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @GET("api/email/verified")
    suspend fun verified_email_api_email_verified_get(
    ): Response<ResponseBody>

    @GET("api/idp/password")
    suspend fun idp_has_password_api_idp_password_get(
    ): Response<ResponseBody>

    @POST("api/idp/password")
    suspend fun idp_set_password_api_idp_password_post(
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @POST("api/idp/signup-links")
    suspend fun create_signup_link_api_idp_signup_links_post(
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @GET("api/idp/status")
    suspend fun idp_status_api_idp_status_get(
    ): Response<ResponseBody>

    @POST("api/keycloak/link-context")
    suspend fun keycloak_link_context_api_keycloak_link_context_post(
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @GET("api/keys")
    suspend fun list_api_keys_api_keys_get(
    ): Response<ResponseBody>

    @POST("api/keys")
    suspend fun create_api_key_api_keys_post(
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @GET("api/keys/current")
    suspend fun get_current_api_key_api_keys_current_get(
    ): Response<ResponseBody>

    @GET("api/keys/llm/byor")
    suspend fun get_llm_api_key_for_byor_api_keys_llm_byor_get(
    ): Response<ResponseBody>

    @GET("api/keys/llm/byor/permitted")
    suspend fun check_byor_permitted_api_keys_llm_byor_permitted_get(
    ): Response<ResponseBody>

    @POST("api/keys/llm/byor/refresh")
    suspend fun refresh_llm_api_key_for_byor_api_keys_llm_byor_refresh_post(
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @POST("api/keys/llm/managed/refresh")
    suspend fun refresh_managed_llm_api_key_api_keys_llm_managed_refresh_post(
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @DELETE("api/keys/{key_id}")
    suspend fun delete_api_key_api_keys_key_id_delete(
        @Path("key_id") keyId: String,
    ): Response<ResponseBody>

    @GET("api/license")
    suspend fun get_license_api_license_get(
        @Query("org_id") orgId: String? = null,
    ): Response<ResponseBody>

    @PUT("api/license")
    suspend fun save_license_api_license_put(
        @Query("org_id") orgId: String? = null,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @GET("api/license/status")
    suspend fun get_license_status_api_license_status_get(
    ): Response<ResponseBody>

    @GET("api/license/usage")
    suspend fun get_license_usage_api_license_usage_get(
    ): Response<ResponseBody>

    @POST("api/logout")
    suspend fun logout_api_logout_post(
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @GET("api/onboarding_status")
    suspend fun onboarding_status_api_onboarding_status_get(
    ): Response<ResponseBody>

    @GET("api/organizations")
    suspend fun list_user_orgs_api_organizations_get(
        @Query("page_id") pageId: String? = null,
        @Query("limit") limit: String? = null,
        @Query("name") name: String? = null,
        @Header("X-Org-Id") xOrgId: String? = null,
    ): Response<ResponseBody>

    @POST("api/organizations")
    suspend fun create_org_api_organizations_post(
        @Query("org_id") orgId: String? = null,
        @Header("X-Org-Id") xOrgId: String? = null,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @GET("api/organizations/app")
    suspend fun get_org_app_settings_api_organizations_app_get(
        @Query("org_id") orgId: String? = null,
        @Header("X-Org-Id") xOrgId: String? = null,
    ): Response<ResponseBody>

    @POST("api/organizations/app")
    suspend fun update_org_app_settings_api_organizations_app_post(
        @Query("org_id") orgId: String? = null,
        @Header("X-Org-Id") xOrgId: String? = null,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @GET("api/organizations/llm")
    suspend fun get_legacy_org_defaults_settings_api_organizations_llm_get(
        @Query("org_id") orgId: String? = null,
        @Header("X-Org-Id") xOrgId: String? = null,
    ): Response<ResponseBody>

    @POST("api/organizations/llm")
    suspend fun update_legacy_org_defaults_settings_api_organizations_llm_post(
        @Query("org_id") orgId: String? = null,
        @Header("X-Org-Id") xOrgId: String? = null,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @GET("api/organizations/members/invite/accept")
    suspend fun accept_invitation_redirect_api_organizations_members_invite_accept_get(
        @Query("token") token: String? = null,
    ): Response<ResponseBody>

    @POST("api/organizations/members/invite/accept")
    suspend fun accept_invitation_api_organizations_members_invite_accept_post(
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @DELETE("api/organizations/{org_id}")
    suspend fun delete_org_api_organizations_org_id_delete(
        @Path("org_id") orgId: String,
        @Header("X-Org-Id") xOrgId: String? = null,
    ): Response<ResponseBody>

    @GET("api/organizations/{org_id}")
    suspend fun get_org_api_organizations_org_id_get(
        @Path("org_id") orgId: String,
        @Header("X-Org-Id") xOrgId: String? = null,
    ): Response<ResponseBody>

    @PATCH("api/organizations/{org_id}")
    suspend fun update_org_api_organizations_org_id_patch(
        @Path("org_id") orgId: String,
        @Header("X-Org-Id") xOrgId: String? = null,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @GET("api/organizations/{org_id}/budgets")
    suspend fun get_org_budget_settings_api_organizations_org_id_budgets_get(
        @Path("org_id") orgId: String,
        @Query("users_page") usersPage: String? = null,
        @Query("users_per_page") usersPerPage: String? = null,
        @Query("users_search") usersSearch: String? = null,
        @Query("users_status") usersStatus: String? = null,
        @Header("X-Org-Id") xOrgId: String? = null,
    ): Response<ResponseBody>

    @PATCH("api/organizations/{org_id}/budgets")
    suspend fun update_org_budget_settings_api_organizations_org_id_budgets_patch(
        @Path("org_id") orgId: String,
        @Query("users_page") usersPage: String? = null,
        @Query("users_per_page") usersPerPage: String? = null,
        @Query("users_search") usersSearch: String? = null,
        @Query("users_status") usersStatus: String? = null,
        @Header("X-Org-Id") xOrgId: String? = null,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @GET("api/organizations/{org_id}/budgets/me")
    suspend fun get_my_org_budget_api_organizations_org_id_budgets_me_get(
        @Path("org_id") orgId: String,
        @Query("include_spend") includeSpend: String? = null,
        @Header("X-Org-Id") xOrgId: String? = null,
    ): Response<ResponseBody>

    @DELETE("api/organizations/{org_id}/budgets/overrides/{user_id}")
    suspend fun delete_org_budget_override_api_organizations_org_id_budgets_overrides_user_id_delete(
        @Path("org_id") orgId: String,
        @Path("user_id") userId: String,
        @Header("X-Org-Id") xOrgId: String? = null,
    ): Response<ResponseBody>

    @PUT("api/organizations/{org_id}/budgets/overrides/{user_id}")
    suspend fun upsert_org_budget_override_api_organizations_org_id_budgets_overrides_user_id_put(
        @Path("org_id") orgId: String,
        @Path("user_id") userId: String,
        @Header("X-Org-Id") xOrgId: String? = null,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @GET("api/organizations/{org_id}/conversations")
    suspend fun list_org_conversations_api_organizations_org_id_conversations_get(
        @Path("org_id") orgId: String,
        @Query("search") search: String? = null,
        @Query("sort_by") sortBy: String? = null,
        @Query("sort_order") sortOrder: String? = null,
        @Query("execution_status") executionStatus: String? = null,
        @Query("sandbox_status") sandboxStatus: String? = null,
        @Query("time_window") timeWindow: String? = null,
        @Query("page") page: String? = null,
        @Query("per_page") perPage: String? = null,
        @Query("include_sub_conversations") includeSubConversations: String? = null,
        @Header("X-Org-Id") xOrgId: String? = null,
    ): Response<ResponseBody>

    @GET("api/organizations/{org_id}/conversations/export")
    suspend fun export_org_conversations_csv_api_organizations_org_id_conversations_export_get(
        @Path("org_id") orgId: String,
        @Query("search") search: String? = null,
        @Query("sort_by") sortBy: String? = null,
        @Query("sort_order") sortOrder: String? = null,
        @Query("execution_status") executionStatus: String? = null,
        @Query("sandbox_status") sandboxStatus: String? = null,
        @Query("time_window") timeWindow: String? = null,
        @Query("include_sub_conversations") includeSubConversations: String? = null,
        @Header("X-Org-Id") xOrgId: String? = null,
    ): Response<ResponseBody>

    @GET("api/organizations/{org_id}/conversations/my-usage")
    suspend fun get_my_org_conversation_usage_stats_api_organizations_org_id_conversations_my_usage_get(
        @Path("org_id") orgId: String,
        @Query("time_window") timeWindow: String? = null,
        @Header("X-Org-Id") xOrgId: String? = null,
    ): Response<ResponseBody>

    @GET("api/organizations/{org_id}/conversations/stats")
    suspend fun get_org_conversation_stats_api_organizations_org_id_conversations_stats_get(
        @Path("org_id") orgId: String,
        @Header("X-Org-Id") xOrgId: String? = null,
    ): Response<ResponseBody>

    @GET("api/organizations/{org_id}/conversations/usage-stats")
    suspend fun get_org_conversation_usage_stats_api_organizations_org_id_conversations_usage_stats_get(
        @Path("org_id") orgId: String,
        @Query("time_window") timeWindow: String? = null,
        @Query("days") days: String? = null,
        @Header("X-Org-Id") xOrgId: String? = null,
    ): Response<ResponseBody>

    @GET("api/organizations/{org_id}/conversations/user-usage")
    suspend fun get_org_conversation_user_usage_stats_api_organizations_org_id_conversations_user_usage_get(
        @Path("org_id") orgId: String,
        @Query("limit") limit: String? = null,
        @Query("offset") offset: String? = null,
        @Query("time_window") timeWindow: String? = null,
        @Header("X-Org-Id") xOrgId: String? = null,
    ): Response<ResponseBody>

    @GET("api/organizations/{org_id}/conversations/{conversation_id}")
    suspend fun get_org_conversation_api_organizations_org_id_conversations_conversation_id_get(
        @Path("org_id") orgId: String,
        @Path("conversation_id") conversationId: String,
        @Header("X-Org-Id") xOrgId: String? = null,
    ): Response<ResponseBody>

    @POST("api/organizations/{org_id}/conversations/{conversation_id}/stop")
    suspend fun stop_org_conversation_api_organizations_org_id_conversations_conversation_id_stop_post(
        @Path("org_id") orgId: String,
        @Path("conversation_id") conversationId: String,
        @Header("X-Org-Id") xOrgId: String? = null,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @GET("api/organizations/{org_id}/git-claims")
    suspend fun get_git_claims_api_organizations_org_id_git_claims_get(
        @Path("org_id") orgId: String,
        @Header("X-Org-Id") xOrgId: String? = null,
    ): Response<ResponseBody>

    @POST("api/organizations/{org_id}/git-claims")
    suspend fun claim_git_organization_api_organizations_org_id_git_claims_post(
        @Path("org_id") orgId: String,
        @Header("X-Org-Id") xOrgId: String? = null,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @DELETE("api/organizations/{org_id}/git-claims/{claim_id}")
    suspend fun disconnect_git_organization_api_organizations_org_id_git_claims_claim_id_delete(
        @Path("org_id") orgId: String,
        @Path("claim_id") claimId: String,
        @Header("X-Org-Id") xOrgId: String? = null,
    ): Response<ResponseBody>

    @GET("api/organizations/{org_id}/mcp-servers")
    suspend fun list_org_mcp_servers_api_organizations_org_id_mcp_servers_get(
        @Path("org_id") orgId: String,
    ): Response<ResponseBody>

    @POST("api/organizations/{org_id}/mcp-servers")
    suspend fun create_org_mcp_server_api_organizations_org_id_mcp_servers_post(
        @Path("org_id") orgId: String,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @DELETE("api/organizations/{org_id}/mcp-servers/{name}")
    suspend fun delete_org_mcp_server_api_organizations_org_id_mcp_servers_name_delete(
        @Path("org_id") orgId: String,
        @Path("name") name: String,
    ): Response<ResponseBody>

    @PUT("api/organizations/{org_id}/mcp-servers/{name}")
    suspend fun update_org_mcp_server_api_organizations_org_id_mcp_servers_name_put(
        @Path("org_id") orgId: String,
        @Path("name") name: String,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @DELETE("api/organizations/{org_id}/mcp-servers/{name}/connection")
    suspend fun disconnect_org_mcp_server_api_organizations_org_id_mcp_servers_name_connection_delete(
        @Path("org_id") orgId: String,
        @Path("name") name: String,
    ): Response<ResponseBody>

    @PUT("api/organizations/{org_id}/mcp-servers/{name}/connection")
    suspend fun connect_org_mcp_server_api_organizations_org_id_mcp_servers_name_connection_put(
        @Path("org_id") orgId: String,
        @Path("name") name: String,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @GET("api/organizations/{org_id}/me")
    suspend fun get_me_api_organizations_org_id_me_get(
        @Path("org_id") orgId: String,
        @Header("X-Org-Id") xOrgId: String? = null,
    ): Response<ResponseBody>

    @GET("api/organizations/{org_id}/members")
    suspend fun get_org_members_api_organizations_org_id_members_get(
        @Path("org_id") orgId: String,
        @Query("page_id") pageId: String? = null,
        @Query("limit") limit: String? = null,
        @Query("email") email: String? = null,
        @Header("X-Org-Id") xOrgId: String? = null,
    ): Response<ResponseBody>

    @GET("api/organizations/{org_id}/members/count")
    suspend fun get_org_members_count_api_organizations_org_id_members_count_get(
        @Path("org_id") orgId: String,
        @Query("email") email: String? = null,
        @Header("X-Org-Id") xOrgId: String? = null,
    ): Response<ResponseBody>

    @GET("api/organizations/{org_id}/members/financial")
    suspend fun get_org_members_financial_api_organizations_org_id_members_financial_get(
        @Path("org_id") orgId: String,
        @Query("page_id") pageId: String? = null,
        @Query("limit") limit: String? = null,
        @Query("email") email: String? = null,
        @Header("X-Org-Id") xOrgId: String? = null,
    ): Response<ResponseBody>

    @GET("api/organizations/{org_id}/members/invite")
    suspend fun list_pending_invitations_api_organizations_org_id_members_invite_get(
        @Path("org_id") orgId: String,
        @Header("X-Org-Id") xOrgId: String? = null,
    ): Response<ResponseBody>

    @POST("api/organizations/{org_id}/members/invite")
    suspend fun create_invitation_api_organizations_org_id_members_invite_post(
        @Path("org_id") orgId: String,
        @Header("X-Org-Id") xOrgId: String? = null,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @DELETE("api/organizations/{org_id}/members/invite/{invitation_id}")
    suspend fun revoke_invitation_api_organizations_org_id_members_invite_invitation_id_delete(
        @Path("org_id") orgId: String,
        @Path("invitation_id") invitationId: String,
        @Header("X-Org-Id") xOrgId: String? = null,
    ): Response<ResponseBody>

    @DELETE("api/organizations/{org_id}/members/{user_id}")
    suspend fun remove_org_member_api_organizations_org_id_members_user_id_delete(
        @Path("org_id") orgId: String,
        @Path("user_id") userId: String,
        @Header("X-Org-Id") xOrgId: String? = null,
    ): Response<ResponseBody>

    @GET("api/organizations/{org_id}/members/{user_id}")
    suspend fun get_org_member_api_organizations_org_id_members_user_id_get(
        @Path("org_id") orgId: String,
        @Path("user_id") userId: String,
        @Header("X-Org-Id") xOrgId: String? = null,
    ): Response<ResponseBody>

    @PATCH("api/organizations/{org_id}/members/{user_id}")
    suspend fun update_org_member_api_organizations_org_id_members_user_id_patch(
        @Path("org_id") orgId: String,
        @Path("user_id") userId: String,
        @Header("X-Org-Id") xOrgId: String? = null,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @GET("api/organizations/{org_id}/meta-profiles")
    suspend fun list_meta_profiles_api_organizations_org_id_meta_profiles_get(
        @Path("org_id") orgId: String,
    ): Response<ResponseBody>

    @PATCH("api/organizations/{org_id}/meta-profiles")
    suspend fun update_meta_profile_settings_api_organizations_org_id_meta_profiles_patch(
        @Path("org_id") orgId: String,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @DELETE("api/organizations/{org_id}/meta-profiles/{name}")
    suspend fun delete_meta_profile_api_organizations_org_id_meta_profiles_name_delete(
        @Path("org_id") orgId: String,
        @Path("name") name: String,
    ): Response<ResponseBody>

    @GET("api/organizations/{org_id}/meta-profiles/{name}")
    suspend fun get_meta_profile_api_organizations_org_id_meta_profiles_name_get(
        @Path("org_id") orgId: String,
        @Path("name") name: String,
    ): Response<ResponseBody>

    @POST("api/organizations/{org_id}/meta-profiles/{name}")
    suspend fun save_meta_profile_api_organizations_org_id_meta_profiles_name_post(
        @Path("org_id") orgId: String,
        @Path("name") name: String,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @POST("api/organizations/{org_id}/meta-profiles/{name}/activate")
    suspend fun activate_meta_profile_api_organizations_org_id_meta_profiles_name_activate_post(
        @Path("org_id") orgId: String,
        @Path("name") name: String,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @POST("api/organizations/{org_id}/meta-profiles/{name}/deactivate")
    suspend fun deactivate_meta_profile_api_organizations_org_id_meta_profiles_name_deactivate_post(
        @Path("org_id") orgId: String,
        @Path("name") name: String,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @GET("api/organizations/{org_id}/profiles")
    suspend fun list_profiles_api_organizations_org_id_profiles_get(
        @Path("org_id") orgId: String,
    ): Response<ResponseBody>

    @DELETE("api/organizations/{org_id}/profiles/{name}")
    suspend fun delete_profile_api_organizations_org_id_profiles_name_delete(
        @Path("org_id") orgId: String,
        @Path("name") name: String,
    ): Response<ResponseBody>

    @GET("api/organizations/{org_id}/profiles/{name}")
    suspend fun get_profile_api_organizations_org_id_profiles_name_get(
        @Path("org_id") orgId: String,
        @Path("name") name: String,
    ): Response<ResponseBody>

    @POST("api/organizations/{org_id}/profiles/{name}")
    suspend fun save_profile_api_organizations_org_id_profiles_name_post(
        @Path("org_id") orgId: String,
        @Path("name") name: String,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @POST("api/organizations/{org_id}/profiles/{name}/activate")
    suspend fun activate_profile_api_organizations_org_id_profiles_name_activate_post(
        @Path("org_id") orgId: String,
        @Path("name") name: String,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @POST("api/organizations/{org_id}/profiles/{name}/rename")
    suspend fun rename_profile_api_organizations_org_id_profiles_name_rename_post(
        @Path("org_id") orgId: String,
        @Path("name") name: String,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @GET("api/organizations/{org_id}/provider-connections")
    suspend fun list_provider_connections_api_organizations_org_id_provider_connections_get(
        @Path("org_id") orgId: String,
    ): Response<ResponseBody>

    @POST("api/organizations/{org_id}/provider-connections")
    suspend fun create_provider_connection_api_organizations_org_id_provider_connections_post(
        @Path("org_id") orgId: String,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @DELETE("api/organizations/{org_id}/provider-connections/{connection_id}")
    suspend fun delete_provider_connection_api_organizations_org_id_provider_connections_connection_id_delete(
        @Path("org_id") orgId: String,
        @Path("connection_id") connectionId: String,
    ): Response<ResponseBody>

    @PATCH("api/organizations/{org_id}/provider-connections/{connection_id}")
    suspend fun update_provider_connection_api_organizations_org_id_provider_connections_connection_id_patch(
        @Path("org_id") orgId: String,
        @Path("connection_id") connectionId: String,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @GET("api/organizations/{org_id}/secrets")
    suspend fun list_org_secrets_api_organizations_org_id_secrets_get(
        @Path("org_id") orgId: String,
    ): Response<ResponseBody>

    @POST("api/organizations/{org_id}/secrets")
    suspend fun create_org_secret_api_organizations_org_id_secrets_post(
        @Path("org_id") orgId: String,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @DELETE("api/organizations/{org_id}/secrets/{secret_name}")
    suspend fun delete_org_secret_api_organizations_org_id_secrets_secret_name_delete(
        @Path("org_id") orgId: String,
        @Path("secret_name") secretName: String,
    ): Response<ResponseBody>

    @PUT("api/organizations/{org_id}/secrets/{secret_name}")
    suspend fun update_org_secret_api_organizations_org_id_secrets_secret_name_put(
        @Path("org_id") orgId: String,
        @Path("secret_name") secretName: String,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @PUT("api/organizations/{org_id}/secrets/{secret_name}/value")
    suspend fun replace_org_secret_value_api_organizations_org_id_secrets_secret_name_value_put(
        @Path("org_id") orgId: String,
        @Path("secret_name") secretName: String,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @GET("api/organizations/{org_id}/secrets/{secret_name}/version")
    suspend fun get_org_secret_value_version_api_organizations_org_id_secrets_secret_name_version_get(
        @Path("org_id") orgId: String,
        @Path("secret_name") secretName: String,
    ): Response<ResponseBody>

    @GET("api/organizations/{org_id}/service-accounts")
    suspend fun list_service_accounts_api_organizations_org_id_service_accounts_get(
        @Path("org_id") orgId: String,
        @Query("page_id") pageId: String? = null,
        @Query("limit") limit: String? = null,
        @Header("X-Org-Id") xOrgId: String? = null,
    ): Response<ResponseBody>

    @POST("api/organizations/{org_id}/service-accounts")
    suspend fun create_service_account_api_organizations_org_id_service_accounts_post(
        @Path("org_id") orgId: String,
        @Header("X-Org-Id") xOrgId: String? = null,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @DELETE("api/organizations/{org_id}/service-accounts/{sa_id}")
    suspend fun delete_service_account_api_organizations_org_id_service_accounts_sa_id_delete(
        @Path("org_id") orgId: String,
        @Path("sa_id") saId: String,
        @Header("X-Org-Id") xOrgId: String? = null,
    ): Response<ResponseBody>

    @GET("api/organizations/{org_id}/service-accounts/{sa_id}")
    suspend fun get_service_account_api_organizations_org_id_service_accounts_sa_id_get(
        @Path("org_id") orgId: String,
        @Path("sa_id") saId: String,
        @Header("X-Org-Id") xOrgId: String? = null,
    ): Response<ResponseBody>

    @PATCH("api/organizations/{org_id}/service-accounts/{sa_id}")
    suspend fun update_service_account_api_organizations_org_id_service_accounts_sa_id_patch(
        @Path("org_id") orgId: String,
        @Path("sa_id") saId: String,
        @Header("X-Org-Id") xOrgId: String? = null,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @POST("api/organizations/{org_id}/service-accounts/{sa_id}/disable")
    suspend fun disable_service_account_api_organizations_org_id_service_accounts_sa_id_disable_post(
        @Path("org_id") orgId: String,
        @Path("sa_id") saId: String,
        @Header("X-Org-Id") xOrgId: String? = null,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @POST("api/organizations/{org_id}/service-accounts/{sa_id}/enable")
    suspend fun enable_service_account_api_organizations_org_id_service_accounts_sa_id_enable_post(
        @Path("org_id") orgId: String,
        @Path("sa_id") saId: String,
        @Header("X-Org-Id") xOrgId: String? = null,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @GET("api/organizations/{org_id}/service-accounts/{sa_id}/secrets")
    suspend fun list_service_account_secrets_api_organizations_org_id_service_accounts_sa_id_secrets_get(
        @Path("org_id") orgId: String,
        @Path("sa_id") saId: String,
        @Header("X-Org-Id") xOrgId: String? = null,
    ): Response<ResponseBody>

    @DELETE("api/organizations/{org_id}/service-accounts/{sa_id}/secrets/{secret_name}")
    suspend fun delete_service_account_secret_api_organizations_org_id_service_accounts_sa_id_secrets_secret_name_delete(
        @Path("org_id") orgId: String,
        @Path("sa_id") saId: String,
        @Path("secret_name") secretName: String,
        @Header("X-Org-Id") xOrgId: String? = null,
    ): Response<ResponseBody>

    @PUT("api/organizations/{org_id}/service-accounts/{sa_id}/secrets/{secret_name}")
    suspend fun put_service_account_secret_api_organizations_org_id_service_accounts_sa_id_secrets_secret_name_put(
        @Path("org_id") orgId: String,
        @Path("sa_id") saId: String,
        @Path("secret_name") secretName: String,
        @Header("X-Org-Id") xOrgId: String? = null,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @GET("api/organizations/{org_id}/settings")
    suspend fun get_org_defaults_settings_api_organizations_org_id_settings_get(
        @Path("org_id") orgId: String,
        @Header("X-Org-Id") xOrgId: String? = null,
    ): Response<ResponseBody>

    @PATCH("api/organizations/{org_id}/settings")
    suspend fun update_org_defaults_settings_api_organizations_org_id_settings_patch(
        @Path("org_id") orgId: String,
        @Header("X-Org-Id") xOrgId: String? = null,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @POST("api/organizations/{org_id}/switch")
    suspend fun switch_org_api_organizations_org_id_switch_post(
        @Path("org_id") orgId: String,
        @Header("X-Org-Id") xOrgId: String? = null,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @POST("api/quota/increase-request")
    suspend fun create_quota_increase_request_api_quota_increase_request_post(
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @GET("api/quota/status")
    suspend fun get_quota_status_api_quota_status_get(
    ): Response<ResponseBody>

    @GET("api/quota/verify")
    suspend fun verify_quota_increase_api_quota_verify_get(
    ): Response<ResponseBody>

    @GET("api/service/health")
    suspend fun service_health_api_service_health_get(
    ): Response<ResponseBody>

    @POST("api/service/users/{user_id}/orgs/{org_id}/api-keys")
    suspend fun get_or_create_api_key_for_user_api_service_users_user_id_orgs_org_id_api_keys_post(
        @Path("user_id") userId: String,
        @Path("org_id") orgId: String,
        @Header("X-Service-API-Key") xServiceAPIKey: String? = null,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @DELETE("api/service/users/{user_id}/orgs/{org_id}/api-keys/{key_name}")
    suspend fun delete_user_api_key_api_service_users_user_id_orgs_org_id_api_keys_key_name_delete(
        @Path("user_id") userId: String,
        @Path("org_id") orgId: String,
        @Path("key_name") keyName: String,
        @Header("X-Service-API-Key") xServiceAPIKey: String? = null,
    ): Response<ResponseBody>

    @GET("api/service/users/{user_id}/orgs/{org_id}/membership")
    suspend fun get_org_member_info_api_service_users_user_id_orgs_org_id_membership_get(
        @Path("user_id") userId: String,
        @Path("org_id") orgId: String,
        @Header("X-Service-API-Key") xServiceAPIKey: String? = null,
    ): Response<ResponseBody>

    @POST("api/service/users/{user_id}/orgs/{org_id}/secret-lookups")
    suspend fun create_secret_lookups_api_service_users_user_id_orgs_org_id_secret_lookups_post(
        @Path("user_id") userId: String,
        @Path("org_id") orgId: String,
        @Header("X-Service-API-Key") xServiceAPIKey: String? = null,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @GET("api/shared-conversations")
    suspend fun batch_get_shared_conversations_api_shared_conversations_get(
        @Query("ids") ids: String? = null,
    ): Response<ResponseBody>

    @GET("api/shared-events")
    suspend fun batch_get_shared_events_api_shared_events_get(
        @Query("conversation_id") conversationId: String? = null,
        @Query("id") id: String? = null,
    ): Response<ResponseBody>

    @GET("api/shared-events/count")
    suspend fun count_shared_events_api_shared_events_count_get(
        @Query("conversation_id") conversationId: String? = null,
        @Query("kind__eq") kindEq: String? = null,
        @Query("timestamp__gte") timestampGte: String? = null,
        @Query("timestamp__lt") timestampLt: String? = null,
    ): Response<ResponseBody>

    @GET("api/shared-events/search")
    suspend fun search_shared_events_api_shared_events_search_get(
        @Query("conversation_id") conversationId: String? = null,
        @Query("kind__eq") kindEq: String? = null,
        @Query("timestamp__gte") timestampGte: String? = null,
        @Query("timestamp__lt") timestampLt: String? = null,
        @Query("sort_order") sortOrder: String? = null,
        @Query("page_id") pageId: String? = null,
        @Query("limit") limit: String? = null,
    ): Response<ResponseBody>

    @GET("api/shared-events/{conversation_id}/{event_id}")
    suspend fun get_shared_event_api_shared_events_conversation_id_event_id_get(
        @Path("conversation_id") conversationId: String,
        @Path("event_id") eventId: String,
    ): Response<ResponseBody>

    @GET("api/tools/catalog")
    suspend fun get_tool_catalog_api_tools_catalog_get(
        @Query("org_id") orgId: String? = null,
    ): Response<ResponseBody>

    @GET("api/users/app")
    suspend fun get_user_app_settings_api_users_app_get(
    ): Response<ResponseBody>

    @POST("api/users/app")
    suspend fun update_user_app_settings_api_users_app_post(
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @GET("health")
    suspend fun health_health_get(
    ): Response<ResponseBody>

    @POST("integration/azure-devops/events")
    suspend fun azure_devops_events_integration_azure_devops_events_post(
        @Header("x-openhands-webhook-secret") xOpenhandsWebhookSecret: String? = null,
        @Header("authorization") authorization: String? = null,
        @Header("x-openhands-notification-type") xOpenhandsNotificationType: String? = null,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @GET("integration/azure-devops/organizations")
    suspend fun get_azure_devops_organizations_integration_azure_devops_organizations_get(
        @Query("org_id") orgId: String? = null,
    ): Response<ResponseBody>

    @POST("integration/azure-devops/reinstall-webhook")
    suspend fun reinstall_azure_devops_webhook_integration_azure_devops_reinstall_webhook_post(
        @Query("organization") organization: String? = null,
        @Query("org_id") orgId: String? = null,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @GET("integration/azure-devops/resources")
    suspend fun get_azure_devops_resources_integration_azure_devops_resources_get(
        @Query("organization") organization: String? = null,
        @Query("org_id") orgId: String? = null,
    ): Response<ResponseBody>

    @POST("integration/azure-devops/uninstall-webhook")
    suspend fun uninstall_azure_devops_webhook_integration_azure_devops_uninstall_webhook_post(
        @Query("organization") organization: String? = null,
        @Query("org_id") orgId: String? = null,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @POST("integration/bitbucket-dc/connections/{connection_id}/events")
    suspend fun bitbucket_dc_connection_events_integration_bitbucket_dc_connections_connection_id_events_post(
        @Path("connection_id") connectionId: String,
        @Header("x-hub-signature") xHubSignature: String? = null,
        @Header("x-event-key") xEventKey: String? = null,
        @Header("x-request-id") xRequestId: String? = null,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @POST("integration/bitbucket-dc/enroll-webhook")
    suspend fun enroll_bitbucket_dc_webhook_integration_bitbucket_dc_enroll_webhook_post(
        @Query("org_id") orgId: String? = null,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @POST("integration/bitbucket-dc/reinstall-webhook")
    suspend fun reinstall_bitbucket_dc_webhook_integration_bitbucket_dc_reinstall_webhook_post(
        @Query("org_id") orgId: String? = null,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @GET("integration/bitbucket-dc/resources")
    suspend fun get_bitbucket_dc_resources_integration_bitbucket_dc_resources_get(
        @Query("org_id") orgId: String? = null,
    ): Response<ResponseBody>

    @POST("integration/bitbucket-dc/uninstall-webhook")
    suspend fun uninstall_bitbucket_dc_webhook_integration_bitbucket_dc_uninstall_webhook_post(
        @Query("org_id") orgId: String? = null,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @PATCH("integration/bitbucket-dc/webhook-id")
    suspend fun update_bitbucket_dc_webhook_id_integration_bitbucket_dc_webhook_id_patch(
        @Query("org_id") orgId: String? = null,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @POST("integration/bitbucket/events")
    suspend fun bitbucket_events_integration_bitbucket_events_post(
        @Header("x-hub-signature") xHubSignature: String? = null,
        @Header("x-event-key") xEventKey: String? = null,
        @Header("x-request-uuid") xRequestUuid: String? = null,
        @Header("x-hook-uuid") xHookUuid: String? = null,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @POST("integration/github/events")
    suspend fun github_events_integration_github_events_post(
        @Header("x-hub-signature-256") xHubSignature256: String? = null,
        @Header("x-github-event") xGithubEvent: String? = null,
        @Header("x-github-delivery") xGithubDelivery: String? = null,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @POST("integration/gitlab/events")
    suspend fun gitlab_events_integration_gitlab_events_post(
        @Header("x-gitlab-token") xGitlabToken: String? = null,
        @Header("x-openhands-webhook-id") xOpenhandsWebhookId: String? = null,
        @Header("x-openhands-user-id") xOpenhandsUserId: String? = null,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @POST("integration/gitlab/reinstall-webhook")
    suspend fun reinstall_gitlab_webhook_integration_gitlab_reinstall_webhook_post(
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @GET("integration/gitlab/resources")
    suspend fun get_gitlab_resources_integration_gitlab_resources_get(
        @Query("provider_id") providerId: String? = null,
    ): Response<ResponseBody>

    @GET("integration/jira/callback")
    suspend fun jira_callback_integration_jira_callback_get(
        @Query("code") code: String? = null,
        @Query("state") state: String? = null,
    ): Response<ResponseBody>

    @POST("integration/jira/events")
    suspend fun jira_events_integration_jira_events_post(
        @Header("x-hub-signature") xHubSignature: String? = null,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @POST("integration/jira/workspaces")
    suspend fun create_jira_workspace_integration_jira_workspaces_post(
        @Query("org_id") orgId: String? = null,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @GET("integration/jira/workspaces/link")
    suspend fun get_current_workspace_link_integration_jira_workspaces_link_get(
    ): Response<ResponseBody>

    @POST("integration/jira/workspaces/link")
    suspend fun create_workspace_link_integration_jira_workspaces_link_post(
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @GET("integration/jira/workspaces/status")
    suspend fun get_jira_instance_status_integration_jira_workspaces_status_get(
    ): Response<ResponseBody>

    @POST("integration/jira/workspaces/unlink")
    suspend fun unlink_workspace_integration_jira_workspaces_unlink_post(
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @GET("integration/jira/workspaces/validate/{workspace_name}")
    suspend fun validate_workspace_integration_integration_jira_workspaces_validate_workspace_name_get(
        @Path("workspace_name") workspaceName: String,
    ): Response<ResponseBody>

    @POST("oauth/device/authorize")
    suspend fun device_authorization_oauth_device_authorize_post(
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @POST("oauth/device/token")
    suspend fun device_token_oauth_device_token_post(
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @POST("oauth/device/verify-authenticated")
    suspend fun device_verification_authenticated_oauth_device_verify_authenticated_post(
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @GET("oauth/git-providers")
    suspend fun oauth_v2_list_git_providers_oauth_git_providers_get(
    ): Response<ResponseBody>

    @POST("oauth/git-providers/{provider_id}/check")
    suspend fun oauth_v2_check_git_provider_oauth_git_providers_provider_id_check_post(
        @Path("provider_id") providerId: String,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @GET("oauth/github/callback")
    suspend fun github_dummy_callback_oauth_github_callback_get(
    ): Response<ResponseBody>

    @GET("oauth/idp-login")
    suspend fun oauth_v2_idp_login_oauth_idp_login_get(
        @Query("redirect_url") redirectUrl: String? = null,
        @Query("mode") mode: String? = null,
    ): Response<ResponseBody>

    @GET("oauth/idp-providers")
    suspend fun oauth_v2_list_idp_providers_oauth_idp_providers_get(
    ): Response<ResponseBody>

    @GET("oauth/idp/invite")
    suspend fun idp_invite_form_oauth_idp_invite_get(
        @Query("token") token: String? = null,
        @Query("error") error: String? = null,
    ): Response<ResponseBody>

    @POST("oauth/idp/invite")
    suspend fun idp_invite_accept_oauth_idp_invite_post(
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @GET("oauth/idp/login")
    suspend fun idp_login_form_oauth_idp_login_get(
        @Query("redirect_url") redirectUrl: String? = null,
        @Query("error") error: String? = null,
    ): Response<ResponseBody>

    @POST("oauth/idp/login")
    suspend fun idp_login_oauth_idp_login_post(
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @GET("oauth/idp/signup")
    suspend fun idp_signup_form_oauth_idp_signup_get(
        @Query("redirect_url") redirectUrl: String? = null,
        @Query("error") error: String? = null,
    ): Response<ResponseBody>

    @POST("oauth/idp/signup")
    suspend fun idp_signup_oauth_idp_signup_post(
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @GET("oauth/keycloak/callback")
    suspend fun keycloak_callback_oauth_keycloak_callback_get(
        @Query("code") code: String? = null,
        @Query("state") state: String? = null,
        @Query("error") error: String? = null,
        @Query("error_description") errorDescription: String? = null,
        @Query("kc_action_status") kcActionStatus: String? = null,
    ): Response<ResponseBody>

    @GET("oauth/keycloak/offline/callback")
    suspend fun keycloak_offline_callback_oauth_keycloak_offline_callback_get(
        @Query("code") code: String? = null,
        @Query("state") state: String? = null,
    ): Response<ResponseBody>

    @GET("oauth/providers")
    suspend fun oauth_v2_list_providers_oauth_providers_get(
    ): Response<ResponseBody>

    @DELETE("oauth/{provider_id}")
    suspend fun oauth_v2_unlink_oauth_provider_id_delete(
        @Path("provider_id") providerId: String,
    ): Response<ResponseBody>

    @GET("oauth/{provider_id}/callback")
    suspend fun oauth_v2_callback_oauth_provider_id_callback_get(
        @Path("provider_id") providerId: String,
        @Query("code") code: String? = null,
        @Query("state") state: String? = null,
        @Query("error") error: String? = null,
    ): Response<ResponseBody>

    @POST("oauth/{provider_id}/link")
    suspend fun oauth_v2_link_oauth_provider_id_link_post(
        @Path("provider_id") providerId: String,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @GET("oauth/{provider_id}/login")
    suspend fun oauth_v2_login_oauth_provider_id_login_get(
        @Path("provider_id") providerId: String,
        @Query("redirect_url") redirectUrl: String? = null,
        @Query("mode") mode: String? = null,
    ): Response<ResponseBody>

    @GET("ready")
    suspend fun is_ready_ready_get(
    ): Response<ResponseBody>

    @GET("saas")
    suspend fun is_saas_saas_get(
    ): Response<ResponseBody>

    @POST("saml/{provider_id}/acs")
    suspend fun saml_acs_saml_provider_id_acs_post(
        @Path("provider_id") providerId: String,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @GET("saml/{provider_id}/login")
    suspend fun saml_login_saml_provider_id_login_get(
        @Path("provider_id") providerId: String,
        @Query("redirect_url") redirectUrl: String? = null,
    ): Response<ResponseBody>

    @GET("saml/{provider_id}/metadata")
    suspend fun saml_metadata_saml_provider_id_metadata_get(
        @Path("provider_id") providerId: String,
    ): Response<ResponseBody>

    @GET("server_info")
    suspend fun get_server_info_server_info_get(
    ): Response<ResponseBody>

    @GET("slack/install")
    suspend fun install_slack_install_get(
        @Query("state") state: String? = null,
    ): Response<ResponseBody>

    @GET("slack/install-callback")
    suspend fun install_callback_slack_install_callback_get(
        @Query("code") code: String? = null,
        @Query("state") state: String? = null,
        @Query("error") error: String? = null,
    ): Response<ResponseBody>

    @GET("slack/keycloak-callback")
    suspend fun keycloak_callback_slack_keycloak_callback_get(
        @Query("code") code: String? = null,
        @Query("state") state: String? = null,
        @Query("error") error: String? = null,
    ): Response<ResponseBody>

    @GET("slack/link-callback")
    suspend fun link_callback_slack_link_callback_get(
        @Query("state") state: String? = null,
    ): Response<ResponseBody>

    @POST("slack/on-event")
    suspend fun on_event_slack_on_event_post(
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @POST("slack/on-form-interaction")
    suspend fun on_form_interaction_slack_on_form_interaction_post(
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @POST("slack/on-options-load")
    suspend fun on_options_load_slack_on_options_load_post(
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @GET("slack/status")
    suspend fun get_slack_status_slack_status_get(
    ): Response<ResponseBody>
}
