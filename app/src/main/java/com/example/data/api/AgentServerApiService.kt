package com.example.data.api

import com.example.data.model.AskAgentRequestDto
import com.example.data.model.ConfirmationResponseRequestDto
import com.example.data.model.ConversationRuntimeDto
import com.example.data.model.EventDto
import com.example.data.model.PageDto
import com.example.data.model.RuntimeConversationDto
import com.example.data.model.RuntimeSuccessDto
import com.example.data.model.SendMessageRequest
import com.example.data.model.StartGoalRequestDto
import com.example.data.model.SwitchLlmRequestDto
import com.example.data.model.UpdateRuntimeConversationRequestDto
import com.example.data.model.VSCodeUrlDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * The runtime Agent Server API.
 *
 * Agent Canvas drives two hosts: the Cloud App API (v1) for conversation
 * history and management, and each conversation's own runtime Agent Server for
 * live control. The runtime host and its ephemeral key come from the
 * conversation resource (`conversation_url`, `session_api_key`); requests are
 * authorised with `X-Session-API-Key` instead of the cloud bearer token.
 *
 * Endpoints mirror the agent server's routers (conversation, event, file,
 * git, bash, tool, vscode, workspace).
 */
interface AgentServerApiService {

    // ---- Conversations -------------------------------------------------------

    /** The local conversation record, including live `agent_state`. */
    @GET("api/conversations/{conversation_id}")
    suspend fun getConversation(
        @Path("conversation_id") conversationId: String
    ): Response<RuntimeConversationDto>

    @GET("api/conversations")
    suspend fun searchConversations(
        @Query("page_id") pageId: String? = null,
        @Query("limit") limit: Int = 50
    ): Response<PageDto<RuntimeConversationDto>>

    @PATCH("api/conversations/{conversation_id}")
    suspend fun updateConversation(
        @Path("conversation_id") conversationId: String,
        @Body body: UpdateRuntimeConversationRequestDto
    ): Response<RuntimeConversationDto>

    @DELETE("api/conversations/{conversation_id}")
    suspend fun deleteConversation(
        @Path("conversation_id") conversationId: String
    ): Response<RuntimeSuccessDto>

    /** Pause the agent loop. */
    @POST("api/conversations/{conversation_id}/pause")
    suspend fun pauseConversation(
        @Path("conversation_id") conversationId: String
    ): Response<RuntimeSuccessDto>

    /** Interrupt the running agent without ending the conversation. */
    @POST("api/conversations/{conversation_id}/interrupt")
    suspend fun interruptConversation(
        @Path("conversation_id") conversationId: String
    ): Response<RuntimeSuccessDto>

    /** Start (or restart) the agent run loop. */
    @POST("api/conversations/{conversation_id}/run")
    suspend fun runConversation(
        @Path("conversation_id") conversationId: String
    ): Response<RuntimeSuccessDto>

    /** Where this conversation's runtime lives (host + session key). */
    @GET("api/conversations/{conversation_id}/runtime")
    suspend fun getRuntime(
        @Path("conversation_id") conversationId: String
    ): Response<ConversationRuntimeDto>

    // ---- Goal loop -------------------------------------------------------------

    /** Start a goal loop (`/goal` in Canvas). */
    @POST("api/conversations/{conversation_id}/goal")
    suspend fun startGoal(
        @Path("conversation_id") conversationId: String,
        @Body body: StartGoalRequestDto
    ): Response<RuntimeSuccessDto>

    @POST("api/conversations/{conversation_id}/goal/stop")
    suspend fun stopGoal(
        @Path("conversation_id") conversationId: String
    ): Response<RuntimeSuccessDto>

    @POST("api/conversations/{conversation_id}/goal/resume")
    suspend fun resumeGoal(
        @Path("conversation_id") conversationId: String
    ): Response<RuntimeSuccessDto>

    // ---- Agent / model control --------------------------------------------------

    /** Ask the agent a side question without adding a turn to the transcript. */
    @POST("api/conversations/{conversation_id}/ask_agent")
    suspend fun askAgent(
        @Path("conversation_id") conversationId: String,
        @Body body: AskAgentRequestDto
    ): Response<RuntimeSuccessDto>

    /** Switch the LLM of a running conversation. */
    @POST("api/conversations/{conversation_id}/switch_llm")
    suspend fun switchLlm(
        @Path("conversation_id") conversationId: String,
        @Body body: SwitchLlmRequestDto
    ): Response<RuntimeSuccessDto>

    /** Trigger a condensation pass on the conversation history. */
    @POST("api/conversations/{conversation_id}/condense")
    suspend fun condenseConversation(
        @Path("conversation_id") conversationId: String
    ): Response<RuntimeSuccessDto>

    /** Fork this conversation at an event, producing a new conversation. */
    @POST("api/conversations/{conversation_id}/fork")
    suspend fun forkConversation(
        @Path("conversation_id") conversationId: String,
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): Response<RuntimeConversationDto>

    // ---- Events ------------------------------------------------------------------

    /** Runtime event stream (live, not the persisted cloud history). */
    @GET("api/conversations/{conversation_id}/events/search")
    suspend fun searchEvents(
        @Path("conversation_id") conversationId: String,
        @Query("page_id") pageId: String? = null,
        @Query("limit") limit: Int = 100,
        @Query("kind") kind: String? = null
    ): Response<PageDto<EventDto>>

    @GET("api/conversations/{conversation_id}/events/count")
    suspend fun countEvents(
        @Path("conversation_id") conversationId: String
    ): Response<Int>

    @GET("api/conversations/{conversation_id}/events/{event_id}")
    suspend fun getEvent(
        @Path("conversation_id") conversationId: String,
        @Path("event_id") eventId: String
    ): Response<EventDto>

    /** Send a message straight to the runtime (used when the cloud is unreachable). */
    @POST("api/conversations/{conversation_id}/events")
    suspend fun sendMessage(
        @Path("conversation_id") conversationId: String,
        @Body body: SendMessageRequest
    ): Response<RuntimeSuccessDto>

    /** Accept or reject a pending agent action (Canvas "Confirm"/"Reject"). */
    @POST("api/conversations/{conversation_id}/events/respond_to_confirmation")
    suspend fun respondToConfirmation(
        @Path("conversation_id") conversationId: String,
        @Body body: ConfirmationResponseRequestDto
    ): Response<RuntimeSuccessDto>

    // ---- Workspace / IDE ----------------------------------------------------------

    /** URL for the conversation's embedded VSCode server, if one is running. */
    @GET("api/vscode/url")
    suspend fun vscodeUrl(): Response<VSCodeUrlDto>

    @GET("api/vscode/status")
    suspend fun vscodeStatus(): Response<RuntimeSuccessDto>

    /** Download a workspace file's raw bytes from the runtime. */
    @GET("api/conversations/{conversation_id}/workspace/{file_path}")
    suspend fun readWorkspaceFile(
        @Path("conversation_id") conversationId: String,
        @Path("file_path") filePath: String
    ): Response<ResponseBody>
}
