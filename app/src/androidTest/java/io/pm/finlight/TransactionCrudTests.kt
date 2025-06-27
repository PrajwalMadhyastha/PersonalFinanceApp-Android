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
        composeTestRule.onNodeWithText("Add Transaction").assertIsDisplayed()
        composeTestRule.onNodeWithText("Description").performTextInput(uniqueDescription)
        composeTestRule.onNodeWithText("Amount").performTextInput("100.0")
        composeTestRule.onNodeWithText("Select Account").performClick()
        composeTestRule.onNodeWithText("SBI").performClick()
        composeTestRule.onNodeWithText("Select Category").performClick()
        composeTestRule.onNodeWithText("Food").performClick()
        composeTestRule.onNodeWithText("Save Transaction").performClick()

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

        // 1. Find the transaction and click to edit it.
        composeTestRule.onNodeWithText(originalDescription, useUnmergedTree = true)
            .performScrollTo()
            .performClick()

        // 2. On the edit screen, change the description.
        // --- FIXED: Wait for the edit screen to fully load before proceeding ---
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("Update Transaction").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Description").performTextClearance()
        composeTestRule.onNodeWithText("Description").performTextInput(updatedDescription)
        composeTestRule.onNodeWithText("Update Transaction").performClick()

        // 3. Wait to navigate back to the dashboard.
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("Monthly Budget").fetchSemanticsNodes().isNotEmpty()
        }

        // 4. Assert that the old description is gone and the new one is displayed.
        composeTestRule.onNodeWithText(originalDescription).assertDoesNotExist()
        composeTestRule.onNodeWithText(updatedDescription, useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    /**
     * Tests that a transaction can be successfully deleted.
     */
    @Test
    fun test_deleteTransaction_removesFromList() {
        val description = addTransactionForTest()

        // 1. Find the transaction and click to edit it.
        composeTestRule.onNodeWithText(description, useUnmergedTree = true)
            .performScrollTo()
            .performClick()

        // 2. On the edit screen, click the new delete button.
        // --- FIXED: Wait for the edit screen to fully load before proceeding ---
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("Update Transaction").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Delete Transaction").performClick()

        // 3. Confirm the deletion in the dialog.
        composeTestRule.onNodeWithText("Confirm Deletion").assertIsDisplayed()
        composeTestRule.onNodeWithText("Delete").performClick()


        // 4. Wait to navigate back to the dashboard and assert the item is gone.
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("Monthly Budget").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(description).assertDoesNotExist()
    }
}
