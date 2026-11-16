package com.example.data.api

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Adds the OpenHands Cloud credential. The V1 app server authenticates with
 * `Authorization: Bearer <api-key>`.
 */
class AuthInterceptor(private val apiKeyProvider: () -> String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val builder = chain.request().newBuilder()
            .header("User-Agent", "OpenHands-Android-Cloud/1.0")
            .header("Accept", "application/json")

        val key = apiKeyProvider().trim()
        if (key.isNotEmpty()) {
            builder.header("Authorization", "Bearer $key")
        }
        return chain.proceed(builder.build())
    }
}

/**
 * Adds the per-sandbox agent-server credential. Runtime calls authenticate with
 * `X-Session-API-Key`, not the Cloud Bearer token.
 */
class SessionAuthInterceptor(private val keyProvider: () -> String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val builder = chain.request().newBuilder()
            .header("User-Agent", "OpenHands-Android-Cloud/1.0")
            .header("Accept", "application/json")

        val key = keyProvider().trim()
        if (key.isNotEmpty()) {
            builder.header("X-Session-API-Key", key)
        }
        return chain.proceed(builder.build())
    }
}

object ApiClient {
    val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    /**
     * Builds a runtime client. The base URL is a placeholder because every call
     * passes a full `@Url` (the sandbox host differs per conversation).
     */
    fun createAgentServer(
        baseUrlProvider: () -> String,
        sessionKeyProvider: () -> String
    ): AgentServerApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(SessionAuthInterceptor(sessionKeyProvider))
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(normalizeBaseUrl(baseUrlProvider()))
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(AgentServerApiService::class.java)
    }

    private val agentServerCache = java.util.concurrent.ConcurrentHashMap<String, AgentServerApiService>()

    /**
     * Returns a cached runtime client bound to one conversation's host and
     * session key. The cache key includes the key itself so a rotated key
     * produces a fresh client rather than a stale credential.
     */
    fun agentServerFor(conversationUrl: String, sessionKey: String): AgentServerApiService {
        val base = runtimeOrigin(conversationUrl)
        val cacheKey = "$base|$sessionKey"
        return agentServerCache.getOrPut(cacheKey) {
            createAgentServer(
                baseUrlProvider = { base },
                sessionKeyProvider = { sessionKey }
            )
        }
    }

    fun create(
        baseUrlProvider: () -> String,
        apiKeyProvider: () -> String
    ): OpenHandsApiService {
        // Full body logging would leak conversation content into release logs.
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(apiKeyProvider))
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(normalizeBaseUrl(baseUrlProvider()))
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenHandsApiService::class.java)
    }

    /**
     * Typed client for the non-`/api/v1` Cloud surface (org admin, keys,
     * billing, analytics, quota, license, SSO/IdP, integrations, Slack, public
     * shares). Shares the Cloud Bearer credential with [create].
     */
    fun createCloudAdmin(
        baseUrlProvider: () -> String,
        apiKeyProvider: () -> String
    ): CloudAdminApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(apiKeyProvider))
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
        return Retrofit.Builder()
            .baseUrl(normalizeBaseUrl(baseUrlProvider()))
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(CloudAdminApiService::class.java)
    }

    fun normalizeBaseUrl(raw: String): String {
        var url = raw.trim()
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://$url"
        }
        if (!url.endsWith("/")) {
            url = "$url/"
        }
        return url
    }

    /**
     * Returns the scheme + authority (`scheme://host[:port]/`) of a URL,
     * discarding any path.
     *
     * OpenHands Cloud reports a conversation's `conversation_url` as
     * `<origin>/api/conversations/<id>`, but the agent-server exposes every
     * route from the host root (`/server_info`, `/api/git/changes`,
     * `/api/conversations/<id>`). Using the reported URL verbatim as the
     * Retrofit base therefore duplicates the path segment and 404s every
     * runtime call, so the runtime client must be rooted at the origin.
     */
    fun runtimeOrigin(raw: String): String {
        val normalized = normalizeBaseUrl(raw)
        return try {
            val uri = java.net.URI(normalized)
            val host = uri.host ?: return normalized
            val port = if (uri.port == -1) "" else ":${uri.port}"
            "${uri.scheme}://$host$port/"
        } catch (e: Exception) {
            normalized
        }
    }

    /** Extracts a human-readable message from a FastAPI error body. */
    fun errorMessage(response: retrofit2.Response<*>): String {
        val body = try {
            response.errorBody()?.string()
        } catch (e: Exception) {
            null
        }
        if (!body.isNullOrBlank()) {
            try {
                val json = JSONObject(body)
                val detail = json.opt("detail")
                if (detail != null && detail.toString() != "null") {
                    return "HTTP ${response.code()}: ${detail.toString().take(300)}"
                }
            } catch (_: Exception) {
                return "HTTP ${response.code()}: ${body.take(300)}"
            }
        }
        return "HTTP ${response.code()} ${response.message()}"
    }
}
