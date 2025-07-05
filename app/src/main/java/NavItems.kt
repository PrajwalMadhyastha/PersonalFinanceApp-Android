// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/NavItems.kt
// REASON: FEATURE - Added a title for the new `retrospective_update_screen`
// route to ensure the TopAppBar displays the correct title when the user
// navigates to the new screen.
// =================================================================================
package io.pm.finlight

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    object Dashboard : BottomNavItem("dashboard", Icons.Filled.Home, "Dashboard")
    object Transactions : BottomNavItem("transaction_list", Icons.Filled.Receipt, "Transactions")
    object Reports : BottomNavItem("reports_screen", Icons.Filled.Assessment, "Reports")
    object Profile : BottomNavItem("profile", Icons.Filled.Person, "Profile")
}
val screenTitles = mapOf(
    BottomNavItem.Dashboard.route to "Dashboard",
    BottomNavItem.Transactions.route to "All Transactions",
    BottomNavItem.Reports.route to "Reports",
    BottomNavItem.Profile.route to "Profile",
    "settings_screen" to "App Settings",
    "add_transaction" to "Add Transaction",
    "transaction_detail/{transactionId}" to "Edit Transaction",
    "account_list" to "Your Accounts",
    "add_account" to "Add New Account",
    "edit_account/{accountId}" to "Edit Account",
    "account_detail/{accountId}" to "Account Details",
    "budget_screen" to "Manage Budgets",
    "add_budget" to "Add Category Budget",
    "edit_budget/{budgetId}" to "Edit Budget",
    "category_list" to "Manage Categories",
    "recurring_transactions" to "Recurring Transactions",
    "add_recurring_transaction" to "Add Recurring Rule",
    "search_screen" to "Search",
    "review_sms_screen" to "Review SMS Transactions",
    "approve_transaction_screen/{amount}/{type}/{merchant}/{smsId}/{smsSender}" to "Approve Transaction",
    "tag_management" to "Manage Tags",
    "edit_profile" to "Edit Profile",
    "income_screen" to "Income",
    "rule_creation_screen/{potentialTransactionJson}" to "Create Parsing Rule",
    "manage_parse_rules" to "Manage Parsing Rules",
    "manage_ignore_rules" to "Manage Ignore List",
    "link_transaction_screen/{potentialTransactionJson}" to "Link to Existing Transaction",
    // --- NEW: Title for the retrospective update screen ---
    "retrospective_update_screen/{transactionId}/{originalDescription}?newDescription={newDescription}&newCategoryId={newCategoryId}" to "Update Similar"
)
