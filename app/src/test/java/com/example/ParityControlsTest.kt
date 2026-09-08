package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.api.ApiClient
import com.example.data.local.AppDatabase
import com.example.data.repository.OpenHandsRepository
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Covers the webui-parity repository surface (model catalog, paged repo search,
 * sandbox creation, profile CRUD, provider tokens, trajectory export) against
 * MockWebServer with real V1 payload shapes.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ParityControlsTest {

    private lateinit var server: MockWebServer
    private lateinit var db: AppDatabase
    private lateinit var repo: OpenHandsRepository

    @Before
    fun setUp() {
        server = MockWebServer().also { it.start() }
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences(OpenHandsRepository.PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        repo = OpenHandsRepository(context, db) { _, keyProvider ->
            ApiClient.create(server.url("/").toString(), keyProvider, enableBodyLogging = false)
        }
        repo.updateConfig("sk-oh-test", server.url("/").toString())
    }

    @After
    fun tearDown() {
        server.shutdown()
        db.close()
    }

    private fun json(body: String, code: Int = 200) =
        MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body)

    @Test
    fun searchModels_mapsVerifiedCatalogAndSkipsBlankIds() = runBlocking {
        // Given a models/search page with a blank-id entry mixed in.
        server.enqueue(json("""{"items":[{"id":"anthropic/claude-opus-4-6","provider":"anthropic","verified":true},{"id":"","provider":"x","verified":true},{"id":"openai/gpt-5","provider":"openai","verified":false}],"next_page_id":null}"""))

        // When loading the model catalog.
        val models = repo.searchModels().getOrThrow()

        // Then blank ids are dropped and fields map through; request pins verified+limit.
        assertEquals(2, models.size)
        assertEquals("anthropic/claude-opus-4-6", models[0].id)
        assertEquals("anthropic", models[0].provider)
        assertTrue(models[0].verified)
        val path = server.takeRequest().path!!
        assertTrue(path, path.startsWith("/api/v1/config/models/search?"))
        assertTrue(path, path.contains("verified__eq=true") && path.contains("limit=50"))
    }

    @Test
    fun searchRepositoriesPaged_returnsReposAndNextPageId() = runBlocking {
        // Given a repo search page with a cursor.
        server.enqueue(json("""{"items":[{"id":"1","full_name":"Chaim12345/ohaa","git_provider":"github","is_public":false,"main_branch":"main","stargazers_count":3,"pushed_at":"2026-09-08T07:00:00Z"}],"next_page_id":"cursor1"}"""))

        // When searching with an empty query (preload path).
        val (repos, next) = repo.searchRepositoriesPaged("", provider = "github").getOrThrow()

        // Then the repo maps and the cursor is returned for load-more.
        assertEquals(1, repos.size)
        assertEquals("Chaim12345/ohaa", repos[0].fullName)
        assertEquals("main", repos[0].mainBranch)
        assertEquals("cursor1", next)
        val path = server.takeRequest().path!!
        assertTrue(path, path.startsWith("/api/v1/git/repositories/search?") && path.contains("provider=github"))
    }

    @Test
    fun createSandbox_postsAndMapsDto() = runBlocking {
        // Given the server accepts a sandbox creation.
        server.enqueue(json("""{"id":"sb1","sandbox_spec_id":null,"status":"STARTING","created_at":"2026-09-08T07:00:00Z"}"""))

        // When creating with the default spec.
        val sandbox = repo.createSandbox().getOrThrow()

        // Then the DTO maps and no spec query param is sent.
        assertEquals("sb1", sandbox.id)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/sandboxes", request.path)
    }

    @Test
    fun setGitProviderToken_postsProviderKeyedMap() = runBlocking {
        // Given the server accepts provider tokens.
        server.enqueue(json("""{}"""))

        // When saving a GitHub token.
        repo.setGitProviderToken("github", "ghp_x").getOrThrow()

        // Then the body nests the token under provider_tokens.<provider>.
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/secrets/git-providers", request.path)
        val body = request.body.readUtf8()
        assertTrue(body, body.contains("\"provider_tokens\"") && body.contains("\"github\"") && body.contains("\"token\":\"ghp_x\""))
    }

    @Test
    fun saveRenameDeleteProfile_hitProfileEndpoints() = runBlocking {
        // Given the server accepts profile writes.
        server.enqueue(json("""{}"""))
        server.enqueue(json("""{}"""))
        server.enqueue(json("""{}"""))

        // When creating, renaming and deleting a profile.
        repo.saveLlmProfile("dev", "openai/gpt-5", "https://x", null).getOrThrow()
        repo.renameLlmProfile("dev", "prod").getOrThrow()
        repo.deleteLlmProfile("prod").getOrThrow()

        // Then each hits its endpoint; the create body carries model keys, rename carries new_name.
        val create = server.takeRequest()
        assertEquals("POST", create.method)
        assertEquals("/api/v1/settings/profiles/dev", create.path)
        val createBody = create.body.readUtf8()
        assertTrue(createBody, createBody.contains("\"model\":\"openai/gpt-5\"") && createBody.contains("\"base_url\":\"https://x\""))
        val rename = server.takeRequest()
        assertEquals("POST", rename.method)
        assertEquals("/api/v1/settings/profiles/dev/rename", rename.path)
        val renameBody = rename.body.readUtf8()
        assertTrue(renameBody, renameBody.contains("\"new_name\":\"prod\""))
        val delete = server.takeRequest()
        assertEquals("DELETE", delete.method)
        assertEquals("/api/v1/settings/profiles/prod", delete.path)
    }

    @Test
    fun downloadTrajectory_returnsRawBytes() = runBlocking {
        // Given a trajectory payload.
        server.enqueue(json("""{"events":[]}"""))

        // When downloading it.
        val bytes = repo.downloadTrajectory("conv1").getOrThrow()

        // Then raw bytes come back and the download endpoint was hit.
        assertTrue(String(bytes), String(bytes).contains("\"events\""))
        assertEquals("/api/v1/app-conversations/conv1/download", server.takeRequest().path)
    }

    @Test
    fun getGitOrganizations_mapsToStrings() = runBlocking {
        // Given the organizations endpoint.
        server.enqueue(json("""{"provider":"github","organizations":["acme","personal"]}"""))

        // When loading organizations.
        val orgs = repo.getGitOrganizations().getOrThrow()

        // Then names map through.
        assertEquals(listOf("acme", "personal"), orgs)
        assertEquals("/api/v1/users/git-organizations", server.takeRequest().path)
    }
}
