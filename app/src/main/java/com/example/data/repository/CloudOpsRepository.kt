package com.example.data.repository

import com.example.data.api.CloudAdminApiService
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response

/**
 * Complete, typed client for the non-`/api/v1` OpenHands Cloud surface. Every
 * method delegates to a real [CloudAdminApiService] call site. Generated from
 * the bundled cloud spec — regenerate rather than editing by hand.
 */
class CloudOpsRepository(private val api: CloudAdminApiService) {

    suspend fun alive_alive_get(): Response<ResponseBody> = api.alive_alive_get()

    suspend fun accept_tos_api_accept_tos_post(
        body: RequestBody,
    ): Response<ResponseBody> = api.accept_tos_api_accept_tos_post(body)

    suspend fun list_admin_users_api_admin_directory_users_get(
        orgId: String? = null,
    ): Response<ResponseBody> = api.list_admin_users_api_admin_directory_users_get(orgId)

    suspend fun delete_admin_user_api_admin_directory_users_user_id_delete(
        userId: String,
        orgId: String? = null,
    ): Response<ResponseBody> = api.delete_admin_user_api_admin_directory_users_user_id_delete(userId, orgId)

    suspend fun update_admin_user_status_api_admin_directory_users_user_id_patch(
        userId: String,
        orgId: String? = null,
        body: RequestBody,
    ): Response<ResponseBody> = api.update_admin_user_status_api_admin_directory_users_user_id_patch(userId, orgId, body)

    suspend fun update_admin_user_groups_api_admin_directory_users_user_id_groups_post(
        userId: String,
        orgId: String? = null,
        body: RequestBody,
    ): Response<ResponseBody> = api.update_admin_user_groups_api_admin_directory_users_user_id_groups_post(userId, orgId, body)

    suspend fun list_flags_api_admin_feature_flags_get(
        orgId: String? = null,
    ): Response<ResponseBody> = api.list_flags_api_admin_feature_flags_get(orgId)

    suspend fun create_flag_api_admin_feature_flags_post(
        orgId: String? = null,
        body: RequestBody,
    ): Response<ResponseBody> = api.create_flag_api_admin_feature_flags_post(orgId, body)

    suspend fun delete_flag_api_admin_feature_flags_key_delete(
        key: String,
        orgId: String? = null,
    ): Response<ResponseBody> = api.delete_flag_api_admin_feature_flags_key_delete(key, orgId)

    suspend fun get_flag_api_admin_feature_flags_key_get(
        key: String,
        orgId: String? = null,
    ): Response<ResponseBody> = api.get_flag_api_admin_feature_flags_key_get(key, orgId)

    suspend fun update_flag_api_admin_feature_flags_key_patch(
        key: String,
        orgId: String? = null,
        body: RequestBody,
    ): Response<ResponseBody> = api.update_flag_api_admin_feature_flags_key_patch(key, orgId, body)

    suspend fun evaluate_flag_api_admin_feature_flags_key_evaluate_post(
        key: String,
        orgId: String? = null,
        body: RequestBody,
    ): Response<ResponseBody> = api.evaluate_flag_api_admin_feature_flags_key_evaluate_post(key, orgId, body)

    suspend fun create_rule_api_admin_feature_flags_key_rules_post(
        key: String,
        orgId: String? = null,
        body: RequestBody,
    ): Response<ResponseBody> = api.create_rule_api_admin_feature_flags_key_rules_post(key, orgId, body)

    suspend fun delete_rule_api_admin_feature_flags_key_rules_rule_id_delete(
        key: String,
        ruleId: String,
        orgId: String? = null,
    ): Response<ResponseBody> = api.delete_rule_api_admin_feature_flags_key_rules_rule_id_delete(key, ruleId, orgId)

    suspend fun get_instance_settings_api_admin_instance_settings_get(): Response<ResponseBody> = api.get_instance_settings_api_admin_instance_settings_get()

    suspend fun update_instance_settings_api_admin_instance_settings_patch(
        orgId: String? = null,
        body: RequestBody,
    ): Response<ResponseBody> = api.update_instance_settings_api_admin_instance_settings_patch(orgId, body)

    suspend fun list_oauth_providers_api_admin_oauth_providers_get(
        orgId: String? = null,
    ): Response<ResponseBody> = api.list_oauth_providers_api_admin_oauth_providers_get(orgId)

    suspend fun create_oauth_provider_api_admin_oauth_providers_post(
        orgId: String? = null,
        body: RequestBody,
    ): Response<ResponseBody> = api.create_oauth_provider_api_admin_oauth_providers_post(orgId, body)

    suspend fun preview_saml_metadata_api_admin_oauth_providers_saml_metadata_preview_post(
        orgId: String? = null,
        body: RequestBody,
    ): Response<ResponseBody> = api.preview_saml_metadata_api_admin_oauth_providers_saml_metadata_preview_post(orgId, body)

    suspend fun delete_oauth_provider_api_admin_oauth_providers_provider_id_delete(
        providerId: String,
        orgId: String? = null,
    ): Response<ResponseBody> = api.delete_oauth_provider_api_admin_oauth_providers_provider_id_delete(providerId, orgId)

    suspend fun get_oauth_provider_api_admin_oauth_providers_provider_id_get(
        providerId: String,
        orgId: String? = null,
    ): Response<ResponseBody> = api.get_oauth_provider_api_admin_oauth_providers_provider_id_get(providerId, orgId)

    suspend fun update_oauth_provider_api_admin_oauth_providers_provider_id_patch(
        providerId: String,
        orgId: String? = null,
        body: RequestBody,
    ): Response<ResponseBody> = api.update_oauth_provider_api_admin_oauth_providers_provider_id_patch(providerId, orgId, body)

    suspend fun list_auth_provider_users_api_admin_oauth_providers_provider_id_users_get(
        providerId: String,
        pageId: String? = null,
        limit: String? = null,
        q: String? = null,
        orgId: String? = null,
    ): Response<ResponseBody> = api.list_auth_provider_users_api_admin_oauth_providers_provider_id_users_get(providerId, pageId, limit, q, orgId)

    suspend fun update_auth_provider_user_api_admin_oauth_providers_provider_id_users_link_id_patch(
        providerId: String,
        linkId: String,
        orgId: String? = null,
        body: RequestBody,
    ): Response<ResponseBody> = api.update_auth_provider_user_api_admin_oauth_providers_provider_id_users_link_id_patch(providerId, linkId, orgId, body)

    suspend fun list_admin_organizations_api_admin_organizations_get(
        orgId: String? = null,
    ): Response<ResponseBody> = api.list_admin_organizations_api_admin_organizations_get(orgId)

    suspend fun delete_admin_organization_api_admin_organizations_org_id_delete(
        orgId: String,
    ): Response<ResponseBody> = api.delete_admin_organization_api_admin_organizations_org_id_delete(orgId)

    suspend fun update_admin_organization_status_api_admin_organizations_org_id_patch(
        orgId: String,
        body: RequestBody,
    ): Response<ResponseBody> = api.update_admin_organization_status_api_admin_organizations_org_id_patch(orgId, body)

    suspend fun set_org_quota_api_admin_quota_orgs_org_id_quota_put(
        orgId: String,
        xOrgId: String? = null,
        body: RequestBody,
    ): Response<ResponseBody> = api.set_org_quota_api_admin_quota_orgs_org_id_quota_put(orgId, xOrgId, body)

    suspend fun list_pending_quota_requests_api_admin_quota_requests_get(
        orgId: String? = null,
        xOrgId: String? = null,
    ): Response<ResponseBody> = api.list_pending_quota_requests_api_admin_quota_requests_get(orgId, xOrgId)

