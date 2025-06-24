package com.example.personalfinanceapp

import android.Manifest
import android.content.Context
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement


/**
 * Instrumented UI test for the main app navigation.
 */
@RunWith(AndroidJUnit4::class)
class AppNavigationTest {

    private val composeTestRule = createAndroidComposeRule<MainActivity>()

    /**
     * The RuleChain ensures our custom rules run in the correct order:
     * 1. DisableAppLockRule runs first to turn off the app's biometric lock.
     * 2. GrantPermissionRule runs next to handle system-level OS permissions.
     * 3. The composeTestRule finally launches the activity into a clean, predictable state.
     */
    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(DisableAppLockRule())
        .around(GrantPermissionRule.grant(
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.POST_NOTIFICATIONS
        ))
        .around(composeTestRule)

    /**
     * Tests the navigation from the Dashboard screen to the Settings screen.
     */
    @Test
    fun testAppNavigation_fromDashboardToSettings() {
        // 1. Wait for the Dashboard screen to appear. This is the most robust way
        // to handle any initial app setup or race conditions.
        val isHeading = SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithText("Dashboard").fetchSemanticsNodes().isNotEmpty()
        }

        // Now that the UI has settled, we can safely perform our assertions.
        composeTestRule.onNode(
            hasText("Dashboard") and isHeading
        ).assertIsDisplayed()

        // 2. Find and click the Settings icon button.
        composeTestRule.onNodeWithContentDescription("Settings").performClick()

        // 3. Verify that we have navigated to the Settings screen by waiting for
        // the "GENERAL" text to appear (it's uppercased in the UI).
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("GENERAL").fetchSemanticsNodes().isNotEmpty()
        }

        // Assert that the "Settings" title is now displayed.
        composeTestRule.onNode(
            hasText("Settings") and isHeading
        ).assertIsDisplayed()

        // Confirm the "GENERAL" header is visible as a final check.
        composeTestRule.onNodeWithText("GENERAL").assertIsDisplayed()
    }
}
