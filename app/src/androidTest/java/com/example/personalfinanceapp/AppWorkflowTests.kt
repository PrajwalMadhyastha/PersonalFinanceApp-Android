package com.example.personalfinanceapp

import android.Manifest
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.util.UUID

/**
 * Instrumented UI test for common user workflows in the application.
 */
@RunWith(AndroidJUnit4::class)
class AppWorkflowTests {
    private val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val ruleChain: RuleChain =
        RuleChain
            .outerRule(DisableAppLockRule())
            .around(
                GrantPermissionRule.grant(
                    Manifest.permission.READ_SMS,
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.POST_NOTIFICATIONS,
                ),
            )
            .around(composeTestRule)

    /**
     * Tests the "happy path" workflow of adding a new transaction and verifying
     * it appears on the dashboard.
     */
    @Test
    fun test_addNewTransaction_appearsOnDashboard() {
        val uniqueDescription = "Test Coffee Purchase ${UUID.randomUUID()}"

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("Dashboard").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithContentDescription("Add Transaction").performClick()

        composeTestRule.onNodeWithText("Add New Transaction").assertIsDisplayed()
        composeTestRule.onNodeWithText("Description").performTextInput(uniqueDescription)
        composeTestRule.onNodeWithText("Amount").performTextInput("150.0")
        composeTestRule.onNodeWithText("Select Account").performClick()
        composeTestRule.onNodeWithText("SBI").performClick()
        composeTestRule.onNodeWithText("Select Category").performClick()
        composeTestRule.onNodeWithText("Food").performClick()

        composeTestRule.onNodeWithText("Save Transaction").performClick()

        val newNode = composeTestRule.onNodeWithText(uniqueDescription, useUnmergedTree = true)
        newNode.performScrollTo()
        newNode.assertIsDisplayed()
    }

    /**
     * Tests the "sad path" workflow where a user tries to save a transaction
     * with invalid input (e.g., non-numeric amount).
     */
    @Test
    fun test_addTransaction_failsWithInvalidAmount_showsValidationError() {
        // 1. Navigate to Add Transaction Screen
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("Dashboard").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithContentDescription("Add Transaction").performClick()

        // 2. Fill out the form, but with an invalid (non-numeric) amount.
        composeTestRule.onNodeWithText("Add New Transaction").assertIsDisplayed()
        composeTestRule.onNodeWithText("Description").performTextInput("Test Invalid Amount")

        // CORRECTED: Enter invalid text in the amount field to enable the button.
        composeTestRule.onNodeWithText("Amount").performTextInput("not-a-number")

        composeTestRule.onNodeWithText("Select Account").performClick()
        composeTestRule.onNodeWithText("SBI").performClick()

        composeTestRule.onNodeWithText("Select Category").performClick()
        composeTestRule.onNodeWithText("Food").performClick()

        // 3. Attempt to save the invalid transaction
        composeTestRule.onNodeWithText("Save Transaction").performClick()

        // 4. Verify the validation error
        // Assert that we are still on the "Add New Transaction" screen.
        composeTestRule.onNodeWithText("Add New Transaction").assertIsDisplayed()

        // Assert that the snackbar with the specific validation error is shown.
        val expectedError = "Please enter a valid, positive amount."
        composeTestRule.onNodeWithText(expectedError).assertIsDisplayed()
    }
}
