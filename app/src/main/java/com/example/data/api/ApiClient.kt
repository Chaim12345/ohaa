package com.example.data.api

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Small settings store (SharedPreferences). Keeping this out of the repository
 * makes config swappable and testable.
 */
class ApiSettings(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("openhands_prefs", Context.MODE_PRIVATE)

    var apiKey: String
        get() {
            val key = prefs.getString(KEY_API, BuildConfig.OPENHANDS_API_KEY) ?: ""
            return if (key == "OPENHANDS_API_KEY_PLACEHOLDER") "" else key
        }
        set(value) = prefs.edit().putString(KEY_API, value.trim()).apply()

    var baseUrl: String
        get() = prefs.getString(KEY_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
        set(value) = prefs.edit().putString(KEY_URL, value.trim().ifEmpty { DEFAULT_BASE_URL }).apply()

    fun hasApiKey(): Boolean = apiKey.isNotBlank()

    companion object {
        const val DEFAULT_BASE_URL = "https://app.all-hands.dev"
        private const val KEY_API = "api_key"
        private const val KEY_URL = "base_url"
    }
}

class AuthInterceptor(private val apiKeyProvider: () -> String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val builder = chain.request().newBuilder()
            .header("User-Agent", "OpenHands-Canvas-Android/2.0")
            .header("Accept", "application/json")

        // The documented scheme (OpenAPI securitySchemes.APIKeyHeader).
        // `Authorization: Bearer` is also accepted server-side, but the spec
        // declares this header, so it is the one we send.
        val key = apiKeyProvider().trim()
        if (key.isNotEmpty()) {
            builder.header("X-Access-Token", key)
        }
        return chain.proceed(builder.build())
    }
}

object ApiClient {

    val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    fun normalizeBaseUrl(raw: String): String {
        var url = raw.trim()
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://$url"
        }
        if (!url.endsWith("/")) url = "$url/"
        return url
    }

    fun create(
        baseUrlProvider: () -> String,
        apiKeyProvider: () -> String,
        enableLogging: Boolean = true
    ): OpenHandsApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = if (enableLogging) {
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

    @Volatile
    private var cached: Pair<Pair<String, String>, OpenHandsApiService>? = null

    /**
     * Returns a shared service for the current config, rebuilding only when the
     * base URL or API key changes. Creating a Retrofit/OkHttpClient per call
     * would leak connection pools and dispatcher threads under polling.
     */
    fun forConfig(baseUrlProvider: () -> String, apiKeyProvider: () -> String): OpenHandsApiService {
        val config = normalizeBaseUrl(baseUrlProvider()) to apiKeyProvider().trim()
        cached?.let { if (it.first == config) return it.second }
        synchronized(this) {
            cached?.let { if (it.first == config) return it.second }
            val service = create({ config.first }, { config.second }, enableLogging = false)
            cached = config to service
            return service
        }
    }
}
