package com.example.data.api

import java.io.BufferedInputStream
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import java.util.zip.GZIPInputStream

/**
 * Minimal HTTP/1.1 client for the one request shape no mainstream client will
 * send: a `GET` with a request body. OkHttp refuses it (both `Request.Builder`
 * and its HTTP codec), and `HttpURLConnection` silently rewrites the method to
 * POST. The agent-server spec nevertheless defines
 * `GET /api/bash/bash_events/` with a JSON array body, and the live server
 * rejects any other method (405) — so the request is written on a raw socket.
 *
 * Supports http and https, Content-Length and chunked responses, and gzip.
 */
object RawHttpClient {

    data class Response(val code: Int, val body: String, val headers: Map<String, String>)

    fun request(
        url: String,
        method: String,
        headers: Map<String, String>,
        body: ByteArray?,
        connectTimeoutMs: Int = 30_000,
        readTimeoutMs: Int = 120_000,
    ): Response {
        val uri = URI(url)
        val secure = uri.scheme.equals("https", ignoreCase = true)
        val port = if (uri.port != -1) uri.port else if (secure) 443 else 80
        val path = buildString {
            append(uri.rawPath?.ifEmpty { "/" } ?: "/")
            uri.rawQuery?.let { append('?').append(it) }
        }

        val socket: Socket = if (secure) {
            val ssl = SSLSocketFactory.getDefault().createSocket() as SSLSocket
            ssl.connect(InetSocketAddress(uri.host, port), connectTimeoutMs)
            val params = ssl.sslParameters
            params.endpointIdentificationAlgorithm = "HTTPS"
            ssl.sslParameters = params
            ssl.startHandshake()
            ssl
        } else {
            Socket().apply { connect(InetSocketAddress(uri.host, port), connectTimeoutMs) }
        }

        socket.soTimeout = readTimeoutMs
        socket.use { sock ->
            val out = sock.getOutputStream()
            val head = StringBuilder()
            head.append(method).append(' ').append(path).append(" HTTP/1.1\r\n")
            head.append("Host: ").append(uri.host)
            if (port != (if (secure) 443 else 80)) head.append(':').append(port)
            head.append("\r\n")
            head.append("Connection: close\r\n")
            headers.forEach { (k, v) -> head.append(k).append(": ").append(v).append("\r\n") }
            if (body != null) {
                head.append("Content-Length: ").append(body.size).append("\r\n")
            }
            head.append("\r\n")
            out.write(head.toString().toByteArray(Charsets.ISO_8859_1))
            if (body != null) out.write(body)
            out.flush()

            return readResponse(BufferedInputStream(sock.getInputStream()))
        }
    }

    private fun readResponse(input: BufferedInputStream): Response {
        val statusLine = readLine(input) ?: error("Empty response from server")
        val parts = statusLine.split(' ')
        val code = parts.getOrNull(1)?.toIntOrNull() ?: error("Bad status line: $statusLine")
        val headers = LinkedHashMap<String, String>()
        while (true) {
            val line = readLine(input) ?: break
            if (line.isEmpty()) break
            val idx = line.indexOf(':')
            if (idx > 0) headers[line.substring(0, idx).trim().lowercase()] = line.substring(idx + 1).trim()
        }

        val raw: InputStream = when {
            headers["transfer-encoding"]?.contains("chunked", ignoreCase = true) == true -> ChunkedInputStream(input)
            headers["content-length"] != null -> LimitedInputStream(input, headers["content-length"]!!.toLong())
            else -> input
        }
        val decoded = if (headers["content-encoding"]?.contains("gzip", ignoreCase = true) == true) {
            GZIPInputStream(raw)
        } else raw
        val text = decoded.readBytes().toString(Charsets.UTF_8)
        return Response(code, text, headers)
    }

    private fun readLine(input: InputStream): String? {
        val sb = StringBuilder()
        while (true) {
            val b = input.read()
            if (b == -1) return if (sb.isEmpty()) null else sb.toString()
            if (b == '\n'.code) return sb.toString().removeSuffix("\r")
            sb.append(b.toChar())
        }
    }

    private class LimitedInputStream(private val source: InputStream, private var remaining: Long) : InputStream() {
        override fun read(): Int {
            if (remaining <= 0) return -1
            val b = source.read()
            if (b != -1) remaining--
            return b
        }
        override fun read(b: ByteArray, off: Int, len: Int): Int {
            if (remaining <= 0) return -1
            val toRead = minOf(len.toLong(), remaining).toInt()
            val n = source.read(b, off, toRead)
            if (n > 0) remaining -= n
            return n
        }
    }

    private class ChunkedInputStream(private val source: InputStream) : InputStream() {
        private var remaining = 0L
        private var done = false

        private fun nextChunk() {
            val sizeLine = readLine(source) ?: run { done = true; return }
            val size = sizeLine.substringBefore(';').trim().toLongOrNull(16) ?: 0L
            if (size == 0L) {
                done = true
                while (true) { if (readLine(source).isNullOrEmpty()) break }
            } else remaining = size
        }

        override fun read(): Int {
            if (done) return -1
            if (remaining == 0L) {
                nextChunk()
                if (done) return -1
            }
            val b = source.read()
            if (b != -1) {
                remaining--
                if (remaining == 0L) readLine(source)
            }
            return b
        }
    }
}
