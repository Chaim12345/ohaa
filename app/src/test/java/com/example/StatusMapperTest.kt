package com.example

import com.example.data.model.ConversationUiStatus
import com.example.data.model.StatusMapper
import org.junit.Assert.assertEquals
import org.junit.Test

class StatusMapperTest {

    @Test
    fun `sandbox error wins over execution status`() {
        assertEquals(
            ConversationUiStatus.ERROR,
            StatusMapper.map(sandboxStatus = "ERROR", executionStatus = "finished")
        )
    }

    @Test
    fun `missing sandbox maps to error`() {
        assertEquals(
            ConversationUiStatus.ERROR,
            StatusMapper.map(sandboxStatus = "MISSING", executionStatus = null)
        )
    }

    @Test
    fun `starting sandbox maps to starting`() {
        assertEquals(
            ConversationUiStatus.STARTING,
            StatusMapper.map(sandboxStatus = "STARTING", executionStatus = null)
        )
    }

    @Test
    fun `paused sandbox maps to paused`() {
        assertEquals(
            ConversationUiStatus.PAUSED,
            StatusMapper.map(sandboxStatus = "PAUSED", executionStatus = "running")
        )
    }

    @Test
    fun `running sandbox with running execution maps to running`() {
        assertEquals(
            ConversationUiStatus.RUNNING,
            StatusMapper.map(sandboxStatus = "RUNNING", executionStatus = "running")
        )
    }

    @Test
    fun `finished execution maps to finished`() {
        assertEquals(
            ConversationUiStatus.FINISHED,
            StatusMapper.map(sandboxStatus = "RUNNING", executionStatus = "finished")
        )
    }

    @Test
    fun `waiting for confirmation maps to waiting input`() {
        assertEquals(
            ConversationUiStatus.WAITING_INPUT,
            StatusMapper.map(sandboxStatus = "RUNNING", executionStatus = "waiting_for_confirmation")
        )
    }

    @Test
    fun `stuck and error execution map to error`() {
        assertEquals(ConversationUiStatus.ERROR, StatusMapper.map("RUNNING", "error"))
        assertEquals(ConversationUiStatus.ERROR, StatusMapper.map("RUNNING", "stuck"))
    }

    @Test
    fun `startup raw status maps to starting when no execution status`() {
        assertEquals(
            ConversationUiStatus.STARTING,
            StatusMapper.map(sandboxStatus = null, executionStatus = null, rawStatus = "PREPARING_REPOSITORY")
        )
    }

    @Test
    fun `generic fallback for legacy single-status payloads`() {
        assertEquals(ConversationUiStatus.FINISHED, StatusMapper.map(null, null, "READY"))
        assertEquals(ConversationUiStatus.RUNNING, StatusMapper.map(null, null, "WORKING"))
        assertEquals(ConversationUiStatus.ERROR, StatusMapper.map(null, null, "FAILED"))
        assertEquals(ConversationUiStatus.UNKNOWN, StatusMapper.map(null, null, "SOMETHING_ELSE"))
    }
}