    suspend fun approve_quota_request_api_admin_quota_requests_request_id_approve_post(
        requestId: String,
        orgId: String? = null,
        xOrgId: String? = null,
        body: RequestBody,
    ): Response<ResponseBody> = api.approve_quota_request_api_admin_quota_requests_request_id_approve_post(requestId, orgId, xOrgId, body)

    suspend fun reject_quota_request_api_admin_quota_requests_request_id_reject_post(
        requestId: String,
        orgId: String? = null,
        xOrgId: String? = null,
        body: RequestBody,
    ): Response<ResponseBody> = api.reject_quota_request_api_admin_quota_requests_request_id_reject_post(requestId, orgId, xOrgId, body)

    suspend fun set_user_quota_api_admin_quota_users_user_id_quota_put(
        userId: String,
        orgId: String? = null,
        xOrgId: String? = null,
        body: RequestBody,
    ): Response<ResponseBody> = api.set_user_quota_api_admin_quota_users_user_id_quota_put(userId, orgId, xOrgId, body)

    suspend fun get_setup_state_api_admin_setup_state_get(): Response<ResponseBody> = api.get_setup_state_api_admin_setup_state_get()

    suspend fun update_setup_state_api_admin_setup_state_patch(
        body: RequestBody,
    ): Response<ResponseBody> = api.update_setup_state_api_admin_setup_state_patch(body)

    suspend fun list_super_admins_api_admin_super_admins_get(): Response<ResponseBody> = api.list_super_admins_api_admin_super_admins_get()

    suspend fun grant_super_admin_api_admin_super_admins_post(
        orgId: String? = null,
        body: RequestBody,
    ): Response<ResponseBody> = api.grant_super_admin_api_admin_super_admins_post(orgId, body)

    suspend fun get_my_super_admin_status_api_admin_super_admins_me_get(): Response<ResponseBody> = api.get_my_super_admin_status_api_admin_super_admins_me_get()

    suspend fun revoke_super_admin_api_admin_super_admins_user_id_delete(
        userId: String,
        orgId: String? = null,
    ): Response<ResponseBody> = api.revoke_super_admin_api_admin_super_admins_user_id_delete(userId, orgId)

    suspend fun list_all_users_api_admin_users_get(
        pageId: String? = null,
        limit: String? = null,
        email: String? = null,
        orgId: String? = null,
    ): Response<ResponseBody> = api.list_all_users_api_admin_users_get(pageId, limit, email, orgId)

    suspend fun count_all_users_api_admin_users_count_get(
        email: String? = null,
        orgId: String? = null,
    ): Response<ResponseBody> = api.count_all_users_api_admin_users_count_get(email, orgId)

    suspend fun delete_user_api_admin_users_user_id_delete(
        userId: String,
        orgId: String? = null,
    ): Response<ResponseBody> = api.delete_user_api_admin_users_user_id_delete(userId, orgId)

    suspend fun disable_user_api_admin_users_user_id_disable_post(
        userId: String,
        orgId: String? = null,
        body: RequestBody,
    ): Response<ResponseBody> = api.disable_user_api_admin_users_user_id_disable_post(userId, orgId, body)

    suspend fun enable_user_api_admin_users_user_id_enable_post(
        userId: String,
        orgId: String? = null,
        body: RequestBody,
    ): Response<ResponseBody> = api.enable_user_api_admin_users_user_id_enable_post(userId, orgId, body)

    suspend fun search_verified_models_api_admin_verified_models_get(
        provider: String? = null,
        pageId: String? = null,
        limit: String? = null,
    ): Response<ResponseBody> = api.search_verified_models_api_admin_verified_models_get(provider, pageId, limit)

    suspend fun create_verified_model_api_admin_verified_models_post(
        body: RequestBody,
    ): Response<ResponseBody> = api.create_verified_model_api_admin_verified_models_post(body)

    suspend fun delete_verified_model_api_admin_verified_models_provider_model_name_delete(
        provider: String,
        modelName: String,
    ): Response<ResponseBody> = api.delete_verified_model_api_admin_verified_models_provider_model_name_delete(provider, modelName)

    suspend fun update_verified_model_api_admin_verified_models_provider_model_name_put(
        provider: String,
        modelName: String,
        body: RequestBody,
    ): Response<ResponseBody> = api.update_verified_model_api_admin_verified_models_provider_model_name_put(provider, modelName, body)

    suspend fun list_agent_profiles_api_agent_profiles_get(
        orgId: String? = null,
    ): Response<ResponseBody> = api.list_agent_profiles_api_agent_profiles_get(orgId)

    suspend fun delete_agent_profile_api_agent_profiles_name_delete(
        name: String,
        scope: String? = null,
    ): Response<ResponseBody> = api.delete_agent_profile_api_agent_profiles_name_delete(name, scope)

    suspend fun get_agent_profile_api_agent_profiles_name_get(
        name: String,
        scope: String? = null,
        orgId: String? = null,
    ): Response<ResponseBody> = api.get_agent_profile_api_agent_profiles_name_get(name, scope, orgId)

    suspend fun save_agent_profile_api_agent_profiles_name_post(
        name: String,
        scope: String? = null,
        body: RequestBody,
    ): Response<ResponseBody> = api.save_agent_profile_api_agent_profiles_name_post(name, scope, body)

    suspend fun materialize_agent_profile_api_agent_profiles_name_materialize_post(
        name: String,
        scope: String? = null,
        orgId: String? = null,
        body: RequestBody,
    ): Response<ResponseBody> = api.materialize_agent_profile_api_agent_profiles_name_materialize_post(name, scope, orgId, body)

    suspend fun rename_agent_profile_api_agent_profiles_name_rename_post(
        name: String,
        scope: String? = null,
        body: RequestBody,
    ): Response<ResponseBody> = api.rename_agent_profile_api_agent_profiles_name_rename_post(name, scope, body)

    suspend fun activate_agent_profile_api_agent_profiles_profile_id_activate_post(
        profileId: String,
        orgId: String? = null,
        body: RequestBody,
    ): Response<ResponseBody> = api.activate_agent_profile_api_agent_profiles_profile_id_activate_post(profileId, orgId, body)

    suspend fun deactivate_agent_profile_api_agent_profiles_profile_id_deactivate_post(
        profileId: String,
        orgId: String? = null,
        body: RequestBody,
    ): Response<ResponseBody> = api.deactivate_agent_profile_api_agent_profiles_profile_id_deactivate_post(profileId, orgId, body)

    suspend fun track_frontend_event_api_analytics_events_post(
        body: RequestBody,
    ): Response<ResponseBody> = api.track_frontend_event_api_analytics_events_post(body)

    suspend fun authenticate_api_authenticate_post(
        body: RequestBody,
    ): Response<ResponseBody> = api.authenticate_api_authenticate_post(body)

    suspend fun cancel_callback_api_billing_cancel_get(
        sessionId: String? = null,
    ): Response<ResponseBody> = api.cancel_callback_api_billing_cancel_get(sessionId)

    suspend fun create_checkout_session_api_billing_create_checkout_session_post(
        body: RequestBody,
    ): Response<ResponseBody> = api.create_checkout_session_api_billing_create_checkout_session_post(body)

    suspend fun create_customer_setup_session_api_billing_create_customer_setup_session_post(
        body: RequestBody,
    ): Response<ResponseBody> = api.create_customer_setup_session_api_billing_create_customer_setup_session_post(body)

    suspend fun get_credits_api_billing_credits_get(): Response<ResponseBody> = api.get_credits_api_billing_credits_get()

    suspend fun has_payment_method_api_billing_has_payment_method_post(
        body: RequestBody,
    ): Response<ResponseBody> = api.has_payment_method_api_billing_has_payment_method_post(body)

