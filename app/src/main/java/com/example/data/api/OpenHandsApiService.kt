package com.example.data.api

import com.example.data.model.ConversationDto
import com.example.data.model.ConversationListDto
import com.example.data.model.CreditsDto
import com.example.data.model.CreateConversationRequestDto
import com.example.data.model.EventsPageDto
import com.example.data.model.SendMessageRequestDto
import com.example.data.model.ServiceHealthDto
import com.example.data.model.StartTaskDto
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
}
