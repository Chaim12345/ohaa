package com.example.data.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Machine-checked API coverage, in three independent parts:
 *
 * 1. **Spec parity** — [EndpointCatalog.CLOUD_V1] and [EndpointCatalog.AGENT_SERVER]
 *    equal the paths in the bundled OpenAPI specs, both directions. A path added
 *    to the spec but not the catalog (or vice versa) fails.
 *
 * 2. **Explicit scope** — every *other* path in the cloud spec must appear in
 *    [EndpointCatalog.OUT_OF_SCOPE_CLOUD]. The out-of-scope list is therefore
 *    exhaustive: a new server path cannot silently fall outside the app's scope.
 *
 * 3. **Real wiring** — every catalog path must appear as a genuine request-URL
 *    call site in production source. This scan deliberately EXCLUDES
 *    `EndpointCatalog.kt` itself, so listing a path in the catalog does not
 *    satisfy it: there must be a Retrofit annotation, an `endpoint(base, path)`
 *    call, or a path literal passed to a service method. Comments are stripped
 *    first so a path mentioned only in a comment does not count.
 */
class ApiCoverageTest {

    // ---- spec parsing ------------------------------------------------------

    private fun specPaths(resource: String): Set<String> {
        val text = javaClass.classLoader!!.getResourceAsStream(resource)!!
            .bufferedReader().use { it.readText() }
        val start = text.indexOf("\"paths\"")
        assertTrue("spec $resource has no paths", start >= 0)
        val brace = text.indexOf('{', start)
        var depth = 0
        var i = brace
        val keys = linkedSetOf<String>()
        var expectingKey = true
        val sb = StringBuilder()
        var inString = false
        var escaped = false
        while (i < text.length) {
            val c = text[i]
            if (inString) {
                if (escaped) escaped = false
                else if (c == '\\') escaped = true
                else if (c == '"') {
                    inString = false
                    val value = sb.toString()
                    sb.setLength(0)
                    if (depth == 1 && expectingKey && value.startsWith("/")) keys.add(value)
                } else sb.append(c)
            } else {
                when (c) {
                    '"' -> inString = true
                    '{', '[' -> depth++
                    '}', ']' -> { depth--; if (depth == 0) break }
                    ':' -> if (depth == 1) expectingKey = false
                    ',' -> if (depth == 1) expectingKey = true
                }
            }
            i++
        }
        return keys
    }

    private fun normalize(path: String): String {
        var s = Regex("\\{[^}]+\\}").replace(path, "{}")
        s = Regex("\\$\\{[^}]*\\}").replace(s, "{}")
        s = Regex("\\$[A-Za-z_][A-Za-z0-9_]*").replace(s, "{}")
        s = Regex("/{2,}").replace(s, "/")
        return if (s.startsWith("/")) s else "/$s"
    }

    private fun cloudSpec() = specPaths("openapi/cloud.json").map { normalize(it) }.toSet()
    private fun agentSpec() = specPaths("openapi/agent.json").map { normalize(it) }.toSet()

    // ---- 1. spec parity ----------------------------------------------------

    @Test
    fun `cloud v1 catalog matches the spec exactly`() {
        val spec = cloudSpec().filter { it.startsWith("/api/v1") }.toSet()
        assertEquals("Cloud paths in spec but not in catalog", emptySet<String>(), spec - EndpointCatalog.CLOUD_V1)
        assertEquals("Cloud paths in catalog but not in spec", emptySet<String>(), EndpointCatalog.CLOUD_V1 - spec)
        assertTrue("expected >50 cloud /api/v1 paths, got ${spec.size}", spec.size > 50)
    }

    @Test
    fun `agent server catalog matches the spec exactly`() {
        val spec = agentSpec()
        assertEquals("Agent paths in spec but not in catalog", emptySet<String>(), spec - EndpointCatalog.AGENT_SERVER)
        assertEquals("Agent paths in catalog but not in spec", emptySet<String>(), EndpointCatalog.AGENT_SERVER - spec)
        assertTrue("expected >100 agent paths, got ${spec.size}", spec.size > 100)
    }

    // ---- 2. explicit scope -------------------------------------------------