    suspend fun get_subscription_access_api_billing_subscription_access_get(): Response<ResponseBody> = api.get_subscription_access_api_billing_subscription_access_get()

    suspend fun success_callback_api_billing_success_get(
        sessionId: String? = null,
    ): Response<ResponseBody> = api.success_callback_api_billing_success_get(sessionId)

    suspend fun complete_onboarding_api_complete_onboarding_post(
        body: RequestBody,
    ): Response<ResponseBody> = api.complete_onboarding_api_complete_onboarding_post(body)

    suspend fun update_email_api_email_post(
        body: RequestBody,
    ): Response<ResponseBody> = api.update_email_api_email_post(body)

    suspend fun resend_email_verification_api_email_resend_put(
        body: RequestBody,
    ): Response<ResponseBody> = api.resend_email_verification_api_email_resend_put(body)

    suspend fun verified_email_api_email_verified_get(): Response<ResponseBody> = api.verified_email_api_email_verified_get()

    suspend fun idp_has_password_api_idp_password_get(): Response<ResponseBody> = api.idp_has_password_api_idp_password_get()

    suspend fun idp_set_password_api_idp_password_post(
        body: RequestBody,
    ): Response<ResponseBody> = api.idp_set_password_api_idp_password_post(body)

    suspend fun create_signup_link_api_idp_signup_links_post(
        body: RequestBody,
    ): Response<ResponseBody> = api.create_signup_link_api_idp_signup_links_post(body)

    suspend fun idp_status_api_idp_status_get(): Response<ResponseBody> = api.idp_status_api_idp_status_get()

    suspend fun keycloak_link_context_api_keycloak_link_context_post(
        body: RequestBody,
    ): Response<ResponseBody> = api.keycloak_link_context_api_keycloak_link_context_post(body)

    suspend fun list_api_keys_api_keys_get(): Response<ResponseBody> = api.list_api_keys_api_keys_get()

    suspend fun create_api_key_api_keys_post(
        body: RequestBody,
    ): Response<ResponseBody> = api.create_api_key_api_keys_post(body)

    suspend fun get_current_api_key_api_keys_current_get(): Response<ResponseBody> = api.get_current_api_key_api_keys_current_get()

    suspend fun get_llm_api_key_for_byor_api_keys_llm_byor_get(): Response<ResponseBody> = api.get_llm_api_key_for_byor_api_keys_llm_byor_get()

    suspend fun check_byor_permitted_api_keys_llm_byor_permitted_get(): Response<ResponseBody> = api.check_byor_permitted_api_keys_llm_byor_permitted_get()

    suspend fun refresh_llm_api_key_for_byor_api_keys_llm_byor_refresh_post(
        body: RequestBody,
    ): Response<ResponseBody> = api.refresh_llm_api_key_for_byor_api_keys_llm_byor_refresh_post(body)

    suspend fun refresh_managed_llm_api_key_api_keys_llm_managed_refresh_post(
        body: RequestBody,
    ): Response<ResponseBody> = api.refresh_managed_llm_api_key_api_keys_llm_managed_refresh_post(body)

    suspend fun delete_api_key_api_keys_key_id_delete(
        keyId: String,
    ): Response<ResponseBody> = api.delete_api_key_api_keys_key_id_delete(keyId)

    suspend fun get_license_api_license_get(
        orgId: String? = null,
    ): Response<ResponseBody> = api.get_license_api_license_get(orgId)

    suspend fun save_license_api_license_put(
        orgId: String? = null,
        body: RequestBody,
    ): Response<ResponseBody> = api.save_license_api_license_put(orgId, body)

    suspend fun get_license_status_api_license_status_get(): Response<ResponseBody> = api.get_license_status_api_license_status_get()

    suspend fun get_license_usage_api_license_usage_get(): Response<ResponseBody> = api.get_license_usage_api_license_usage_get()

    suspend fun logout_api_logout_post(
        body: RequestBody,
    ): Response<ResponseBody> = api.logout_api_logout_post(body)

    suspend fun onboarding_status_api_onboarding_status_get(): Response<ResponseBody> = api.onboarding_status_api_onboarding_status_get()

    suspend fun list_user_orgs_api_organizations_get(
        pageId: String? = null,
        limit: String? = null,
        name: String? = null,
        xOrgId: String? = null,
    ): Response<ResponseBody> = api.list_user_orgs_api_organizations_get(pageId, limit, name, xOrgId)

    suspend fun create_org_api_organizations_post(
        orgId: String? = null,
        xOrgId: String? = null,
        body: RequestBody,
    ): Response<ResponseBody> = api.create_org_api_organizations_post(orgId, xOrgId, body)

    suspend fun get_org_app_settings_api_organizations_app_get(
        orgId: String? = null,
        xOrgId: String? = null,
    ): Response<ResponseBody> = api.get_org_app_settings_api_organizations_app_get(orgId, xOrgId)

    suspend fun update_org_app_settings_api_organizations_app_post(
        orgId: String? = null,
        xOrgId: String? = null,
        body: RequestBody,
    ): Response<ResponseBody> = api.update_org_app_settings_api_organizations_app_post(orgId, xOrgId, body)

    suspend fun get_legacy_org_defaults_settings_api_organizations_llm_get(
        orgId: String? = null,
        xOrgId: String? = null,
    ): Response<ResponseBody> = api.get_legacy_org_defaults_settings_api_organizations_llm_get(orgId, xOrgId)

    suspend fun update_legacy_org_defaults_settings_api_organizations_llm_post(
        orgId: String? = null,
        xOrgId: String? = null,
        body: RequestBody,
    ): Response<ResponseBody> = api.update_legacy_org_defaults_settings_api_organizations_llm_post(orgId, xOrgId, body)

    suspend fun accept_invitation_redirect_api_organizations_members_invite_accept_get(
        token: String? = null,
    ): Response<ResponseBody> = api.accept_invitation_redirect_api_organizations_members_invite_accept_get(token)

    suspend fun accept_invitation_api_organizations_members_invite_accept_post(
        body: RequestBody,
    ): Response<ResponseBody> = api.accept_invitation_api_organizations_members_invite_accept_post(body)

    suspend fun delete_org_api_organizations_org_id_delete(
        orgId: String,
        xOrgId: String? = null,
    ): Response<ResponseBody> = api.delete_org_api_organizations_org_id_delete(orgId, xOrgId)

    suspend fun get_org_api_organizations_org_id_get(
        orgId: String,
        xOrgId: String? = null,
    ): Response<ResponseBody> = api.get_org_api_organizations_org_id_get(orgId, xOrgId)

    suspend fun update_org_api_organizations_org_id_patch(
        orgId: String,
        xOrgId: String? = null,
        body: RequestBody,
    ): Response<ResponseBody> = api.update_org_api_organizations_org_id_patch(orgId, xOrgId, body)

    suspend fun get_org_budget_settings_api_organizations_org_id_budgets_get(
        orgId: String,
        usersPage: String? = null,
        usersPerPage: String? = null,
        usersSearch: String? = null,
        usersStatus: String? = null,
        xOrgId: String? = null,
    ): Response<ResponseBody> = api.get_org_budget_settings_api_organizations_org_id_budgets_get(orgId, usersPage, usersPerPage, usersSearch, usersStatus, xOrgId)

    suspend fun update_org_budget_settings_api_organizations_org_id_budgets_patch(
        orgId: String,
        usersPage: String? = null,
        usersPerPage: String? = null,
        usersSearch: String? = null,
        usersStatus: String? = null,
        xOrgId: String? = null,
        body: RequestBody,
    ): Response<ResponseBody> = api.update_org_budget_settings_api_organizations_org_id_budgets_patch(orgId, usersPage, usersPerPage, usersSearch, usersStatus, xOrgId, body)

