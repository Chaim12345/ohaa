package com.example.util

/**
 * Browser clock.
 *
 * Uses the browser's own `Date.now()` rather than kotlinx-datetime's
 * `Clock.System`, whose singleton fails to link in a wasm build with
 * `IrLinkageError: Can not get instance of singleton 'System'`.
 */
actual fun currentTimeMillis(): Long = jsDateNow().toLong()

@JsFun("() => Date.now()")
private external fun jsDateNow(): Double
