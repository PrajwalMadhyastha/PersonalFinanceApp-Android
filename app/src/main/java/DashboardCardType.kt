// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/DashboardCardType.kt
// REASON: NEW FILE - This enum defines all the possible cards that can be
// displayed on the dashboard. It provides a type-safe way to manage the layout
// configuration, making it easy to reference and reorder the dashboard components.
// =================================================================================
package io.pm.finlight

/**
 * Represents the different types of cards that can be displayed on the dashboard.
 * Used for managing customizable dashboard layouts.
 */
enum class DashboardCardType {
    OVERALL_BUDGET,
    MONTHLY_STATS,
    QUICK_ACTIONS,
    NET_WORTH,
    RECENT_ACTIVITY,
    ACCOUNTS_SUMMARY,
    BUDGET_WATCH
}