    suspend fun get_my_org_budget_api_organizations_org_id_budgets_me_get(
        orgId: String,
        includeSpend: String? = null,
        xOrgId: String? = null,
    ): Response<ResponseBody> = api.get_my_org_budget_api_organizations_org_id_budgets_me_get(orgId, includeSpend, xOrgId)

    suspend fun delete_org_budget_override_api_organizations_org_id_budgets_overrides_user_id_delete(
        orgId: String,
        userId: String,
        xOrgId: String? = null,
    ): Response<ResponseBody> = api.delete_org_budget_override_api_organizations_org_id_budgets_overrides_user_id_delete(orgId, userId, xOrgId)

    suspend fun upsert_org_budget_override_api_organizations_org_id_budgets_overrides_user_id_put(
        orgId: String,
        userId: String,
        xOrgId: String? = null,
        body: RequestBody,
    ): Response<ResponseBody> = api.upsert_org_budget_override_api_organizations_org_id_budgets_overrides_user_id_put(orgId, userId, xOrgId, body)

    suspend fun list_org_conversations_api_organizations_org_id_conversations_get(
        orgId: String,
        search: String? = null,
        sortBy: String? = null,
        sortOrder: String? = null,
        executionStatus: String? = null,
        sandboxStatus: String? = null,
        timeWindow: String? = null,
        page: String? = null,
        perPage: String? = null,
        includeSubConversations: String? = null,
        xOrgId: String? = null,
    ): Response<ResponseBody> = api.list_org_conversations_api_organizations_org_id_conversations_get(orgId, search, sortBy, sortOrder, executionStatus, sandboxStatus, timeWindow, page, perPage, includeSubConversations, xOrgId)

    suspend fun export_org_conversations_csv_api_organizations_org_id_conversations_export_get(
        orgId: String,
        search: String? = null,
        sortBy: String? = null,
        sortOrder: String? = null,
        executionStatus: String? = null,
        sandboxStatus: String? = null,
        timeWindow: String? = null,
        includeSubConversations: String? = null,
        xOrgId: String? = null,
    ): Response<ResponseBody> = api.export_org_conversations_csv_api_organizations_org_id_conversations_export_get(orgId, search, sortBy, sortOrder, executionStatus, sandboxStatus, timeWindow, includeSubConversations, xOrgId)

    suspend fun get_my_org_conversation_usage_stats_api_organizations_org_id_conversations_my_usage_get(
        orgId: String,
        timeWindow: String? = null,
        xOrgId: String? = null,
    ): Response<ResponseBody> = api.get_my_org_conversation_usage_stats_api_organizations_org_id_conversations_my_usage_get(orgId, timeWindow, xOrgId)

    suspend fun get_org_conversation_stats_api_organizations_org_id_conversations_stats_get(
        orgId: String,
        xOrgId: String? = null,
    ): Response<ResponseBody> = api.get_org_conversation_stats_api_organizations_org_id_conversations_stats_get(orgId, xOrgId)

    suspend fun get_org_conversation_usage_stats_api_organizations_org_id_conversations_usage_stats_get(
        orgId: String,
        timeWindow: String? = null,
        days: String? = null,
        xOrgId: String? = null,
    ): Response<ResponseBody> = api.get_org_conversation_usage_stats_api_organizations_org_id_conversations_usage_stats_get(orgId, timeWindow, days, xOrgId)

    suspend fun get_org_conversation_user_usage_stats_api_organizations_org_id_conversations_user_usage_get(
        orgId: String,
        limit: String? = null,
        offset: String? = null,
        timeWindow: String? = null,
        xOrgId: String? = null,
    ): Response<ResponseBody> = api.get_org_conversation_user_usage_stats_api_organizations_org_id_conversations_user_usage_get(orgId, limit, offset, timeWindow, xOrgId)

    suspend fun get_org_conversation_api_organizations_org_id_conversations_conversation_id_get(
        orgId: String,
        conversationId: String,
        xOrgId: String? = null,
    ): Response<ResponseBody> = api.get_org_conversation_api_organizations_org_id_conversations_conversation_id_get(orgId, conversationId, xOrgId)

    suspend fun stop_org_conversation_api_organizations_org_id_conversations_conversation_id_stop_post(
        orgId: String,
        conversationId: String,
        xOrgId: String? = null,
        body: RequestBody,
    ): Response<ResponseBody> = api.stop_org_conversation_api_organizations_org_id_conversations_conversation_id_stop_post(orgId, conversationId, xOrgId, body)

    suspend fun get_git_claims_api_organizations_org_id_git_claims_get(
        orgId: String,
        xOrgId: String? = null,
    ): Response<ResponseBody> = api.get_git_claims_api_organizations_org_id_git_claims_get(orgId, xOrgId)

    suspend fun claim_git_organization_api_organizations_org_id_git_claims_post(
        orgId: String,
        xOrgId: String? = null,
        body: RequestBody,
    ): Response<ResponseBody> = api.claim_git_organization_api_organizations_org_id_git_claims_post(orgId, xOrgId, body)

    suspend fun disconnect_git_organization_api_organizations_org_id_git_claims_claim_id_delete(
        orgId: String,
        claimId: String,
        xOrgId: String? = null,
    ): Response<ResponseBody> = api.disconnect_git_organization_api_organizations_org_id_git_claims_claim_id_delete(orgId, claimId, xOrgId)

    suspend fun list_org_mcp_servers_api_organizations_org_id_mcp_servers_get(
        orgId: String,
    ): Response<ResponseBody> = api.list_org_mcp_servers_api_organizations_org_id_mcp_servers_get(orgId)

    suspend fun create_org_mcp_server_api_organizations_org_id_mcp_servers_post(
        orgId: String,
        body: RequestBody,
    ): Response<ResponseBody> = api.create_org_mcp_server_api_organizations_org_id_mcp_servers_post(orgId, body)

    suspend fun delete_org_mcp_server_api_organizations_org_id_mcp_servers_name_delete(
        orgId: String,
        name: String,
    ): Response<ResponseBody> = api.delete_org_mcp_server_api_organizations_org_id_mcp_servers_name_delete(orgId, name)

    suspend fun update_org_mcp_server_api_organizations_org_id_mcp_servers_name_put(
        orgId: String,
        name: String,
        body: RequestBody,
    ): Response<ResponseBody> = api.update_org_mcp_server_api_organizations_org_id_mcp_servers_name_put(orgId, name, body)

    suspend fun disconnect_org_mcp_server_api_organizations_org_id_mcp_servers_name_connection_delete(
        orgId: String,
        name: String,
    ): Response<ResponseBody> = api.disconnect_org_mcp_server_api_organizations_org_id_mcp_servers_name_connection_delete(orgId, name)

    suspend fun connect_org_mcp_server_api_organizations_org_id_mcp_servers_name_connection_put(
        orgId: String,
        name: String,
        body: RequestBody,
    ): Response<ResponseBody> = api.connect_org_mcp_server_api_organizations_org_id_mcp_servers_name_connection_put(orgId, name, body)

    suspend fun get_me_api_organizations_org_id_me_get(
        orgId: String,
        xOrgId: String? = null,
    ): Response<ResponseBody> = api.get_me_api_organizations_org_id_me_get(orgId, xOrgId)

    suspend fun get_org_members_api_organizations_org_id_members_get(
        orgId: String,
        pageId: String? = null,
        limit: String? = null,
        email: String? = null,
        xOrgId: String? = null,
    ): Response<ResponseBody> = api.get_org_members_api_organizations_org_id_members_get(orgId, pageId, limit, email, xOrgId)

