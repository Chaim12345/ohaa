package com.example.util

/** Android clock: `System.currentTimeMillis()` is the platform's own wall clock. */
actual fun currentTimeMillis(): Long = System.currentTimeMillis()
