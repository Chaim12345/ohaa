package com.example.data.api

import com.example.data.model.ConversationResponse
import com.example.data.model.EventResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class AuthInterceptor(private val apiKeyProvider: () -> String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val builder = original.newBuilder()
            .header("User-Agent", "OpenHands-Android-Cloud/1.0")
            .header("Accept", "application/json")

        val key = apiKeyProvider().trim()
        if (key.isNotEmpty()) {
            builder.header("Authorization", "Bearer $key")
        }

        return chain.proceed(builder.build())
    }
}

object ApiClient {
    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    fun create(
        baseUrlProvider: () -> String,
        apiKeyProvider: () -> String
    ): OpenHandsApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(apiKeyProvider))
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        var sanitizedUrl = baseUrlProvider().trim()
        if (!sanitizedUrl.startsWith("http://") && !sanitizedUrl.startsWith("https://")) {
            sanitizedUrl = "https://$sanitizedUrl"
        }
        if (!sanitizedUrl.endsWith("/")) {
            sanitizedUrl = "$sanitizedUrl/"
        }

        return Retrofit.Builder()
            .baseUrl(sanitizedUrl)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenHandsApiService::class.java)
    }

    fun parseConversation(rawJson: String): ConversationResponse? {
        return try {
            val adapter = moshi.adapter(ConversationResponse::class.java)
            adapter.fromJson(rawJson)
        } catch (e: Exception) {
            null
        }
    }

    fun parseConversationList(rawJson: String): List<ConversationResponse> {
        val list = mutableListOf<ConversationResponse>()
        try {
            val trimmed = rawJson.trim()
            val adapter = moshi.adapter(ConversationResponse::class.java)
            if (trimmed.startsWith("[")) {
                val jsonArray = JSONArray(trimmed)
                for (i in 0 until jsonArray.length()) {
                    val itemStr = jsonArray.getJSONObject(i).toString()
                    adapter.fromJson(itemStr)?.let { list.add(it) }
                }
            } else if (trimmed.startsWith("{")) {
                val jsonObject = JSONObject(trimmed)
                val array = when {
                    jsonObject.has("conversations") -> jsonObject.getJSONArray("conversations")
                    jsonObject.has("items") -> jsonObject.getJSONArray("items")
                    jsonObject.has("results") -> jsonObject.getJSONArray("results")
                    else -> null
                }
                if (array != null) {
                    for (i in 0 until array.length()) {
                        val itemStr = array.getJSONObject(i).toString()
                        adapter.fromJson(itemStr)?.let { list.add(it) }
                    }
                } else {
                    // Single conversation object
                    adapter.fromJson(trimmed)?.let { list.add(it) }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun parseEventList(rawJson: String): List<EventResponse> {
        val list = mutableListOf<EventResponse>()
        try {
            val trimmed = rawJson.trim()
            val adapter = moshi.adapter(EventResponse::class.java)
            if (trimmed.startsWith("[")) {
                val jsonArray = JSONArray(trimmed)
                for (i in 0 until jsonArray.length()) {
                    val itemStr = jsonArray.getJSONObject(i).toString()
                    adapter.fromJson(itemStr)?.let { list.add(it) }
                }
            } else if (trimmed.startsWith("{")) {
                val jsonObject = JSONObject(trimmed)
                val array = when {
                    jsonObject.has("events") -> jsonObject.getJSONArray("events")
                    jsonObject.has("items") -> jsonObject.getJSONArray("items")
                    else -> null
                }
                if (array != null) {
                    for (i in 0 until array.length()) {
                        val itemStr = array.getJSONObject(i).toString()
                        adapter.fromJson(itemStr)?.let { list.add(it) }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}