    suspend fun get_org_members_count_api_organizations_org_id_members_count_get(
        orgId: String,
        email: String? = null,
        xOrgId: String? = null,
    ): Response<ResponseBody> = api.get_org_members_count_api_organizations_org_id_members_count_get(orgId, email, xOrgId)

    suspend fun get_org_members_financial_api_organizations_org_id_members_financial_get(
        orgId: String,
        pageId: String? = null,
        limit: String? = null,
        email: String? = null,
        xOrgId: String? = null,
    ): Response<ResponseBody> = api.get_org_members_financial_api_organizations_org_id_members_financial_get(orgId, pageId, limit, email, xOrgId)

    suspend fun list_pending_invitations_api_organizations_org_id_members_invite_get(
        orgId: String,
        xOrgId: String? = null,
    ): Response<ResponseBody> = api.list_pending_invitations_api_organizations_org_id_members_invite_get(orgId, xOrgId)

    suspend fun create_invitation_api_organizations_org_id_members_invite_post(
        orgId: String,
        xOrgId: String? = null,
        body: RequestBody,
    ): Response<ResponseBody> = api.create_invitation_api_organizations_org_id_members_invite_post(orgId, xOrgId, body)

    suspend fun revoke_invitation_api_organizations_org_id_members_invite_invitation_id_delete(
        orgId: String,
        invitationId: String,
        xOrgId: String? = null,
    ): Response<ResponseBody> = api.revoke_invitation_api_organizations_org_id_members_invite_invitation_id_delete(orgId, invitationId, xOrgId)

    suspend fun remove_org_member_api_organizations_org_id_members_user_id_delete(
        orgId: String,
        userId: String,
        xOrgId: String? = null,
    ): Response<ResponseBody> = api.remove_org_member_api_organizations_org_id_members_user_id_delete(orgId, userId, xOrgId)

    suspend fun get_org_member_api_organizations_org_id_members_user_id_get(
        orgId: String,
        userId: String,
        xOrgId: String? = null,
    ): Response<ResponseBody> = api.get_org_member_api_organizations_org_id_members_user_id_get(orgId, userId, xOrgId)

    suspend fun update_org_member_api_organizations_org_id_members_user_id_patch(
        orgId: String,
        userId: String,
        xOrgId: String? = null,
        body: RequestBody,
    ): Response<ResponseBody> = api.update_org_member_api_organizations_org_id_members_user_id_patch(orgId, userId, xOrgId, body)

    suspend fun list_meta_profiles_api_organizations_org_id_meta_profiles_get(
        orgId: String,
    ): Response<ResponseBody> = api.list_meta_profiles_api_organizations_org_id_meta_profiles_get(orgId)

    suspend fun update_meta_profile_settings_api_organizations_org_id_meta_profiles_patch(
        orgId: String,
        body: RequestBody,
    ): Response<ResponseBody> = api.update_meta_profile_settings_api_organizations_org_id_meta_profiles_patch(orgId, body)

    suspend fun delete_meta_profile_api_organizations_org_id_meta_profiles_name_delete(
        orgId: String,
        name: String,
    ): Response<ResponseBody> = api.delete_meta_profile_api_organizations_org_id_meta_profiles_name_delete(orgId, name)

    suspend fun get_meta_profile_api_organizations_org_id_meta_profiles_name_get(
        orgId: String,
        name: String,
    ): Response<ResponseBody> = api.get_meta_profile_api_organizations_org_id_meta_profiles_name_get(orgId, name)

    suspend fun save_meta_profile_api_organizations_org_id_meta_profiles_name_post(
        orgId: String,
        name: String,
        body: RequestBody,
    ): Response<ResponseBody> = api.save_meta_profile_api_organizations_org_id_meta_profiles_name_post(orgId, name, body)

    suspend fun activate_meta_profile_api_organizations_org_id_meta_profiles_name_activate_post(
        orgId: String,
        name: String,
        body: RequestBody,
    ): Response<ResponseBody> = api.activate_meta_profile_api_organizations_org_id_meta_profiles_name_activate_post(orgId, name, body)

    suspend fun deactivate_meta_profile_api_organizations_org_id_meta_profiles_name_deactivate_post(
        orgId: String,
        name: String,
        body: RequestBody,
    ): Response<ResponseBody> = api.deactivate_meta_profile_api_organizations_org_id_meta_profiles_name_deactivate_post(orgId, name, body)

    suspend fun list_profiles_api_organizations_org_id_profiles_get(
        orgId: String,
    ): Response<ResponseBody> = api.list_profiles_api_organizations_org_id_profiles_get(orgId)

    suspend fun delete_profile_api_organizations_org_id_profiles_name_delete(
        orgId: String,
        name: String,
    ): Response<ResponseBody> = api.delete_profile_api_organizations_org_id_profiles_name_delete(orgId, name)

    suspend fun get_profile_api_organizations_org_id_profiles_name_get(
        orgId: String,
        name: String,
    ): Response<ResponseBody> = api.get_profile_api_organizations_org_id_profiles_name_get(orgId, name)

    suspend fun save_profile_api_organizations_org_id_profiles_name_post(
        orgId: String,
        name: String,
        body: RequestBody,
    ): Response<ResponseBody> = api.save_profile_api_organizations_org_id_profiles_name_post(orgId, name, body)

    suspend fun activate_profile_api_organizations_org_id_profiles_name_activate_post(
        orgId: String,
        name: String,
        body: RequestBody,
    ): Response<ResponseBody> = api.activate_profile_api_organizations_org_id_profiles_name_activate_post(orgId, name, body)

    suspend fun rename_profile_api_organizations_org_id_profiles_name_rename_post(
        orgId: String,
        name: String,
        body: RequestBody,
    ): Response<ResponseBody> = api.rename_profile_api_organizations_org_id_profiles_name_rename_post(orgId, name, body)

    suspend fun list_provider_connections_api_organizations_org_id_provider_connections_get(
        orgId: String,
    ): Response<ResponseBody> = api.list_provider_connections_api_organizations_org_id_provider_connections_get(orgId)

    suspend fun create_provider_connection_api_organizations_org_id_provider_connections_post(
        orgId: String,
        body: RequestBody,
    ): Response<ResponseBody> = api.create_provider_connection_api_organizations_org_id_provider_connections_post(orgId, body)

    suspend fun delete_provider_connection_api_organizations_org_id_provider_connections_connection_id_delete(
        orgId: String,
        connectionId: String,
    ): Response<ResponseBody> = api.delete_provider_connection_api_organizations_org_id_provider_connections_connection_id_delete(orgId, connectionId)

    suspend fun update_provider_connection_api_organizations_org_id_provider_connections_connection_id_patch(
        orgId: String,
        connectionId: String,
        body: RequestBody,
    ): Response<ResponseBody> = api.update_provider_connection_api_organizations_org_id_provider_connections_connection_id_patch(orgId, connectionId, body)

    suspend fun list_org_secrets_api_organizations_org_id_secrets_get(
        orgId: String,
    ): Response<ResponseBody> = api.list_org_secrets_api_organizations_org_id_secrets_get(orgId)

    suspend fun create_org_secret_api_organizations_org_id_secrets_post(
        orgId: String,
        body: RequestBody,
    ): Response<ResponseBody> = api.create_org_secret_api_organizations_org_id_secrets_post(orgId, body)

    suspend fun delete_org_secret_api_organizations_org_id_secrets_secret_name_delete(
        orgId: String,
        secretName: String,
    ): Response<ResponseBody> = api.delete_org_secret_api_organizations_org_id_secrets_secret_name_delete(orgId, secretName)

