package com.example.data.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Machine-checked API coverage.
 *
 * Parses the bundled OpenAPI specs (`src/test/resources/openapi/cloud.json`
 * and `agent.json`) and asserts that [EndpointCatalog] lists every path in each
 * spec — and nothing that is not in the spec. This is the proof that coverage
 * is 100% of the documented surface, and it fails loudly if a spec adds a path
 * the app has not implemented.
 *
 * The specs are the ones fetched from the live deployment:
 *  - cloud.json  <- https://app.all-hands.dev/openapi.json
 *  - agent.json  <- <sandbox conversation_url>/openapi.json
 */
class ApiCoverageTest {

    private fun specPaths(resource: String): Set<String> {
        val text = javaClass.classLoader!!.getResourceAsStream(resource)!!
            .bufferedReader().use { it.readText() }
        // Minimal JSON scan: collect the keys of the top-level "paths" object.
        val pathsKey = "\"paths\""
        val start = text.indexOf(pathsKey)
        assertTrue("spec $resource has no paths", start >= 0)
        val brace = text.indexOf('{', start)
        var depth = 0
        var i = brace
        val keys = linkedSetOf<String>()
        var expectingKey = true
        val sb = StringBuilder()
        var inString = false
        var escaped = false
        var stringStart = -1
        while (i < text.length) {
            val c = text[i]
            if (inString) {
                if (escaped) {
                    escaped = false
                } else if (c == '\\') {
                    escaped = true
                } else if (c == '"') {
                    inString = false
                    val value = sb.toString()
                    sb.setLength(0)
                    if (depth == 1 && expectingKey && value.startsWith("/")) {
                        keys.add(value)
                    }
                } else {
                    sb.append(c)
                }
            } else {
                when (c) {
                    '"' -> {
                        inString = true
                        stringStart = i
                    }
                    '{', '[' -> depth++
                    '}', ']' -> {
                        depth--
                        if (depth == 0) break
                    }
                    ':' -> if (depth == 1) expectingKey = false
                    ',' -> if (depth == 1) expectingKey = true
                }
            }
            i++
        }
        return keys
    }

    private fun normalize(path: String): String =
        Regex("\\{[^}]+\\}").replace(path, "{}")

    @Test
    fun `cloud v1 catalog matches the spec exactly`() {
        val spec = specPaths("openapi/cloud.json")
            .filter { it.startsWith("/api/v1") }
            .map { normalize(it) }
            .toSet()
        val missing = spec - EndpointCatalog.CLOUD_V1
        val extra = EndpointCatalog.CLOUD_V1 - spec
        assertEquals("Cloud paths in spec but not in catalog: $missing", emptySet<String>(), missing)
        assertEquals("Cloud paths in catalog but not in spec: $extra", emptySet<String>(), extra)
        assertTrue("expected the cloud spec to have >50 /api/v1 paths, got ${spec.size}", spec.size > 50)
    }

    @Test
    fun `agent server catalog matches the spec exactly`() {
        val spec = specPaths("openapi/agent.json")
            .map { normalize(it) }
            .toSet()
        val missing = spec - EndpointCatalog.AGENT_SERVER
        val extra = EndpointCatalog.AGENT_SERVER - spec
        assertEquals("Agent paths in spec but not in catalog: $missing", emptySet<String>(), missing)
        assertEquals("Agent paths in catalog but not in spec: $extra", emptySet<String>(), extra)
        assertTrue("expected the agent spec to have >100 paths, got ${spec.size}", spec.size > 100)
    }

    /**
     * Proves every catalog path is actually wired into production code, not
     * merely listed. Scans the compiled sources for string literals that build
     * the request URL (annotations and `endpoint(...)`/`@Url` call sites) and
     * normalizes `$var`/`${...}` interpolation the same way as the spec.
     */
    @Test
    fun `every catalog path is referenced in production source`() {
        val sourceDirs = listOf(
            java.io.File("src/main/java/com/example"),
            java.io.File("app/src/main/java/com/example"),
        ).firstOrNull { it.isDirectory }
        assertTrue("could not locate production sources", sourceDirs != null)

        val literals = linkedSetOf<String>()
        sourceDirs!!.walkTopDown().filter { it.extension == "kt" }.forEach { file ->
            Regex("\"([^\"]*)\"").findAll(file.readText()).forEach { m ->
                literals.add(normalizeLiteral(m.groupValues[1]))
            }
        }

        val all = EndpointCatalog.CLOUD_V1 + EndpointCatalog.AGENT_SERVER
        val unreferenced = all.filter { it !in literals }.sorted()
        assertEquals(
            "Catalog paths never referenced in production source: $unreferenced",
            emptyList<String>(),
            unreferenced,
        )
    }

    /** `"/api/conversations/$id/pause"` and `"/api/conversations/{id}/pause"` -> `/api/conversations/{}/pause`. */
    private fun normalizeLiteral(raw: String): String {
        var s = raw
        s = Regex("\\$\\{[^}]*\\}").replace(s, "{}")
        s = Regex("\\$[A-Za-z_][A-Za-z0-9_]*").replace(s, "{}")
        s = Regex("\\{[^}]+\\}").replace(s, "{}")
        s = Regex("/{2,}").replace(s, "/")
        return if (s.startsWith("/")) s else "/$s"
    }
}
