package io.pm.finlight

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
 * Instrumented UI tests for the full CRUD (Create, Read, Update, Delete)
 * lifecycle of a transaction.
 */
@RunWith(AndroidJUnit4::class)
class TransactionCrudTests {
    private val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val ruleChain: RuleChain =
        RuleChain
            .outerRule(DisableOnboardingRule())
            .around(DisableAppLockRule())
            .around(
                GrantPermissionRule.grant(
                    Manifest.permission.READ_SMS,
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.POST_NOTIFICATIONS,
                ),
            )
            .around(composeTestRule)

    /**
     * A helper function to add a transaction, reducing code duplication in tests.
     * @return The unique description of the created transaction.
     */
    private fun addTransactionForTest(): String {
        val uniqueDescription = "Test Transaction ${UUID.randomUUID()}"

        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithText("Monthly Budget").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithContentDescription("Add").performClick()

        // Wait for AddTransactionScreen to appear and fill the form
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("description_input").fetchSemanticsNodes().isNotEmpty()
        }
        // --- FIX: Use onNodeWithTag to reliably find the text field ---
        composeTestRule.onNodeWithTag("description_input").performTextInput(uniqueDescription)
        composeTestRule.onNodeWithText("0.00").performTextInput("100.0")
        composeTestRule.onNodeWithText("Select account").performClick()
        composeTestRule.onNodeWithText("SBI").performClick()
        composeTestRule.onNodeWithText("Select category").performClick()
        composeTestRule.onNodeWithText("Food & Drinks").performClick()
        composeTestRule.onNodeWithText("Save").performClick()

        // Wait to return to the dashboard and confirm the new item is there.
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText(uniqueDescription).fetchSemanticsNodes().isNotEmpty()
        }
        return uniqueDescription
    }

    /**
     * Tests that a newly created transaction appears on the dashboard.
     */
    @Test
    fun test_createTransaction_appearsOnDashboard() {
        val description = addTransactionForTest()
        composeTestRule.onNodeWithText(description, useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    /**
     * Tests that a transaction can be successfully edited and the update
     * is reflected on the dashboard.
     */
    @Test
    fun test_editTransaction_updatesSuccessfully() {
        val originalDescription = addTransactionForTest()
        val updatedDescription = "Updated UI Test Dinner"

        // 1. Find the transaction on the dashboard and click to open the detail screen.
        composeTestRule.onNodeWithText(originalDescription, useUnmergedTree = true)
            .performScrollTo()
            .performClick()

        // 2. On the detail screen, wait for it to load, then click the description to open the bottom sheet.
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText(originalDescription).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(originalDescription).performClick()

        // 3. In the bottom sheet, edit the text field and save.
        // --- FIX: Use onNodeWithTag to reliably find the text field ---
        composeTestRule.onNodeWithTag("value_input").performTextClearance()
        composeTestRule.onNodeWithTag("value_input").performTextInput(updatedDescription)
        composeTestRule.onNodeWithText("Save").performClick()


        // 4. Verify the description is updated on the detail screen.
        composeTestRule.onNodeWithText(updatedDescription).assertIsDisplayed()

        // 5. Navigate back to the dashboard.
        composeTestRule.onNodeWithContentDescription("Back").performClick()

        // 6. Assert that the old description is gone and the new one is displayed on the dashboard.
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("Monthly Budget").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(originalDescription).assertDoesNotExist()
        composeTestRule.onNodeWithText(updatedDescription, useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    /**
     * Tests that a transaction can be successfully deleted from the detail screen.
     */
    @Test
    fun test_deleteTransaction_removesFromList() {
        val description = addTransactionForTest()

        // 1. Find the transaction on the dashboard and click to open the detail screen.
        composeTestRule.onNodeWithText(description, useUnmergedTree = true)
            .performScrollTo()
            .performClick()

        // 2. On the detail screen, click the 'More' menu icon.
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithContentDescription("More options").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithContentDescription("More options").performClick()

        // 3. Click the 'Delete' option in the dropdown menu.
        composeTestRule.onNodeWithText("Delete").performClick()

        // 4. Confirm the deletion in the dialog.
        composeTestRule.onNodeWithText("Delete Transaction?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Delete").performClick()

        // 5. Wait to navigate back to the dashboard and assert the item is gone.
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("Monthly Budget").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(description).assertDoesNotExist()
    }
}
