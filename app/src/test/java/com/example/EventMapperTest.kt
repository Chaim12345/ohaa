package com.example

import com.example.data.api.ApiClient
import com.example.data.api.EventMapper
import com.example.data.model.EventCategory
import com.example.data.model.EventDto
import com.example.data.model.EventKind
import com.example.data.model.PageDto
import com.squareup.moshi.Types
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Parses real V1 event shapes (from the OpenAPI schema) through Moshi + EventMapper. */
class EventMapperTest {

    private val eventAdapter = ApiClient.moshi.adapter(EventDto::class.java)
    private val pageAdapter = ApiClient.moshi.adapter<PageDto<EventDto>>(
        Types.newParameterizedType(PageDto::class.java, EventDto::class.java)
    )

    private fun parse(json: String) = EventMapper.map(eventAdapter.fromJson(json)!!, "conv-1", 0L)

    @Test
    fun userMessageEvent_becomesUserBubble() {
        val e = parse(
            """
            {"id":"01J","kind":"MessageEvent","timestamp":"2026-09-08T07:31:29.652635Z","source":"user",
             "llm_message":{"role":"user","content":[{"type":"text","text":"Fix the failing test"},{"type":"image","image_urls":["data:x"]}]},
             "activated_skills":[],"extended_content":[]}
            """
        )
        assertEquals(EventKind.MESSAGE, e.kind)
        assertEquals(EventCategory.USER_MESSAGE, e.category)
        assertTrue(e.isFromUser)
        assertEquals("Fix the failing test\n[1 image attached]", e.body)
        assertTrue(e.timestamp > 0)
        assertFalse(e.isNoise)
    }

    @Test
    fun terminalAction_extractsCommandAndThought() {
        val e = parse(
            """
            {"id":"a1","kind":"ActionEvent","timestamp":"2026-09-08T07:32:00Z","source":"agent",
             "thought":[{"type":"text","text":"Let me run the tests"}],
             "action":{"kind":"TerminalAction","command":"pytest -q","is_input":false},
             "tool_name":"terminal","tool_call_id":"call_1","security_risk":"LOW","summary":"Run test suite"}
            """
        )
        assertEquals(EventCategory.COMMAND, e.category)
        assertEquals("pytest -q", e.code)
        assertEquals("Let me run the tests", e.body)
        assertEquals("Run test suite", e.title)
        assertEquals("terminal", e.toolName)
        assertNull(e.securityRisk) // LOW/UNKNOWN are not surfaced
    }

    @Test
    fun terminalObservation_nonZeroExitIsError() {
        val e = parse(
            """
            {"id":"o1","kind":"ObservationEvent","timestamp":"2026-09-08T07:32:05Z","source":"environment",
             "tool_name":"terminal","tool_call_id":"call_1","action_id":"a1",
             "observation":{"kind":"TerminalObservation","content":[{"type":"text","text":"FAILED tests/test_x.py::test_y"}],
                            "is_error":false,"command":"pytest -q","exit_code":1,"timeout":false}}
            """
        )
        assertEquals(EventCategory.OBSERVATION, e.category)
        assertTrue(e.isError)
        assertEquals(1, e.exitCode)
        assertEquals("a1", e.actionId)
        assertEquals("FAILED tests/test_x.py::test_y", e.code)
    }

    @Test
    fun fileEditorStrReplace_rendersDiff() {
        val e = parse(
            """
            {"id":"a2","kind":"ActionEvent","source":"agent","thought":[],
             "action":{"kind":"FileEditorAction","command":"str_replace","path":"/workspace/app/x.py","old_str":"return False","new_str":"return True"},
             "tool_name":"file_editor"}
            """
        )
        assertEquals(EventCategory.FILE_EDIT, e.category)
        assertEquals("/workspace/app/x.py", e.filePath)
        assertEquals("diff", e.codeLabel)
        assertEquals("- return False\n+ return True", e.code)
    }

    @Test
    fun finishAction_isFinishCategory() {
        val e = parse("""{"id":"f","kind":"ActionEvent","source":"agent","action":{"kind":"FinishAction","message":"All done"},"tool_name":"finish"}""")
        assertEquals(EventCategory.FINISH, e.category)
        assertEquals("All done", e.body)
    }

    @Test
    fun errorEvents_flagIsError() {
        val server = parse("""{"id":"s","kind":"ServerErrorEvent","source":"environment","code":"RuntimeError","detail":"sandbox crashed"}""")
        assertTrue(server.isError)
        assertEquals("RuntimeError", server.title)
        assertEquals("sandbox crashed", server.body)

        val agent = parse("""{"id":"e","kind":"AgentErrorEvent","source":"agent","tool_name":"terminal","tool_call_id":"c","error":"tool exploded"}""")
        assertTrue(agent.isError)
        assertEquals("tool exploded", agent.body)
    }

    @Test
    fun noiseKinds_areMarkedNoise() {
        listOf("SystemPromptEvent", "TokenEvent", "LLMCompletionLogEvent", "StreamingDeltaEvent", "ConversationStateUpdateEvent", "Condensation").forEach { kind ->
            val e = parse("""{"id":"n-$kind","kind":"$kind","source":"agent"}""")
            assertTrue("$kind should be noise", e.isNoise)
        }
        assertFalse(parse("""{"id":"p","kind":"PauseEvent","source":"user"}""").isNoise)
    }

