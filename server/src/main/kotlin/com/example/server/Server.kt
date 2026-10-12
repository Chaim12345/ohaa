package com.example.server

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.staticFiles
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.httpMethod
import io.ktor.server.request.receiveText
import io.ktor.server.request.uri
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import java.io.File

/**
 * Backend for the OpenHands Cloud web client.
 *
 * Two responsibilities:
 *  1. Serve the compiled Compose/Wasm app from the same origin, so the browser
 *     bundle needs no CORS negotiation with app.all-hands.dev.
 *  2. Proxy `/cloud/...` to the real OpenHands Cloud API, injecting the bearer
 *     token server-side. The key never reaches the browser, and the wasm client
 *     calls exactly one origin: this server.
 *
 * Nothing here fabricates responses — a request the upstream rejects is returned
 * to the caller with the upstream status and body intact.
 */
fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    embeddedServer(Netty, port = port, host = "0.0.0.0") { module() }.start(wait = true)
}

private val upstreamBase: String =
    System.getenv("OPENHANDS_BASE_URL")?.trimEnd('/') ?: "https://app.all-hands.dev"

/** Server-side default token; a per-request `X-OpenHands-Key` header overrides it. */
private val defaultApiKey: String? = System.getenv("OPENHANDS_API_KEY")

/** Directory holding the wasm distribution; defaults to the Gradle build output. */
private val webRoot: File =
    File(System.getenv("WEB_ROOT") ?: "web/build/dist/wasmJs/productionExecutable")

fun Application.module() {
    val proxy = HttpClient()

    install(CallLogging)
    install(CORS) {
        anyHost()
        allowHeaders { true }
        allowMethod(HttpMethod.Options)
    }
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respondText(
                text = """{"detail":${JsonPrimitive(cause.message ?: "proxy error")}}""",
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.BadGateway
            )
        }
    }

    routing {
        // Liveness for the browser bundle and any external health check.
        get("/health") {
            call.respondText("""{"status":"ok"}""", ContentType.Application.Json)
        }

        // Session bootstrap: tells the client whether a server-side key exists,
        // so it knows if it must ask the user for one.
        get("/api/session") {
            val payload = buildJsonObject {
                put("hasServerKey", JsonPrimitive(defaultApiKey?.isNotBlank() == true))
                put("upstream", JsonPrimitive(upstreamBase))
            }
            call.respondText(payload.toString(), ContentType.Application.Json)
        }

        // Everything under the cloud prefix is forwarded to OpenHands Cloud verbatim.
        route("/cloud/{proxyPath...}") {
            handle {
                // Derive the suffix from the raw URI rather than the route
                // parameter: a caller-supplied query parameter named `path`
                // would otherwise collide with the `path` tailcard, since Ktor
                // merges query and path parameters.
                val uri = call.request.uri
                val suffix = uri.substringBefore('?').removePrefix("/cloud").trimStart('/')
                val query = uri.substringAfter('?', "")
                val target = buildString {
                    append(upstreamBase).append('/').append(suffix)
                    if (query.isNotEmpty()) append('?').append(query)
                }

                val apiKey = call.request.headers["X-OpenHands-Key"]?.takeIf { it.isNotBlank() }
                    ?: defaultApiKey
                if (apiKey.isNullOrBlank()) {
                    call.respondText(
                        text = """{"detail":"No API key. Set OPENHANDS_API_KEY on the server or provide one in Settings."}""",
                        contentType = ContentType.Application.Json,
                        status = HttpStatusCode.Unauthorized
                    )
                    return@handle
                }

                val method = HttpMethod.parse(call.request.httpMethod.value)
                val body = if (method == HttpMethod.Get || method == HttpMethod.Head) {
                    null
                } else {
                    call.receiveText().takeIf { it.isNotBlank() }
                }

                val upstream = proxy.request(target) {
                    this.method = method
                    header("Authorization", "Bearer $apiKey")
                    header("Accept", "application/json")
                    body?.let {
                        contentType(ContentType.Application.Json)
                        setBody(it)
                    }
                }

                call.respondBytes(
                    bytes = upstream.readRawBytes(),
                    contentType = upstream.contentType(),
                    status = upstream.status
                )
            }
        }

        // The compiled Compose/Wasm bundle, served from the same origin.
        staticFiles("/", webRoot)
    }
}
