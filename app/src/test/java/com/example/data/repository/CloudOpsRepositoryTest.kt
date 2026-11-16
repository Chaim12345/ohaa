package com.example.data.repository

import com.example.data.api.ApiClient
import com.example.data.api.CloudAdminApiService
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Proves the generated non-`/api/v1` cloud bindings are real: they build the
 * documented path, carry the Cloud Bearer credential, and (for bodies) send the
 * JSON payload. This exercises the actual Retrofit interface, not a mock.
 */
class CloudOpsRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: CloudOpsRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val api: CloudAdminApiService = ApiClient.createCloudAdmin(
            baseUrlProvider = { server.url("/").toString() },
            apiKeyProvider = { "cloud-key" },
        )
        repository = CloudOpsRepository(api)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `admin user listing hits the documented path with the bearer token`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"items":[]}"""))

        val response = repository.list_admin_users_api_admin_directory_users_get(orgId = "org-7")

        assertTrue(response.isSuccessful)
        val recorded = server.takeRequest()
        assertEquals("GET", recorded.method)
        assertEquals("/api/admin/directory/users?org_id=org-7", recorded.path)
        assertEquals("Bearer cloud-key", recorded.getHeader("Authorization"))
    }

    @Test
    fun `billing checkout posts the request body`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"url":"https://x"}"""))

        val response = repository.create_checkout_session_api_billing_create_checkout_session_post(
            body = """{"amount":10}""".toRequestBody("application/json".toMediaType()),
        )

        assertTrue(response.isSuccessful)
        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/api/billing/create-checkout-session", recorded.path)
        assertEquals("""{"amount":10}""", recorded.body.readUtf8())
    }

    @Test
    fun `keys endpoint carries the path parameter`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

        val response = repository.delete_api_key_api_keys_key_id_delete(keyId = "key-9")

        assertTrue(response.isSuccessful)
        val recorded = server.takeRequest()
        assertEquals("DELETE", recorded.method)
        assertEquals("/api/keys/key-9", recorded.path)
    }
}
