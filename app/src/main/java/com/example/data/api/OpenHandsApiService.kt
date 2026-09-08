package com.example.data.api

import com.example.data.model.CreateConversationRequest
import com.example.data.model.SendMessageRequest
import com.example.data.model.ServiceHealth
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface OpenHandsApiService {

    @GET("api/service/health")
    suspend fun checkHealth(): Response<ServiceHealth>

    // V1 Start conversation task
    @POST("api/v1/app-conversations")
    suspend fun createConversation(
        @Body request: CreateConversationRequest
    ): Response<ResponseBody>

    // V1 Check start task status
    @GET("api/v1/app-conversations/start-tasks")
    suspend fun getStartTaskStatus(
        @Query("ids") ids: String
    ): Response<ResponseBody>

    // V1 List conversations by ids
    @GET("api/v1/app-conversations")
    suspend fun listConversations(
        @Query("ids") ids: String? = null
    ): Response<ResponseBody>

    // V1 Search / List all conversations
    @GET("api/v1/app-conversations/search")
    suspend fun searchConversations(
        @Query("limit") limit: Int = 50,
        @Query("page_id") pageId: String? = null
    ): Response<ResponseBody>

    @GET("api/v1/app-conversations/{id}")
    suspend fun getConversation(
        @Path("id") id: String
    ): Response<ResponseBody>

    @POST("api/v1/app-conversations/{id}/send-message")
    suspend fun sendMessage(
        @Path("id") id: String,
        @Body request: SendMessageRequest
    ): Response<ResponseBody>

    @DELETE("api/v1/app-conversations/{id}")
    suspend fun deleteConversation(
        @Path("id") id: String
    ): Response<ResponseBody>

    // Event Endpoints: Search events
    @GET("api/v1/conversation/{conversation_id}/events/search")
    suspend fun searchEvents(
        @Path("conversation_id") conversationId: String,
        @Query("kind__eq") kind: String? = null,
        @Query("sort_order") sortOrder: String = "TIMESTAMP",
        @Query("page_id") pageId: String? = null,
        @Query("limit") limit: Int = 100
    ): Response<ResponseBody>

    // Legacy or Direct events fallback
    @GET("api/v1/conversation/{conversation_id}/events")
    suspend fun getEvents(
        @Path("conversation_id") conversationId: String
    ): Response<ResponseBody>

    // Count events
    @GET("api/v1/conversation/{conversation_id}/events/count")
    suspend fun countEvents(
        @Path("conversation_id") conversationId: String
    ): Response<ResponseBody>

    // Batch get events
    @GET("api/v1/conversation/{conversation_id}/events/batch")
    suspend fun batchGetEvents(
        @Path("conversation_id") conversationId: String,
        @Query("ids") ids: String
    ): Response<ResponseBody>
}
