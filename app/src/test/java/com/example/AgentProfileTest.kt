package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.data.settings.AgentKind
import com.example.data.settings.AgentProfile
import com.example.data.settings.AgentProfileStore
import com.example.data.settings.DEFAULT_OPENHANDS_MODEL
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Agent profile behaviour:
 * - default is the plain OpenHands agent (ACP only when explicitly chosen),
 * - per-conversation overrides enable mid-session switching,
 * - clearing an override falls back to the default.
 */
@RunWith(RobolectricTestRunner::class)
class AgentProfileTest {

    private fun store(): AgentProfileStore =
        AgentProfileStore(ApplicationProvider.getApplicationContext())

    @Test
    fun `default profile is the OpenHands agent with a LiteLLM model`() {
        val profile = store().defaultProfile()
        assertEquals(AgentKind.OPENHANDS, profile.kind)
        assertEquals(DEFAULT_OPENHANDS_MODEL, profile.model)
        assertTrue(profile.isValid())
        assertTrue("default label should show OpenHands", profile.shortLabel().contains("OpenHands"))
    }

    @Test
    fun `ACP is used when explicitly selected via settings`() {
        val profile = AgentProfile(
            kind = AgentKind.ACP,
            acpCommand = "npx -y @agentclientprotocol/claude-agent-acp"
        )
        assertTrue(profile.isValid())
        assertTrue(profile.shortLabel().contains("ACP"))
        assertEquals(profile.shortLabel().split(" · ").size, 2)

        // Persisting + reloading an ACP default keeps it ACP.
        val store = store()
        store.saveDefaultProfile(profile)
        val loaded = store.defaultProfile()
        assertEquals(AgentKind.ACP, loaded.kind)
        assertEquals(profile.acpCommand, loaded.acpCommand)
    }

    @Test
    fun `per conversation override persists and survives a reload`() {
        val store = store()
        store.saveDefaultProfile(AgentProfile()) // OpenHands default
        store.setProfileFor(
            "conversation-1",
            AgentProfile(kind = AgentKind.OPENHANDS, model = "openai/gpt-4o")
        )

        // Fresh store instance (reload path)
        val reloaded = AgentProfileStore(ApplicationProvider.getApplicationContext())
        assertEquals("openai/gpt-4o", reloaded.profileFor("conversation-1").model)

        // Unrelated conversation still uses the default
        assertEquals(DEFAULT_OPENHANDS_MODEL, reloaded.profileFor("other-conversation").model)
    }

    @Test
    fun `clearing an override falls back to the default`() {
        val store = store()
        store.saveDefaultProfile(
            AgentProfile(kind = AgentKind.OPENHANDS, model = "o3")
        )
        store.setProfileFor(
            "c2",
            AgentProfile(kind = AgentKind.OPENHANDS, model = "openai/gpt-4o")
        )
        assertEquals("openai/gpt-4o", store.profileFor("c2").model)

        store.clearProfileFor("c2")
        assertEquals("o3", store.profileFor("c2").model)
    }

    @Test
    fun `shortLabel shows the active model tail for OpenHands agent`() {
        val profile = AgentProfile(kind = AgentKind.OPENHANDS, model = "openai/gpt-4o")
        assertEquals("OpenHands · gpt-4o", profile.shortLabel())
    }

    @Test
    fun `invalid profiles are reported`() {
        assertTrue(!AgentProfile(kind = AgentKind.OPENHANDS, model = "").isValid())
        assertTrue(!AgentProfile(kind = AgentKind.OPENHANDS, model = "no-slash").isValid())
        assertTrue(!AgentProfile(kind = AgentKind.ACP, acpCommand = "").isValid())
    }
}