    @Test
    fun unknownKind_doesNotCrash() {
        val e = parse("""{"id":"u","kind":"SomeFutureEvent","source":"agent","summary":"hello"}""")
        assertEquals(EventKind.UNKNOWN, e.kind)
        assertEquals("SomeFutureEvent", e.title)
        assertEquals("hello", e.body)
    }

    @Test
    fun eventPage_parsesItemsAndCursor() {
        val page = pageAdapter.fromJson("""{"items":[{"id":"1","kind":"PauseEvent"},{"id":"2","kind":"MessageEvent","source":"agent","llm_message":{"role":"assistant","content":[{"type":"text","text":"hi"}]}}],"next_page_id":"abc"}""")
        assertNotNull(page)
        assertEquals(2, page!!.items.size)
        assertEquals("abc", page.nextPageId)
        assertEquals("hi", EventMapper.map(page.items[1], "c").body)
    }
}

class AcpEventMapperTest {
    private val adapter = ApiClient.moshi.adapter(EventDto::class.java)
    private fun parse(json: String) = EventMapper.map(adapter.fromJson(json)!!, "conv-1", 0L)

    @Test
    fun acpPendingCall_isMarkedPending() {
        val e = parse("""{"id":"p1","kind":"ACPToolCallEvent","timestamp":"2026-09-08T07:05:40.427801","source":"agent","tool_call_id":"call_1","title":"Terminal","status":"pending","tool_kind":"execute","raw_input":{},"raw_output":null,"content":null,"is_error":false}""")
        assertTrue(e.isPendingToolCall)
        assertEquals(EventCategory.COMMAND, e.category)
        assertEquals("call_1", e.toolCallId)
        assertNull(e.code)
        assertTrue(e.timestamp > 0) // naive timestamp parsed as UTC
    }

    @Test
    fun acpCompletedExecute_hasCommandAndOutput() {
        val e = parse("""{"id":"c1","kind":"ACPToolCallEvent","source":"agent","tool_call_id":"call_1","title":"git status","status":"completed","tool_kind":"execute",
            "raw_input":{"command":"git status","description":"Show status"},"raw_output":"On branch main","content":[{"content":{"type":"text","text":"```console\nOn branch main\n```"}}],"is_error":false}""")
        assertFalse(e.isPendingToolCall)
        assertFalse(e.isError)
        assertEquals("git status", e.title)
        assertEquals("Show status", e.body)
        assertEquals("$ git status\nOn branch main", e.code)
        assertEquals("bash", e.codeLabel)
    }

    @Test
    fun acpFailedExecute_parsesExitCode() {
        val e = parse("""{"id":"f1","kind":"ACPToolCallEvent","source":"agent","tool_call_id":"call_2","title":"pytest","status":"failed","tool_kind":"execute",
            "raw_input":{"command":"pytest"},"raw_output":"Exit code 1\nFAILED","content":null,"is_error":false}""")
        assertTrue(e.isError)
        assertEquals(1, e.exitCode)
        assertEquals("pytest (failed)", e.title)
    }

    @Test
    fun acpRead_isFileRead() {
        val e = parse("""{"id":"r1","kind":"ACPToolCallEvent","source":"agent","tool_call_id":"call_3","title":"Read x.kt","status":"completed","tool_kind":"read",
            "raw_input":{"file_path":"/workspace/project/x.kt"},"raw_output":"1\tpackage a","content":null,"is_error":false}""")
        assertEquals(EventCategory.FILE_READ, e.category)
        assertEquals("/workspace/project/x.kt", e.filePath)
        assertEquals("x.kt", e.codeLabel)
    }
}

class ReasoningAndFinishTest {
    private val adapter = ApiClient.moshi.adapter(EventDto::class.java)
    private fun parse(json: String) = EventMapper.map(adapter.fromJson(json)!!, "c", 0L)

    @Test
    fun finishAction_withEmptyThoughtUsesReasoningContent() {
        val e = parse("""{"id":"a","kind":"ActionEvent","source":"agent","tool_name":"finish","thought":[],
            "reasoning_content":"The user wants me to inspect the PR.","action":{"kind":"FinishAction","message":"Done: merged and pushed."}}""")
        assertEquals(EventCategory.FINISH, e.category)
        assertTrue(e.body!!.contains("The user wants me to inspect the PR."))
        assertTrue(e.body!!.contains("Done: merged and pushed."))
    }

    @Test
    fun finishObservation_showsAgentAnswer() {
        val e = parse("""{"id":"o","kind":"ObservationEvent","source":"environment","tool_name":"finish","tool_call_id":"t","action_id":"a",
            "observation":{"kind":"FinishObservation","is_error":false,"content":[{"type":"text","text":"Short answer: no, not yet."}]}}""")
        assertEquals(EventCategory.FINISH, e.category)
        assertEquals("Short answer: no, not yet.", e.body)
    }
}
