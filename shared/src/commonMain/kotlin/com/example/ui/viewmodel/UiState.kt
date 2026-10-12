package com.example.ui.viewmodel

/**
 * UI state types referenced by the shared screens.
 *
 * These sit alongside the Android ViewModel in :app but carry no platform
 * dependency, so both the Android and web targets share one definition.
 */

/** Sandbox action progress, so controls can disable while a call is in flight. */
data class SandboxUiState(
    val isWorking: Boolean = false,
    val message: String? = null
)

/** Stages of the launch flow, surfaced to the user while a task starts. */
enum class LaunchStage(val label: String) {
    IDLE(""),
    SUBMITTING("Submitting task…"),
    WAITING_SANDBOX("Waiting for sandbox…"),
    PREPARING_REPO("Preparing repository…"),
    SETTING_UP_SKILLS("Setting up skills…"),
    READY("Ready"),
    FAILED("Failed")
}

/** Top-level destinations, mirrored by the Android navigation bar. */
enum class AppTab(val label: String) {
    DASHBOARD("Dashboard"),
    CONVERSATIONS("Conversations"),
    NEW_TASK("New Task"),
    SETTINGS("Settings")
}
