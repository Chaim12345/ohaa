package com.example.data.api

import com.example.data.model.AgentProfileDto
import com.example.data.model.BranchDto
import com.example.data.model.ConversationDto
import com.example.data.model.ConversationListDto
import com.example.data.model.CreditsDto
import com.example.data.model.CreateConversationRequestDto
import com.example.data.model.EventsPageDto
import com.example.data.model.GitChangeDto
import com.example.data.model.GitDiffDto
import com.example.data.model.GitOrganizationsDto
import com.example.data.model.PageDto
import com.example.data.model.RepositoryDto
import com.example.data.model.SendMessageRequestDto
import com.example.data.model.ServiceHealthDto
import com.example.data.model.StartTaskDto
import com.example.data.model.SuccessDto
import com.example.data.model.SwitchAcpModelRequest
import com.example.data.model.SwitchProfileRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Typed contract for the OpenHands Cloud API — see docs/API_SPEC.md.
 */
interface OpenHandsApiService {

    @GET("api/service/health")
    suspend fun checkHealth(): Response<ServiceHealthDto>

    @POST("api/v1/app-conversations")
    suspend fun createConversation(
        @Body request: CreateConversationRequestDto
    ): Response<StartTaskDto>

    @GET("api/v1/app-conversations/start-tasks")
    suspend fun getStartTasks(
        @Query("ids") ids: String
    ): Response<List<StartTaskDto>>

    @GET("api/v1/app-conversations")
    suspend fun listConversations(
        @Query("ids") ids: String? = null
    ): Response<List<ConversationDto>>

    @GET("api/v1/app-conversations/search")
    suspend fun searchConversations(
        @Query("q") query: String
    ): Response<ConversationListDto>

    @POST("api/v1/app-conversations/{id}/send-message")
    suspend fun sendMessage(
        @Path("id") conversationId: String,
        @Body request: SendMessageRequestDto
    ): Response<Unit>

    @GET("api/v1/conversation/{id}/events/search")
    suspend fun searchEvents(
        @Path("id") conversationId: String
    ): Response<EventsPageDto>

    @DELETE("api/v1/app-conversations/{id}")
    suspend fun deleteConversation(
        @Path("id") conversationId: String
    ): Response<Unit>

    @GET("api/billing/credits")
    suspend fun getCredits(): Response<CreditsDto>

    // ------------------------------------------------------------------- git

    /** List repositories from the configured upstream git provider (GitHub etc.). */
    @GET("api/v1/git/repositories/search")
    suspend fun searchRepositories(
        @Query("query") query: String? = null,
        @Query("provider") provider: String? = null,
        @Query("installation_id") installationId: String? = null,
        @Query("sort_order") sortOrder: String? = null,
        @Query("page_id") pageId: String? = null,
        @Query("limit") limit: Int = 30
    ): Response<PageDto<RepositoryDto>>

    /** Branches for a repository (provider and repo are required). */
    @GET("api/v1/git/branches/search")
    suspend fun searchBranches(
        @Query("repository") repository: String,
        @Query("provider") provider: String,
        @Query("query") query: String = "",
        @Query("page_id") pageId: String? = null,
        @Query("limit") limit: Int = 30
    ): Response<PageDto<BranchDto>>

    /** Installation ids of the OpenHands app for a provider (GitHub App). */
    @GET("api/v1/git/installations/search")
    suspend fun searchInstallations(
        @Query("provider") provider: String,
        @Query("page_id") pageId: String? = null,
        @Query("limit") limit: Int = 50
    ): Response<PageDto<String>>

    @GET("api/v1/users/git-organizations")
    suspend fun gitOrganizations(): Response<GitOrganizationsDto>

    // ------------------------------------------------------- conversation git

    /** Working-tree changes for the conversation's sandbox. */
    @GET("api/v1/app-conversations/{conversation_id}/git/changes")
    suspend fun gitChanges(
        @Path("conversation_id") conversationId: String,
        @Query("path") repoPath: String? = null
    ): Response<GitDiffDto>

    /** Unified diff for the conversation's sandbox. */
    @GET("api/v1/app-conversations/{conversation_id}/git/diff")
    suspend fun gitDiff(
        @Path("conversation_id") conversationId: String,
        @Query("path") repoPath: String? = null
    ): Response<GitDiffDto>

    // -------------------------------------------------- agent / ACP switching

    /** Saved LLM profiles switchable onto a running conversation. */
    @GET("api/v1/settings/profiles")
    suspend fun listProfiles(): Response<List<AgentProfileDto>>

    /** Switch the LLM profile used by a running conversation. */
    @POST("api/v1/app-conversations/{conversation_id}/switch_profile")
    suspend fun switchProfile(
        @Path("conversation_id") conversationId: String,
        @Body request: SwitchProfileRequest
    ): Response<SuccessDto>

    /** For ACP-backed conversations: switch the model exposed by the ACP server. */
    @POST("api/v1/app-conversations/{conversation_id}/switch_acp_model")
    suspend fun switchAcpModel(
        @Path("conversation_id") conversationId: String,
        @Body request: SwitchAcpModelRequest
    ): Response<SuccessDto>
}
