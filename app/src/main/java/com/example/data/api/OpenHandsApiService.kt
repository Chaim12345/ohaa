package com.example.data.api

import com.example.data.model.CreateConversationRequest
import com.example.data.model.EventResponse
import com.example.data.model.SendMessageRequest
import com.example.data.model.ServiceHealth
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface OpenHandsApiService {

    @GET("api/service/health")
    suspend fun checkHealth(): Response<ServiceHealth>

    @POST("api/v1/app-conversations")
    suspend fun createConversation(
        @Body request: CreateConversationRequest
    ): Response<ResponseBody>

    @GET("api/v1/app-conversations")
    suspend fun listConversations(
        @Query("ids") ids: String? = null
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

    @GET("api/v1/conversation/{id}/events")
    suspend fun getEvents(
        @Path("id") id: String
    ): Response<ResponseBody>

    @DELETE("api/v1/app-conversations/{id}")
    suspend fun deleteConversation(
        @Path("id") id: String
    ): Response<ResponseBody>
}
