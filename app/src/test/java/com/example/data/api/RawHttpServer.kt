package com.example.data.api

import java.io.BufferedInputStream
import java.net.ServerSocket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Tiny single-connection HTTP/1.1 server for tests. Unlike MockWebServer it
 * records the raw method token, so a test can prove that a request really went
 * out as `GET` while carrying a body (MockWebServer normalizes nothing, but
 * OkHttp's builder refuses to create such a request at all — [RawHttpClient]
 * is the only way the app can produce it).
 */
class RawHttpServer(
    private val status: Int = 200,
    private val contentType: String = "application/json",
    private val responseBody: String = "{}",
) {
    data class Request(val method: String, val path: String, val headers: Map<String, String>, val body: String)

    private val server = ServerSocket(0)
    private val latch = CountDownLatch(1)
    @Volatile private var captured: Request? = null

    private val thread = Thread {
        try {
            server.accept().use { socket ->
                val input = BufferedInputStream(socket.getInputStream())
                val requestLine = readLine(input).orEmpty()
                val parts = requestLine.split(' ')
                val headers = LinkedHashMap<String, String>()
                while (true) {
                    val line = readLine(input) ?: break
                    if (line.isEmpty()) break
                    val idx = line.indexOf(':')
                    if (idx > 0) headers[line.substring(0, idx).trim().lowercase()] = line.substring(idx + 1).trim()
                }
                val body = headers["content-length"]?.let { length ->
                    val bytes = ByteArray(length.toInt())
                    var read = 0
                    while (read < length.toInt()) {
                        val n = input.read(bytes, read, length.toInt() - read)
                        if (n == -1) break
                        read += n
                    }
                    String(bytes, 0, read)
                }.orEmpty()
                captured = Request(parts.getOrElse(0) { "" }, parts.getOrElse(1) { "" }, headers, body)

                val reason = if (status in 200..299) "OK" else "Error"
                val payload = responseBody.toByteArray(Charsets.UTF_8)
                val response = buildString {
                    append("HTTP/1.1 $status $reason\r\n")
                    append("Content-Type: $contentType\r\n")
                    append("Content-Length: ${payload.size}\r\n")
                    append("Connection: close\r\n\r\n")
                }
                socket.getOutputStream().apply {
                    write(response.toByteArray(Charsets.ISO_8859_1))
                    write(payload)
                    flush()
                }
            }
        } catch (_: Exception) {
        } finally {
            latch.countDown()
        }
    }.apply { isDaemon = true; start() }

    fun baseUrl(): String = "http://127.0.0.1:${server.localPort}"

    fun awaitRequest(timeoutSeconds: Long = 10): Request {
        latch.await(timeoutSeconds, TimeUnit.SECONDS)
        return captured ?: error("No request captured")
    }

    fun stop() {
        runCatching { server.close() }
    }

    private fun readLine(input: BufferedInputStream): String? {
        val sb = StringBuilder()
        while (true) {
            val b = input.read()
            if (b == -1) return if (sb.isEmpty()) null else sb.toString()
            if (b == '\n'.code) return sb.toString().removeSuffix("\r")
            sb.append(b.toChar())
        }
    }
}