    suspend fun update_org_secret_api_organizations_org_id_secrets_secret_name_put(
        orgId: String,
        secretName: String,
        body: RequestBody,
    ): Response<ResponseBody> = api.update_org_secret_api_organizations_org_id_secrets_secret_name_put(orgId, secretName, body)

    suspend fun replace_org_secret_value_api_organizations_org_id_secrets_secret_name_value_put(
        orgId: String,
        secretName: String,
        body: RequestBody,
    ): Response<ResponseBody> = api.replace_org_secret_value_api_organizations_org_id_secrets_secret_name_value_put(orgId, secretName, body)

    suspend fun get_org_secret_value_version_api_organizations_org_id_secrets_secret_name_version_get(
        orgId: String,
        secretName: String,
    ): Response<ResponseBody> = api.get_org_secret_value_version_api_organizations_org_id_secrets_secret_name_version_get(orgId, secretName)

    suspend fun list_service_accounts_api_organizations_org_id_service_accounts_get(
        orgId: String,
        pageId: String? = null,
        limit: String? = null,
        xOrgId: String? = null,
    ): Response<ResponseBody> = api.list_service_accounts_api_organizations_org_id_service_accounts_get(orgId, pageId, limit, xOrgId)

    suspend fun create_service_account_api_organizations_org_id_service_accounts_post(
        orgId: String,
        xOrgId: String? = null,
        body: RequestBody,
    ): Response<ResponseBody> = api.create_service_account_api_organizations_org_id_service_accounts_post(orgId, xOrgId, body)

    suspend fun delete_service_account_api_organizations_org_id_service_accounts_sa_id_delete(
        orgId: String,
        saId: String,
        xOrgId: String? = null,
    ): Response<ResponseBody> = api.delete_service_account_api_organizations_org_id_service_accounts_sa_id_delete(orgId, saId, xOrgId)

    suspend fun get_service_account_api_organizations_org_id_service_accounts_sa_id_get(
        orgId: String,
        saId: String,
        xOrgId: String? = null,
    ): Response<ResponseBody> = api.get_service_account_api_organizations_org_id_service_accounts_sa_id_get(orgId, saId, xOrgId)

    suspend fun update_service_account_api_organizations_org_id_service_accounts_sa_id_patch(
        orgId: String,
        saId: String,
        xOrgId: String? = null,
        body: RequestBody,
    ): Response<ResponseBody> = api.update_service_account_api_organizations_org_id_service_accounts_sa_id_patch(orgId, saId, xOrgId, body)

    suspend fun disable_service_account_api_organizations_org_id_service_accounts_sa_id_disable_post(
        orgId: String,
        saId: String,
        xOrgId: String? = null,
        body: RequestBody,
    ): Response<ResponseBody> = api.disable_service_account_api_organizations_org_id_service_accounts_sa_id_disable_post(orgId, saId, xOrgId, body)

    suspend fun enable_service_account_api_organizations_org_id_service_accounts_sa_id_enable_post(
        orgId: String,
        saId: String,
        xOrgId: String? = null,
        body: RequestBody,
    ): Response<ResponseBody> = api.enable_service_account_api_organizations_org_id_service_accounts_sa_id_enable_post(orgId, saId, xOrgId, body)

    suspend fun list_service_account_secrets_api_organizations_org_id_service_accounts_sa_id_secrets_get(
        orgId: String,
        saId: String,
        xOrgId: String? = null,
    ): Response<ResponseBody> = api.list_service_account_secrets_api_organizations_org_id_service_accounts_sa_id_secrets_get(orgId, saId, xOrgId)

    suspend fun delete_service_account_secret_api_organizations_org_id_service_accounts_sa_id_secrets_secret_name_delete(
        orgId: String,
        saId: String,
        secretName: String,
        xOrgId: String? = null,
    ): Response<ResponseBody> = api.delete_service_account_secret_api_organizations_org_id_service_accounts_sa_id_secrets_secret_name_delete(orgId, saId, secretName, xOrgId)

    suspend fun put_service_account_secret_api_organizations_org_id_service_accounts_sa_id_secrets_secret_name_put(
        orgId: String,
        saId: String,
        secretName: String,
        xOrgId: String? = null,
        body: RequestBody,
    ): Response<ResponseBody> = api.put_service_account_secret_api_organizations_org_id_service_accounts_sa_id_secrets_secret_name_put(orgId, saId, secretName, xOrgId, body)

    suspend fun get_org_defaults_settings_api_organizations_org_id_settings_get(
        orgId: String,
        xOrgId: String? = null,
    ): Response<ResponseBody> = api.get_org_defaults_settings_api_organizations_org_id_settings_get(orgId, xOrgId)

    suspend fun update_org_defaults_settings_api_organizations_org_id_settings_patch(
        orgId: String,
        xOrgId: String? = null,
        body: RequestBody,
    ): Response<ResponseBody> = api.update_org_defaults_settings_api_organizations_org_id_settings_patch(orgId, xOrgId, body)

    suspend fun switch_org_api_organizations_org_id_switch_post(
        orgId: String,
        xOrgId: String? = null,
        body: RequestBody,
    ): Response<ResponseBody> = api.switch_org_api_organizations_org_id_switch_post(orgId, xOrgId, body)

    suspend fun create_quota_increase_request_api_quota_increase_request_post(
        body: RequestBody,
    ): Response<ResponseBody> = api.create_quota_increase_request_api_quota_increase_request_post(body)

    suspend fun get_quota_status_api_quota_status_get(): Response<ResponseBody> = api.get_quota_status_api_quota_status_get()

    suspend fun verify_quota_increase_api_quota_verify_get(): Response<ResponseBody> = api.verify_quota_increase_api_quota_verify_get()

    suspend fun service_health_api_service_health_get(): Response<ResponseBody> = api.service_health_api_service_health_get()

    suspend fun get_or_create_api_key_for_user_api_service_users_user_id_orgs_org_id_api_keys_post(
        userId: String,
        orgId: String,
        xServiceAPIKey: String? = null,
        body: RequestBody,
    ): Response<ResponseBody> = api.get_or_create_api_key_for_user_api_service_users_user_id_orgs_org_id_api_keys_post(userId, orgId, xServiceAPIKey, body)

    suspend fun delete_user_api_key_api_service_users_user_id_orgs_org_id_api_keys_key_name_delete(
        userId: String,
        orgId: String,
        keyName: String,
        xServiceAPIKey: String? = null,
    ): Response<ResponseBody> = api.delete_user_api_key_api_service_users_user_id_orgs_org_id_api_keys_key_name_delete(userId, orgId, keyName, xServiceAPIKey)

    suspend fun get_org_member_info_api_service_users_user_id_orgs_org_id_membership_get(
        userId: String,
        orgId: String,
        xServiceAPIKey: String? = null,
    ): Response<ResponseBody> = api.get_org_member_info_api_service_users_user_id_orgs_org_id_membership_get(userId, orgId, xServiceAPIKey)

    suspend fun create_secret_lookups_api_service_users_user_id_orgs_org_id_secret_lookups_post(
        userId: String,
        orgId: String,
        xServiceAPIKey: String? = null,
        body: RequestBody,
    ): Response<ResponseBody> = api.create_secret_lookups_api_service_users_user_id_orgs_org_id_secret_lookups_post(userId, orgId, xServiceAPIKey, body)

    suspend fun batch_get_shared_conversations_api_shared_conversations_get(
        ids: String? = null,
    ): Response<ResponseBody> = api.batch_get_shared_conversations_api_shared_conversations_get(ids)

    suspend fun batch_get_shared_events_api_shared_events_get(
        conversationId: String? = null,
        id: String? = null,
    ): Response<ResponseBody> = api.batch_get_shared_events_api_shared_events_get(conversationId, id)

