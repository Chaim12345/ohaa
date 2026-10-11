package com.example

import androidx.test.core.app.ApplicationProvider
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
 * - a single OpenHands agent with an LLM model selection,
 * - per-conversation overrides enable mid-session switching,
 * - clearing an override falls back to the default.
 */
@RunWith(RobolectricTestRunner::class)
class AgentProfileTest {

    private fun store(): AgentProfileStore =
        AgentProfileStore(ApplicationProvider.getApplicationContext())

    @Test
    fun `default profile uses the OpenHands default model`() {
        val profile = store().defaultProfile()
        assertEquals(DEFAULT_OPENHANDS_MODEL, profile.model)
        assertTrue(profile.isValid())
        assertEquals("deepseek-v4.1-flash", profile.shortLabel())
    }

    @Test
    fun `per conversation override persists and survives a reload`() {
        val store = store()
        store.saveDefaultProfile(AgentProfile())
        store.setProfileFor("conversation-1", AgentProfile(model = "openhands/deepseek-v4-flash"))

        val reloaded = AgentProfileStore(ApplicationProvider.getApplicationContext())
        assertEquals("openhands/deepseek-v4-flash", reloaded.profileFor("conversation-1").model)
        assertEquals(DEFAULT_OPENHANDS_MODEL, reloaded.profileFor("other-conversation").model)
    }

    @Test
    fun `clearing an override falls back to the default`() {
        val store = store()
        store.saveDefaultProfile(AgentProfile(model = "openhands/deepseek-v4-flash"))
        store.setProfileFor("c2", AgentProfile(model = "openhands/deepseek-v4.1-flash"))
        assertEquals("openhands/deepseek-v4.1-flash", store.profileFor("c2").model)

        store.clearProfileFor("c2")
        assertEquals("openhands/deepseek-v4-flash", store.profileFor("c2").model)
    }

    @Test
    fun `shortLabel is the bare model name`() {
        assertEquals("deepseek-v4.1-flash", AgentProfile(model = "openhands/deepseek-v4.1-flash").shortLabel())
    }

    @Test
    fun `invalid profiles are reported`() {
        assertTrue(!AgentProfile(model = "").isValid())
        assertTrue(AgentProfile(model = "openhands/deepseek-v4.1-flash").isValid())
    }
}
