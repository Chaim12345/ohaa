package com.example.data.api

import com.example.data.model.ApiErrorDto
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Raised for any non-2xx response. [code] is the HTTP status; [message] is user-presentable. */
class ApiException(val code: Int, message: String, val rawBody: String? = null) : IOException(message) {
    val isAuthError: Boolean get() = code == 401 || code == 403
}

/**
 * Adds the OpenHands Cloud credentials.
 * The OpenAPI security scheme is `X-Access-Token`; the public docs also show `Authorization: Bearer`.
 * Both are sent so either gateway accepts the request.
 */
class AuthInterceptor(private val apiKeyProvider: () -> String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val builder = chain.request().newBuilder()
            .header("User-Agent", "OpenHands-Android/1.0")
            .header("Accept", "application/json")
        val key = apiKeyProvider().trim()
        if (key.isNotEmpty()) {
            builder.header("X-Access-Token", key)
            builder.header("Authorization", "Bearer $key")
        }
        return chain.proceed(builder.build())
    }
}

object ApiClient {
    val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    fun normalizeBaseUrl(raw: String): String {
        var url = raw.trim().ifEmpty { DEFAULT_BASE_URL }
        if (!url.startsWith("http://") && !url.startsWith("https://")) url = "https://$url"
        if (!url.endsWith("/")) url += "/"
        return url
    }

    fun create(
        baseUrl: String,
        apiKeyProvider: () -> String,
        enableBodyLogging: Boolean
    ): OpenHandsApiService {
        val clientBuilder = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(apiKeyProvider))
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)

        if (enableBodyLogging) {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
                redactHeader("X-Access-Token")
                redactHeader("Authorization")
                redactHeader("X-Session-API-Key")
            }
            clientBuilder.addInterceptor(logging)
        }

        return Retrofit.Builder()
            .baseUrl(normalizeBaseUrl(baseUrl))
            .client(clientBuilder.build())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenHandsApiService::class.java)
    }

    /** Converts an error body into a readable message. */
    fun describeError(code: Int, rawBody: String?): String {
        val parsed = try {
            rawBody?.takeIf { it.isNotBlank() }?.let { moshi.adapter(ApiErrorDto::class.java).fromJson(it) }
        } catch (_: Exception) {
            null
        }
        val serverMessage: String? = parsed?.let { err ->
            when (val d = err.detail) {
                is String -> d
                is List<*> -> d.mapNotNull { item ->
                    (item as? Map<*, *>)?.let { m ->
                        val loc = (m["loc"] as? List<*>)?.joinToString(".") ?: ""
                        val msg = m["msg"] as? String ?: ""
                        if (loc.isNotEmpty()) "$loc: $msg" else msg
                    }
                }.joinToString("; ").ifBlank { null }
                is Map<*, *> -> d["message"] as? String ?: d.toString()
                else -> null
            } ?: err.message ?: err.error
        }
        return when (code) {
            401 -> "Not authenticated (401). Check your OpenHands API key in Settings." +
                (serverMessage?.let { " Server said: $it" } ?: "")
            403 -> "Forbidden (403). ${serverMessage ?: "This key does not have access to that resource."}"
            404 -> "Not found (404). ${serverMessage ?: "The resource no longer exists."}"
            409 -> "Conflict (409). ${serverMessage ?: ""}".trim()
            422 -> "Invalid request (422). ${serverMessage ?: ""}".trim()
            429 -> "Rate limited (429). ${serverMessage ?: "Please wait and try again."}"
            in 500..599 -> "OpenHands Cloud error ($code). ${serverMessage ?: ""}".trim()
            else -> "Request failed ($code). ${serverMessage ?: ""}".trim()
        }
    }

    const val DEFAULT_BASE_URL = "https://app.all-hands.dev"
}

/** Unwraps a Retrofit response or throws [ApiException] with a friendly message. */
fun <T> retrofit2.Response<T>.bodyOrThrow(): T {
    if (isSuccessful) {
        @Suppress("UNCHECKED_CAST")
        return body() ?: (Unit as T)
    }
    val raw = try { errorBody()?.string() } catch (_: Exception) { null }
    throw ApiException(code(), ApiClient.describeError(code(), raw), raw)
}

/** Like [bodyOrThrow] but tolerates an empty 2xx body. */
fun <T> retrofit2.Response<T>.bodyOrNullOrThrow(): T? {
    if (isSuccessful) return body()
    val raw = try { errorBody()?.string() } catch (_: Exception) { null }
    throw ApiException(code(), ApiClient.describeError(code(), raw), raw)
}