    suspend fun count_shared_events_api_shared_events_count_get(
        conversationId: String? = null,
        kindEq: String? = null,
        timestampGte: String? = null,
        timestampLt: String? = null,
    ): Response<ResponseBody> = api.count_shared_events_api_shared_events_count_get(conversationId, kindEq, timestampGte, timestampLt)

    suspend fun search_shared_events_api_shared_events_search_get(
        conversationId: String? = null,
        kindEq: String? = null,
        timestampGte: String? = null,
        timestampLt: String? = null,
        sortOrder: String? = null,
        pageId: String? = null,
        limit: String? = null,
    ): Response<ResponseBody> = api.search_shared_events_api_shared_events_search_get(conversationId, kindEq, timestampGte, timestampLt, sortOrder, pageId, limit)

    suspend fun get_shared_event_api_shared_events_conversation_id_event_id_get(
        conversationId: String,
        eventId: String,
    ): Response<ResponseBody> = api.get_shared_event_api_shared_events_conversation_id_event_id_get(conversationId, eventId)

    suspend fun get_tool_catalog_api_tools_catalog_get(
        orgId: String? = null,
    ): Response<ResponseBody> = api.get_tool_catalog_api_tools_catalog_get(orgId)

    suspend fun get_user_app_settings_api_users_app_get(): Response<ResponseBody> = api.get_user_app_settings_api_users_app_get()

    suspend fun update_user_app_settings_api_users_app_post(
        body: RequestBody,
    ): Response<ResponseBody> = api.update_user_app_settings_api_users_app_post(body)

    suspend fun health_health_get(): Response<ResponseBody> = api.health_health_get()

    suspend fun azure_devops_events_integration_azure_devops_events_post(
        xOpenhandsWebhookSecret: String? = null,
        authorization: String? = null,
        xOpenhandsNotificationType: String? = null,
        body: RequestBody,
    ): Response<ResponseBody> = api.azure_devops_events_integration_azure_devops_events_post(xOpenhandsWebhookSecret, authorization, xOpenhandsNotificationType, body)

    suspend fun get_azure_devops_organizations_integration_azure_devops_organizations_get(
        orgId: String? = null,
    ): Response<ResponseBody> = api.get_azure_devops_organizations_integration_azure_devops_organizations_get(orgId)

    suspend fun reinstall_azure_devops_webhook_integration_azure_devops_reinstall_webhook_post(
        organization: String? = null,
        orgId: String? = null,
        body: RequestBody,
    ): Response<ResponseBody> = api.reinstall_azure_devops_webhook_integration_azure_devops_reinstall_webhook_post(organization, orgId, body)

    suspend fun get_azure_devops_resources_integration_azure_devops_resources_get(
        organization: String? = null,
        orgId: String? = null,
    ): Response<ResponseBody> = api.get_azure_devops_resources_integration_azure_devops_resources_get(organization, orgId)

    suspend fun uninstall_azure_devops_webhook_integration_azure_devops_uninstall_webhook_post(
        organization: String? = null,
        orgId: String? = null,
        body: RequestBody,
    ): Response<ResponseBody> = api.uninstall_azure_devops_webhook_integration_azure_devops_uninstall_webhook_post(organization, orgId, body)

    suspend fun bitbucket_dc_connection_events_integration_bitbucket_dc_connections_connection_id_events_post(
        connectionId: String,
        xHubSignature: String? = null,
        xEventKey: String? = null,
        xRequestId: String? = null,
        body: RequestBody,
    ): Response<ResponseBody> = api.bitbucket_dc_connection_events_integration_bitbucket_dc_connections_connection_id_events_post(connectionId, xHubSignature, xEventKey, xRequestId, body)

    suspend fun enroll_bitbucket_dc_webhook_integration_bitbucket_dc_enroll_webhook_post(
        orgId: String? = null,
        body: RequestBody,
    ): Response<ResponseBody> = api.enroll_bitbucket_dc_webhook_integration_bitbucket_dc_enroll_webhook_post(orgId, body)

    suspend fun reinstall_bitbucket_dc_webhook_integration_bitbucket_dc_reinstall_webhook_post(
        orgId: String? = null,
        body: RequestBody,
    ): Response<ResponseBody> = api.reinstall_bitbucket_dc_webhook_integration_bitbucket_dc_reinstall_webhook_post(orgId, body)

    suspend fun get_bitbucket_dc_resources_integration_bitbucket_dc_resources_get(
        orgId: String? = null,
    ): Response<ResponseBody> = api.get_bitbucket_dc_resources_integration_bitbucket_dc_resources_get(orgId)

    suspend fun uninstall_bitbucket_dc_webhook_integration_bitbucket_dc_uninstall_webhook_post(
        orgId: String? = null,
        body: RequestBody,
    ): Response<ResponseBody> = api.uninstall_bitbucket_dc_webhook_integration_bitbucket_dc_uninstall_webhook_post(orgId, body)

    suspend fun update_bitbucket_dc_webhook_id_integration_bitbucket_dc_webhook_id_patch(
        orgId: String? = null,
        body: RequestBody,
    ): Response<ResponseBody> = api.update_bitbucket_dc_webhook_id_integration_bitbucket_dc_webhook_id_patch(orgId, body)

    suspend fun bitbucket_events_integration_bitbucket_events_post(
        xHubSignature: String? = null,
        xEventKey: String? = null,
        xRequestUuid: String? = null,
        xHookUuid: String? = null,
        body: RequestBody,
    ): Response<ResponseBody> = api.bitbucket_events_integration_bitbucket_events_post(xHubSignature, xEventKey, xRequestUuid, xHookUuid, body)

    suspend fun github_events_integration_github_events_post(
        xHubSignature256: String? = null,
        xGithubEvent: String? = null,
        xGithubDelivery: String? = null,
        body: RequestBody,
    ): Response<ResponseBody> = api.github_events_integration_github_events_post(xHubSignature256, xGithubEvent, xGithubDelivery, body)

    suspend fun gitlab_events_integration_gitlab_events_post(
        xGitlabToken: String? = null,
        xOpenhandsWebhookId: String? = null,
        xOpenhandsUserId: String? = null,
        body: RequestBody,
    ): Response<ResponseBody> = api.gitlab_events_integration_gitlab_events_post(xGitlabToken, xOpenhandsWebhookId, xOpenhandsUserId, body)

    suspend fun reinstall_gitlab_webhook_integration_gitlab_reinstall_webhook_post(
        body: RequestBody,
    ): Response<ResponseBody> = api.reinstall_gitlab_webhook_integration_gitlab_reinstall_webhook_post(body)

    suspend fun get_gitlab_resources_integration_gitlab_resources_get(
        providerId: String? = null,
    ): Response<ResponseBody> = api.get_gitlab_resources_integration_gitlab_resources_get(providerId)

    suspend fun jira_callback_integration_jira_callback_get(
        code: String? = null,
        state: String? = null,
    ): Response<ResponseBody> = api.jira_callback_integration_jira_callback_get(code, state)

    suspend fun jira_events_integration_jira_events_post(
        xHubSignature: String? = null,
        body: RequestBody,
    ): Response<ResponseBody> = api.jira_events_integration_jira_events_post(xHubSignature, body)

    suspend fun create_jira_workspace_integration_jira_workspaces_post(
        orgId: String? = null,
        body: RequestBody,
    ): Response<ResponseBody> = api.create_jira_workspace_integration_jira_workspaces_post(orgId, body)

    suspend fun get_current_workspace_link_integration_jira_workspaces_link_get(): Response<ResponseBody> = api.get_current_workspace_link_integration_jira_workspaces_link_get()

    suspend fun create_workspace_link_integration_jira_workspaces_link_post(
        body: RequestBody,
    ): Response<ResponseBody> = api.create_workspace_link_integration_jira_workspaces_link_post(body)

