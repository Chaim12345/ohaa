package com.example.data.repository

import com.example.data.api.ApiClient
import com.example.data.api.EndpointCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * A thin, generic caller used by the API Explorer screen so that *every*
 * endpoint in [EndpointCatalog] has a reachable UI home — including the
 * server-to-server callbacks and the OpenAI-compatible surface that have no
 * bespoke screen.
 *
 * It deliberately bypasses Retrofit: the explorer needs to hit an arbitrary
 * method + path at runtime, which typed interfaces cannot express. Auth headers
 * match the two surfaces (Cloud Bearer vs. sandbox session key).
 */
class ApiExplorerRepository {

    data class Result(
        val ok: Boolean,
        val code: Int,
        val body: String,
        val error: String? = null,
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    val cloudPaths: List<String> get() = EndpointCatalog.CLOUD_V1.sorted()
    val cloudAdminPaths: List<String> get() = EndpointCatalog.CLOUD_EXTRA.sorted()
    val agentPaths: List<String> get() = EndpointCatalog.AGENT_SERVER.sorted()

    /**
     * @param surface `"cloud"` or `"agent"`.
     * @param path a catalog path, optionally with `{}` placeholders already
     *   substituted by the caller and a query string appended.
     */
    suspend fun call(
        surface: String,
        method: String,
        baseUrl: String,
        credential: String,
        path: String,
        body: String?,
    ): Result = withContext(Dispatchers.IO) {
        val base = ApiClient.normalizeBaseUrl(baseUrl).trimEnd('/')
        val url = base + if (path.startsWith("/")) path else "/$path"
        val media = "application/json; charset=utf-8".toMediaType()
        val builder = Request.Builder().url(url).header("Accept", "application/json")
        when (surface) {
            "agent" -> builder.header("X-Session-API-Key", credential.trim())
            else -> if (credential.isNotBlank()) builder.header("Authorization", "Bearer ${credential.trim()}")
        }
        val payload = body?.takeIf { it.isNotBlank() }
        val request = when (method.uppercase()) {
            "GET" -> builder.get().build()
            "DELETE" -> builder.delete().build()
            "POST" -> builder.post((payload ?: "{}").toRequestBody(media)).build()
            "PUT" -> builder.put((payload ?: "{}").toRequestBody(media)).build()
            "PATCH" -> builder.patch((payload ?: "{}").toRequestBody(media)).build()
            else -> return@withContext Result(false, 0, "", "Unsupported method $method")
        }
        try {
            client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                Result(response.isSuccessful, response.code, text)
            }
        } catch (e: Exception) {
            Result(false, 0, "", e.message ?: e.javaClass.simpleName)
        }
    }
}