    @Test
    fun `every non-v1 cloud spec path is explicitly scoped out`() {
        val others = cloudSpec().filter { !it.startsWith("/api/v1") }.toSet()
        val unclassified = others - EndpointCatalog.OUT_OF_SCOPE_CLOUD
        assertEquals(
            "Cloud spec paths neither implemented nor declared out of scope: $unclassified",
            emptySet<String>(),
            unclassified,
        )
        val stale = EndpointCatalog.OUT_OF_SCOPE_CLOUD - others
        assertEquals(
            "Out-of-scope entries that no longer exist in the spec: $stale",
            emptySet<String>(),
            stale,
        )
    }

    // ---- 3. real wiring ----------------------------------------------------

    @Test
    fun `every catalog path is wired to a real call site in production source`() {
        val root = listOf(File("src/main/java/com/example"), File("app/src/main/java/com/example"))
            .firstOrNull { it.isDirectory }
        assertTrue("could not locate production sources", root != null)

        val callSites = linkedSetOf<String>()
        root!!.walkTopDown()
            .filter { it.extension == "kt" && it.name != "EndpointCatalog.kt" }
            .forEach { file ->
                val text = stripComments(file.readText())
                // Retrofit annotation paths, e.g. @GET("api/v1/users/me")
                Regex("@(?:GET|POST|PUT|PATCH|DELETE|HEAD|OPTIONS)\\(\\s*\"([^\"]*)\"").findAll(text)
                    .forEach { if (it.groupValues[1].isNotBlank()) callSites.add(normalize(it.groupValues[1])) }
                // endpoint(base, "/api/...") helper calls
                Regex("endpoint\\([^,]+,\\s*\"([^\"]+)\"").findAll(text)
                    .forEach { callSites.add(normalize(it.groupValues[1])) }
                // bare path literals used as @Url arguments
                Regex("\"(/?api/[^\"\\s]*)\"").findAll(text)
                    .forEach { callSites.add(normalize(it.groupValues[1])) }
                Regex("\"(/v1/[^\"\\s]*)\"").findAll(text)
                    .forEach { callSites.add(normalize(it.groupValues[1])) }
            }

        val all = EndpointCatalog.CLOUD_V1 + EndpointCatalog.AGENT_SERVER
        val unwired = all
            .filter { it !in callSites }
            .filter { it !in EndpointCatalog.UNWIRED_BY_TRANSPORT }
            .sorted()
        assertEquals(
            "Catalog paths with no real call site in production source: $unwired",
            emptyList<String>(),
            unwired,
        )
    }

    @Test
    fun `declared transport-unwired paths are real and still unwired`() {
        // The declaration must not become a dumping ground: each entry must exist
        // in the catalog, and must genuinely have no call site (so it cannot be
        // used to hide a path that is actually wired).
        val all = EndpointCatalog.CLOUD_V1 + EndpointCatalog.AGENT_SERVER
        val bogus = EndpointCatalog.UNWIRED_BY_TRANSPORT - all
        assertEquals("UNWIRED_BY_TRANSPORT entries not in the catalog: $bogus", emptySet<String>(), bogus)

        val root = listOf(File("src/main/java/com/example"), File("app/src/main/java/com/example"))
            .firstOrNull { it.isDirectory }
        assertTrue("could not locate production sources", root != null)
        val callSites = linkedSetOf<String>()
        root!!.walkTopDown()
            .filter { it.extension == "kt" && it.name != "EndpointCatalog.kt" }
            .forEach { file ->
                val text = stripComments(file.readText())
                Regex("@(?:GET|POST|PUT|PATCH|DELETE|HEAD|OPTIONS)\\(\\s*\"([^\"]*)\"").findAll(text)
                    .forEach { if (it.groupValues[1].isNotBlank()) callSites.add(normalize(it.groupValues[1])) }
                Regex("endpoint\\([^,]+,\\s*\"([^\"]+)\"").findAll(text)
                    .forEach { callSites.add(normalize(it.groupValues[1])) }
                Regex("\"(/?api/[^\"\\s]*)\"").findAll(text)
                    .forEach { callSites.add(normalize(it.groupValues[1])) }
                Regex("\"(/v1/[^\"\\s]*)\"").findAll(text)
                    .forEach { callSites.add(normalize(it.groupValues[1])) }
            }
        val nowWired = EndpointCatalog.UNWIRED_BY_TRANSPORT.filter { it in callSites }
        assertEquals(
            "These paths are declared unwired but now have a call site — remove them from UNWIRED_BY_TRANSPORT: $nowWired",
            emptyList<String>(),
            nowWired,
        )
    }

    private fun stripComments(text: String): String {
        var s = Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL).replace(text, "")
        s = Regex("//[^\n]*").replace(s, "")
        return s
    }
}