    suspend fun get_jira_instance_status_integration_jira_workspaces_status_get(): Response<ResponseBody> = api.get_jira_instance_status_integration_jira_workspaces_status_get()

    suspend fun unlink_workspace_integration_jira_workspaces_unlink_post(
        body: RequestBody,
    ): Response<ResponseBody> = api.unlink_workspace_integration_jira_workspaces_unlink_post(body)

    suspend fun validate_workspace_integration_integration_jira_workspaces_validate_workspace_name_get(
        workspaceName: String,
    ): Response<ResponseBody> = api.validate_workspace_integration_integration_jira_workspaces_validate_workspace_name_get(workspaceName)

    suspend fun device_authorization_oauth_device_authorize_post(
        body: RequestBody,
    ): Response<ResponseBody> = api.device_authorization_oauth_device_authorize_post(body)

    suspend fun device_token_oauth_device_token_post(
        body: RequestBody,
    ): Response<ResponseBody> = api.device_token_oauth_device_token_post(body)

    suspend fun device_verification_authenticated_oauth_device_verify_authenticated_post(
        body: RequestBody,
    ): Response<ResponseBody> = api.device_verification_authenticated_oauth_device_verify_authenticated_post(body)

    suspend fun oauth_v2_list_git_providers_oauth_git_providers_get(): Response<ResponseBody> = api.oauth_v2_list_git_providers_oauth_git_providers_get()

    suspend fun oauth_v2_check_git_provider_oauth_git_providers_provider_id_check_post(
        providerId: String,
        body: RequestBody,
    ): Response<ResponseBody> = api.oauth_v2_check_git_provider_oauth_git_providers_provider_id_check_post(providerId, body)

    suspend fun github_dummy_callback_oauth_github_callback_get(): Response<ResponseBody> = api.github_dummy_callback_oauth_github_callback_get()

    suspend fun oauth_v2_idp_login_oauth_idp_login_get(
        redirectUrl: String? = null,
        mode: String? = null,
    ): Response<ResponseBody> = api.oauth_v2_idp_login_oauth_idp_login_get(redirectUrl, mode)

    suspend fun oauth_v2_list_idp_providers_oauth_idp_providers_get(): Response<ResponseBody> = api.oauth_v2_list_idp_providers_oauth_idp_providers_get()

    suspend fun idp_invite_form_oauth_idp_invite_get(
        token: String? = null,
        error: String? = null,
    ): Response<ResponseBody> = api.idp_invite_form_oauth_idp_invite_get(token, error)

    suspend fun idp_invite_accept_oauth_idp_invite_post(
        body: RequestBody,
    ): Response<ResponseBody> = api.idp_invite_accept_oauth_idp_invite_post(body)

    suspend fun idp_login_form_oauth_idp_login_get(
        redirectUrl: String? = null,
        error: String? = null,
    ): Response<ResponseBody> = api.idp_login_form_oauth_idp_login_get(redirectUrl, error)

    suspend fun idp_login_oauth_idp_login_post(
        body: RequestBody,
    ): Response<ResponseBody> = api.idp_login_oauth_idp_login_post(body)

    suspend fun idp_signup_form_oauth_idp_signup_get(
        redirectUrl: String? = null,
        error: String? = null,
    ): Response<ResponseBody> = api.idp_signup_form_oauth_idp_signup_get(redirectUrl, error)

    suspend fun idp_signup_oauth_idp_signup_post(
        body: RequestBody,
    ): Response<ResponseBody> = api.idp_signup_oauth_idp_signup_post(body)

    suspend fun keycloak_callback_oauth_keycloak_callback_get(
        code: String? = null,
        state: String? = null,
        error: String? = null,
        errorDescription: String? = null,
        kcActionStatus: String? = null,
    ): Response<ResponseBody> = api.keycloak_callback_oauth_keycloak_callback_get(code, state, error, errorDescription, kcActionStatus)

    suspend fun keycloak_offline_callback_oauth_keycloak_offline_callback_get(
        code: String? = null,
        state: String? = null,
    ): Response<ResponseBody> = api.keycloak_offline_callback_oauth_keycloak_offline_callback_get(code, state)

    suspend fun oauth_v2_list_providers_oauth_providers_get(): Response<ResponseBody> = api.oauth_v2_list_providers_oauth_providers_get()

    suspend fun oauth_v2_unlink_oauth_provider_id_delete(
        providerId: String,
    ): Response<ResponseBody> = api.oauth_v2_unlink_oauth_provider_id_delete(providerId)

    suspend fun oauth_v2_callback_oauth_provider_id_callback_get(
        providerId: String,
        code: String? = null,
        state: String? = null,
        error: String? = null,
    ): Response<ResponseBody> = api.oauth_v2_callback_oauth_provider_id_callback_get(providerId, code, state, error)

    suspend fun oauth_v2_link_oauth_provider_id_link_post(
        providerId: String,
        body: RequestBody,
    ): Response<ResponseBody> = api.oauth_v2_link_oauth_provider_id_link_post(providerId, body)

    suspend fun oauth_v2_login_oauth_provider_id_login_get(
        providerId: String,
        redirectUrl: String? = null,
        mode: String? = null,
    ): Response<ResponseBody> = api.oauth_v2_login_oauth_provider_id_login_get(providerId, redirectUrl, mode)

    suspend fun is_ready_ready_get(): Response<ResponseBody> = api.is_ready_ready_get()

    suspend fun is_saas_saas_get(): Response<ResponseBody> = api.is_saas_saas_get()

    suspend fun saml_acs_saml_provider_id_acs_post(
        providerId: String,
        body: RequestBody,
    ): Response<ResponseBody> = api.saml_acs_saml_provider_id_acs_post(providerId, body)

    suspend fun saml_login_saml_provider_id_login_get(
        providerId: String,
        redirectUrl: String? = null,
    ): Response<ResponseBody> = api.saml_login_saml_provider_id_login_get(providerId, redirectUrl)

    suspend fun saml_metadata_saml_provider_id_metadata_get(
        providerId: String,
    ): Response<ResponseBody> = api.saml_metadata_saml_provider_id_metadata_get(providerId)

    suspend fun get_server_info_server_info_get(): Response<ResponseBody> = api.get_server_info_server_info_get()

    suspend fun install_slack_install_get(
        state: String? = null,
    ): Response<ResponseBody> = api.install_slack_install_get(state)

    suspend fun install_callback_slack_install_callback_get(
        code: String? = null,
        state: String? = null,
        error: String? = null,
    ): Response<ResponseBody> = api.install_callback_slack_install_callback_get(code, state, error)

    suspend fun keycloak_callback_slack_keycloak_callback_get(
        code: String? = null,
        state: String? = null,
        error: String? = null,
    ): Response<ResponseBody> = api.keycloak_callback_slack_keycloak_callback_get(code, state, error)

    suspend fun link_callback_slack_link_callback_get(
        state: String? = null,
    ): Response<ResponseBody> = api.link_callback_slack_link_callback_get(state)

    suspend fun on_event_slack_on_event_post(
        body: RequestBody,
    ): Response<ResponseBody> = api.on_event_slack_on_event_post(body)

    suspend fun on_form_interaction_slack_on_form_interaction_post(
        body: RequestBody,
    ): Response<ResponseBody> = api.on_form_interaction_slack_on_form_interaction_post(body)

    suspend fun on_options_load_slack_on_options_load_post(
        body: RequestBody,
    ): Response<ResponseBody> = api.on_options_load_slack_on_options_load_post(body)

    suspend fun get_slack_status_slack_status_get(): Response<ResponseBody> = api.get_slack_status_slack_status_get()
}
