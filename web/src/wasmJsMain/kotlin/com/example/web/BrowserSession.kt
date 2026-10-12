package com.example.web

import kotlin.js.ExperimentalWasmJsInterop

/**
 * Per-browser session settings.
 *
 * The API key is held in `localStorage` so it survives a reload, and is sent to
 * the backend as `X-OpenHands-Key` — never to app.all-hands.dev directly, which
 * keeps the browser free of CORS preflights against a third-party origin.
 *
 * When the server already holds a key (`OPENHANDS_API_KEY`), the key is optional
 * here and the backend injects its own.
 */
@OptIn(ExperimentalWasmJsInterop::class)
object BrowserSession {

    private const val KEY_API_KEY = "openhands.apiKey"
    private const val KEY_UPSTREAM = "openhands.upstream"

    private var cachedApiKey: String = ""
    private var cachedUpstream: String = "https://app.all-hands.dev"

    fun load() {
        cachedApiKey = getItem(KEY_API_KEY)
        cachedUpstream = getItem(KEY_UPSTREAM).ifBlank { "https://app.all-hands.dev" }
    }

    fun apiKey(): String = cachedApiKey

    fun setApiKey(value: String) {
        cachedApiKey = value.trim()
        setItem(KEY_API_KEY, cachedApiKey)
    }

    fun upstream(): String = cachedUpstream

    fun setUpstream(value: String) {
        cachedUpstream = value.trim().ifBlank { "https://app.all-hands.dev" }
        setItem(KEY_UPSTREAM, cachedUpstream)
    }
}

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(key) => { try { return window.localStorage.getItem(key) ?? '' } catch (e) { return '' } }")
private external fun getItem(key: String): String

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(key, value) => { try { if (value === '') window.localStorage.removeItem(key); else window.localStorage.setItem(key, value) } catch (e) {} }")
private external fun setItem(key: String, value: String)
