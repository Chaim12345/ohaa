package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.data.model.AcpEffortOptionDto
import com.example.data.model.AcpModelInfoDto
import com.example.ui.screens.ModelSheet
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Pins the ACP runtime-switch gating on the Model sheet: when the conversation's
 * profile reports `acp_supports_runtime_model_switch = false`, the model and
 * effort controls must be disabled with an explanation rather than issuing a
 * call the server will reject.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp", sdk = [36])
class ModelSheetAcpTest {

    @get:Rule val composeTestRule = createComposeRule()

    private val models = listOf(AcpModelInfoDto(modelId = "claude-sonnet", name = "Sonnet"))
    private val efforts = listOf(AcpEffortOptionDto(value = "high", name = "High"))

    private fun setSheet(
        supportsSwitch: Boolean,
        onSwitchModel: (String) -> Unit = {},
        onSwitchEffort: (String) -> Unit = {},
    ) {
        composeTestRule.setContent {
            MyApplicationTheme {
                ModelSheet(
                    isAcp = true,
                    currentModel = "claude-sonnet",
                    profiles = emptyList(),
                    acpModels = models,
                    catalogModels = emptyList(),
                    efforts = efforts,
                    currentEffort = "high",
                    acpSupportsRuntimeModelSwitch = supportsSwitch,
                    isBusy = false,
                    onSwitchProfile = {},
                    onSwitchModel = onSwitchModel,
                    onSwitchEffort = onSwitchEffort,
                    onSearchModels = {},
                    onDismiss = {},
                )
            }
        }
    }

    @Test
    fun `model switch is disabled when the profile forbids it`() {
        var switched = false
        setSheet(supportsSwitch = false, onSwitchModel = { switched = true })
        composeTestRule.onNodeWithTag("acp_model_claude-sonnet").performScrollTo().assertIsNotEnabled()
        composeTestRule.onNodeWithText("This ACP agent does not support switching the model at runtime.")
            .assertIsDisplayed()
        assertEquals(false, switched)
    }

    @Test
    fun `model switch fires when the profile allows it`() {
        var switched: String? = null
        setSheet(supportsSwitch = true, onSwitchModel = { switched = it })
        composeTestRule.onNodeWithTag("acp_model_claude-sonnet").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals("claude-sonnet", switched)
    }
}
