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
 * 2. **Explicit scope** — the non-v1 cloud surface is fully implemented, so every
 *    such path must appear in [EndpointCatalog.CLOUD_EXTRA] and
 *    [EndpointCatalog.OUT_OF_SCOPE_CLOUD] must be empty. Nothing can silently
 *    fall outside the app's scope.
 *
 * 3. **Real wiring** — every catalog path must appear as a genuine request-URL
 *    call site in production source. This scan deliberately EXCLUDES
 *    `EndpointCatalog.kt` itself **and the generic API Explorer**
 *    (`ApiExplorerRepository.kt`, `ApiExplorerScreen.kt`), so neither listing a
 *    path in the catalog nor hitting it through the runtime-path explorer
 *    satisfies it: there must be a Retrofit annotation, an `endpoint(base, path)`
 *    call, or a path literal passed to a service method. Comments are stripped
 *    first so a path mentioned only in a comment does not count. A companion test
 *    asserts the explorer contributes no call sites of its own.
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
    fun `every non-v1 cloud operation has a typed binding`() {
        // Path parity is not enough: a path can carry several methods. This parses
        // the spec and asserts each (method, path) operation has a matching
        // Retrofit annotation in the generated CloudAdminApiService.
        val text = File("app/src/main/java/com/example/data/api/CloudAdminApiService.kt")
            .takeIf { it.exists() }
            ?: File("src/main/java/com/example/data/api/CloudAdminApiService.kt")
        assertTrue("CloudAdminApiService.kt not found", text.exists())
        val bindings = linkedSetOf<String>()
        Regex("@(GET|POST|PUT|PATCH|DELETE|HEAD|OPTIONS)\\(\\s*\"([^\"]*)\"").findAll(text.readText())
            .forEach { bindings.add(it.groupValues[1] + " " + normalize(it.groupValues[2])) }

        val ops = cloudSpecOperations()
        assertTrue("expected ~242 non-v1 cloud operations, parsed ${ops.size}", ops.size > 200)
        val missing = mutableListOf<String>()
        ops.forEach { (method, path) ->
            if (!path.startsWith("/api/v1") && "$method ${normalize(path)}" !in bindings) {
                missing.add("$method $path")
            }
        }
        assertEquals("Non-v1 cloud operations with no typed binding: $missing", emptyList<String>(), missing)
    }

    private fun cloudSpecOperations(): List<Pair<String, String>> {
        // org.json is a stub in unit tests, so scan the JSON text directly. The
        // `paths` object has one key per path (depth 1) whose value is an object
        // of HTTP methods (depth 2). Track the depth-1 key while reading it.
        val text = javaClass.classLoader!!.getResourceAsStream("openapi/cloud.json")!!
            .bufferedReader().use { it.readText() }
        val match = Regex("\"paths\"\\s*:\\s*\\{").find(text)
        assertTrue("spec has no paths object", match != null)
        val brace = match!!.range.last
        val methods = setOf("get", "post", "put", "patch", "delete", "head", "options")
        val out = mutableListOf<Pair<String, String>>()

        var depth = 0
        var i = brace
        var inString = false
        var escaped = false
        val sb = StringBuilder()
        var currentPath: String? = null
        var expectingKey = true

        while (i < text.length) {
            val c = text[i]
            if (inString) {
                if (escaped) escaped = false
                else if (c == '\\') escaped = true
                else if (c == '"') {
                    inString = false
                    val value = sb.toString()
                    sb.setLength(0)
                    if (expectingKey) {
                        if (depth == 1 && value.startsWith("/")) {
                            currentPath = value
                        } else if (depth == 2 && value.lowercase() in methods && currentPath != null) {
                            out.add(value.uppercase() to currentPath!!)
                        }
                    }
                    expectingKey = false
                } else sb.append(c)
            } else {
                when (c) {
                    '"' -> inString = true
                    '{', '[' -> {
                        depth++
                        expectingKey = true
                    }
                    '}', ']' -> {
                        depth--
                        if (depth == 0) break
                        expectingKey = false
                    }
                    ':' -> expectingKey = false
                    ',' -> expectingKey = true
                }
            }
            i++
        }
        return out
    }

    @Test
    fun `every non-v1 cloud spec path is explicitly scoped out`() {
        val others = cloudSpec().filter { !it.startsWith("/api/v1") }.toSet()
        val unclassified = others - EndpointCatalog.OUT_OF_SCOPE_CLOUD - EndpointCatalog.CLOUD_EXTRA
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

    @Test
    fun `cloud extra catalog matches the spec exactly`() {
        // The non-v1 cloud surface is fully implemented, so CLOUD_EXTRA must equal
        // the non-v1 spec paths exactly — no path invented, none left uncovered.
        val others = cloudSpec().filter { !it.startsWith("/api/v1") }.toSet()
        val bogus = EndpointCatalog.CLOUD_EXTRA - others
        assertEquals("CLOUD_EXTRA paths not in the cloud spec: $bogus", emptySet<String>(), bogus)
        val uncovered = others - EndpointCatalog.CLOUD_EXTRA
        assertEquals(
            "Non-v1 cloud spec paths not implemented: $uncovered",
            emptySet<String>(),
            uncovered,
        )
        assertTrue("CLOUD_EXTRA should not be empty", EndpointCatalog.CLOUD_EXTRA.isNotEmpty())
    }

    @Test
    fun `no cloud spec path is out of scope`() {
        // Provable full coverage: the client implements the entire bundled cloud
        // spec, so the out-of-scope set is empty. This is the assertion that turns
        // "100% of the documented surface" into "100% of the spec".
        assertEquals(
            "OUT_OF_SCOPE_CLOUD must be empty for full coverage: ${EndpointCatalog.OUT_OF_SCOPE_CLOUD}",
            emptySet<String>(),
            EndpointCatalog.OUT_OF_SCOPE_CLOUD,
        )
        val specTotal = cloudSpec().size
        val covered = EndpointCatalog.CLOUD_V1.size + EndpointCatalog.CLOUD_EXTRA.size
        assertEquals("catalog must cover every cloud spec path", specTotal, covered)
    }

    // ---- 3. real wiring ----------------------------------------------------

    /**
     * Guards the exclusion in [every catalog path is wired to a real call site
     * in production source]: the generic API Explorer must not contribute any
     * call site. It builds request URLs at runtime from [EndpointCatalog], so it
     * has no literal path of its own; if one is ever added to make coverage
     * *look* satisfied, this fails.
     */
    @Test
    fun `the generic API explorer contributes no call sites of its own`() {
        val root = listOf(File("src/main/java/com/example"), File("app/src/main/java/com/example"))
            .firstOrNull { it.isDirectory }
        assertTrue("could not locate production sources", root != null)

        val explorerFiles = listOf("ApiExplorerRepository.kt", "ApiExplorerScreen.kt")
        val found = linkedSetOf<String>()
        root!!.walkTopDown()
            .filter { it.extension == "kt" && it.name in explorerFiles }
            .forEach { file ->
                val text = stripComments(file.readText())
                Regex("@(?:GET|POST|PUT|PATCH|DELETE|HEAD|OPTIONS)\\(\\s*\"([^\"]*)\"").findAll(text)
                    .forEach { if (it.groupValues[1].isNotBlank()) found.add(normalize(it.groupValues[1])) }
                Regex("endpoint\\([^,]+,\\s*\"([^\"]+)\"").findAll(text)
                    .forEach { found.add(normalize(it.groupValues[1])) }
                Regex("\"(/?api/[^\"\\s]*)\"").findAll(text)
                    .forEach { found.add(normalize(it.groupValues[1])) }
                Regex("\"(/v1/[^\"\\s]*)\"").findAll(text)
                    .forEach { found.add(normalize(it.groupValues[1])) }
            }
        assertEquals(
            "The API Explorer must not hard-code catalog paths; wiring them there " +
                "would let an unimplemented endpoint pass the coverage test: $found",
            emptySet<String>(),
            found,
        )
    }

    @Test
    fun `every catalog path is wired to a real call site in production source`() {
        val root = listOf(File("src/main/java/com/example"), File("app/src/main/java/com/example"))
            .firstOrNull { it.isDirectory }
        assertTrue("could not locate production sources", root != null)

        val callSites = linkedSetOf<String>()
        root!!.walkTopDown()
            .filter { it.extension == "kt" && it.name != "EndpointCatalog.kt" }
            // The generic API Explorer calls arbitrary paths at runtime; it must
            // never be allowed to satisfy this test. Only typed bindings or
            // explicit call sites in the real data layer count as "wired".
            .filter { it.name != "ApiExplorerRepository.kt" && it.name != "ApiExplorerScreen.kt" }
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

        val all = EndpointCatalog.CLOUD_V1 + EndpointCatalog.CLOUD_EXTRA + EndpointCatalog.AGENT_SERVER
        val unwired = all
            .filter { it !in callSites }
            .sorted()
        assertEquals(
            "Catalog paths with no real call site in production source: $unwired",
            emptyList<String>(),
            unwired,
        )
    }

    private fun stripComments(text: String): String {
        var s = Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL).replace(text, "")
        s = Regex("//[^\n]*").replace(s, "")
        return s
    }
}
